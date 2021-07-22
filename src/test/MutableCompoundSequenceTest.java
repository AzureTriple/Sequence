package test;

import org.junit.jupiter.api.Test;
import sequence.MutableCompoundSequenceBuilder;
import sequence.SequenceBuilder;
import test.TestUtils.provider;

class MutableCompoundSequenceTest extends CompoundSequenceTest {
    @Override provider p() {return () -> new MutableCompoundSequenceBuilder();}
    @Override
    MutableCompoundSequenceBuilder csb(SequenceBuilder sb) {
        return (MutableCompoundSequenceBuilder)sb;
    }
    
    @Test
    void testSet() {
        final String s = TestUtils.getSetChrString();
        final long start = TestUtils.getCharAtStart();
        final long end = TestUtils.getCharAtEnd();
        TestUtils.charAt(
            p(),
            b -> csb(b).data(split(s)),
            b -> csb(b).start(start),
            b -> csb(b).end(end)
        );
    }
    
    @Test
    void testSetCharArray() {
        final String s = TestUtils.getSetArrString();
        TestUtils.setArr(p(),b -> csb(b).data(split(s)));
    }
    
    @Test
    void testSetCharSequence() {
        final String s = TestUtils.getSetCSString();
        TestUtils.setCS(p(),b -> csb(b).data(split(s)));
    }
    
    @Test
    void testMutableSubSequence() {
        final String s = TestUtils.getMutableSubSequenceString();
        TestUtils.mutableSubSequence(p(),b -> csb(b).data(split(s)));
    }
    
    @Test
    void testMIterator() {
        final String s = TestUtils.getMItrString();
        TestUtils.mutableIterator(p(),b -> csb(b).data(split(s)));
    }
}
