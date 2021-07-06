package sequence_old;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import sequence_old.iterator.SequenceIterator;

public abstract class BufferedSequence implements Sequence {
    
    protected final ByteBuffer buf;
    protected final CharBuffer chr;
    
    protected int start,end;
    
    public BufferedSequence(final ByteBuffer buf,final int start,final int end)
                        throws NullPointerException,IndexOutOfBoundsException {
        if((this.end = end) < (this.start = start))
            throw new IndexOutOfBoundsException(
                "%d > %d".formatted(start,end)
            );
        chr = (this.buf = buf).asCharBuffer();
    }
    
    @Override public int start() {return start;}
    @Override public int end() {return end;}
    
    @Override public char charAt(final int index) {return chr.charAt(idx(index));}
    
    @Override
    public boolean isWrappedIn(final char wrap) {
        return chr.charAt(start()) == wrap && chr.charAt(end()) == wrap;
    }
    
    @Override
    public ForwardSequenceIterator iterator() {
        return new ForwardSequenceIterator();
    }
    @Override
    public ReverseSequenceIterator reverseIterator() {
        return new ReverseSequenceIterator();
    }
    private abstract class BufferedSequenceIterator implements SequenceIterator {
        protected int cursor,mark;
        protected BufferedSequenceIterator(final int begin) {
            mark = cursor = begin;
        }
        /**
         * @return The value at the cursor without advancing, or <code>null</code> if
         *         {@linkplain SequenceIterator#next()} hasn't been called for the first
         *         time.
         */
        public abstract Character peek();
        /**
         * @return The value at the cursor's position offset by the argument without
         *         advancing, or <code>null</code> if
         *         {@linkplain SequenceIterator#next()} hasn't been called for the first
         *         time.
         */
        public abstract Character peek(final int offset);
        /**
         * @return The next non-whitespace character without advancing, or
         *         <code>null</code> if the end is reached.
         */
        public abstract Character peekNextNonWS();
        /**
         * @return The index of the character last returned by
         *         {@linkplain SequenceIterator#next()}, adjusted to the current
         *         sequence's range.
         */
        public int index() {return cursor - start;}
        /**@return The next non-whitespace character, or <code>null</code> if the end is reached.*/
        public abstract Character nextNonWS();
        
        protected abstract boolean rangeCheck();
        protected abstract void advanceFirst();
        /**@return The closest non-whitespace character, or <code>null</code> if the end is reached.*/
        public Character skipWS() {
            if(rangeCheck()) return null;
            advanceFirst();
            if(!Character.isWhitespace(charAt(cursor))) return charAt(cursor);
            return nextNonWS();
        }
        /**@return The sequence being iterated over.*/
        public BufferedSequence getParent() {return BufferedSequence.this;}
        /**@return Saves the current position.*/
        public void mark() {mark = cursor;}
        protected int offset(final int offset) {return Math.max(Math.min(cursor + offset,end),start);}
        /**@return Saves the current position offset by the argument.*/
        public void mark(final int offset) {mark = offset(offset);}
        @Override public BufferedSequence parent() {return BufferedSequence.this;}
        /**@return The sub-sequence between the first marked position to the current index.*/
        public abstract BufferedSequence subSequence();
        /**Sets the cursor to the specified position.*/
        public BufferedSequenceIterator jumpTo(final int index) {cursor = idx(index); return this;}
        /**Offsets the current position.*/
        public BufferedSequenceIterator jumpOffset(final int offset) {cursor += offset; return this;}
    }
    /**A {@linkplain SequenceIterator} which iterates from start to end.*/
    public class ForwardSequenceIterator extends BufferedSequenceIterator {
        private ForwardSequenceIterator() {super(start - 1);}
        @Override public boolean hasNext() {return cursor < end - 1;}
        @Override public Character next() {return ++cursor < end? charAt(cursor) : null;}
        @Override public Character peek() {return cursor < start || cursor >= end? null : charAt(cursor);}
        @Override public Character peek(final int offset) {return cursor < start? null : charAt(offset(offset));}
        @Override
        public boolean rangeCheck() {return cursor >= end;}
        @Override
        public void advanceFirst() {if(cursor == start - 1) ++cursor;}
        @Override
        public Character peekNextNonWS() {
            int temp = cursor;
            while(++temp < end && Character.isWhitespace(charAt(temp)));
            return temp < start || temp >= end? null : charAt(temp);
        }
        @Override
        public Character nextNonWS() {
            while(++cursor < end && Character.isWhitespace(charAt(cursor)));
            return cursor < end? charAt(cursor) : null;
        }
        @Override
        public BufferedSequence subSequence() {
            return (BufferedSequence)BufferedSequence.this.subSequence(mark,cursor);
        }
        
        @Override
        public String toString() {
            return BufferedSequence.this.subSequence(cursor,end).toString();
        }
    }
    /**A {@linkplain SequenceIterator} which iterates from end to start.*/
    public class ReverseSequenceIterator extends BufferedSequenceIterator {
        private ReverseSequenceIterator() {super(end);}
        @Override public boolean hasNext() {return cursor > start;}
        @Override public Character next() {return --cursor >= start? charAt(cursor) : null;}
        @Override public Character peek() {return cursor == end? null : charAt(cursor);}
        @Override public Character peek(final int offset) {return cursor == end? null : charAt(offset(offset));}
        @Override
        public boolean rangeCheck() {return cursor < start;}
        @Override
        public void advanceFirst() {if(cursor == end) --cursor;}
        @Override
        public Character peekNextNonWS() {
            int temp = cursor;
            while(temp > start && Character.isWhitespace(charAt(--temp)));
            return temp == end || temp < start? null : charAt(temp);
        }
        @Override
        public Character nextNonWS() {
            while(cursor > start && Character.isWhitespace(charAt(--cursor)));
            return cursor == end || cursor < start? null : charAt(cursor);
        }
        @Override
        public BufferedSequence subSequence() {
            return (BufferedSequence)BufferedSequence.this.subSequence(cursor,mark);
        }
        
        @Override
        public String toString() {
            return BufferedSequence.this.subSequence(start,cursor - start + 1).toString();
        }
    }
}




























