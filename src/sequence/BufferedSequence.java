package sequence;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

public class BufferedSequence extends IntegerIndexedSequence {
    
    private ByteBuffer buf;
    private CharBuffer chr;
    
    protected BufferedSequence(final ByteBuffer buf,final CharBuffer chr,
                               final int start,final int end,
                               final boolean mutable)
                               throws IndexOutOfBoundsException {
        super(start,end,mutable);
        this.buf = buf;
        this.chr = chr;
    }
    public BufferedSequence(final ByteBuffer buf,
                            final int start,final int end,
                            final boolean mutable)
                            throws IndexOutOfBoundsException {
        this(buf,buf.asCharBuffer(),start,end,mutable);
    }
    public BufferedSequence(final ByteBuffer buf,
                            final int start,final int end)
                            throws IndexOutOfBoundsException {
        this(buf,buf.asCharBuffer(),start,end,DEFAULT_MUTABLE);
    }
    
    @Override
    public char charAt(final int index) throws IndexOutOfBoundsException {
        return chr.charAt(idx(index));
    }
    
    @Override
    public BufferedSequence subSequence(int start,int end) {
        if(this.start < (start = idx(start)))
            throw new IndexOutOfBoundsException(
                "Invalid start index: %d"
                .formatted(start)
            );
        if(this.end > (end = idx(end)))
            throw new IndexOutOfBoundsException(
                "Invalid end index: %d"
                .formatted(end)
            );
        if(mutable) {
            this.start = start;
            this.end = end;
            return this;
        }
        return new BufferedSequence(buf,chr,start,end,false);
    }
    
    /**Buffered Sequence Iterator*/
    private abstract class BSI implements IntegerIndexedSequenceIterator {
        protected int cursor,mark;
        protected BSI(final int begin) {mark = cursor = begin;}
        
        @Override public Character peek() {return start <= cursor && cursor < end? chr.charAt(cursor) : null;}
        
        /**
         * @return The index of the character last returned by
         *         {@linkplain SequenceIterator#next()}, adjusted to the current
         *         sequence's range.
         */
        public int index() {return cursor - start;}
        
        protected abstract boolean rangeCheck();
        protected abstract void advanceFirst();
        
        /**
         * @return The closest non-whitespace character, or <code>null</code> if the end
         *         is reached.
         */
        public Character skipWS() {
            if(rangeCheck()) return null;
            advanceFirst();
            {
                final char c = chr.charAt(cursor);
                if(!Character.isWhitespace(c)) return c;
            }
            return nextNonWS();
        }
        /**@return The sequence being iterated over.*/
        public BufferedSequence getParent() {return BufferedSequence.this;}
        /**@return Saves the current position.*/
        public void mark() {mark = cursor;}
        protected int offset(final int offset) {return Math.max(Math.min(cursor + offset,end),start);}
        /**@return Saves the current position offset by the argument.*/
        public abstract void mark(int offset);
        /**Sets the cursor to the specified position.*/
        public IntegerIndexedSequenceIterator jumpTo(final int index) {cursor = idx(index); return this;}
        /**Offsets the current position in the direction of iteration.*/
        public abstract IntegerIndexedSequenceIterator jumpOffset(int offset);
    }
    /**Forward Buffered Sequence Iterator*/
    private class FBSI extends BSI {
        protected FBSI() {super(start - 1);}
        
        @Override public boolean hasNext() {return cursor + 1 < end;}
        @Override
        public Character next() {return hasNext()? chr.charAt(++cursor) : null;}
        @Override
        public Character peek(int offset) {
            offset += cursor;
            return start <= offset && offset < end? chr.charAt(offset) : null;
        }
        @Override protected boolean rangeCheck() {return cursor >= end;}
        @Override protected void advanceFirst() {if(cursor == start - 1) ++cursor;}
        @Override public FBSI jumpOffset(final int offset) {cursor = offset(offset); return this;}
        @Override
        public Character peekNextNonWS() {
            if(hasNext())
                for(int tmp = cursor;++tmp < end;) {
                    final char c = chr.charAt(tmp);
                    if(!Character.isWhitespace(c))
                        return c;
                }
            return null;
        }
        @Override
        public Character nextNonWS() {
            if(hasNext())
                while(++cursor < end) {
                    final char c = chr.charAt(cursor);
                    if(!Character.isWhitespace(c))
                        return c;
                }
            return null;
        }
        @Override public void mark(final int offset) {mark = offset(offset);}
        @Override
        public BufferedSequence subSequence() {
            return BufferedSequence.this.subSequence(mark,cursor);
        }
        @Override
        public String toString() {
            return BufferedSequence.this.subSequence(Math.max(cursor,start),end).toString();
        }
    }
    /**Reverse Buffered Sequence Iterator*/
    private class RBSI extends BSI {
        protected RBSI() {super(end);}
        
        @Override public boolean hasNext() {return cursor > start;}
        @Override public Character next() {return hasNext()? chr.charAt(--cursor) : null;}
        @Override
        public Character peek(int offset) {
            offset = cursor - offset;
            return start <= offset && offset < end? chr.charAt(offset) : null;
        }
        @Override protected boolean rangeCheck() {return cursor < start;}
        @Override protected void advanceFirst() {if(cursor == end) --cursor;}
        @Override public RBSI jumpOffset(final int offset) {cursor = offset(-offset); return this;}
        public Character peekNextNonWS() {
            if(hasNext())
                for(int tmp = cursor;--tmp >= start;) {
                    final char c = chr.charAt(tmp);
                    if(!Character.isWhitespace(c))
                        return c;
                }
            return null;
        }
        @Override
        public Character nextNonWS() {
            if(hasNext())
                while(--cursor >= start) {
                    final char c = chr.charAt(cursor);
                    if(!Character.isWhitespace(c))
                        return c;
                }
            return null;
        }
        @Override public void mark(final int offset) {mark = offset(-offset);}
        @Override
        public BufferedSequence subSequence() {
            return BufferedSequence.this.subSequence(cursor,mark);
        }
        @Override
        public String toString() {
            return BufferedSequence.this.subSequence(start,cursor).toString();
        }
    }
    @Override public IntegerIndexedSequenceIterator iterator() {return new FBSI();}
    @Override public IntegerIndexedSequenceIterator reverseIterator() {return new RBSI();}
}