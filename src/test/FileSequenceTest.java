package test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import sequence.FileSequenceBuilder;
import sequence.SequenceBuilder;
import test.TestUtils.provider;

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
    provider p() {return () -> new FileSequenceBuilder();}
    FileSequenceBuilder fsb(final SequenceBuilder sb) {return (FileSequenceBuilder)sb;}
    
    @Test
    void testBuilder() {
        TestUtils.testBuilder(
            p(),
            b -> fsb(b).data(write(TestUtils.getTestBuilderString())),
            b -> fsb(b).start((long)TestUtils.getTestBuilderStart()),
            b -> fsb(b).start((long)TestUtils.getTestBuilderFakeStart()),
            b -> fsb(b).end((long)TestUtils.getTestBuilderEnd()),
            b -> fsb(b).end((long)TestUtils.getTestBuilderFakeEnd()),
            b -> fsb(b).range((long)TestUtils.getTestBuilderStart(),(long)TestUtils.getTestBuilderEnd()),
            b -> fsb(b).range((long)TestUtils.getTestBuilderFakeStart(),(long)TestUtils.getTestBuilderFakeEnd()),
            b -> fsb(b).length((long)TestUtils.getTestBuilderLength()),
            b -> fsb(b).offset((long)TestUtils.getTestBuilderStart(),(long)TestUtils.getTestBuilderLength()),
            b -> fsb(b).offset((long)TestUtils.getTestBuilderFakeStart(),(long)TestUtils.getTestBuilderFakeLength()),
            b -> fsb(b).start((long)TestUtils.getTestBuilderString().length() + 1L),
            b -> fsb(b).start((long)-TestUtils.getTestBuilderString().length() - 1L),
            b -> fsb(b).length(-1L),
            b -> fsb(b).offset(-1L,2L),
            b -> fsb(b).end((long)TestUtils.getTestBuilderString().length() + 1L),
            b -> fsb(b).end((long)-TestUtils.getTestBuilderString().length() - 1L),
            b -> fsb(b).start(2L).end(1L)
        );
    }
    
    @Test
    void testLength() {
        final String s = TestUtils.getLengthString();
        TestUtils.length(p(),b -> fsb(b).data(write(s)));
    }
    
    @Test
    void testCharAt() {
        final String s = TestUtils.getCharAtString();
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
    void testSubSequence() {
        final String s = TestUtils.getSubSequenceString();
        TestUtils.subSequence(p(),b -> fsb(b).data(write(s)));
    }
    
    @Test
    void testIterator() {
        {
            final String s = TestUtils.getSimpleItrString();
            TestUtils.iterator(p(),b -> fsb(b).data(write(s)));
        }
        {
            final String s = TestUtils.getItr1();
            TestUtils.itr1(p(),b -> fsb(b).data(write(s)));
        }
        {
            final String s = TestUtils.getItr2();
            TestUtils.itr2(p(),b -> fsb(b).data(write(s)));
        }
        {
            final String s = TestUtils.getItr3();
            TestUtils.itr3(p(),b -> fsb(b).data(write(s)));
        }
    }
}






























