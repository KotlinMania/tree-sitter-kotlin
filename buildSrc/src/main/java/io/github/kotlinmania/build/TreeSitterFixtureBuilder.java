package io.github.kotlinmania.build;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Builds the mandatory existing tree-sitter JNI/native bindings with the selected toolchains. */
public final class TreeSitterFixtureBuilder {
    private TreeSitterFixtureBuilder() {}
    private static void require(boolean value, String message) { if (!value) throw new IllegalArgumentException(message); }
    private static void input(Path path) { require(Files.isRegularFile(path), "Missing tree-sitter input: " + path); }
    private static boolean windows() { return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows"); }

    private static void execute(Path root, String stage, List<String> command) throws Exception {
        Path reports = root.resolve("build/reports/treesitter-fixtures"), scratch = root.resolve("build/tmp/treesitter-fixtures/" + stage);
        Files.createDirectories(reports); Files.createDirectories(scratch);
        Files.writeString(reports.resolve(stage + ".command.txt"), String.join("\n", command) + "\n");
        ProcessBuilder builder = new ProcessBuilder(command).directory(root.toFile()).redirectOutput(reports.resolve(stage + ".stdout").toFile()).redirectError(reports.resolve(stage + ".stderr").toFile());
        builder.environment().putAll(Map.of("TMPDIR", scratch.toString(), "TMP", scratch.toString(), "TEMP", scratch.toString()));
        String javaOptions = builder.environment().getOrDefault("JAVA_TOOL_OPTIONS", "");
        builder.environment().put("JAVA_TOOL_OPTIONS", javaOptions + " -Djava.io.tmpdir=\"" + scratch + "\"");
        Process process = builder.start(); boolean finished;
        try { finished = process.waitFor(5, TimeUnit.MINUTES); }
        catch (InterruptedException interrupted) { stop(process); Thread.currentThread().interrupt(); throw interrupted; }
        if (!finished) stop(process);
        int exit = finished ? process.exitValue() : 124; Files.writeString(reports.resolve(stage + ".exit"), exit + "\n");
        require(exit == 0, "tree-sitter " + stage + " failed (" + exit + "): " + Files.readString(reports.resolve(stage + ".stderr")));
    }
    private static void stop(Process process) throws InterruptedException {
        try { process.descendants().forEach(ProcessHandle::destroyForcibly); }
        catch (UnsupportedOperationException ignored) { /* Stop the owned process below as well. */ }
        finally { process.destroyForcibly(); process.waitFor(); }
    }

    static List<String> jniCommand(Path root, Path javaHome, Path output) throws Exception {
        boolean mac = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac");
        Path javaInclude = javaHome.resolve("include"), osInclude = javaInclude.resolve(windows() ? "win32" : mac ? "darwin" : "linux");
        input(javaInclude.resolve("jni.h")); input(osInclude.resolve("jni_md.h"));
        Path vendor = root.resolve("tree-sitter/lib"), jni = root.resolve("src/jni");
        List<String> command = new ArrayList<>(List.of("clang", "-shared"));
        if (!windows()) command.add("-fPIC"); if (mac) command.add("-mmacosx-version-min=15.0");
        command.addAll(List.of("-O2", "-I" + javaInclude, "-I" + osInclude, "-I" + jni, "-I" + vendor.resolve("include"), "-I" + vendor.resolve("src"), "-DTREE_SITTER_HIDE_SYMBOLS", "-D_DEFAULT_SOURCE", "-D_POSIX_C_SOURCE=200112L"));
        List<Path> sources = new ArrayList<>(List.of(vendor.resolve("src/lib.c"), root.resolve("src/native/interop/cinterop/treesitter_ext.c")));
        try (var files = Files.list(jni)) { sources.addAll(files.filter(path -> path.toString().endsWith(".c")).sorted().toList()); }
        require(sources.size() > 2, "No tree-sitter JNI sources"); for (Path source : sources) { input(source); command.add(source.toString()); }
        command.addAll(List.of("-o", output.toString())); return command;
    }

    static List<String> nativeArguments(Path root, Path source, Path output) {
        Path vendor = root.resolve("tree-sitter/lib");
        return List.of("-I" + vendor.resolve("src"), "-I" + vendor.resolve("include"), "-DTREE_SITTER_HIDE_SYMBOLS", "-D_DEFAULT_SOURCE", "-D_POSIX_C_SOURCE=200112L", "-fvisibility=hidden", "-std=c11", "-O2", "-g", "-c", "-o", output.toString(), source.toString());
    }
    private static String response(List<String> arguments) {
        return arguments.stream().map(value -> { require(!value.contains("\n") && !value.contains("\r"), "Invalid compiler argument"); return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }).collect(java.util.stream.Collectors.joining("\n", "", "\n"));
    }
    public static int run(String... args) throws Exception {
        Map<String, String> options = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) { require(args[i].startsWith("--") && i + 1 < args.length, "Missing argument"); require(options.put(args[i].substring(2), args[++i]) == null, "Duplicate argument"); }
        require(List.of("jni", "native").contains(options.get("mode")), "Use --mode jni or native");
        boolean jni = options.get("mode").equals("jni");
        require(options.keySet().equals(jni ? java.util.Set.of("root", "output", "mode", "java-home") : java.util.Set.of("root", "output", "mode", "konan-home", "target")), "Wrong tree-sitter arguments");
        Path root = Path.of(options.get("root")).toRealPath(), output = Path.of(options.get("output")).toAbsolutePath().normalize();
        Files.createDirectories(root.resolve("build")); require(root.resolve("build").toRealPath().startsWith(root), "Project build folder escapes its repository");
        require(output.startsWith(root.resolve("build")), "Output must stay inside the project's build folder"); Files.createDirectories(output.getParent());
        require(output.getParent().toRealPath().startsWith(root.resolve("build").toRealPath()) && !Files.isSymbolicLink(output), "Output symlink escapes project build folder");
        // A failed compiler must not leave an old artifact that appears usable.
        Files.deleteIfExists(output);
        if (jni) execute(root, "jni", jniCommand(root, Path.of(options.get("java-home")), output));
        else {
            String target = options.get("target"); require(target.matches("[a-z0-9_]+"), "Invalid Kotlin/Native target");
            Path home = Path.of(options.get("konan-home")), runKonan = home.resolve("bin/" + (windows() ? "run_konan.bat" : "run_konan")); input(runKonan);
            List<Path> sources = List.of(root.resolve("tree-sitter/lib/src/lib.c"), root.resolve("src/native/interop/cinterop/treesitter_ext.c")); List<String> archive = new ArrayList<>(List.of(runKonan.toString(), "llvm", "llvm-ar", "rcs", output.toString()));
            for (int i = 0; i < sources.size(); i++) {
                Path source = sources.get(i); input(source); Path object = output.resolveSibling(i == 0 ? "lib.o" : "treesitter_ext.o"), argsFile = output.resolveSibling("compile-" + i + ".args");
                Files.writeString(argsFile, response(nativeArguments(root, source, object))); Files.deleteIfExists(object);
                execute(root, target + "-compile-" + i, List.of(runKonan.toString(), "clang", "clang", target, "@" + argsFile)); archive.add(object.toString());
            }
            execute(root, target + "-archive", archive);
        }
        require(Files.isRegularFile(output) && Files.size(output) > 0, "Compiler produced no tree-sitter artifact: " + output);
        System.out.println("Built " + output); return 0;
    }
    public static void main(String[] args) throws Exception { System.exit(run(args)); }
}
