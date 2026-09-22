#!/usr/bin/env python3
"""Regenerates 'testData/llvm-project', the copy of LLVM's TableGen files the integration tests inspect.

The copy consists of every TableGen file of LLVM at its original location, of the TableGen files found in the build
directory within 'build' and of 'tablegen_compile_commands.yml', the
compilation commands LLVM's CMake generates, with all paths made relative to the root of the copy. This allows the
integration tests to work with all of LLVM's TableGen code without having to configure, or even download, LLVM.

Usage: updateLLVMMirror.py <llvm-project> <build directory> [<commit>]

The build directory has to have been configured with '-DLLVM_ENABLE_PROJECTS=clang;flang;mlir'. The commit of LLVM,
recorded in the copy for the reports of the tests, only has to be given if <llvm-project> is not a git repository.
"""
import re
import shutil
import subprocess
import sys
from pathlib import Path

COMPILE_COMMANDS = "tablegen_compile_commands.yml"


# Directory within the copy that mirrors the build directory.
BUILD = "build"


def relativize(path: str, source: Path, build: Path):
    """Returns 'path' relative to the root of the copy or None if it is not a path the copy can contain."""
    resolved = Path(path).resolve()
    if resolved.is_relative_to(build):
        return (BUILD / resolved.relative_to(build)).as_posix()
    if resolved.is_relative_to(source):
        return resolved.relative_to(source).as_posix()
    return None


def main():
    if len(sys.argv) not in (3, 4):
        sys.exit(__doc__)
    source, build = (Path(arg).resolve() for arg in sys.argv[1:3])
    commit = sys.argv[3] if len(sys.argv) == 4 else subprocess.check_output(
        ["git", "-C", source, "rev-parse", "HEAD"], text=True).strip()
    mirror = Path(__file__).resolve().parent / "testData" / "llvm-project"

    if mirror.exists():
        shutil.rmtree(mirror)
    mirror.mkdir(parents=True)

    count = 0
    # Build directories within LLVM, of which there may be more than just <build directory>, are not sources of LLVM.
    build_directories = {cache.parent for cache in source.rglob("CMakeCache.txt")} | {build}
    # A handful of TableGen files are generated when building LLVM, e.g. 'OmpCommon.td'. These are only part of the copy,
    # and includes of them only resolve, if the targets generating them have been built.
    for root, files in ((source, source.rglob("*.td")), (build, build.rglob("*.td"))):
        for file in sorted(files):
            if not file.is_file() or (root == source and any(file.is_relative_to(dir) for dir in build_directories)):
                continue
            # Running LLVM's tests leaves the TableGen files the tests split their inputs into in the build directory.
            if root == build and "Output" in file.relative_to(build).parts:
                continue
            target = mirror / relativize(str(file), source, build)
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(file, target)
            count += 1
    (mirror / "llvm-commit.txt").write_text(commit + "\n")
    # The license the files are distributed under.
    shutil.copyfile(source / "llvm" / "LICENSE.TXT", mirror / "LICENSE.TXT")

    # The file is a sequence of YAML documents of the following, very regular form:
    #   --- !FileInfo:
    #     filepath: "<path>"
    #     includes: "<path>;<path>;..."
    entries = 0
    with open(build / COMPILE_COMMANDS) as input, open(mirror / COMPILE_COMMANDS, "w") as output:
        filepath = None
        for line in input:
            if match := re.fullmatch(r'\s*filepath: "(.*)"\s*', line):
                filepath = relativize(match[1], source, build)
                # LLVM also runs TableGen on a few files that are not TableGen files and hence not part of the copy.
                if filepath is not None and not (mirror / filepath).is_file():
                    filepath = None
            elif match := re.fullmatch(r'\s*includes: "(.*)"\s*', line):
                if filepath is None:
                    continue
                includes = [relativize(include, source, build) for include in match[1].split(";") if include]
                # Directories without any TableGen files are not part of the copy.
                includes = [include for include in includes if include is not None and (mirror / include).is_dir()]
                output.write("--- !FileInfo:\n")
                output.write(f'  filepath: "{filepath}"\n')
                output.write(f'  includes: "{";".join(include for include in includes if include is not None)}"\n')
                entries += 1
                filepath = None

    print(f"Copied {count} TableGen files and {entries} compilation commands to {mirror}")


if __name__ == "__main__":
    main()
