package io.github.kotlinmania.treesitter

import io.github.kotlinmania.regexsyntax.parser.ParserBuilder
import io.github.kotlinmania.treesitter.lib.TSFieldMapEntry
import io.github.kotlinmania.treesitter.lib.TSLanguage
import io.github.kotlinmania.treesitter.lib.TSLanguageMetadata
import io.github.kotlinmania.treesitter.lib.TSLexerMode
import io.github.kotlinmania.treesitter.lib.TSMapSlice
import io.github.kotlinmania.treesitter.lib.TSParseAction
import io.github.kotlinmania.treesitter.lib.TSParseActionEntry
import io.github.kotlinmania.treesitter.lib.TSSymbolMetadata
import io.github.kotlinmania.treesitter.util.CBufferIter
import io.github.kotlinmania.treesitter.wasmlanguage.WasmEngine
import io.github.kotlinmania.treesitter.wasmlanguage.WasmError
import io.github.kotlinmania.treesitter.wasmlanguage.WasmErrorKind
import io.github.kotlinmania.treesitter.wasmlanguage.WasmStore
import io.github.kotlinmania.treesitter.wasmlanguage.isWasm
import io.github.kotlinmania.treesitterlanguage.LanguageFn
import io.github.kotlinmania.treesitterlanguage.LanguageProvider
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalUnsignedTypes::class)
class LanguageTest {
    companion object {
        private data class SymbolSpec(
            val name: String,
            val visible: Boolean,
            val named: Boolean,
            val supertype: Boolean = false,
        )

        internal fun createRustFixtureTsLanguage(abiVersion: UInt = LANGUAGE_VERSION): TSLanguage {
            val literalSubtypes =
                listOf(
                    "boolean_literal",
                    "char_literal",
                    "float_literal",
                    "integer_literal",
                    "raw_string_literal",
                    "string_literal",
                )
            val patternNamedSubtypes =
                listOf(
                    "captured_pattern",
                    "const_block",
                    "generic_pattern",
                    "identifier",
                    "macro_invocation",
                    "mut_pattern",
                    "or_pattern",
                    "range_pattern",
                    "ref_pattern",
                    "reference_pattern",
                    "remaining_field_pattern",
                    "scoped_identifier",
                    "slice_pattern",
                    "struct_pattern",
                    "tuple_pattern",
                    "tuple_struct_pattern",
                )
            val typeNamedSubtypes =
                listOf(
                    "abstract_type",
                    "array_type",
                    "bounded_type",
                    "dynamic_type",
                    "function_type",
                    "generic_type",
                    "metavariable",
                    "never_type",
                    "pointer_type",
                    "primitive_type",
                    "reference_type",
                    "removed_trait_bound",
                    "scoped_type_identifier",
                    "tuple_type",
                    "type_identifier",
                    "unit_type",
                )

            val specs = mutableListOf<SymbolSpec>()
            // 0: end
            specs += SymbolSpec("end", visible = false, named = false)
            // Punctuation & comment tokens
            specs += SymbolSpec("//", visible = true, named = false)
            specs += SymbolSpec("/*", visible = true, named = false)
            specs += SymbolSpec("struct", visible = true, named = false)
            specs += SymbolSpec("=>", visible = true, named = false)
            specs += SymbolSpec("[", visible = true, named = false)
            specs += SymbolSpec("]", visible = true, named = false)
            specs += SymbolSpec("(", visible = true, named = false)
            specs += SymbolSpec(")", visible = true, named = false)
            specs += SymbolSpec("{", visible = true, named = false)
            specs += SymbolSpec("}", visible = true, named = false)
            specs += SymbolSpec("_", visible = true, named = false)
            // Named terminal tokens
            specs += SymbolSpec("identifier", visible = true, named = true)
            specs += SymbolSpec("line_comment", visible = true, named = true)
            specs += SymbolSpec("block_comment", visible = true, named = true)
            for (name in literalSubtypes) {
                specs += SymbolSpec(name, visible = true, named = true)
            }
            for (name in patternNamedSubtypes) {
                if (specs.none { it.name == name }) {
                    specs += SymbolSpec(name, visible = true, named = true)
                }
            }
            for (name in typeNamedSubtypes) {
                if (specs.none { it.name == name }) {
                    specs += SymbolSpec(name, visible = true, named = true)
                }
            }
            // Hidden/auxiliary scanner tokens checked in test_symbol_metadata_checks
            specs += SymbolSpec("_raw_string_literal_start", visible = false, named = false)
            specs += SymbolSpec("_raw_string_literal_end", visible = false, named = false)
            specs += SymbolSpec("_line_doc_comment", visible = false, named = false)
            specs += SymbolSpec("_error_sentinel", visible = false, named = false)

            val tokenCount = specs.size.toUInt()

            // Non-terminal rules & supertypes
            specs += SymbolSpec("source_file", visible = true, named = true)
            specs += SymbolSpec("struct_item", visible = true, named = true)
            specs += SymbolSpec("enum_item", visible = true, named = true)
            specs += SymbolSpec("type_item", visible = true, named = true)
            specs += SymbolSpec("_declaration_statement", visible = false, named = true, supertype = true)
            specs += SymbolSpec("_expression", visible = false, named = true, supertype = true)
            specs += SymbolSpec("_literal", visible = false, named = true, supertype = true)
            specs += SymbolSpec("_literal_pattern", visible = false, named = true, supertype = true)
            specs += SymbolSpec("_pattern", visible = false, named = true, supertype = true)
            specs += SymbolSpec("_type", visible = false, named = true, supertype = true)

            val symbolCount = specs.size.toUInt()
            val idByName = specs.withIndex().associate { (idx, spec) -> spec.name to idx.toUShort() }

            val supertypeNames =
                listOf(
                    "_expression",
                    "_literal",
                    "_literal_pattern",
                    "_pattern",
                    "_type",
                )
            val supertypeSymbols = UShortArray(supertypeNames.size) { idByName.getValue(supertypeNames[it]) }

            val subtypeEntries = mutableListOf<UShort>()
            val supertypeMapSlices = MutableList(symbolCount.toInt()) { TSMapSlice(0u, 0u) }

            fun registerSubtypes(supertypeName: String, childNames: List<String>) {
                val supId = idByName.getValue(supertypeName).toInt()
                val start = subtypeEntries.size.toUShort()
                for (child in childNames) {
                    subtypeEntries += idByName.getValue(child)
                }
                supertypeMapSlices[supId] = TSMapSlice(start, childNames.size.toUShort())
            }

            registerSubtypes("_expression", listOf("identifier", "macro_invocation"))
            registerSubtypes("_literal", literalSubtypes)
            registerSubtypes("_literal_pattern", literalSubtypes)
            registerSubtypes("_pattern", listOf("_", "_literal_pattern") + patternNamedSubtypes)
            registerSubtypes("_type", (typeNamedSubtypes + "macro_invocation").sorted())

            // Parse actions:
            // 0: empty header
            // 1..2: Shift to state 2 (after `struct` keyword)
            // 3..4: Shift extra (for comments)
            // 5..6: Shift to state 3 (after `identifier` in state 2)
            val parseActions =
                listOf(
                    TSParseActionEntry.Header(count = 0u, reusable = false),
                    TSParseActionEntry.Header(count = 1u, reusable = true),
                    TSParseActionEntry.Action(TSParseAction.Shift(state = 2u, extra = false, repetition = false)),
                    TSParseActionEntry.Header(count = 1u, reusable = true),
                    TSParseActionEntry.Action(TSParseAction.Shift(state = 0u, extra = true, repetition = false)),
                    TSParseActionEntry.Header(count = 1u, reusable = true),
                    TSParseActionEntry.Action(TSParseAction.Shift(state = 3u, extra = false, repetition = false)),
                )

            // States:
            // State 0 (large): initial state
            // State 1 (large): inside struct item before `struct` keyword -> shifting `struct` leads to state 2
            // State 2 (large): after `struct` keyword -> valid lookahead symbols:
            //   "//", "/*", "identifier", "line_comment", "block_comment"
            // State 3 (small): small parse state with valid lookahead symbols `{` and `}`
            val largeStateCount = 3u
            val stateCount = 8u
            val parseTable = UShortArray(largeStateCount.toInt() * symbolCount.toInt())

            fun setLargeEntry(state: Int, symbolName: String, value: UShort) {
                val sym = idByName.getValue(symbolName).toInt()
                parseTable[state * symbolCount.toInt() + sym] = value
            }

            // In state 1, `struct` shifts to state 2 (action index 1)
            setLargeEntry(1, "struct", 1u)
            // In state 2, valid lookaheads are "//", "/*", "identifier", "line_comment", "block_comment"
            setLargeEntry(2, "//", 3u)
            setLargeEntry(2, "/*", 3u)
            setLargeEntry(2, "identifier", 5u)
            setLargeEntry(2, "line_comment", 3u)
            setLargeEntry(2, "block_comment", 3u)

            // Small state 3 has 1 group with action index 3 and 2 symbols: `{` and `}`
            val smallParseTable =
                ushortArrayOf(
                    1u, // groupCount = 1
                    3u, // sectionValue = action index 3
                    2u, // symbolCount = 2
                    idByName.getValue("{"),
                    idByName.getValue("}"),
                    1u,
                    3u,
                    1u,
                    idByName.getValue("end"),
                    1u,
                    3u,
                    1u,
                    idByName.getValue("end"),
                    1u,
                    3u,
                    1u,
                    idByName.getValue("}"),
                    1u,
                    3u,
                    2u,
                    idByName.getValue("end"),
                    idByName.getValue("struct"),
                )
            val smallParseTableMap = uintArrayOf(0u, 5u, 9u, 13u, 17u)

            val fieldNames = listOf("", "body", "name", "type")

            return TSLanguage(
                abiVersion = abiVersion,
                symbolCount = symbolCount,
                aliasCount = 0u,
                tokenCount = tokenCount,
                externalTokenCount = 0u,
                stateCount = stateCount,
                largeStateCount = largeStateCount,
                productionIdCount = 1u,
                fieldCount = (fieldNames.size - 1).toUInt(),
                maxAliasSequenceLength = 0u,
                parseTable = parseTable,
                smallParseTable = smallParseTable,
                smallParseTableMap = smallParseTableMap,
                parseActions = parseActions,
                symbolNames = specs.map { it.name },
                fieldNames = fieldNames,
                fieldMapSlices = listOf(TSMapSlice(0u, 0u)),
                fieldMapEntries = emptyList<TSFieldMapEntry>(),
                symbolMetadata =
                    specs.map {
                        TSSymbolMetadata(
                            visible = it.visible,
                            named = it.named,
                            supertype = it.supertype,
                        )
                    },
                publicSymbolMap = UShortArray(symbolCount.toInt()) { it.toUShort() },
                aliasMap = UShortArray(0),
                aliasSequences = UShortArray(0),
                lexModes = List(stateCount.toInt()) { TSLexerMode(0u, 0u, 0u) },
                lexFn = { _, _ -> false },
                keywordLexFn = { _, _ -> false },
                keywordCaptureToken = 0u,
                externalScanner = null,
                primaryStateIds = UShortArray(stateCount.toInt()) { it.toUShort() },
                name = "rust",
                reservedWords = UShortArray(0),
                maxReservedWordSetSize = 0u,
                supertypeCount = supertypeSymbols.size.toUInt(),
                supertypeSymbols = supertypeSymbols,
                supertypeMapSlices = supertypeMapSlices,
                supertypeMapEntries = subtypeEntries.toUShortArray(),
                metadata = TSLanguageMetadata(0u, 23u, 2u),
            )
        }

        internal fun getLanguage(): Language {
            val languageFn = registerLanguageFn(createRustFixtureTsLanguage())
            return Language(languageFn)
        }
    }

