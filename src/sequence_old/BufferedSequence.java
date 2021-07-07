package sequence_old;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * A {@linkplain IntegerIndexedSequence} with data stored in a {@linkplain ByteBuffer}.
 * 
 * @author AzureTriple
 */
public class BufferedSequence extends IntegerIndexedSequence {//TODO clone buffer on sub-sequence and iterator
    private ByteBuffer buf;
    private CharBuffer chr; // A CharBuffer view of 'buf'
    
    // Constructor which bypasses checks.
    protected BufferedSequence(final ByteBuffer buf,final CharBuffer chr,
                               final int start,final int end,
                               final boolean mutable)
                               throws IndexOutOfBoundsException {
        super(start,end,mutable);
        this.buf = buf;
        this.chr = chr;
    }
    /**
     * 
     * @param buf
     * @param start
     * @param end
     * @param mutable
     * @throws IndexOutOfBoundsException
     */
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
    
    protected BufferedSequence internalSS(final int start,final int end) {
        if(mutable) {
            this.start = start;
            this.end = end;
            return this;
        }
        return new BufferedSequence(buf,chr,start,end,false);
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
    public abstract class BufferedSequenceIterator extends IntegerIndexedSequenceIterator {
        protected final ByteBuffer viewBuf = buf;
        protected final CharBuffer viewData = chr;
        
        protected BufferedSequenceIterator(final int begin) {super(begin);}
        
        @Override public BufferedSequence getParent() {return BufferedSequence.this;}
        
        @Override public Character peek() {return oob(cursor)? null : viewData.charAt(cursor);}
        @Override
        public Character peek(int offset) {
            return oob(offset = offset(offset))? null : viewData.charAt(offset);
        }
        
        @Override
        public Character next() {
            if(!oob(cursor)) {
                final char c = viewData.charAt(cursor);
                increment();
                return c;
            }
            return null;
        }
        
        @Override
        public Character skipWS() {
            if(!oob(cursor)) {
                do {
                    final char c = viewData.charAt(cursor);
                    if(!Character.isWhitespace(c)) return c;
                    increment();
                } while(hasNext());
            }
            return null;
        }
        @Override
        public Character nextNonWS() {
            if(!oob(cursor)) {
                increment();
                while(hasNext()) {
                    final char c = viewData.charAt(cursor);
                    if(!Character.isWhitespace(c)) return c;
                    increment();
                }
            }
            return null;
        }
    }
    /**Forward Buffered Sequence Iterator*/
    private class FBSI extends BufferedSequenceIterator {
        protected FBSI() {super(start);}
        
        @Override protected int offset(final int offset) {return cursor + offset;}
        @Override protected void increment() {++cursor;}
        
        @Override public boolean hasNext() {return cursor != viewEnd;}
        
        @Override
        public Character peekNextNonWS() {
            if(!oob(cursor + 1)) {
                for(int tmp = cursor;++tmp != viewEnd;) {
                    final char c = viewData.charAt(tmp);
                    if(!Character.isWhitespace(c)) return c;
                }
            }
            return null;
        }
        
        @Override
        public BufferedSequence subSequence() {
            return new BufferedSequence(viewBuf,viewData,mark,cursor,mutable);
        }
    }
    /**Reverse Buffered Sequence Iterator*/
    private class RBSI extends BufferedSequenceIterator {
        protected RBSI() {super(end - 1);}
        
        @Override protected int offset(final int offset) {return cursor - offset;}
        @Override protected void increment() {--cursor;}
        
        @Override public boolean hasNext() {return cursor != start - 1;}
        
        @Override
        public Character peekNextNonWS() {
            if(!oob(cursor - 1)) {
                for(int tmp = cursor;--tmp != viewStart;) {
                    final char c = viewData.charAt(tmp);
                    if(!Character.isWhitespace(c)) return c;
                }
            }
            return null;
        }
        
        @Override
        public BufferedSequence subSequence() {
            return new BufferedSequence(viewBuf,viewData,cursor,mark,mutable);
        }
    }
    
    @Override public BufferedSequenceIterator iterator() {return new FBSI();}
    @Override public BufferedSequenceIterator reverseIterator() {return new RBSI();}
}