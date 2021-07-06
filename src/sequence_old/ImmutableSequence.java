package sequence_old;

import java.util.Iterator;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

public class ImmutableSequence extends BufferedSequence {
    
    public ImmutableSequence(final ByteBuffer buf,final int start,final int end)
                             throws IndexOutOfBoundsException {
        super(buf,start,end);
    }
    
    @Override
    public ImmutableSequence subSequence(final int start,final int end)
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
        if(nStart <= nEnd)
            throw new IndexOutOfBoundsException(
                "%d <= %d".formatted(nStart,nEnd)
            );
        return new ImmutableSequence(buf,nStart,nEnd);
    }
    @Override
    public ImmutableSequence subSequence(final int start)
                                       throws IndexOutOfBoundsException {
        final int nStart = idx(start);
        if(this.start > nStart)
            throw new IndexOutOfBoundsException(
                "%d > %d".formatted(this.start,nStart)
            );
        if(nStart <= end)
            throw new IndexOutOfBoundsException(
                "%d <= %d".formatted(nStart,end)
            );
        return new ImmutableSequence(buf,nStart,end);
    }
    
    @Override
    public ImmutableSequence repeat(final int count) {
        final ByteBuffer bb = ByteBuffer.allocate(buf.capacity() * count);
        final CharBuffer cb = bb.asCharBuffer(),ss = chr.subSequence(start,end);
        for(int i = 0;i < count;++i)
            cb.append(ss);
        return new ImmutableSequence(bb,0,cb.length());
    }
    
    @Override
    public ImmutableSequence stripLeading() {
        int idx = 0;
        for(final char c : this) {
            if(!Character.isWhitespace(c)) break;
            ++idx;
        }
        return new ImmutableSequence(buf,start + idx,end);
    }
    
    @Override
    public ImmutableSequence stripTailing() {
        int idx = 0;
        for(
            final Iterator<Character> i = reverseIterator();
            i.hasNext() && Character.isWhitespace(i.next());
        ) ++idx;
        return new ImmutableSequence(buf,start,end - idx);
    }
    
    @Override
    public ImmutableSequence strip() {
        int a = 0;
        for(final char c : this) {
            if(!Character.isWhitespace(c)) break;
            ++a;
        }
        int b = 0;
        if(a != length()) {
            for(
                final Iterator<Character> i = reverseIterator();
                i.hasNext() && Character.isWhitespace(i.next());
            ) ++b;
        }
        return new ImmutableSequence(buf,start + a,end - b);
    }
}