# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 2/6 (33.3%)
- **Function parity:** 16/202 matched (target 38) — 7.9%
- **Class/type parity:** 32/88 matched (target 49) — 36.4%
- **Combined symbol parity:** 48/290 matched (target 87) — 16.6%
- **Average inline-code cosine:** 0.13 (function body across 2 matched files)
- **Average documentation cosine:** 0.27 (doc text across 2 matched files)
- **Cheat-zeroed Files:** 1
- **Critical Issues:** 2 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. lib

- **Target:** `Tree [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 1691210.0
- **Functions:** 14/166 matched (target 35)
- **Missing functions:** `new`, `drop`, `name`, `abi_version`, `metadata`, `supertypes`, `field_count`, `field_name_for_id`, `field_id_for_name`, `next_state`, `lookahead_iterator`, `clone`, `deref`, `default`, `set_language`, `language`, `logger`, `set_logger`, `log`, `print_dot_graphs`, `stop_printing_dot_graphs`, `parse`, `parse_utf16`, `parse_with`, `parse_with_options`, `progress`, `read`, `parse_utf16_with`, `parse_utf16_le`, `parse_utf16_le_with_options`, `parse_utf16_be`, `parse_utf16_be_with_options`, `parse_custom_encoding`, `decode_fn`, `reset`, `timeout_micros`, `set_timeout_micros`, `set_included_ranges`, `included_ranges`, `cancellation_flag`, `set_cancellation_flag`, `root_node`, `root_node_with_offset`, `edit`, `walk`, `changed_ranges`, `print_dot_graph`, `fmt`, `id`, `kind_id`, `grammar_id`, `kind`, `grammar_name`, `is_named`, `is_extra`, `has_changes`, `is_error`, `parse_state`, `next_parse_state`, `is_missing`, `start_byte`, `end_byte`, `byte_range`, `range`, `start_position`, `end_position`, `child`, `child_count`, `named_child`, `named_child_count`, `child_by_field_name`, `child_by_field_id`, `field_name_for_child`, `field_name_for_named_child`, `children`, `named_children`, `children_by_field_name`, `children_by_field_id`, `parent`, `child_with_descendant`, `next_sibling`, `prev_sibling`, `next_named_sibling`, `prev_named_sibling`, `first_child_for_byte`, `first_named_child_for_byte`, `descendant_count`, `descendant_for_byte_range`, `named_descendant_for_byte_range`, `descendant_for_point_range`, `named_descendant_for_point_range`, `to_sexp`, `utf8_text`, `utf16_text`, `eq`, `hash`, `node`, `field_id`, `field_name`, `depth`, `descendant_index`, `goto_first_child`, `goto_last_child`, `goto_parent`, `goto_next_sibling`, `goto_descendant`, `goto_previous_sibling`, `goto_first_child_for_byte`, `goto_first_child_for_point`, `reset_to`, `current_symbol`, `current_symbol_name`, `reset_state`, `iter_names`, `next`, `from_raw_parts`, `start_byte_for_pattern`, `end_byte_for_pattern`, `pattern_count`, `capture_names`, `capture_quantifiers`, `capture_index_for_name`, `property_predicates`, `property_settings`, `general_predicates`, `disable_capture`, `disable_pattern`, `is_pattern_rooted`, `is_pattern_non_local`, `is_pattern_guaranteed_at_step`, `parse_property`, `match_limit`, `set_match_limit`, `did_exceed_match_limit`, `matches`, `matches_with_options`, `captures`, `captures_with_options`, `set_byte_range`, `set_point_range`, `set_max_start_depth`, `remove`, `nodes_for_capture_index`, `satisfies_text_predicates`, `get_text`, `advance`, `get_mut`, `text`, `predicate_error`, `format_sexp`, `wasm_stdlib_symbols`, `set_allocator`
- **Types:** 31/46 matched (target 48)
- **Missing types:** `LookaheadNamesIterator`, `QueryCursorOptionsDrop`, `FieldId`, `Logger`, `Decode`, `QueryMatches`, `QueryCaptures`, `TextProvider`, `TextPredicateCapture`, `Target`, `Payload`, `Item`, `TSQueryDrop`, `NodeText`, `I`

### 2. util

- **Target:** `CBufferIter`
- **Similarity:** 0.27
- **Dependents:** 0
- **Priority Score:** 30607.3
- **Functions:** 2/4 matched (target 3)
- **Missing functions:** `new`, `drop`
- **Types:** 1/2 matched (target 1)
- **Missing types:** `Item`

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

