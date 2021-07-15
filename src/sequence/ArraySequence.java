package sequence;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

import util.NoIO;

/**
 * A {@linkplain Sequence} backed by a character array.
 * 
 * @author AzureTriple
 */
@NoIO
public class ArraySequence implements Sequence {
    protected char[] data;
    protected int start,end,length;
    
    protected ArraySequence(final char[] data,final int start,final int end,final int length) {
        this.data = data;
        this.end = end;
        this.start = start;
        this.length = length;
    }
    public static class ArraySequenceBuilder {
        private char[] data = null;
        private Integer start = null,end = null,length = null;
        
        public ArraySequenceBuilder data(final char...data) {
            this.data = data;
            return this;
        }
        public ArraySequenceBuilder data(final CharSequence data) {
            this.data = data.toString().toCharArray();
            return this;
        }
        public ArraySequenceBuilder start(final Integer start) {
            this.start = start;
            return this;
        }
        public ArraySequenceBuilder end(final Integer end) {
            this.end = end;
            length = null;
            return this;
        }
        public ArraySequenceBuilder length(final Integer length) {
            this.length = length;
            end = null;
            return this;
        }
        /**
         * @param start {@linkplain #start(Integer)}
         * @param end   {@linkplain #end(Integer)}
         */
        public ArraySequenceBuilder range(final Integer start,final Integer end) {
            this.start = start;
            this.end = end;
            length = null;
            return this;
        }
        /**
         * @param offset  {@linkplain #start(Integer)}
         * @param length {@linkplain #length(Integer)}
         */
        public ArraySequenceBuilder offset(final Integer offset,final Integer length) {
            start = offset;
            this.length = length;
            end = null;
            return this;
        }
        
        public Sequence build() throws IllegalArgumentException {
            if(data == null || data.length == 0) return EMPTY;
            
            if(start == null) start = 0;
            else if(data.length < start || start < 0 && (start += data.length) < 0)
                throw new IllegalArgumentException(
                    "Invalid start index %d for array of length %d."
                    .formatted(start,data.length)
                );
            
            if(end == null) {
                if(length == null) length = (end = data.length) - start;
                else if(length < 0 || (end = length + start) > data.length)
                    throw new IllegalArgumentException(
                        "Length %d is invalid."
                        .formatted(length)
                    );
            } else {
                if(data.length < end || end < 0 && (end += data.length) < 0)
                    throw new IllegalArgumentException(
                        "Invalid end index %d for array of length %d."
                        .formatted(end,data.length)
                    );
                if((length = end - start) < 0)
                    throw new IllegalArgumentException(
                        "Invalid range: [%d,%d)"
                        .formatted(start,end)
                    );
            }
            return length == 0? EMPTY : new ArraySequence(data,start,end,length);
        }
    }
    public static ArraySequenceBuilder builder() {return new ArraySequenceBuilder();}
    
