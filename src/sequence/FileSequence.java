package sequence;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import sequence.Sequence.SequenceIterator;
import util.FixedSizeCharset;

/**
 * A {@linkplain Sequence} backed by a {@linkplain RandomAccessFile}.
 * 
 * @author AzureTriple
 * 
 * @implSpec Although this specific class does not implement the
 *           {@linkplain MutableSequence} interface, it does not guarantee that
 *           the file itself will not be modified by external processes.
 * 
 * @implNote Objects of this type may leak resources if not closed.
 */
public class FileSequence implements Sequence {
    /**This is not declared in settings because it is for debug only.*/
    private static final int MAX_STRING_SIZE = 8192;
    
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
    
    /**Wraps an {@linkplain IOException} in an {@linkplain UncheckedIOException}.*/
    protected static UncheckedIOException ioe(final IOException e) {
        return new UncheckedIOException(e);
    }
    /**Wraps an {@linkplain Exception} in an {@linkplain UncheckedIOException}.*/
    protected static UncheckedIOException ioe(final Exception e) {
        return ioe(e instanceof IOException? (IOException)e : new IOException(e));
    }
    
    private String suffix;
    private File file;
    private RandomAccessFile data;
    private FixedSizeCharset cs;
    private long start,end,length; // Measured in bytes.
    private boolean big;
    private final Mutability mutability;
    
    protected FileSequence(final File file,final long start,final long end,final long length,
                           final Mutability mutability,final String suffix,final FixedSizeCharset cs)
                           throws UncheckedIOException {
        try {data = new RandomAccessFile(this.file = file,(this.mutability = mutability).mode);}
        catch(FileNotFoundException|SecurityException e) {throw ioe(e);}
        this.start = start;
        this.length = length;
        this.end = end;
        this.suffix = suffix;
        big = (this.cs = cs).size > 1;
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
        big = (this.cs = cs).size > 1;
    }
    
    protected static class FileSequenceBuilder {
        private File data = null;
        private Long start = null,end = null,length = null;
        private Charset cs = null;
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
        /**
         * @param start The first character index, inclusive.
         * 
         * @return <code>this</code>
         */
        public FileSequenceBuilder start(final Long start) {
            this.start = start;
            return this;
        }
        /**
         * @param end The last character index, exclusive.
         * 
         * @return <code>this</code>
         */
        public FileSequenceBuilder end(final Long end) {
            this.end = end;
            length = null;
            return this;
        }
        /**
         * @param length The total number of characters.
         * 
         * @return <code>this</code>
         */
        public FileSequenceBuilder length(final Long length) {
            this.length = length;
            end = null;
            return this;
        }
        /**
         * @param start {@linkplain #start(Long)}
         * @param end   {@linkplain #end(Long)}
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
         * @param offset  {@linkplain #start(Long)}
         * @param length {@linkplain #length(Long)}
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
         * @param cs The charset used to decode the file.
         * 
         * @return <code>this</code>
         */
        public FileSequenceBuilder charset(final Charset cs) {
            this.cs = cs;
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
            if(cs == null) cs = StandardCharsets.UTF_8;
            // Make temporary file which contains characters with a fixed size.
            final String suffix;
            final FixedSizeCharset fscs;
            {
                final File tmp = Files.createTempFile(
                    TMP_DIR.toPath(),
                    null,
                    suffix = ".%s.%s".formatted(mutability().toString(),data.getName())
                ).toFile();
                tmp.deleteOnExit();
                fscs = FixedSizeCharset.transfer(data,tmp,cs);
                data = tmp;
            }
            return new FileSequence(
                data,
                start * fscs.size,
                end * fscs.size,
                length * fscs.size,
                mutability(),
                suffix,
                fscs
            );
        }
    }
    public static FileSequenceBuilder builder() {return new FileSequenceBuilder();}
    
    @Override public int length() {return (int)size();}
    @Override public long size() {return length / cs.size;}
    
