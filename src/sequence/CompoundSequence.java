package sequence;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static sequence.FileSequence.ioe;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

import java.io.UncheckedIOException;

/**
 * A {@linkplain Sequence} backed by an array containing other sequences.
 * 
 * @author AzureTriple
 */
public class CompoundSequence implements Sequence {
    /**This is not declared in settings because it is for debug only.*/
    private static final int MAX_STRING_SIZE = 8192;
    
    protected Sequence[] data;
    /**Holds the total size of all sequences before and at each index.*/
    protected long[] subSizes;
    
    protected CompoundSequence(final long[] subSizes,final Sequence[] data) {
        this.subSizes = subSizes;
        this.data = data;
    }
    
    public static class CompoundSequenceBuilder {
        private CompoundSequenceBuilder() {}
        
        private Sequence[] data = null;
        private Long start,end,length;
        
        public CompoundSequenceBuilder data(final Sequence...data) {
            this.data = data;
            return this;
        }
        public CompoundSequenceBuilder start(final Long start) {
            this.start = start;
            return this;
        }
        public CompoundSequenceBuilder end(final Long end) {
            this.end = end;
            length = null;
            return this;
        }
        public CompoundSequenceBuilder length(final Long length) {
            this.length = length;
            end = null;
            return this;
        }
        /**
         * @param start {@linkplain #start(Long)}
         * @param end   {@linkplain #end(Long)}
         */
        public CompoundSequenceBuilder range(final Long start,final Long end) {
            this.start = start;
            this.end = end;
            length = null;
            return this;
        }
        /**
         * @param offset  {@linkplain #start(Long)}
         * @param length {@linkplain #length(Long)}
         */
        public CompoundSequenceBuilder offset(final Long offset,final Long length) {
            start = offset;
            this.length = length;
            end = null;
            return this;
        }
        
        public Sequence build() {
            if(data == null || data.length == 0) return EMPTY;
            if(data.length == 1) return data[0];
            // Remove null and empty sequences.
            int set = 0;
            long ts = 0;
            long[] sizes = new long[data.length];
            for(int i = 0;i < data.length;++i) {
                if(data[i] != null && !data[i].isEmpty()) {
                    if(i != set)
                        data[set] = data[i] instanceof MutableSequence
                            ? ((MutableSequence)data[i]).immutableCopy()
                            : data[i];
                    sizes[set] = ts += data[set].size();
                    ++set;
                }
            }
            if(set == 0) return data[0];
            if(set != data.length) {
                {
                    final Sequence[] tmp = new Sequence[set];
                    System.arraycopy(data,0,tmp,0,set);
                    data = tmp;
                }
                {
                    final long[] tmp = new long[set];
                    System.arraycopy(sizes,0,tmp,0,set);
                    sizes = tmp;
                }
            }
            
            if(start == null) start = 0L;
            else if(ts < start || start < 0L && (start += ts) < 0L)
                throw new IllegalArgumentException(
                    "Invalid start index %d for array of length %d."
                    .formatted(start,ts)
                );
            
            if(end == null) {
                if(length == null) end = ts;
                else if(length < 0L || (end = length + start) > ts)
                    throw new IllegalArgumentException(
                        "Length %d is invalid."
                        .formatted(length)
                    );
            } else {
                if(ts < end || end < 0 && (end += ts) < 0L)
                    throw new IllegalArgumentException(
                        "Invalid end index %d for array of length %d."
                        .formatted(end,ts)
                    );
                if(end - start < 0L)
                    throw new IllegalArgumentException(
                        "Invalid range: [%d,%d)"
                        .formatted(start,end)
                    );
            }
            return internalSS(data,sizes,start,end);
        }
    }
    public static CompoundSequenceBuilder builder() {return new CompoundSequenceBuilder();}
    
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
    protected static long idx(final long idx,final long length)
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
    protected long idx(final long idx) throws IndexOutOfBoundsException {return idx(idx,size());}
    
