package sequence;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static sequence.FileSequence.ioe;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

import java.io.UncheckedIOException;
import util.NoIO;
import util.NoIO.Suppresses;

/**
 * A {@linkplain Sequence} backed by an array containing other sequences.
 * 
 * @author AzureTriple
 */
class CompoundSequence implements Sequence {
    static final CSConstructor CONSTRUCTOR = (s,d) -> new CompoundSequence(s,d);
    
    Sequence[] data;
    /**Holds the total size of all sequences before and at each index.*/
    long[] subSizes;
    
    CompoundSequence(final long[] subSizes,final Sequence[] data) {
        this.subSizes = subSizes;
        this.data = data;
    }
    
    @Override
    public boolean equals(final Object obj) {
        return obj == this || obj instanceof CharSequence && compareTo((CharSequence)obj) == 0;
    }
    
    @Override public int length() {return (int)size();}
    @Override public long size() {return subSizes[subSizes.length - 1];}
    
    /**
     * @param idx   The index of the desired character. Negative values indicate an
     *              offset from the end instead of the start.
     * @param length   The index of the last character (exclusive).
     * 
     * @return The adjusted index.
     * 
     * @throws IndexOutOfBoundsException <code>|idx| &ge; length</code>
     */
    static long idx(final long idx,final long length)
                              throws IndexOutOfBoundsException {
        final long out = idx + (idx < 0L? length : 0L);
        if(length <= out || out < 0L)
            throw new IndexOutOfBoundsException(
                "%d is outside the range [0,%d) (shifted: %d,[0,%d))."
                .formatted(idx,length,out,length)
            );
        return out;
    }
    /**
     * @param idx The index of the desired character. Negative values indicate an
     *            offset from the end instead of the start.
     * 
     * @return The adjusted index.
     * 
     * @throws IndexOutOfBoundsException <code>|idx| &ge; size()</code>
     */
    long idx(final long idx) throws IndexOutOfBoundsException {return idx(idx,size());}
    
    /**
     * @return The index of the segment which contains the character at the
     *         specified index. The returned index is always in the range
     *         <code>[0,subSizes.length)</code>.
     */
    static int segment(final long idx,final long[] subSizes) {
        // Binary search
        int i = subSizes.length / 2,j = i;
        do {
            final int k = (i & 1) + (i /= 2);
            if(idx == subSizes[j]) return j + 1;
            if(idx < subSizes[j]) {
                if(j == 0 || idx >= subSizes[j - 1]) return j;
                j -= k;
            } else j += k;
        } while(i > 0);
        return min(subSizes.length - 1,j);
    }
    /**
     * @return The index of the segment which contains the character at the
     *         specified index. The returned index is always in the range
     *         <code>[0,subSizes.length)</code>.
     */
    int segment(final long idx) {return segment(idx,subSizes);}
    /**@return The character index set relative to the specified segment.*/
    static long relative(final long index,final int segment,final long[] subSizes) {
        return index - (segment == 0? 0L : subSizes[segment - 1]);
    }
    /**@return The character index set relative to the specified segment.*/
    long relative(final long index,final int segment) {return relative(index,segment,subSizes);}
    @Override
    public char charAt(long index) throws IndexOutOfBoundsException,UncheckedIOException {
        final int segment = segment(index = idx(index));
        return data[segment].charAt(relative(index,segment));
    }
    @Override
    public char charAt(final int index) throws IndexOutOfBoundsException,UncheckedIOException {
        return charAt((long)index);
    }
    