    @Test
    fun testLanguageFnAndUnifiedLanguageConstruction() {
        val tsLanguage = createRustFixtureTsLanguage()
        val languageFn: LanguageFn = registerLanguageFn(tsLanguage)

        val viaTopLevelFn = Language(languageFn)
        val viaExtension = languageFn.toLanguage()
        val viaAnyConstructor = Language(languageFn as Any)
        val viaProvider = Language(languageFn.intoRaw())
        val viaTsLanguage = Language(tsLanguage)
        val viaCopy = viaTopLevelFn.copy()

        assertEquals(viaTopLevelFn, viaExtension)
        assertEquals(viaTopLevelFn, viaAnyConstructor)
        assertEquals(viaTopLevelFn, viaProvider)
        assertEquals(viaTopLevelFn, viaTsLanguage)
        assertEquals(viaTopLevelFn, viaCopy)
        assertEquals(viaTopLevelFn.hashCode(), viaCopy.hashCode())

        assertEquals(15u, viaTopLevelFn.abiVersion)
        assertEquals("rust", viaTopLevelFn.name)
        val metadata = assertNotNull(viaTopLevelFn.metadata)
        assertEquals(Triple<UShort, UShort, UShort>(0u, 23u, 2u), metadata.semanticVersion)
        assertEquals("Metadata(semanticVersion=\"0.23.2\")", metadata.toString())

        assertEquals(3u, viaTopLevelFn.fieldCount)
        assertNull(viaTopLevelFn.fieldNameForId(0u))
        assertEquals("body", viaTopLevelFn.fieldNameForId(1u))
        assertEquals("name", viaTopLevelFn.fieldNameForId(2u))
        assertEquals("type", viaTopLevelFn.fieldNameForId(3u))
        assertNull(viaTopLevelFn.fieldNameForId(4u))
        assertEquals(2u, viaTopLevelFn.fieldIdForName("name"))
        assertEquals(0u, viaTopLevelFn.fieldIdForName("nonexistent"))

        assertFailsWith<IllegalArgumentException> {
            Language(0L)
        }
        assertFailsWith<IllegalArgumentException> {
            Language(-1L)
        }
        assertFailsWith<IllegalArgumentException> {
            Language("not-a-language")
        }
        assertFailsWith<IllegalArgumentException> {
            Language(LanguageFn.fromRaw(LanguageProvider { 0L }))
        }
        assertFailsWith<IllegalArgumentException> {
            Language(createRustFixtureTsLanguage(abiVersion = 12u))
        }
        assertFailsWith<IllegalArgumentException> {
            Language(createRustFixtureTsLanguage(abiVersion = 16u))
        }
    }

