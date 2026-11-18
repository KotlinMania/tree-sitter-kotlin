package io.github.kotlinmania.treesitter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ParserTest {
    @Test
    fun testParseSimpleStruct() {
        val parser = Parser()
        val language = LanguageTest.getLanguage()
        parser.language = language
        assertEquals(language, parser.language)

        val code = "struct Foo {}"
        val tree = parser.parse(code)
        assertNotNull(tree)
        assertEquals(language, tree.language)

        val root = tree.rootNode
        assertNotNull(root)
        assertEquals("source_file", root.type)
        assertFalse(root.hasError)
        assertTrue(root.childCount >= 1u)

        val structItem = root.child(0u)
        assertNotNull(structItem)
        assertEquals("struct_item", structItem.type)
        assertFalse(structItem.hasError)

        val nameNode = structItem.childByFieldName("name")
        if (nameNode != null) {
            assertEquals("identifier", nameNode.type)
            assertFalse(nameNode.hasError)
        }

        val cursor = tree.walk()
        assertEquals("source_file", cursor.currentNode.type)
        assertTrue(cursor.gotoFirstChild())
        assertEquals("struct_item", cursor.currentNode.type)
        assertTrue(cursor.gotoParent())
        assertEquals("source_file", cursor.currentNode.type)

        val copyTree = tree.copy()
        assertEquals(tree.rootNode.type, copyTree.rootNode.type)

        parser.reset()
    }

    @Test
    fun testTreeEditingAndIncrementalParsing() {
        val parser = Parser()
        val language = LanguageTest.getLanguage()
        parser.language = language

        val code1 = "struct Foo {}"
        val tree1 = parser.parse(code1)
        assertNotNull(tree1)

        val edit =
            InputEdit(
                startByte = 11u,
                oldEndByte = 11u,
                newEndByte = 12u,
                startPoint = Point(0u, 11u),
                oldEndPoint = Point(0u, 11u),
                newEndPoint = Point(0u, 12u),
            )
        tree1.edit(edit)

        val code2 = "struct Foo { }"
        val tree2 = parser.parse(code2, oldTree = tree1)
        assertNotNull(tree2)
        assertEquals("source_file", tree2.rootNode.type)
    }

    @Test
    fun testNodePositionsAndRanges() {
        val parser = Parser()
        val language = LanguageTest.getLanguage()
        parser.language = language

        val code = "struct Foo {}"
        val tree = parser.parse(code)
        val root = tree.rootNode
        assertEquals(0u, root.startByte)
        assertEquals(code.length.toUInt(), root.endByte)
        assertEquals(0u, root.startPoint.row)
        assertEquals(0u, root.startPoint.column)
        assertEquals(0u, root.range.startPoint.row)
    }

    @Test
    fun testParserReadCallback() {
        val parser = Parser()
        val language = LanguageTest.getLanguage()
        parser.language = language

        val chunks = listOf("struct ", "Foo ", "{}")
        var index = 0
        val tree =
            parser.parse(
                readCallback = { _, _ ->
                    if (index < chunks.size) chunks[index++] else null
                },
            )
        assertNotNull(tree)
        assertEquals("source_file", tree.rootNode.type)
    }
}
