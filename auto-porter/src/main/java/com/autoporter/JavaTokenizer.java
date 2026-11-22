package com.autoporter;

import java.util.*;

/**
 * Minimal lexer for Java and Kotlin sources. It only needs to tell code apart from comments and string/char
 * literals, so remapping never touches text inside {@code "…"}, {@code """…"""}, {@code '…'}, {@code // …} or
 * {@code /* … *}{@code /}.
 */
final class JavaTokenizer {
    enum Kind { IDENT, PUNCT, NUMBER, STRING, COMMENT, SPACE }

    record Token(Kind kind, String text, int start, int end) {
        boolean is(String s) { return text.equals(s); }
        boolean code() { return kind != Kind.SPACE && kind != Kind.COMMENT; }
    }

    private JavaTokenizer() {}

    static List<Token> tokenize(String src) {
        List<Token> out = new ArrayList<>();
        int i = 0, n = src.length();
        while (i < n) {
            char c = src.charAt(i);
            int start = i;
            Kind kind;
            if (Character.isWhitespace(c)) {
                while (i < n && Character.isWhitespace(src.charAt(i))) i++;
                kind = Kind.SPACE;
            } else if (c == '/' && i + 1 < n && src.charAt(i + 1) == '/') {
                while (i < n && src.charAt(i) != '\n') i++;
                kind = Kind.COMMENT;
            } else if (c == '/' && i + 1 < n && src.charAt(i + 1) == '*') {
                int close = src.indexOf("*/", i + 2);
                i = close < 0 ? n : close + 2;
                kind = Kind.COMMENT;
            } else if (src.startsWith("\"\"\"", i)) {
                int close = src.indexOf("\"\"\"", i + 3);
                i = close < 0 ? n : close + 3;
                while (i < n && src.charAt(i) == '"') i++; // Kotlin raw strings may end with extra quotes
                kind = Kind.STRING;
            } else if (c == '"' || c == '\'') {
                i++;
                while (i < n && src.charAt(i) != c && src.charAt(i) != '\n') {
                    if (src.charAt(i) == '\\') i++;
                    i++;
                }
                i = Math.min(n, i + 1);
                kind = Kind.STRING;
            } else if (c == '`') {
                int close = src.indexOf('`', i + 1);
                i = close < 0 ? n : close + 1;
                kind = Kind.STRING; // Kotlin backtick identifier: never remapped
            } else if (Character.isJavaIdentifierStart(c)) {
                while (i < n && Character.isJavaIdentifierPart(src.charAt(i))) i++;
                kind = Kind.IDENT;
            } else if (Character.isDigit(c)) {
                while (i < n && (Character.isLetterOrDigit(src.charAt(i)) || src.charAt(i) == '_' || src.charAt(i) == '.')) i++;
                kind = Kind.NUMBER;
            } else if (c == ':' && i + 1 < n && src.charAt(i + 1) == ':') {
                i += 2;
                kind = Kind.PUNCT;
            } else {
                i++;
                kind = Kind.PUNCT;
            }
            out.add(new Token(kind, src.substring(start, i), start, i));
        }
        return out;
    }
}
