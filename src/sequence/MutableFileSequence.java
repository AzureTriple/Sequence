package sequence;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import util.FileUtils;
import util.FixedSizeCharset;

/**
 * A {@linkplain FileSequence} which implements the
 * {@linkplain MutableSequence} interface.
 * 
 * @author AzureTriple
 */
class MutableFileSequence extends FileSequence implements MutableSequence {
    /**In order to work smoothly, mutable file sequences always use fixed-2.*/
    static final FixedSizeCharset MUTABLE_CS = FixedSizeCharset.Fixed_2;
    static final int M_SCALAR = MUTABLE_CS.size; 
    
    MutableFileSequence(final File file,
                        final long start,
                        final long end,
                        final long length,
                        final String suffix)
                        throws UncheckedIOException {
        super(file,start,end,length,Mutability.MUTABLE,suffix,MUTABLE_CS);
    }
    
    @Override
    public MutableSequence set(final int index,final char c)
                               throws IndexOutOfBoundsException,
                                      UncheckedIOException {
        return set((long)index,c);
    }
    @Override
    public MutableSequence set(final long index,final char c)
                               throws IndexOutOfBoundsException,
                                      UncheckedIOException {
        try {data.seek(idx(index)); data.writeChar(c);}
        catch(final IOException e) {throw ioe(e);}
        return this;
    }
    @Override
    public MutableSequence set(final int offset,final char[] data)
                               throws IndexOutOfBoundsException,
                                      UncheckedIOException {
        return set((long)offset,data);
    }
    @Override
    public MutableSequence set(long offset,final char[] data)
                               throws IndexOutOfBoundsException,
                                      UncheckedIOException {
        if((offset = idx(offset)) + data.length * M_SCALAR > length + start)
            throw new IndexOutOfBoundsException(
                "Input string of size %d is too large to set at index %d with sequence length %d."
                .formatted(data.length,(offset - start) / M_SCALAR,length / M_SCALAR)
            );
        try {
            this.data.seek(offset);
            for(final char c : data) this.data.writeChar(c);
        } catch(final IOException e) {throw ioe(e);}
        return this;
    }
    @Override
    public MutableSequence set(final int offset,final CharSequence data)
                               throws IndexOutOfBoundsException,
                                      UncheckedIOException {
        return set((long)offset,data);
    }
    private void putFS1(final FileSequence fs) throws IOException {
        // Direct transfer.
        fs.data.getChannel().transferTo(
            fs.start,
            fs.length,
            this.data.getChannel()
        );
    }
    private void putFS2(final FileSequence fs,final BufferedOutputStream O)
                        throws IOException,SecurityException {
        fs.data.seek(fs.start);
        final BufferedInputStream I = new BufferedInputStream(
            new FileInputStream(fs.data.getFD())
        );
        for(long i = 0L;i < fs.length;++i) {
            O.write(0);
            O.write(I.read());
        }
        O.flush();
    }
    private void putFS(final FileSequence fs) throws IOException,SecurityException {
        if(fs.big) putFS1(fs);
        else {
            putFS2(
                fs,
                new BufferedOutputStream(
                    new FileOutputStream(data.getFD())
                )
            );
        }
    }
    private void putAS(final ArraySequence as,final BufferedOutputStream O)
                       throws IOException {
        for(int c = as.start;c != as.end;++c) {
            O.write(as.data[c] >> 8);
            O.write(as.data[c]);
        }
        O.flush();
    }
    private void putAS(final ArraySequence as) throws IOException,SecurityException {
        putAS(
            as,
            new BufferedOutputStream(
                new FileOutputStream(data.getFD())
            )
        );
    }
    private void putUnknown(final Sequence s,final BufferedOutputStream O)
                            throws IOException {
        for(final char c : s) {O.write(c >> 8); O.write(c);}
        O.flush();
    }
    private void putUnknown(final Sequence s) throws IOException,SecurityException {
        putUnknown(
            s,
            new BufferedOutputStream(
                new FileOutputStream(data.getFD())
            )
        );
    }
    private void putCS(final Sequence[] cs,final BufferedOutputStream O)
                       throws IOException {
        for(final Sequence s : cs) {
            if(s instanceof FileSequence) {
                final FileSequence fs = (FileSequence)s;
                if(fs.big) putFS1(fs);
                else putFS2(fs,O);
            } else if(s instanceof ArraySequence)
                putAS((ArraySequence)s,O);
            else if(s instanceof CompoundSequence)
                putCS(((CompoundSequence)s).data,O);
            else putUnknown(s,O);
        }
    }
    private void putCS(final CompoundSequence cs) throws IOException,SecurityException {
        putCS(
            cs.data,
            new BufferedOutputStream(
                new FileOutputStream(data.getFD())
            )
        );
    }
    @Override
    public MutableSequence set(long offset,final CharSequence data)
                               throws IndexOutOfBoundsException,
                                      UncheckedIOException {
        offset = idx(offset);
        try {
            this.data.seek(offset);
            if(data instanceof Sequence) {
                final long s = ((Sequence)data).size() * M_SCALAR;
                if(offset + s > length + start)
                    throw new IndexOutOfBoundsException(
                        "Input string of size %d is too large to set at index %d with sequence length %d."
                        .formatted(s / M_SCALAR,(offset - start) / M_SCALAR,length / M_SCALAR)
                    );
                if(data instanceof FileSequence)
                    putFS((FileSequence)data);
                else if(data instanceof ArraySequence)
                    putAS((ArraySequence)data);
                else if(data instanceof CompoundSequence)
                    putCS((CompoundSequence)data);
                else
                    putUnknown((Sequence)data);
                this.data.getFD().sync();
            } else {
                final int l = data.length();
                if(offset + (long)l * M_SCALAR > length + start)
                    throw new IndexOutOfBoundsException(
                        "Input string of size %d is too large to set at index %d with sequence length %d."
                        .formatted(l,(offset - start) / M_SCALAR,length / M_SCALAR)
                    );
                for(int i = 0;i < l;++i) this.data.writeChar(data.charAt(i));
            }
        } catch(IOException|SecurityException e) {throw ioe(e);}
        return this;
    }
    
