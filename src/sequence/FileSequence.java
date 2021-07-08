package sequence;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class FileSequence implements Sequence {
    private File file;
    private RandomAccessFile data;
    private long start,end,length; // Measured in bytes (2 bytes/char).
    
    protected String mode() {return "r";}
    private FileSequence(final File file,final long start,final long end,final long length)
                         throws UncheckedIOException,SecurityException {
        try {data = new RandomAccessFile(this.file = file,mode());}
        catch(final FileNotFoundException e) {throw new UncheckedIOException(e);}
        this.start = start;
        this.end = end;
        this.length = length;
    }
    
    public static class FileSequenceBuilder {
        private File data = null;
        private Long start = null,end = null,length = null;
        
        public FileSequenceBuilder data(final File data) {
            this.data = data;
            return this;
        }
        public FileSequenceBuilder data(final Path data) {
            this.data = data == null? null : data.toFile();
            return this;
        }
        public FileSequenceBuilder data(final String data) {
            this.data = data == null? null : new File(data);
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
            if(data == null || (dataLength = data.length()) == 0L) return EMPTY;
            
            if(start == null) start = 0L;
            else if(dataLength < start || start < 0L && (start += dataLength) < 0L)
                throw new IllegalArgumentException(
                    "Invalid start index %d for array of length %d."
                    .formatted(start,dataLength)
                );
            
            if(end == null) {
                if(length == null) length = (end = dataLength) - start;
                else if(length < 0 || (end = length + start) > dataLength)
                    throw new IllegalArgumentException(
                        "Length %d is invalid."
                        .formatted(length)
                    );
            } else {
                if(dataLength < end || end < 0 && (end += dataLength) < 0)
                    throw new IllegalArgumentException(
                        "Invalid end index %d for array of length %d."
                        .formatted(end,dataLength)
                    );
                if((length = end - start) < 0)
                    throw new IllegalArgumentException(
                        "Invalid range: [%d,%d)"
                        .formatted(start,end)
                    );
            }
            if(length % 2 != 0)
                throw new IllegalArgumentException(
                    "The range [%d,%d) has odd length %d."
                    .formatted(start,end,length)
                );
            return length == 0? EMPTY : new FileSequence(data,start,end,length);
        }
    }
    public static FileSequenceBuilder builder() {return new FileSequenceBuilder();}
    
    @Override public int length() {return (int)size();}
    @Override public long size() {return length / 2L;}
    
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
    protected static long idx(final long idx,final long start,final long end)
                              throws IndexOutOfBoundsException {
        final long out = idx * 2L + (idx < 0L? end : start);
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
    protected long idx(final long idx) throws IndexOutOfBoundsException {return idx(idx,start,end);}
    
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
        return new FileSequence(file,start,end,end - start);
    }
    /**
     * Same as {@linkplain #subSequence(int,int)}, but takes long values to account
     * for the size difference.
     * 
     * @param start Index of the first character (inclusive).
     * @param end   Index of the last character (exclusive).
     * 
     * @throws IndexOutOfBoundsException The indices represent an invalid range or
     *                                   {@linkplain #idx(long)} threw.
     * 
     * @see #idx(long)
     */
    public FileSequence subSequence(long start,long end) throws IndexOutOfBoundsException,
                                                                UncheckedIOException,
                                                                SecurityException {
        if((end = idx(end)) < (start = idx(start)))
            throw new IndexOutOfBoundsException(
                "Range [%d,%d) is invalid."
                .formatted(end,start)
            );
        return internalSS(start,end);
    }
    /**
     * @throws IndexOutOfBoundsException The indices represent an invalid range or
     *                                   {@linkplain #idx(long)} threw.
     * 
     * @see #subSequence(long,long)
     */
    @Override
    public FileSequence subSequence(final int start,final int end) throws IndexOutOfBoundsException,
                                                                          UncheckedIOException,
                                                                          SecurityException {
        return subSequence((long)start,(long)end);
    }
    
    /**
     * A {@linkplain SequenceIterator} view of the characters stored in a file. This
     * class locks the relevant region of the file in order to ensure that data does
     * not change during iteration.
     */
    public abstract class FileSequenceIterator implements SequenceIterator,AutoCloseable {
        protected final FileLock lock;
        protected final long viewStart,viewEnd;
        protected final File viewFile;
        protected final RandomAccessFile viewData;
        protected String mode() {return "r";}
        {
            try {
                // Create new view of the data to prevent interference. TODO test
                lock = (viewData = new RandomAccessFile(viewFile = file,mode())).getChannel().tryLock(start,length,true);
                if(lock == null) throw new OverlappingFileLockException();
            } catch(final OverlappingFileLockException e) {throw new UncheckedIOException(new IOException(e));}
            catch(final IOException e) {throw new UncheckedIOException(e);}
            viewStart = start;
            viewEnd = end;
        }
        protected long cursor,mark; // Indices are measured in bytes (2 bytes/char).
        
        protected FileSequenceIterator(final long begin) throws UncheckedIOException,
                                                                SecurityException {
            cursor = mark = begin;
        }
        
        protected char get(final long i) throws UncheckedIOException {
            try {viewData.seek(i); return viewData.readChar();} 
            catch(final IOException e) {throw new UncheckedIOException(e);}
        }
        protected abstract void increment();
        protected abstract long offset(long i);
        protected boolean oob(final long i) {return viewEnd <= i || i < viewStart;}
        
        @Override public long index() {return (cursor - viewStart) / 2L;}
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
            cursor = FileSequence.idx(index,viewStart,viewEnd);
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
            return new FileSequence(viewFile,a,b,b - a);
        }
        
        protected abstract long strBegin();
        protected abstract long strLength();
        @Override
        public String toString() throws UncheckedIOException {
            try {
                return StandardCharsets.UTF_8.decode(
                    viewData.getChannel()
                            .map(
                                MapMode.READ_ONLY,
                                strBegin(),
                                strLength()
                            )
                ).toString();
            } catch(final IOException e) {throw new UncheckedIOException(e);}
        }
        
        @Override public void close() throws IOException {lock.release();}
    }
    /**Forward File Sequence Iterator*/
    protected class FFSI extends FileSequenceIterator {
        public FFSI() throws UncheckedIOException,SecurityException {super(start);}
        
        @Override protected void increment() {cursor += 2L;}
        @Override protected long offset(final long i) {return cursor + i * 2L;}
        
        @Override public boolean hasNext() {return cursor != viewEnd;}
        
        @Override
        public Character skipWS() throws UncheckedIOException {
            if(hasNext()) {
                try {
                    viewData.seek(cursor);
                    do {
                        final char c = viewData.readChar();
                        if(!Character.isWhitespace(c)) return c;
                    } while((cursor += 2L) != viewEnd);
                } catch(final IOException e) {throw new UncheckedIOException(e);}
            }
            return null;
        }
        @Override
        public Character peekNextNonWS() throws UncheckedIOException {
            if(!oob(cursor + 2L)) {
                try {
                    viewData.seek(cursor + 2L);
                    for(long tmp = cursor;(tmp += 2L) != viewEnd;) {
                        final char c = viewData.readChar();
                        if(!Character.isWhitespace(c)) return c;
                    }
                } catch(final IOException e) {throw new UncheckedIOException(e);}
            }
            return null;
        }
        @Override
        public Character nextNonWS() throws UncheckedIOException {
            if(hasNext() && (cursor += 2L) != viewEnd) {
                try {
                    viewData.seek(cursor);
                    do {
                        final char c = viewData.readChar();
                        if(!Character.isWhitespace(c)) return c;
                    } while((cursor += 2L) != viewEnd);
                } catch(final IOException e) {throw new UncheckedIOException(e);}
            }
            return null;
        }
        
        @Override
        public void mark(final long offset) throws IndexOutOfBoundsException {
            if(oob(mark = offset(offset)) && mark != viewEnd)
                throw new IndexOutOfBoundsException(
                    "Cannot mark index %d (range: [%d,%d),input: %d)."
                    .formatted(mark,start,end,offset)
                );
        }
        
        @Override protected long subBegin() {return mark;}
        @Override protected long subEnd() {return cursor;}
        
        @Override protected long strBegin() {return viewStart;}
        @Override protected long strLength() {return cursor - mark;}
    }
    /**Reverse File Sequence Iterator*/
    protected class RFSI extends FileSequenceIterator {
        public RFSI() throws UncheckedIOException,SecurityException {super(end - 2L);}
        
        @Override protected void increment() {cursor -= 2L;}
        @Override protected long offset(final long i) {return cursor - i * 2L;}
        
        @Override public boolean hasNext() {return cursor != viewStart - 2L;}
        
        @Override
        public Character skipWS() throws UncheckedIOException {
            if(hasNext()) {
                try {
                    do {
                        viewData.seek(cursor);
                        final char c = viewData.readChar();
                        if(!Character.isWhitespace(c)) return c;
                    } while((cursor -= 2L) != viewStart - 2L);
                } catch(final IOException e) {throw new UncheckedIOException(e);}
            }
            return null;
        }
        @Override
        public Character peekNextNonWS() throws UncheckedIOException {
            if(cursor != viewStart) { // equivalent to (cursor - 2L != viewStart - 2L)
                try {
                    for(long tmp = cursor;tmp != viewStart;) {
                        data.seek(tmp -= 2L);
                        final char c = data.readChar();
                        if(!Character.isWhitespace(c)) return c;
                    }
                } catch(final IOException e) {throw new UncheckedIOException(e);}
            }
            return null;
        }
        @Override
        public Character nextNonWS() throws UncheckedIOException {
            if(hasNext() && (cursor -= 2L) != viewStart - 2L) {
                try {
                    do {
                        data.seek(cursor);
                        final char c = data.readChar();
                        if(!Character.isWhitespace(c)) return c;
                    } while((cursor -= 2L) != viewStart - 2L);
                } catch(final IOException e) {throw new UncheckedIOException(e);}
            }
            return null;
        }
        
        @Override
        public void mark(final long offset) throws IndexOutOfBoundsException {
            if(oob(mark = offset(offset)) && mark != viewStart - 2L)
                throw new IndexOutOfBoundsException(
                    "Cannot mark index %d (range: [%d,%d),input: %d)."
                    .formatted(mark + 2L,viewStart,viewEnd,offset)
                );
        }
        
        @Override protected long subBegin() {return cursor + 2L;}
        @Override protected long subEnd() {return mark + 2L;}
        
        @Override protected long strBegin() {return cursor + 2L;}
        @Override protected long strLength() {return viewEnd - cursor - 2L;}
    }
    
    /**
     * @throws UncheckedIOException The iterator could not be created, likely
     *                              because another iterator locked the backing
     *                              file.
     * 
     * @implNote The {@linkplain FileSequenceIterator} class is
     *           {@linkplain AutoCloseable} to ensure that the file lock used to
     *           control access to the data is released. Therefore, this method
     *           should be called in a try-with-resourses block.
     * 
     * @see sequence.Sequence#iterator()
     */
    @Override
    public FileSequenceIterator iterator() throws UncheckedIOException,
                                                  SecurityException {
        return new FFSI();
    }
    /**
     * @throws UncheckedIOException The iterator could not be created, likely
     *                              because another iterator locked the backing
     *                              file.
     * 
     * @implNote The {@linkplain FileSequenceIterator} class is
     *           {@linkplain AutoCloseable} to ensure that the file lock used to
     *           control access to the data is released. Therefore, this method
     *           should be called in a try-with-resourses block.
     * 
     * @see sequence.Sequence#iterator()
     */
    @Override
    public FileSequenceIterator reverseIterator() throws UncheckedIOException,
                                                         SecurityException {
        return new RFSI();
    }
}





























