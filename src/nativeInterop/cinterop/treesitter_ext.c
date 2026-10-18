#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>
#include <string.h>
#include "tree_sitter/api.h"
#include "parser.h"

enum {
    KTS_SYM_END = 0,
    KTS_SYM_SLASH_SLASH = 1,
    KTS_SYM_SLASH_STAR = 2,
    KTS_SYM_STRUCT = 3,
    KTS_SYM_FAT_ARROW = 4,
    KTS_SYM_LBRACK = 5,
    KTS_SYM_RBRACK = 6,
    KTS_SYM_LPAREN = 7,
    KTS_SYM_RPAREN = 8,
    KTS_SYM_LBRACE = 9,
    KTS_SYM_RBRACE = 10,
    KTS_SYM_UNDERSCORE = 11,
    KTS_SYM_IDENTIFIER = 12,
    KTS_SYM_LINE_COMMENT = 13,
    KTS_SYM_BLOCK_COMMENT = 14,
    KTS_SYM_BOOLEAN_LITERAL = 15,
    KTS_SYM_CHAR_LITERAL = 16,
    KTS_SYM_FLOAT_LITERAL = 17,
    KTS_SYM_INTEGER_LITERAL = 18,
    KTS_SYM_RAW_STRING_LITERAL = 19,
    KTS_SYM_STRING_LITERAL = 20,
    KTS_SYM_CAPTURED_PATTERN = 21,
    KTS_SYM_CONST_BLOCK = 22,
    KTS_SYM_GENERIC_PATTERN = 23,
    KTS_SYM_MACRO_INVOCATION = 24,
    KTS_SYM_MUT_PATTERN = 25,
    KTS_SYM_OR_PATTERN = 26,
    KTS_SYM_RANGE_PATTERN = 27,
    KTS_SYM_REF_PATTERN = 28,
    KTS_SYM_REFERENCE_PATTERN = 29,
    KTS_SYM_REMAINING_FIELD_PATTERN = 30,
    KTS_SYM_SCOPED_IDENTIFIER = 31,
    KTS_SYM_SLICE_PATTERN = 32,
    KTS_SYM_STRUCT_PATTERN = 33,
    KTS_SYM_TUPLE_PATTERN = 34,
    KTS_SYM_TUPLE_STRUCT_PATTERN = 35,
    KTS_SYM_ABSTRACT_TYPE = 36,
    KTS_SYM_ARRAY_TYPE = 37,
    KTS_SYM_BOUNDED_TYPE = 38,
    KTS_SYM_DYNAMIC_TYPE = 39,
    KTS_SYM_FUNCTION_TYPE = 40,
    KTS_SYM_GENERIC_TYPE = 41,
    KTS_SYM_METAVARIABLE = 42,
    KTS_SYM_NEVER_TYPE = 43,
    KTS_SYM_POINTER_TYPE = 44,
    KTS_SYM_PRIMITIVE_TYPE = 45,
    KTS_SYM_REFERENCE_TYPE = 46,
    KTS_SYM_REMOVED_TRAIT_BOUND = 47,
    KTS_SYM_SCOPED_TYPE_IDENTIFIER = 48,
    KTS_SYM_TUPLE_TYPE = 49,
    KTS_SYM_TYPE_IDENTIFIER = 50,
    KTS_SYM_UNIT_TYPE = 51,
    KTS_SYM_RAW_STRING_LITERAL_START = 52,
    KTS_SYM_RAW_STRING_LITERAL_END = 53,
    KTS_SYM_LINE_DOC_COMMENT = 54,
    KTS_SYM_ERROR_SENTINEL = 55,
    KTS_SYM_SOURCE_FILE = 56,
    KTS_SYM_STRUCT_ITEM = 57,
    KTS_SYM_ENUM_ITEM = 58,
    KTS_SYM_TYPE_ITEM = 59,
    KTS_SYM_DECLARATION_STATEMENT = 60,
    KTS_SYM_EXPRESSION = 61,
    KTS_SYM_LITERAL = 62,
    KTS_SYM_LITERAL_PATTERN = 63,
    KTS_SYM_PATTERN = 64,
    KTS_SYM_TYPE = 65,
    KTS_TOKEN_COUNT = 56,
    KTS_SYMBOL_COUNT = 66,
    KTS_LARGE_STATE_COUNT = 3,
    KTS_STATE_COUNT = 8
};