    /**
     * @param idx    The index of the desired character. Negative values indicate an
     *               offset from the end instead of the start.
     * @param start  The index of the first byte (inclusive).
     * @param end    The index of the last byte (exclusive).
     * @param scalar The number of bytes per character.
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
                .formatted(idx * scalar,start,end,out - start,end - start)
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
    
    protected static char read(final RandomAccessFile data,final boolean big) throws IOException {
        return big? data.readChar() : (char)data.read();
    }
    protected char read() throws IOException {return read(data,big);}
    @Override
    public char charAt(final long index) throws IndexOutOfBoundsException,UncheckedIOException {
        try {data.seek(idx(index)); return read();}
        catch(final IOException e) {throw ioe(e);}
    }
    @Override
    public char charAt(final int index) throws IndexOutOfBoundsException,UncheckedIOException {
        return charAt((long)index);
    }
    
    /**
     * Same as {@linkplain #idx(long,long,long,long)}, except <code>end</code> is
     * included in the range of valid indices.
     */
    protected static long ssidx(final long idx,final long start,final long end,final long scalar)
                                throws IndexOutOfBoundsException {
        final long out = idx * scalar + (idx < 0L? end : start);
        if(end < out || out < start)
            throw new IndexOutOfBoundsException(
                "%d is outside the range [%d,%d] (shifted: %d,[0,%d])."
                .formatted(idx * scalar,start,end,out - start,end - start)
            );
        return out;
    }
    /**
     * Same as {@linkplain #idx(long)}, except <code>end</code> is included in the
     * range of valid indices.
     */
    protected long ssidx(final long idx) throws IndexOutOfBoundsException {
        return ssidx(idx,start,end,cs.size);
    }
    @Override
    public FileSequence subSequence(long start,long end) throws IndexOutOfBoundsException,
                                                                UncheckedIOException {
        if((end = ssidx(end)) < (start = ssidx(start)))
            throw new IndexOutOfBoundsException(
                "Range [%d,%d) is invalid."
                .formatted(end,start)
            );
        return new FileSequence(file,start,end,end - start,mutability,suffix,cs);
    }
    @Override
    public FileSequence subSequence(final int start,final int end) throws IndexOutOfBoundsException,
                                                                          UncheckedIOException {
        return subSequence((long)start,(long)end);
    }
    
    /**
     * A {@linkplain SimpleSequenceIterator} view of the characters stored in a
     * file.
     */
    protected class SFSI implements SimpleSequenceIterator {
        protected final File viewFile = file;
        protected final RandomAccessFile viewData;
        protected final long viewEnd = end;
        protected long cursor = start;
        protected final boolean viewBig = big;
        
        protected SFSI() throws UncheckedIOException {
            // Create new view of the data to prevent interference.
            try {viewData = new RandomAccessFile(viewFile,"r");}
            catch(FileNotFoundException|SecurityException e) {throw ioe(e);}
        }
        
        @Override
        public SFSI skip(final long count) throws IllegalArgumentException,
                                                  NoSuchElementException,
                                                  UncheckedIOException {
            if(count == 0L) return this;
            if(count < 0L)
                throw new IllegalArgumentException(
                    "Negative offset %d."
                    .formatted(count)
                );
            if(cursor + count >= viewEnd)
                throw new NoSuchElementException(
                    "Cannot skip %d characters after index %d."
                    .formatted(count,cursor)
                );
            try {viewData.seek(cursor += count);}
            catch(final IOException e) {throw ioe(e);}
            return this;
        }
        
        @Override public boolean hasNext() {return cursor != viewEnd;}
        @Override
        public Character next() throws NoSuchElementException,UncheckedIOException {
            if(!hasNext()) throw new NoSuchElementException();
            ++cursor;
            try {return viewBig? viewData.readChar() : (char)viewData.read();}
            catch(final IOException e) {throw ioe(e);}
        }
        