    /**
     * Same as {@linkplain #idx(long,long)}, except <code>length</code> is
     * included in the range of valid indices.
     */
    static long ssidx(final long idx,final long length)
                      throws IndexOutOfBoundsException {
        final long out = idx + (idx < 0L? length : 0L);
        if(length < out || out < 0L)
            throw new IndexOutOfBoundsException(
                "%d is outside the range [0,%d] (shifted: %d,[0,%d])."
                .formatted(idx,length,out,length)
            );
        return out;
    }
    /**
     * Same as {@linkplain #idx(long)}, except <code>length</code> is included in the
     * range of valid indices.
     */
    long ssidx(final long idx) throws IndexOutOfBoundsException {return ssidx(idx,size());}
    static Sequence[] ndata(final Sequence[] data,
                                      final int first,final int last,
                                      final long r0,final long r1)
                                      throws UncheckedIOException {
        final Sequence[] ndata = new Sequence[last - first + 1];
        ndata[0] = data[first].subSequence(r0,data[first].size());
        System.arraycopy(data,first + 1,ndata,1,last - first - 1);
        ndata[ndata.length - 1] = data[last].subSequence(0L,r1);
        return ndata;
    }
    static long[] nss(final Sequence[] ndata,
                                final long lastSize,
                                final long[] subSizes,
                                final int first,
                                final long r0,final long r1)
                                throws UncheckedIOException {
        final long[] nss = new long[ndata.length];
        {
            final long diff = r0 + (first == 0? 0L : subSizes[first - 1]);
            if(diff == 0L)
                System.arraycopy(subSizes,0,nss,0,nss.length);
            else
                for(int i = 0,j = first;i < nss.length;++i,++j)
                    nss[i] = subSizes[j] - diff;
        }
        nss[nss.length - 1] -= lastSize - r1;
        return nss;
    }
    static interface CSConstructor {Sequence construct(long[] nss,Sequence[] ndata);}
    CSConstructor constructor() {return CONSTRUCTOR;}
    /**
     * @implNote Since the sub-sequence operations of the sequences composing this
     *           sequence are assumed to be efficient, there is no need to fiddle
     *           around with index math.
     */
    static Sequence internalSS(final Sequence[] data,final long[] subSizes,
                               final long start,final long end,
                               final CSConstructor constructor)
                               throws UncheckedIOException {
        if(start == end) return EMPTY;
        final int first = segment(start,subSizes),last = segment(end - 1,subSizes);
        final long r0 = relative(start,first,subSizes),r1 = relative(end,last,subSizes);
        if(first == last) return data[first].subSequence(r0,r1);
        
        final Sequence[] ndata = ndata(data,first,last,r0,r1);
        final long[] nss = nss(ndata,data[last].size(),subSizes,first,r0,r1);
        
        return constructor.construct(nss,ndata);
    }
    @Override
    public Sequence subSequence(long start,long end) throws IndexOutOfBoundsException,
                                                            UncheckedIOException {
        if((end = ssidx(end)) < (start = ssidx(start)))
            throw new IndexOutOfBoundsException(
                "Range [%d,%d) is invalid."
                .formatted(start,end)
            );
        return internalSS(data,subSizes,start,end,constructor());
    }
    @Override
    public Sequence subSequence(final int start,final int end) throws IndexOutOfBoundsException,
                                                                      UncheckedIOException {
        return subSequence((long)start,(long)end);
    }
    
    /**Simple Compound Sequence Iterator*/
    private static class SCSI implements SimpleSequenceIterator {
        int segment;
        final long end;
        long cursor = 0L;
        final Sequence[] data;
        final long[] subSizes;
        SimpleSequenceIterator itr;
        
        SCSI(final CompoundSequence parent) throws UncheckedIOException {
            if(cursor == (end = parent.size())) {
                itr = null;
                subSizes = null;
                data = null;
            } else {
                segment = segment(cursor,subSizes = parent.subSizes);
                itr = (data = parent.data)[segment].iterator();
                itr.skip(relative(cursor,segment,subSizes));
            }
        }
        
        @Override
        public SimpleSequenceIterator skip(final long count)
                                           throws IllegalArgumentException,
                                                  NoSuchElementException,
                                                  UncheckedIOException {
            if(count == 0L) return this;
            if(count < 0L)
                throw new IllegalArgumentException(
                    "Negative offset %d."
                    .formatted(count)
                );
            if(cursor + count >= end)
                throw new NoSuchElementException(
                    "Cannot skip %d characters after index %d."
                    .formatted(count,cursor)
                );
            final int nSegment = segment(cursor += count,subSizes);
            if(segment != nSegment) {
                itr.close();
                itr = data[segment = nSegment].iterator();
                itr.skip(relative(cursor,segment,subSizes));
            } else itr.skip(count);
            return this;
        }
        
