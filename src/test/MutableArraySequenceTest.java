package test;

import org.junit.jupiter.api.Test;
import sequence.MutableArraySequenceBuilder;
import sequence.SequenceBuilder;
import test.TestUtils.provider;

class MutableArraySequenceTest extends ArraySequenceTest {
    
    @Override provider p() {return () -> new MutableArraySequenceBuilder();}
    @Override MutableArraySequenceBuilder asb(SequenceBuilder sb) {return (MutableArraySequenceBuilder)sb;}
    
    @Test
    void testSet() {
        final String s = TestUtils.getSetChrString();
        final int start = TestUtils.getCharAtStart();
        final int end = TestUtils.getCharAtEnd();
        TestUtils.charAt(
            p(),
            b -> asb(b).data(s),
            b -> asb(b).start(start),
            b -> asb(b).end(end)
        );
    }
    
    @Test
    void testSetCharArray() {
        final String s = TestUtils.getSetArrString();
        TestUtils.setArr(p(),b -> asb(b).data(s));
    }
    
    @Test
    void testSetCharSequence() {
        final String s = TestUtils.getSetCSString();
        TestUtils.setCS(p(),b -> asb(b).data(s));
    }
    
    @Test
    void testMutableSubSequence() {
        final String s = TestUtils.getMutableSubSequenceString();
        TestUtils.mutableSubSequence(p(),b -> asb(b).data(s));
    }
    
    @Test
    void testMIterator() {
        final String s = TestUtils.getMItrString();
        TestUtils.mutableIterator(p(),b -> asb(b).data(s));
    }
    
}




