        @Override
        public void forEachRemaining(final Consumer<? super Character> action) throws UncheckedIOException {
            if(action == null) return;
            try {
                if(viewBig) {
                    while(cursor < viewEnd) {
                        action.accept(viewData.readChar());
                        ++cursor;
                    }
                } else {
                    while(cursor < viewEnd) {
                        action.accept((char)viewData.read());
                        ++cursor;
                    }
                }
            } catch(final IOException e) {throw ioe(e);}
        }
        
        @Override
        public void close() throws UncheckedIOException {
            try {viewData.close();}
            catch(final IOException e) {throw ioe(e);}
        }
    }
    /**@implNote See note in {@linkplain Sequence#iterator()}.*/
    @Override public SimpleSequenceIterator iterator() throws UncheckedIOException {return new SFSI();}
    
    /**A {@linkplain SequenceIterator} view of the characters stored in a file.*/
    protected abstract class FSI implements SequenceIterator {
        // Indices are measured in bytes.
        protected final String viewSuffix = suffix;
        protected final long viewStart = start,viewEnd = end,lastIdx;
        protected final File viewFile = file;
        protected final RandomAccessFile viewData;
        protected final FixedSizeCharset viewCS = cs;
        protected final int scalar = viewCS.size;
        protected final boolean viewBig = big;
        protected long cursor,mark;
        
        protected FSI(final long begin,final long end) throws UncheckedIOException {
            // Create new view of the data to prevent interference.
            try {viewData = new RandomAccessFile(viewFile,mutability.mode);}
            catch(FileNotFoundException|SecurityException e) {throw ioe(e);}
            cursor = mark = begin;
            lastIdx = end;
        }
        
        protected char get(final long i) throws UncheckedIOException {
            try {viewData.seek(i); return viewBig? viewData.readChar() : (char)viewData.read();} 
            catch(final IOException e) {throw ioe(e);}
        }
        protected abstract void increment();
        protected abstract long offset(long i);
        protected boolean oob(final long i) {return viewEnd <= i || i < viewStart;}
        protected abstract long skipidx(long i);
        
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
        
        @Override public boolean hasNext() {return cursor != lastIdx;}
        @Override
        public Character next() throws UncheckedIOException {
            if(hasNext()) {
                final char c = get(cursor);
                increment();
                return c;
            }
            return null;
        }
        
        protected abstract Character iSWS(long limit) throws UncheckedIOException;
        @Override
        public Character skipWS() throws UncheckedIOException {
            return iSWS(lastIdx);
        }
        @Override
        public Character skipWS(final int limit) throws UncheckedIOException {
            return skipWS((long)limit);
        }
        @Override
        public Character skipWS(final long limit) throws UncheckedIOException {
            return iSWS(skipidx(limit));
        }
        
        protected abstract Character iPNWS(long limit) throws UncheckedIOException;
        @Override
        public Character peekNonWS() throws UncheckedIOException {
            return iPNWS(lastIdx);
        }
        @Override
        public Character peekNonWS(final int limit) throws UncheckedIOException {
            return peekNonWS((long)limit);
        }
        @Override
        public Character peekNonWS(final long limit) throws UncheckedIOException {
            return iPNWS(skipidx(limit));
        }
        
        protected abstract Character iPNNWS(long limit) throws UncheckedIOException;
        @Override
        public Character peekNextNonWS() throws UncheckedIOException {
            return iPNNWS(lastIdx);
        }
        @Override
        public Character peekNextNonWS(final int limit) throws UncheckedIOException {
            return peekNextNonWS((long)limit);
        }
        @Override
        public Character peekNextNonWS(final long limit) throws UncheckedIOException {
            return iPNNWS(skipidx(limit));
        }
        
        protected abstract Character iNNWS(long limit) throws UncheckedIOException;
        @Override
        public Character nextNonWS() throws UncheckedIOException {
            return iNNWS(lastIdx);
        }
        @Override
        public Character nextNonWS(final int limit) throws UncheckedIOException {
            return nextNonWS((long)limit);
        }
        @Override
        public Character nextNonWS(final long limit) throws UncheckedIOException {
            return iNNWS(skipidx(limit));
        }
        
