package sequence;

import static sequence.Sequence.EMPTY;

import util.NoIO;

/**
 * A builder for {@linkplain ArraySequence} objects.
 * 
 * @author AzureTriple
 */
@NoIO
public class ArraySequenceBuilder implements SequenceBuilder {
    public ArraySequenceBuilder() {}
    public static ArraySequenceBuilder builder() {return new ArraySequenceBuilder();}
    
    private char[] data = null;
    private Integer start = null,end = null,length = null;
    
    /**
     * Sets the data of this sequence to hold the specified characters.
     * 
     * @return <code>this</code>
     */
    public ArraySequenceBuilder data(final char...data) {
        this.data = data;
        return this;
    }
    /**
     * Sets the data of this sequence to hold the specified characters.
     * 
     * @return <code>this</code>
     */
    public ArraySequenceBuilder data(final CharSequence data) {
        this.data = data.toString().toCharArray();
        return this;
    }
    /**
     * Sets the index of the first character in the sequence. This number can be
     * negative, in which case the index will be set relative to the last index
     * (inclusive), or <code>null</code> to use the default value.
     * 
     * @return <code>this</code>
     */
    public ArraySequenceBuilder start(final Integer start) {
        this.start = start;
        return this;
    }
    /**
     * Sets the index of the last character in the sequence, exclusive. This number
     * can be negative, in which case the index will be set relative to the last
     * index (inclusive), or <code>null</code> to use the default value. This method
     * clears the <code>length</code> value.
     * 
     * @return <code>this</code>
     */
    public ArraySequenceBuilder end(final Integer end) {
        this.end = end;
        length = null;
        return this;
    }
    /**
     * Sets the length of the sequence. This method clears the <code>end</code>
     * value.
     * 
     * @return <code>this</code>
     */
    public ArraySequenceBuilder length(final Integer length) {
        this.length = length;
        end = null;
        return this;
    }
    /**
     * A convenience method to set both the <code>start</code> and <code>end</code>
     * indices. This method clears the <code>length</code> value.
     * 
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
     * A convenience method to set both the <code>start</code> and
     * <code>length</code> indices. This method clears the <code>end</code> value.
     * 
     * @param offset {@linkplain #start(Integer)}
     * @param length {@linkplain #length(Integer)}
     */
    public ArraySequenceBuilder offset(final Integer offset,final Integer length) {
        start = offset;
        this.length = length;
        end = null;
        return this;
    }
    
    ArraySequence construct(final char[] data,
                            final int start,
                            final int end,
                            final int length) {
        return new ArraySequence(data,start,end,length);
    }
    /**
     * @throws IllegalArgumentException The indices are outside the input data or
     *                                  represent a negative length.
     */
    @NoIO @Override
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
        return length == 0? EMPTY : construct(data,start,end,length);
    }
}