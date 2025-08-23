package com.github.zero9178.mlirods

import com.intellij.openapi.util.IconLoader

object MyIcons {
    @JvmField
    val TableGenIcon = IconLoader.getIcon("/icons/tablegen-file.svg", javaClass)

    @JvmField
    val TableGenClass = IconLoader.getIcon("/icons/tablegen-class.svg", javaClass)

    @JvmField
    val TableGenDef = IconLoader.getIcon("/icons/tablegen-def.svg", javaClass)

    @JvmField
    val TableGenOverriding = IconLoader.getIcon("/icons/tablegen-overridingMethod.svg", javaClass)

    @JvmField
    val TableGenOverridden = IconLoader.getIcon("/icons/tablegen-overriddenMethod.svg", javaClass)
}
