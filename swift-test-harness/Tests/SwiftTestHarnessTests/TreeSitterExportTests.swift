import XCTest
import TreeSitter

// Smoke test for the Kotlin → Swift Export → SPM → swift test pipeline.
//
// The file's mere existence and successful compilation prove three layers
// of the pipeline:
//
//   1. `embedSwiftExportForXcode` produced `TreeSitter.swiftmodule/`
//      and the supporting KotlinRuntimeSupport / ExportedKotlinPackages /
//      KotlinRuntime swiftmodule bundles. If any of them were missing,
//      `import TreeSitter` above would fail at compile time.
//
//   2. The static archive `libTreeSitter.a` (produced by the
//      `linkSwiftExportBinaryDebugStaticMacosArm64` and
//      `mergeMacosDebugSwiftExportLibraries` tasks) supplied every
//      `__root____*` and `KotlinError`-related symbol the Swift modules
//      reference. If the archive were missing or empty, this test
//      executable would fail to link with "undefined symbols for
//      architecture arm64".
//
//   3. The Kotlin `swiftExport { moduleName = "TreeSitter" }` and
//      `flattenPackage = "io.github.kotlinmania.treesitter"` configuration
//      in build.gradle.kts produced a module name that's both
//      syntactically valid as a Swift identifier and reachable from this
//      Package.swift via the `TreeSitterLibrary` product.
final class TreeSitterExportTests: XCTestCase {
    func testSwiftModuleLoads() throws {
        XCTAssertTrue(true, "TreeSitter swift module imported cleanly")
    }
}
