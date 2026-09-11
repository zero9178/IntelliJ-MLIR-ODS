# MLIR ODS

![Build](https://github.com/zero9178/IntelliJ-MLIR-ODS/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/26204-mlir-ods.svg)](https://plugins.jetbrains.com/plugin/26204-mlir-ods)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/26204-mlir-ods.svg)](https://plugins.jetbrains.com/plugin/26204-mlir-ods)

<!-- Plugin description -->
Language support for [TableGen](https://llvm.org/docs/TableGen/) `.td` files, the language LLVM and
[MLIR ODS](https://mlir.llvm.org/docs/DefiningDialects/Operations/) use to define dialects, operations, types and
attributes.

Features:

* Syntax highlighting, brace matching, code folding and breadcrumbs.
* Code completion for fields, keywords, bang operators and for classes and definitions from other files.
* Navigation to definitions, find usages, rename, go-to-class and go-to-symbol, parameter info and gutter markers for
  inherited classes and overridden field assignments.
* Quick documentation showing an element's definition together with its doc comment.
* Error highlighting and inspections for syntax mistakes, unresolved references, unknown fields and mismatched template
  arguments.

In CLion, includes are resolved using the `tablegen_compile_commands.yml` file of the active CMake profile, so files of
the LLVM monorepo are analyzed with the same include paths that TableGen itself is invoked with.
<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "MLIR ODS"</kbd> >
  <kbd>Install</kbd>
  
- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/26204-mlir-ods) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/26204-mlir-ods/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/zero9178/IntelliJ-MLIR-ODS/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
