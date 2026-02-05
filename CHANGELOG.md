<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# MLIR ODS Changelog

## [Unreleased]

### Fixed

- The plugin is now compatible with all JetBrains IDEs.

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

[Unreleased]: https://github.com/zero9178/IntelliJ-MLIR-ODS/compare/v0.12.0...HEAD
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
