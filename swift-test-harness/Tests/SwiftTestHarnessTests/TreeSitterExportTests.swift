import Testing
import TreeSitter

@Suite struct TreeSitterExportTests {
    @Test func testSwiftModuleLoads() throws {
        let parser = Parser()
        #expect(parser.timeoutMicros == 0)
    }
}