        @Override
        public boolean hasNext() throws UncheckedIOException {
            if(itr != null) {
                if(cursor != end) return true;
                close();
            }
            return false;
        }
        @Override
        public Character next() throws NoSuchElementException,UncheckedIOException {
            if(!hasNext()) throw new NoSuchElementException();
            if(!itr.hasNext()) {
                itr.close();
                itr = data[++segment].iterator();
            }
            ++cursor;
            return itr.next();
        }
        
        @Override
        public void forEachRemaining(final Consumer<? super Character> action) throws UncheckedIOException {
            if(action == null) return;
            if(itr != null) {
                itr.forEachRemaining(action);
                itr = null;
                final int lastSegment = segment(end - 1L,subSizes);
                while(++segment <= lastSegment) {
                    try(final SimpleSequenceIterator i = data[segment].iterator()) {
                        i.forEachRemaining(action);
                    }
                }
            }
        }
        
        @Override
        public void close() throws UncheckedIOException {
            cursor = end;
            if(itr != null) {itr.close(); itr = null;}
        }
    }
    @Override public SimpleSequenceIterator iterator() throws UncheckedIOException {return new SCSI(this);}
    
    /**
     * A {@linkplain SequenceIterator} view of the characters stored in multiple
     * consecutive sequences.
     */
    static abstract class CSI implements SequenceIterator {
        final Sequence[] data;
        final long[] subSizes;
        final long end,lastIdx;
        SequenceIterator itr;
        long cursor,mark;
        int segment;
        private final CompoundSequence cs;
        private final CSConstructor constructor;
        
        abstract void increment() throws UncheckedIOException;
        abstract SequenceIterator getItr(final Sequence segment) throws UncheckedIOException;
        abstract long offset(long i);
        boolean oob(final long i) {return end <= i || i < 0L;}
        abstract long skipidx(long i);
        
        CSI(final long begin,final long end,final CompoundSequence parent)
            throws UncheckedIOException {
            data = parent.data;
            subSizes = parent.subSizes;
            this.end = parent.size();
            cs = parent;
            lastIdx = end;
            itr = getItr(data[segment = segment(cursor = mark = begin,subSizes)]);
            this.constructor = parent.constructor();
        }
        
        @Override public long index() {return cursor;}
        @Override public Sequence getParent() {return cs;}
        
        @Override
        public Character peek() throws UncheckedIOException {
            return itr == null? null : itr.peek();
        }
        @Override
        public Character peek(final int offset) throws UncheckedIOException {
            return peek((long)offset);
        }
        @Override
        public Character peek(long offset) throws UncheckedIOException {
            if(oob(offset = offset(offset))) return null;
            final int pkSeg = segment(offset,subSizes);
            return data[pkSeg].charAt(relative(offset,pkSeg,subSizes));
        }
        
        @Override public boolean hasNext() {return cursor != lastIdx;}
        @Override
        public Character next() throws UncheckedIOException {
            if(hasNext()) {
                final char c = itr.next();
                increment();
                return c;
            }
            return null;
        }
        
        abstract Character iSWS(long limit) throws UncheckedIOException;
        @Override
        public Character skipWS() throws UncheckedIOException {
            return iSWS(lastIdx);
        }
        @Override
        public Character skipWS(final int limit) throws UncheckedIOException {
            return skipWS((long)limit);
        }
        @Override
        public Character skipWS(final long limit) throws UncheckedIOException {
            return iSWS(skipidx(limit));
        }
        
        abstract Character iPNWS(long limit) throws UncheckedIOException;
        @Override
        public Character peekNonWS() throws UncheckedIOException {
            return iPNWS(lastIdx);
        }
        @Override
        public Character peekNonWS(final int limit) throws UncheckedIOException {
            return peekNonWS((long)limit);
        }
        @Override
        public Character peekNonWS(final long limit) throws UncheckedIOException {
            return iPNWS(skipidx(limit));
        }
        
        abstract Character iPNNWS(long limit) throws UncheckedIOException;
        @Override
        public Character peekNextNonWS() throws UncheckedIOException {
            return iPNNWS(lastIdx);
        }
        @Override
        public Character peekNextNonWS(final int limit) throws UncheckedIOException {
            return peekNextNonWS((long)limit);
        }
        @Override
        public Character peekNextNonWS(final long limit) throws UncheckedIOException {
            return iPNNWS(skipidx(limit));
        }
        
