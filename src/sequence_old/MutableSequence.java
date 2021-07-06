package sequence_old;

import java.util.Iterator;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

public class MutableSequence extends BufferedSequence {
    
    public MutableSequence(final ByteBuffer buf,final int start,final int end)
                           throws IndexOutOfBoundsException {
        super(buf,start,end);
    }
    
    @Override
    public MutableSequence subSequence(final int start,final int end)
                                       throws IndexOutOfBoundsException {
        final int nStart = idx(start),nEnd = idx(end);
        if(this.start > nStart)
            throw new IndexOutOfBoundsException(
                "%d > %d".formatted(this.start,nStart)
            );
        if(this.end < nEnd)
            throw new IndexOutOfBoundsException(
                "%d > %d".formatted(this.end,nEnd)
            );
        if((this.start = nStart) <= (this.end = nEnd))
            throw new IndexOutOfBoundsException(
                "%d <= %d".formatted(nStart,nEnd)
            );
        return this;
    }
    @Override
    public MutableSequence subSequence(final int start)
                                       throws IndexOutOfBoundsException {
        final int nStart = idx(start);
        if(this.start > nStart)
            throw new IndexOutOfBoundsException(
                "%d > %d".formatted(this.start,nStart)
            );
        if((this.start = nStart) <= end)
            throw new IndexOutOfBoundsException(
                "%d <= %d".formatted(nStart,end)
            );
        return this;
    }
    
    @Override
    public MutableSequence repeat(final int count) {
        final ByteBuffer bb = ByteBuffer.allocate(buf.capacity() * count);
        final CharBuffer cb = bb.asCharBuffer(),ss = chr.subSequence(start,end);
        for(int i = 0;i < count;++i)
            cb.append(ss);
        return new MutableSequence(bb,0,cb.length());
    }
    
    @Override
    public MutableSequence stripLeading() {
        int idx = 0;
        for(final char c : this) {
            if(!Character.isWhitespace(c)) break;
            ++idx;
        }
        start += idx;
        return this;
    }
    
    @Override
    public MutableSequence stripTailing() {
        int idx = 0;
        for(
            final Iterator<Character> i = reverseIterator();
            i.hasNext() && Character.isWhitespace(i.next());
        ) ++idx;
        end -= idx;
        return this;
    }
    
    @Override
    public MutableSequence strip() {
        int a = 0;
        for(final char c : this) {
            if(!Character.isWhitespace(c)) break;
            ++a;
        }
        if(a != length()) {
            int b = 0;
            for(
                final Iterator<Character> i = reverseIterator();
                i.hasNext() && Character.isWhitespace(i.next());
            ) ++b;
            end -= b;
        }
        start += a;
        return this;
    }
}