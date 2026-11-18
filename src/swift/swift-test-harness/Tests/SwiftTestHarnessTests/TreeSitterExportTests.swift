import Testing
import TreeSitter

@Suite struct TreeSitterExportTests {
    @Test func testSwiftModuleLoads() throws {
        let parser = Parser()
        #expect(parser.language == nil)
        #expect(parser.includedRanges.isEmpty)
        let origin = Point.Companion.shared.MIN
        #expect(origin == Point.Companion.shared.MIN)
    }
}