        abstract Character iNNWS(long limit) throws UncheckedIOException;
        @Override
        public Character nextNonWS() throws UncheckedIOException {
            return iNNWS(lastIdx);
        }
        @Override
        public Character nextNonWS(final int limit) throws UncheckedIOException {
            return nextNonWS((long)limit);
        }
        @Override
        public Character nextNonWS(final long limit) throws UncheckedIOException {
            return iNNWS(skipidx(limit));
        }
        
        @Override
        public SequenceIterator mark() throws IndexOutOfBoundsException {
            return mark(0L);
        }
        @Override
        public SequenceIterator mark(final int offset) throws IndexOutOfBoundsException {
            return mark((long)offset);
        }
        @Override public abstract SequenceIterator mark(long offset) throws IndexOutOfBoundsException;
        
        /* IMPORTANT: The jump methods should NEVER send the cursor to an out-of-bounds position. */
        void jumpBase(final long nc) throws UncheckedIOException {
            {
                final int ns = segment(cursor = nc,subSizes);
                if(ns != segment) {
                    if(itr != null) itr.close();
                    itr = getItr(data[segment = ns]);
                }
            }
            // (itr == null) == (segment == data.length)
            // 'ns' is in [0,data.length)
            // Therefore, (itr == null) == (ns != segment)
            // 'itr' is never null here. Q.E.D.
            itr.jumpTo(relative(cursor,segment,subSizes));
        }
        @Override
        public SequenceIterator jumpTo(final int index) throws IndexOutOfBoundsException,
                                                               UncheckedIOException {
            return jumpTo((long)index);
        }
        @Override
        public SequenceIterator jumpTo(final long index) throws IndexOutOfBoundsException,
                                                                UncheckedIOException {
            final long nc = CompoundSequence.idx(index,end);
            if(nc != cursor) jumpBase(nc);
            return this;
        }
        @Override
        public SequenceIterator jumpOffset(final int offset) throws IndexOutOfBoundsException,
                                                                    UncheckedIOException {
            return jumpOffset((long)offset);
        }
        @Override
        public SequenceIterator jumpOffset(final long offset) throws IndexOutOfBoundsException,
                                                                     UncheckedIOException {
            final long nc = offset(offset);
            if(nc != cursor) {
                if(oob(nc))
                    throw new IndexOutOfBoundsException(
                        "Cannot jump to index %d (range: [0,%d),input: %d)."
                        .formatted(nc,end,offset)
                    );
                jumpBase(nc);
            }
            return this;
        }
        
        abstract long subBegin();
        abstract long subEnd();
        @Override
        public Sequence subSequence() throws UncheckedIOException {
            final long a = subBegin(),b = subEnd();
            if(b < a)
                throw new IndexOutOfBoundsException(
                    "Range [%d,%d) is invalid."
                    .formatted(a,b)
                );
            return internalSS(data,subSizes,a,b,constructor);
        }
        
        abstract String fullStrings(String current) throws UncheckedIOException;
        @Override
        public String toString() throws UncheckedIOException {
            return itr != null? fullStrings(itr.toString()) : "";
        }
        
        @Override public void close() throws UncheckedIOException {if(itr != null) itr.close();}
    }
    /**Forward Compound Sequence Iterator*/
    static class FCSI extends CSI {
        FCSI(final CompoundSequence parent) throws UncheckedIOException {
            super(0L,parent.size(),parent);
        }
        
        @Override public long offset() {return cursor;}
        
        @Override
        void increment() throws UncheckedIOException {
            if(++cursor == subSizes[segment]) nextSegment();
        }
        void nextSegment() throws UncheckedIOException {
            itr.close();
            itr = ++segment != data.length? getItr(data[segment])
                                          : null;
        }
        @Override
        SequenceIterator getItr(final Sequence segment) throws UncheckedIOException {
            return segment.forwardIterator();
        }
        @Override long offset(final long i) {return cursor + i;}
        @Override long skipidx(final long i) {return min(i,end);}
        