static const char * const kts_symbol_names[KTS_SYMBOL_COUNT] = {
    "end",
    "//",
    "/*",
    "struct",
    "=>",
    "[",
    "]",
    "(",
    ")",
    "{",
    "}",
    "_",
    "identifier",
    "line_comment",
    "block_comment",
    "boolean_literal",
    "char_literal",
    "float_literal",
    "integer_literal",
    "raw_string_literal",
    "string_literal",
    "captured_pattern",
    "const_block",
    "generic_pattern",
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
    "_raw_string_literal_start",
    "_raw_string_literal_end",
    "_line_doc_comment",
    "_error_sentinel",
    "source_file",
    "struct_item",
    "enum_item",
    "type_item",
    "_declaration_statement",
    "_expression",
    "_literal",
    "_literal_pattern",
    "_pattern",
    "_type",
};

static const TSSymbolMetadata kts_symbol_metadata[KTS_SYMBOL_COUNT] = {
    [KTS_SYM_END] = { .visible = false, .named = false, .supertype = false },
    [KTS_SYM_SLASH_SLASH] = { .visible = true, .named = false, .supertype = false },
    [KTS_SYM_SLASH_STAR] = { .visible = true, .named = false, .supertype = false },
    [KTS_SYM_STRUCT] = { .visible = true, .named = false, .supertype = false },
    [KTS_SYM_FAT_ARROW] = { .visible = true, .named = false, .supertype = false },
    [KTS_SYM_LBRACK] = { .visible = true, .named = false, .supertype = false },
    [KTS_SYM_RBRACK] = { .visible = true, .named = false, .supertype = false },
    [KTS_SYM_LPAREN] = { .visible = true, .named = false, .supertype = false },
    [KTS_SYM_RPAREN] = { .visible = true, .named = false, .supertype = false },
    [KTS_SYM_LBRACE] = { .visible = true, .named = false, .supertype = false },
    [KTS_SYM_RBRACE] = { .visible = true, .named = false, .supertype = false },
    [KTS_SYM_UNDERSCORE] = { .visible = true, .named = false, .supertype = false },
    [KTS_SYM_IDENTIFIER] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_LINE_COMMENT] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_BLOCK_COMMENT] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_BOOLEAN_LITERAL] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_CHAR_LITERAL] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_FLOAT_LITERAL] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_INTEGER_LITERAL] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_RAW_STRING_LITERAL] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_STRING_LITERAL] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_CAPTURED_PATTERN] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_CONST_BLOCK] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_GENERIC_PATTERN] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_MACRO_INVOCATION] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_MUT_PATTERN] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_OR_PATTERN] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_RANGE_PATTERN] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_REF_PATTERN] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_REFERENCE_PATTERN] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_REMAINING_FIELD_PATTERN] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_SCOPED_IDENTIFIER] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_SLICE_PATTERN] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_STRUCT_PATTERN] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_TUPLE_PATTERN] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_TUPLE_STRUCT_PATTERN] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_ABSTRACT_TYPE] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_ARRAY_TYPE] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_BOUNDED_TYPE] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_DYNAMIC_TYPE] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_FUNCTION_TYPE] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_GENERIC_TYPE] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_METAVARIABLE] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_NEVER_TYPE] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_POINTER_TYPE] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_PRIMITIVE_TYPE] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_REFERENCE_TYPE] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_REMOVED_TRAIT_BOUND] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_SCOPED_TYPE_IDENTIFIER] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_TUPLE_TYPE] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_TYPE_IDENTIFIER] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_UNIT_TYPE] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_RAW_STRING_LITERAL_START] = { .visible = false, .named = false, .supertype = false },
    [KTS_SYM_RAW_STRING_LITERAL_END] = { .visible = false, .named = false, .supertype = false },
    [KTS_SYM_LINE_DOC_COMMENT] = { .visible = false, .named = false, .supertype = false },
    [KTS_SYM_ERROR_SENTINEL] = { .visible = false, .named = false, .supertype = false },
    [KTS_SYM_SOURCE_FILE] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_STRUCT_ITEM] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_ENUM_ITEM] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_TYPE_ITEM] = { .visible = true, .named = true, .supertype = false },
    [KTS_SYM_DECLARATION_STATEMENT] = { .visible = false, .named = true, .supertype = true },
    [KTS_SYM_EXPRESSION] = { .visible = false, .named = true, .supertype = true },
    [KTS_SYM_LITERAL] = { .visible = false, .named = true, .supertype = true },
    [KTS_SYM_LITERAL_PATTERN] = { .visible = false, .named = true, .supertype = true },
    [KTS_SYM_PATTERN] = { .visible = false, .named = true, .supertype = true },
    [KTS_SYM_TYPE] = { .visible = false, .named = true, .supertype = true },
};