    @Override
    public MutableSequence subSequence(final int start,final int end) throws IndexOutOfBoundsException,
                                                                             UncheckedIOException {
        return subSequence((long)start,(long)end);
    }
    @Override
    public MutableSequence subSequence(long start,long end) throws IndexOutOfBoundsException,
                                                                   UncheckedIOException {
        if((end = ssidx(end)) < (start = ssidx(start)))
            throw new IndexOutOfBoundsException(
                "Range [%d,%d) is invalid."
                .formatted(end / M_SCALAR,start / M_SCALAR)
            );
        return start != end? start != this.start || end != this.end
                ? new MutableFileSequence(file,start,end,end - start,suffix)
                : shallowCopy()
                : EMPTY;
    }
    @Override
    public MutableSequence copySubSequence(final int start,final int end)
                                           throws IndexOutOfBoundsException,
                                                  UncheckedIOException {
        return copySubSequence((long)start,(long)end);
    }
    private final File cpy(final long start,final long length) throws UncheckedIOException {
        final File nf = tmpFile(Mutability.MUTABLE);
        // Mutable file sequence already in fixed-2 form, don't need to re-encode.
        try {FileUtils.transferDirect(file,start,nf,0L,length);}
        catch(IOException|SecurityException|IllegalArgumentException e) {
            try {nf.delete();}
            catch(final SecurityException e1) {}
            throw ioe(e);
        }
        return nf;
    }
    @Override
    public MutableSequence copySubSequence(long start,long end)
                                           throws IndexOutOfBoundsException,
                                                  UncheckedIOException {
        if((end = ssidx(end)) < (start = ssidx(start)))
            throw new IndexOutOfBoundsException(
                "Range [%d,%d) is invalid."
                .formatted(end / M_SCALAR,start / M_SCALAR)
            );
        if(start == end) return EMPTY;
        final long l = end - start;
        return new MutableFileSequence(cpy(start,l),0,l,l,suffix);
    }
    @Override
    public MutableSequence mutableSubSequence(final int start,final int end)
                                              throws IndexOutOfBoundsException,
                                                     UncheckedIOException {
        return mutableSubSequence((long)start,(long)end);
    }
    @Override
    public MutableSequence mutableSubSequence(long start,long end)
                                              throws IndexOutOfBoundsException,
                                                     UncheckedIOException {
        if((end = ssidx(end)) < (start = ssidx(start)))
            throw new IndexOutOfBoundsException(
                "Invalid range: [%d,%d)"
                .formatted(end / M_SCALAR,start / M_SCALAR)
            );
        if((length = (this.end = end) - (this.start = start)) == 0L) {
            close();
            return EMPTY;
        }
        return this;
    }
    
