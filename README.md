# tree-sitter-kotlin in Kotlin

[![GitHub link](https://img.shields.io/badge/GitHub-KotlinMania%2Ftree--sitter--kotlin-blue.svg)](https://github.com/KotlinMania/tree-sitter-kotlin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kotlinmania/tree-sitter-kotlin)](https://central.sonatype.com/artifact/io.github.kotlinmania/tree-sitter-kotlin)
[![Build status](https://img.shields.io/github/actions/workflow/status/KotlinMania/tree-sitter-kotlin/ci.yml?branch=main)](https://github.com/KotlinMania/tree-sitter-kotlin/actions)

This is a Kotlin Multiplatform line-by-line transliteration port of [`tree-sitter/tree-sitter`](https://github.com/tree-sitter/tree-sitter).

**Original Project:** This port is based on [`tree-sitter/tree-sitter`](https://github.com/tree-sitter/tree-sitter). All design credit and project intent belong to the upstream authors; this repository is a faithful port to Kotlin Multiplatform with no behavioural changes intended.

### Porting status

This is an **in-progress port**. The goal is feature parity with the upstream Rust crate while providing a native Kotlin Multiplatform API. Every Kotlin file carries a `// port-lint: source <path>` header naming its upstream Rust counterpart so the AST-distance tool can track provenance.

---

## Upstream README — `tree-sitter/tree-sitter`

> The text below is reproduced and lightly edited from [`https://github.com/tree-sitter/tree-sitter`](https://github.com/tree-sitter/tree-sitter). It is the upstream project's own description and remains under the upstream authors' authorship; links have been rewritten to absolute upstream URLs so they continue to resolve from this repository.

## tree-sitter

[![DOI](https://zenodo.org/badge/14164618.svg)](https://zenodo.org/badge/latestdoi/14164618)
[![discord][discord]](https://discord.gg/w7nTvsVJhm)
[![matrix][matrix]](https://matrix.to/#/#tree-sitter-chat:matrix.org)

Tree-sitter is a parser generator tool and an incremental parsing library. It can build a concrete syntax tree for a source file and efficiently update the syntax tree as the source file is edited. Tree-sitter aims to be:

- **General** enough to parse any programming language
- **Fast** enough to parse on every keystroke in a text editor
- **Robust** enough to provide useful results even in the presence of syntax errors
- **Dependency-free** so that the runtime library (which is written in pure C) can be embedded in any application

## Links
- [Documentation](https://tree-sitter.github.io)
- [Rust binding](https://github.com/tree-sitter/tree-sitter/blob/HEAD/lib/binding_rust/README.md)
- [Wasm binding](https://github.com/tree-sitter/tree-sitter/blob/HEAD/lib/binding_web/README.md)
- [Command-line interface](https://github.com/tree-sitter/tree-sitter/blob/HEAD/crates/cli/README.md)

[discord]: https://img.shields.io/discord/1063097320771698699?logo=discord&label=discord
[matrix]: https://img.shields.io/matrix/tree-sitter-chat%3Amatrix.org?logo=matrix&label=matrix

---

## About this Kotlin port

### Installation

```kotlin
dependencies {
    implementation("io.github.kotlinmania:tree-sitter-kotlin:0.1.0")
}
```

### Building

```bash
./gradlew build
./gradlew test
```

### Targets

- macOS arm64
- Linux x64
- Windows mingw-x64
- iOS arm64 / simulator-arm64 (Swift export + XCFramework)
- JS (browser + Node.js)
- Wasm-JS (browser + Node.js)
- Android (API 24+)

### Porting guidelines

See [AGENTS.md](AGENTS.md) and [CLAUDE.md](CLAUDE.md) for translator discipline, port-lint header convention, and Rust → Kotlin idiom mapping.

### License

This Kotlin port is distributed under the same MIT license as the upstream [`tree-sitter/tree-sitter`](https://github.com/tree-sitter/tree-sitter). See [LICENSE](LICENSE) (and any sibling `LICENSE-*` / `NOTICE` files mirrored from upstream) for the full text.

Original work copyrighted by the tree-sitter authors.  
Kotlin port: Copyright (c) 2026 Sydney Renee and The Solace Project.

### Acknowledgments

Thanks to the [`tree-sitter/tree-sitter`](https://github.com/tree-sitter/tree-sitter) maintainers and contributors for the original Rust implementation. This port reproduces their work in Kotlin Multiplatform; bug reports about upstream design or behavior should go to the upstream repository.