static const TSSymbol kts_public_symbol_map[KTS_SYMBOL_COUNT] = {
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
    20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
    40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59,
    60, 61, 62, 63, 64, 65
};

static const char * const kts_field_names[4] = {
    NULL,
    "body",
    "name",
    "type",
};

static const TSMapSlice kts_field_map_slices[2] = {
    [0] = { .index = 0, .length = 0 },
    [1] = { .index = 0, .length = 1 },
};

static const TSFieldMapEntry kts_field_map_entries[1] = {
    [0] = { .field_id = 2, .child_index = 1, .inherited = false },
};

static const TSSymbol kts_supertype_symbols[5] = {
    KTS_SYM_EXPRESSION,
    KTS_SYM_LITERAL,
    KTS_SYM_LITERAL_PATTERN,
    KTS_SYM_PATTERN,
    KTS_SYM_TYPE,
};

static const TSSymbol kts_supertype_map_entries[49] = {
    /* _expression (0..1, len 2) */
    KTS_SYM_IDENTIFIER,
    KTS_SYM_MACRO_INVOCATION,
    /* _literal (2..7, len 6) */
    KTS_SYM_BOOLEAN_LITERAL,
    KTS_SYM_CHAR_LITERAL,
    KTS_SYM_FLOAT_LITERAL,
    KTS_SYM_INTEGER_LITERAL,
    KTS_SYM_RAW_STRING_LITERAL,
    KTS_SYM_STRING_LITERAL,
    /* _literal_pattern (8..13, len 6) */
    KTS_SYM_BOOLEAN_LITERAL,
    KTS_SYM_CHAR_LITERAL,
    KTS_SYM_FLOAT_LITERAL,
    KTS_SYM_INTEGER_LITERAL,
    KTS_SYM_RAW_STRING_LITERAL,
    KTS_SYM_STRING_LITERAL,
    /* _pattern (14..31, len 18) */
    KTS_SYM_UNDERSCORE,
    KTS_SYM_LITERAL_PATTERN,
    KTS_SYM_CAPTURED_PATTERN,
    KTS_SYM_CONST_BLOCK,
    KTS_SYM_GENERIC_PATTERN,
    KTS_SYM_IDENTIFIER,
    KTS_SYM_MACRO_INVOCATION,
    KTS_SYM_MUT_PATTERN,
    KTS_SYM_OR_PATTERN,
    KTS_SYM_RANGE_PATTERN,
    KTS_SYM_REF_PATTERN,
    KTS_SYM_REFERENCE_PATTERN,
    KTS_SYM_REMAINING_FIELD_PATTERN,
    KTS_SYM_SCOPED_IDENTIFIER,
    KTS_SYM_SLICE_PATTERN,
    KTS_SYM_STRUCT_PATTERN,
    KTS_SYM_TUPLE_PATTERN,
    KTS_SYM_TUPLE_STRUCT_PATTERN,
    /* _type (32..48, len 17) */
    KTS_SYM_ABSTRACT_TYPE,
    KTS_SYM_ARRAY_TYPE,
    KTS_SYM_BOUNDED_TYPE,
    KTS_SYM_DYNAMIC_TYPE,
    KTS_SYM_FUNCTION_TYPE,
    KTS_SYM_GENERIC_TYPE,
    KTS_SYM_MACRO_INVOCATION,
    KTS_SYM_METAVARIABLE,
    KTS_SYM_NEVER_TYPE,
    KTS_SYM_POINTER_TYPE,
    KTS_SYM_PRIMITIVE_TYPE,
    KTS_SYM_REFERENCE_TYPE,
    KTS_SYM_REMOVED_TRAIT_BOUND,
    KTS_SYM_SCOPED_TYPE_IDENTIFIER,
    KTS_SYM_TUPLE_TYPE,
    KTS_SYM_TYPE_IDENTIFIER,
    KTS_SYM_UNIT_TYPE,
};

