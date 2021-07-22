package sequence;

import java.io.UncheckedIOException;
import util.NoIO;

/**
 * An {@linkplain ArraySequence} which implements the
 * {@linkplain MutableSequence} interface.
 * 
 * @author AzureTriple
 */
@NoIO
class MutableArraySequence extends ArraySequence implements MutableSequence {
    MutableArraySequence(final char[] data,
                         final int start,
                         final int end,
                         final int length) {
        super(data,start,end,length);
    }
    
    @NoIO @Override
    public MutableSequence set(final int index,final char c)
                               throws IndexOutOfBoundsException {
        data[idx(index)] = c;
        return this;
    }
    @NoIO @Override
    public MutableSequence set(final long index,final char c)
                               throws IndexOutOfBoundsException {
        return set((int)index,c);
    }
    @NoIO @Override
    public MutableSequence set(int offset,final char[] data)
                               throws IndexOutOfBoundsException {
        if((offset = idx(offset)) + data.length > length + start)
            throw new IndexOutOfBoundsException(
                "Input string of size %d is too large to set at index %d with sequence length %d."
                .formatted(data.length,offset - start,length)
            );
        System.arraycopy(data,0,this.data,offset,data.length);
        return this;
    }
    @NoIO @Override
    public MutableSequence set(final long offset,final char[] data)
                               throws IndexOutOfBoundsException {
        return set((int)offset,data);
    }
    @Override
    public MutableSequence set(int offset,final CharSequence data)
                               throws IndexOutOfBoundsException,
                                      UncheckedIOException {
        if(data instanceof Sequence)
            ((Sequence)data).copyTo(this.data,idx(offset));
        else {
            final int dl = data.length();
            if((offset = idx(offset)) + dl > length + start)
                throw new IndexOutOfBoundsException(
                    "Input string of size %d is too large to set at index %d with sequence length %d."
                    .formatted(dl,offset - start,length)
                );
            for(int i = offset,j = 0;j < dl;++i,++j)
                this.data[i] = data.charAt(j);
        }
        return this;
    }
    @Override
    public MutableSequence set(final long offset,final CharSequence data)
                               throws IndexOutOfBoundsException,
                                      UncheckedIOException {
        return set((int)offset,data);
    }
    
    @NoIO @Override
    public MutableSequence subSequence(int start,int end)
                                       throws IndexOutOfBoundsException {
        if((end = ssidx(end)) < (start = ssidx(start)))
            throw new IndexOutOfBoundsException(
                "Invalid range: [%d,%d)"
                .formatted(end,start)
            );
        return new MutableArraySequence(data,start,end,end-start);
    }
    @NoIO @Override
    public MutableSequence subSequence(final long start,final long end)
                                       throws IndexOutOfBoundsException {
        return subSequence((int)start,(int)end);
    }
    @NoIO @Override
    public MutableSequence mutableSubSequence(int start,int end)
                                              throws IndexOutOfBoundsException {
        if((end = ssidx(end)) < (start = ssidx(start)))
            throw new IndexOutOfBoundsException(
                "Invalid range: [%d,%d)"
                .formatted(end,start)
            );
        length = (this.end = end) - (this.start = start);
        return this;
    }
    @NoIO @Override
    public MutableSequence mutableSubSequence(final long start,final long end)
                                              throws IndexOutOfBoundsException {
        return subSequence((int)start,(int)end);
    }
    
    /**Mutable Array Sequence Iterator*/
    @NoIO
    static abstract class MASI extends MSI<ASI> {
        MASI(final ASI asi) {super(asi);}
        
        @NoIO @Override public Character peek() {return super.peek();}
        @NoIO @Override public Character peek(final int offset) {return super.peek(offset);}
        @NoIO @Override public Character peek(final long offset) {return super.peek(offset);}
        
        @NoIO @Override public Character next() {return super.next();}
        
        @NoIO @Override public Character skipWS() {return super.skipWS();}
        @NoIO @Override public Character skipWS(final int limit) {return super.skipWS(limit);}
        @NoIO @Override public Character skipWS(final long limit) {return super.skipWS(limit);}
        
