package sequence;

import static sequence.FileSequence.ioe;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

import java.io.UncheckedIOException;
import util.NoIO;
import util.NoIO.Suppresses;

public class CompoundSequence implements Sequence {
    protected Sequence[] data;
    /**Holds the total size of all sequences before and at each index.*/
    protected long[] subSizes;
    protected long start,end;
    
    protected CompoundSequence(final long start,final long end,final long[] subSizes,
                               final Sequence[] data) {
        this.start = start;
        this.end = end;
        this.subSizes = subSizes;
        this.data = data;
    }
    
    protected static class CompoundSequenceBuilder {
        private Sequence[] data = null;
        
        public CompoundSequenceBuilder data(final Sequence...data) {
            this.data = data;
            return this;
        }
        
        public Sequence build() {
            if(data == null || data.length == 0) return EMPTY;
            if(data.length == 1) return data[0];
            // Remove null and empty sequences.
            int set = 0;
            long end = 0;
            long[] sizes = new long[data.length];
            for(int i = 0;i < data.length;++i) {
                if(data[i] != null && !data[i].isEmpty()) {
                    if(i != set)
                        data[set] = data[i] instanceof MutableSequence
                            ? ((MutableSequence)data[i]).immutableCopy()
                            : data[i];
                    sizes[set] = end += data[set].size();
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
            return new CompoundSequence(0,end,sizes,data);
        }
    }
    
    @Override public int length() {return (int)size();}
    @Override public long size() {return end - start;}
    
    /**
     * @param idx   The index of the desired character. Negative values indicate an
     *              offset from the end instead of the start.
     * @param start The index of the first character (inclusive).
     * @param end   The index of the last character (exclusive).
     * 
     * @return The adjusted index.
     * 
     * @throws IndexOutOfBoundsException <code>|idx| &ge; (end - start)</code>
     */
    protected static long idx(final long idx,final long start,final long end)
                              throws IndexOutOfBoundsException {
        final long out = idx + (idx < 0L? end : start);
        if(end <= out || out < start)
            throw new IndexOutOfBoundsException(
                "%d is outside the range [%d,%d) (shifted: %d,[0,%d))."
                .formatted(idx,start,end,out - start,end - start)
            );
        return out;
    }
    /**
     * @param idx The index of the desired character. Negative values indicate an
     *            offset from the end instead of the start.
     * 
     * @return The index of the byte in the file.
     * 
     * @throws IndexOutOfBoundsException <code>|idx| &ge; size()</code>
     */
    protected long idx(final long idx) throws IndexOutOfBoundsException {return idx(idx,start,end);}
    
    /**
     * @return The index of the segment which contains the character at the
     *         specified index.
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
        return j;
    }
    /**
     * @return The index of the segment which contains the character at the
     *         specified index.
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
     * Same as {@linkplain #idx(long,long,long)}, except <code>end</code> is
     * included in the range of valid indices.
     */
    protected static long ssidx(final long idx,final long start,final long end)
                                throws IndexOutOfBoundsException {
        final long out = idx + (idx < 0L? end : start);
        if(end < out || out < start)
            throw new IndexOutOfBoundsException(
                "%d is outside the range [%d,%d] (shifted: %d,[0,%d])."
                .formatted(idx,start,end,out - start,end - start)
            );
        return out;
    }
    /**
     * Same as {@linkplain #idx(long)}, except <code>end</code> is included in the
     * range of valid indices.
     */
    protected long ssidx(final long idx) throws IndexOutOfBoundsException {return ssidx(idx,start,end);}
    @NoIO(suppresses = Suppresses.EXCEPTIONS) @Override
    public CompoundSequence subSequence(long start,long end) throws IndexOutOfBoundsException {
        if((end = ssidx(end)) < (start = ssidx(start)))
            throw new IndexOutOfBoundsException(
                "Range [%d,%d) is invalid."
                .formatted(end,start)
            );
        return new CompoundSequence(start,end,subSizes,data);
    }
    @NoIO(suppresses = Suppresses.EXCEPTIONS) @Override
    public CompoundSequence subSequence(final int start,final int end) throws IndexOutOfBoundsException {
        return subSequence((long)start,(long)end);
    }
    
    /**Simple Compound Sequence Iterator*/
    protected class SCSI implements SimpleSequenceIterator {
        protected int segment;
        protected final long viewEnd = end;
        protected long cursor = start;
        protected final Sequence[] viewData = data;
        protected final long[] viewSubSizes = subSizes;
        protected SimpleSequenceIterator itr;
        
        protected SCSI() throws UncheckedIOException {
            if(cursor != viewEnd) itr = null;
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
        @Override public boolean hasNext() {return cursor < viewEnd;}
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
        @Override public void close() throws UncheckedIOException {if(itr != null) itr.close();}
    }
    @Override public SimpleSequenceIterator iterator() throws UncheckedIOException {return new SCSI();}
    
    /**
     * A {@linkplain SequenceIterator} view of the characters stored in multiple
     * consecutive sequences.
     */
    protected abstract class CSI implements SequenceIterator {
        protected final Sequence[] viewData = data;
        protected final long[] viewSubSizes = subSizes;
        protected final long viewStart = start,viewEnd = end,lastIdx;
        protected SequenceIterator itr;
        protected long cursor,mark;
        protected int segment;
        
        protected abstract void increment() throws UncheckedIOException;
        protected abstract SequenceIterator getItr(final Sequence segment) throws UncheckedIOException;
        protected abstract long offset(long i);
        protected boolean oob(final long i) {return viewEnd <= i || i < viewStart;}
        protected abstract long skipidx(long i);
        
        protected CSI(final long begin,final long end) throws UncheckedIOException {
            itr = getItr(viewData[segment = segment(cursor = mark = begin,viewSubSizes)]);
            lastIdx = end;
        }
        
        @Override public long index() {return cursor - viewStart;}
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
        
        @Override public void mark() throws IndexOutOfBoundsException {mark(0L);}
        @Override public void mark(final int offset) throws IndexOutOfBoundsException {mark((long)offset);}
        
        /* IMPORTANT: The jump methods should NEVER send the cursor to an out-of-bounds position. */
        @Override
        public CSI jumpTo(final int index) throws IndexOutOfBoundsException {
            return jumpTo((long)index);
        }
        @Override
        public CSI jumpTo(final long index) throws IndexOutOfBoundsException {
            cursor = CompoundSequence.idx(index,viewStart,viewEnd);
            return this;
        }
        @Override
        public CSI jumpOffset(final int offset) throws IndexOutOfBoundsException {
            return jumpOffset((long)offset);
        }
        @Override
        public CSI jumpOffset(final long offset) throws IndexOutOfBoundsException {
            final long nc = offset(offset);
            if(oob(nc))
                throw new IndexOutOfBoundsException(
                    "Cannot jump to index %d (range: [%d,%d),input: %d)."
                    .formatted(nc,viewStart,viewEnd,offset)
                );
            cursor = nc;
            return this;
        }
        
        protected abstract long subBegin();
        protected abstract long subEnd();
        @Override
        public CompoundSequence subSequence() throws UncheckedIOException {
            final long a = subBegin(),b = subEnd();
            if(b < a)
                throw new IndexOutOfBoundsException(
                    "Range [%d,%d) is invalid."
                    .formatted(a,b)
                );
            return new CompoundSequence(a,b,viewSubSizes,viewData);
        }
        
        // The easy way out.
        @Override public String toString() throws UncheckedIOException {return itr != null? itr.toString() : "";}
        
        @Override public void close() throws UncheckedIOException {if(itr != null) itr.close();}
    }
    /**Forward Compound Sequence Iterator*/
    protected class FCSI extends CSI {
        protected FCSI() throws UncheckedIOException {super(start,end);}
        
        @Override public long offset() {return cursor - viewStart;}
        
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
        @Override protected long skipidx(final long i) {return Math.min(i,viewStart);}
        
        protected Character iSWSBase(final long limit) throws UncheckedIOException {
            // Subtract the iterator's offset so that the loop always adds
            // the number of skipped characters to the cursor.
            // itr should never be null.
            cursor -= itr.offset();
            do {
                // The limit relative to the iterator's starting position
                // is the same as the limit relative to the cursor, since
                // the cursor is always at the absolute position of this
                // segment's start.
                final Character c = itr.skipWS(limit - cursor);
                // Add the number of characters skipped.
                cursor += itr.offset();
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
                // (cursor - idx) is the absolute position of the current segment's
                // starting index. Subtracting this value from the limit yields the
                // limit relative to the current segment.
                // itr should never be null.
                final long idx = itr.offset();
                final Character c = itr.peekNonWS(limit -= cursor - idx);
                // (itr.getParent().size() - idx) is the number of characters
                // skipped if no non-whitespace was found. Subtracting from the
                // limit yields the limit relative to the next segment.
                return c == null? iPNWSBase(limit - itr.getParent().size() + idx)
                                : c;
            }
            return null;
        }
        @Override
        protected Character iPNNWS(long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            if(cursor + 1L < limit) {
                // (cursor - idx) is the absolute position of the current segment's
                // starting index. Subtracting this value from the limit yields the
                // limit relative to the current segment.
                // itr should never be null.
                final long idx = itr.offset();
                final Character c = itr.peekNextNonWS(limit -= cursor - idx);
                // (itr.getParent().size() - idx) is the number of characters
                // skipped if no non-whitespace was found. Subtracting from the
                // limit yields the limit relative to the next segment.
                return c == null? iPNWSBase(limit - itr.getParent().size() + idx)
                                : c;
            }
            return null;
        }
        @Override
        protected Character iNNWS(long limit) throws UncheckedIOException {
            if(cursor + 1L < limit) {
                increment();
                return iSWSBase(limit);
            }
            return null;
        }
        
        @Override
        public void mark(final long offset) throws IndexOutOfBoundsException {
            if(oob(mark = offset(offset)) && mark != viewEnd)
                throw new IndexOutOfBoundsException(
                    "Cannot mark index %d (range: [%d,%d],input: %d)."
                    .formatted(mark,viewStart,viewEnd,offset)
                );
        }
        
        @Override protected long subBegin() {return mark;}
        @Override protected long subEnd() {return cursor;}
    }
    protected class RCSI extends CSI { 
        protected RCSI() throws UncheckedIOException {super(end - 1L,start - 1L);}
        
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
        @Override protected long skipidx(final long i) {return Math.max(i,viewStart) - 1L;}
        
        protected Character iSWSBase(final long limit) throws UncheckedIOException {
            // Add the iterator's offset so that the loop always subtracts 
            // the number of skipped characters from the cursor.
            // itr should never be null.
            cursor += itr.offset();
            do {
                // The limit relative to the iterator's ending position is
                // the same as the limit relative to the sequence's start,
                // since the cursor is always at the absolute position of
                // this segment's end.
                final Character c = itr.skipWS(limit - cursor + itr.getParent().size());
                // Subtract the number of characters skipped.
                cursor -= itr.offset();
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
        //TODO convert to reverse
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
                // (cursor - itr.index()) is the absolute position of the current
                // segment's starting index. Subtracting this value from the limit
                // yields the limit relative to the current segment.
                // itr should never be null.
                final long idx = cursor - itr.index();
                final Character c = itr.peekNonWS(limit - idx);
                return c == null? iPNWSBase(limit,idx)
                                : c;
            }
            return null;
        }
        @Override
        protected Character iPNNWS(final long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            if(cursor - 1L > limit) {
                // (cursor - itr.index()) is the absolute position of the current
                // segment's starting index. Subtracting this value from the limit
                // yields the limit relative to the current segment.
                // itr should never be null.
                final long idx = cursor - itr.index();
                final Character c = itr.peekNextNonWS(limit - idx);
                return c == null? iPNWSBase(limit,idx)
                                : c;
            }
            return null;
        }
        @Override
        protected Character iNNWS(long limit) throws UncheckedIOException {
            if(cursor - 1L > limit) {
                increment();
                return iSWSBase(limit);
            }
            return null;
        }
        
        @Override
        public void mark(final long offset) throws IndexOutOfBoundsException {
            if(oob(mark = offset(offset)) && mark != viewStart - 1L)
                throw new IndexOutOfBoundsException(
                    "Cannot mark index %d (range: [%d,%d],input: %d)."
                    .formatted(mark + 1L,viewStart,viewEnd,offset)
                );
        }
        
        @Override protected long subBegin() {return cursor + 1L;}
        @Override protected long subEnd() {return mark + 1L;}
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
}

























