package util;

import static util.FileUtils.bisr;
import static util.FileUtils.transferCoded;
import static util.FileUtils.transferDirect;
import static util.FileUtils.writeAndTruncate;
import static util.FileUtils.writeAndTruncateCoded;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.file.Files;

/**
 * A custom {@linkplain Charset} which defines a character to be a fixed number
 * of bytes in size.
 * 
 * @author AzureTriple
 */
public final class FixedSizeCharset extends Charset {
    public static final FixedSizeCharset Fixed_1 = new FixedSizeCharset(1),
                                         Fixed_2 = new FixedSizeCharset(2),
                                         Fixed_3 = new FixedSizeCharset(3);
    
    /**Number of bytes per char.*/
    public final int size;
    
    /**
     * Creates a fixed size charset.
     * 
     * @param size <code>true</code> to specify 2 bytes per character, or
     *             <code>false</code> to specify 1 byte per character.
     */
    private FixedSizeCharset(final int size) {
        super("Fixed-"+size,null);
        this.size = size;
    }
    
    /**
     * @return A fixed size charset with the specified number of bytes per
     *         character.
     * 
     * @throws IllegalArgumentException The size is less than one.
     */
    public static FixedSizeCharset withSize(final int size) throws IllegalArgumentException {
        if(size < 1) throw new IllegalArgumentException("Invalid size: "+size);
        return switch(size) {
            case 1 -> Fixed_1;
            case 2 -> Fixed_2;
            case 3 -> Fixed_3;
            default-> new FixedSizeCharset(size);
        };
    }
    
    @Override
    public boolean contains(final Charset cs) {
        return cs instanceof FixedSizeCharset && 
               size >= ((FixedSizeCharset)cs).size;
    }
    
    private static abstract class D extends CharsetDecoder {
        private D(final FixedSizeCharset cs,final float s) {super(cs,s,1f);}
        
        abstract int scaleIr(int i);
        abstract int scaleOr(int o);
        abstract int scaleIl(int l);
        abstract int scaleOl(int l);
        abstract void arrLoop(byte[] ia,char[] oa,int[] indices);
        final void doArrLoop(final byte[] ia,final char[] oa,
                             final int i,final int o,int l) {
            final int[] indices = {i,o};
            while(l > 0) {arrLoop(ia,oa,indices); --l;}
        }
        abstract void bufLoop(ByteBuffer I,CharBuffer O);
        final void doBufLoop(final ByteBuffer I,final CharBuffer O,int l) {
            while(l > 0) {bufLoop(I,O); --l;}
        }
        
        @Override
        protected final CoderResult decodeLoop(final ByteBuffer I,final CharBuffer O) {
            final int l = Math.min(scaleIr(I.remaining()),scaleOr(O.remaining()));
            if(I.hasArray() && O.hasArray()) {
                final int ip = I.position(),
                          op = O.position();
                doArrLoop(
                    I.array(),
                    O.array(),
                    I.arrayOffset() + ip,
                    O.arrayOffset() + op,
                    l
                );
                I.position(ip + scaleIl(l));
                O.position(op + scaleOl(l));
            } else doBufLoop(I,O,l);
            return I.hasRemaining() && !O.hasRemaining()? CoderResult.OVERFLOW
                                                        : CoderResult.UNDERFLOW;
        }
    }
    private static final class D1 extends D {
        private D1(final FixedSizeCharset cs) {super(cs,1f);}
        
        @Override int scaleIr(final int i) {return i;}
        @Override int scaleOr(final int o) {return o;}
        @Override int scaleIl(final int l) {return l;}
        @Override int scaleOl(final int l) {return l;}
        @Override
        void arrLoop(final byte[] ia,final char[] oa,
                     final int [] indices) {
            oa[indices[1]++] = (char)ia[indices[0]++];
        }
        @Override
        void bufLoop(final ByteBuffer I,final CharBuffer O) {
            O.put((char)I.get());
        }
    }
    private static final class D2 extends D {
        private D2(final FixedSizeCharset cs) {super(cs,1f/2f);}
        
        @Override int scaleIr(final int i) {return i / 2;}
        @Override int scaleOr(final int o) {return o;}
        @Override int scaleIl(final int l) {return l * 2;}
        @Override int scaleOl(final int l) {return l;}
        @Override
        void arrLoop(final byte[] ia,final char[] oa,
                     final int [] indices) {
            oa[indices[1]++] = (char)(
                ia[indices[0]++] << 8 |
                ia[indices[0]++]
            );
        }
        @Override
        void bufLoop(final ByteBuffer I,final CharBuffer O) {
            O.put((char)(
                I.get() << 8 |
                I.get()
            ));
        }
    }
    private static final class D3 extends D {
        // ceil([#bytes/encoded char]/[#bytes/Java char]) / [#bytes/encoded char]
        // => ceil(3/2)/3 => 2/3
        private D3(final FixedSizeCharset cs) {super(cs,2f/3f);}
        
