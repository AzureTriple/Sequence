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
    Sequence prepSequence(final Sequence in) {
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