// treesitter_bindings.cpp — N-API bindings for tree-sitter C engine
#include <napi.h>
#include <cstdlib>
#include <cstring>
#include <vector>
#include "tree_sitter/api.h"

extern "C" const TSLanguage *kts_rust_fixture_language(void);

// Slot storage for TSNode to allow safe 64-bit handle passing to JS
static std::vector<TSNode*> g_nodes;

static int alloc_node(TSNode node) {
    TSNode* copy = new TSNode(node);
    for (size_t i = 1; i < g_nodes.size(); i++) {
        if (g_nodes[i] == nullptr) {
            g_nodes[i] = copy;
            return static_cast<int>(i);
        }
    }
    if (g_nodes.empty()) {
        g_nodes.push_back(nullptr); // 0 is invalid
    }
    g_nodes.push_back(copy);
    return static_cast<int>(g_nodes.size() - 1);
}

static TSNode* get_node(int handle) {
    if (handle <= 0 || static_cast<size_t>(handle) >= g_nodes.size()) return nullptr;
    return g_nodes[handle];
}

static void free_node(int handle) {
    if (handle <= 0 || static_cast<size_t>(handle) >= g_nodes.size()) return;
    delete g_nodes[handle];
    g_nodes[handle] = nullptr;
}

static std::vector<TSTreeCursor*> g_cursors;

static int alloc_cursor(TSTreeCursor cursor) {
    TSTreeCursor* copy = new TSTreeCursor(cursor);
    for (size_t i = 1; i < g_cursors.size(); i++) {
        if (g_cursors[i] == nullptr) {
            g_cursors[i] = copy;
            return static_cast<int>(i);
        }
    }
    if (g_cursors.empty()) {
        g_cursors.push_back(nullptr);
    }
    g_cursors.push_back(copy);
    return static_cast<int>(g_cursors.size() - 1);
}

static TSTreeCursor* get_cursor(int handle) {
    if (handle <= 0 || static_cast<size_t>(handle) >= g_cursors.size()) return nullptr;
    return g_cursors[handle];
}

static void free_cursor(int handle) {
    if (handle <= 0 || static_cast<size_t>(handle) >= g_cursors.size()) return;
    ts_tree_cursor_delete(g_cursors[handle]);
    delete g_cursors[handle];
    g_cursors[handle] = nullptr;
}

static int64_t get_ptr_arg(const Napi::CallbackInfo& info, size_t index) {
    if (info.Length() <= index) return 0;
    if (info[index].IsBigInt()) {
        bool lossless = true;
        return info[index].As<Napi::BigInt>().Int64Value(&lossless);
    } else if (info[index].IsNumber()) {
        return static_cast<int64_t>(info[index].As<Napi::Number>().DoubleValue());
    }
    return 0;
}

static Napi::Value ptr_to_value(Napi::Env env, void* ptr) {
    return Napi::BigInt::New(env, reinterpret_cast<int64_t>(ptr));
}

