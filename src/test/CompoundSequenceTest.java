package test;

import java.util.Random;

import org.junit.jupiter.api.Test;
import sequence.ArraySequenceBuilder;
import sequence.CompoundSequenceBuilder;
import sequence.Sequence;
import sequence.SequenceBuilder;
import test.TestUtils.provider;

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
    static Sequence[] split(final String str) {
        final Sequence[] out = new Sequence[3];
        final int limit = str.length() / 3;
        int start = 0;
        for(int i = 0;i < 2;++i) {
            final int end = start + 1 + r.nextInt(limit);
            out[i] = new ArraySequenceBuilder().data(str.subSequence(start,end)).build();
            start = end;
        }
        out[2] = new ArraySequenceBuilder().data(str.substring(start)).build();
        return out;
    }
    
    provider p() {return () -> new CompoundSequenceBuilder();}
    CompoundSequenceBuilder csb(final SequenceBuilder sb) {return (CompoundSequenceBuilder)sb;}
    
    @Test
    void testBuilder() {
        TestUtils.testBuilder(
            p(),
            b -> csb(b).data(split(TestUtils.getTestBuilderString())),
            b -> csb(b).start((long)TestUtils.getTestBuilderStart()),
            b -> csb(b).start((long)TestUtils.getTestBuilderFakeStart()),
            b -> csb(b).end((long)TestUtils.getTestBuilderEnd()),
            b -> csb(b).end((long)TestUtils.getTestBuilderFakeEnd()),
            b -> csb(b).range((long)TestUtils.getTestBuilderStart(),(long)TestUtils.getTestBuilderEnd()),
            b -> csb(b).range((long)TestUtils.getTestBuilderFakeStart(),(long)TestUtils.getTestBuilderFakeEnd()),
            b -> csb(b).length((long)TestUtils.getTestBuilderLength()),
            b -> csb(b).offset((long)TestUtils.getTestBuilderStart(),(long)TestUtils.getTestBuilderLength()),
            b -> csb(b).offset((long)TestUtils.getTestBuilderFakeStart(),(long)TestUtils.getTestBuilderFakeLength()),
            b -> csb(b).start((long)TestUtils.getTestBuilderString().length() + 1L),
            b -> csb(b).start((long)-TestUtils.getTestBuilderString().length() - 1L),
            b -> csb(b).length(-1L),
            b -> csb(b).offset(-1L,2L),
            b -> csb(b).end((long)TestUtils.getTestBuilderString().length() + 1L),
            b -> csb(b).end((long)-TestUtils.getTestBuilderString().length() - 1L),
            b -> csb(b).start(2L).end(1L)
        );
    }
    
    @Test
    void testLength() {
        final String s = TestUtils.getLengthString();
        TestUtils.length(p(),b -> csb(b).data(split(s)));
    }
    
    @Test
    void testCharAt() {
        final String s = TestUtils.getCharAtString();
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
    void testSubSequence() {
        final String s = TestUtils.getSubSequenceString();
        TestUtils.subSequence(p(),b -> csb(b).data(split(s)));
    }
    
    @Test
    void testIterator() {
        {
            final String s = TestUtils.getSimpleItrString();
            TestUtils.iterator(p(),b -> csb(b).data(split(s)));
        }
        {
            final String s = TestUtils.getItr1();
            TestUtils.itr1(p(),b -> csb(b).data(split(s)));
        }
        {
            final String s = TestUtils.getItr2();
            TestUtils.itr2(p(),b -> csb(b).data(split(s)));
        }
        {
            final String s = TestUtils.getItr3();
            TestUtils.itr3(p(),b -> csb(b).data(split(s)));
        }
    }
}



































