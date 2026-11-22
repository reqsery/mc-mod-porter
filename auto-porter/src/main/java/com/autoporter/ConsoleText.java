package com.autoporter;

import java.io.*;
import java.nio.charset.*;
import java.util.Map;

/**
 * Keeps the CLI readable on consoles that are not UTF-8 (the Windows default code pages): when
 * {@code stdout.encoding} is not UTF-8, the porter's symbols are written as plain ASCII instead of "?" or mojibake.
 */
final class ConsoleText {
    private static final Map<Character, String> ASCII = Map.ofEntries(
        Map.entry('→', "->"), Map.entry('←', "<-"), Map.entry('•', "-"), Map.entry('—', "-"), Map.entry('–', "-"),
        Map.entry('…', "..."), Map.entry('✓', "OK"), Map.entry('✗', "X"), Map.entry('⚠', "!"),
        Map.entry('═', "="), Map.entry('─', "-"), Map.entry('║', "|"), Map.entry('│', "|"),
        Map.entry('╔', "+"), Map.entry('╗', "+"), Map.entry('╚', "+"), Map.entry('╝', "+"),
        Map.entry('“', "\""), Map.entry('”', "\""), Map.entry('‘', "'"), Map.entry('’', "'"));

    private ConsoleText() {}

    /** Wraps System.out / System.err when their encoding cannot show the symbols above. */
    static void install() {
        System.setOut(wrap(System.out, System.getProperty("stdout.encoding")));
        System.setErr(wrap(System.err, System.getProperty("stderr.encoding")));
    }

    private static PrintStream wrap(PrintStream stream, String encoding) {
        Charset charset;
        try { charset = encoding == null ? Charset.defaultCharset() : Charset.forName(encoding); }
        catch (IllegalArgumentException e) { return stream; }
        if (charset.equals(StandardCharsets.UTF_8)) return stream;
        return new PrintStream(new FileOutputStream(stream == System.err ? FileDescriptor.err : FileDescriptor.out), true, charset) {
            @Override public void print(String s) { super.print(toAscii(String.valueOf(s))); }
            @Override public void println(String s) { super.println(toAscii(String.valueOf(s))); }
            @Override public void print(Object o) { print(String.valueOf(o)); }
            @Override public void println(Object o) { println(String.valueOf(o)); }
        };
    }

    /** Replaces every symbol in the table, even ones the code page has: the reader of a pipe may decode differently. */
    static String toAscii(String s) { return toAscii(s, c -> false); }

    /** Replaces the symbols {@code canShow} rejects (e.g. glyphs a font lacks). */
    static String toAscii(String s, java.util.function.Predicate<Character> canShow) {
        StringBuilder out = null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            String replacement = canShow.test(c) ? null : ASCII.get(c);
            if (replacement == null) { if (out != null) out.append(c); continue; }
            if (out == null) out = new StringBuilder(s.length() + 8).append(s, 0, i);
            out.append(replacement);
        }
        return out == null ? s : out.toString();
    }
}