        @Override int scaleIr(final int i) {return i / 3;}
        @Override int scaleOr(final int o) {return o / 2;}
        @Override int scaleIl(final int l) {return l * 3;}
        @Override int scaleOl(final int l) {return l * 2;}
        @Override
        void arrLoop(final byte[] ia,final char[] oa,
                     final int [] indices) {
            oa[indices[1]++] = (char)(
                ia[indices[0]++] << 8 |
                ia[indices[0]++]
            );
            oa[indices[1]++] = (char)
                ia[indices[0]++];
        }
        @Override
        void bufLoop(final ByteBuffer I,final CharBuffer O) {
            O.put((char)(
                I.get() << 8 |
                I.get()
            )).put((char)
                I.get()
            );
        }
    }
    private static final class DN extends D {
        private final int N,N2,NM;
        private final boolean mod;
        // ceil([#bytes/encoded char]/[#bytes/Java char]) / [#bytes/encoded char]
        // => ceil(N/2)/N
        private DN(final FixedSizeCharset cs,final int N) {
            super(cs,(float)((N + 1) / 2) / (float)N);
            NM = (N2 = (this.N = N) / 2) + (N & 1);
            mod = (N & 1) != 0;
        }
        
        @Override int scaleIr(final int i) {return i / N;}
        @Override int scaleOr(final int o) {return o / NM;}
        @Override int scaleIl(final int l) {return l * N;}
        @Override int scaleOl(final int l) {return l * NM;}
        @Override
        void arrLoop(final byte[] ia,final char[] oa,
                     final int [] indices) {
            for(int x = 0;x < N2;++x)
                oa[indices[1]++] = (char)(
                    ia[indices[0]++] << 8 |
                    ia[indices[0]++]
                );
            if(mod)
                oa[indices[1]++] = (char)
                    ia[indices[0]++];
        }
        @Override
        void bufLoop(final ByteBuffer I,final CharBuffer O) {
            for(int x = 0;x < N2;++x)
                O.put((char)(
                    I.get() << 8 |
                    I.get()
                ));
            if(mod)
                O.put((char)
                    I.get()
                );
        }
    }
    @Override
    public CharsetDecoder newDecoder() {
        return switch(size) {
            case 1 -> new D1(this);
            case 2 -> new D2(this);
            case 3 -> new D3(this);
            default-> new DN(this,size);
        };
    }
    
    private static abstract class E extends CharsetEncoder {
        protected static byte[] R(final int n) {
            final byte[] out = new byte[n];
            out[n - 1] = (byte)'?';
            return out;
        }
        private E(final FixedSizeCharset cs,final float s,final byte[] r) {super(cs,s,s,r);}
        // This is manually set, so no computation is necessary.
        @Override public final boolean isLegalReplacement(byte[] repl) {return true;}
        
        abstract int scaleIr(int i);
        abstract int scaleOr(int o);
        abstract int scaleIl(int l);
        abstract int scaleOl(int l);
        
        abstract void arrLoop(char[] ia,byte[] oa,int[] indices);
        void doArrLoop(final char[] ia,final byte[] oa,
                       final int i,final int o,int l) {
            final int[] indices = {i,o};
            while(l > 0) {arrLoop(ia,oa,indices); --l;}
        }
        
        abstract void bufLoop(CharBuffer I,ByteBuffer O);
        void doBufLoop(final CharBuffer I,final ByteBuffer O,int l) {
            while(l > 0) {bufLoop(I,O); --l;}
        }
        
        @Override
        protected final CoderResult encodeLoop(final CharBuffer I,final ByteBuffer O) {
            final int l = Math.min(scaleIr(I.remaining()),scaleOr(O.remaining()));
            if(I.hasArray() && O.hasArray()) {
                final int ip = I.position(),
                          op = O.position();
                doArrLoop(
                    I.array(),
                    O.array(),
                    I.arrayOffset() + ip,
                    O.arrayOffset() + op,
                    l
                );
                I.position(ip + scaleIl(l));
                O.position(op + scaleOl(l));
            } else doBufLoop(I,O,l);
            return I.hasRemaining()? CoderResult.OVERFLOW
                                   : CoderResult.UNDERFLOW;
        }
    }
    private static final class E1 extends E {
        private static final byte[] R1 = {'?'};
        private E1(final FixedSizeCharset cs) {super(cs,1f,R1);}
        
