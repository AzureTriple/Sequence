package sequence;

import static java.lang.Math.min;

import java.io.UncheckedIOException;

/**
 * A {@linkplain CompoundSequence} which implements the
 * {@linkplain MutableSequence} interface.
 * 
 * @author AzureTriple
 */
class MutableCompoundSequence extends CompoundSequence implements MutableSequence {
    static final CSConstructor CONSTRUCTOR = (s,d) -> new MutableCompoundSequence(s,d);
    
    MutableCompoundSequence(final long[] subSizes,final Sequence[] data) {super(subSizes,data);}
    
    @Override
    public MutableSequence set(final int index,final char c)
                               throws IndexOutOfBoundsException,
                                      UncheckedIOException {
        return set((long)index,c);
    }
    @Override
    public MutableSequence set(long index,final char c)
                               throws IndexOutOfBoundsException,
                                      UncheckedIOException {
        index = idx(index);
        final int segment = segment(index);
        ((MutableSequence)data[segment]).set(relative(index,segment),c);
        return this;
    }
    @Override
    public MutableSequence set(final int offset,final char[] data)
                               throws IndexOutOfBoundsException,
                                      UncheckedIOException {
        return set((long)offset,data);
    }
    @Override
    public MutableSequence set(long offset,final char[] data)
                               throws IndexOutOfBoundsException,
                                      UncheckedIOException {
        if((offset = idx(offset)) + data.length > size())
            throw new IndexOutOfBoundsException(
                "Input string of size %d is too large to set at index %d with sequence length %d."
                .formatted(data.length,offset,size())
            );
        int segment = segment(offset);
        int l = data.length,o;
        {
            final long relative = relative(offset,segment);
            final long delta = this.data[segment].size() - relative;
            if(delta >= l) {
                ((MutableSequence)this.data[segment]).set(relative,data);
                return this;
            }
            final char[] d = new char[(int)delta];
            System.arraycopy(data,0,d,0,(int)delta);
            ((MutableSequence)this.data[segment]).set(relative,d);
            l -= delta;
            o = (int)delta;
        }
        do {
            final char[] d = new char[(int)min(l,this.data[++segment].size())];
            System.arraycopy(data,o,d,0,d.length);
            ((MutableSequence)this.data[segment]).set(0,d);
            o += d.length;
            l -= d.length;
        } while(l != 0);
        return this;
    }
    @Override
    public MutableSequence set(final int offset,final CharSequence data)
                               throws IndexOutOfBoundsException,
                                      UncheckedIOException {
        return set((long)offset,data);
    }
    private void setS(long offset,final Sequence data) {
        if((offset = idx(offset)) + data.size() > size())
            throw new IndexOutOfBoundsException(
                "Input string of size %d is too large to set at index %d with sequence length %d."
                .formatted(data.size(),offset,size())
            );
        int segment = segment(offset);
        long l = data.size(),o;
        {
            final long relative = relative(offset,segment);
            o = this.data[segment].size() - relative;
            if(o >= l) {
                ((MutableSequence)this.data[segment]).set(relative,data);
                return;
            }
            ((MutableSequence)this.data[segment]).set(relative,data.subSequence(0L,o));
            l -= o;
        }
        do {
            final long dl = min(l,this.data[++segment].size());
            ((MutableSequence)this.data[segment]).set(0,data.subSequence(o,o += dl));
            l -= dl;
        } while(l != 0);
    }
    @Override
    public MutableSequence set(long offset,final CharSequence data)
                               throws IndexOutOfBoundsException,
                                      UncheckedIOException {
        if(data instanceof Sequence) {setS(offset,(Sequence)data); return this;}
        if((offset = idx(offset)) + data.length() > size())
            throw new IndexOutOfBoundsException(
                "Input string of size %d is too large to set at index %d with sequence length %d."
                .formatted(data.length(),offset,size())
            );
        int segment = segment(offset);
        int l = data.length(),o;
        {
            final long relative = relative(offset,segment);
            final long delta = this.data[segment].size() - relative;
            if(delta >= l) {
                ((MutableSequence)this.data[segment]).set(relative,data);
                return this;
            }
            ((MutableSequence)this.data[segment]).set(relative,data.subSequence(0,o = (int)delta));
            l -= delta;
        }
        do {
            final int dl = (int)min(l,this.data[++segment].size());
            ((MutableSequence)this.data[segment]).set(0,data.subSequence(o,o += dl));
            l -= dl;
        } while(l != 0);
        return this;
    }
    
