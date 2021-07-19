package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static sequence.CompoundSequence.builder;
import static test.TestUtils.charAt;
import static test.TestUtils.getItr1;
import static test.TestUtils.getItr2;
import static test.TestUtils.getItr3;
import static test.TestUtils.iterator;
import static test.TestUtils.itr1;
import static test.TestUtils.itr2;
import static test.TestUtils.itr3;
import static test.TestUtils.streq;
import static test.TestUtils.subSequence;

import java.util.Random;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import sequence.ArraySequence;
import sequence.CompoundSequence;
import sequence.CompoundSequence.CompoundSequenceBuilder;
import sequence.Sequence;

/**
 * Test cases for {@linkplain CompoundSequence}.
 * 
 * @author AzureTriple
 * 
 * @implNote The sub-sequences here are always {@linkplain ArraySequences}.
 *           Since the behavior between all sequences (including nested
 *           compounds) are required to be the same, these tests should
 *           generalize.
 */
class CompoundSequenceTest {
    static final Random r = new Random();
    static final String str = "0123456789ABCDEF";
    static Sequence[] split(final String str) {
        final Sequence[] out = new Sequence[3];
        final int limit = str.length() / 3;
        int start = 0;
        for(int i = 0;i < 2;++i) {
            final int end = start + 1 + r.nextInt(limit);
            out[i] = ArraySequence.builder().data(str.subSequence(start,end)).build();
            start = end;
        }
        out[2] = ArraySequence.builder().data(str.substring(start)).build();
        return out;
    }
    
    static CompoundSequenceBuilder csb() {return builder().data(split(str));}
    static interface appender {void append(CompoundSequenceBuilder asb);}
    static void buildeq(final String a,final boolean before,final appender...appenders) {
        final CompoundSequenceBuilder csb = builder();
        if(before) csb.data(split(str));
        for(final appender app : appenders) app.append(csb);
        if(!before) csb.data(split(str));
        final Sequence as = csb.build();
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
    static final appender DATA = a -> a.data(split(str)),
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
        streq(str,csb().build());
        
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
        
        assertThrows(IllegalArgumentException.class,() -> csb().start(str.length() + 1L).build());
        assertThrows(IllegalArgumentException.class,() -> csb().start(-str.length() - 1L).build());
        
        assertThrows(IllegalArgumentException.class,() -> csb().length(-1L).build());
        assertThrows(IllegalArgumentException.class,() -> csb().offset(-1L,2L).build());
        
        assertThrows(IllegalArgumentException.class,() -> csb().end(str.length() + 1L).build());
        assertThrows(IllegalArgumentException.class,() -> csb().start(-str.length() - 1L).build());
        assertThrows(IllegalArgumentException.class,() -> csb().start(2L).end(1L).build());
    }
    
    @Test
    void testLength() {
        assertEquals((long)str.length(),csb().build().size());
        assertEquals(length,csb().length(length).build().size());
    }
    
    @Test
    void testCharAt() {
        charAt(
            csb().build(),
            csb().range(start,end)
                 .build()
        );
    }
    
    @Test
    void testSubSequence() {
        subSequence(csb().build());
    }
    
    @Test
    void testIterator() {
        iterator(csb().build());
        
        itr1(builder().data(split(getItr1())).build());
        itr2(builder().data(split(getItr2())).build());
        itr3(builder().data(split(getItr3())).build());
    }
    
}



































