package sequence;

public class ArraySequence implements Sequence {
    protected char[] data;
    protected int start,end,length;
    
    private ArraySequence(final char[] data,final int start,final int end,final int length) {
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
    
    protected int idx(final int idx) throws IndexOutOfBoundsException {
        final int out = idx + (idx < 0? end : start);
        if(end <= out || out < start)
            throw new IndexOutOfBoundsException(
                "%d is outside the range [%d,%d) (shifted: %d,[0,%d))."
                .formatted(idx,start,end,out - start,end - start)
            );
        return out;
    }
    @Override
    public char charAt(int index) {
        return 0;
    }
    
    @Override
    public CharSequence subSequence(int start,int end) {
        return null;
    }
    
    @Override
    public SequenceIterator iterator() {
        return null;
    }
    
    @Override
    public SequenceIterator reverseIterator() {
        return null;
    }
    
}
