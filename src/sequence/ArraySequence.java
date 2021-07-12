package sequence;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 * A {@linkplain Sequence} backed by a character array.
 * 
 * @author AzureTriple
 */
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
    @Override public char charAt(final int index) throws IndexOutOfBoundsException {return data[idx(index)];}
    
    /**
     * Same as {@linkplain #idx(int)}, except <code>end</code> is included in the
     * range of valid indices.
     */
    protected int ssidx(final int idx) throws IndexOutOfBoundsException {
        final int out = idx + (idx < 0? end : start);
        if(end < out || out < start)
            throw new IndexOutOfBoundsException(
                "%d is outside the range [%d,%d] (shifted: %d,[0,%d])."
                .formatted(idx,start,end,out - start,end - start)
            );
        return out;
    }
    @Override
    public ArraySequence subSequence(int start,int end) throws IllegalArgumentException {
        if((end = ssidx(end)) < (start = ssidx(start)))
            throw new IllegalArgumentException(
                "Invalid range: [%d,%d)"
                .formatted(end,start)
            );
        return new ArraySequence(data,start,end,end - start);
    }
    @Override
    public ArraySequence subSequence(final long start,final long end) throws IndexOutOfBoundsException {
        return subSequence((int)start,(int)end);
    }
    
    @Override
    public Iterator<Character> iterator() {
        return new Iterator<>() {
            final char[] viewData = Arrays.copyOfRange(data,start,end);
            int cursor = 0;
            
            @Override public boolean hasNext() {return cursor != viewData.length;}
            @Override
            public Character next() throws NoSuchElementException {
                try {return viewData[cursor++];}
                catch(final ArrayIndexOutOfBoundsException e) {
                    throw new NoSuchElementException(e);
                }
            }
            @Override
            public void forEachRemaining(final Consumer<? super Character> action)
                                         throws NullPointerException {
                while(cursor != viewData.length) action.accept(viewData[cursor++]);
            }
        };
    }
    
    /**A {@linkplain SequenceIterator} for an {@linkplain ArraySequence}.*/
    public abstract class ArraySequenceIterator implements SequenceIterator {
        protected final int viewStart = start,viewEnd = end;
        protected final char[] viewData = data;
        protected int cursor,mark;
        
        protected ArraySequenceIterator(final int begin) {cursor = mark = begin;}
        
        protected abstract int increment(int i);
        protected void increment() {cursor = increment(cursor);}
        protected abstract int offset(int i);
        protected boolean oob(final int i) {return viewEnd <= i || i < viewStart;}
        protected abstract boolean hasNext(int i);
        
        @Override public long index() {return cursor;}
        @Override public ArraySequence getParent() {return ArraySequence.this;}
        
        @Override public Character peek() {return hasNext()? null : viewData[cursor];}
        @Override public Character peek(int offset) {return oob(offset = offset(offset))? null : viewData[offset];}
        @Override public Character peek(final long offset) {return peek((int)offset);}
        
        @Override public boolean hasNext() {return hasNext(cursor);}
        @Override
        public Character next() {
            if(hasNext()) {
                final char c = viewData[cursor];
                increment();
                return c;
            }
            return null;
        }
        
        @Override
        public Character skipWS() {
            while(hasNext()) {
                final char c = viewData[cursor];
                if(!Character.isWhitespace(c)) return c;
                increment();
            }
            return null;
        }
        @Override
        public Character peekNextNonWS() {
            if(hasNext()) {
                for(int i = cursor;hasNext(i = increment(i));) {
                    final char c = viewData[i];
                    if(!Character.isWhitespace(c)) return c;
                }
            }
            return null;
        }
        @Override
        public Character nextNonWS() {
            if(hasNext()) {
                increment();
                while(hasNext()) {
                    final char c = viewData[cursor];
                    if(!Character.isWhitespace(c)) return c;
                    increment();
                }
            }
            return null;
        }
        
        @Override public void mark() throws IndexOutOfBoundsException {mark(0);}
        @Override public void mark(final long offset) throws IndexOutOfBoundsException {mark((int)offset);}
        
        @Override
        public ArraySequenceIterator jumpTo(final int index) throws IndexOutOfBoundsException {
            cursor = idx(index,viewStart,viewEnd);
            return this;
        }
        @Override
        public ArraySequenceIterator jumpTo(final long index) throws IndexOutOfBoundsException {
            return jumpTo((int)index);
        }
        @Override
        public ArraySequenceIterator jumpOffset(final int offset) throws IndexOutOfBoundsException {
            if(oob(cursor = offset(offset)))
                throw new IndexOutOfBoundsException(
                    "Cannot jump to index %d (range: [%d,%d),input: %d)."
                    .formatted(cursor,viewStart,viewEnd,offset)
                );
            return this;
        }
        @Override
        public ArraySequenceIterator jumpOffset(final long offset) throws IndexOutOfBoundsException {
            return jumpOffset((int)offset);
        }
        
        protected abstract int subBegin();
        protected abstract int subEnd();
        @Override
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
        @Override public String toString() {return new String(viewData,strBegin(),strEnd());}
    }
    /**Forward Array Sequence Iterator*/
    protected class FASI extends ArraySequenceIterator {
        protected FASI() {super(start);}
        
        @Override protected int increment(final int i) {return i + 1;}
        @Override protected int offset(final int i) {return cursor + i;}
        @Override protected boolean hasNext(final int i) {return i != viewEnd;}
        
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
    protected class RASI extends ArraySequenceIterator {
        protected RASI() {super(end - 1);}
        
        @Override protected int increment(final int i) {return i - 1;}
        @Override protected int offset(final int i) {return cursor - i;}
        @Override protected boolean hasNext(final int i) {return i != viewStart - 1;}
        
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
    
    @Override public ArraySequenceIterator forwardIterator() {return new FASI();}
    @Override public ArraySequenceIterator reverseIterator() {return new RASI();}
}