Napi::Object Init(Napi::Env env, Napi::Object exports) {
    // Language fixture
    exports.Set("kts_rust_fixture_language", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        return ptr_to_value(info.Env(), (void*)kts_rust_fixture_language());
    }));

    // Language inspection
    exports.Set("ts_language_abi_version", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto lang = reinterpret_cast<const TSLanguage*>(get_ptr_arg(info, 0));
        return Napi::Number::New(info.Env(), lang ? ts_language_abi_version(lang) : 0);
    }));
    exports.Set("ts_language_symbol_count", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto lang = reinterpret_cast<const TSLanguage*>(get_ptr_arg(info, 0));
        return Napi::Number::New(info.Env(), lang ? ts_language_symbol_count(lang) : 0);
    }));
    exports.Set("ts_language_state_count", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto lang = reinterpret_cast<const TSLanguage*>(get_ptr_arg(info, 0));
        return Napi::Number::New(info.Env(), lang ? ts_language_state_count(lang) : 0);
    }));
    exports.Set("ts_language_field_count", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto lang = reinterpret_cast<const TSLanguage*>(get_ptr_arg(info, 0));
        return Napi::Number::New(info.Env(), lang ? ts_language_field_count(lang) : 0);
    }));
    exports.Set("ts_language_name", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto lang = reinterpret_cast<const TSLanguage*>(get_ptr_arg(info, 0));
        const char* name = lang ? ts_language_name(lang) : nullptr;
        return name ? Napi::String::New(info.Env(), name) : info.Env().Null();
    }));
    exports.Set("ts_language_symbol_name", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto lang = reinterpret_cast<const TSLanguage*>(get_ptr_arg(info, 0));
        uint16_t sym = info[1].As<Napi::Number>().Uint32Value();
        const char* name = lang ? ts_language_symbol_name(lang, sym) : nullptr;
        return name ? Napi::String::New(info.Env(), name) : info.Env().Null();
    }));
    exports.Set("ts_language_symbol_for_name", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto lang = reinterpret_cast<const TSLanguage*>(get_ptr_arg(info, 0));
        std::string name = info[1].As<Napi::String>().Utf8Value();
        bool is_named = info[2].As<Napi::Boolean>().Value();
        return Napi::Number::New(info.Env(), lang ? ts_language_symbol_for_name(lang, name.c_str(), name.length(), is_named) : 0);
    }));
    exports.Set("ts_language_field_name_for_id", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto lang = reinterpret_cast<const TSLanguage*>(get_ptr_arg(info, 0));
        uint16_t id = info[1].As<Napi::Number>().Uint32Value();
        const char* name = lang ? ts_language_field_name_for_id(lang, id) : nullptr;
        return name ? Napi::String::New(info.Env(), name) : info.Env().Null();
    }));
    exports.Set("ts_language_field_id_for_name", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto lang = reinterpret_cast<const TSLanguage*>(get_ptr_arg(info, 0));
        std::string name = info[1].As<Napi::String>().Utf8Value();
        return Napi::Number::New(info.Env(), lang ? ts_language_field_id_for_name(lang, name.c_str(), name.length()) : 0);
    }));
    exports.Set("ts_language_next_state", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto lang = reinterpret_cast<const TSLanguage*>(get_ptr_arg(info, 0));
        uint16_t state = info[1].As<Napi::Number>().Uint32Value();
        uint16_t symbol = info[2].As<Napi::Number>().Uint32Value();
        return Napi::Number::New(info.Env(), lang ? ts_language_next_state(lang, state, symbol) : 0);
    }));

    // Parser
    exports.Set("ts_parser_new", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        return ptr_to_value(info.Env(), ts_parser_new());
    }));
    exports.Set("ts_parser_delete", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto parser = reinterpret_cast<TSParser*>(get_ptr_arg(info, 0));
        if (parser) ts_parser_delete(parser);
        return info.Env().Undefined();
    }));
    exports.Set("ts_parser_set_language", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto parser = reinterpret_cast<TSParser*>(get_ptr_arg(info, 0));
        auto lang = reinterpret_cast<const TSLanguage*>(get_ptr_arg(info, 1));
        bool ok = parser ? ts_parser_set_language(parser, lang) : false;
        return Napi::Boolean::New(info.Env(), ok);
    }));
    exports.Set("ts_parser_reset", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto parser = reinterpret_cast<TSParser*>(get_ptr_arg(info, 0));
        if (parser) ts_parser_reset(parser);
        return info.Env().Undefined();
    }));
    exports.Set("ts_parser_parse_string", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto parser = reinterpret_cast<TSParser*>(get_ptr_arg(info, 0));
        auto old_tree = reinterpret_cast<const TSTree*>(get_ptr_arg(info, 1));
        std::string source = info[2].As<Napi::String>().Utf8Value();
        TSTree* tree = parser ? ts_parser_parse_string(parser, old_tree, source.data(), source.length()) : nullptr;
        return ptr_to_value(info.Env(), tree);
    }));

    // Tree
    exports.Set("ts_tree_delete", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto tree = reinterpret_cast<TSTree*>(get_ptr_arg(info, 0));
        if (tree) ts_tree_delete(tree);
        return info.Env().Undefined();
    }));
    exports.Set("ts_tree_copy", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto tree = reinterpret_cast<const TSTree*>(get_ptr_arg(info, 0));
        return ptr_to_value(info.Env(), tree ? ts_tree_copy(tree) : nullptr);
    }));
    exports.Set("ts_tree_root_node", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto tree = reinterpret_cast<const TSTree*>(get_ptr_arg(info, 0));
        if (!tree) return Napi::Number::New(info.Env(), 0);
        TSNode root = ts_tree_root_node(tree);
        return Napi::Number::New(info.Env(), alloc_node(root));
    }));

    // Node
    exports.Set("ts_node_type", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto node = get_node(info[0].As<Napi::Number>().Int32Value());
        const char* type = node ? ts_node_type(*node) : nullptr;
        return type ? Napi::String::New(info.Env(), type) : info.Env().Null();
    }));
    exports.Set("ts_node_symbol", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto node = get_node(info[0].As<Napi::Number>().Int32Value());
        return Napi::Number::New(info.Env(), node ? ts_node_symbol(*node) : 0);
    }));
    exports.Set("ts_node_start_byte", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto node = get_node(info[0].As<Napi::Number>().Int32Value());
        return Napi::Number::New(info.Env(), node ? ts_node_start_byte(*node) : 0);
    }));
    exports.Set("ts_node_end_byte", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto node = get_node(info[0].As<Napi::Number>().Int32Value());
        return Napi::Number::New(info.Env(), node ? ts_node_end_byte(*node) : 0);
    }));
    exports.Set("ts_node_child_count", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto node = get_node(info[0].As<Napi::Number>().Int32Value());
        return Napi::Number::New(info.Env(), node ? ts_node_child_count(*node) : 0);
    }));
    exports.Set("ts_node_named_child_count", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto node = get_node(info[0].As<Napi::Number>().Int32Value());
        return Napi::Number::New(info.Env(), node ? ts_node_named_child_count(*node) : 0);
    }));
    exports.Set("ts_node_child", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto node = get_node(info[0].As<Napi::Number>().Int32Value());
        uint32_t index = info[1].As<Napi::Number>().Uint32Value();
        if (!node) return Napi::Number::New(info.Env(), 0);
        TSNode child = ts_node_child(*node, index);
        return Napi::Number::New(info.Env(), ts_node_is_null(child) ? 0 : alloc_node(child));
    }));
    exports.Set("ts_node_named_child", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto node = get_node(info[0].As<Napi::Number>().Int32Value());
        uint32_t index = info[1].As<Napi::Number>().Uint32Value();
        if (!node) return Napi::Number::New(info.Env(), 0);
        TSNode child = ts_node_named_child(*node, index);
        return Napi::Number::New(info.Env(), ts_node_is_null(child) ? 0 : alloc_node(child));
    }));
    exports.Set("ts_node_child_by_field_name", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto node = get_node(info[0].As<Napi::Number>().Int32Value());
        std::string name = info[1].As<Napi::String>().Utf8Value();
        if (!node) return Napi::Number::New(info.Env(), 0);
        TSNode child = ts_node_child_by_field_name(*node, name.c_str(), name.length());
        return Napi::Number::New(info.Env(), ts_node_is_null(child) ? 0 : alloc_node(child));
    }));
    exports.Set("ts_node_has_error", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto node = get_node(info[0].As<Napi::Number>().Int32Value());
        return Napi::Boolean::New(info.Env(), node ? ts_node_has_error(*node) : false);
    }));
    exports.Set("ts_node_is_named", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto node = get_node(info[0].As<Napi::Number>().Int32Value());
        return Napi::Boolean::New(info.Env(), node ? ts_node_is_named(*node) : false);
    }));
    exports.Set("ts_node_is_missing", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto node = get_node(info[0].As<Napi::Number>().Int32Value());
        return Napi::Boolean::New(info.Env(), node ? ts_node_is_missing(*node) : false);
    }));
    exports.Set("ts_node_is_extra", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto node = get_node(info[0].As<Napi::Number>().Int32Value());
        return Napi::Boolean::New(info.Env(), node ? ts_node_is_extra(*node) : false);
    }));
    exports.Set("ts_node_string", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto node = get_node(info[0].As<Napi::Number>().Int32Value());
        if (!node) return info.Env().Null();
        char* str = ts_node_string(*node);
        Napi::String res = Napi::String::New(info.Env(), str ? str : "");
        if (str) free(str);
        return res;
    }));
    exports.Set("ts_node_free", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        free_node(info[0].As<Napi::Number>().Int32Value());
        return info.Env().Undefined();
    }));

    // Cursor
    exports.Set("ts_tree_cursor_new", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto node = get_node(info[0].As<Napi::Number>().Int32Value());
        if (!node) return Napi::Number::New(info.Env(), 0);
        TSTreeCursor cursor = ts_tree_cursor_new(*node);
        return Napi::Number::New(info.Env(), alloc_cursor(cursor));
    }));
    exports.Set("ts_tree_cursor_delete", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        free_cursor(info[0].As<Napi::Number>().Int32Value());
        return info.Env().Undefined();
    }));
    exports.Set("ts_tree_cursor_goto_first_child", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto cursor = get_cursor(info[0].As<Napi::Number>().Int32Value());
        return Napi::Boolean::New(info.Env(), cursor ? ts_tree_cursor_goto_first_child(cursor) : false);
    }));
    exports.Set("ts_tree_cursor_goto_next_sibling", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto cursor = get_cursor(info[0].As<Napi::Number>().Int32Value());
        return Napi::Boolean::New(info.Env(), cursor ? ts_tree_cursor_goto_next_sibling(cursor) : false);
    }));
    exports.Set("ts_tree_cursor_goto_parent", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto cursor = get_cursor(info[0].As<Napi::Number>().Int32Value());
        return Napi::Boolean::New(info.Env(), cursor ? ts_tree_cursor_goto_parent(cursor) : false);
    }));
    exports.Set("ts_tree_cursor_current_node", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto cursor = get_cursor(info[0].As<Napi::Number>().Int32Value());
        if (!cursor) return Napi::Number::New(info.Env(), 0);
        TSNode current = ts_tree_cursor_current_node(cursor);
        return Napi::Number::New(info.Env(), alloc_node(current));
    }));
    exports.Set("ts_tree_cursor_current_field_name", Napi::Function::New(env, [](const Napi::CallbackInfo& info) -> Napi::Value {
        auto cursor = get_cursor(info[0].As<Napi::Number>().Int32Value());
        const char* name = cursor ? ts_tree_cursor_current_field_name(cursor) : nullptr;
        return name ? Napi::String::New(info.Env(), name) : info.Env().Null();
    }));

    return exports;
}

NODE_API_MODULE(treesitter_native, Init)