    @Override CSConstructor constructor() {return CONSTRUCTOR;}
    @Override
    public MutableSequence subSequence(final int start,final int end)
                                       throws IndexOutOfBoundsException,
                                              UncheckedIOException {
        return (MutableSequence)super.subSequence(start,end);
    }
    @Override
    public MutableSequence subSequence(final long start,final long end)
                                       throws IndexOutOfBoundsException,
                                              UncheckedIOException {
        return (MutableSequence)super.subSequence(start,end);
    }
    @Override
    public MutableSequence mutableSubSequence(final int start,final int end)
                                              throws IndexOutOfBoundsException,
                                                     UncheckedIOException {
        return mutableSubSequence((long)start,(long)end);
    }
    @Override
    public MutableSequence mutableSubSequence(long start,long end)
                                              throws IndexOutOfBoundsException,
                                                     UncheckedIOException {
        if((end = ssidx(end)) < (start = ssidx(start)))
            throw new IndexOutOfBoundsException(
                "Range [%d,%d) is invalid."
                .formatted(end,start)
            );
        if(start == end) return EMPTY;
        final int first = segment(start,subSizes),last = segment(end - 1,subSizes);
        final long r0 = relative(start,first,subSizes),r1 = relative(end,last,subSizes);
        if(first == last) return ((MutableSequence)data[first]).mutableSubSequence(r0,r1);
        final long ls = data[last].size();
        
        data = ndata(data,first,last,r0,r1);
        subSizes = nss(data,ls,subSizes,first,r0,r1);
        return this;
    }
    
    /**Mutable Compound Sequence Iterator*/
    static abstract class MCSI extends MSI<CSI> {
        MCSI(final CSI sooper) throws UncheckedIOException {super(sooper);}
        
        @Override
        public MutableSequenceIterator set(final char c) throws IndexOutOfBoundsException,
                                                                UncheckedIOException {
            if(sooper.oob(sooper.cursor))
                throw new IndexOutOfBoundsException(
                    "Cannot set character at index %d."
                    .formatted(sooper.cursor)
                );
            ((MutableSequenceIterator)sooper.itr).set(c);
            return this;
        }
        @Override
        public MutableSequenceIterator set(final int offset,final char c)
                                           throws IndexOutOfBoundsException,
                                                  UncheckedIOException {
            return set((long)offset,c);
        }
        @Override
        public MutableSequenceIterator set(final long offset,final char c)
                                           throws IndexOutOfBoundsException,
                                                  UncheckedIOException {
            final long noff = sooper.offset(offset);
            if(sooper.oob(noff))
                throw new IndexOutOfBoundsException(
                    "Cannot jump to index %d (range: [0,%d),input: %d)."
                    .formatted(noff,sooper.end,offset)
                );
            final int segment = segment(noff,sooper.subSizes);
            ((MutableSequence)sooper.data[segment]).set(relative(noff,segment,sooper.subSizes),c);
            return this;
        }
        
        @Override
        public MutableSequence subSequence() throws IndexOutOfBoundsException,
                                                    UncheckedIOException {
            return (MutableSequence)sooper.subSequence();
        }
    }
    /**Mutable Forward Compound Sequence Iterator*/
    static class MFCSI extends MCSI {
        MFCSI(final MutableCompoundSequence parent) throws UncheckedIOException {
            super(new FCSI(parent));
        }
    }
    /**Mutable Reverse Compound Sequence Iterator*/
    static class MRCSI extends MCSI {
        MRCSI(final MutableCompoundSequence parent) throws UncheckedIOException {
            super(new RCSI(parent));
        }
    }
    
    @Override
    public MutableSequenceIterator forwardIterator() throws UncheckedIOException {
        return isEmpty()? EMPTY.forwardIterator() : new MFCSI(this);
    }
    @Override
    public MutableSequenceIterator reverseIterator() throws UncheckedIOException {
        return isEmpty()? EMPTY.reverseIterator() : new MRCSI(this);
    }
    
    @Override
    public MutableSequence copyTo(final char[] arr,final int offset)
                                  throws IllegalArgumentException,
                                         IndexOutOfBoundsException,
                                         UncheckedIOException {
        super.copyTo(arr,offset);
        return this;
    }
    @Override
    public Sequence immutableCopy() throws UncheckedIOException {
        final Sequence[] cpy = new Sequence[data.length];
        for(int i = 0;i < cpy.length;++i) cpy[i] = data[i].immutableCopy();
        return new CompoundSequence(sscpy(subSizes),cpy);
    }
}