        @Override int scaleIr(final int i) {return i;}
        @Override int scaleOr(final int o) {return o;}
        @Override int scaleIl(final int l) {return l;}
        @Override int scaleOl(final int l) {return l;}
        @Override
        void arrLoop(final char[] ia,final byte[] oa,
                     final int [] indices) {
            oa[indices[1]++] = (byte)ia[indices[0]++];
        }
        @Override
        void bufLoop(final CharBuffer I,final ByteBuffer O) {
            O.put((byte)I.get());
        }
    }
    private static final class E2 extends E {
        private static final byte[] R2 = {0,'?'};
        private E2(final FixedSizeCharset cs) {super(cs,2f,R2);}
        
        @Override int scaleIr(final int i) {return i;}
        @Override int scaleOr(final int o) {return o / 2;}
        @Override int scaleIl(final int l) {return l;}
        @Override int scaleOl(final int l) {return l * 2;}
        @Override
        void arrLoop(final char[] ia,final byte[] oa,
                     final int [] indices) {
            oa[indices[1]++] = (byte)(ia[indices[0]] >>> 8);
            oa[indices[1]++] = (byte)ia[indices[0]++];
        }
        @Override
        void bufLoop(final CharBuffer I,final ByteBuffer O) {
            final char x = I.get();
            O.put((byte)(x >>> 8))
             .put((byte)x);
        }
    }
    private static final class E3 extends E {
        private static final byte[] R3 = {0,0,'?'};
        // [#bytes/encoded char] / ceil([#bytes/Java char]/[#bytes/encoded char])
        // => 3 / ceil(3/2) => 3/2
        private E3(final FixedSizeCharset cs) {super(cs,3f/2f,R3);}
        
        @Override int scaleIr(final int i) {return i / 2;}
        @Override int scaleOr(final int o) {return o / 3;}
        @Override int scaleIl(final int l) {return l * 2;}
        @Override int scaleOl(final int l) {return l * 3;}
        @Override
        void arrLoop(final char[] ia,final byte[] oa,
                     final int [] indices) {
            oa[indices[1]++] = (byte)(ia[indices[0]] >>> 8);
            oa[indices[1]++] = (byte)ia[indices[0]++];
            oa[indices[1]++] = (byte)ia[indices[0]++];
        }
        @Override
        void bufLoop(final CharBuffer I,final ByteBuffer O) {
            {
                final char x = I.get();
                O.put((byte)(x >>> 8))
                 .put((byte)x);
            }
            O.put((byte)I.get());
        }
    }
    private static final class EN extends E {
        private final int N,N2,NM;
        private final boolean mod;
        // [#bytes/encoded char] / ceil([#bytes/Java char]/[#bytes/encoded char])
        // => N / ceil(N/2)
        private EN(final FixedSizeCharset cs,final int N) {
            super(cs,(float)N / ((N + 1) / 2),R(N));
            NM = (N2 = (this.N = N) / 2) + (N & 1);
            mod = (N & 1) != 0;
        }
        
        @Override int scaleIr(final int i) {return i / NM;}
        @Override int scaleOr(final int o) {return o / N;}
        @Override int scaleIl(final int l) {return l * NM;}
        @Override int scaleOl(final int l) {return l * N;}
        @Override
        void arrLoop(final char[] ia,final byte[] oa,
                     final int [] indices) {
            for(int x = 0;x < N2;++x) {
                oa[indices[1]++] = (byte)(ia[indices[0]] >>> 8);
                oa[indices[1]++] = (byte)ia[indices[0]++];
            }
            if(mod)
                oa[indices[1]++] = (byte)ia[indices[0]++];
        }
        @Override
        void bufLoop(final CharBuffer I,final ByteBuffer O) {
            for(int x = 0;x < N2;++x) {
                final char i = I.get();
                O.put((byte)(i >>> 8))
                 .put((byte)i);
            }
            if(mod)
                O.put((byte)I.get());
        }
    }
    @Override
    public CharsetEncoder newEncoder() {
        return switch(size) {
            case 1 -> new E1(this);
            case 2 -> new E2(this);
            case 3 -> new E3(this);
            default-> new EN(this,size);
        };
    }
    