    @Test
    fun testLookaheadIterator() {
        val language = getLanguage()

        val structSymbol = language.idForNodeKind("struct", named = false)
        assertNotEquals(0u, structSymbol)

        val nextState = language.nextState(1u, structSymbol)
        assertNotEquals(0u, nextState)
        assertTrue(nextState.toUInt() < language.parseStateCount())

        val expectedSymbols = listOf("//", "/*", "identifier", "line_comment", "block_comment")
        val lookahead = language.lookaheadIterator(nextState)
        assertEquals(language, lookahead.language)
        assertEquals("ERROR", lookahead.currentSymbolName)
        assertEquals(UShort.MAX_VALUE, lookahead.currentSymbol)
        assertEquals(expectedSymbols, lookahead.symbolNames().toList())

        assertTrue(lookahead.reset(nextState))
        assertEquals(expectedSymbols, lookahead.symbolNames().toList())

        assertTrue(lookahead.reset(nextState, language))
        assertEquals(
            expectedSymbols,
            lookahead.asSequence().map { language.nodeKindForId(it.id)!! }.toList(),
        )

        // Small parse state (state 3) lookahead verification
        val smallLookahead = language.lookaheadIterator(3u)
        assertEquals(listOf("{", "}"), smallLookahead.symbolNames().toList())

        // Out-of-range parse state must fail
        assertFalse(lookahead.reset(999u))
        assertFailsWith<IllegalArgumentException> {
            language.lookaheadIterator(999u)
        }
    }

