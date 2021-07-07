package sequence_old;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

/**
 * A {@linkplain Sequence} which is directly stored in a file.
 * 
 * @author AzureTriple
 */
public class FileSequence extends Sequence implements AutoCloseable {
    private RandomAccessFile data;
    private long start,end; // Indices are measured in bytes (2 bytes / char).
    
    // Constructor which bypasses checks
    protected FileSequence(final long start,final long end,
                           final RandomAccessFile data,
                           final boolean mutable) {
        super(mutable);
        this.data = data;
        this.start = start;
        this.end = end;
    }
    /**
     * Creates a file sequence.
     * 
     * @param data    File containing the sequence.
     * @param start   Index of the first byte (inclusive).
     * @param end     Index of the last byte (exclusive).
     * @param mutable <code>false</code> iff functions that change the start and end
     *                indices apply modifications to a new object.
     *                
     * @throws IndexOutOfBoundsException The indices represent an invalid range.
     * @throws NullPointerException      <code>data</code> is <code>null</code>.
     */
    public FileSequence(final RandomAccessFile data,
                        final long start,final long end,
                        final boolean mutable)
                        throws IndexOutOfBoundsException,
                               NullPointerException {
        super(mutable);
        if(end < start || start < 0L) // condition failing implies end >= 0
            throw new IndexOutOfBoundsException(
                "The range [%d,%d) is invalid."
                .formatted(start,end)
            );
        if((end - start) % 2 != 0)
            throw new IndexOutOfBoundsException(
                "The range [%d,%d) does not contain an even number of bytes."
                .formatted(start,end)
            );
        if((this.data = data) == null)
            throw new NullPointerException("Input file is null.");
        this.start = start;
        this.end = end;
    }
    /**
     * Creates an immutable file sequence.
     * 
     * @param data    File containing the sequence_old.
     * @param start   Index of the first byte (inclusive).
     * @param end     Index of the last byte (exclusive).
     *                
     * @throws IndexOutOfBoundsException The indices represent an invalid range.
     * @throws NullPointerException      <code>data</code> is <code>null</code>.
     */
    public FileSequence(final RandomAccessFile data,
                        final long start,final long end)
                        throws IndexOutOfBoundsException,
                               NullPointerException {
        this(data,start,end,DEFAULT_MUTABLE);
    }
    
    @Override public long size() {return (end - start) / 2L;}
    
    /**
     * This function should not be used instead of {@linkplain #size()}; this
     * method returns the same value but upper-bounded by
     * {@linkplain Integer#MAX_VALUE}. This method was only included to satisfy
     * {@linkplain CharSequence}.
     */
    @Override public int length() {return (int)Math.min(size(),Integer.MAX_VALUE);}
    
    /**
     * @param idx   The index of the desired character. Negative values indicate an
     *              offset from the end instead of the start.
     * @param start The index of the first byte (inclusive).
     * @param end   The index of the last byte (exclusive).
     * 
     * @return The index of the byte in the file.
     * 
     * @throws IndexOutOfBoundsException <code>|idx| &gt (end - start)</code>
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
     * @throws IndexOutOfBoundsException <code>|idx| &gt size()</code>
     */
    protected long idx(final long idx) throws IndexOutOfBoundsException {return idx(idx,start,end);}
    
