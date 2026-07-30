package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.psi.TableGenFile
import com.github.zero9178.mlirods.language.generated.psi.TableGenIncludeDirective
import com.github.zero9178.mlirods.language.psi.TableGenIncludeReferenceSet
import com.github.zero9178.mlirods.language.psi.impl.TableGenPsiImplUtil.Companion.getStringValue
import com.github.zero9178.mlirods.language.stubs.impl.TableGenIncludeDirectiveStub
import com.github.zero9178.mlirods.model.TableGenIncludeGraphService
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.openapi.components.service
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.concurrency.annotations.RequiresReadLock

abstract class TableGenIncludeDirectiveMixin : StubBasedPsiElementBase<TableGenIncludeDirectiveStub>,
    TableGenIncludeDirective {

    private val mySubtreeModificationTracker = SimpleModificationTracker()
    private val myIncludedFileCache = lazy {
        CachedValuesManager.getManager(project)
            .createCachedValue {
                val service = project.service<TableGenIncludeGraphService>()
                // The active context's include paths are the source of truth for include resolution; the graph resolves
                // its own edges against the very same paths, so both stay in sync.
                val result = (containingFile as? TableGenFile)?.includePaths.orEmpty().firstNotNullOfOrNull {
                    if (!it.isValid) return@firstNotNullOfOrNull null

                    it.findFileByRelativePath(includeSuffix)
                }
                // Anything that may make this directive resolve to a different file – the file's context changing, the
                // file it resolves to appearing or disappearing – changes the edge the graph derives from this very
                // directive and therefore the graph itself.
                CachedValueProvider.Result.create(
                    result,
                    service.graphChangedModificationTracker,
                    mySubtreeModificationTracker
                )
            }
    }

    override fun subtreeChanged() {
        super.subtreeChanged()
        mySubtreeModificationTracker.incModificationCount()
    }

    override val includedFile: VirtualFile?
        @RequiresReadLock
        get() = myIncludedFileCache.value.value

    override val includeSuffix: String
        get() = greenStub?.includeSuffix ?: string?.let { getStringValue(it) } ?: ""

    override fun getReferences(): Array<out FileReference?> {
        return TableGenIncludeReferenceSet(this).allReferences
    }

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenIncludeDirectiveStub, stubType: IStubElementType<*, *>) : super(stub, stubType)
}
