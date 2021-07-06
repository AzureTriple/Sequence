package sequence;

public abstract class IntegerIndexedSequence extends Sequence {
    
    protected int start,end;
    
    public IntegerIndexedSequence(final int start,final int end)
                                  throws IndexOutOfBoundsException {
        this(start,end,DEFAULT_MUTABLE);
    }
    public IntegerIndexedSequence(final int start,final int end,
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
    
    @Override public int length() {return end - start;}
    
    protected int idx(final int idx) throws IndexOutOfBoundsException {
        final int out = idx + (idx < 0? end : start);
        if(end < out || out < start)
            throw new IndexOutOfBoundsException(
                "%d is outside the range [%d,%d) (shifted: %d,[0,%d))."
                .formatted(idx,start,end,out - start,end - start)
            );
        return out;
    }
    
    public interface IntegerIndexedSequenceIterator extends SequenceIterator {
        /**
         * @return The index of the character last returned by
         *         {@linkplain SequenceIterator#next()}, adjusted to the current
         *         sequence's range.
         */
        public int index();
        /**
         * @return The value at the cursor's position offset by the argument without
         *         advancing, or <code>null</code> if
         *         {@linkplain SequenceIterator#next()} hasn't been called for the first
         *         time.
         */
        public Character peek(int offset);
        /**@return Saves the current position offset by the argument.*/
        public void mark(int offset);
        /**
         * Sets the cursor to the specified position.
         * 
         * @return <code>this</code>
         */
        public IntegerIndexedSequenceIterator jumpTo(int index);
        /**
         * Offsets the current position.
         * 
         * @return <code>this</code>
         */
        public IntegerIndexedSequenceIterator jumpOffset(int offset);
    }
    
    @Override public abstract IntegerIndexedSequenceIterator iterator();
    @Override public abstract IntegerIndexedSequenceIterator reverseIterator();
    
}
