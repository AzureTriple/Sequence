package sequence;

import static sequence.Sequence.EMPTY;

import java.io.UncheckedIOException;
import sequence.CompoundSequence.CSConstructor;

/**
 * A builder for {@linkplain CompoundSequence} objects.
 * 
 * @author AzureTriple
 */
public class CompoundSequenceBuilder implements SequenceBuilder {
    public CompoundSequenceBuilder() {}
    public static CompoundSequenceBuilder builder() {return new CompoundSequenceBuilder();}
    
    private Sequence[] data = null;
    private Long start,end,length;
    
    /**
     * Sets the data of this sequence to hold the specified child sequences.
     * 
     * @return <code>this</code>
     */
    public CompoundSequenceBuilder data(final Sequence...data) {
        this.data = data;
        return this;
    }
    /**
     * Sets the index of the first character in the sequence. This number can be
     * negative, in which case the index will be set relative to the last index
     * (inclusive), or <code>null</code> to use the default value.
     * 
     * @return <code>this</code>
     */
    public CompoundSequenceBuilder start(final Long start) {
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
    public CompoundSequenceBuilder end(final Long end) {
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
    public CompoundSequenceBuilder length(final Long length) {
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
    public CompoundSequenceBuilder range(final Long start,final Long end) {
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
     * 
     * @return <code>this</code>
     */
    public CompoundSequenceBuilder offset(final Long offset,final Long length) {
        start = offset;
        this.length = length;
        end = null;
        return this;
    }
    
    CSConstructor constructor() {return CompoundSequence.CONSTRUCTOR;}
    Sequence prepSequence(final Sequence in) {
        return in instanceof MutableSequence? in.immutableCopy()
                                            : in;
    }
    /**
     * @throws IllegalArgumentException The indices are outside the input data or
     *                                  represent a negative length.
     */
    @Override
    public Sequence build() throws IllegalArgumentException,
                                   UncheckedIOException {
        if(data == null || data.length == 0) return EMPTY;
        // Remove null and empty sequences.
        int set = 0;
        long ts = 0;
        long[] sizes = new long[data.length];
        for(int i = 0;i < data.length;++i) {
            if(data[i] != null && !data[i].isEmpty()) {
                data[set] = prepSequence(data[i]);
                sizes[set] = ts += data[set].size();
                ++set;
            }
        }
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
        return set == 0? data[0].subSequence(start,end)
                       : CompoundSequence.internalSS(data,sizes,start,end,constructor());
    }
}