        @Override public void mark() throws IndexOutOfBoundsException {mark(0L);}
        @Override public void mark(final int offset) throws IndexOutOfBoundsException {mark((long)offset);}
        
        /* IMPORTANT: The jump methods should NEVER send the cursor to an out-of-bounds position. */
        @Override
        public FSI jumpTo(final int index) throws IndexOutOfBoundsException {
            return jumpTo((long)index);
        }
        @Override
        public FSI jumpTo(final long index) throws IndexOutOfBoundsException {
            cursor = FileSequence.idx(index,viewStart,viewEnd,scalar);
            return this;
        }
        @Override
        public FSI jumpOffset(final int offset) throws IndexOutOfBoundsException {
            return jumpOffset((long)offset);
        }
        @Override
        public FSI jumpOffset(final long offset) throws IndexOutOfBoundsException {
            final long nc = offset(offset);
            if(oob(nc))
                throw new IndexOutOfBoundsException(
                    "Cannot jump to index %d (range: [%d,%d),input: %d)."
                    .formatted(nc,viewStart,viewEnd,offset)
                );
            cursor = nc;
            return this;
        }
        
        protected abstract long subBegin();
        protected abstract long subEnd();
        @Override
        public FileSequence subSequence() throws UncheckedIOException {
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
            try{
                return viewCS.stringDecode(
                    (int)Math.min(strLength() / scalar,MAX_STRING_SIZE),
                    viewData,
                    strBegin() / scalar
                );
            } catch(final IOException e) {throw ioe(e);}
        }
        
        @Override
        public void close() throws UncheckedIOException {
            try {viewData.close();}
            catch(final IOException e) {throw ioe(e);}
        }
    }
    /**Forward File Sequence Iterator*/
    protected class FFSI extends FSI {
        public FFSI() throws UncheckedIOException {super(start,end);}
        
        @Override public long offset() {return (cursor - viewStart) / scalar;}
        
        @Override protected void increment() {cursor += scalar;}
        @Override protected long offset(final long i) {return cursor + i * scalar;}
        @Override protected long skipidx(final long i) {return Math.min(i * scalar,viewEnd);}
        
