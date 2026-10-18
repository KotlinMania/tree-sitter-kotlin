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
}