        Character iSWSBase(final long limit) throws UncheckedIOException {
            // limit <= end (capped by skipidx)
            // cursor < limit (calling condition)
            // -> cursor < end
            // Methods which modify the cursor are sync'd w/ the segment index
            // -> segment < data.length
            // itr != null Q.E.D.
            
            // Subtract the iterator's offset so that the loop always adds
            // the number of skipped characters to the cursor.
            cursor -= itr.offset();
            do {
                // The limit relative to the iterator's starting position
                // is the same as the limit relative to the cursor, since
                // the cursor is always at the absolute position of this
                // segment's start.
                final Character c = itr.skipWS(limit - cursor);
                // Add the number of characters skipped.
                cursor += itr.getParent().size();
                // Check exit conditions.
                if(c != null || cursor == limit) return c;
                // Move on to the next segment, if any.
                nextSegment();
            } while(segment != data.length);
            return null;
        }
        @Override
        Character iSWS(final long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            return cursor < limit? iSWSBase(limit) : null;
        }
        Character iPNWSBase(long limit) throws UncheckedIOException {
            // Iterate through all other segments.
            for(int s = segment + 1;s < data.length;++s) {
                final Character c;
                try(final SequenceIterator i = getItr(data[s])) {
                    c = i.skipWS(limit);
                    limit -= i.offset();
                }
                if(c != null || limit == 0L) return c;
            }
            return null;
        }
        @Override
        Character iPNWS(long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            if(cursor < limit) {
                // limit <= end (capped by skipidx)
                // cursor < limit (above condition)
                // -> cursor < end
                // Methods which modify the cursor are sync'd w/ the segment index
                // -> segment < data.length
                // itr != null Q.E.D.
                
                // (cursor - idx) is the absolute position of the current segment's
                // starting index. Subtracting this value from the limit yields the
                // limit relative to the current segment.
                final long idx = itr.offset();
                final Character c = itr.peekNonWS(limit -= cursor - idx);
                // (itr.getParent().size() - idx) is the number of characters
                // skipped if no non-whitespace was found. Subtracting from the
                // limit yields the limit relative to the next segment.
                return c == null? iPNWSBase(limit - itr.getParent().size() + idx)
                                // The cast keeps the return value of iPNWS from
                                // auto-unboxing, which allows it to return null.
                                : (Character)c;
            }
            return null;
        }
        @Override
        Character iPNNWS(long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            if(cursor + 1L < limit) {
                // limit <= end (capped by skipidx)
                // cursor + 1 < limit (calling condition)
                // -> cursor < limit - 1
                // -> cursor < end - 1
                // -> cursor < end
                // Methods which modify the cursor are sync'd w/ the segment index
                // -> segment < data.length
                // itr != null Q.E.D.
                
                // (cursor - idx) is the absolute position of the current segment's
                // starting index. Subtracting this value from the limit yields the
                // limit relative to the current segment.
                final long idx = itr.offset();
                final Character c = itr.peekNextNonWS(limit -= cursor - idx);
                // (itr.getParent().size() - idx) is the number of characters
                // skipped if no non-whitespace was found. Subtracting from the
                // limit yields the limit relative to the next segment.
                return c == null? iPNWSBase(limit - itr.getParent().size() + idx)
                                // The cast keeps the return value of iPNWS from
                                // auto-unboxing, which allows it to return null.
                                : (Character)c;
            }
            return null;
        }
        @Override
        Character iNNWS(long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            if(cursor + 1L < limit) {
                // limit <= end (capped by skipidx)
                // cursor + 1 < limit (calling condition)
                // -> cursor < limit - 1
                // -> cursor < end - 1
                // -> cursor < end
                // Methods which modify the cursor are sync'd w/ the segment index
                // -> segment < data.length
                // itr != null Q.E.D.
                itr.next();
                increment();
                return iSWSBase(limit);
            }
            return null;
        }
        
        @Override
        public FCSI mark(final long offset) throws IndexOutOfBoundsException {
            if(oob(mark = offset(offset)) && mark != end)
                throw new IndexOutOfBoundsException(
                    "Cannot mark index %d (range: [0,%d],input: %d)."
                    .formatted(mark,end,offset)
                );
            return this;
        }
        
        @Override long subBegin() {return mark;}
        @Override long subEnd() {return cursor;}
        
