package sequence;

import static java.lang.Character.isWhitespace;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

import util.NoIO;

/**
 * A {@linkplain Sequence} backed by a character array.
 * 
 * @author AzureTriple
 */
@NoIO
class ArraySequence implements Sequence {
    char[] data;
    int start,end,length;
    
    ArraySequence(final char[] data,final int start,final int end,final int length) {
        this.data = data;
        this.end = end;
        this.start = start;
        this.length = length;
    }
    
    @Override
    public boolean equals(final Object obj) {
        return obj == this || obj instanceof CharSequence && compareTo((CharSequence)obj) == 0;
    }
    
    @Override public int length() {return length;}
    
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
    static int idx(final int idx,final int start,final int end) throws IndexOutOfBoundsException {
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
    int idx(final int idx) throws IndexOutOfBoundsException {
        return idx(idx,start,end);
    }
    @NoIO @Override public char charAt(final int index) throws IndexOutOfBoundsException {return data[idx(index)];}
    @NoIO @Override public char charAt(final long index) throws IndexOutOfBoundsException {return charAt((int)index);}
    
    /**
     * Same as {@linkplain #idx(int,int,int)}, except <code>end</code> is
     * included in the range of valid indices.
     */
    static int ssidx(final int idx,final int start,final int end)
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
    int ssidx(final int idx) throws IndexOutOfBoundsException {return ssidx(idx,start,end);}
    @NoIO @Override
    public Sequence subSequence(int start,int end) throws IndexOutOfBoundsException {
        if((end = ssidx(end)) < (start = ssidx(start)))
            throw new IndexOutOfBoundsException(
                "Invalid range: [%d,%d)"
                .formatted(end,start)
            );
        return start != end? start != this.start || end != this.end
                ? new ArraySequence(data,start,end,end - start)
                : this
                : EMPTY;
    }
    @NoIO @Override
    public Sequence subSequence(final long start,final long end) throws IndexOutOfBoundsException {
        return subSequence((int)start,(int)end);
    }
    
    /**Simple Array Sequence Iterator*/
    @NoIO
    private static class SASI implements SimpleSequenceIterator {
        final char[] data;
        int cursor;
        final int end;
        
        SASI(final ArraySequence parent) {
            data = parent.data;
            cursor = parent.start;
            end = parent.end;
        }
        
        @NoIO @Override
        public SimpleSequenceIterator skip(final long count) throws IllegalArgumentException,
                                                                    NoSuchElementException {
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
            cursor += count;
            return this;
        }
        
        @NoIO @Override public boolean hasNext() {return cursor != end;}
        @NoIO @Override
        public Character next() throws NoSuchElementException {
            if(!hasNext()) throw new NoSuchElementException();
            return data[cursor++];
        }
        
        @NoIO @Override
        public void forEachRemaining(final Consumer<? super Character> action) {
            if(action == null) return;
            while(cursor != end) action.accept(data[cursor++]);
        }
        
        @NoIO @Override public void close() {}
    }
    @NoIO @Override public SimpleSequenceIterator iterator() {return new SASI(this);}
    
    /**A {@linkplain SequenceIterator} for an {@linkplain ArraySequence}.*/
    @NoIO
    static abstract class ASI implements SequenceIterator {
        final int start,end,lastIdx;
        final char[] data;
        int cursor,mark;
        final ArraySequence parent;
        
        ASI(final int begin,final int end,final ArraySequence parent) {
            cursor = mark = begin;
            lastIdx = end;
            start = parent.start;
            this.end = parent.end;
            data = parent.data;
            this.parent = parent;
        }

        abstract int offset(int i);
        boolean oob(final int i) {return end <= i || i < start;}
        abstract int skipidx(int i);
        abstract int skipidx(long i);
        
        @Override public long index() {return cursor - start;}
        @Override public Sequence getParent() {return parent;}
        
        @NoIO @Override public Character peek() {return hasNext()? data[cursor] : null;}
        @NoIO @Override
        public Character peek(int offset) {
            return oob(offset = offset(offset))? null : data[offset];
        }
        @NoIO @Override public Character peek(final long offset) {return peek((int)offset);}
        
        @Override public boolean hasNext() {return cursor != lastIdx;}
        @NoIO @Override public abstract Character next();
        
        @NoIO abstract Character iSWS(final int limit);
        @NoIO @Override public Character skipWS() {return iSWS(lastIdx);}
        @NoIO @Override public Character skipWS(final int limit) {return iSWS(skipidx(limit));}
        @NoIO @Override public Character skipWS(final long limit) {return iSWS(skipidx(limit));}
        
        @NoIO abstract Character iPNWS(final int limit);
        @NoIO @Override public Character peekNonWS() {return iPNWS(lastIdx);}
        @NoIO @Override public Character peekNonWS(final int limit) {return iPNWS(skipidx(limit));}
        @NoIO @Override public Character peekNonWS(final long limit) {return iPNWS(skipidx(limit));}
        
        @NoIO abstract Character iPNNWS(final int limit);
        @NoIO @Override public Character peekNextNonWS() {return iPNNWS(lastIdx);}
        @NoIO @Override public Character peekNextNonWS(final int limit) {return iPNNWS(skipidx(limit));}
        @NoIO @Override public Character peekNextNonWS(final long limit) {return iPNNWS(skipidx(limit));}
        
        @NoIO abstract Character iNNWS(final int limit);
        @NoIO @Override public Character nextNonWS() {return iNNWS(lastIdx);}
        @NoIO @Override public Character nextNonWS(final int limit) {return iNNWS(skipidx(limit));}
        @NoIO @Override public Character nextNonWS(final long limit) {return iNNWS(skipidx(limit));}
        
        @NoIO abstract boolean iFind(final int limit,final char c);
        @NoIO @Override public boolean find(final char c) {return iFind(lastIdx,c);}
        @NoIO @Override public boolean find(final int limit,final char c) {return iFind(skipidx(limit),c);}
        @NoIO @Override public boolean find(final long limit,final char c) {return iFind(skipidx(limit),c);}
        
        @Override public SequenceIterator mark() throws IndexOutOfBoundsException {return mark(0);}
        @Override public abstract SequenceIterator mark(int offset) throws IndexOutOfBoundsException;
        @Override public SequenceIterator mark(final long offset) throws IndexOutOfBoundsException {return mark((int)offset);}
        
        @NoIO @Override
        public SequenceIterator jumpTo(final int index) throws IndexOutOfBoundsException {
            cursor = idx(index,start,end);
            return this;
        }
        @NoIO @Override
        public SequenceIterator jumpTo(final long index) throws IndexOutOfBoundsException {
            return jumpTo((int)index);
        }
        @NoIO @Override
        public SequenceIterator jumpOffset(final int offset) throws IndexOutOfBoundsException {
            if(oob(cursor = offset(offset)))
                throw new IndexOutOfBoundsException(
                    "Cannot jump to index %d (range: [%d,%d),input: %d)."
                    .formatted(cursor,start,end,offset)
                );
            return this;
        }
        @NoIO @Override
        public SequenceIterator jumpOffset(final long offset) throws IndexOutOfBoundsException {
            return jumpOffset((int)offset);
        }
        
        abstract int subBegin();
        abstract int subEnd();
        @NoIO @Override
        public Sequence subSequence() throws IndexOutOfBoundsException {
            final int a = subBegin(),b = subEnd();
            if(b < a)
                throw new IndexOutOfBoundsException(
                    "Range [%d,%d) is invalid."
                    .formatted(a,b)
                );
            return a != b? a != start || b != end
                    ? new ArraySequence(data,a,b,b - a)
                    : parent
                    : EMPTY;
        }
        
        abstract int strBegin();
        abstract int strEnd();
        @NoIO @Override public String toString() {return new String(data,strBegin(),strEnd());}
        
        @NoIO @Override public void close() {}
    }
    /**Forward Array Sequence Iterator*/
    @NoIO
    static class FASI extends ASI {
        FASI(final ArraySequence parent) {super(parent.start,parent.end,parent);}
        
        @Override public long offset() {return cursor - start;}
        
        @Override int offset(final int i) {return cursor + i;}
        @Override int skipidx(final int i) {return min(i,end);}
        @Override int skipidx(final long i) {return (int)min(i,end);}
        
        @NoIO @Override public Character next() {return hasNext()? data[cursor++] : null;}
        
        @NoIO @Override
        Character iSWS(final int limit) {
            // This method trusts that the cursor never underflows via jump.
            if(cursor < limit) {
                do if(!isWhitespace(data[cursor])) return data[cursor];
                while(++cursor != limit);
            }
            return null;
        }
        @NoIO @Override
        Character iPNWS(final int limit) {
            // This method trusts that the cursor never underflows via jump.
            return cursor < limit? isWhitespace(data[cursor])
                                 ? iNNWS(limit)
                                 // The cast keeps the return value of iNNWS from
                                 // auto-unboxing, which allows it to return null.
                                 : (Character)data[cursor]
                                 : null;
        }
        @NoIO @Override
        Character iPNNWS(final int limit) {
            // This method trusts that the cursor never underflows via jump.
            for(int tmp = cursor + 1;tmp < limit;++tmp)
                if(!isWhitespace(data[tmp]))
                    return data[tmp];
            return null;
        }
        @NoIO @Override
        Character iNNWS(final int limit) {
            // This method trusts that the cursor never underflows via jump.
            if(cursor < limit)
                while(++cursor != limit)
                    if(!isWhitespace(data[cursor]))
                        return data[cursor];
            return null;
        }
        @NoIO @Override
        boolean iFind(final int limit,final char c) {
            if(cursor < limit) {
                do if(data[cursor++] == c) return true;
                while(cursor != limit);
            }
            return false;
        }
        
        @Override
        public SequenceIterator mark(final int offset) throws IndexOutOfBoundsException {
            if(oob(mark = offset(offset)) && mark != end)
                throw new IndexOutOfBoundsException(
                    "Cannot mark index %d (range: [%d,%d],input: %d)."
                    .formatted(mark,start,end,offset)
                );
            return this;
        }
        
        @Override int subBegin() {return mark;}
        @Override int subEnd() {return cursor;}
        
        @Override int strBegin() {return start;}
        @Override int strEnd() {return cursor;}
    }
    /**Reverse Array Sequence Iterator.*/
    @NoIO
    static class RASI extends ASI {
        RASI(final ArraySequence parent) {super(parent.end - 1,parent.start - 1,parent);}
        
        @Override public long offset() {return end - 1 - cursor;}
        
        @Override int offset(final int i) {return cursor - i;}
        @Override int skipidx(final int i) {return max(i,-1) + start;}
        @Override int skipidx(final long i) {return (int)(max(i,-1L) + start);}
        
        @NoIO @Override public Character next() {return hasNext()? data[cursor--] : null;}
        
        @NoIO @Override
        Character iSWS(final int limit) {
            // This method trusts that the cursor never underflows via jump.
            return cursor > limit? isWhitespace(data[cursor])
                                 ? iNNWS(limit)
                                 // The cast keeps the return value of iNNWS from
                                 // auto-unboxing, which allows it to return null.
                                 : (Character)data[cursor]
                                 : null;
        }
        @NoIO @Override
        Character iPNWS(final int limit) {
            // This method trusts that the cursor never underflows via jump.
            for(int tmp = cursor;tmp > limit;--tmp)
                if(!isWhitespace(data[tmp]))
                    return data[tmp];
            return null;
        }
        @NoIO @Override
        Character iPNNWS(final int limit) {
            // This method trusts that the cursor never underflows via jump.
            for(int tmp = cursor - 1;tmp > limit;--tmp)
                if(!isWhitespace(data[tmp]))
                    return data[tmp];
            return null;
        }
        @NoIO @Override
        Character iNNWS(final int limit) {
            // This method trusts that the cursor never underflows via jump.
            if(cursor > limit)
                while(--cursor != limit)
                    if(!isWhitespace(data[cursor]))
                        return data[cursor];
            return null;
        }
        @NoIO @Override
        boolean iFind(final int limit,final char c) {
            if(cursor > limit) {
                do if(data[cursor--] == c) return true;
                while(cursor != limit);
            }
            return false;
        }
        
        @Override
        public SequenceIterator mark(final int offset) throws IndexOutOfBoundsException {
            if(oob(mark = offset(offset)) && mark != start - 1)
                throw new IndexOutOfBoundsException(
                    "Cannot mark index %d (range: [%d,%d),input: %d)."
                    .formatted(mark + 1,start,end,offset)
                );
            return this;
        }
        
        @Override int subBegin() {return cursor + 1;}
        @Override int subEnd() {return mark + 1;}
        
        @Override int strBegin() {return cursor + 1;}
        @Override int strEnd() {return end;}
    }
    
    @NoIO @Override
    public SequenceIterator forwardIterator() {
        return isEmpty()? EMPTY.forwardIterator() : new FASI(this);
    }
    @NoIO @Override
    public SequenceIterator reverseIterator() {
        return isEmpty()? EMPTY.reverseIterator() : new RASI(this);
    }
    
    @NoIO @Override public void close() {}
    
    @NoIO @Override public String toString() {return String.valueOf(data,start,length);}
    
    @NoIO @Override
    public Sequence copyTo(final char[] arr,int offset) throws IllegalArgumentException {
        final int size = length();
        if(size > 0) {
            if(offset < 0) offset += arr.length;
            if(offset + size > arr.length)
                throw new IllegalArgumentException(
                    "Cannot copy sequence of size %d to an array of size %d at index %d."
                    .formatted(size,arr.length,offset)
                );
            System.arraycopy(data,start,arr,offset,size);
        }
        return this;
    }
    
    static char[] cpy(final char[] data,final int start,final int length) {
        final char[] cpy = new char[length];
        System.arraycopy(data,start,cpy,0,length);
        return cpy;
    }
    @NoIO @Override
    public MutableSequence mutableCopy() {
        return new MutableArraySequence(cpy(data,start,length),0,length,length);
    }
    @NoIO @Override
    public Sequence immutableCopy() {
        return new ArraySequence(cpy(data,start,length),0,length,length);
    }
}