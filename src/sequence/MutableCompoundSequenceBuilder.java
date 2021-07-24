package sequence;

import java.io.UncheckedIOException;
import sequence.CompoundSequence.CSConstructor;

/**
 * A builder for {@linkplain MutableCompoundSequence} objects.
 * 
 * @author AzureTriple
 * 
 * @see CompoundSequenceBuilder
 */
public class MutableCompoundSequenceBuilder extends CompoundSequenceBuilder {
    public MutableCompoundSequenceBuilder() {super();}
    public static MutableCompoundSequenceBuilder builder() {return new MutableCompoundSequenceBuilder();}
    
    @Override
    public MutableCompoundSequenceBuilder data(final Sequence...data) {
        super.data(data);
        return this;
    }
    @Override
    public MutableCompoundSequenceBuilder start(final Long start) {
        super.start(start);
        return this;
    }
    @Override
    public MutableCompoundSequenceBuilder end(final Long end) {
        super.end(end);
        return this;
    }
    @Override
    public MutableCompoundSequenceBuilder length(final Long length) {
        super.length(length);
        return this;
    }
    @Override
    public MutableCompoundSequenceBuilder range(final Long start,final Long end) {
        super.range(start,end);
        return this;
    }
    @Override
    public MutableCompoundSequenceBuilder offset(final Long offset,final Long length) {
        super.offset(offset,length);
        return this;
    }
    
    @Override
    Sequence prepSequence(final Sequence in) throws UncheckedIOException {
        // Make a copy even if already mutable to prevent external interference.
        return in.mutableCopy();
    }
    @Override CSConstructor constructor() {return MutableCompoundSequence.CONSTRUCTOR;}
    @Override
    public MutableSequence build() throws IllegalArgumentException,
                                          UncheckedIOException {
        return (MutableSequence)super.build();
    }
}