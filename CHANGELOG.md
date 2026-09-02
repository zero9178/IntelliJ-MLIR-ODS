<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# MLIR ODS Changelog

## [Unreleased]

### Added

- Typing `!` now offers completion for every bang operator known to the plugin, inserting the parentheses of its argument list and, in the case of `!cast`, also the angle brackets of its type argument. Delimiters that have already been written are reused and, if left unclosed, completed to a balanced pair, and the Tab key moves the caret out of an inserted pair — for `!cast`, from the type argument into the parentheses and finally behind them.
- Completing a class or the `list` and `bits` types now also enters angled brackets that were already written but are still empty, and the Tab key moves the caret out of the brackets after the content has been written.

### Fixed

- Files reachable from the compile commands are now indexed right after opening a project, rather than only once a module's content roots happened to change afterwards. Their classes, defs and other definitions were previously not found by references, completion or find usages until e.g. CMake reconfigured.
- Completing `list` or `bits` in front of already written angled brackets no longer inserts a second empty pair.
- Selecting a class, `list` or `bits` completion by typing `<` no longer types that `<` into the angled brackets inserted by the completion.
- Fixed an exception when an include points to a file that isn't a TableGen file.
- TableGen files are now recognized based on the IDE's filetype setting rather than by extension.

## [0.15.0] - 2026-08-07

### Added

- Added an error for `include` directives whose path cannot be resolved to a file.
- Added an error for class references that do not resolve to any class.
- Reference-resolution errors are suppressed in files not reachable from any compile commands, where references cannot be resolved anyway.
- Added reference resolution for `#ifdef`/`#ifndef` directives to their corresponding `#define`, including navigation, renaming from either side, and find usages.
- The `!cast` and `!filter` operators are now highlighted as bang operators.
- Added an error for accesses to a field that the surrounding record neither defines nor inherits.
- Added an editor banner warning that a file's includes and references cannot be resolved when the file is not reachable from any file with compile commands.
- `multiclass` statements now introduce a scope, so that references within them resolve to the multiclass' template arguments and to statements declared inside it.
- Added an error for a template argument whose value is of a type that is not assignable to the argument's declared type.
- Exceptions originating from the plugin can now be reported to the plugin's vendor from the IDE's error dialog.

### Changed

- Massively improved performance when compile commands or the include graph changes.

### Fixed

- Binary literals such as `0b1010` are now `bits<n>` values with one bit per digit written rather than integers, making e.g. `{ 0b1010, 0b0101 }` a `bits<8>` rather than a `bits<2>`.
- Binary literals are no longer accepted where TableGen requires an integer, such as the width of a `bits<n>` type.
- Usages of a definition are now highlighted and found in every TableGen file, rather than only in those belonging to the same module as the definition.
- A class that is forward declared before being defined, as LLVM's `Instruction` is, is now recognized as one class rather than two. Records deriving from its definition are therefore no longer reported as having the wrong type where the declaration is used as a template argument type or base class.

## [0.14.0] - 2026-06-25

### Added

- Added errors for unknown bang operators and for bang operators given the wrong number of arguments
- Added an error for a `!div` whose divisor evaluates to a constant zero, including when the zero only becomes apparent through a `def`'s instantiation of a class
- Added support for the `!sort` operator
- Added support for the `!filter` operator, including reference resolution and type inference of its iteration variable
- Added support for the `!switch` operator, including validation of its case clauses and mandatory default value
- Added an inspection reporting fields that redefine an existing field, with a quick fix to convert them into a `let` statement
- Added an error for positional arguments that follow a named argument in a class argument list
- Added distinct syntax highlighting for named arguments in a class argument list
- Added reference resolution and renaming for named arguments in a class argument list
- Added errors for invalid class argument lists: duplicate arguments, unknown or excess arguments, and missing required arguments

## [0.13.0] - 2026-05-13

### Added

- Added a settings page under `Tools` -> `TableGen` that allows disabling the LSP.
- Added support for `append` and `prepend` modifiers in `let` items and statements

### Fixed

- The plugin is now compatible with all JetBrains IDEs.
- Auto-completion of directories in `include` will no longer place redundant slashes. 
- `let`-item syntax in `let`-statements has been to their new syntax with braces rather than angle-brackets.

## [0.12.0] - 2026-02-05

### Added

- Line markers for fields that are overwritten (either by `let` or another field).
- Breadcrumbs in the UI for TableGen elements that open a scope.
- Identifier resolution for `foreach` statements.

### Fixed

- Stale includes paths when `tablegen_compile_commands.yml` is modified, created or deleted.

## [0.11.0] - 2025-09-01

### Added

- Line markers to `let` body items that navigates to the previous `let` statement that is overridden.
- Line markers to `class` statements that allows navigation to derived `def`- and `class` statements.

### Fixed

- Class statement lookup used for fields now finds definitions before declarations.
- Exception when the user wrote infinite `include` statements.

## [0.10.0] - 2025-08-21

### Added

- Code completion for identifiers in included files.
- Brace insertion for click string literals. Typing `[{` will now automatically insert `}]`.
- Parameter info popup for class instantiation now shows the parameter name and type of the corresponding template arguments of the class.
- Implemented global `def` lookup. 

### Fixed

- Better error recovery for invalid syntax in `dag` arguments.
- Better error recovery for invalid syntax in `dag` init lists.
- Better error recovery for invalid syntax in inheritance list.
- Fixed an exception when an included file gets deleted.
- `include` statements included as part of the context (i.e. `test.td` file is only used as part of compilation of `other.td` and `other.td` has includes before `test.td`) are now properly tracked.

