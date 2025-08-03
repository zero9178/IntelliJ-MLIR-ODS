package com.github.zero9178.mlirods.language

import com.github.zero9178.mlirods.index.ALL_RECORDS_INDEX
import com.github.zero9178.mlirods.index.DEF_INDEX
import com.github.zero9178.mlirods.index.processAllKeys
import com.github.zero9178.mlirods.index.processElements
import com.github.zero9178.mlirods.language.stubs.disallowTreeLoading
import com.github.zero9178.mlirods.language.values.TableGenRecordValue
import com.github.zero9178.mlirods.language.values.TableGenStringValue
import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.NavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.indexing.IdFilter

private class TableGenDefNameGotoSymbolContributor : ChooseByNameContributorEx {
    override fun processNames(
        processor: Processor<in String>,
        scope: GlobalSearchScope,
        filter: IdFilter?
    ) {
        DEF_INDEX.processAllKeys(processor, scope, filter)
    }

    override fun processElementsWithName(
        name: String,
        processor: Processor<in NavigationItem>,
        parameters: FindSymbolParameters
    ) {
        DEF_INDEX.processElements(name, parameters.project, parameters.searchScope, processor)
    }
}

private class TableGenOpMnemonicGotoSymbolContributor : ChooseByNameContributorEx {
    override fun processNames(
        processor: Processor<in String>,
        scope: GlobalSearchScope,
        filter: IdFilter?
    ) = disallowTreeLoading {
        val project = scope.project ?: return@disallowTreeLoading
        ALL_RECORDS_INDEX.processElements(0, project, scope) {
            val mnemonic =
                TableGenRecordValue(it).fields["opName"] as? TableGenStringValue ?: return@processElements true
            val dialect =
                (TableGenRecordValue(it).fields["opDialect"] as? TableGenRecordValue)?.fields?.get("name") as? TableGenStringValue
                    ?: return@processElements true
            processor.process(mnemonic.value + "." + dialect.value)
        }
    }

    override fun processElementsWithName(
        name: String,
        processor: Processor<in NavigationItem>,
        parameters: FindSymbolParameters
    ) = disallowTreeLoading {
        ALL_RECORDS_INDEX.processElements(0, parameters.project, parameters.searchScope) {
            val mnemonic =
                TableGenRecordValue(it).fields["opName"] as? TableGenStringValue ?: return@processElements true
            val dialect =
                (TableGenRecordValue(it).fields["opDialect"] as? TableGenRecordValue)?.fields?.get("name") as? TableGenStringValue
                    ?: return@processElements true
            if (mnemonic.value + "." + dialect.value == name)
                processor.process(it)
            else true
        }
    }
}
