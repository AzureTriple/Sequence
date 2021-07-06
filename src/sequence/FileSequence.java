package sequence;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import sequence.Sequence.SequenceIterator;

public class FileSequence extends Sequence {
    
    private RandomAccessFile data;
    private long start,end;
    
    protected FileSequence(final long start,final long end,
                           final RandomAccessFile data,
                           final boolean mutable) {
        super(mutable);
        this.data = data;
        this.start = start;
        this.end = end;
    }
    public FileSequence(final RandomAccessFile data,
                        final long start,final long end,
                        final boolean mutable)
                        throws IndexOutOfBoundsException,
                               NullPointerException {
        super(mutable);
        if(end < start || start < 0L) // condition failing implies end >= 0
            throw new IndexOutOfBoundsException(
                "The range [%d,%d) is invalid."
                .formatted(start,end)
            );
        if((end - start) % 2 != 0)
            throw new IndexOutOfBoundsException(
                "The range [%d,%d) does not contain an even number of bytes."
                .formatted(start,end)
            );
        if((this.data = data) == null)
            throw new NullPointerException("Input file is null.");
        this.start = start;
        this.end = end;
    }
    public FileSequence(final RandomAccessFile data,
                        final long start,final long end)
                        throws IndexOutOfBoundsException,
                               NullPointerException {
        this(data,start,end,DEFAULT_MUTABLE);
    }
    
    @Override public long size() {return (end - start) >>> 1;}
    
    /**
     * This function should not be used instead of {@linkplain #size()}; this
     * method returns the same value but upper-bounded by
     * {@linkplain Integer#MAX_VALUE}. This method was only included to satisfy
     * {@linkplain CharSequence}.
     */
    @Override
    public int length() {
        try {return Math.toIntExact(size());}
        catch(final ArithmeticException e) {return Integer.MAX_VALUE;}
    }
    
    protected long idx(final long idx) throws IndexOutOfBoundsException {
        final long out = (idx << 1) + (idx < 0L? end : start);
        if(end < out || out < start)
            throw new IndexOutOfBoundsException(
                "%d is outside the range [%d,%d) (shifted: %d,[0,%d))."
                .formatted(idx,start,end,out - start,end - start)
            );
        return out;
    }
    
    protected FileSequence internalSS(final long start,final long end) {
        if(mutable) {
            this.start = start;
            this.end = end;
            return this;
        }
        return new FileSequence(start,end,data,mutable);
    }
    /**
     * Same as {@linkplain #subSequence(int,int)}, but takes long values to account
     * for the size difference.
     */
    public FileSequence subSequence(final long start,final long end) throws IndexOutOfBoundsException {
        return internalSS(idx(start),idx(end));
    }
    /**@see #subSequence(long,long)*/
    @Override
    public FileSequence subSequence(final int start,final int end) {
        return subSequence((long)start,(long)end);
    }
    
    /**
     * Same as {@linkplain #charAt(int,int)}, but takes long values to account for
     * the size difference.
     */
    public char charAt(final long index) throws IndexOutOfBoundsException,
                                                UncheckedIOException {
        try {data.seek(idx(index)); return data.readChar();}
        catch(final IOException e) {throw new UncheckedIOException(e);}
    }
    /**@see #charAt(long)*/
    @Override
    public char charAt(final int index) throws IndexOutOfBoundsException,
                                               UncheckedIOException {
        return charAt((long)index);
    }
    
    public abstract class FileSequenceIterator implements SequenceIterator {
        protected long cursor,mark;
        protected FileSequenceIterator(final long begin) {cursor = mark = begin;}
        
        @Override public FileSequence getParent() {return FileSequence.this;}
        
        protected char seek(final long pos) throws UncheckedIOException {
            try {data.seek(pos); return data.readChar();}
            catch(final IOException e) {throw new UncheckedIOException(e);}
        }
        
        /**
         * @return The index of the character last returned by
         *         {@linkplain SequenceIterator#next()}, adjusted to the current
         *         sequence's range.
         */
        public long index() {return (end - cursor) / 2L;}
        @Override
        public Character peek() throws UncheckedIOException {
            return start <= cursor && cursor < end? seek(cursor) : null;
        }
        /**
         * @return The value at the cursor's position offset by the argument without
         *         advancing, or <code>null</code> if
         *         {@linkplain #next()} hasn't been called for the first
         *         time.
         */
        public abstract Character peek(long offset) throws UncheckedIOException;
        
