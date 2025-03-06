<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# MLIR ODS Changelog

## [Unreleased]
### Added
- Added resolution of simple identifiers referring to 'defs' and similar.
### Fixed
- Fixed exception in highlighter when `let` does not yet have a field identifier.

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
