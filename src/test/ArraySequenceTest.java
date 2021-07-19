package test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static sequence.ArraySequence.builder;
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

import org.junit.jupiter.api.Test;
import sequence.ArraySequence.ArraySequenceBuilder;
import sequence.Sequence;

class ArraySequenceTest {
    static final String str = "0123456789ABCDEF";
    static final char[] arr = str.toCharArray();
    
    static ArraySequenceBuilder asb() {return builder().data(arr);}
    static interface appender {void append(ArraySequenceBuilder asb);}
    static void buildeq(final String a,final boolean before,final appender...appenders) {
        final ArraySequenceBuilder asb = builder();
        if(before) asb.data(arr);
        for(final appender app : appenders) app.append(asb);
        if(!before) asb.data(arr);
        final Sequence as = asb.build();
        streq(a,as);
    }
    static void buildeq(final String a,final appender...appenders) {
        buildeq(a,true,appenders);
        buildeq(a,false,appenders);
    }
    
    static final int start = 5,
                     end = 10,
                     length = 8,
                     fake_start = 6,
                     fake_end = 11,
                     fake_length = 9;
    static final appender DATA = a -> a.data(arr),
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
    void testBuilder() {
        streq(str,builder().data(arr).build());
        streq(str,builder().data(str).build());
        
        buildeq(str.substring(start),START);
        buildeq(str.substring(start),FAKE_START,START);
        buildeq(str.substring(0,end),END);
        buildeq(str.substring(0,end),FAKE_END,END);
        
        final String range = str.substring(start,end);
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
        
        buildeq(str.substring(0,length),LENGTH);
        final String offs = str.substring(start,start + length);
        buildeq(offs,START,LENGTH);
        buildeq(offs,OFFSET);
        buildeq(offs,FAKE_START,START,LENGTH);
        buildeq(offs,FAKE_END,OFFSET);
        buildeq(offs,FAKE_OFFSET,START,LENGTH);
        buildeq(offs,FAKE_OFFSET,LENGTH,START);
        buildeq(offs,FAKE_OFFSET,OFFSET);
        buildeq(offs,RANGE,OFFSET);
        buildeq(range,OFFSET,RANGE);
        
        assertThrows(IllegalArgumentException.class,() -> asb().start(arr.length + 1).build());
        assertThrows(IllegalArgumentException.class,() -> asb().start(-arr.length - 1).build());
        
        assertThrows(IllegalArgumentException.class,() -> asb().length(-1).build());
        assertThrows(IllegalArgumentException.class,() -> asb().offset(-1,2).build());
        
        assertThrows(IllegalArgumentException.class,() -> asb().end(arr.length + 1).build());
        assertThrows(IllegalArgumentException.class,() -> asb().start(-arr.length - 1).build());
        assertThrows(IllegalArgumentException.class,() -> asb().start(2).end(1).build());
    }
    
    @Test
    void testLength() {
        length(
            asb().build(),
            builder().data(str).build(),
            asb().length(length).build()
        );
    }
    
    @Test
    void testCharAt() {
        charAt(
            asb().build(),
            asb().range(start,end).build()
        );
    }
    
    @Test
    void testSubSequence() {
        subSequence(asb().build());
    }
    
    @Test
    void testIterator() {
        iterator(asb().build());
        
        itr1(builder().data(getItr1()).build());
        itr2(builder().data(getItr2()).build());
        itr3(builder().data(getItr3()).build());
    }
}












