    @Override public int length() {return end - start;}
    
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
    protected static int idx(final int idx,final int start,final int end) throws IndexOutOfBoundsException {
        final int out = idx + (idx < 0? end : start);
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
     * @return The adjusted index.
     * 
     * @throws IndexOutOfBoundsException <code>|idx| &ge; size()</code>
     */
    protected int idx(final int idx) throws IndexOutOfBoundsException {
        return idx(idx,start,end);
    }
    @NoIO @Override public char charAt(final int index) throws IndexOutOfBoundsException {return data[idx(index)];}
    @NoIO @Override public char charAt(final long index) throws IndexOutOfBoundsException {return charAt((int)index);}
    
    /**
     * Same as {@linkplain #idx(int,int,int)}, except <code>end</code> is
     * included in the range of valid indices.
     */
    protected static int ssidx(final int idx,final int start,final int end)
                               throws IndexOutOfBoundsException {
        final int out = idx + (idx < 0? end : start);
        if(end < out || out < start)
            throw new IndexOutOfBoundsException(
                "%d is outside the range [%d,%d] (shifted: %d,[0,%d])."
                .formatted(idx,start,end,out - start,end - start)
            );
        return out;
    }
    /**
     * Same as {@linkplain #idx(int)}, except <code>end</code> is included in the
     * range of valid indices.
     */
    protected int ssidx(final int idx) throws IndexOutOfBoundsException {return ssidx(idx,start,end);}
    @NoIO @Override
    public ArraySequence subSequence(int start,int end) throws IllegalArgumentException {
        if((end = ssidx(end)) < (start = ssidx(start)))
            throw new IllegalArgumentException(
                "Invalid range: [%d,%d)"
                .formatted(end,start)
            );
        return new ArraySequence(data,start,end,end - start);
    }
    @NoIO @Override
    public ArraySequence subSequence(final long start,final long end) throws IndexOutOfBoundsException {
        return subSequence((int)start,(int)end);
    }
    
    /**Simple Array Sequence Iterator*/
    @NoIO
    protected class SASI implements SimpleSequenceIterator {
        protected final char[] viewData = data;
        protected int cursor = start;
        protected final int viewEnd = end;
        
        @NoIO @Override
        public SASI skip(final long count) throws IllegalArgumentException,
                                                  NoSuchElementException {
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
            cursor += count;
            return this;
        }
        
        @Override public boolean hasNext() {return cursor != viewEnd;}
        @NoIO @Override
        public Character next() throws NoSuchElementException {
            if(!hasNext()) throw new NoSuchElementException();
            return viewData[cursor++];
        }
        
        @NoIO @Override
        public void forEachRemaining(final Consumer<? super Character> action) {
            if(action == null) return;
            while(cursor != viewEnd) action.accept(viewData[cursor++]);
        }
        
        @NoIO @Override public void close() {}
    }
    @NoIO @Override public SimpleSequenceIterator iterator() {return new SASI();}
    
    /**A {@linkplain SequenceIterator} for an {@linkplain ArraySequence}.*/
    @NoIO
    protected abstract class ASI implements SequenceIterator {
        protected final int viewStart = start,viewEnd = end,lastIdx;
        protected final char[] viewData = data;
        protected int cursor,mark;
        
        protected ASI(final int begin,final int end) {
            cursor = mark = begin;
            lastIdx = end;
        }
        
        protected abstract int offset(int i);
        protected boolean oob(final int i) {return viewEnd <= i || i < viewStart;}
        protected abstract int skipidx(int i);
        protected abstract int skipidx(long i);
        
        @Override public long index() {return cursor - viewStart;}
        @Override public ArraySequence getParent() {return ArraySequence.this;}
        
        @NoIO @Override public Character peek() {return hasNext()? null : viewData[cursor];}
        @NoIO @Override
        public Character peek(int offset) {
            return oob(offset = offset(offset))? null : viewData[offset];
        }
        @NoIO @Override public Character peek(final long offset) {return peek((int)offset);}
        
        @Override public boolean hasNext() {return cursor != lastIdx;}
        @NoIO @Override public abstract Character next();
        
        @NoIO protected abstract Character iSWS(final int limit);
        @NoIO @Override public Character skipWS() {return iSWS(lastIdx);}
        @NoIO @Override public Character skipWS(final int limit) {return iSWS(skipidx(limit));}
        @NoIO @Override public Character skipWS(final long limit) {return iSWS(skipidx(limit));}
        
        @NoIO protected abstract Character iPNWS(final int limit);
        @NoIO @Override public Character peekNonWS() {return iPNWS(lastIdx);}
        @NoIO @Override public Character peekNonWS(final int limit) {return iPNWS(skipidx(limit));}
        @NoIO @Override public Character peekNonWS(final long limit) {return iPNWS(skipidx(limit));}
        
        @NoIO protected abstract Character iPNNWS(final int limit);
        @NoIO @Override public Character peekNextNonWS() {return iPNNWS(lastIdx);}
        @NoIO @Override public Character peekNextNonWS(final int limit) {return iPNNWS(skipidx(limit));}
        @NoIO @Override public Character peekNextNonWS(final long limit) {return iPNNWS(skipidx(limit));}
        
        @NoIO protected abstract Character iNNWS(final int limit);
        @NoIO @Override public Character nextNonWS() {return iNNWS(lastIdx);}
        @NoIO @Override public Character nextNonWS(final int limit) {return iNNWS(skipidx(limit));}
        @NoIO @Override public Character nextNonWS(final long limit) {return iNNWS(skipidx(limit));}
        
        @Override public void mark() throws IndexOutOfBoundsException {mark(0);}
        @Override public void mark(final long offset) throws IndexOutOfBoundsException {mark((int)offset);}
        
        @Override
        public ASI jumpTo(final int index) throws IndexOutOfBoundsException {
            cursor = idx(index,viewStart,viewEnd);
            return this;
        }
        @Override
        public ASI jumpTo(final long index) throws IndexOutOfBoundsException {
            return jumpTo((int)index);
        }
        @Override
        public ASI jumpOffset(final int offset) throws IndexOutOfBoundsException {
            if(oob(cursor = offset(offset)))
                throw new IndexOutOfBoundsException(
                    "Cannot jump to index %d (range: [%d,%d),input: %d)."
                    .formatted(cursor,viewStart,viewEnd,offset)
                );
            return this;
        }
        @Override
        public ASI jumpOffset(final long offset) throws IndexOutOfBoundsException {
            return jumpOffset((int)offset);
        }
        
        protected abstract int subBegin();
        protected abstract int subEnd();
        @NoIO @Override
        public ArraySequence subSequence() throws IndexOutOfBoundsException {
            final int a = subBegin(),b = subEnd();
            if(b < a)
                throw new IndexOutOfBoundsException(
                    "Range [%d,%d) is invalid."
                    .formatted(a,b)
                );
            return new ArraySequence(viewData,a,b,b - a);
        }
        
        protected abstract int strBegin();
        protected abstract int strEnd();
        @NoIO @Override public String toString() {return new String(viewData,strBegin(),strEnd());}
        
        @NoIO @Override public void close() {}
    }
    /**Forward Array Sequence Iterator*/
    @NoIO
    protected class FASI extends ASI {
        protected FASI() {super(start,end);}
        
        @Override public long offset() {return cursor - viewStart;}
        
        @Override protected int offset(final int i) {return cursor + i;}
        @Override protected int skipidx(final int i) {return Math.min(i,viewEnd);}
        @Override protected int skipidx(final long i) {return (int)Math.min(i,viewEnd);}
        
        @NoIO @Override public Character next() {return hasNext()? viewData[cursor++] : null;}
        
        @NoIO @Override
        protected Character iSWS(final int limit) {
            // This method trusts that the cursor never underflows via jump.
            if(cursor < limit) {
                do if(!Character.isWhitespace(viewData[cursor])) return viewData[cursor];
                while(++cursor != limit);
            }
            return null;
        }
        @NoIO @Override
        protected Character iPNWS(final int limit) {
            // This method trusts that the cursor never underflows via jump.
            return cursor < limit? Character.isWhitespace(viewData[cursor])? iNNWS(limit)
                                                                           : viewData[cursor]
                                 : null;
        }
        @NoIO @Override
        protected Character iPNNWS(final int limit) {
            // This method trusts that the cursor never underflows via jump.
            for(int tmp = cursor + 1;tmp < limit;++tmp)
                if(!Character.isWhitespace(viewData[tmp]))
                    return viewData[tmp];
            return null;
        }
        @NoIO @Override
        protected Character iNNWS(final int limit) {
            // This method trusts that the cursor never underflows via jump.
            if(cursor < limit)
                while(++cursor != limit)
                    if(!Character.isWhitespace(viewData[cursor]))
                        return viewData[cursor];
            return null;
        }
        
        @Override
        public void mark(final int offset) throws IndexOutOfBoundsException {
            if(oob(mark = offset(offset)) && mark != viewEnd)
                throw new IndexOutOfBoundsException(
                    "Cannot mark index %d (range: [%d,%d],input: %d)."
                    .formatted(mark,viewStart,viewEnd,offset)
                );
        }
        
        @Override protected int subBegin() {return mark;}
        @Override protected int subEnd() {return cursor;}
        
        @Override protected int strBegin() {return viewStart;}
        @Override protected int strEnd() {return cursor;}
    }
    /**Reverse Array Sequence Iterator.*/
    @NoIO
    protected class RASI extends ASI {
        protected RASI() {super(end - 1,start - 1);}
        
        @Override public long offset() {return viewEnd - 1 - cursor;}
        
        @Override protected int offset(final int i) {return cursor - i;}
        @Override protected int skipidx(final int i) {return Math.max(i,viewStart) - 1;}
        @Override protected int skipidx(final long i) {return (int)(Math.max(i,viewStart) - 1L);}
        
        @NoIO @Override public Character next() {return hasNext()? viewData[cursor--] : null;}
        
        @NoIO @Override
        protected Character iSWS(final int limit) {
            // This method trusts that the cursor never underflows via jump.
            return cursor > limit? Character.isWhitespace(viewData[cursor])? iNNWS(limit)
                                                                           : viewData[cursor]
                                 : null;
        }
        @NoIO @Override
        protected Character iPNWS(final int limit) {
            // This method trusts that the cursor never underflows via jump.
            for(int tmp = cursor;tmp > limit;--tmp)
                if(!Character.isWhitespace(viewData[tmp]))
                    return viewData[tmp];
            return null;
        }
        @NoIO @Override
        protected Character iPNNWS(final int limit) {
            // This method trusts that the cursor never underflows via jump.
            for(int tmp = cursor - 1;tmp > limit;--tmp)
                if(!Character.isWhitespace(viewData[tmp]))
                    return viewData[tmp];
            return null;
        }
        @NoIO @Override
        protected Character iNNWS(final int limit) {
            // This method trusts that the cursor never underflows via jump.
            if(cursor > limit)
                while(--cursor != limit)
                    if(!Character.isWhitespace(viewData[cursor]))
                        return viewData[cursor];
            return null;
        }
        
        @Override
        public void mark(final int offset) throws IndexOutOfBoundsException {
            if(oob(mark = offset(offset)) && mark != viewStart - 1)
                throw new IndexOutOfBoundsException(
                    "Cannot mark index %d (range: [%d,%d),input: %d)."
                    .formatted(mark + 1,viewStart,viewEnd,offset)
                );
        }
        
        @Override protected int subBegin() {return cursor + 1;}
        @Override protected int subEnd() {return mark + 1;}
        
        @Override protected int strBegin() {return cursor + 1;}
        @Override protected int strEnd() {return viewEnd;}
    }
    
    @NoIO @Override
    public SequenceIterator forwardIterator() {
        return isEmpty()? EMPTY.forwardIterator() : new FASI();
    }
    @NoIO @Override
    public SequenceIterator reverseIterator() {
        return isEmpty()? EMPTY.reverseIterator() : new RASI();
    }
    
    @NoIO @Override public void close() {}
}