static const TSMapSlice kts_supertype_map_slices[KTS_SYMBOL_COUNT] = {
    [KTS_SYM_EXPRESSION] = { .index = 0, .length = 2 },
    [KTS_SYM_LITERAL] = { .index = 2, .length = 6 },
    [KTS_SYM_LITERAL_PATTERN] = { .index = 8, .length = 6 },
    [KTS_SYM_PATTERN] = { .index = 14, .length = 18 },
    [KTS_SYM_TYPE] = { .index = 32, .length = 17 },
};

static const TSParseActionEntry kts_parse_actions[] = {
    [0] = { .entry = { .count = 0, .reusable = false } },
    [1] = { .entry = { .count = 1, .reusable = true } },
    [2] = SHIFT(2),
    [3] = { .entry = { .count = 1, .reusable = true } },
    [4] = SHIFT_EXTRA(),
    [5] = { .entry = { .count = 1, .reusable = true } },
    [6] = SHIFT(3),
    [7] = { .entry = { .count = 1, .reusable = true } },
    [8] = SHIFT(6),
    [9] = { .entry = { .count = 1, .reusable = true } },
    [10] = ACCEPT_INPUT(),
    [11] = { .entry = { .count = 1, .reusable = true } },
    [12] = REDUCE(KTS_SYM_SOURCE_FILE, 1, 0, 0),
    [13] = { .entry = { .count = 1, .reusable = true } },
    [14] = SHIFT(7),
    [15] = { .entry = { .count = 1, .reusable = true } },
    [16] = REDUCE(KTS_SYM_STRUCT_ITEM, 4, 0, 1),
    [17] = { .entry = { .count = 1, .reusable = true } },
    [18] = REDUCE(KTS_SYM_SOURCE_FILE, 0, 0, 0),
};

static const uint16_t kts_parse_table[KTS_LARGE_STATE_COUNT][KTS_SYMBOL_COUNT] = {
    [0] = { 0 },
    [1] = {
        [KTS_SYM_END] = 17,
        [KTS_SYM_STRUCT] = 1,
        [KTS_SYM_LINE_COMMENT] = 3,
        [KTS_SYM_BLOCK_COMMENT] = 3,
        [KTS_SYM_SOURCE_FILE] = 4,
        [KTS_SYM_STRUCT_ITEM] = 5,
    },
    [2] = {
        [KTS_SYM_SLASH_SLASH] = 3,
        [KTS_SYM_SLASH_STAR] = 3,
        [KTS_SYM_IDENTIFIER] = 5,
        [KTS_SYM_LINE_COMMENT] = 3,
        [KTS_SYM_BLOCK_COMMENT] = 3,
    },
};

static const uint16_t kts_small_parse_table[] = {
    /* [0] State 3: small state with '{' and '}' */
    1, 7, 2, KTS_SYM_LBRACE, KTS_SYM_RBRACE,
    /* [5] State 4: after source_file -> Accept on EOF */
    1, 9, 1, KTS_SYM_END,
    /* [9] State 5: after struct_item -> Reduce source_file(1) on EOF */
    1, 11, 1, KTS_SYM_END,
    /* [13] State 6: after '{' -> Shift 7 on '}' */
    1, 13, 1, KTS_SYM_RBRACE,
    /* [17] State 7: after '}' -> Reduce struct_item(4, prod 1) on EOF or 'struct' */
    1, 15, 2, KTS_SYM_END, KTS_SYM_STRUCT,
};

static const uint32_t kts_small_parse_table_map[KTS_STATE_COUNT - KTS_LARGE_STATE_COUNT] = {
    0,
    5,
    9,
    13,
    17,
};

static const TSLexerMode kts_lex_modes[KTS_STATE_COUNT] = {
    [0] = { .lex_state = 0, .external_lex_state = 0, .reserved_word_set_id = 0 },
    [1] = { .lex_state = 0, .external_lex_state = 0, .reserved_word_set_id = 0 },
    [2] = { .lex_state = 0, .external_lex_state = 0, .reserved_word_set_id = 0 },
    [3] = { .lex_state = 0, .external_lex_state = 0, .reserved_word_set_id = 0 },
    [4] = { .lex_state = 0, .external_lex_state = 0, .reserved_word_set_id = 0 },
    [5] = { .lex_state = 0, .external_lex_state = 0, .reserved_word_set_id = 0 },
    [6] = { .lex_state = 0, .external_lex_state = 0, .reserved_word_set_id = 0 },
    [7] = { .lex_state = 0, .external_lex_state = 0, .reserved_word_set_id = 0 },
};