        @NoIO @Override public Character peekNonWS() {return super.peekNonWS();}
        @NoIO @Override public Character peekNonWS(final int limit) {return super.peekNonWS(limit);}
        @NoIO @Override public Character peekNonWS(final long limit) {return super.peekNonWS(limit);}
        
        @NoIO @Override public Character peekNextNonWS() {return super.peekNextNonWS();}
        @NoIO @Override public Character peekNextNonWS(final int limit) {return super.peekNextNonWS(limit);}
        @NoIO @Override public Character peekNextNonWS(final long limit) {return super.peekNextNonWS(limit);}
        
        @NoIO @Override public Character nextNonWS() {return super.nextNonWS();}
        @NoIO @Override public Character nextNonWS(final int limit) {return super.nextNonWS(limit);}
        @NoIO @Override public Character nextNonWS(final long limit) {return super.nextNonWS(limit);}
        
        @NoIO @Override
        public MutableSequenceIterator jumpTo(final int index) throws IndexOutOfBoundsException {
            super.jumpTo(index);
            return this;
        }
        @NoIO @Override
        public MutableSequenceIterator jumpTo(final long index) throws IndexOutOfBoundsException {
            super.jumpTo(index);
            return this;
        }
        @NoIO @Override
        public MutableSequenceIterator jumpOffset(final int offset) throws IndexOutOfBoundsException {
            super.jumpOffset(offset);
            return this;
        }
        @NoIO @Override
        public MutableSequenceIterator jumpOffset(final long offset) throws IndexOutOfBoundsException {
            super.jumpOffset(offset);
            return this;
        }
        
        @NoIO @Override
        public MutableSequenceIterator set(final char c) throws IndexOutOfBoundsException {
            if(sooper.oob(sooper.cursor))
                throw new IndexOutOfBoundsException(
                    "Cannot set character at index %d."
                    .formatted(sooper.cursor)
                );
            sooper.data[sooper.cursor] = c;
            return this;
        }
        @NoIO @Override
        public MutableSequenceIterator set(final int offset,final char c) throws IndexOutOfBoundsException {
            final int noff = sooper.offset(offset);
            if(sooper.oob(noff))
                throw new IndexOutOfBoundsException(
                    "Cannot jump to index %d (range: [%d,%d),input: %d)."
                    .formatted(noff,sooper.start,sooper.end,offset)
                );
            sooper.data[noff] = c;
            return this;
        }
        @NoIO @Override
        public MutableSequenceIterator set(final long offset,final char c) throws IndexOutOfBoundsException {
            return set((int)offset,c);
        }
        
        @NoIO @Override
        public MutableSequence subSequence() throws IndexOutOfBoundsException {
            final int a = sooper.subBegin(),b = sooper.subEnd();
            if(b < a)
                throw new IndexOutOfBoundsException(
                    "Range [%d,%d) is invalid."
                    .formatted(a,b)
                );
            return new MutableArraySequence(sooper.data,a,b,b-a);
        }
    }
    /**Mutable Forward Array Sequence Iterator*/
    @NoIO static class MFASI extends MASI {MFASI(final MutableArraySequence parent) {super(new FASI(parent));}}
    /**Mutable Reverse Array Sequence Iterator*/
    @NoIO static class MRASI extends MASI {MRASI(final MutableArraySequence parent) {super(new RASI(parent));}}
    
    @NoIO @Override
    public MutableSequenceIterator forwardIterator() {
        return isEmpty()? EMPTY.forwardIterator() : new MFASI(this);
    }
    @NoIO @Override
    public MutableSequenceIterator reverseIterator() {
        return isEmpty()? EMPTY.reverseIterator() : new MRASI(this);
    }
    
    @NoIO @Override
    public MutableSequence copyTo(final char[] arr,final int offset)
                                  throws IllegalArgumentException,
                                         IndexOutOfBoundsException {
        super.copyTo(arr,offset);
        return this;
    }
}