package test;

import org.junit.jupiter.api.Test;
import sequence.ArraySequenceBuilder;
import sequence.SequenceBuilder;
import test.TestUtils.provider;

class ArraySequenceTest {
    provider p() {return () -> new ArraySequenceBuilder();}
    ArraySequenceBuilder asb(final SequenceBuilder sb) {return (ArraySequenceBuilder)sb;}
    
    @Test
    void testBuilder() {
        TestUtils.testBuilder(
            p(),
            b -> asb(b).data(TestUtils.getTestBuilderString()),
            b -> asb(b).start(TestUtils.getTestBuilderStart()),
            b -> asb(b).start(TestUtils.getTestBuilderFakeStart()),
            b -> asb(b).end(TestUtils.getTestBuilderEnd()),
            b -> asb(b).end(TestUtils.getTestBuilderFakeEnd()),
            b -> asb(b).range(TestUtils.getTestBuilderStart(),TestUtils.getTestBuilderEnd()),
            b -> asb(b).range(TestUtils.getTestBuilderFakeStart(),TestUtils.getTestBuilderFakeEnd()),
            b -> asb(b).length(TestUtils.getTestBuilderLength()),
            b -> asb(b).offset(TestUtils.getTestBuilderStart(),TestUtils.getTestBuilderLength()),
            b -> asb(b).offset(TestUtils.getTestBuilderFakeStart(),TestUtils.getTestBuilderFakeLength()),
            b -> asb(b).start(TestUtils.getTestBuilderString().length() + 1),
            b -> asb(b).start(-TestUtils.getTestBuilderString().length() - 1),
            b -> asb(b).length(-1),
            b -> asb(b).offset(-1,2),
            b -> asb(b).end(TestUtils.getTestBuilderString().length() + 1),
            b -> asb(b).end(-TestUtils.getTestBuilderString().length() - 1),
            b -> asb(b).start(2).end(1)
        );
    }
    
    @Test
    void testLength() {
        final String s = TestUtils.getLengthString();
        TestUtils.length(p(),b -> asb(b).data(s));
    }
    
    @Test
    void testCharAt() {
        final String s = TestUtils.getCharAtString();
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
    void testSubSequence() {
        final String s = TestUtils.getSubSequenceString();
        TestUtils.subSequence(p(),b -> asb(b).data(s));
    }
    
    @Test
    void testIterator() {
        {
            final String s = TestUtils.getSimpleItrString();
            TestUtils.iterator(p(),b -> asb(b).data(s));
        }
        {
            final String s = TestUtils.getItr1();
            TestUtils.itr1(p(),b -> asb(b).data(s));
        }
        {
            final String s = TestUtils.getItr2();
            TestUtils.itr2(p(),b -> asb(b).data(s));
        }
        {
            final String s = TestUtils.getItr3();
            TestUtils.itr3(p(),b -> asb(b).data(s));
        }
    }
}












