static const TSStateId kts_primary_state_ids[KTS_STATE_COUNT] = {
    0, 1, 2, 3, 4, 5, 6, 7
};

static const TSSymbol kts_alias_map[1] = { 0 };
static const TSSymbol kts_alias_sequences[1] = { 0 };
static const TSSymbol kts_reserved_words[1] = { 0 };

static bool kts_is_ident_start(int32_t c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
}

static bool kts_is_ident_cont(int32_t c) {
    return kts_is_ident_start(c) || (c >= '0' && c <= '9');
}

static bool kts_rust_lex(TSLexer *lexer, TSStateId state) {
    (void)state;
    while (lexer->lookahead == ' ' || lexer->lookahead == '\t' ||
           lexer->lookahead == '\r' || lexer->lookahead == '\n') {
        lexer->advance(lexer, true);
    }

    if (lexer->eof(lexer)) {
        lexer->mark_end(lexer);
        lexer->result_symbol = KTS_SYM_END;
        return true;
    }

    if (lexer->lookahead == '{') {
        lexer->advance(lexer, false);
        lexer->mark_end(lexer);
        lexer->result_symbol = KTS_SYM_LBRACE;
        return true;
    }

    if (lexer->lookahead == '}') {
        lexer->advance(lexer, false);
        lexer->mark_end(lexer);
        lexer->result_symbol = KTS_SYM_RBRACE;
        return true;
    }

    if (lexer->lookahead == '/') {
        lexer->advance(lexer, false);
        if (lexer->lookahead == '/') {
            lexer->advance(lexer, false);
            while (!lexer->eof(lexer) && lexer->lookahead != '\n') {
                lexer->advance(lexer, false);
            }
            lexer->mark_end(lexer);
            lexer->result_symbol = KTS_SYM_LINE_COMMENT;
            return true;
        }
        return false;
    }

    if (kts_is_ident_start(lexer->lookahead)) {
        const char *kw = "struct";
        size_t matched = 0;
        bool is_struct = true;
        while (!lexer->eof(lexer) && kts_is_ident_cont(lexer->lookahead)) {
            if (is_struct) {
                if (matched < 6 && lexer->lookahead == (int32_t)kw[matched]) {
                    matched++;
                } else {
                    is_struct = false;
                }
            }
            lexer->advance(lexer, false);
        }
        lexer->mark_end(lexer);
        lexer->result_symbol = (is_struct && matched == 6) ? KTS_SYM_STRUCT : KTS_SYM_IDENTIFIER;
        return true;
    }

    return false;
}

static const TSLanguage kts_rust_language_instance = {
    .abi_version = TREE_SITTER_LANGUAGE_VERSION,
    .symbol_count = KTS_SYMBOL_COUNT,
    .alias_count = 0,
    .token_count = KTS_TOKEN_COUNT,
    .external_token_count = 0,
    .state_count = KTS_STATE_COUNT,
    .large_state_count = KTS_LARGE_STATE_COUNT,
    .production_id_count = 2,
    .field_count = 3,
    .max_alias_sequence_length = 0,
    .parse_table = &kts_parse_table[0][0],
    .small_parse_table = kts_small_parse_table,
    .small_parse_table_map = kts_small_parse_table_map,
    .parse_actions = kts_parse_actions,
    .symbol_names = kts_symbol_names,
    .field_names = kts_field_names,
    .field_map_slices = kts_field_map_slices,
    .field_map_entries = kts_field_map_entries,
    .symbol_metadata = kts_symbol_metadata,
    .public_symbol_map = kts_public_symbol_map,
    .alias_map = kts_alias_map,
    .alias_sequences = kts_alias_sequences,
    .lex_modes = kts_lex_modes,
    .lex_fn = kts_rust_lex,
    .keyword_lex_fn = NULL,
    .keyword_capture_token = 0,
    .external_scanner = { 0 },
    .primary_state_ids = kts_primary_state_ids,
    .name = "rust",
    .reserved_words = kts_reserved_words,
    .max_reserved_word_set_size = 0,
    .supertype_count = 5,
    .supertype_symbols = kts_supertype_symbols,
    .supertype_map_slices = kts_supertype_map_slices,
    .supertype_map_entries = kts_supertype_map_entries,
    .metadata = {
        .major_version = 0,
        .minor_version = 23,
        .patch_version = 2,
    },
};

const TSLanguage *kts_rust_fixture_language(void) {
    return &kts_rust_language_instance;
}
