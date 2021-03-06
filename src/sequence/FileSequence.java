package sequence;

import static java.lang.Character.isWhitespace;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.lang.ref.Cleaner.Cleanable;
import java.nio.file.Files;
import java.nio.file.Paths;
import util.FixedSizeCharset;
import util.NoIO;
import util.NoIO.Suppresses;

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
class FileSequence implements Sequence {
    static final File TMP_DIR;
    static {
        // Create temporary directories
        TMP_DIR = Paths.get(System.getProperty("user.dir"),"sequence-tmp").toFile();
        TMP_DIR.mkdir();
        TMP_DIR.deleteOnExit();
    }
    
    /**An enumeration of open-modes for files based on mutability.*/
    static enum Mutability {
        IMMUTABLE("r"),
        MUTABLE("rw");
        
        public final String mode;
        private Mutability(final String mode) {this.mode = mode;}
    }
    
    /**Wraps an {@linkplain IOException} in an {@linkplain UncheckedIOException}.*/
    static UncheckedIOException ioe(final IOException e) {
        return new UncheckedIOException(e);
    }
    /**Wraps an {@linkplain Exception} in an {@linkplain UncheckedIOException}.*/
    static UncheckedIOException ioe(final Exception e) {
        return ioe(e instanceof IOException? (IOException)e : new IOException(e));
    }
    
    static final class fscleaner implements Runnable {
        RandomAccessFile data;
        Exception e = null;
        fscleaner(final RandomAccessFile data) {this.data = data;}
        @Override
        public void run() {
            if(data != null) {
                try {data.close();}
                catch(final Exception e) {this.e = e;}
            }
        }
    }
    private final Cleanable cleanable;
    final fscleaner fsc;
    final String suffix;
    final File file;
    final RandomAccessFile data;
    FixedSizeCharset cs;
    long start,end,length; // Measured in bytes.
    final boolean big;
    final Mutability mutability;
    
    FileSequence(final File file,final long start,final long end,final long length,
                 final Mutability mutability,final String suffix,final FixedSizeCharset cs)
                 throws UncheckedIOException {
        try {
            cleanable = CleaningUtil.register(
                this,
                fsc = new fscleaner(
                    data = new RandomAccessFile(
                        this.file = file,
                        (this.mutability = mutability).mode
                    )
                )
            );
        } catch(FileNotFoundException|SecurityException e) {throw ioe(e);}
        this.start = start;
        this.length = length;
        this.end = end;
        this.suffix = suffix;
        big = (this.cs = cs).size > 1;
    }
    
