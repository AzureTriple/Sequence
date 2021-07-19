package test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static sequence.FileSequence.builder;
import static test.TestUtils.charAt;
import static test.TestUtils.getItr1;
import static test.TestUtils.getItr2;
import static test.TestUtils.getItr3;
import static test.TestUtils.iterator;
import static test.TestUtils.itr1;
import static test.TestUtils.itr2;
import static test.TestUtils.itr3;
import static test.TestUtils.length;
import static test.TestUtils.streq;
import static test.TestUtils.subSequence;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import sequence.FileSequence.FileSequenceBuilder;
import sequence.Sequence;

class FileSequenceTest {
    static File write(String s) {
        try {
            final File out = Files.createTempFile(null,null).toFile();
            out.deleteOnExit();
            try(BufferedWriter w = new BufferedWriter(new FileWriter(out,StandardCharsets.UTF_8))) {
                w.write(s);
            }
            return out;
        } catch(final IOException e) {e.printStackTrace(); System.exit(1);}
        return null;
    }
    static final String str = "0123456789ABCDEF";
    static final File file = write(str);
    
    static FileSequenceBuilder fsb() {return builder().data(file);}
    static interface appender {void append(FileSequenceBuilder asb);}
    static void buildeq(final String a,final boolean before,final appender...appenders)
                        throws FileNotFoundException,SecurityException,IOException {
        final FileSequenceBuilder fsb = builder();
        if(before) fsb.data(file);
        for(final appender app : appenders) app.append(fsb);
        if(!before) fsb.data(file);
        final Sequence as = fsb.build();
        streq(a,as);
    }
    static void buildeq(final String a,final appender...appenders) throws FileNotFoundException,SecurityException,IOException {
        buildeq(a,true,appenders);
        buildeq(a,false,appenders);
    }
    
    static final long start = 5,
                      end = 10,
                      length = 8,
                      fake_start = 6,
                      fake_end = 11,
                      fake_length = 9;
    static final appender DATA = a -> a.data(file),
                          START = a -> a.start(start),
                          END = a -> a.end(end),
                          LENGTH = a -> a.length(length),
                          FAKE_START = a -> a.start(fake_start),
                          FAKE_END = a -> a.end(fake_end),
                          FAKE_LENGTH = a -> a.length(fake_length),
                          RANGE = a -> a.range(start,end),
                          FAKE_RANGE = a -> a.range(fake_start,fake_end),
                          OFFSET = a -> a.offset(start,length),
                          FAKE_OFFSET = a -> a.offset(fake_start,fake_length);
    @Test
    void testBuilder() throws FileNotFoundException,SecurityException,IOException {
        streq(str,builder().data(file).build());
        streq(str,builder().data(file.toPath()).build());
        streq(str,builder().data(file.toString()).build());
        
        buildeq(str.substring((int)start),START);
        buildeq(str.substring((int)start),FAKE_START,START);
        buildeq(str.substring(0,(int)end),END);
        buildeq(str.substring(0,(int)end),FAKE_END,END);
        
        final String range = str.substring((int)start,(int)end);
        buildeq(range,START,END);
        buildeq(range,END,START);
        buildeq(range,RANGE);
        buildeq(range,FAKE_START,START,END);
        buildeq(range,FAKE_END,START,END);
        buildeq(range,START,FAKE_END,END);
        buildeq(range,FAKE_END,END,START);
        buildeq(range,END,FAKE_START,START);
        buildeq(range,FAKE_START,RANGE);
        buildeq(range,FAKE_END,RANGE);
        buildeq(range,FAKE_START,FAKE_END,RANGE);
        buildeq(range,FAKE_RANGE,RANGE);
        buildeq(range,FAKE_RANGE,START,END);
        
        buildeq(str.substring(0,(int)length),LENGTH);
        final String offs = str.substring((int)start,(int)(start + length));
        buildeq(offs,START,LENGTH);
        buildeq(offs,OFFSET);
        buildeq(offs,FAKE_START,START,LENGTH);
        buildeq(offs,FAKE_END,OFFSET);
        buildeq(offs,FAKE_OFFSET,START,LENGTH);
        buildeq(offs,FAKE_OFFSET,LENGTH,START);
        buildeq(offs,FAKE_OFFSET,OFFSET);
        buildeq(offs,RANGE,OFFSET);
        buildeq(range,OFFSET,RANGE);
        
        assertThrows(IllegalArgumentException.class,() -> fsb().start(str.length() + 1L).build());
        assertThrows(IllegalArgumentException.class,() -> fsb().start(-str.length() - 1L).build());
        
        assertThrows(IllegalArgumentException.class,() -> fsb().length(-1L).build());
        assertThrows(IllegalArgumentException.class,() -> fsb().offset(-1L,2L).build());
        
        assertThrows(IllegalArgumentException.class,() -> fsb().end(str.length() + 1L).build());
        assertThrows(IllegalArgumentException.class,() -> fsb().start(-str.length() - 1L).build());
        assertThrows(IllegalArgumentException.class,() -> fsb().start(2L).end(1L).build());
    }
    
    @Test
    void testLength() throws FileNotFoundException,SecurityException,IOException {
        length(
            fsb().build(),
            builder().data(file.toPath()).build(),
            fsb().length(length).build()
        );
    }
    
    @Test
    void testCharAt() throws FileNotFoundException,SecurityException,IOException {
        charAt(
            fsb().build(),
            fsb().range(start,end).build()
        );
    }
    
    @Test
    void testSubSequence() throws FileNotFoundException,SecurityException,IOException {
        subSequence(fsb().build());
    }
    
    @Test
    void testIterator() throws FileNotFoundException,SecurityException,IOException {
        iterator(fsb().build());
        
        itr1(builder().data(write(getItr1())).build());
        itr2(builder().data(write(getItr2())).build());
        itr3(builder().data(write(getItr3())).build());
    }
    
}






























