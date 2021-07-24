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
    public MutableArraySequenceBuilder data(final char...data) {
        super.data(data);
        return this;
    }
    @Override
    public MutableArraySequenceBuilder data(final CharSequence data) {
        super.data(data);
        return this;
    }
    @Override
    public MutableArraySequenceBuilder start(final Integer start) {
        super.start(start);
        return this;
    }
    @Override
    public MutableArraySequenceBuilder end(final Integer end) {
        super.end(end);
        return this;
    }
    @Override
    public MutableArraySequenceBuilder length(final Integer length) {
        super.length(length);
        return this;
    }
    @Override
    public MutableArraySequenceBuilder range(final Integer start,final Integer end) {
        super.range(start,end);
        return this;
    }
    @Override
    public MutableArraySequenceBuilder offset(final Integer offset,final Integer length) {
        super.offset(offset,length);
        return this;
    }
    
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