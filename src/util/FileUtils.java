package util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

/**
 * Utility functions for matipulating files.
 * 
 * @author AzureTriple
 */
public final class FileUtils {
    private FileUtils() {}
    
    static BufferedReader br(final Reader r) {return new BufferedReader(r);}
    static BufferedWriter bw(final Writer w) {return new BufferedWriter(w);}
    static BufferedReader bisr(final FileInputStream f,final CharsetDecoder d) {
        return br(new InputStreamReader(f,d));
    }
    static BufferedReader bisr(final File f,final CharsetDecoder d) throws FileNotFoundException {
        return bisr(new FileInputStream(f),d);
    }
    static BufferedWriter bosw(final FileOutputStream f,final CharsetEncoder e) {
        return bw(new OutputStreamWriter(f,e));
    }
    static BufferedWriter bosw(final File f,final CharsetEncoder e) throws FileNotFoundException {
        return bosw(new FileOutputStream(f),e);
    }
    static BufferedInputStream bis(final InputStream i) {
        return new BufferedInputStream(i);
    }
    static BufferedInputStream bis(final File f) throws FileNotFoundException {
        return bis(new FileInputStream(f));
    }
    static BufferedOutputStream bos(final OutputStream o) {
        return new BufferedOutputStream(o);
    }
    static BufferedOutputStream bos(final File f) throws FileNotFoundException {
        return bos(new FileOutputStream(f));
    }
    static void writeAndTruncateCoded(final File f,final CharsetDecoder d,
                                             final File t,final CharsetEncoder e)
                                             throws IOException {
        try(FileInputStream fis = new FileInputStream(t);
            BufferedReader I = bisr(fis,d);
            BufferedWriter O = bosw(t,e)) {
            fis.getChannel().truncate(I.transferTo(O));
        }
    }
    static void writeAndTruncate(final File f,final File t) throws IOException {
        try(FileOutputStream fos = new FileOutputStream(t);
            BufferedInputStream I = bis(f);
            BufferedOutputStream O = bos(fos)) {
            fos.getChannel().truncate(I.transferTo(O));
        }
    }
    /**
     * Transers the contents of one file to another.
     * 
     * @param f Input file.
     * @param d Decoder for input.
     * @param t Output file.
     * @param e Encoder for output.
     * 
     * @throws IOException
     */
    public static void transferCoded(final File f,final CharsetDecoder d,
                                     final File t,final CharsetEncoder e)
                                     throws IOException {
        try(BufferedReader I = bisr(f,d);
            BufferedWriter O = bosw(t,e)) {
            I.transferTo(O);
        }
    }
    /**
     * Transers the contents of one file to another.
     * 
     * @param f Input file.
     * @param t Output file.
     * 
     * @throws IOException
     */
    public static void transferDirect(final File f,final File t) throws IOException {
        try(BufferedInputStream I = bis(f);
            BufferedOutputStream O = bos(t)) {
            I.transferTo(O);
        }
    }
    static final int BUFFER_SCALAR = 13;
    static final int BUFFER_SIZE = 1 << BUFFER_SCALAR;
    /**
     * Transfers bytes between files.
     * 
     * @param src      Source file.
     * @param dst      Destination file.
     * @param srcStart Index of the first byte in the source file, inclusive.
     * @param dstStart Index of the first byte in the destination file, inclusive.
     * @param length   Maximum number of bytes to transfer.
     * 
     * @throws IllegalArgumentException Any of the numeric inputs were negative.
     * @throws IOException
     * @throws SecurityException
     */
    public static void transferDirect(final File src,final long srcStart,
                                      final File dst,final long dstStart,
                                      final long length)
                                      throws IOException,SecurityException,
                                             IllegalArgumentException {
        if(length == 0L) return;
        if(length < 0L)
            throw new IllegalArgumentException(
                "Negative length: %d"
                .formatted(length)
            );
        if(srcStart < 0L)
            throw new IllegalArgumentException(
                "Negative source start index: %d"
                .formatted(srcStart)
            );
        if(dstStart < 0L)
            throw new IllegalArgumentException(
                "Negative destination start index: %d"
                .formatted(dstStart)
            );
        try(FileInputStream I = new FileInputStream(src);
            FileOutputStream O = new FileOutputStream(dst)) {
            I.getChannel().position(srcStart);
            O.getChannel().position(dstStart);
            final byte[] buf = new byte[BUFFER_SIZE];
            if(length > BUFFER_SIZE) {
                long i = length >>> BUFFER_SCALAR;
                do {
                    final int read = I.read(buf,0,BUFFER_SIZE);
                    if(read <= 0) return;
                    O.write(buf,0,read);
                } while(--i != 0L);
            }
            final int read = I.read(buf,0,(int)(length & (BUFFER_SIZE - 1)));
            if(read > 0) O.write(buf,0,read);
        }
    }
}