## [0.9.0] - 2025-07-22

### Added

- Trailing comma syntax for slice elements
- Parsing of `include` statements anywhere in the file
- Cross-file class name completion
- Parsing dump statements within records

### Fixed

- Stack overflow exception when using `Find Usage` on a TableGen file
- Syntax errors in slice syntax when using a `-` separator
- Exception when an include directory is deleted
- Correct parsing of `def` names to exclude curly braces
- Index-out-of-date exceptions and improved startup time
- Class name resolution within the referenced class
- Error recovery when parsing parent class lists

## [0.8.0] - 2025-07-12

### Added

- Support for remote CLion toolchains in include resolution
- Preprocessor support with conditional compilation and highlighting

### Fixed

- Find usage action no longer working
- Parser error recovery in list inits
- Parser error recovery in the top level file
- LSP build prompt is no longer shown for unsupported toolchains
- Fixed exception when include directory is deleted

## [0.7.0] - 2025-05-29

### Added

- Code completion for keywords
- Code completion for `let` targets in bodies
- Always inserting matching braces

### Fixed

- Compatibility with future IDE versions

## [0.6.0] - 2025-04-20

### Added

- Added usage type categories in `Find Usage`
- Local code completion of class types
- Local code completion of `def` values
- Code completion of field names
- Code completion of paths in `include` directives
- Added `def`s and `class`s to the GoTo menu (pressing shift twice)
- Resolve `def`s to `!foldl` and `!foreach` operators

### Fixed

- Fix null pointer exception on `defvar`s without a name
- Null pointer exception while typing `include` strings
- Parser recovery in class template definitions
- Compilation against 2025 IDE versions

### Removed

- Disabled use of LSP for `Find usage` and `Goto Definition`. The IDE is more accurate at this point and having two
items in the `Find usage` functionality confusing for users.

## [0.5.0] - 2025-03-23

### Added

- Added resolution of simple identifiers referring to `def`s and similar.
- Restrict `def` search to files which are included (directly or transitively) in the current file.
- Added resolution of class names to class statements.
- Added resolution of field names.
- Identifiers referencing fields are now highlighted as such.
- Implemented renaming of `def`s
- Implemented renaming of classes
- Implemented renaming of fields

### Fixed

- Fixed exception in highlighter when `let` does not yet have a field identifier.
- Parsing priority of the `#` operator

## [0.4.0] - 2025-02-18

### Added

- Added resolution of include statements, enabling the `GoTo` action 
- Implemented renaming of include statements, enabling the `Rename` action of TableGen files
- Implemented syntax highlighting of preprocessor directives and macro names

## [0.3.0] - 2025-02-04

### Added

- Syntax highlighting of fields in `let`-expressions, record fields and field accesses
- Color page in `Editor -> Color Scheme -> TableGen` 
- Added editor folding of code literals, braced bodies and angle brackets   
- Syntax highlighting of bang operators

## [0.2.0] - 2025-01-16

### Added

- Enabled `Comment with Line/Block Comment` functionality in TableGen
- Implemented automatic insertion of pairs of `"` (Smart Keys `Insert Pair Quote` functionality)
- Implement syntax highlighting of escape sequences in string literals

### Fixed

- Enable and verify for 2025 IDEs

## [0.1.0] - 2025-01-03

### Added

- Initial file type and syntax highlighting support
- Add highlighting of matching delimiters

## [0.0.2] - 2025-01-01

### Fixed

- Possible race conditions and cancellations on project startup leading to the LSP not being started.

## [0.0.1] - 2024-12-27

### Added

- Support for using `tblgen-lsp-server` built as part of the opened CMake project

[Unreleased]: https://github.com/zero9178/IntelliJ-MLIR-ODS/compare/v0.15.0...HEAD
[0.15.0]: https://github.com/zero9178/IntelliJ-MLIR-ODS/compare/v0.14.0...v0.15.0
[0.14.0]: https://github.com/zero9178/IntelliJ-MLIR-ODS/compare/v0.13.0...v0.14.0
[0.13.0]: https://github.com/zero9178/IntelliJ-MLIR-ODS/compare/v0.12.0...v0.13.0
[0.12.0]: https://github.com/zero9178/IntelliJ-MLIR-ODS/compare/v0.11.0...v0.12.0
[0.11.0]: https://github.com/zero9178/IntelliJ-MLIR-ODS/compare/v0.10.0...v0.11.0
[0.10.0]: https://github.com/zero9178/IntelliJ-MLIR-ODS/compare/v0.9.0...v0.10.0
[0.9.0]: https://github.com/zero9178/IntelliJ-MLIR-ODS/compare/v0.8.0...v0.9.0
[0.8.0]: https://github.com/zero9178/IntelliJ-MLIR-ODS/compare/v0.7.0...v0.8.0
[0.7.0]: https://github.com/zero9178/IntelliJ-MLIR-ODS/compare/v0.6.0...v0.7.0
[0.6.0]: https://github.com/zero9178/IntelliJ-MLIR-ODS/compare/v0.5.0...v0.6.0
[0.5.0]: https://github.com/zero9178/IntelliJ-MLIR-ODS/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/zero9178/IntelliJ-MLIR-ODS/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/zero9178/IntelliJ-MLIR-ODS/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/zero9178/IntelliJ-MLIR-ODS/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/zero9178/IntelliJ-MLIR-ODS/compare/v0.0.2...v0.1.0
[0.0.2]: https://github.com/zero9178/IntelliJ-MLIR-ODS/compare/v0.0.1...v0.0.2
[0.0.1]: https://github.com/zero9178/IntelliJ-MLIR-ODS/commits/v0.0.1
