package sequence;

import static sequence.Sequence.EMPTY;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import sequence.FileSequence.Mutability;
import util.FixedSizeCharset;

/**
 * A builder for {@linkplain FileSequence} objects.
 * 
 * @author AzureTriple
 */
public class FileSequenceBuilder implements SequenceBuilder {
    public FileSequenceBuilder() {}
    public static FileSequenceBuilder builder() {return new FileSequenceBuilder();}
    
    private File data = null;
    private Long start = null,end = null,length = null;
    private Charset cs = null;
    
    /**
     * Sets the data of this sequence to hold the contents of the specified file.
     * 
     * @return <code>this</code>
     */
    public FileSequenceBuilder data(final File data) {
        this.data = data;
        return this;
    }
    /**
     * Sets the data of this sequence to hold the contents of the specified file.
     * 
     * @return <code>this</code>
     */
    public FileSequenceBuilder data(final Path data) {
        this.data = data == null? null : data.toFile();
        return this;
    }
    /**
     * Sets the data of this sequence to hold the contents of the specified file.
     * 
     * @return <code>this</code>
     */
    public FileSequenceBuilder data(final String data) {
        this.data = data == null? null : new File(data);
        return this;
    }
    /**
     * Sets the index of the first character in the sequence. This number can be
     * negative, in which case the index will be set relative to the last index
     * (inclusive), or <code>null</code> to use the default value.
     * 
     * @return <code>this</code>
     */
    public FileSequenceBuilder start(final Long start) {
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
    public FileSequenceBuilder end(final Long end) {
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
    public FileSequenceBuilder length(final Long length) {
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
     * 
     * @return <code>this</code>
     */
    public FileSequenceBuilder range(final Long start,final Long end) {
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
    public FileSequenceBuilder offset(final Long offset,final Long length) {
        start = offset;
        this.length = length;
        end = null;
        return this;
    }
    /**
     * Sets the charset used to decode the input file.
     * 
     * @return <code>this</code>
     */
    public FileSequenceBuilder charset(final Charset cs) {
        this.cs = cs;
        return this;
    }
    
    FileSequence construct(final File data,
                           final long start,
                           final long end,
                           final long length,
                           final Charset cs)
                           throws IOException,SecurityException {
        final Mutability mut = Mutability.IMMUTABLE;
        final String suffix = data.getName();
        // Make temporary file which contains characters with a fixed size.
        final File tmp = Files.createTempFile(
            FileSequence.TMP_DIR.toPath(),
            null,
            ".%s.%s".formatted(mut.toString(),suffix)
        ).toFile();
        tmp.deleteOnExit();
        try {
            final FixedSizeCharset fscs = FixedSizeCharset.transfer(data,tmp,cs);
            
            return new FileSequence(
                tmp,
                start * fscs.size,
                end * fscs.size,
                length * fscs.size,
                mut,
                suffix,
                fscs
            );
        } catch(UncheckedIOException|IOException|SecurityException e) {
            try {tmp.delete();}
            catch(final SecurityException e1) {}
            if(e instanceof UncheckedIOException)
                throw ((UncheckedIOException)e).getCause();
            throw e;
        }
    }
    /**
     * @throws IllegalArgumentException The indices are outside the input data or
     *                                  represent a negative length.
     */
    @Override
    public Sequence build() throws IllegalArgumentException,
                                   UncheckedIOException {
        final long dataLength;
        if(data == null || !data.isFile() || (dataLength = data.length()) == 0L) return EMPTY;
        
        if(start == null) start = 0L;
        else if(dataLength < start || start < 0L && (start += dataLength) < 0L)
            throw new IllegalArgumentException(
                "Invalid start index %d for array of length %d."
                .formatted(start,dataLength)
            );
        
        if(end == null) {
            if(length == null) length = (end = dataLength) - start;
            else if(length < 0L || (end = length + start) > dataLength)
                throw new IllegalArgumentException(
                    "Length %d is invalid."
                    .formatted(length)
                );
        } else {
            if(dataLength < end || end < 0L && (end += dataLength) < 0L)
                throw new IllegalArgumentException(
                    "Invalid end index %d for array of length %d."
                    .formatted(end,dataLength)
                );
            if((length = end - start) < 0L)
                throw new IllegalArgumentException(
                    "Invalid range: [%d,%d)"
                    .formatted(start,end)
                );
        }
        if(length == 0L) return EMPTY;
        try {return construct(data,start,end,length,cs == null? StandardCharsets.UTF_8 : cs);}
        catch(IOException|SecurityException e) {throw FileSequence.ioe(e);}
    }
}