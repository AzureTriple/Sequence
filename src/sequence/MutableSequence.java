package sequence;

import java.io.UncheckedIOException;

/**
 * An extension of the {@linkplain Sequence} interface which allows
 * modification.
 * 
 * @author AzureTriple
 */
public interface MutableSequence extends Sequence {
    /**
     * Sets the character at the specified index.
     * 
     * @return <code>this</code>
     * 
     * @throws IndexOutOfBoundsException <code>|index| &ge; size()</code>
     */
    MutableSequence set(int index,char c) throws IndexOutOfBoundsException,
                                                 UncheckedIOException;
    /**
     * Sets the character at the specified index.
     * 
     * @return <code>this</code>
     * 
     * @throws IndexOutOfBoundsException <code>|index| &ge; size()</code>
     */
    MutableSequence set(long index,char c) throws IndexOutOfBoundsException,
                                                  UncheckedIOException;
    
    /**
     * Sets a region of characters starting at the specified offset.
     * 
     * @return <code>this</code>
     * 
     * @throws IndexOutOfBoundsException <code>|offset| + data.length &ge; size()</code>
     */
    MutableSequence set(int offset,char[] data) throws IndexOutOfBoundsException,
                                                       UncheckedIOException;
    /**
     * Sets a region of characters starting at the specified offset.
     * 
     * @return <code>this</code>
     * 
     * @throws IndexOutOfBoundsException <code>|offset| + data.length &ge; size()</code>
     */
    MutableSequence set(long offset,char[] data) throws IndexOutOfBoundsException,
                                                        UncheckedIOException;
    
    /**
     * Sets a region of characters starting at the specified offest.
     * 
     * @return <code>this</code>
     * 
     * @throws IndexOutOfBoundsException <code>|offset| + data.length() &ge; size()</code>
     */
    MutableSequence set(int offset,CharSequence data) throws IndexOutOfBoundsException,
                                                             UncheckedIOException;
    /**
     * Sets a region of characters starting at the specified offest.
     * 
     * @return <code>this</code>
     * 
     * @throws IndexOutOfBoundsException <code>|offset| + data.length() &ge; size()</code>
     */
    MutableSequence set(long offset,CharSequence data) throws IndexOutOfBoundsException,
                                                              UncheckedIOException;
    
    @Override
    MutableSequence subSequence(int start,int end) throws IndexOutOfBoundsException,
                                                          UncheckedIOException;
    @Override
    MutableSequence subSequence(long start,long end) throws IndexOutOfBoundsException,
                                                            UncheckedIOException;
    
    /**
     * Resizes this sequence to the specified bounds.
     * 
     * @param start The starting index, inclusive.
     * @param end   The ending index, exclusive.
     * 
     * @return <code>this</code>
     * 
     * @throws IndexOutOfBoundsException <code>start</code> or <code>end</code> are
     *                                   out of bounds, or the shifted value of
     *                                   <code>end</code> is less than
     *                                   <code>start</code>.
     */
    MutableSequence mutableSubSequence(int start,int end) throws IndexOutOfBoundsException,
                                                                 UncheckedIOException;
    /**
     * Resizes this sequence to the specified bounds.
     * 
     * @param start The starting index, inclusive.
     * @param end   The ending index, exclusive.
     * 
     * @return <code>this</code>
     * 
     * @throws IndexOutOfBoundsException <code>start</code> or <code>end</code> are
     *                                   out of bounds, or the shifted value of
     *                                   <code>end</code> is less than
     *                                   <code>start</code>.
     */
    MutableSequence mutableSubSequence(long start,long end) throws IndexOutOfBoundsException,
                                                                   UncheckedIOException;
    
    /**
     * A {@linkplain SequenceIterator} with added functionality to modify its base
     * sequence.
     */
    interface MutableSequenceIterator extends SequenceIterator {
        @Override MutableSequence getParent();
        
        @Override MutableSequenceIterator mark();
        @Override MutableSequenceIterator mark(int offset) throws IndexOutOfBoundsException;
        @Override MutableSequenceIterator mark(long offset) throws IndexOutOfBoundsException;
        
        @Override MutableSequenceIterator jumpTo(int index) throws IndexOutOfBoundsException,
                                                                   UncheckedIOException;
        @Override MutableSequenceIterator jumpTo(long index) throws IndexOutOfBoundsException,
                                                                    UncheckedIOException;
        @Override MutableSequenceIterator jumpOffset(int offset) throws IndexOutOfBoundsException,
                                                                        UncheckedIOException;
        @Override MutableSequenceIterator jumpOffset(long offset) throws IndexOutOfBoundsException,
                                                                         UncheckedIOException;
        
        /**
         * Sets the character at the cursor's position.
         * 
         * @return <code>this</code>
         * 
         * @throws IndexOutOfBoundsException {@linkplain #hasNext()} returns
         *                                   <code>false</code>.
         */
        MutableSequenceIterator set(char c) throws IndexOutOfBoundsException,
                                                   UncheckedIOException;
        
        /**
         * Sets the character at an offset from the cursor's position.
         * 
         * @return <code>this</code>
         * 
         * @throws IndexOutOfBoundsException <code>cursor + offset</code> is out of
         *                                   bounds.
         */
        MutableSequenceIterator set(int offset,char c) throws IndexOutOfBoundsException,
                                                              UncheckedIOException;
        /**
         * Sets the character at an offset from the cursor's position.
         * 
         * @return <code>this</code>
         * 
         * @throws IndexOutOfBoundsException <code>cursor + offset</code> is out of
         *                                   bounds.
         */
        MutableSequenceIterator set(long offset,char c) throws IndexOutOfBoundsException,
                                                               UncheckedIOException;
        
        @Override MutableSequence subSequence() throws IndexOutOfBoundsException,
                                                       UncheckedIOException;
    }
    @Override MutableSequenceIterator forwardIterator() throws UncheckedIOException;
    @Override MutableSequenceIterator reverseIterator() throws UncheckedIOException;
    
    @Override
    MutableSequence copyTo(char[] arr,int offset) throws IllegalArgumentException,
                                                         IndexOutOfBoundsException,
                                                         UncheckedIOException;
}