package sequence_old;

/**
 * A {@linkplain Sequence} which uses 32-bit integers.
 * 
 * @author AzureTriple
 */
public abstract class IntegerIndexedSequence extends Sequence {
    protected int start,end; // Indices are measured in characters (2 bytes / char).
    
    /**
     * Creates an integer-indexed sequence.
     * 
     * @param start   Index of the first character (inclusive).
     * @param end     Index of the last character (exclusive).
     * @param mutable <code>false</code> iff functions that change the start and end
     *                indices apply modifications to a new object.
     * 
     * @throws IndexOutOfBoundsException The indices represent an invalid range.
     */
    protected IntegerIndexedSequence(final int start,final int end,
                                     final boolean mutable)
                                     throws IndexOutOfBoundsException {
        super(mutable);
        if(end < start || start < 0) // condition failing implies end >= 0
            throw new IndexOutOfBoundsException(
                "The range [%d,%d) is invalid."
                .formatted(start,end)
            );
        this.start = start;
        this.end = end;
    }
    /**
     * Creates an immutable integer-indexed sequence.
     * 
     * @param start Index of the first character (inclusive).
     * @param end   Index of the last character (exclusive).
     * 
     * @throws IndexOutOfBoundsException The indices represent an invalid range.
     */
    protected IntegerIndexedSequence(final int start,final int end)
                                     throws IndexOutOfBoundsException {
        this(start,end,DEFAULT_MUTABLE);
    }
    
    @Override public int length() {return end - start;}
    
    /**
     * @param idx   The index of the desired character. Negative values indicate an
     *              offset from the end instead of the start.
     * @param start The index of the first character (inclusive).
     * @param end   The index of the last character (exclusive).
     * 
     * @return The adjusted index.
     * 
     * @throws IndexOutOfBoundsException <code>|idx| &gt (end - start)</code>
     */
    protected static int idx(final int idx,final int start,final int end)
                             throws IndexOutOfBoundsException {
        final int out = idx + (idx < 0? end : start);
        if(end <= out || out < start)
            throw new IndexOutOfBoundsException(
                "%d is outside the range [%d,%d) (shifted: %d,[0,%d))."
                .formatted(idx,start,end,out - start,end - start)
            );
        return out;
    }
    /**
     * @param idx The index of the desired character. Negative values indicate an
     *            offset from the end instead of the start.
     * 
     * @return The adjusted index.
     * 
     * @throws IndexOutOfBoundsException <code>|idx| &gt size()</code>
     */
    protected int idx(final int idx) throws IndexOutOfBoundsException {return idx(idx,start,end);}
    
    /**
     * A {@linkplain SequenceIterator} view of the characters wrapped in a sequence
     * indexed with 32-bit integers.
     */
    public abstract class IntegerIndexedSequenceIterator implements SequenceIterator {
        protected final int viewStart = start,viewEnd = end;
        protected int cursor,mark; // Indices measured in characters.
        
        protected IntegerIndexedSequenceIterator(final int begin) {cursor = mark = begin;}
        
        protected boolean oob(final int index) {return viewEnd <= index || index < viewStart;}
        protected final int bound(final int index) throws IndexOutOfBoundsException {
            if(oob(index))
                throw new IndexOutOfBoundsException(
                    "%d is outside the range [%d,%d] (shifted: %d,[0,%d])."
                    .formatted(index,viewStart,viewEnd,index - viewStart,viewEnd - viewStart)
                );
            return index;
        }
        protected abstract int offset(int offset);
        protected abstract void increment();
        
        @Override public long index() {return cursor - viewStart;}
        @Override public IntegerIndexedSequence getParent() {return IntegerIndexedSequence.this;}
        
        @Override public Character peek(final long offset) {return peek((int)offset);}
        
        @Override public void mark() {mark = cursor;}
        @Override
        public void mark(final int offset) throws IndexOutOfBoundsException {
            mark = bound(offset(offset));
        }
        @Override public void mark(final long offset) throws IndexOutOfBoundsException {mark((int)mark);}
        
        @Override
        public IntegerIndexedSequenceIterator jumpTo(final int index) throws IndexOutOfBoundsException {
            cursor = idx(index,viewStart,viewEnd);
            return this;
        }
        @Override
        public IntegerIndexedSequenceIterator jumpTo(final long index) throws IndexOutOfBoundsException {
            return jumpTo((int)index);
        }
        @Override
        public IntegerIndexedSequenceIterator jumpOffset(final int offset) throws IndexOutOfBoundsException {
            cursor = bound(offset(offset));
            return this;
        }
        @Override
        public IntegerIndexedSequenceIterator jumpOffset(final long offset) throws IndexOutOfBoundsException {
            return jumpOffset((int)offset);
        }
    }
}