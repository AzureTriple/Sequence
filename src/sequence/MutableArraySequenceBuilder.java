package sequence;

/**
 * A builder for {@linkplain MutableArraySequence} objects.
 * 
 * @author AzureTriple
 * 
 * @see ArraySequenceBuilder
 */
public class MutableArraySequenceBuilder extends ArraySequenceBuilder {
    public MutableArraySequenceBuilder() {super();}
    public static MutableArraySequenceBuilder builder() {return new MutableArraySequenceBuilder();}
    
    @Override
    ArraySequence construct(final char[] data,
                            final int start,
                            final int end,
                            final int length) {
        return new MutableArraySequence(data,start,end,length);
    }
    @Override
    public MutableSequence build() throws IllegalArgumentException {
        return (MutableSequence)super.build();
    }
}