    /**
     * @return The index of the segment which contains the character at the
     *         specified index. The returned index is always in the range
     *         <code>[0,subSizes.length)</code>.
     */
    protected static int segment(final long idx,final long[] subSizes) {
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
    protected int segment(final long idx) {return segment(idx,subSizes);}
    /**@return The character index set relative to the specified segment.*/
    protected static long relative(final long index,final int segment,final long[] subSizes) {
        return index - (segment == 0? 0L : subSizes[segment - 1]);
    }
    /**@return The character index set relative to the specified segment.*/
    protected long relative(final long index,final int segment) {return relative(index,segment,subSizes);}
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
    protected static long ssidx(final long idx,final long length)
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
    protected long ssidx(final long idx) throws IndexOutOfBoundsException {return ssidx(idx,size());}
    protected static Sequence internalSS(final Sequence[] data,final long[] subSizes,
                                         final long start,final long end)
                                         throws UncheckedIOException {
        if(start == end) return EMPTY;
        final int first = segment(start,subSizes),last = segment(end - 1,subSizes);
        final long r0 = relative(start,first,subSizes),r1 = relative(end,last,subSizes);
        if(first == last) return data[first].subSequence(r0,r1);
        
        final Sequence[] ndata = new Sequence[last - first + 1];
        ndata[0] = data[first].subSequence(r0,data[first].size());
        System.arraycopy(data,first + 1,ndata,1,last - first - 1);
        ndata[ndata.length - 1] = data[last].subSequence(0L,r1);
        
        final long[] nss = new long[ndata.length];
        {
            final long diff = r0 + (first == 0? 0L : subSizes[first - 1]);
            if(diff == 0L)
                System.arraycopy(subSizes,0,nss,0,nss.length);
            else
                for(int i = 0,j = first;i < nss.length;++i,++j)
                    nss[i] = subSizes[j] - diff;
        }
        nss[nss.length - 1] -= data[last].size() - r1;
        
        return new CompoundSequence(nss,ndata);
    }
    @Override
    public Sequence subSequence(long start,long end) throws IndexOutOfBoundsException,
                                                            UncheckedIOException {
        if((end = ssidx(end)) < (start = ssidx(start)))
            throw new IndexOutOfBoundsException(
                "Range [%d,%d) is invalid."
                .formatted(end,start)
            );
        return internalSS(data,subSizes,start,end);
    }
    @Override
    public Sequence subSequence(final int start,final int end) throws IndexOutOfBoundsException,
                                                                      UncheckedIOException {
        return subSequence((long)start,(long)end);
    }
    
    /**Simple Compound Sequence Iterator*/
    protected class SCSI implements SimpleSequenceIterator {
        protected int segment;
        protected final long viewEnd = size();
        protected long cursor = 0L;
        protected final Sequence[] viewData = data;
        protected final long[] viewSubSizes = subSizes;
        protected SimpleSequenceIterator itr;
        
        protected SCSI() throws UncheckedIOException {
            if(cursor == viewEnd) itr = null;
            else {
                segment = segment(cursor,viewSubSizes);
                itr = viewData[segment].iterator();
                itr.skip(relative(cursor,segment,viewSubSizes));
            }
        }
        @Override
        public SCSI skip(final long count) throws IllegalArgumentException,
                                                  NoSuchElementException,
                                                  UncheckedIOException {
            if(count == 0L) return this;
            if(count < 0L)
                throw new IllegalArgumentException(
                    "Negative offset %d."
                    .formatted(count)
                );
            if(cursor + count >= viewEnd)
                throw new NoSuchElementException(
                    "Cannot skip %d characters after index %d."
                    .formatted(count,cursor)
                );
            final int nSegment = segment(cursor += count,viewSubSizes);
            if(segment != nSegment) {
                itr.close();
                itr = viewData[segment = nSegment].iterator();
                itr.skip(relative(cursor,segment,viewSubSizes));
            } else itr.skip(count);
            return this;
        }
        @Override
        public boolean hasNext() throws UncheckedIOException {
            if(itr != null) {
                if(cursor != viewEnd) return true;
                close();
            }
            return false;
        }
        @Override
        public Character next() throws NoSuchElementException,UncheckedIOException {
            if(!hasNext()) throw new NoSuchElementException();
            if(!itr.hasNext()) {
                itr.close();
                itr = viewData[++segment].iterator();
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
                final int lastSegment = segment(viewEnd - 1,viewSubSizes);
                while(++segment <= lastSegment) {
                    try(final SimpleSequenceIterator i = viewData[segment].iterator()) {
                        i.forEachRemaining(action);
                    }
                }
            }
        }
        @Override
        public void close() throws UncheckedIOException {
            cursor = viewEnd;
            if(itr != null) {itr.close(); itr = null;}
        }
    }
    @Override public SimpleSequenceIterator iterator() throws UncheckedIOException {return new SCSI();}
    
    /**
     * A {@linkplain SequenceIterator} view of the characters stored in multiple
     * consecutive sequences.
     */
    protected abstract class CSI implements SequenceIterator {
        protected final Sequence[] viewData = data;
        protected final long[] viewSubSizes = subSizes;
        protected final long viewEnd = size(),lastIdx;
        protected SequenceIterator itr;
        protected long cursor,mark;
        protected int segment;
        
        protected abstract void increment() throws UncheckedIOException;
        protected abstract SequenceIterator getItr(final Sequence segment) throws UncheckedIOException;
        protected abstract long offset(long i);
        protected boolean oob(final long i) {return viewEnd <= i || i < 0L;}
        protected abstract long skipidx(long i);
        
        protected CSI(final long begin,final long end) throws UncheckedIOException {
            itr = getItr(viewData[segment = segment(cursor = mark = begin,viewSubSizes)]);
            lastIdx = end;
        }
        
        @Override public long index() {return cursor;}
        @Override public CompoundSequence getParent() {return CompoundSequence.this;}
        
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
            final int pkSeg = segment(offset,viewSubSizes);
            return viewData[pkSeg].charAt(relative(offset,pkSeg,viewSubSizes));
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
        
        protected abstract Character iSWS(long limit) throws UncheckedIOException;
        @Override public Character skipWS() throws UncheckedIOException {return iSWS(lastIdx);}
        @Override
        public Character skipWS(final int limit) throws UncheckedIOException {
            return skipWS((long)limit);
        }
        @Override
        public Character skipWS(final long limit) throws UncheckedIOException {
            return iSWS(skipidx(limit));
        }
        
        protected abstract Character iPNWS(long limit) throws UncheckedIOException;
        @Override public Character peekNonWS() throws UncheckedIOException {return iPNWS(lastIdx);}
        @Override
        public Character peekNonWS(final int limit) throws UncheckedIOException {
            return peekNonWS((long)limit);
        }
        @Override
        public Character peekNonWS(final long limit) throws UncheckedIOException {
            return iPNWS(skipidx(limit));
        }
        
        protected abstract Character iPNNWS(long limit) throws UncheckedIOException;
        @Override public Character peekNextNonWS() throws UncheckedIOException {return iPNNWS(lastIdx);}
        @Override
        public Character peekNextNonWS(final int limit) throws UncheckedIOException {
            return peekNextNonWS((long)limit);
        }
        @Override
        public Character peekNextNonWS(final long limit) throws UncheckedIOException {
            return iPNNWS(skipidx(limit));
        }
        
        protected abstract Character iNNWS(long limit) throws UncheckedIOException;
        @Override public Character nextNonWS() throws UncheckedIOException {return iNNWS(lastIdx);}
        @Override
        public Character nextNonWS(final int limit) throws UncheckedIOException {
            return nextNonWS((long)limit);
        }
        @Override
        public Character nextNonWS(final long limit) throws UncheckedIOException {
            return iNNWS(skipidx(limit));
        }
        
        @Override public CSI mark() throws IndexOutOfBoundsException {return mark(0L);}
        @Override public CSI mark(final int offset) throws IndexOutOfBoundsException {return mark((long)offset);}
        @Override public abstract CSI mark(long offset) throws IndexOutOfBoundsException;
        
        /* IMPORTANT: The jump methods should NEVER send the cursor to an out-of-bounds position. */
        protected void jumpBase(final long nc) throws UncheckedIOException {
            {
                final int ns = segment(cursor = nc,viewSubSizes);
                if(ns != segment) {
                    if(itr != null) itr.close();
                    itr = getItr(viewData[segment = ns]);
                }
            }
            // (itr == null) == (segment == viewData.length)
            // 'ns' is in [0,viewData.length)
            // Therefore, (itr == null) == (ns != segment)
            // 'itr' is never null here. Q.E.D.
            itr.jumpTo(relative(cursor,segment,viewSubSizes));
        }
        @Override
        public CSI jumpTo(final int index) throws IndexOutOfBoundsException,
                                                  UncheckedIOException {
            return jumpTo((long)index);
        }
        @Override
        public CSI jumpTo(final long index) throws IndexOutOfBoundsException,
                                                   UncheckedIOException {
            final long nc = CompoundSequence.idx(index,viewEnd);
            if(nc != cursor) jumpBase(nc);
            return this;
        }
        @Override
        public CSI jumpOffset(final int offset) throws IndexOutOfBoundsException,
                                                       UncheckedIOException {
            return jumpOffset((long)offset);
        }
        @Override
        public CSI jumpOffset(final long offset) throws IndexOutOfBoundsException,
                                                        UncheckedIOException {
            final long nc = offset(offset);
            if(nc != cursor) {
                if(oob(nc))
                    throw new IndexOutOfBoundsException(
                        "Cannot jump to index %d (range: [0,%d),input: %d)."
                        .formatted(nc,viewEnd,offset)
                    );
                jumpBase(nc);
            }
            return this;
        }
        
        protected abstract long subBegin();
        protected abstract long subEnd();
        @Override
        public Sequence subSequence() throws UncheckedIOException {
            final long a = subBegin(),b = subEnd();
            if(b < a)
                throw new IndexOutOfBoundsException(
                    "Range [%d,%d) is invalid."
                    .formatted(a,b)
                );
            return internalSS(viewData,viewSubSizes,a,b);
        }
        
        protected abstract String fullStrings(String current) throws UncheckedIOException;
        @Override
        public String toString() throws UncheckedIOException {
            return itr != null? fullStrings(itr.toString()) : "";
        }
        
        @Override public void close() throws UncheckedIOException {if(itr != null) itr.close();}
    }
    /**Forward Compound Sequence Iterator*/
    protected class FCSI extends CSI {
        protected FCSI() throws UncheckedIOException {super(0L,size());}
        
        @Override public long offset() {return cursor;}
        
        @Override
        protected void increment() throws UncheckedIOException {
            if(++cursor == viewSubSizes[segment])
                nextSegment();
        }
        protected void nextSegment() throws UncheckedIOException {
            itr.close();
            itr = ++segment != viewData.length? getItr(viewData[segment])
                                              : null;
        }
        @Override
        protected SequenceIterator getItr(final Sequence segment) throws UncheckedIOException {
            return segment.forwardIterator();
        }
        @Override protected long offset(final long i) {return cursor + i;}
        @Override protected long skipidx(final long i) {return min(i,viewEnd);}
        
        protected Character iSWSBase(final long limit) throws UncheckedIOException {
            // limit <= viewEnd (capped by skipidx)
            // cursor < limit (calling condition)
            // -> cursor < viewEnd
            // Methods which modify the cursor are sync'd w/ the segment index
            // -> segment < viewData.length
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
            } while(segment != viewData.length);
            return null;
        }
        @Override
        protected Character iSWS(final long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            return cursor < limit? iSWSBase(limit) : null;
        }
        protected Character iPNWSBase(long limit) throws UncheckedIOException {
            // Iterate through all other segments.
            for(int s = segment + 1;s < viewData.length;++s) {
                final Character c;
                try(final SequenceIterator i = getItr(viewData[s])) {
                    c = i.skipWS(limit);
                    limit -= i.offset();
                }
                if(c != null || limit == 0L) return c;
            }
            return null;
        }
        @Override
        protected Character iPNWS(long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            if(cursor < limit) {
                // limit <= viewEnd (capped by skipidx)
                // cursor < limit (above condition)
                // -> cursor < viewEnd
                // Methods which modify the cursor are sync'd w/ the segment index
                // -> segment < viewData.length
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
        protected Character iPNNWS(long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            if(cursor + 1L < limit) {
                // limit <= viewEnd (capped by skipidx)
                // cursor + 1 < limit (calling condition)
                // -> cursor < limit - 1
                // -> cursor < viewEnd - 1
                // -> cursor < viewEnd
                // Methods which modify the cursor are sync'd w/ the segment index
                // -> segment < viewData.length
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
        protected Character iNNWS(long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            if(cursor + 1L < limit) {
                // limit <= viewEnd (capped by skipidx)
                // cursor + 1 < limit (calling condition)
                // -> cursor < limit - 1
                // -> cursor < viewEnd - 1
                // -> cursor < viewEnd
                // Methods which modify the cursor are sync'd w/ the segment index
                // -> segment < viewData.length
                // itr != null Q.E.D.
                itr.next();
                increment();
                return iSWSBase(limit);
            }
            return null;
        }
        
        @Override
        public FCSI mark(final long offset) throws IndexOutOfBoundsException {
            if(oob(mark = offset(offset)) && mark != viewEnd)
                throw new IndexOutOfBoundsException(
                    "Cannot mark index %d (range: [0,%d],input: %d)."
                    .formatted(mark,viewEnd,offset)
                );
            return this;
        }
        
        @Override protected long subBegin() {return mark;}
        @Override protected long subEnd() {return cursor;}
        
        @Override
        protected String fullStrings(final String current) throws UncheckedIOException {
            final StringBuilder out = new StringBuilder(current);
            // Get the index of the last segment, accounting for max string size.
            final int last = segment(MAX_STRING_SIZE,viewSubSizes);
            for(int i = segment;++i < last;) out.append(viewData[i]);
            final long diff = MAX_STRING_SIZE - out.length();
            return out.append(
                diff < viewData[last].size()? viewData[last].subSequence(0L,diff)
                                            : viewData[last]
            ).toString();
        }
    }
    protected class RCSI extends CSI {
        protected RCSI() throws UncheckedIOException {super(size() - 1L,-1L);}
        
        @Override public long offset() {return viewEnd - 1L - cursor;}
        
        protected void increment() throws UncheckedIOException {
            if(--cursor == (segment != 0? viewSubSizes[segment - 1] - 1L : -1L))
                nextSegment();
        }
        protected void nextSegment() throws UncheckedIOException {
            itr.close();
            itr = --segment != -1? getItr(viewData[segment])
                                 : null;
        }
        @Override
        protected SequenceIterator getItr(final Sequence segment) throws UncheckedIOException {
            return segment.reverseIterator();
        }
        @Override protected long offset(final long i) {return cursor - i;}
        @Override protected long skipidx(final long i) {return max(i,-1L);}
        
        protected Character iSWSBase(final long limit) throws UncheckedIOException {
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
        protected Character iSWS(final long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            return cursor > limit? iSWSBase(limit) : null;
        }
        protected Character iPNWSBase(final long limit,long vcursor) throws UncheckedIOException {
            // Iterate through all other segments.
            for(int s = segment - 1;s > 0;--s) {
                final Character c;
                try(final SequenceIterator i = getItr(viewData[s])) {
                    c = i.skipWS(limit - vcursor + viewData[s].size());
                    vcursor -= i.offset();
                }
                if(c != null || vcursor == limit) return c;
            }
            return null;
        }
        @Override
        protected Character iPNWS(final long limit) throws UncheckedIOException {
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
        protected Character iPNNWS(final long limit) throws UncheckedIOException {
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
        protected Character iNNWS(long limit) throws UncheckedIOException {
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
                    .formatted(mark + 1L,viewEnd,offset)
                );
            return this;
        }
        
        @Override protected long subBegin() {return cursor + 1L;}
        @Override protected long subEnd() {return mark + 1L;}
        
        @Override
        protected String fullStrings(final String current) throws UncheckedIOException {
            final int first;
            final StringBuilder out;
            {
                final long diff = viewSubSizes[segment - 1] - MAX_STRING_SIZE + current.length();
                first = segment != 0? 1 + segment(diff,viewSubSizes)
                                              : 1;
                out = new StringBuilder(
                    diff > 0L? viewData[first - 1].subSequence(
                                   relative(
                                       diff,
                                       first - 1,
                                       viewSubSizes
                                   ),
                                   viewData[first - 1].size()
                               )
                             : viewData[first - 1]
                );
            }
            for(int i = first;i < segment;++i) out.append(viewData[i]);
            return out.append(current).toString();
        }
    }
    
    @Override
    public SequenceIterator forwardIterator() throws UncheckedIOException {
        return isEmpty()? EMPTY.forwardIterator() : new FCSI();
    }
    @Override
    public SequenceIterator reverseIterator() throws UncheckedIOException {
        return isEmpty()? EMPTY.reverseIterator() : new RCSI();
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
        final int last = segment(MAX_STRING_SIZE,subSizes);
        for(int i = 0;i < last;++i) out.append(data[i]);
        final long diff = MAX_STRING_SIZE - out.length();
        return out.append(
            diff < data[last].size()? data[last].subSequence(0L,diff)
                                    : data[last]
        ).toString();
    }
}