    private static void resize(final File f,
                               final D d,final int ds,
                               final E e,final int es)
                               throws IOException,SecurityException {
        final File tmp = Files.createTempFile(
            f.toPath().getParent(),
            "resize-temp",
            null
        ).toFile();
        tmp.deleteOnExit();
        // Copy data to the temp file using the smaller format, then copy back using the
        // desired format.
        try {
            if(ds > es) {
                writeAndTruncateCoded(f,d,tmp,e);
                transferDirect(tmp,f);
            } else {
                transferDirect(f,tmp);
                transferCoded(tmp,d,f,e);
            }
        } finally {tmp.delete();}
    }
    /**
     * Converts the bytes/char count of the file. This assumes that the file is
     * encoded using a {@linkplain FixedSizeCharset}.
     * 
     * @throws IllegalArgumentException <code>from</code> and/or <code>to</code> is
     *                                  less than one.
     * @throws NullPointerException     The file is null.
     */
    public static void resize(final File f,final int from,final int to) throws IOException,
                                                                               IllegalArgumentException,
                                                                               NullPointerException,
                                                                               SecurityException {
        if(f == null) throw new NullPointerException("File is null.");
        if(from < 1) throw new IllegalArgumentException("Invalid size for input 'from': " + from);
        if(to < 1) throw new IllegalArgumentException("Invalid size for input 'to': " + to);
        if(from == to) return;
        resize(
            f,
            switch(from) {
                case 1 -> new D1(Fixed_1);
                case 2 -> new D2(Fixed_2);
                case 3 -> new D3(Fixed_3);
                default-> (D)new FixedSizeCharset(from).newDecoder();
            },from,
            switch(to) {
                case 1 -> new E1(Fixed_1);
                case 2 -> new E2(Fixed_2);
                case 3 -> new E3(Fixed_3);
                default-> (E)new FixedSizeCharset(to).newEncoder();
            },to
        );
    }
    /**
     * Transfers the data from the source to the destination using the specified
     * {@linkplain FixedSizeCharset}s.
     */
    public static void transfer(final File src,final FixedSizeCharset from,
                                final File dst,final FixedSizeCharset to)
                                throws IOException,NullPointerException,
                                       SecurityException {
        if(src == null) throw new NullPointerException("Null source file.");
        if(dst == null) throw new NullPointerException("Null destination file.");
        if(from == null) throw new NullPointerException("Null source charset.");
        if(to == null) throw new NullPointerException("Null destination charset.");
        final D d = (D)from.newDecoder();
        final E e = (E)to.newEncoder();
        if(src.equals(dst)) {
            if(from.size == to.size) return;
            resize(src,d,from.size,e,to.size);
        } else if(from.size == to.size) transferDirect(src,dst);
        else transferCoded(src,d,dst,e);
    }
    // Same as E2, but detects whether all the characters it encodes can instead be
    // encoded with E1 to save space.
    private static final class Eauto extends E {
        private boolean big = false;
        private Eauto(final FixedSizeCharset cs) {super(cs,2f,E2.R2);}
        
        @Override int scaleIr(final int i) {return i;}
        @Override int scaleOr(final int o) {return o / 2;}
        @Override int scaleIl(final int l) {return l;}
        @Override int scaleOl(final int l) {return l * 2;}
        @Override void arrLoop(char[] ia,byte[] oa,int[] indices) {}
        @Override void bufLoop(CharBuffer I,ByteBuffer O) {}
        @Override
        void doArrLoop(final char[] ia,final byte[] oa,
                       int i,int o,int l) {
            while(!big && l > 0) {
                big = ia[i] > 0xFF;
                oa[o++] = (byte)(ia[i] >>> 8);
                oa[o++] = (byte)ia[i++];
                --l;
            }
            while(l > 0) {
                oa[o++] = (byte)(ia[i] >>> 8);
                oa[o++] = (byte)ia[i++];
                --l;
            }
        }
        @Override
        void doBufLoop(final CharBuffer I,final ByteBuffer O,int l) {
            while(!big && l > 0) {
                final char i = I.get();
                big = i > 0xFF;
                O.put((byte)(i >>> 8))
                 .put((byte)i);
                --l;
            }
            while(l > 0) {
                final char i = I.get();
                O.put((byte)(i >>> 8))
                 .put((byte)i);
                --l;
            }
        }
    }
    