        @Override
        protected Character iSWS(long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            if(cursor < limit) {
                try {
                    viewData.seek(cursor);
                    if(viewBig) {
                        do {
                            final char c = viewData.readChar();
                            if(!Character.isWhitespace(c)) return c;
                        } while((cursor += 2L) != limit);
                    } else {
                        do {
                            final char c = (char)viewData.read();
                            if(!Character.isWhitespace(c)) return c;
                        } while(++cursor != limit);
                    }
                } catch(final IOException e) {throw ioe(e);}
            }
            return null;
        }
        @Override
        protected Character iPNWS(final long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            if(cursor < limit) {
                try {
                    data.seek(cursor);
                    final char c = viewBig? data.readChar() : (char)data.read();
                    return Character.isWhitespace(c)? iPNNWS(limit) : c;
                } catch(final IOException e) {throw ioe(e);}
            }
            return null;
        }
        @Override
        protected Character iPNNWS(final long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            long tmp = cursor + scalar;
            if(tmp < limit) {
                try {
                    viewData.seek(tmp);
                    if(viewBig) {
                        do {
                            final char c = viewData.readChar();
                            if(!Character.isWhitespace(c)) return c;
                        } while((tmp += 2L) != limit);
                    } else {
                        do {
                            final char c = (char)viewData.read();
                            if(!Character.isWhitespace(c)) return c;
                        } while(++tmp != limit);
                    }
                } catch(final IOException e) {throw ioe(e);}
            }
            return null;
        }
        @Override
        protected Character iNNWS(final long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            if(cursor < limit && (cursor += scalar) != limit) {
                try {
                    viewData.seek(cursor);
                    if(viewBig) {
                        do {
                            final char c = viewData.readChar();
                            if(!Character.isWhitespace(c)) return c;
                        } while((cursor += 2L) != limit);
                    } else {
                        do {
                            final char c = (char)viewData.read();
                            if(!Character.isWhitespace(c)) return c;
                        } while(++cursor != limit);
                    }
                } catch(final IOException e) {throw ioe(e);}
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
        
        @Override protected long strBegin() {return cursor;}
        @Override protected long strLength() {return viewEnd - cursor;}
    }
    /**Reverse File Sequence Iterator*/
    protected class RFSI extends FSI {
        public RFSI() throws UncheckedIOException {super(end - cs.size,start - cs.size);}
        
        @Override public long offset() {return (viewEnd - cursor) / scalar - 1L;}
        
        @Override protected void increment() {cursor -= scalar;}
        @Override protected long offset(final long i) {return cursor - i * scalar;}
        @Override protected long skipidx(final long i) {return Math.max(i * scalar,viewStart) - scalar;}
        
        @Override
        protected Character iSWS(final long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            if(cursor > limit) {
                try {
                    if(viewBig) {
                        do {
                            viewData.seek(cursor);
                            final char c = viewData.readChar();
                            if(!Character.isWhitespace(c)) return c;
                        } while((cursor -= 2L) != limit);
                    } else {
                        do {
                            viewData.seek(cursor);
                            final char c = (char)viewData.read();
                            if(!Character.isWhitespace(c)) return c;
                        } while(--cursor != limit);
                    }
                } catch(final IOException e) {throw ioe(e);}
            }
            return null;
        }
        @Override
        protected Character iPNWS(final long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            if(cursor > limit) {
                try {
                    data.seek(cursor);
                    final char c = viewBig? data.readChar() : (char)data.read();
                    return Character.isWhitespace(c)? iPNNWS(limit) : c;
                } catch(final IOException e) {throw ioe(e);}
            }
            return null;
        }
        @Override
        protected Character iPNNWS(final long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            long tmp = cursor - scalar;
            if(tmp > limit) {
                try {
                    if(viewBig) {
                        do {
                            data.seek(tmp);
                            final char c = data.readChar();
                            if(!Character.isWhitespace(c)) return c;
                        } while((tmp -= 2L) != limit);
                    } else {
                        do {
                            data.seek(tmp);
                            final char c = (char)data.read();
                            if(!Character.isWhitespace(c)) return c;
                        } while(--tmp != limit);
                    }
                } catch(final IOException e) {throw ioe(e);}
            }
            return null;
        }
        @Override
        protected Character iNNWS(final long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            if(cursor > limit && (cursor -= scalar) != limit) {
                try {
                    if(viewBig) {
                        do {
                            data.seek(cursor);
                            final char c = data.readChar();
                            if(!Character.isWhitespace(c)) return c;
                        } while((cursor -= 2L) != limit);
                    } else {
                        do {
                            data.seek(cursor);
                            final char c = (char)data.read();
                            if(!Character.isWhitespace(c)) return c;
                        } while(--cursor != limit);
                    }
                } catch(final IOException e) {throw ioe(e);}
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
        
        @Override protected long strBegin() {return Math.max(viewStart,cursor - scalar * MAX_STRING_SIZE);}
        @Override protected long strLength() {return cursor - viewStart + scalar;}
    }
    
    /**@implNote See note in {@link Sequence#forwardIterator()}.*/
    @Override
    public SequenceIterator forwardIterator() throws UncheckedIOException {
        return isEmpty()? EMPTY.forwardIterator() : new FFSI();
    }
    /**@implNote See note in {@link Sequence#reverseIterator()}.*/
    @Override
    public SequenceIterator reverseIterator() throws UncheckedIOException {
        return isEmpty()? EMPTY.reverseIterator() : new RFSI();
    }
    
    @Override
    public void close() throws UncheckedIOException {
        try {data.close();}
        catch(final IOException e) {throw ioe(e);}
    }
    
    @Override
    public String toString() throws UncheckedIOException {
        try {return cs.stringDecode(MAX_STRING_SIZE,data);}
        catch(final IOException e) {throw ioe(e);}
    }
}