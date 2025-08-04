package org.figuramc.figura.lua.errors;

import org.spongepowered.include.com.google.common.base.Charsets;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class LuaMatcher {
    /**
     * Quick-exit static checked exception for failed matching assertions.
     *
     * @see Reader#require
     */
    public static class MatchFailed extends Exception {
        public static MatchFailed INSTANCE = new MatchFailed();

        private MatchFailed() {
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return null;
        }
    }

    /**
     * Wrapper around a {@link ByteBuffer} with operators that are useful for parsing.
     */
    public static class Reader {
        private final ByteBuffer buf;

        public Reader(ByteBuffer buf) {
            this.buf = buf;
        }

        public boolean isAtEOF() {
            return buf.position() == buf.limit();
        }

        public boolean space() {
            boolean any = false;
            while (!isAtEOF()) {
                byte b = buf.get();
                if (Character.isWhitespace((char) b)) {
                    any = true;
                } else {
                    buf.position(buf.position() - 1);
                    break;
                }
            }
            return any;
        }

        public byte peek() {
            if (isAtEOF()) return 0;
            byte b = buf.get();
            buf.position(buf.position() - 1);
            return b;
        }

        public boolean check(char ch) {
            if ((int) ch > 0xff) throw new RuntimeException("Supplied character is outside the single-byte range");
            return check((byte) ch);
        }

        public boolean check(byte by) {
            if (isAtEOF()) return false;
            byte b = buf.get();
            if (b == by) return true;
            buf.position(buf.position() - 1);
            return false;
        }

        public boolean check(String env) {
            int mark = buf.position();
            for (byte b : env.getBytes(StandardCharsets.UTF_8)) {
                if (!check(b)) {
                    buf.position(mark);
                    return false;
                }
            }
            return true;
        }

        public Reader require(char ch) throws MatchFailed {
            if ((int) ch > 0xff) throw new RuntimeException("Supplied character is outside the single-byte range");
            return require((byte) ch);
        }

        public Reader require(byte by) throws MatchFailed {
            if (isAtEOF()) throw MatchFailed.INSTANCE;
            if (buf.get() != by) throw MatchFailed.INSTANCE;
            return this;
        }

        public int position() {
            return buf.position();
        }

        public void position(int newPosition) {
            buf.position(newPosition);
        }

        public void next() {
            buf.get();
        }

        public String readback(int marker) {
            int size = buf.position() - marker;
            byte[] b = new byte[size];
            buf.position(marker);
            buf.get(size, b);
            return new String(b, Charsets.UTF_8);
        }

        public Reader copy() {
            return new Reader(buf.duplicate());
        }
    }

    /**
     * Container for setting up a chain of matchers with automatic backtracking support.
     */
    public static class MatchState {
        private final Iterator<Reader> backtrackingSupplier;
        public final ArrayList<Matcher> matchers = new ArrayList<>();

        public MatchState(Iterator<Reader> backtrackingSupplier) {
            this.backtrackingSupplier = backtrackingSupplier;
        }

        public MatchState then(Matcher element) {
            matchers.add(element);
            return this;
        }

        public ArrayList<Matcher> execute() {
            nextMatch:
            while (backtrackingSupplier.hasNext()) {
                Reader b = backtrackingSupplier.next();
                for (Matcher m : matchers) {
                    if (!m.match(b)) continue nextMatch;
                }
                return matchers;
            }
            return null;
        }

        public static MatchState noMatch() {
            return new MatchState(Collections.emptyIterator());
        }
    }

    /**
     * Scaffolding for iterators around a function that produces a value of type {@link T} or {@code null}.
     *
     * @param <T> what type the iterator produces
     */
    private static abstract class PreemptNextIterator<T> implements Iterator<T> {
        private T next;

        protected abstract T prepareNext();

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public T next() {
            T result = next;
            next = prepareNext();
            return result;
        }

        { /* instance initializer */
            prepareNext();
        }
    }

    /**
     * If the provided matcher doesn't match on the provided reader, throw {@link MatchFailed}.
     * @param m what matcher to apply
     * @param buf the buffer to apply to
     * @throws MatchFailed no match
     */
    public static void assertMatch(Matcher m, Reader buf) throws MatchFailed {
        if (!m.match(buf)) throw MatchFailed.INSTANCE;
    }

    /**
     * Tests if the provided name is 'simple' - i.e. if it is an allowed variable name and would support dot-indexing.
     */
    public static boolean isSimpleName(String name) {
        return name.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
    }

    /**
     * An element that can match text at the current position, and captures the matched text for later examination.
     */
    public interface Matcher {
        boolean match(Reader buf);

        String getCaptured();
    }

    /**
     * A {@link Matcher} that also can scan for matching text within the buffer.
     */
    public interface StartMatcher extends Matcher {
        MatchState search(Reader buf);
    }

    /**
     * Captures the colon-call syntax for object-oriented code.
     * <pre>
     * IsSimpleName(key): [space] ':' [space] key
     * else: (no match)
     * </pre>
     */
    public static class ColonIndex implements StartMatcher {
        private final boolean isSimple;
        public final String key;
        private String captured;

        public ColonIndex(String key) {
            this.key = key;
            this.isSimple = isSimpleName(key);
        }

        @Override
        public boolean match(Reader buf) {
            if (!isSimple) return false;

            int marker = buf.position();
            try {
                buf.space();
                buf.require(':');
                buf.space();
                for (byte b : key.getBytes(StandardCharsets.UTF_8)) {
                    buf.require(b);
                }

                captured = buf.readback(marker);
                marker = buf.position();
                return true;
            } catch (BufferUnderflowException | MatchFailed e) {
                return false;
            } finally {
                buf.position(marker);
            }
        }

        @Override
        public MatchState search(Reader buf) {
            if (!isSimple) return MatchState.noMatch();
            throw new RuntimeException("!");
        }

        @Override
        public String getCaptured() {
            return captured;
        }
    }

    /**
     * A string indexing operation, dot or bracketed
     * <pre>
     * [space] '[' [space] '"' key '"' [space] ']'
     * | IsSimpleName(key)? [space] '.' [space] key
     * </pre>
     */
    public static class Index implements StartMatcher {
        public final boolean isSimple;
        public final String key;
        private String captured;

        public Index(String key) {
            this.key = key;
            this.isSimple = isSimpleName(key);
        }

        @Override
        public boolean match(Reader buf) {
            int marker = buf.position();
            try {
                buf.space();
                if (buf.check('[')) {
                    buf.space();
                    buf.require('"');
                    for (byte b : key.getBytes(StandardCharsets.UTF_8)) {
                        buf.require(b);
                    }
                    buf.require('"');
                    buf.space();
                    buf.require(']');

                    // Commit this
                    captured = buf.readback(marker);
                    marker = buf.position();
                    return true;
                } else if (isSimple && buf.check('.')) {
                    buf.space();
                    for (byte b : key.getBytes(StandardCharsets.UTF_8)) {
                        buf.require(b);
                    }

                    captured = buf.readback(marker);
                    marker = buf.position();
                    return true;
                } else {
                    return false;
                }
            } catch (BufferUnderflowException | MatchFailed e) {
                return false;
            } finally {
                buf.position(marker);
            }
        }

        @Override
        public String getCaptured() {
            return captured;
        }

        @Override
        public MatchState search(Reader buf) {
            return new MatchState(new PreemptNextIterator<>() {
                @Override
                protected Reader prepareNext() {
                    while (!buf.isAtEOF()) {
                        byte next = buf.peek();
                        if (next == (byte) '[' || (isSimple && next == (byte) '.')) {
                            if (match(buf)) return buf.copy();
                        }
                    }
                    return null;
                }
            });
        }
    }

    /**
     * Global variable access, with or without _ENV
     * <pre>
     * IsSimpleName(key): "_ENV" Index(key) || key
     * else: "_ENV" Index(key)
     * </pre>
     */
    public static class GlobalAccess implements StartMatcher {
        public final boolean isSimple;
        public final String key;

        private String captured;

        public GlobalAccess(String key) {
            this.key = key;
            this.isSimple = isSimpleName(key);
        }

        @Override
        public boolean match(Reader buf) {
            int marker = buf.position();
            try {
                buf.space();
                if (buf.check("_ENV")) {
                    assertMatch(new Index(key), buf);
                    captured = buf.readback(marker);
                    marker = buf.position();
                    return true;
                } else if (isSimple) {
                    for (byte b : key.getBytes(StandardCharsets.UTF_8)) {
                        buf.require(b);
                    }

                    captured = buf.readback(marker);
                    marker = buf.position();
                    return true;
                } else {
                    return false;
                }
            } catch (BufferUnderflowException | MatchFailed e) {
                return false;
            } finally {
                buf.position(marker);
            }
        }

        @Override
        public String getCaptured() {
            return captured;
        }

        @Override
        public MatchState search(Reader buf) {
            return new MatchState(new PreemptNextIterator<>() {
                @Override
                protected Reader prepareNext() {
                    while (!buf.isAtEOF()) {
                        int marker = buf.position();
                        if (buf.check("_ENV") || buf.check(key)) {
                            buf.position(marker);
                            if (match(buf)) {
                                return buf.copy();
                            }
                        }
                        buf.next();
                    }
                    return null;
                }
            });
        }
    }

    /**
     * Local or upvalue access syntax.
     * <pre>
     * IsSimpleName(name): [space] key
     * else: (no match)
     * </pre>
     */
    public static class LocalUpvalAccess implements StartMatcher {
        public final String key;
        private String captured;

        public LocalUpvalAccess(String key) {
            this.key = key;
        }

        @Override
        public boolean match(Reader buf) {
            if (!isSimpleName(key)) return false;

            int marker = buf.position();
            try {
                buf.space();
                for (byte b : key.getBytes(StandardCharsets.UTF_8)) {
                    buf.require(b);
                }

                captured = buf.readback(marker);
                marker = buf.position();
                return true;
            } catch (BufferUnderflowException | MatchFailed e) {
                return false;
            } finally {
                buf.position(marker);
            }
        }

        @Override
        public MatchState search(Reader buf) {
            if (!isSimpleName(key)) return MatchState.noMatch();
            return new MatchState(new PreemptNextIterator<>() {
                @Override
                protected Reader prepareNext() {
                    while (!buf.isAtEOF()) {
                        int marker = buf.position();
                        if (buf.check(key)) {
                            buf.position(marker);
                            if (match(buf)) return buf.copy();
                        }
                        buf.next();
                    }
                    return null;
                }
            });
        }

        @Override
        public String getCaptured() {
            return captured;
        }
    }
}
