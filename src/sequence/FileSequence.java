package sequence;

import java.util.Iterator;
import java.util.NoSuchElementException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import util.FixedSizeCharset;

/**
 * A {@linkplain Sequence} backed by a {@linkplain RandomAccessFile}.
 * 
 * @author AzureTriple
 * 
 * @implSpec Although this specific class does not implement the
 *           {@linkplain MutableSequence} interface, it does not guarantee that
 *           the file itself will not be modified by external processes.
 */
public class FileSequence implements Sequence,AutoCloseable {
    protected static final File TMP_DIR;
    static {
        // Create temporary directories
        TMP_DIR = Paths.get(System.getProperty("user.dir"),"sequence-tmp").toFile();
        TMP_DIR.mkdir();
        TMP_DIR.deleteOnExit();
    }
    
    protected static enum Mutability {
        IMMUTABLE("r",true),
        MUTABLE("rw",false);
        
        public final String mode;
        public final boolean shared;
        private Mutability(final String mode,final boolean shared) {
            this.mode = mode;
            this.shared = shared;
        }
    }
    
    private String suffix;
    private File file;
    private RandomAccessFile data;
    private FixedSizeCharset cs;
    private long start,end,length; // Measured in bytes (2 bytes/char).
    private final Mutability mutability;
    
    protected FileSequence(final File file,final long start,final long end,final long length,
                           final Mutability mutability,final String suffix,
                           final FixedSizeCharset cs)
                           throws UncheckedIOException,SecurityException {
        try {data = new RandomAccessFile(this.file = file,(this.mutability = mutability).mode);}
        catch(final IOException e) {throw new UncheckedIOException(e);}
        this.start = start;
        this.length = length;
        this.end = end;
        this.suffix = suffix;
        this.cs = cs;
    }
    protected FileSequence(final File file,final RandomAccessFile data,
                           final long start,final long end,final long length,
                           final Mutability mutability,final String suffix,
                           final FixedSizeCharset cs) {
        this.file = file;
        this.data = data;
        this.start = start;
        this.end = end;
        this.length = length;
        this.mutability = mutability;
        this.suffix = suffix;
        this.cs = cs;
    }
    
    public static class FileSequenceBuilder {
        private File data = null;
        private Long start = null,end = null,length = null;
        protected Mutability mutability() {return Mutability.IMMUTABLE;}
        
        public FileSequenceBuilder data(final File data) {
            this.data = data;
            return this;
        }
        public FileSequenceBuilder data(final Path data) {
            this.data = data == null? null : data.toFile();
            return this;
        }
        public FileSequenceBuilder data(final String data) {
            this.data = new File(data);
            return this;
        }
        public FileSequenceBuilder start(final Long start) {
            this.start = start;
            return this;
        }
        public FileSequenceBuilder end(final Long end) {
            this.end = end;
            length = null;
            return this;
        }
        public FileSequenceBuilder length(final Long length) {
            this.length = length;
            end = null;
            return this;
        }
        /**
         * @param start {@linkplain #start(Long)}
         * @param end   {@linkplain #end(Long)}
         */
        public FileSequenceBuilder range(final Long start,final Long end) {
            this.start = start;
            this.end = end;
            length = null;
            return this;
        }
        /**
         * @param offset  {@linkplain #start(Long)}
         * @param length {@linkplain #length(Long)}
         */
        public FileSequenceBuilder offset(final Long offset,final Long length) {
            start = offset;
            this.length = length;
            end = null;
            return this;
        }
        
        public Sequence build() throws FileNotFoundException,SecurityException,IOException {
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
            if(length % 2 != 0)
                throw new IllegalArgumentException(
                    "The range [%d,%d) has odd length %d."
                    .formatted(start,end,length)
                );
            // Make temporary file which contains characters with a fixed size.
            final String suffix;
            final FixedSizeCharset cs;
            {
                final File tmp = Files.createTempFile(
                    TMP_DIR.toPath(),
                    null,
                    suffix = ".%s.%s".formatted(mutability().toString(),data.getName())
                ).toFile();
                tmp.deleteOnExit();
                cs = FixedSizeCharset.transfer(data,tmp,StandardCharsets.UTF_8);
                data = tmp;
            }
            return new FileSequence(
                data,
                start,end,length,
                mutability(),
                suffix,
                cs
            );
        }
    }
    public static FileSequenceBuilder builder() {return new FileSequenceBuilder();}
    
    @Override public int length() {return (int)size();}
    @Override public long size() {return length / cs.size;}
    
