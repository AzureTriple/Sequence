package sequence;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import sequence.FileSequence.Mutability;
import util.FixedSizeCharset;

/**
 * A builder for {@linkplain MutableFileSequence} objects.
 * 
 * @author AzureTriple
 * 
 * @see FileSequenceBuilder
 */
public class MutableFileSequenceBuilder extends FileSequenceBuilder {
    public MutableFileSequenceBuilder() {super();}
    public static MutableFileSequenceBuilder builder() {return new MutableFileSequenceBuilder();}
    
    @Override
    public MutableFileSequenceBuilder data(final File data) {
        super.data(data);
        return this;
    }
    @Override
    public MutableFileSequenceBuilder data(final Path data) {
        super.data(data);
        return this;
    }
    @Override
    public MutableFileSequenceBuilder data(final String data) {
        super.data(data);
        return this;
    }
    @Override
    public MutableFileSequenceBuilder start(final Long start) {
        super.start(start);
        return this;
    }
    @Override
    public MutableFileSequenceBuilder end(final Long end) {
        super.end(end);
        return this;
    }
    @Override
    public MutableFileSequenceBuilder length(final Long length) {
        super.length(length);
        return this;
    }
    @Override
    public MutableFileSequenceBuilder range(final Long start,final Long end) {
        super.range(start,end);
        return this;
    }
    @Override
    public MutableFileSequenceBuilder offset(final Long offset,final Long length) {
        super.offset(offset,length);
        return this;
    }
    @Override
    public MutableFileSequenceBuilder charset(final Charset cs) {
        super.charset(cs);
        return this;
    }
    
    FileSequence construct(File data,
                           final long start,
                           final long end,
                           final long length,
                           final Charset cs)
                           throws IOException,SecurityException {
        final String suffix;
        {
            final File tmp = Files.createTempFile(
                MutableFileSequence.TMP_DIR.toPath(),
                null,
                ".%s.%s".formatted(Mutability.MUTABLE.toString(),suffix = data.getName())
            ).toFile();
            tmp.deleteOnExit();
            FixedSizeCharset.transfer(data,cs,tmp,MutableFileSequence.MUTABLE_CS);
            data = tmp;
        }
        return new MutableFileSequence(
            data,
            start << 1,
            end << 1,
            length << 1,
            suffix
        );
    }
    @Override
    public MutableSequence build() throws IllegalArgumentException,
                                          UncheckedIOException {
        return (MutableSequence)super.build();
    }
}