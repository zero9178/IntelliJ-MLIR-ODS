<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# MLIR ODS Changelog

## [Unreleased]

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
