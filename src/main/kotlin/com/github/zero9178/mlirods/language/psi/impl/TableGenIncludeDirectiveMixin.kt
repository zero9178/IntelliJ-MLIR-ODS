package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.psi.TableGenFile
import com.github.zero9178.mlirods.language.generated.psi.TableGenIncludeDirective
import com.github.zero9178.mlirods.language.psi.TableGenIncludeReferenceSet
import com.github.zero9178.mlirods.language.psi.impl.TableGenPsiImplUtil.Companion.getStringValue
import com.github.zero9178.mlirods.language.stubs.impl.TableGenIncludeDirectiveStub
import com.github.zero9178.mlirods.model.TableGenContextService
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
                val tableGenFile = containingFile as? TableGenFile
                val result = tableGenFile?.run {
                    context.includePaths.firstNotNullOfOrNull {
                        if (!it.isValid) return@firstNotNullOfOrNull null

                        it.findFileByRelativePath(includeSuffix)
                    }
                }
                CachedValueProvider.Result.create(
                    result,
                    project.service<TableGenContextService>().includeResultModificationTracker,
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