        @Override
        String fullStrings(final String current) throws UncheckedIOException {
            final StringBuilder out = new StringBuilder(current);
            // Get the index of the last segment, accounting for max string size.
            final int last = segment(Integer.MAX_VALUE,subSizes);
            for(int i = segment;++i < last;) out.append(data[i]);
            final long diff = Integer.MAX_VALUE - out.length();
            return out.append(
                diff < data[last].size()? data[last].subSequence(0L,diff)
                                        : data[last]
            ).toString();
        }
    }
    /**Reverse Compound Sequence Iterator*/
    static class RCSI extends CSI {
        RCSI(final CompoundSequence parent) throws UncheckedIOException {
            super(parent.size() - 1L,-1L,parent);
        }
        
        @Override public long offset() {return end - 1L - cursor;}
        
        void increment() throws UncheckedIOException {
            if(--cursor == (segment != 0? subSizes[segment - 1] - 1L : -1L))
                nextSegment();
        }
        void nextSegment() throws UncheckedIOException {
            itr.close();
            itr = --segment != -1? getItr(data[segment])
                                 : null;
        }
        @Override
        SequenceIterator getItr(final Sequence segment) throws UncheckedIOException {
            return segment.reverseIterator();
        }
        @Override long offset(final long i) {return cursor - i;}
        @Override long skipidx(final long i) {return max(i,-1L);}
        
        Character iSWSBase(final long limit) throws UncheckedIOException {
            // limit >= -1 (capped by skipidx)
            // cursor > limit (calling condition)
            // -> cursor > -1
            // Methods which modify the cursor are sync'd w/ the segment index
            // -> segment > -1
            // itr != null Q.E.D.
            
            // Add the iterator's offset so that the loop always subtracts 
            // the number of skipped characters from the cursor.
            cursor += itr.offset();
            do {
                // The limit relative to the iterator's ending position is
                // the same as the limit relative to the sequence's start,
                // since the cursor is always at the absolute position of
                // this segment's end.
                final long ps = itr.getParent().size();
                final Character c = itr.skipWS(limit - cursor + ps - 1);
                // Subtract the number of characters skipped.
                cursor -= ps;
                // Check exit conditions.
                if(c != null || cursor == limit) return c;
                // Move on to the next segment, if any.
                nextSegment();
            } while(segment != -1);
            return null;
        }
        @Override
        Character iSWS(final long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            return cursor > limit? iSWSBase(limit) : null;
        }
        Character iPNWSBase(final long limit,long vcursor) throws UncheckedIOException {
            // Iterate through all other segments.
            for(int s = segment - 1;s > 0;--s) {
                final Character c;
                try(final SequenceIterator i = getItr(data[s])) {
                    c = i.skipWS(limit - vcursor + data[s].size());
                    vcursor -= i.offset();
                }
                if(c != null || vcursor == limit) return c;
            }
            return null;
        }
        @Override
        Character iPNWS(final long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            if(cursor > limit) {
                // limit >= -1 (capped by skipidx)
                // cursor > limit (above condition)
                // -> cursor > -1
                // Methods which modify the cursor are sync'd w/ the segment index
                // -> segment > -1
                // itr != null Q.E.D.
                
                // (cursor - itr.index()) is the absolute position of the current
                // segment's starting index. Subtracting this value from the limit
                // yields the limit relative to the current segment.
                final long idx = cursor - itr.index();
                final Character c = itr.peekNonWS(limit - idx);
                return c == null? iPNWSBase(limit,idx)
                                // The cast keeps the return value of iPNWS from
                                // auto-unboxing, which allows it to return null.
                                : (Character)c;
            }
            return null;
        }
        @Override
        Character iPNNWS(final long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            if(cursor - 1L > limit) {
                // limit >= -1 (capped by skipidx)
                // cursor - 1 > limit (above condition)
                // -> cursor > limit + 1
                // -> cursor > 0
                // -> cursor > -1
                // Methods which modify the cursor are sync'd w/ the segment index
                // -> segment > -1
                // itr != null Q.E.D.
                
                // (cursor - itr.index()) is the absolute position of the current
                // segment's starting index. Subtracting this value from the limit
                // yields the limit relative to the current segment.
                final long idx = cursor - itr.index();
                final Character c = itr.peekNextNonWS(limit - idx);
                return c == null? iPNWSBase(limit,idx)
                                // The cast keeps the return value of iPNWS from
                                // auto-unboxing, which allows it to return null.
                                : (Character)c;
            }
            return null;
        }
        @Override
        Character iNNWS(long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            if(cursor - 1L > limit) {
                // limit >= -1 (capped by skipidx)
                // cursor - 1 > limit (above condition)
                // -> cursor > limit + 1
                // -> cursor > 0
                // -> cursor > -1
                // Methods which modify the cursor are sync'd w/ the segment index
                // -> segment > -1
                // itr != null Q.E.D.
                itr.next();
                increment();
                return iSWSBase(limit);
            }
            return null;
        }
        