    /**
     * @param idx   The index of the desired character. Negative values indicate an
     *              offset from the end instead of the start.
     * @param start The index of the first byte (inclusive).
     * @param end   The index of the last byte (exclusive).
     * 
     * @return The index of the byte in the file.
     * 
     * @throws IndexOutOfBoundsException <code>|idx| &ge; (end - start)</code>
     */
    protected static long idx(final long idx,final long start,final long end,final long scalar)
                              throws IndexOutOfBoundsException {
        final long out = idx * scalar + (idx < 0L? end : start);
        if(end <= out || out < start)
            throw new IndexOutOfBoundsException(
                "%d is outside the range [%d,%d) (shifted: %d,[0,%d))."
                .formatted(idx,start,end,out - start,end - start)
            );
        return out;
    }
    /**
     * @param idx The index of the desired character. Negative values indicate an
     *            offset from the end instead of the start.
     * 
     * @return The index of the byte in the file.
     * 
     * @throws IndexOutOfBoundsException <code>|idx| &ge; size()</code>
     */
    protected long idx(final long idx) throws IndexOutOfBoundsException {return idx(idx,start,end,cs.size);}
    
    @Override
    public char charAt(final long index) throws IndexOutOfBoundsException,UncheckedIOException {
        try {data.seek(idx(index)); return data.readChar();}
        catch(final IOException e) {throw new UncheckedIOException(e);}
    }
    @Override
    public char charAt(final int index) throws IndexOutOfBoundsException,UncheckedIOException {
        return charAt((long)index);
    }
    
    protected FileSequence internalSS(final long start,final long end)
                                      throws UncheckedIOException,SecurityException {
        return new FileSequence(file,start,end,end - start,mutability,suffix,cs);
    }
    /**
     * Same as {@linkplain #idx(long)}, except <code>end</code> is included in the
     * range of valid indices.
     */
    protected long ssidx(final long idx) throws IndexOutOfBoundsException {
        final long out = idx + (idx < 0L? end : start);
        if(end < out || out < start)
            throw new IndexOutOfBoundsException(
                "%d is outside the range [%d,%d] (shifted: %d,[0,%d])."
                .formatted(idx,start,end,out - start,end - start)
            );
        return out;
    }
    @Override
    public FileSequence subSequence(long start,long end) throws IndexOutOfBoundsException,
                                                                UncheckedIOException,
                                                                SecurityException {
        if((end = ssidx(end)) < (start = ssidx(start)))
            throw new IndexOutOfBoundsException(
                "Range [%d,%d) is invalid."
                .formatted(end,start)
            );
        return internalSS(start,end);
    }
    @Override
    public FileSequence subSequence(final int start,final int end) throws IndexOutOfBoundsException,
                                                                          UncheckedIOException,
                                                                          SecurityException {
        return subSequence((long)start,(long)end);
    }
    
    /**
     * A {@linkplain SimpleSequenceIterator} view of the characters stored in a
     * file.
     */
    public class SimpleFileSequenceIterator implements Iterator<Character>,AutoCloseable {
        protected final File viewFile = file;
        protected final RandomAccessFile viewData;
        protected final long viewEnd = end;
        protected long cursor = start;
        
        protected SimpleFileSequenceIterator() throws UncheckedIOException,SecurityException {
            // Create new view of the data to prevent interference.
            try {viewData = new RandomAccessFile(viewFile,"r");}
            catch(final IOException e) {throw new UncheckedIOException(e);}
        }
        
        @Override public boolean hasNext() {return cursor != viewEnd;}
        @Override
        public Character next() throws NoSuchElementException,UncheckedIOException {
            if(cursor == viewEnd) throw new NoSuchElementException();
            ++cursor;
            try {return viewData.readChar();}
            catch(final IOException e) {throw new UncheckedIOException(e);}
        }
        
        @Override public void close() throws IOException {viewData.close();}
    }
    /**
     * @implNote The {@linkplain SimpleFileSequenceIterator} class is
     *           {@linkplain AutoCloseable} to ensure that the backing file is
     *           closed. Therefore, this method should be called in a
     *           try-with-resourses block.
     * 
     * @see sequence.Sequence#iterator()
     */
    @Override
    public SimpleFileSequenceIterator iterator() throws UncheckedIOException,
                                                        SecurityException {
        return new SimpleFileSequenceIterator();
    }
    
    /**A {@linkplain SequenceIterator} view of the characters stored in a file.*/
    public abstract class FileSequenceIterator implements SequenceIterator,AutoCloseable {
        protected final String viewSuffix = suffix;
        protected final long viewStart = start,viewEnd = end;
        protected final File viewFile = file;
        protected final RandomAccessFile viewData;
        protected final FixedSizeCharset viewCS = cs;
        protected final int scalar = viewCS.size;
        protected long cursor,mark; // Indices are measured in bytes (2 bytes/char).
        
