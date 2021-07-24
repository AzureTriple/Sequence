package sequence;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import java.io.UncheckedIOException;
import util.NoIO;

/**
 * An arbitrary sequence of characters. Types implementing this interface are
 * meant to wrap a representation of their character data.
 * 
 * @author AzureTriple
 */
public interface Sequence extends CharSequence,
                                  Comparable<CharSequence>,
                                  Iterable<Character>,
                                  AutoCloseable {
    /**
     * An iterator which traverses the characters in a {@linkplain Sequence}.
     * <p>
     * Unless marked with {@linkplain NoIO}, implementing types should automatically
     * close themselves when {@linkplain #hasNext()} returns <code>false</code>.
     */
    interface SimpleSequenceIterator extends Iterator<Character>,AutoCloseable {
        /**
         * @param count Number of characters to skip.
         * 
         * @return <code>this</code>
         * 
         * @throws IllegalArgumentException <code>count</code> is negative.
         * @throws NoSuchElementException   <code>count</code> is out of bounds.
         */
        SimpleSequenceIterator skip(long count) throws IllegalArgumentException,
                                                       NoSuchElementException,
                                                       UncheckedIOException;
        /**
         * {@inheritDoc}
         * <p>
         * Unless marked with {@linkplain NoIO}, implementing types should automatically
         * close themselves when {@linkplain #hasNext()} returns <code>false</code>.
         * 
         * @see java.util.Iterator#hasNext()
         */
        @Override boolean hasNext() throws UncheckedIOException;
        @Override Character next() throws NoSuchElementException,UncheckedIOException;
        @Override void forEachRemaining(Consumer<? super Character> action) throws UncheckedIOException;
        @Override default void close() throws UncheckedIOException {}
    }
    /**
     * Returns a {@linkplain SimpleSequenceIterator}.
     * 
     * @return An iterator over the characters in this sequence.
     * 
     * @implNote The {@linkplain SimpleSequenceIterator} interface extends
     *           {@linkplain AutoCloseable} to ensure that all resources are closed.
     *           Therefore, the return value should be closed after use unless
     *           marked with the {@linkplain NoIO} annotation.
     */
    @Override SimpleSequenceIterator iterator() throws UncheckedIOException;
    
    /**An iterator which traverses the characters in a {@linkplain Sequence}.*/
    interface SequenceIterator extends Iterator<Character>,AutoCloseable {
        /**
         * @return The index of the character returned by {@linkplain #peek()}, adjusted
         *         to the current sequence's range.
         */
        long index();
        /**
         * @return The number of characters between the current position (exclusive) and
         *         the starting position (inclusive).
         */
        long offset();
        /**@return The sequence being iterated over.*/
        Sequence getParent();
        
        /**
         * @return The value at the cursor without advancing, or <code>null</code> if
         *         the cursor is at the end of the sequence.
         */
        Character peek() throws UncheckedIOException;
        /**
         * @return The value at the cursor's position offset by the argument without
         *         advancing, or <code>null</code> if the cursor is at the end of the
         *         sequence.
         */
        Character peek(int offset) throws UncheckedIOException;
        /**
         * @return The value at the cursor's position offset by the argument without
         *         advancing, or <code>null</code> if the cursor is at the end of the
         *         sequence.
         */
        Character peek(long offset) throws UncheckedIOException;
        
        /**
         * @return The next element in the iteration, or <code>null</code> if the end is
         *         reached.
         * 
         * @see java.util.Iterator#next()
         */
        @Override Character next() throws UncheckedIOException;
        
        /**
         * @return The closest non-whitespace character, or <code>null</code> if the end
         *         is reached.
         */
        Character skipWS() throws UncheckedIOException;
        /**
         * @param limit Index of the last character to consider, exclusive.
         *              Out-of-bounds indices are clipped.
         * 
         * @return The closest non-whitespace character, or <code>null</code> if the
         *         limit is reached.
         */
        Character skipWS(int limit) throws UncheckedIOException;
        /**
         * @param limit Index of the last character to consider, exclusive.
         *              Out-of-bounds indices are clipped.
         * 
         * @return The closest non-whitespace character, or <code>null</code> if the
         *         limit is reached.
         */
        Character skipWS(long limit) throws UncheckedIOException;
        /**
         * @return The closest non-whitespace character without advancing, or
         *         <code>null</code> if the end is reached.
         */
        Character peekNonWS() throws UncheckedIOException;
        /**
         * @param limit Index of the last character to consider, exclusive.
         *              Out-of-bounds indices are clipped.
         * 
         * @return The closest non-whitespace character without advancing, or
         *         <code>null</code> if the limit is reached.
         */
        Character peekNonWS(int limit) throws UncheckedIOException;
        /**
         * @param limit Index of the last character to consider, exclusive.
         *              Out-of-bounds indices are clipped.
         * 
         * @return The closest non-whitespace character without advancing, or
         *         <code>null</code> if the limit is reached.
         */
        Character peekNonWS(long limit) throws UncheckedIOException;
        /**
         * @return The next non-whitespace character without advancing, or
         *         <code>null</code> if the end is reached.
         */
        Character peekNextNonWS() throws UncheckedIOException;
        /**
         * @param limit Index of the last character to consider, exclusive.
         *              Out-of-bounds indices are clipped.
         * 
         * @return The next non-whitespace character without advancing, or
         *         <code>null</code> if the limit is reached.
         */
        Character peekNextNonWS(int limit) throws UncheckedIOException;
        /**
         * @param limit Index of the last character to consider, exclusive.
         *              Out-of-bounds indices are clipped.
         * 
         * @return The next non-whitespace character without advancing, or
         *         <code>null</code> if the limit is reached.
         */
        Character peekNextNonWS(long limit) throws UncheckedIOException;
        /**
         * @return The next non-whitespace character, or <code>null</code> if the end is
         *         reached.
         */
        Character nextNonWS() throws UncheckedIOException;
        /**
         * @param limit Index of the last character to consider, exclusive.
         *              Out-of-bounds indices are clipped.
         * 
         * @return The next non-whitespace character, or <code>null</code> if the limit
         *         is reached.
         */
        Character nextNonWS(int limit) throws UncheckedIOException;
        /**
         * @param limit Index of the last character to consider, exclusive.
         *              Out-of-bounds indices are clipped.
         * 
         * @return The next non-whitespace character, or <code>null</code> if the limit
         *         is reached.
         */
        Character nextNonWS(long limit) throws UncheckedIOException;
        
        /**
         * Saves the current position.
         * 
         * @return <code>this</code>
         */
        SequenceIterator mark();
        /**
         * Saves the current position offset by the argument.
         * 
         * @return <code>this</code>
         * 
         * @throws IndexOutOfBoundsException <code>|index| &ge; size()</code>
         */
        SequenceIterator mark(int offset) throws IndexOutOfBoundsException;
        /**
         * Saves the current position offset by the argument.
         * 
         * @return <code>this</code>
         * 
         * @throws IndexOutOfBoundsException <code>|index| &ge; size()</code>
         */
        SequenceIterator mark(long offset) throws IndexOutOfBoundsException;
        
        /**
         * @return The sub-sequence between the last marked position (inclusive) and
         *         the current index (exclusive).
         * 
         * @throws IndexOutOfBoundsException The marked position was not updated
         *                                   following an invocation of a jump method.
         */
        Sequence subSequence() throws IndexOutOfBoundsException,
                                      UncheckedIOException,
                                      SecurityException;
        
        /**
         * Sets the cursor to the specified position.
         * 
         * @return <code>this</code>
         * 
         * @throws IndexOutOfBoundsException <code>|index| &ge; size()</code>
         */
        SequenceIterator jumpTo(int index) throws IndexOutOfBoundsException,
                                                  UncheckedIOException;
        /**
         * Sets the cursor to the specified position.
         * 
         * @return <code>this</code>
         * 
         * @throws IndexOutOfBoundsException <code>|index| &ge; size()</code>
         */
        SequenceIterator jumpTo(long index) throws IndexOutOfBoundsException,
                                                   UncheckedIOException;
        /**
         * Offsets the current position.
         * 
         * @return <code>this</code>
         * 
         * @throws IndexOutOfBoundsException <code>|index()+offset| &ge; size()</code>
         */
        SequenceIterator jumpOffset(int offset) throws IndexOutOfBoundsException,
                                                       UncheckedIOException;
        /**
         * Offsets the current position.
         * 
         * @return <code>this</code>
         * 
         * @throws IndexOutOfBoundsException <code>|index()+offset| &ge; size()</code>
         */
        SequenceIterator jumpOffset(long offset) throws IndexOutOfBoundsException,
                                                        UncheckedIOException;
        
        @Override default void close() throws UncheckedIOException {}
        
        /**
         * Generates a string based on the iterator's current position.
         * 
         * @return A string containing all characters from the starting index
         *         (inclusive) to the current index (exclusive).
         */
        @Override String toString() throws UncheckedIOException;
    }
    
    /**
     * @return A {@linkplain SequenceIterator}
     * 
     * @see java.lang.Iterable#iterator()
     * @see SequenceIterator
     * 
     * @implNote The {@linkplain SequenceIterator} interface extends
     *           {@linkplain AutoCloseable} to ensure that all resources are closed.
     *           Therefore, the return value should be closed after use unless
     *           marked with the {@linkplain NoIO} annotation.
     */
    SequenceIterator forwardIterator() throws UncheckedIOException;
    /**
     * @return An iterator which iterates in the opposite direction as the one
     *         returned by {@linkplain #iterator()}.
     *         
     * @see java.lang.Iterable#iterator()
     * @see SequenceIterator
     * 
     * @implNote The {@linkplain SequenceIterator} interface extends
     *           {@linkplain AutoCloseable} to ensure that all resources are closed.
     *           Therefore, the return value should be closed after use unless
     *           marked with the {@linkplain NoIO} annotation.
     */
    SequenceIterator reverseIterator() throws UncheckedIOException;
    
    /**
     * @return The number of characters in the sequence. This method is a cheap hack
     *         to get around the {@linkplain CharSequence} interface's
     *         {@linkplain #length()} returning an int in cases where the sequence
     *         could actually be larger.
     * 
     * @implSpec This method should be overridden if the size of this sequence is
     *           larger.
     */
    default long size() {return length();}
    
    /**
     * Returns the char value at the specified index.
     * 
     * @param index An index in the range <code>[-size(),size())</code>. Negative
     *              values are wrapped to the end by adding to <code>size()</code>.
     * 
     * @return The character at the specified index.
     * 
     * @throws IndexOutOfBoundsException <code>|index| &ge; size()</code>
     *              
     * @see java.lang.CharSequence#charAt(int)
     */
    @Override char charAt(int index) throws IndexOutOfBoundsException,UncheckedIOException;
    /**
     * Returns the char value at the specified index.
     * 
     * @param index An index in the range <code>[-size(),size())</code>. Negative
     *              values are wrapped to the end by adding to <code>size()</code>.
     * 
     * @return The character at the specified index.
     * 
     * @throws IndexOutOfBoundsException <code>|index| &ge; size()</code>
     *              
     * @see #charAt(int)
     */
    char charAt(final long index) throws IndexOutOfBoundsException,UncheckedIOException;
    
    @Override
    default int compareTo(final CharSequence o) {
        if(o == null) return 1;
        final long oSize;
        try(final SimpleSequenceIterator a = iterator()) {
            if(o instanceof Sequence) {
                try(final SimpleSequenceIterator b = ((Sequence)o).iterator()) {
                    while(a.hasNext() && b.hasNext()) {
                        final int diff = Character.compare(a.next(),b.next());
                        if(diff != 0) return diff;
                    }
                }
                oSize = ((Sequence)o).size();
            } else {
                for(int i = 0;a.hasNext() && i < o.length();++i) {
                    final int diff = Character.compare(a.next(),o.charAt(i));
                    if(diff != 0) return diff;
                }
                oSize = o.length();
            }
        }
        return Long.compare(size(),oSize);
    }
    @Override boolean equals(Object obj);
    
    /**
     * Returns the sequence of characters between the two indices.
     * 
     * @throws IndexOutOfBoundsException The indices represent an invalid range or
     *                                   at least one of the indices satisfies
     *                                   <code>|index| &gt size()</code>.
     * 
     * @see #subSequence(long,long)
     */
    @Override
    Sequence subSequence(int start,int end) throws IndexOutOfBoundsException,
                                                   UncheckedIOException;
    /**
     * Same as {@linkplain #subSequence(int,int)}, but takes long values to account
     * for the size difference.
     * 
     * @param start Index of the first character (inclusive).
     * @param end   Index of the last character (exclusive).
     * 
     * @throws IndexOutOfBoundsException The indices represent an invalid range or
     *                                   at least one of the indices satisfies
     *                                   <code>|index| &gt size()</code>.
     */
    Sequence subSequence(long start,long end) throws IndexOutOfBoundsException,
                                                     UncheckedIOException;
    
    @Override String toString() throws UncheckedIOException;
    /**
     * @implNote In general, using this sequence after this method has been called
     *           yields undefined behavior.
     */
    @Override void close() throws UncheckedIOException;
    
    /**
     * Copies this sequence to the specified character array.
     * 
     * @param arr    A character array to hold the characters.
     * @param offset An index in the range <code>[-arr.length,arr.length)</code>.
     *               Negative values are wrapped to the end by adding to
     *               <code>arr.length</code>.
     * 
     * @return <code>this</code>
     * 
     * @throws IllegalArgumentException The array cannot hold this sequence at the
     *                                  specified offset.
     */
    Sequence copyTo(char[] arr,int offset) throws IllegalArgumentException,
                                                  UncheckedIOException;
    /**Creates a mutable copy of this sequence as a {@linkplain MutableSequence}.*/
    MutableSequence mutableCopy() throws UncheckedIOException;
    /**Creates an immutable copy of this sequence.*/
    Sequence immutableCopy() throws UncheckedIOException;
    /**
     * @return <code>true</code> iff the {@linkplain #close()} method closes all
     *         references to this sequence.
     * 
     * @see #shallowCopy()
     */
    default boolean closeIsShared() {return false;}
    /**
     * Creates a shallow copy of this instance. Using this method is preferred over
     * using a plain copy assignment when {@linkplain #closeIsShared()} returns
     * <code>true</code>.
     */
    default Sequence shallowCopy() throws UncheckedIOException {return this;}
    
    /**A shared instance of an empty sequence.*/
    MutableSequence EMPTY = new MutableSequence() {
        @Override
        public boolean equals(final Object obj) {
            return obj == this || obj instanceof CharSequence && ((CharSequence)obj).isEmpty();
        }
        private void ssoob(final int idx) {
            if(idx != 0)
                throw new IndexOutOfBoundsException(
                    "%d is outside the range [0,0] (shifted: %<d,[0,0])."
                    .formatted(idx)
                );
        }
        private void ssoob(final long idx) {
            if(idx != 0L)
                throw new IndexOutOfBoundsException(
                    "%d is outside the range [0,0] (shifted: %<d,[0,0])."
                    .formatted(idx)
                );
        }
        private IndexOutOfBoundsException oob(final int idx) {
            return new IndexOutOfBoundsException(
                "%d is outside the range <EMPTY> (shifted: %<d,<EMPTY>)."
                .formatted(idx)
            );
        }
        private IndexOutOfBoundsException oob(final long idx) {
            return new IndexOutOfBoundsException(
                "%d is outside the range <EMPTY> (shifted: %<d,<EMPTY>)."
                .formatted(idx)
            );
        }
        @NoIO @Override
        public MutableSequence subSequence(final int start,final int end)
                                           throws IndexOutOfBoundsException {
            ssoob(start); ssoob(end);
            return this;
        }
        @NoIO @Override
        public MutableSequence subSequence(final long start,final long end)
                                           throws IndexOutOfBoundsException {
            ssoob(start); ssoob(end);
            return this;
        }
        @NoIO @Override
        public MutableSequence mutableSubSequence(final int start,final int end)
                                                  throws IndexOutOfBoundsException {
            return subSequence(start,end);
        }
        @NoIO @Override
        public MutableSequence mutableSubSequence(final long start,final long end)
                                                  throws IndexOutOfBoundsException {
            return subSequence(start,end);
        }
        @Override public int length() {return 0;}
        @Override public long size() {return 0L;}
        @Override public boolean isEmpty() {return true;}
        @NoIO @Override
        public char charAt(final int index) throws IndexOutOfBoundsException {
            throw oob(index);
        }
        @NoIO @Override
        public char charAt(final long index) throws IndexOutOfBoundsException {
            throw oob(index);
        }
        
        @NoIO @Override
        public MutableSequence set(final int index,final char c) throws IndexOutOfBoundsException {
            throw oob(index);
        }
        @NoIO @Override
        public MutableSequence set(final long index,final char c) throws IndexOutOfBoundsException {
            throw oob(index);
        }
        @NoIO @Override
        public MutableSequence set(final int index,final char[] c) throws IndexOutOfBoundsException {
            throw oob(index);
        }
        @NoIO @Override
        public MutableSequence set(final long index,final char[] c) throws IndexOutOfBoundsException {
            throw oob(index);
        }
        @NoIO @Override
        public MutableSequence set(final int index,final CharSequence c) throws IndexOutOfBoundsException {
            throw oob(index);
        }
        @NoIO @Override
        public MutableSequence set(final long index,final CharSequence c) throws IndexOutOfBoundsException {
            throw oob(index);
        }
        
        @NoIO
        final class EMPTYITR implements MutableSequenceIterator,SimpleSequenceIterator {
            @NoIO @Override
            public SimpleSequenceIterator skip(final long count) {
                throw oob(count);
            }
            @Override public long index() {return 0L;}
            @Override public long offset() {return 0L;}
            @Override public MutableSequence getParent() {return EMPTY;}
            
            @NoIO @Override public Character peek() {return null;}
            @NoIO @Override public Character peek(final int offset) {return null;}
            @NoIO @Override public Character peek(final long offset) {return null;}
            
            @Override public boolean hasNext() {return false;}
            @NoIO @Override public Character next() {return null;}
            
            @NoIO @Override public Character skipWS() {return null;}
            @NoIO @Override public Character skipWS(final int limit) {return null;}
            @NoIO @Override public Character skipWS(final long limit) {return null;}
            @NoIO @Override public Character peekNonWS() {return null;}
            @NoIO @Override public Character peekNonWS(final int limit) {return null;}
            @NoIO @Override public Character peekNonWS(final long limit) {return null;}
            @NoIO @Override public Character peekNextNonWS() {return null;}
            @NoIO @Override public Character peekNextNonWS(final int limit) {return null;}
            @NoIO @Override public Character peekNextNonWS(final long limit) {return null;}
            @NoIO @Override public Character nextNonWS() {return null;}
            @NoIO @Override public Character nextNonWS(final int limit) {return null;}
            @NoIO @Override public Character nextNonWS(final long limit) {return null;}
            
            @Override public MutableSequenceIterator mark() {return this;}
            @Override
            public MutableSequenceIterator mark(final int offset) throws IndexOutOfBoundsException {
                throw oob(offset);
            }
            @Override
            public MutableSequenceIterator mark(final long offset) throws IndexOutOfBoundsException {
                throw oob(offset);
            }
            
            @NoIO @Override
            public MutableSequenceIterator set(final char c) throws IndexOutOfBoundsException {
                throw oob(0);
            }
            @NoIO @Override
            public MutableSequenceIterator set(final int offset,final char c) throws IndexOutOfBoundsException {
                throw oob(offset);
            }
            @NoIO @Override
            public MutableSequenceIterator set(final long offset,final char c) throws IndexOutOfBoundsException {
                throw oob(offset);
            }
            
            @NoIO @Override public MutableSequence subSequence() {return EMPTY;}
            
            @NoIO @Override
            public MutableSequenceIterator jumpTo(final int index) throws IndexOutOfBoundsException {
                throw oob(index);
            }
            @NoIO @Override
            public MutableSequenceIterator jumpTo(final long index) throws IndexOutOfBoundsException {
                throw oob(index);
            }
            @NoIO @Override
            public MutableSequenceIterator jumpOffset(final int offset) throws IndexOutOfBoundsException {
                throw oob(offset);
            }
            @NoIO @Override
            public MutableSequenceIterator jumpOffset(final long offset) throws IndexOutOfBoundsException {
                throw oob(offset);
            }
            
            @NoIO @Override public void forEachRemaining(final Consumer<? super Character> action) {}
            @NoIO @Override public void close() {}
        }
        final EMPTYITR ITER = new EMPTYITR();
        @NoIO @Override public SimpleSequenceIterator iterator() {return ITER;}
        @NoIO @Override public MutableSequenceIterator forwardIterator() {return ITER;}
        @NoIO @Override public MutableSequenceIterator reverseIterator() {return ITER;}
        
        @NoIO @Override public void close() {}
        @NoIO @Override public String toString() {return "";}
        
        @NoIO @Override
        public MutableSequence copyTo(final char[] arr,final int offset) {return this;}
        
        @NoIO @Override public MutableSequence mutableCopy() {return this;}
        @NoIO @Override public Sequence immutableCopy() {return this;}
    };
    
    static ArraySequenceBuilder arraySequenceBuilder() {return new ArraySequenceBuilder();}
    static FileSequenceBuilder fileSequenceBuilder() {return new FileSequenceBuilder();}
    static CompoundSequenceBuilder compoundSequenceBuilder() {return new CompoundSequenceBuilder();}
    
    static MutableArraySequenceBuilder mutableArraySequenceBuilder() {return new MutableArraySequenceBuilder();}
    static MutableFileSequenceBuilder mutableFileSequenceBuilder() {return new MutableFileSequenceBuilder();}
    static MutableCompoundSequenceBuilder mutableCompoundSequenceBuilder() {return new MutableCompoundSequenceBuilder();}
}