    @Test
    fun testLookaheadIteratorModifiableOnlyByMut() {
        val language = getLanguage()
        val structSymbol = language.idForNodeKind("struct", named = false)
        val nextState = language.nextState(1u, structSymbol)
        assertNotEquals(0u, nextState)

        val lookahead = language.lookaheadIterator(nextState)
        val firstSymbol = lookahead.next()
        assertEquals("//", firstSymbol.name)

        val names = lookahead.symbolNames().iterator()
        assertEquals("//", names.next())
    }

    @Test
    fun testSymbolMetadataChecks() {
        val language = getLanguage()
        for (i in 0u until language.nodeKindCount()) {
            val sym = i.toUShort()
            val name = assertNotNull(language.nodeKindForId(sym))
            when (name) {
                "_type",
                "_expression",
                "_pattern",
                "_literal",
                "_literal_pattern",
                "_declaration_statement",
                -> {
                    assertTrue(language.nodeKindIsSupertype(sym), "Expected $name to be supertype")
                }

                "_raw_string_literal_start",
                "_raw_string_literal_end",
                "_line_doc_comment",
                "_error_sentinel",
                -> {
                    assertFalse(language.nodeKindIsSupertype(sym), "Expected $name not to be supertype")
                }

                "enum_item", "struct_item", "type_item" -> {
                    assertTrue(language.nodeKindIsNamed(sym), "Expected $name to be named")
                }

                "=>", "[", "]", "(", ")", "{", "}" -> {
                    assertTrue(language.nodeKindIsVisible(sym), "Expected $name to be visible")
                }
            }
        }
    }