        protected FileSequenceIterator(final long begin) throws UncheckedIOException,
                                                                SecurityException {
            // Create new view of the data to prevent interference.
            try {viewData = new RandomAccessFile(viewFile,mutability.mode);}
            catch(final IOException e) {throw new UncheckedIOException(e);}
            cursor = mark = begin;
        }
        
        protected char get(final long i) throws UncheckedIOException {
            try {viewData.seek(i); return viewData.readChar();} 
            catch(final IOException e) {throw new UncheckedIOException(e);}
        }
        protected abstract void increment();
        protected abstract long offset(long i);
        protected boolean oob(final long i) {return viewEnd <= i || i < viewStart;}
        
        @Override public long index() {return (cursor - viewStart) / scalar;}
        @Override public FileSequence getParent() {return FileSequence.this;}
        
        @Override
        public Character peek() throws UncheckedIOException {
            return hasNext()? null : get(cursor);
        }
        @Override
        public Character peek(final int offset) throws UncheckedIOException {
            return peek((long)offset);
        }
        @Override
        public Character peek(long offset) throws UncheckedIOException {
            return oob(offset = offset(offset))? null : get(offset);
        }
        
        @Override
        public Character next() throws UncheckedIOException {
            if(hasNext()) {
                final char c = get(cursor);
                increment();
                return c;
            }
            return null;
        }
        
        @Override public abstract Character skipWS() throws UncheckedIOException;
        @Override public abstract Character peekNextNonWS() throws UncheckedIOException;
        @Override public abstract Character nextNonWS() throws UncheckedIOException;
        
        @Override public void mark() throws IndexOutOfBoundsException {mark(0L);}
        @Override public void mark(final int offset) throws IndexOutOfBoundsException {mark((long)offset);}
        
        @Override
        public FileSequenceIterator jumpTo(final int index) throws IndexOutOfBoundsException {
            return jumpTo((long)index);
        }
        @Override
        public FileSequenceIterator jumpTo(final long index) throws IndexOutOfBoundsException {
            cursor = FileSequence.idx(index,viewStart,viewEnd,scalar);
            return this;
        }
        @Override
        public FileSequenceIterator jumpOffset(final int offset) throws IndexOutOfBoundsException {
            return jumpOffset((long)offset);
        }
        @Override
        public FileSequenceIterator jumpOffset(final long offset) throws IndexOutOfBoundsException {
            if(oob(cursor = offset(offset)))
                throw new IndexOutOfBoundsException(
                    "Cannot jump to index %d (range: [%d,%d),input: %d)."
                    .formatted(cursor,start,end,offset)
                );
            return this;
        }
        
        protected abstract long subBegin();
        protected abstract long subEnd();
        @Override
        public FileSequence subSequence() throws UncheckedIOException,SecurityException {
            final long a = subBegin(),b = subEnd();
            if(b < a)
                throw new IndexOutOfBoundsException(
                    "Range [%d,%d) is invalid."
                    .formatted(a,b)
                );
            return new FileSequence(viewFile,a,b,b - a,mutability,viewSuffix,viewCS);
        }
        
        protected abstract long strBegin();
        protected abstract long strLength();
        @Override
        public String toString() throws UncheckedIOException {
            try {return viewCS.stringDecode(0,viewData);}
            catch(final IOException e) {throw new UncheckedIOException(e);}
        }
        
        @Override public void close() throws IOException {viewData.close();}
    }
    /**Forward File Sequence Iterator*/
    protected class FFSI extends FileSequenceIterator {
        public FFSI() throws UncheckedIOException,SecurityException {super(start);}
        
        @Override protected void increment() {cursor += scalar;}
        @Override protected long offset(final long i) {return cursor + i * scalar;}
        
        @Override public boolean hasNext() {return cursor != viewEnd;}
        