    /**Mutable File Sequence Iterator*/
    static abstract class MFSI extends MSI<FSI> {
        MFSI(final FSI sooper) {super(sooper);}
        
        @Override
        public MutableSequenceIterator set(final char c) throws IndexOutOfBoundsException,
                                                                UncheckedIOException {
            if(sooper.oob(sooper.cursor))
                throw new IndexOutOfBoundsException(
                    "Cannot set character at index %d."
                    .formatted(sooper.cursor / M_SCALAR)
                );
            try {sooper.data.seek(sooper.cursor); sooper.data.writeChar(c);}
            catch(final IOException e) {throw ioe(e);}
            return this;
        }
        @Override
        public MutableSequenceIterator set(final int offset,final char c)
                                           throws IndexOutOfBoundsException,
                                                  UncheckedIOException {
            return set((long)offset,c);
        }
        @Override
        public MutableSequenceIterator set(final long offset,final char c)
                                           throws IndexOutOfBoundsException,
                                                  UncheckedIOException {
            final long noff = sooper.offset(offset);
            if(sooper.oob(noff))
                throw new IndexOutOfBoundsException(
                    "Cannot jump to index %d (range: [%d,%d),input: %d)."
                    .formatted(
                        noff / M_SCALAR,
                        sooper.start / M_SCALAR,
                        sooper.end / M_SCALAR,
                        offset
                    )
                );
            try {sooper.data.seek(noff); sooper.data.writeChar(c);}
            catch(final IOException e) {throw ioe(e);}
            return this;
        }
        
        @Override
        public MutableSequence subSequence() throws IndexOutOfBoundsException,
                                                    UncheckedIOException {
            final long a = sooper.subBegin(),b = sooper.subEnd();
            if(b < a)
                throw new IndexOutOfBoundsException(
                    "Range [%d,%d) is invalid."
                    .formatted(a / M_SCALAR,b / M_SCALAR)
                );
            return a != b? a != sooper.start || b != sooper.end
                    ? new MutableFileSequence(sooper.file,a,b,b-a,sooper.suffix)
                    : (MutableSequence)sooper.parent.shallowCopy()
                    : EMPTY;
        }
    }
    /**Mutable Forward File Sequence Iterator*/
    static class MFFSI extends MFSI {
        MFFSI(final MutableFileSequence parent) throws UncheckedIOException {
            super(new FFSI(parent));
        }
    }
    /**Mutable Reverse File Sequence Iterator*/
    static class MRFSI extends MFSI {
        MRFSI(final MutableFileSequence parent) throws UncheckedIOException {
            super(new RFSI(parent));
        }
    }
    
    @Override
    public MutableSequenceIterator forwardIterator() throws UncheckedIOException {
        return isEmpty()? EMPTY.forwardIterator() : new MFFSI(this);
    }
    @Override
    public MutableSequenceIterator reverseIterator() throws UncheckedIOException {
        return isEmpty()? EMPTY.reverseIterator() : new MRFSI(this);
    }
    
    @Override
    public MutableSequence copyTo(final char[] arr,final int offset)
                                  throws IllegalArgumentException,
                                         UncheckedIOException {
        super.copyTo(arr,offset);
        return this;
    }
    @Override
    public MutableSequence mutableCopy() throws UncheckedIOException {
        return new MutableFileSequence(cpy(start,length),0,length,length,suffix);
    }
    @Override
    public Sequence immutableCopy() throws UncheckedIOException {
        final File nf = tmpFile(Mutability.IMMUTABLE);
        try {
            return new FileSequence(
                nf,
                start,end,length,
                Mutability.IMMUTABLE,
                suffix,
                FixedSizeCharset.transfer(file,nf,MUTABLE_CS)
            );
        } catch(UncheckedIOException|IOException|SecurityException e) {
            try {nf.delete();}
            catch(final SecurityException e1) {}
            throw ioe(e);
        }
    }
    @Override
    public MutableSequence shallowCopy() throws UncheckedIOException {
        return new MutableFileSequence(file,start,end,length,suffix);
    }
}