        @Override
        public RCSI mark(final long offset) throws IndexOutOfBoundsException {
            if(oob(mark = offset(offset)) && mark != -1L)
                throw new IndexOutOfBoundsException(
                    "Cannot mark index %d (range: [0,%d],input: %d)."
                    .formatted(mark + 1L,end,offset)
                );
            return this;
        }
        
        @Override long subBegin() {return cursor + 1L;}
        @Override long subEnd() {return mark + 1L;}
        
        @Override
        String fullStrings(final String current) throws UncheckedIOException {
            final int first;
            final StringBuilder out;
            {
                final long diff = subSizes[segment - 1] - Integer.MAX_VALUE + current.length();
                first = segment != 0? 1 + segment(diff,subSizes)
                                              : 1;
                out = new StringBuilder(
                    diff > 0L? data[first - 1].subSequence(
                                   relative(
                                       diff,
                                       first - 1,
                                       subSizes
                                   ),
                                   data[first - 1].size()
                               )
                             : data[first - 1]
                );
            }
            for(int i = first;i < segment;++i) out.append(data[i]);
            return out.append(current).toString();
        }
    }
    
    @Override
    public SequenceIterator forwardIterator() throws UncheckedIOException {
        return isEmpty()? EMPTY.forwardIterator() : new FCSI(this);
    }
    @Override
    public SequenceIterator reverseIterator() throws UncheckedIOException {
        return isEmpty()? EMPTY.reverseIterator() : new RCSI(this);
    }
    
    /**
     * @see sequence.Sequence#close()
     * 
     * @implNote This method calls {@linkplain Sequence#close()}, then re-throws the
     *           last exception it gets (if any).
     */
    @Override
    public void close() throws UncheckedIOException {
        UncheckedIOException except = null;
        for(final Sequence s : data) {
            try {s.close();}
            catch(final UncheckedIOException e) {except = e;}
            catch(final Exception e) {except = ioe(e);}
        }
        if(except != null) throw except;
    }
    
    @Override
    public String toString() {
        final StringBuilder out = new StringBuilder();
        // Get the index of the last segment, accounting for max string size.
        final int last = segment(Integer.MAX_VALUE,subSizes);
        for(int i = 0;i < last;++i) out.append(data[i]);
        final long diff = Integer.MAX_VALUE - out.length();
        return out.append(
            diff < data[last].size()? data[last].subSequence(0L,diff)
                                    : data[last]
        ).toString();
    }
    
    @Override
    public Sequence copyTo(final char[] arr,int offset) throws IllegalArgumentException,
                                                               UncheckedIOException {
        final long size = size();
        if(size > 0L) {
            if(offset < 0) offset += arr.length;
            if(offset + size > arr.length)
                throw new IllegalArgumentException(
                    "Cannot copy sequence of size %d to an array of size %d at index %d."
                    .formatted(size,arr.length,offset)
                );
            for(final Sequence s : data) {
                s.copyTo(arr,offset);
                offset += s.length();
            }
        }
        return this;
    }
    
    static long[] sscpy(final long[] subSizes) {
        final long[] cpy = new long[subSizes.length];
        System.arraycopy(subSizes,0,cpy,0,cpy.length);
        return cpy;
    }
    @Override
    public MutableSequence mutableCopy() throws UncheckedIOException {
        final MutableSequence[] cpy = new MutableSequence[data.length];
        for(int i = 0;i < cpy.length;++i) cpy[i] = data[i].mutableCopy();
        return new MutableCompoundSequence(sscpy(subSizes),cpy);
    }
    @NoIO(suppresses = Suppresses.EXCEPTIONS) @Override
    public Sequence immutableCopy() {
        // Data is already immutable, no need for further processing.
        return new CompoundSequence(subSizes,data);
    }
}