    /**
     * Copies characters encoded by the specified charset from the source file and
     * writes them using the specified {@linkplain FixedSizeCharset}.
     */
    public static void transfer(final File src,final Charset scs,
                                final File dst,final FixedSizeCharset dcs)
                                throws IOException {
        transferCoded(src,scs.newDecoder(),dst,dcs.newEncoder());
    }
    /**
     * Submits a data transfer task to the executor. If an exception is raised
     * during execution, the executor will be shut down immediately and the returned
     * future object will raise an exception when the get method is called.
     */
    private static Future<Void> transferTask(final File I,final CharsetDecoder D,
                                             final File O,final CharsetEncoder E,
                                             final ExecutorService exec) {
        return exec.submit(() -> {
            try {transferCoded(I,D,O,E); return null;}
            catch(final Exception e) {exec.shutdownNow(); throw e;}
        });
    }
    /**
     * Copies characters encoded by the specified charset from the source file and
     * writes them using the smallest {@linkplain FixedSizeCharset} possible.
     * 
     * @return The {@linkplain FixedSizeCharset} used to encode the data.
     */
    public static FixedSizeCharset transfer(final File src,final File dst,final Charset cs)
                                            throws IOException,SecurityException {
        
        // If the charset encoder is 1 byte per char, do a simple copy.
        if(cs.canEncode() && cs.newEncoder().maxBytesPerChar() == 1f)
            transferCoded(src,cs.newDecoder(),dst,Fixed_1.newEncoder());
        else {
            // Write to the file using two bytes/character while also keeping track of the
            // minimum size required to store all the characters. Also write a temp 1-byte
            // file in case that the bytes/char is small.
            final File tmp = Files.createTempFile(
                dst.toPath().getParent(),
                "decode-temp",
                null
            ).toFile();
            tmp.deleteOnExit();
            try {
                {
                    final E f1 = new E1   (Fixed_1),
                            f2 = new Eauto(Fixed_2);
                    try {
                        final ExecutorService exec = Executors.newFixedThreadPool(2);
                        final Future<Void> r1 = transferTask(src,cs.newDecoder(),tmp,f1,exec),
                                           r2 = transferTask(src,cs.newDecoder(),dst,f2,exec);
                        exec.shutdown();
                        exec.awaitTermination(Long.MAX_VALUE,TimeUnit.DAYS);
                        r1.get(); r2.get();
                    } catch(final Exception e) {throw new IOException(e);}
                    // Check to see if the encoder received a 2-byte character.
                    if(((Eauto)f2).big) return Fixed_2;
                }
                // Move the condensed data back to the original file.
                writeAndTruncate(tmp,dst);
            } finally {tmp.delete();}
        }
        return Fixed_1;
    }
    
    /**
     * Reads a file encoded by this charset and returns a string representing the
     * contents with the specified maximum size (in characters).
     * 
     * @throws NullPointerException     The file is <code>null</code>.
     * @throws IllegalArgumentException The size is negative.
     * @throws OutOfMemoryError         The specified size was too large.
     */
    public String stringDecode(final int size,final File f) throws NullPointerException,
                                                                   IllegalArgumentException,
                                                                   IOException,OutOfMemoryError {
        if(size < 0) throw new IllegalArgumentException("Negative size: " + size);
        if(size == 0) return "";
        final char[] chr = new char[size];
        final int nchr;
        try(BufferedReader I = bisr(f,(D)newDecoder())) {nchr = I.read(chr);}
        return String.valueOf(chr,0,nchr);
    }
    /**
     * Reads a file encoded by this charset and returns a string representing the
     * contents with the specified maximum size (in characters).
     * 
     * @throws NullPointerException     The file is <code>null</code>.
     * @throws IllegalArgumentException The size is negative.
     * @throws OutOfMemoryError         The specified size was too large.
     */
    public String stringDecode(final int size,final RandomAccessFile f) throws NullPointerException,
                                                                               IllegalArgumentException,
                                                                               IOException,OutOfMemoryError {
        if(size < 0) throw new IllegalArgumentException("Negative size: " + size);
        if(size == 0) return "";
        final byte[] byt = new byte[size * this.size];
        final int l = f.read(byt);
        if(l == -1) return "";
        return new String(byt,0,l,this);
    }
    /**
     * Reads a file encoded by this charset and returns a string representing the
     * contents with the specified maximum size (in characters).
     * 
     * @throws NullPointerException     The file is <code>null</code>.
     * @throws IllegalArgumentException The size or offset is negative.
     * @throws OutOfMemoryError         The specified size was too large.
     */
    public String stringDecode(final int size,final RandomAccessFile f,final long offset)
                               throws NullPointerException,IllegalArgumentException,
                                      IOException,OutOfMemoryError {
        if(size < 0) throw new IllegalArgumentException("Negative size: " + size);
        if(offset < 0) throw new IllegalArgumentException("Negative offset: "+offset);
        if(size == 0) return "";
        final byte[] byt = new byte[size * this.size];
        f.seek(offset);
        final int l = f.read(byt);
        if(l == -1) return "";
        return new String(byt,0,l,this);
    }
}