    @Test
    fun testSupertypes() {
        val language = getLanguage()
        val supertypes = language.supertypes

        assertEquals(5, supertypes.size)
        assertEquals(
            listOf(
                "_expression",
                "_literal",
                "_literal_pattern",
                "_pattern",
                "_type",
            ),
            supertypes.mapNotNull { language.nodeKindForId(it) },
        )

        for (supertype in supertypes) {
            val subtypes =
                language
                    .subtypesForSupertype(supertype)
                    .mapNotNull { language.nodeKindForId(it) }
                    .distinct()
                    .sorted()

            when (language.nodeKindForId(supertype)) {
                "_literal" -> {
                    assertEquals(
                        listOf(
                            "boolean_literal",
                            "char_literal",
                            "float_literal",
                            "integer_literal",
                            "raw_string_literal",
                            "string_literal",
                        ),
                        subtypes,
                    )
                }

                "_pattern" -> {
                    assertEquals(
                        listOf(
                            "_",
                            "_literal_pattern",
                            "captured_pattern",
                            "const_block",
                            "generic_pattern",
                            "identifier",
                            "macro_invocation",
                            "mut_pattern",
                            "or_pattern",
                            "range_pattern",
                            "ref_pattern",
                            "reference_pattern",
                            "remaining_field_pattern",
                            "scoped_identifier",
                            "slice_pattern",
                            "struct_pattern",
                            "tuple_pattern",
                            "tuple_struct_pattern",
                        ),
                        subtypes,
                    )
                }

                "_type" -> {
                    assertEquals(
                        listOf(
                            "abstract_type",
                            "array_type",
                            "bounded_type",
                            "dynamic_type",
                            "function_type",
                            "generic_type",
                            "macro_invocation",
                            "metavariable",
                            "never_type",
                            "pointer_type",
                            "primitive_type",
                            "reference_type",
                            "removed_trait_bound",
                            "scoped_type_identifier",
                            "tuple_type",
                            "type_identifier",
                            "unit_type",
                        ),
                        subtypes,
                    )
                }
            }
        }
    }

    @Test
    fun testRegexSyntaxAndQueryPredicateMatch() {
        val regex = QueryPredicate.Match.compilePattern(0u, "^[A-Z][a-z0-9_]*$")
        val predicate =
            QueryPredicate.Match(
                name = "match?",
                capture = "type",
                pattern = regex,
                isPositive = true,
                isAny = false,
            )
        assertEquals(
            listOf(
                QueryPredicateArg.Capture("type"),
                QueryPredicateArg.Literal("^[A-Z][a-z0-9_]*$"),
            ),
            predicate.args,
        )
        assertTrue(regex.containsMatchIn("Stuff"))
        assertFalse(regex.containsMatchIn("stuff"))

        // Invalid regex pattern is rejected via regex-syntax-kotlin without try/catch
        assertFailsWith<QueryError.Predicate> {
            QueryPredicate.Match.compilePattern(3u, "[unclosed")
        }

        // Empty capture list returns !isPositive
        val emptyMatch = QueryMatch(0u, emptyList())
        assertFalse(predicate(emptyMatch))

        val negativePredicate =
            QueryPredicate.Match(
                name = "not-match?",
                capture = "type",
                pattern = regex,
                isPositive = false,
                isAny = false,
            )
        assertTrue(negativePredicate(emptyMatch))

        // Verify regex-syntax-kotlin ParserBuilder integration
        val regexBuilder = ParserBuilder().utf8(false)
        val validParse = regexBuilder.build().parse("^[A-Z][a-z0-9_]*$")
        val invalidParse = regexBuilder.build().parse("[unclosed")
        assertTrue(validParse.isSuccess)
        assertTrue(invalidParse.isFailure)
    }

    @Test
    fun testLossyUtf8AndCBufferIter() {
        val validBytes = "hello world".encodeToByteArray()
        assertEquals(listOf("hello world"), LossyUtf8(validBytes).asSequence().toList())

        val mixedBytes = byteArrayOf('h'.code.toByte(), 'i'.code.toByte(), 0xFF.toByte(), '!'.code.toByte())
        assertEquals(listOf("hi", "\uFFFD", "!"), LossyUtf8(mixedBytes).asSequence().toList())

        var released = false
        val iter =
            CBufferIter.new(listOf(10, 20, 30)) {
                released = true
            }
        assertEquals(3, iter.count)
        assertEquals(3 to 3, iter.sizeHint())
        assertEquals(10, iter.next())
        assertEquals(2 to 2, iter.sizeHint())
        iter.close()
        assertTrue(released)
        assertFalse(iter.hasNext())
        assertContentEquals(emptyList(), iter.asSequence().toList())
    }

    @Test
    fun testWasmLanguage() {
        val engine = WasmEngine.new()
        val store = WasmStore.new(engine)
        assertEquals(0, store.languageCount())
        assertEquals(0, store.languageCount)

        val err =
            assertFailsWith<WasmError> {
                store.loadLanguage("test", byteArrayOf(0, 1, 2))
            }
        assertEquals(WasmErrorKind.Instantiate, err.kind)
        assertTrue(err.toString().startsWith("Failed to instantiate wasm module:"))

        val language = getLanguage()
        assertFalse(language.isWasm())

        store.close()
    }
}