    @Override
    public boolean equals(final Object obj) {
        return obj == this || obj instanceof CharSequence && compareTo((CharSequence)obj) == 0;
    }
    
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
    static long idx(final long idx,final long start,final long end,final long scalar)
                    throws IndexOutOfBoundsException {
        final long out = idx * scalar + (idx < 0L? end : start);
        if(end <= out || out < start)
            throw new IndexOutOfBoundsException(
                "%d is outside the range [%d,%d) (shifted: %d,[0,%d))."
                .formatted(idx * scalar,start,end,(out - start) / scalar,(end - start) / scalar)
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
    long idx(final long idx) throws IndexOutOfBoundsException {return idx(idx,start,end,cs.size);}
    
    static char read(final RandomAccessFile data,final boolean big) throws IOException {
        return big? data.readChar() : (char)data.read();
    }
    char read() throws IOException {return read(data,big);}
    @Override
    public char charAt(final int index) throws IndexOutOfBoundsException,UncheckedIOException {
        return charAt((long)index);
    }
    @Override
    public char charAt(final long index) throws IndexOutOfBoundsException,UncheckedIOException {
        try {data.seek(idx(index)); return read();}
        catch(final IOException e) {throw ioe(e);}
    }
    
    /**
     * Same as {@linkplain #idx(long,long,long,long)}, except <code>end</code> is
     * included in the range of valid indices.
     */
    static long ssidx(final long idx,final long start,final long end,final long scalar)
                                throws IndexOutOfBoundsException {
        final long out = idx * scalar + (idx < 0L? end : start);
        if(end < out || out < start)
            throw new IndexOutOfBoundsException(
                "%d is outside the range [%d,%d] (shifted: %d,[0,%d])."
                .formatted(idx * scalar,start,end,(out - start) / scalar,(end - start) / scalar)
            );
        return out;
    }
    /**
     * Same as {@linkplain #idx(long)}, except <code>end</code> is included in the
     * range of valid indices.
     */
    long ssidx(final long idx) throws IndexOutOfBoundsException {
        return ssidx(idx,start,end,cs.size);
    }
    @Override
    public Sequence subSequence(final int start,final int end) throws IndexOutOfBoundsException,
                                                                      UncheckedIOException {
        return subSequence((long)start,(long)end);
    }
    @Override
    public Sequence subSequence(long start,long end) throws IndexOutOfBoundsException,
                                                            UncheckedIOException {
        if((end = ssidx(end)) < (start = ssidx(start)))
            throw new IndexOutOfBoundsException(
                "Range [%d,%d) is invalid."
                .formatted(end / cs.size,start / cs.size)
            );
        return start != end? start != this.start || end != this.end
                ? new FileSequence(file,start,end,end - start,mutability,suffix,cs)
                : shallowCopy()
                : EMPTY;
              
    }
    
    /**
     * A {@linkplain SimpleSequenceIterator} view of the characters stored in a
     * file.
     */
    private static class SFSI implements SimpleSequenceIterator {
        final Cleanable cleanable;
        final fscleaner sfsc;
        // Indices are measured in characters.
        final RandomAccessFile data;
        final long start,end;
        long cursor = 0L;
        final boolean big;
        
        SFSI(final FileSequence parent) throws UncheckedIOException {
            // Create new view of the data to prevent interference.
            try {
                cleanable = CleaningUtil.register(
                    this,
                    sfsc = new fscleaner(
                        data = new RandomAccessFile(parent.file,"r")
                    )
                );
                data.seek(start = parent.start);
            } catch(IOException|SecurityException e) {throw ioe(e);}
            end = parent.size();
            big = parent.big;
        }
        
        @Override
        public SimpleSequenceIterator skip(final long count)
                                           throws IllegalArgumentException,
                                                  NoSuchElementException,
                                                  UncheckedIOException {
            if(count == 0L) return this;
            if(count < 0L)
                throw new IllegalArgumentException(
                    "Negative offset %d."
                    .formatted(count)
                );
            if(cursor + count >= end)
                throw new NoSuchElementException(
                    "Cannot skip %d characters after index %d."
                    .formatted(count,cursor)
                );
            try {data.seek(start + (cursor += count) * (big? 2L : 1L));}
            catch(final IOException e) {throw ioe(e);}
            return this;
        }
        
        @Override
        public boolean hasNext() throws UncheckedIOException {
            if(cursor != end) return true;
            close(); return false;
        }
        @Override
        public Character next() throws NoSuchElementException,UncheckedIOException {
            if(!hasNext()) throw new NoSuchElementException();
            ++cursor;
            try {return big? data.readChar() : (char)data.read();}
            catch(final IOException e) {throw ioe(e);}
        }
        
        @Override
        public void forEachRemaining(final Consumer<? super Character> action) throws UncheckedIOException {
            if(action == null) return;
            try {
                if(big) {
                    while(cursor < end) {
                        action.accept(data.readChar());
                        ++cursor;
                    }
                } else {
                    while(cursor < end) {
                        action.accept((char)data.read());
                        ++cursor;
                    }
                }
            } catch(final IOException e) {throw ioe(e);}
        }
        
        @Override
        public void close() throws UncheckedIOException {
            cursor = end;
            cleanable.clean();
            if(sfsc.e != null) throw ioe(sfsc.e);
        }
    }
    /**@implNote See note in {@linkplain Sequence#iterator()}.*/
    @Override public SimpleSequenceIterator iterator() throws UncheckedIOException {return new SFSI(this);}
    
    /**A {@linkplain SequenceIterator} view of the characters stored in a file.*/
    static abstract class FSI implements SequenceIterator {
        private final Cleanable cleanable;
        final fscleaner fsic;
        // Indices are measured in bytes.
        final String suffix;
        final long start,end,lastIdx;
        final File file;
        final RandomAccessFile data;
        final FixedSizeCharset cs;
        final int scalar;
        final boolean big;
        long cursor,mark;
        final FileSequence parent;
        
        FSI(final long begin,final long end,final FileSequence fs)
            throws UncheckedIOException {
            // Create new view of the data to prevent interference.
            try {
                cleanable = CleaningUtil.register(
                    this,
                    fsic = new fscleaner(
                        data = new RandomAccessFile(fs.file,fs.mutability.mode)
                    )
                );
            }
            catch(FileNotFoundException|SecurityException e) {throw ioe(e);}
            cursor = mark = begin;
            lastIdx = end;
            this.parent = fs;
            suffix = fs.suffix;
            start = fs.start;
            this.end = fs.end;
            file = fs.file;
            cs = fs.cs;
            scalar = fs.cs.size;
            big = fs.big;
        }
        
        char get(final long i) throws UncheckedIOException {
            try {data.seek(i); return big? data.readChar() : (char)data.read();} 
            catch(final IOException e) {throw ioe(e);}
        }
        abstract void increment();
        abstract long offset(long i);
        boolean oob(final long i) {return end <= i || i < start;}
        abstract long skipidx(long i);
        
        @Override public long index() {return (cursor - start) / scalar;}
        @Override public Sequence getParent() {return parent;}
        
        @Override
        public Character peek() throws UncheckedIOException {
            return hasNext()? get(cursor) : null;
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
        
        abstract Character iSWS(long limit) throws UncheckedIOException;
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
        
        abstract Character iPNWS(long limit) throws UncheckedIOException;
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
        
        abstract Character iPNNWS(long limit) throws UncheckedIOException;
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
        
        abstract Character iNNWS(long limit) throws UncheckedIOException;
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
        
        abstract boolean iFind(final long limit,final char c) throws UncheckedIOException;
        @Override
        public boolean find(final char c) throws UncheckedIOException {
            return iFind(lastIdx,c);
        }
        @Override
        public boolean find(final int limit,final char c) throws UncheckedIOException {
            return iFind(skipidx(limit),c);
        }
        @Override
        public boolean find(final long limit,final char c) throws UncheckedIOException {
            return iFind(skipidx(limit),c);
        }
        
        @Override public SequenceIterator mark() throws IndexOutOfBoundsException {return mark(0L);}
        @Override public SequenceIterator mark(final int offset) throws IndexOutOfBoundsException {return mark((long)offset);}
        @Override public abstract SequenceIterator mark(long offset) throws IndexOutOfBoundsException;
        
        /* IMPORTANT: The jump methods should NEVER send the cursor to an out-of-bounds position. */
        @NoIO(suppresses = Suppresses.EXCEPTIONS) @Override
        public SequenceIterator jumpTo(final int index) throws IndexOutOfBoundsException {
            return jumpTo((long)index);
        }
        @NoIO(suppresses = Suppresses.EXCEPTIONS) @Override
        public SequenceIterator jumpTo(final long index) throws IndexOutOfBoundsException {
            cursor = FileSequence.idx(index,start,end,scalar);
            return this;
        }
        @NoIO(suppresses = Suppresses.EXCEPTIONS) @Override
        public SequenceIterator jumpOffset(final int offset) throws IndexOutOfBoundsException {
            return jumpOffset((long)offset);
        }
        @NoIO(suppresses = Suppresses.EXCEPTIONS) @Override
        public SequenceIterator jumpOffset(final long offset) throws IndexOutOfBoundsException {
            final long nc = offset(offset);
            if(oob(nc))
                throw new IndexOutOfBoundsException(
                    "Cannot jump to index %d (range: [%d,%d),input: %d)."
                    .formatted(nc / scalar,start / scalar,end / scalar,offset)
                );
            cursor = nc;
            return this;
        }
        
        abstract long subBegin();
        abstract long subEnd();
        @Override
        public Sequence subSequence() throws UncheckedIOException {
            final long a = subBegin(),b = subEnd();
            if(b < a)
                throw new IndexOutOfBoundsException(
                    "Range [%d,%d) is invalid."
                    .formatted(a / scalar,b / scalar)
                );
            return a != b? a != start || b != end
                    ? new FileSequence(file,a,b,b - a,parent.mutability,suffix,cs)
                    : parent.shallowCopy()
                    : EMPTY;
        }
        
        abstract long strBegin();
        abstract long strLength();
        @Override
        public String toString() throws UncheckedIOException {
            try{
                return cs.stringDecode(
                    (int)min(strLength() / scalar,Integer.MAX_VALUE),
                    data,
                    strBegin() / scalar
                );
            } catch(final IOException e) {throw ioe(e);}
        }
        
        @Override
        public void close() throws UncheckedIOException {
            cleanable.clean();
            if(fsic.e != null) throw ioe(fsic.e);
        }
    }
    /**Forward File Sequence Iterator*/
    static class FFSI extends FSI {
        FFSI(final FileSequence fs) throws UncheckedIOException {
            super(fs.start,fs.end,fs);
        }
        
        @Override public long offset() {return (cursor - start) / scalar;}
        
        @Override void increment() {cursor += scalar;}
        @Override long offset(final long i) {return cursor + i * scalar;}
        @Override long skipidx(final long i) {return min(i * scalar,end);}
        
        @Override
        Character iSWS(long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            if(cursor < limit) {
                try {
                    data.seek(cursor);
                    if(big) {
                        do {
                            final char c = data.readChar();
                            if(!isWhitespace(c)) return c;
                        } while((cursor += 2L) != limit);
                    } else {
                        do {
                            final char c = (char)data.read();
                            if(!isWhitespace(c)) return c;
                        } while(++cursor != limit);
                    }
                } catch(final IOException e) {throw ioe(e);}
            }
            return null;
        }
        @Override
        Character iPNWS(final long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            if(cursor < limit) {
                try {
                    data.seek(cursor);
                    final char c = big? data.readChar() : (char)data.read();
                    return isWhitespace(c)? iPNNWS(limit)
                                          // The cast keeps the return value of iPNNWS from
                                          // auto-unboxing, which allows it to return null.
                                          : (Character)c;
                } catch(final IOException e) {throw ioe(e);}
            }
            return null;
        }
        @Override
        Character iPNNWS(final long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            long tmp = cursor + scalar;
            if(tmp < limit) {
                try {
                    data.seek(tmp);
                    if(big) {
                        do {
                            final char c = data.readChar();
                            if(!isWhitespace(c)) return c;
                        } while((tmp += 2L) != limit);
                    } else {
                        do {
                            final char c = (char)data.read();
                            if(!isWhitespace(c)) return c;
                        } while(++tmp != limit);
                    }
                } catch(final IOException e) {throw ioe(e);}
            }
            return null;
        }
        @Override
        Character iNNWS(final long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            if(cursor < limit && (cursor += scalar) != limit) {
                try {
                    data.seek(cursor);
                    if(big) {
                        do {
                            final char c = data.readChar();
                            if(!isWhitespace(c)) return c;
                        } while((cursor += 2L) != limit);
                    } else {
                        do {
                            final char c = (char)data.read();
                            if(!isWhitespace(c)) return c;
                        } while(++cursor != limit);
                    }
                } catch(final IOException e) {throw ioe(e);}
            }
            return null;
        }
        @Override
        boolean iFind(final long limit,final char c) throws UncheckedIOException {
            if(cursor < limit) {
                try {
                    data.seek(cursor);
                    if(big) {
                        do {
                            cursor += 2L;
                            if(data.readChar() == c) return true;
                        } while(cursor != limit);
                    } else {
                        do {
                            ++cursor;
                            if(data.read() == c) return true;
                        } while(cursor != limit);
                    }
                } catch(final IOException e) {throw ioe(e);}
            }
            return false;
        }
        
        @Override
        public SequenceIterator mark(final long offset) throws IndexOutOfBoundsException {
            if(oob(mark = offset(offset)) && mark != end)
                throw new IndexOutOfBoundsException(
                    "Cannot mark index %d (range: [%d,%d],input: %d)."
                    .formatted(mark / scalar,start / scalar,end / scalar,offset)
                );
            return this;
        }
        
        @Override long subBegin() {return mark;}
        @Override long subEnd() {return cursor;}
        
        @Override long strBegin() {return cursor;}
        @Override long strLength() {return end - cursor;}
    }
    /**Reverse File Sequence Iterator*/
    static class RFSI extends FSI {
        RFSI(final FileSequence fs) throws UncheckedIOException {
            super(fs.end - fs.cs.size,fs.start - fs.cs.size,fs);
        }
        
        @Override public long offset() {return (end - cursor) / scalar - 1L;}
        
        @Override void increment() {cursor -= scalar;}
        @Override long offset(final long i) {return cursor - i * scalar;}
        @Override long skipidx(final long i) {return max(i,-1L) * scalar + start;}
        
        @Override
        Character iSWS(final long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            if(cursor > limit) {
                try {
                    if(big) {
                        do {
                            data.seek(cursor);
                            final char c = data.readChar();
                            if(!isWhitespace(c)) return c;
                        } while((cursor -= 2L) != limit);
                    } else {
                        do {
                            data.seek(cursor);
                            final char c = (char)data.read();
                            if(!isWhitespace(c)) return c;
                        } while(--cursor != limit);
                    }
                } catch(final IOException e) {throw ioe(e);}
            }
            return null;
        }
        @Override
        Character iPNWS(final long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            if(cursor > limit) {
                try {
                    data.seek(cursor);
                    final char c = big? data.readChar() : (char)data.read();
                    return isWhitespace(c)? iPNNWS(limit)
                                          // The cast keeps the return value of iPNNWS from
                                          // auto-unboxing, which allows it to return null.
                                          : (Character)c;
                } catch(final IOException e) {throw ioe(e);}
            }
            return null;
        }
        @Override
        Character iPNNWS(final long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            long tmp = cursor - scalar;
            if(tmp > limit) {
                try {
                    if(big) {
                        do {
                            data.seek(tmp);
                            final char c = data.readChar();
                            if(!isWhitespace(c)) return c;
                        } while((tmp -= 2L) != limit);
                    } else {
                        do {
                            data.seek(tmp);
                            final char c = (char)data.read();
                            if(!isWhitespace(c)) return c;
                        } while(--tmp != limit);
                    }
                } catch(final IOException e) {throw ioe(e);}
            }
            return null;
        }
        @Override
        Character iNNWS(final long limit) throws UncheckedIOException {
            // This method trusts that the cursor never underflows via jump.
            if(cursor > limit && (cursor -= scalar) != limit) {
                try {
                    if(big) {
                        do {
                            data.seek(cursor);
                            final char c = data.readChar();
                            if(!isWhitespace(c)) return c;
                        } while((cursor -= 2L) != limit);
                    } else {
                        do {
                            data.seek(cursor);
                            final char c = (char)data.read();
                            if(!isWhitespace(c)) return c;
                        } while(--cursor != limit);
                    }
                } catch(final IOException e) {throw ioe(e);}
            }
            return null;
        }
        @Override
        boolean iFind(final long limit,final char c) throws UncheckedIOException {
            if(cursor > limit) {
                try {
                    if(big) {
                        do {
                            data.seek(cursor);
                            cursor -= 2L;
                            if(data.readChar() == c) return true;
                        } while(cursor != limit);
                    } else {
                        do {
                            data.seek(cursor--);
                            if(data.read() == c) return true;
                        } while(cursor != limit);
                    }
                } catch(final IOException e) {throw ioe(e);}
            }
            return false;
        }
        
        @Override
        public SequenceIterator mark(final long offset) throws IndexOutOfBoundsException {
            if(oob(mark = offset(offset)) && mark != start - scalar)
                throw new IndexOutOfBoundsException(
                    "Cannot mark index %d (range: [%d,%d),input: %d)."
                    .formatted(mark / scalar + 1L,start / scalar,end / scalar,offset)
                );
            return this;
        }
        
        @Override long subBegin() {return cursor + scalar;}
        @Override long subEnd() {return mark + scalar;}
        
        @Override long strBegin() {return max(start,cursor - scalar * Integer.MAX_VALUE);}
        @Override long strLength() {return cursor - start + scalar;}
    }
    
    /**@implNote See note in {@link Sequence#forwardIterator()}.*/
    @Override
    public SequenceIterator forwardIterator() throws UncheckedIOException {
        return isEmpty()? EMPTY.forwardIterator() : new FFSI(this);
    }
    /**@implNote See note in {@link Sequence#reverseIterator()}.*/
    @Override
    public SequenceIterator reverseIterator() throws UncheckedIOException {
        return isEmpty()? EMPTY.reverseIterator() : new RFSI(this);
    }
    
    @Override
    public void close() throws UncheckedIOException {
        cleanable.clean();
        if(fsc.e != null) throw ioe(fsc.e);
    }
    
    @Override
    public String toString() throws UncheckedIOException {
        try {return cs.stringDecode((int)min(Integer.MAX_VALUE,size()),data,start);}
        catch(final IOException e) {throw ioe(e);}
    }
    
    @Override
    public Sequence copyTo(final char[] arr,final int offset) throws IllegalArgumentException,
                                                                     UncheckedIOException {
        final long size = size();
        if(size != 0L) {
            final int offs = offset < 0? offset + arr.length : offset;
            if(arr.length <= offs || offs < 0 || offs + size > arr.length)
                throw new IllegalArgumentException(
                    "Cannot copy sequence of size %d to an array of size %d at index %d."
                    .formatted(size,arr.length,offs)
                );
            try {
                data.seek(start);
                final int l = (int)(offs + size);
                if(big)
                    for(int i = offs;i < l;++i) arr[i] = data.readChar();
                else
                    for(int i = offs;i < l;++i) arr[i] = (char)data.read();
            } catch(final IOException e) {throw ioe(e);}
        }
        return this;
    }
    
    File tmpFile(final Mutability mut) throws UncheckedIOException {
        try {
            final File cpy = Files.createTempFile(
                TMP_DIR.toPath(),
                null,
                ".%s.%s".formatted(mut.toString(),suffix)
            ).toFile();
            cpy.deleteOnExit();
            return cpy;
        } catch(IOException|SecurityException e) {throw ioe(e);}
    }
    @Override
    public MutableSequence mutableCopy() throws UncheckedIOException {
        final File nf = tmpFile(Mutability.MUTABLE);
        try {
            FixedSizeCharset.transfer(file,cs,nf,MutableFileSequence.MUTABLE_CS);
            return new MutableFileSequence(nf,start,end,length,suffix);
        } catch(UncheckedIOException|IOException|SecurityException e) {
            try {nf.delete();}
            catch(final SecurityException e1) {}
            throw ioe(e);
        }
    }
    @Override
    public Sequence immutableCopy() throws UncheckedIOException {
        final File nf = tmpFile(Mutability.IMMUTABLE);
        // Immutable file sequence is already in it's minimal form, don't need to re-encode.
        try(BufferedInputStream I = new BufferedInputStream(new FileInputStream(file));
            BufferedOutputStream O = new BufferedOutputStream(new FileOutputStream(nf))) {
            I.transferTo(O);
            return new FileSequence(nf,start,end,length,Mutability.IMMUTABLE,suffix,cs);
        } catch(UncheckedIOException|IOException|SecurityException e) {
            try {nf.delete();}
            catch(final SecurityException e1) {}
            throw ioe(e);
        }
    }
    @Override public boolean closeIsShared() {return true;}
    @Override
    public Sequence shallowCopy() throws UncheckedIOException {
        return new FileSequence(file,start,end,length,mutability,suffix,cs);
    }
}