        protected long offset(final long offset) {return Math.max(Math.min(cursor + offset * 2L,end),start);}
        
        @Override public void mark() {mark = cursor;}
        /**@return Saves the current position offset by the argument.*/
        public abstract void mark(long offset);
        /**
         * Sets the cursor to the specified position.
         * 
         * @return <code>this</code>
         */
        public FileSequenceIterator jumpTo(final long index) throws IndexOutOfBoundsException {
            cursor = idx(index);
            return this;
        }
        /**
         * Offsets the current position.
         * 
         * @return <code>this</code>
         */
        public abstract FileSequenceIterator jumpOffset(long offset);
    }
    /**Forward File Sequence Iterator*/
    private class FFSI extends FileSequenceIterator {
        protected FFSI() {super(start);}
        
        @Override public boolean hasNext() {return cursor + 2L < end;}
        @Override
        public Character next() throws UncheckedIOException {
            return hasNext()? seek(cursor += 2L) : null;
        }
        @Override
        public Character peek(long offset) throws UncheckedIOException {
            offset = offset(offset);
            return start <= offset && offset < end? seek(offset) : null;
        }
        
        @Override
        public Character skipWS() throws UncheckedIOException {
            if(cursor < end) {
                try {
                    data.seek(cursor);
                    do {
                        final char c = data.readChar();
                        if(!Character.isWhitespace(c)) return c;
                    } while((cursor += 2L) < end);
                } catch(final IOException e) {throw new UncheckedIOException(e);}
            }
            return null;
        }
        @Override
        public Character peekNextNonWS() throws UncheckedIOException {
            if(hasNext()) {
                try {
                    long tmp = cursor + 2L;
                    data.seek(tmp);
                    do {
                        final char c = data.readChar();
                        if(!Character.isWhitespace(c)) return c;
                    } while((tmp += 2L) < end);
                } catch(final IOException e) {throw new UncheckedIOException(e);}
            }
            return null;
        }
        @Override
        public Character nextNonWS() throws UncheckedIOException {
            if(hasNext()) {
                try {
                    data.seek(cursor += 2L);
                    do {
                        final char c = data.readChar();
                        if(!Character.isWhitespace(c)) return c;
                    } while((cursor += 2L) < end);
                } catch(final IOException e) {throw new UncheckedIOException(e);}
            }
            return null;
        }
        
        @Override public void mark(final long offset) {mark = offset(offset);}
        
        @Override
        public FileSequence subSequence() {
            return FileSequence.this.internalSS(mark,cursor);
        }
        
        @Override
        public FFSI jumpOffset(final long offset) {
            cursor = offset(offset);
            return this;
        }
    }
    /**Reverse File Sequence Iterator*/
    private class RFSI extends FileSequenceIterator {
        protected RFSI() {super(end);}
        
        @Override public boolean hasNext() {return cursor > start;}
        @Override
        public Character next() throws UncheckedIOException {
            return hasNext()? seek(cursor -= 2L) : null;
        }
        @Override
        public Character peek(long offset) throws UncheckedIOException {
            offset = offset(-offset);
            return start <= offset && offset < end? seek(offset) : null;
        }
        
        @Override
        public Character skipWS() throws UncheckedIOException {
            while(cursor >= start) {
                final char c = seek(cursor);
                if(!Character.isWhitespace(c)) return c;
                cursor -= 2L;
            }
            return null;
        }
        @Override
        public Character peekNextNonWS() throws UncheckedIOException {
            if(hasNext()) {
                for(long tmp = cursor - 2L;tmp >= start;tmp -= 2L) {
                    final char c = seek(tmp);
                    if(!Character.isWhitespace(c)) return c;
                }
            }
            return null;
        }
        @Override
        public Character nextNonWS() throws UncheckedIOException {
            while(cursor > start) {
                final char c = seek(cursor -= 2L);
                if(!Character.isWhitespace(c)) return c;
            }
            return null;
        }
        
        @Override public void mark(final long offset) {mark = offset(-offset);}
        
        @Override
        public FileSequence subSequence() {
            return FileSequence.this.internalSS(Math.max(cursor,start),mark);
        }
        
        @Override
        public RFSI jumpOffset(final long offset) {
            cursor = offset(-offset);
            return this;
        }
    }
    
    @Override public FileSequenceIterator iterator() {return new FFSI();}
    @Override public FileSequenceIterator reverseIterator() {return new RFSI();}
}


































