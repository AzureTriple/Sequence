package sequence;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
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