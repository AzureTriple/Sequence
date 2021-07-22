package test;

import org.junit.jupiter.api.Test;
import sequence.MutableFileSequenceBuilder;
import sequence.SequenceBuilder;
import test.TestUtils.provider;

class MutableFileSequenceTest extends FileSequenceTest {
    @Override provider p() {return () -> new MutableFileSequenceBuilder();}
    @Override MutableFileSequenceBuilder fsb(SequenceBuilder sb) {return (MutableFileSequenceBuilder)sb;}
    
    @Test
    void testSet() {
        final String s = TestUtils.getSetChrString();
        final long start = TestUtils.getCharAtStart();
        final long end = TestUtils.getCharAtEnd();
        TestUtils.charAt(
            p(),
            b -> fsb(b).data(write(s)),
            b -> fsb(b).start(start),
            b -> fsb(b).end(end)
        );
    }
    
    @Test
    void testSetCharArray() {
        final String s = TestUtils.getSetArrString();
        TestUtils.setArr(p(),b -> fsb(b).data(write(s)));
    }
    
    @Test
    void testSetCharSequence() {
        final String s = TestUtils.getSetCSString();
        TestUtils.setCS(p(),b -> fsb(b).data(write(s)));
    }
    
    @Test
    void testMutableSubSequence() {
        final String s = TestUtils.getMutableSubSequenceString();
        TestUtils.mutableSubSequence(p(),b -> fsb(b).data(write(s)));
    }
    
    @Test
    void testMIterator() {
        final String s = TestUtils.getMItrString();
        TestUtils.mutableIterator(p(),b -> fsb(b).data(write(s)));
    }
}