    protected FileSequence internalSS(final long start,final long end) {
        if(mutable) {
            this.start = start;
            this.end = end;
            return this;
        }
        return new FileSequence(start,end,data,false);
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
    public FileSequence subSequence(final long start,final long end) throws IndexOutOfBoundsException {
        return internalSS(idx(start),idx(end));
    }
    /**
     * @throws IndexOutOfBoundsException {@linkplain #idx(long)} threw.
     * 
     * @see #subSequence(long,long)
     */
    @Override
    public FileSequence subSequence(final int start,final int end) throws IndexOutOfBoundsException {
        return subSequence((long)start,(long)end);
    }
    
    /**
     * Same as {@linkplain #charAt(int,int)}, but takes long values to account for
     * the size difference.
     * 
     * @param index The index of the character to read.
     * 
     * @throws IndexOutOfBoundsException {@linkplain #idx(long)} threw.
     * @throws UncheckedIOException
     */
    public char charAt(final long index) throws IndexOutOfBoundsException,
                                                UncheckedIOException {
        try {data.seek(idx(index)); return data.readChar();}
        catch(final IOException e) {throw new UncheckedIOException(e);}
    }
    /**
     * @throws IndexOutOfBoundsException {@linkplain #idx(long)} threw.
     * 
     * @see #charAt(long)
     */
    @Override
    public char charAt(final int index) throws IndexOutOfBoundsException,
                                               UncheckedIOException {
        return charAt((long)index);
    }
    
    /**
     * A {@linkplain SequenceIterator} view of the characters stored in a file. This
     * class locks the relevant region of the file in order to ensure that data does
     * not change during iteration.
     */
    public abstract class FileSequenceIterator implements SequenceIterator,AutoCloseable {
        protected final FileLock lock;
        protected final long viewStart,viewEnd;
        protected final RandomAccessFile viewData;
        {
            try {
                lock = (viewData = data).getChannel().tryLock(start,size(),true);
                if(lock == null) throw new OverlappingFileLockException();
            } catch(final OverlappingFileLockException e) {throw new UncheckedIOException(new IOException(e));}
            catch(final IOException e) {throw new UncheckedIOException(e);}
            viewStart = start;
            viewEnd = end;
        }
        protected long cursor,mark; // Indices are measured in bytes (2 bytes / char).
        
        protected FileSequenceIterator(final long begin) throws UncheckedIOException {
            cursor = mark = begin;
        }
        
        protected boolean oob(final long index) {return end <= index || index < start;}
        protected final long bound(final long index) throws IndexOutOfBoundsException {
            if(oob(index))
                throw new IndexOutOfBoundsException(
                    "%d is outside the range [%d,%d] (shifted: %d,[0,%d])."
                    .formatted(index,start,end,index - start,end - start)
                );
            return index;
        }
        protected abstract long offset(long offset);
        protected abstract void increment();
        
        @Override public long index() {return (cursor - start) / 2L;}
        @Override public FileSequence getParent() {return FileSequence.this;}
        
        protected Character seek(final long pos) throws UncheckedIOException {
            if(oob(pos)) return null;
            try {viewData.seek(pos); return viewData.readChar();}
            catch(final IOException e) {throw new UncheckedIOException(e);}
        }
        @Override public Character peek() throws UncheckedIOException {return seek(cursor);}
        @Override
        public Character peek(final int offset) throws UncheckedIOException {
            return peek((long)offset);
        }
        @Override
        public Character peek(long offset) throws UncheckedIOException {
            return seek(offset(offset));
        }
        
        @Override
        public Character next() {
            final Character out = seek(cursor);
            if(out != null) increment();
            return out;
        }
        
        @Override public void mark() {mark = cursor;}
        @Override
        public void mark(final int offset) throws IndexOutOfBoundsException {
            mark((long)offset);
        }
        @Override
        public void mark(final long offset) throws IndexOutOfBoundsException {
            mark = bound(offset(offset));
        }
        
        /**@see #jumpTo(long)*/
        @Override
        public FileSequenceIterator jumpTo(final int index) throws IndexOutOfBoundsException {
            return jumpTo((long)index);
        }
        /**@see sequence_old.Sequence.SequenceIterator#jumpTo(long)*/
        @Override
        public FileSequenceIterator jumpTo(final long index) throws IndexOutOfBoundsException {
            cursor = idx(index,viewStart,viewEnd);
            return this;
        }
        @Override
        public FileSequenceIterator jumpOffset(final int offset) throws IndexOutOfBoundsException {
            return jumpOffset((long)offset);
        }
        @Override
        public FileSequenceIterator jumpOffset(final long offset) throws IndexOutOfBoundsException {
            cursor = bound(offset(offset));
            return this;
        }
        
        @Override public void close() throws IOException {lock.release();}
    }
    /**Forward File Sequence Iterator*/
    private class FFSI extends FileSequenceIterator {
        protected FFSI() throws UncheckedIOException {super(start);}
        
        @Override protected long offset(final long offset) {return cursor + offset * 2L;}
        @Override protected void increment() {cursor += 2L;}
        
        @Override public boolean hasNext() {return cursor != end;}
        
        @Override
        public Character skipWS() throws UncheckedIOException {
            if(!oob(cursor)) {
                try {
                    data.seek(cursor);
                    do {
                        final char c = data.readChar();
                        if(!Character.isWhitespace(c)) return c;
                        increment();
                    } while(hasNext());
                } catch(final IOException e) {throw new UncheckedIOException(e);}
            }
            return null;
        }
        @Override
        public Character peekNextNonWS() throws UncheckedIOException {
            if(!oob(cursor + 2L)) {
                try {
                    data.seek(cursor + 2L);
                    for(long tmp = cursor;(tmp += 2L) < end;) {
                        final char c = data.readChar();
                        if(!Character.isWhitespace(c)) return c;
                    }
                } catch(final IOException e) {throw new UncheckedIOException(e);}
            }
            return null;
        }
        @Override
        public Character nextNonWS() throws UncheckedIOException {
            if(!oob(cursor)) {
                increment();
                if(hasNext()) {
                    try {
                        data.seek(cursor);
                        do {
                            final char c = data.readChar();
                            if(!Character.isWhitespace(c)) return c;
                            increment();
                        } while(hasNext());
                    } catch(final IOException e) {throw new UncheckedIOException(e);}
                }
            }
            return null;
        }
        
        // Don't use internalSS cuz iterator might continue to be used, causing issues with 
        // mutable start and end indices.
        @Override public FileSequence subSequence() {return new FileSequence(mark,cursor,data,mutable);}
    }
    /**Reverse File Sequence Iterator*/
    private class RFSI extends FileSequenceIterator {
        protected RFSI() throws UncheckedIOException {super(end - 2L);}
        
        @Override protected long offset(final long offset) {return cursor - offset * 2L;}
        @Override protected void increment() {cursor -= 2L;}
        
        @Override public boolean hasNext() {return cursor != start - 2L;}
        
        @Override
        public Character skipWS() throws UncheckedIOException {
            if(!oob(cursor)) {
                do {
                    final char c = seek(cursor);
                    if(!Character.isWhitespace(c)) return c;
                    increment();
                } while(hasNext());
            }
            return null;
        }
        @Override
        public Character peekNextNonWS() throws UncheckedIOException {
            if(!oob(cursor - 2L)) {
                for(long tmp = cursor;tmp != start;) {
                    final char c = seek(tmp -= 2L);
                    if(!Character.isWhitespace(c)) return c;
                }
            }
            return null;
        }
        @Override
        public Character nextNonWS() throws UncheckedIOException {
            if(!oob(cursor)) {
                increment();
                while(hasNext()) {
                    final char c = seek(cursor);
                    if(!Character.isWhitespace(c)) return c;
                    increment();
                }
            }
            return null;
        }
        
        // Don't use internalSS bc iterator might continue to be used, causing issues with 
        // mutable start and end indices.
        @Override public FileSequence subSequence() {return new FileSequence(cursor,mark,data,mutable);}
    }
    
    /**
     * @throws UncheckedIOException The part of the file being used in this sequence
     *                              was locked, perhaps by another iterator.
     * 
     * @implNote The {@linkplain FileSequenceIterator} class is
     *           {@linkplain AutoCloseable} to ensure that the file lock used to
     *           control access to the data is released. Therefore, this method
     *           should be called in a try-with-resourses block.
     * 
     * @see sequence_old.Sequence#iterator()
     */
    @Override public FileSequenceIterator iterator() throws UncheckedIOException {return new FFSI();}
    /**
     * @throws UncheckedIOException The part of the file being used in this sequence
     *                              was locked, perhaps by another iterator.
     * 
     * @implNote The {@linkplain FileSequenceIterator} class is
     *           {@linkplain AutoCloseable} to ensure that the file lock used to
     *           control access to the data is released. Therefore, this method
     *           should be called in a try-with-resourses block.
     * 
     * @see sequence_old.Sequence#iterator()
     */
    @Override public FileSequenceIterator reverseIterator() throws UncheckedIOException {return new RFSI();}
    
    @Override public void close() throws IOException {data.close();}
}


































