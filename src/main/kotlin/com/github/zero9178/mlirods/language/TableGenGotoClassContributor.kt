package com.github.zero9178.mlirods.language

import com.github.zero9178.mlirods.index.CLASS_INDEX
import com.github.zero9178.mlirods.index.processAllKeys
import com.github.zero9178.mlirods.index.processElements
import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.NavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.indexing.IdFilter

private class TableGenGotoClassContributor : ChooseByNameContributorEx {
    override fun processNames(
        processor: Processor<in String>,
        scope: GlobalSearchScope,
        filter: IdFilter?
    ) {
        CLASS_INDEX.processAllKeys(processor, scope, filter)
    }

    override fun processElementsWithName(
        name: String,
        processor: Processor<in NavigationItem>,
        parameters: FindSymbolParameters
    ) {
        CLASS_INDEX.processElements(name, parameters.project, parameters.searchScope, processor)
    }
}