        @Override
        public Character skipWS() throws UncheckedIOException {
            if(hasNext()) {
                try {
                    viewData.seek(cursor);
                    do {
                        final char c = viewData.readChar();
                        if(!Character.isWhitespace(c)) return c;
                    } while((cursor += scalar) != viewEnd);
                } catch(final IOException e) {throw new UncheckedIOException(e);}
            }
            return null;
        }
        @Override
        public Character peekNextNonWS() throws UncheckedIOException {
            if(!oob(cursor + scalar)) {
                try {
                    viewData.seek(cursor + scalar);
                    for(long tmp = cursor;(tmp += scalar) != viewEnd;) {
                        final char c = viewData.readChar();
                        if(!Character.isWhitespace(c)) return c;
                    }
                } catch(final IOException e) {throw new UncheckedIOException(e);}
            }
            return null;
        }
        @Override
        public Character nextNonWS() throws UncheckedIOException {
            if(hasNext() && (cursor += scalar) != viewEnd) {
                try {
                    viewData.seek(cursor);
                    do {
                        final char c = viewData.readChar();
                        if(!Character.isWhitespace(c)) return c;
                    } while((cursor += scalar) != viewEnd);
                } catch(final IOException e) {throw new UncheckedIOException(e);}
            }
            return null;
        }
        
        @Override
        public void mark(final long offset) throws IndexOutOfBoundsException {
            if(oob(mark = offset(offset)) && mark != viewEnd)
                throw new IndexOutOfBoundsException(
                    "Cannot mark index %d (range: [%d,%d],input: %d)."
                    .formatted(mark,viewStart,viewEnd,offset)
                );
        }
        
        @Override protected long subBegin() {return mark;}
        @Override protected long subEnd() {return cursor;}
        
        @Override protected long strBegin() {return viewStart;}
        @Override protected long strLength() {return cursor - mark;}
    }
    /**Reverse File Sequence Iterator*/
    protected class RFSI extends FileSequenceIterator {
        public RFSI() throws UncheckedIOException,SecurityException {super(end - cs.size);}
        
        @Override protected void increment() {cursor -= scalar;}
        @Override protected long offset(final long i) {return cursor - i * scalar;}
        
        @Override public boolean hasNext() {return cursor != viewStart - scalar;}
        
        @Override
        public Character skipWS() throws UncheckedIOException {
            if(hasNext()) {
                try {
                    do {
                        viewData.seek(cursor);
                        final char c = viewData.readChar();
                        if(!Character.isWhitespace(c)) return c;
                    } while((cursor -= scalar) != viewStart - scalar);
                } catch(final IOException e) {throw new UncheckedIOException(e);}
            }
            return null;
        }
        @Override
        public Character peekNextNonWS() throws UncheckedIOException {
            if(cursor != viewStart) { // equivalent to (cursor - scalar != viewStart - scalar)
                try {
                    for(long tmp = cursor;tmp != viewStart;) {
                        data.seek(tmp -= scalar);
                        final char c = data.readChar();
                        if(!Character.isWhitespace(c)) return c;
                    }
                } catch(final IOException e) {throw new UncheckedIOException(e);}
            }
            return null;
        }
        @Override
        public Character nextNonWS() throws UncheckedIOException {
            if(hasNext() && (cursor -= scalar) != viewStart - scalar) {
                try {
                    do {
                        data.seek(cursor);
                        final char c = data.readChar();
                        if(!Character.isWhitespace(c)) return c;
                    } while((cursor -= scalar) != viewStart - scalar);
                } catch(final IOException e) {throw new UncheckedIOException(e);}
            }
            return null;
        }
        
        @Override
        public void mark(final long offset) throws IndexOutOfBoundsException {
            if(oob(mark = offset(offset)) && mark != viewStart - scalar)
                throw new IndexOutOfBoundsException(
                    "Cannot mark index %d (range: [%d,%d),input: %d)."
                    .formatted(mark + scalar,viewStart,viewEnd,offset)
                );
        }
        
        @Override protected long subBegin() {return cursor + scalar;}
        @Override protected long subEnd() {return mark + scalar;}
        
        @Override protected long strBegin() {return cursor + scalar;}
        @Override protected long strLength() {return viewEnd - cursor - scalar;}
    }
    
    /**
     * @implNote The {@linkplain FileSequenceIterator} class is
     *           {@linkplain AutoCloseable} to ensure that the backing file is
     *           closed. Therefore, this method should be called in a
     *           try-with-resourses block.
     * 
     * @see sequence.Sequence#iterator()
     */
    @Override
    public FileSequenceIterator forwardIterator() throws UncheckedIOException,
                                                         SecurityException {
        return new FFSI();
    }
    /**
     * @implNote The {@linkplain FileSequenceIterator} class is
     *           {@linkplain AutoCloseable} to ensure that the backing file is
     *           closed. Therefore, this method should be called in a
     *           try-with-resourses block.
     * 
     * @see sequence.Sequence#iterator()
     */
    @Override
    public FileSequenceIterator reverseIterator() throws UncheckedIOException,
                                                         SecurityException {
        return new RFSI();
    }
    
    @Override public void close() throws IOException {data.close();}
}





























