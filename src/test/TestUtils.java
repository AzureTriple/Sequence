package test;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.Consumer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import sequence.ArraySequenceBuilder;
import sequence.CompoundSequenceBuilder;
import sequence.FileSequenceBuilder;
import sequence.MutableSequence;
import sequence.MutableSequence.MutableSequenceIterator;
import sequence.Sequence;
import sequence.Sequence.SequenceIterator;
import sequence.SequenceBuilder;

final class TestUtils {
    private TestUtils() {}
    
    static interface appender {SequenceBuilder append(SequenceBuilder asb);}
    static interface provider {SequenceBuilder provide();}
    
    static void streq(String a,Sequence b) {
        assertEquals(
            a,
            b.toString()
        );
    }
    static void buildeq(final String a,final boolean before,final SequenceBuilder b,
                        final appender data,final appender...appenders) {
        if(before) data.append(b);
        for(final appender app : appenders) app.append(b);
        if(!before) data.append(b);
        final Sequence as = b.build();
        streq(a,as);
    }
    static void buildeq(final String a,final SequenceBuilder b,
                        final appender data,final appender...appenders) {
        buildeq(a,true,b,data,appenders);
        buildeq(a,false,b,data,appenders);
    }
    
    static String getTestBuilderString() {return "0123456789ABCDEF";}
    static int getTestBuilderStart() {return 5;}
    static int getTestBuilderEnd() {return 10;}
    static int getTestBuilderLength() {return 8;}
    static int getTestBuilderFakeStart() {return 6;}
    static int getTestBuilderFakeEnd() {return 11;}
    static int getTestBuilderFakeLength() {return 9;}
    static void testBuilder(final provider p,
                            final appender DATA,
                            final appender START,
                            final appender FAKE_START,
                            final appender END,
                            final appender FAKE_END,
                            final appender RANGE,
                            final appender FAKE_RANGE,
                            final appender LENGTH,
                            final appender OFFSET,
                            final appender FAKE_OFFSET,
                            final appender OOB_START_P,
                            final appender OOB_START_N,
                            final appender OOB_LENGTH,
                            final appender OOB_OFFSET,
                            final appender OOB_END_P,
                            final appender OOB_END_N,
                            final appender OOB_START_END) {
        final String test = getTestBuilderString();
        final int start = getTestBuilderStart(),
                  end = getTestBuilderEnd(),
                  length = getTestBuilderLength();
        
        streq(test,DATA.append(p.provide()).build());
        
        buildeq(test.substring(start),p.provide(),DATA,START);
        buildeq(test.substring(start),p.provide(),DATA,FAKE_START,START);
        buildeq(test.substring(0,end),p.provide(),DATA,END);
        buildeq(test.substring(0,end),p.provide(),DATA,FAKE_END,END);
        
        final String range = test.substring(start,end);
        buildeq(range,p.provide(),DATA,START,END);
        buildeq(range,p.provide(),DATA,END,START);
        buildeq(range,p.provide(),DATA,RANGE);
        buildeq(range,p.provide(),DATA,FAKE_START,START,END);
        buildeq(range,p.provide(),DATA,FAKE_END,START,END);
        buildeq(range,p.provide(),DATA,START,FAKE_END,END);
        buildeq(range,p.provide(),DATA,FAKE_END,END,START);
        buildeq(range,p.provide(),DATA,END,FAKE_START,START);
        buildeq(range,p.provide(),DATA,FAKE_START,RANGE);
        buildeq(range,p.provide(),DATA,FAKE_END,RANGE);
        buildeq(range,p.provide(),DATA,FAKE_START,FAKE_END,RANGE);
        buildeq(range,p.provide(),DATA,FAKE_RANGE,RANGE);
        buildeq(range,p.provide(),DATA,FAKE_RANGE,START,END);
        
        buildeq(test.substring(0,length),p.provide(),DATA,LENGTH);
        final String offs = test.substring(start,start + length);
        buildeq(offs,p.provide(),DATA,START,LENGTH);
        buildeq(offs,p.provide(),DATA,OFFSET);
        buildeq(offs,p.provide(),DATA,FAKE_START,START,LENGTH);
        buildeq(offs,p.provide(),DATA,FAKE_END,OFFSET);
        buildeq(offs,p.provide(),DATA,FAKE_OFFSET,START,LENGTH);
        buildeq(offs,p.provide(),DATA,FAKE_OFFSET,LENGTH,START);
        buildeq(offs,p.provide(),DATA,FAKE_OFFSET,OFFSET);
        buildeq(offs,p.provide(),DATA,RANGE,OFFSET);
        buildeq(range,p.provide(),DATA,OFFSET,RANGE);
        
        assertThrows(IllegalArgumentException.class,() -> DATA.append(OOB_START_P.append(p.provide())).build());
        assertThrows(IllegalArgumentException.class,() -> DATA.append(OOB_START_N.append(p.provide())).build());
        
        assertThrows(IllegalArgumentException.class,() -> DATA.append(OOB_LENGTH.append(p.provide())).build());
        assertThrows(IllegalArgumentException.class,() -> DATA.append(OOB_OFFSET.append(p.provide())).build());
        
        assertThrows(IllegalArgumentException.class,() -> DATA.append(OOB_END_P.append(p.provide())).build());
        assertThrows(IllegalArgumentException.class,() -> DATA.append(OOB_END_N.append(p.provide())).build());
        assertThrows(IllegalArgumentException.class,() -> DATA.append(OOB_START_END.append(p.provide())).build());
    }
    
    static String getLengthString() {return getTestBuilderString();}
    static void length(final provider p,final appender DATA) {
        assertEquals(getLengthString().length(),DATA.append(p.provide()).build().size());
    }
    
    static String getCharAtString() {return getTestBuilderString();}
    static int getCharAtStart() {return getTestBuilderStart();}
    static int getCharAtEnd() {return getTestBuilderEnd();}
    static void charAt(final provider p,final appender DATA,final appender START,final appender END) {
        final char[] arr = getCharAtString().toCharArray();
        {
            final Sequence a = DATA.append(p.provide()).build();
            for(int i = 0;i < arr.length;++i) {
                assertEquals(arr[i],a.charAt(i),"a[%d]".formatted(i));
                assertEquals(arr[i],a.charAt((long)i),"a[%dL]".formatted(i));
                if(i != 0) {
                    assertEquals(arr[arr.length - i],a.charAt(-i),"a[-%d]".formatted(i));
                    assertEquals(arr[arr.length - i],a.charAt((long)-i),"a[-%dL]".formatted(i));
                }
            }
            assertThrows(IndexOutOfBoundsException.class,() -> a.charAt(arr.length));
            assertThrows(IndexOutOfBoundsException.class,() -> a.charAt(-arr.length - 1));
            assertThrows(IndexOutOfBoundsException.class,() -> a.charAt((long)arr.length));
            assertThrows(IndexOutOfBoundsException.class,() -> a.charAt((long)-arr.length - 1L));
        }
        
        {
            final int start = getCharAtStart(),end = getCharAtEnd();
            final Sequence b = START.append(END.append(DATA.append(p.provide()))).build();
            for(int i = start;i < end;++i) {
                assertEquals(arr[i],b.charAt(i - start),"b[%d]".formatted(i - start));
                assertEquals(arr[i],b.charAt((long)(i - start)),"b[%dL]".formatted(i - start));
                if(i != start) {
                    assertEquals(arr[end - i + start],b.charAt(start - i),"b[%d]".formatted(start - i));
                    assertEquals(arr[end - i + start],b.charAt((long)(start - i)),"b[%dL]".formatted(start - i));
                }
            }
            assertThrows(IndexOutOfBoundsException.class,() -> b.charAt(end - start));
            assertThrows(IndexOutOfBoundsException.class,() -> b.charAt(start - end - 1));
            assertThrows(IndexOutOfBoundsException.class,() -> b.charAt((long)(end - start)));
            assertThrows(IndexOutOfBoundsException.class,() -> b.charAt((long)(start - end) - 1L));
        }
    }
    
    static String getSubSequenceString() {return getTestBuilderString();}
    private static void ssbase(final String s,final Sequence a,final int x,final int y) {
        streq(s,a.subSequence(0,s.length()));
        streq(s.substring(0,s.length() - 1),a.subSequence(0,-1));
        streq(s.substring(x,y),a.subSequence(x,y));

        streq(s,a.subSequence(0L,(long)s.length()));
        streq(s.substring(0,s.length() - 1),a.subSequence(0L,-1L));
        streq(s.substring(x,y),a.subSequence((long)x,(long)y));
        
        assertThrows(IndexOutOfBoundsException.class,() -> a.subSequence(0,s.length() + 1));
        assertThrows(IndexOutOfBoundsException.class,() -> a.subSequence(0,-s.length() - 1));
        assertThrows(IndexOutOfBoundsException.class,() -> a.subSequence(s.length() + 1,0));
        assertThrows(IndexOutOfBoundsException.class,() -> a.subSequence(-s.length() - 1,0));
        assertThrows(IndexOutOfBoundsException.class,() -> a.subSequence(s.length() + 1,s.length() + 1));
        assertThrows(IndexOutOfBoundsException.class,() -> a.subSequence(-s.length() - 1,s.length() + 1));
        assertThrows(IndexOutOfBoundsException.class,() -> a.subSequence(s.length() + 1,-s.length() - 1));
        assertThrows(IndexOutOfBoundsException.class,() -> a.subSequence(-s.length() - 1,-s.length() - 1));
        assertThrows(IndexOutOfBoundsException.class,() -> a.subSequence(1,0));
        
        assertThrows(IndexOutOfBoundsException.class,() -> a.subSequence(0L,(long)s.length() + 1L));
        assertThrows(IndexOutOfBoundsException.class,() -> a.subSequence(0L,(long)-s.length() - 1L));
        assertThrows(IndexOutOfBoundsException.class,() -> a.subSequence((long)s.length() + 1L,0L));
        assertThrows(IndexOutOfBoundsException.class,() -> a.subSequence((long)-s.length() - 1L,0L));
        assertThrows(IndexOutOfBoundsException.class,() -> a.subSequence((long)s.length() + 1L,(long)s.length() + 1L));
        assertThrows(IndexOutOfBoundsException.class,() -> a.subSequence((long)-s.length() - 1L,(long)s.length() + 1L));
        assertThrows(IndexOutOfBoundsException.class,() -> a.subSequence((long)s.length() + 1L,(long)-s.length() - 1L));
        assertThrows(IndexOutOfBoundsException.class,() -> a.subSequence((long)-s.length() - 1L,(long)-s.length() - 1L));
        assertThrows(IndexOutOfBoundsException.class,() -> a.subSequence(1L,0L));
    }
    static void subSequence(final provider p,final appender DATA) {
        final Sequence a = DATA.append(p.provide()).build();
        final String str = getSubSequenceString();
        ssbase(str,a,5,10);
        ssbase(
            str.substring(1,5),
            a.subSequence(1,5),
            2,4
        );
    }
    
    static String getSimpleItrString() {return getTestBuilderString();}
    private static void itrbase(final String s,final Sequence a) {
        int i = 0;
        for(final char c : a)
            assertEquals(s.charAt(i++),c);
        i = 2;
        try(Sequence.SimpleSequenceIterator itr = a.iterator()) {
            itr.skip(2);
            while(itr.hasNext())
                assertEquals(s.charAt(i++),itr.next());
        }
        try(Sequence.SimpleSequenceIterator itr = a.iterator()) {
            itr.skip(2);
            itr.forEachRemaining(new Consumer<Character>() {
                int i = 2;
                @Override
                public void accept(Character t) {
                    assertEquals(s.charAt(i++),t);
                }
            });
        }
    }
    static void iterator(final provider p,final appender DATA) {
        final Sequence a = DATA.append(p.provide()).build();
        final String str = getSimpleItrString();
        final int x = 5,y = 10;
        itrbase(str,a);
        itrbase(str.substring(x,y),
            a.subSequence(x,y));
    }
    
    private static void itrbasic(String s,final Sequence a) {
        try(SequenceIterator i = a.forwardIterator()) {
            assertEquals(a,i.getParent());
            long si = 0L;
            while(i.hasNext()) {
                streq(s.substring(0,(int)si),i.subSequence());
                assertEquals(si,i.index());
                assertEquals(si,i.offset());
                assertEquals(s.charAt((int)si),i.peek());
                assertEquals(s.charAt((int)si++),i.next());
            }
            assertEquals((long)s.length(),si,"forwardIterator().hasNext() stopped early");
            assertNull(i.next());
            assertNull(i.peek());
        }
        try(SequenceIterator i = a.reverseIterator()) {
            assertEquals(a,i.getParent());
            long si = s.length();
            while(i.hasNext()) {
                streq(s.substring((int)si),i.subSequence());
                assertEquals(--si,i.index());
                assertEquals(s.length() - si - 1L,i.offset());
                assertEquals(s.charAt((int)si),i.peek());
                assertEquals(s.charAt((int)si),i.next());
            }
            assertEquals(0L,si,"reverseIterator().hasNext() stopped early");
            assertNull(i.next());
            assertNull(i.peek());
        }
    }
    private static void itrJump(final long length,Sequence a) {
        try(SequenceIterator f = a.forwardIterator();
            SequenceIterator r = a.reverseIterator()) {
            assertEquals(3L,f.jumpOffset(3).index());
            assertEquals(length - 4L,r.jumpOffset(3).index());
            
            assertEquals(6L,f.jumpOffset(3).index());
            assertEquals(length - 7L,r.jumpOffset(3).index());
            
            assertEquals(3L,f.jumpOffset(-3).index());
            assertEquals(length - 4L,r.jumpOffset(-3).index());
            
            assertEquals(2L,f.jumpTo(2).index());
            assertEquals(2L,r.jumpTo(2).index());
            
            assertEquals(length - 2L,f.jumpTo(-2).index());
            assertEquals(length - 2L,r.jumpTo(-2).index());
        }
        try(SequenceIterator f = a.forwardIterator();
            SequenceIterator r = a.reverseIterator()) {
            assertEquals(3L,f.jumpOffset(3L).index());
            assertEquals(length - 4L,r.jumpOffset(3L).index());
            
            assertEquals(6L,f.jumpOffset(3L).index());
            assertEquals(length - 7L,r.jumpOffset(3L).index());
            
            assertEquals(3L,f.jumpOffset(-3L).index());
            assertEquals(length - 4L,r.jumpOffset(-3L).index());
            
            assertEquals(2L,f.jumpTo(2L).index());
            assertEquals(2L,r.jumpTo(2L).index());
            
            assertEquals(length - 2L,f.jumpTo(-2L).index());
            assertEquals(length - 2L,r.jumpTo(-2L).index());
        }
    }
    private static void itrPeekOffset(String s,Sequence a) {
        try(SequenceIterator f = a.forwardIterator();
            SequenceIterator r = a.reverseIterator()) {
            assertEquals(s.charAt(s.length() / 2),f.peek(s.length() / 2));
            assertEquals(s.charAt((s.length() - 1) / 2),r.peek(s.length() / 2));
            f.jumpTo(-1);
            r.jumpTo(0);
            assertEquals(s.charAt((s.length() - 1) / 2),f.peek(-s.length() / 2));
            assertEquals(s.charAt(s.length() / 2),r.peek(-s.length() / 2));
            
            assertNull(f.peek(1));
            assertNull(r.peek(1));
        }
        try(SequenceIterator f = a.forwardIterator();
            SequenceIterator r = a.reverseIterator()) {
            assertEquals(s.charAt(s.length() / 2),f.peek(s.length() / 2L));
            assertEquals(s.charAt((s.length() - 1) / 2),r.peek(s.length() / 2L));
            f.jumpTo(-1L);
            r.jumpTo(0L);
            assertEquals(s.charAt((s.length() - 1) / 2),f.peek(-s.length() / 2L));
            assertEquals(s.charAt(s.length() / 2),r.peek(-s.length() / 2L));
            
            assertNull(f.peek(1L));
            assertNull(r.peek(1L));
        }
    }
    private static void itrmark(String s,Sequence a) {
        try(SequenceIterator f = a.forwardIterator();
            SequenceIterator r = a.reverseIterator()) {
            f.jumpTo(1).mark().jumpTo(5);
            streq(s.substring(1,5),f.subSequence());
            
            r.jumpTo(4).mark().jumpTo(0);
            streq(s.substring(1,5),r.subSequence());
            
            f.jumpTo(1).mark();
            streq("",f.subSequence());
            
            r.jumpTo(1).mark();
            streq("",r.subSequence());
        }
        try(SequenceIterator f = a.forwardIterator();
            SequenceIterator r = a.reverseIterator()) {
            f.jumpTo(1L).mark().jumpTo(5L);
            streq(s.substring(1,5),f.subSequence());
            
            r.jumpTo(4L).mark().jumpTo(0L);
            streq(s.substring(1,5),r.subSequence());
            
            f.jumpTo(1L).mark();
            streq("",f.subSequence());
            
            r.jumpTo(1L).mark();
            streq("",r.subSequence());
        }
    }
    private static void itrskip(String s,Sequence a) {
        try(SequenceIterator f = a.forwardIterator();
            SequenceIterator r = a.reverseIterator()) {
            {
                final char c = s.stripLeading().charAt(0);
                assertEquals(c,f.peekNonWS());
                assertEquals(c,f.skipWS());
                f.jumpTo(0);
            }
            
            {
                final String s2 = s.stripTrailing();
                final char c = s2.charAt(s2.length() - 1);
                assertEquals(c,r.peekNonWS());
                assertEquals(c,r.skipWS());
                r.jumpTo(-1);
            }
            
            {
                final char c = s.substring(1).stripLeading().charAt(0);
                assertEquals(
                    c,
                    f.peekNextNonWS()
                );
                assertEquals(
                    c,
                    f.nextNonWS()
                );
                f.jumpTo(0);
            }
            
            {
                final String s2 = s.substring(0,s.length() - 1).stripTrailing();
                final char c = s2.charAt(s2.length() - 1);
                assertEquals(
                    c,
                    r.peekNextNonWS()
                );
                assertEquals(
                    c,
                    r.nextNonWS()
                );
                r.jumpTo(-1);
            }
        }
    }
    private static void itrskiplim(String s,Sequence a) {
        try(SequenceIterator f = a.forwardIterator();
            SequenceIterator r = a.reverseIterator()) {
            {
                Character c = null;
                for(int i = 0;i < 3;++i)
                    if(!Character.isWhitespace(s.charAt(i))) {
                        c = s.charAt(i);
                        break;
                    }
                assertEquals(c,f.peekNonWS(3));
                assertEquals(c,f.skipWS(3));
                f.jumpTo(0);
                assertEquals(c,f.peekNonWS(3L));
                assertEquals(c,f.skipWS(3L));
                f.jumpTo(0);
            }
            
            {
                Character c = null;
                for(int i = s.length();--i > 3;)
                    if(!Character.isWhitespace(s.charAt(i))) {
                        c = s.charAt(i);
                        break;
                    }
                assertEquals(c,r.peekNonWS(3));
                assertEquals(c,r.skipWS(3));
                r.jumpTo(-1);
                assertEquals(c,r.peekNonWS(3L));
                assertEquals(c,r.skipWS(3L));
                r.jumpTo(-1);
            }
            
            {
                Character c = null;
                for(int i = 1;i < 3;++i)
                    if(!Character.isWhitespace(s.charAt(i))){
                        c = s.charAt(i);
                        break;
                    }
                assertEquals(c,f.peekNextNonWS(3));
                assertEquals(c,f.nextNonWS(3));
                f.jumpTo(0);
                assertEquals(c,f.peekNextNonWS(3L));
                assertEquals(c,f.nextNonWS(3L));
                f.jumpTo(0);
            }
            
            {
                Character c = null;
                for(int i = s.length() - 1;--i > 3;)
                    if(!Character.isWhitespace(s.charAt(i))){
                        c = s.charAt(i);
                        break;
                    }
                assertEquals(c,r.peekNextNonWS(3));
                assertEquals(
                    c,
                    r.nextNonWS(3)
                );
                r.jumpTo(-1);
                assertEquals(c,r.peekNextNonWS(3L));
                assertEquals(c,r.nextNonWS(3L));
                r.jumpTo(-1);
            }
        }
    }
    private static void itrskipnull(Sequence a) {
        try(SequenceIterator f = a.forwardIterator();
            SequenceIterator r = a.reverseIterator()) {
            assertNull(f.peekNonWS());
            assertNull(f.skipWS());
            f.jumpTo(0);
            
            assertNull(r.peekNonWS());
            assertNull(r.skipWS());
            r.jumpTo(-1);
            
            assertNull(f.peekNonWS(3));
            assertNull(f.skipWS(3));
            f.jumpTo(0);
            
            assertNull(r.peekNonWS(3));
            assertNull(r.skipWS(3));
            r.jumpTo(-1);
            
            assertNull(f.peekNonWS(3L));
            assertNull(f.skipWS(3L));
            f.jumpTo(0);
            
            assertNull(r.peekNonWS(3L));
            assertNull(r.skipWS(3L));
            r.jumpTo(-1);
            
            
            assertNull(f.peekNextNonWS());
            assertNull(f.nextNonWS());
            f.jumpTo(0);
            
            assertNull(r.peekNextNonWS());
            assertNull(r.nextNonWS());
            r.jumpTo(-1);
            
            assertNull(f.peekNextNonWS(2));
            assertNull(f.nextNonWS(2));
            f.jumpTo(0);
            
            assertNull(r.peekNextNonWS(2));
            assertNull(r.nextNonWS(2));
            r.jumpTo(-1);
            
            assertNull(f.peekNextNonWS(2L));
            assertNull(f.nextNonWS(2L));
            f.jumpTo(0);
            
            assertNull(r.peekNextNonWS(2L));
            assertNull(r.nextNonWS(2L));
            r.jumpTo(-1);
        }
    }
    
    public static String getItr1() {return getTestBuilderString();}
    public static void itr1(final provider p,final appender DATA) {
        final Sequence a = DATA.append(p.provide()).build();
        final String test = getItr1(),unwrapped = test.substring(1,test.length() - 1);
        itrbasic(test,a);
        itrbasic(unwrapped,a.subSequence(1,-1));
        
        itrJump(test.length(),a);
        itrJump(test.length() - 2,a.subSequence(1,-1));
        
        itrPeekOffset(test,a);
        itrPeekOffset(unwrapped,a.subSequence(1,-1));
        
        itrmark(test,a);
        itrmark(unwrapped,a.subSequence(1,-1));
    }
    public static String getItr2() {return "x abc \t\n x";}
    public static void itr2(final provider p,final appender DATA) {
        final Sequence a = DATA.append(p.provide()).build();
        final String test = getItr2(),unwrapped = test.substring(1,test.length() - 1);
        itrskip(test,a);
        itrskip(unwrapped,a.subSequence(1,-1));
        
        itrskiplim(test,a);
        itrskiplim(unwrapped,a.subSequence(1,-1));
    }
    public static String getItr3() {return "   ";}
    public static void itr3(final provider p,final appender DATA) {
        final Sequence a = DATA.append(p.provide()).build();
        itrskipnull(a);
        itrskipnull(a.subSequence(1,-1));
    }
    
    public static String getMutableSubSequenceString() {return getTestBuilderString();}
    public static void mutableSubSequence(final provider p,final appender DATA) {
        final MutableSequence ms = (MutableSequence)DATA.append(p.provide()).build();
        {
            final int a = ms.length();
            ms.subSequence(1,-1);
            assertEquals(a,ms.length());
        }
        final int x = (ms.length() / 2) - 1;
        ms.set(x,'*');
        {
            final int a = ms.length();
            ms.mutableSubSequence(1,-1);
            assertEquals(a - 2,ms.length());
        }
        final String s;
        {
            final String t = getMutableSubSequenceString();
            s = t.substring(1,t.length() - 1);
        }
        for(int i = 0;i < x - 1;++i)
            assertEquals(s.charAt(i),ms.charAt(i));
        assertEquals('*',ms.charAt(x - 1));
        for(int i = x;i < s.length();++i)
            assertEquals(s.charAt(i),ms.charAt(i));
    }
    
    public static String getSetChrString() {return getTestBuilderString();}
    private static void schr(final String s,final MutableSequence ms) {
        final int a = 0,b = ms.length() / 2,c = -1;
        ms.set(a,'*').set((long)b,'*').set(c,'*');
        assertEquals('*',ms.charAt(a));
        assertEquals('*',ms.charAt(b));
        assertEquals('*',ms.charAt(c));
        for(int i = a + 1;i < b;++i)
            assertEquals(s.charAt(i),ms.charAt(i));
        for(int i = b + 1;i < c;++i)
            assertEquals(s.charAt(i),ms.charAt(i));
    }
    public static void setChr(final provider p,final appender DATA) {
        final MutableSequence ms = (MutableSequence)DATA.append(p.provide()).build();
        final MutableSequence ms1 = ms.mutableCopy().subSequence(1,-1);
        final String s = getSetChrString();
        schr(s,ms);
        schr(s.substring(1,s.length() - 1),ms1);
    }
    
    public static String getSetArrString() {return getTestBuilderString();}
    static char[] getMArr() {return new char[] {'*','*','*'};}
    private static void sarr(final char[] arr,final String s,final MutableSequence ms) {
        final int a = 0,b = (ms.length() - arr.length) / 2,c = ms.length() - arr.length;
        ms.set(a,arr).set((long)b,arr).set(c,arr);
        for(int i = a,j = b,k = c,l = 0;l < arr.length;++i,++j,++k,++l) {
            final char x = arr[l];
            assertEquals(x,ms.charAt(i));
            assertEquals(x,ms.charAt(j));
            assertEquals(x,ms.charAt(k));
        }
        for(int i = a + arr.length;i < b;++i)
            assertEquals(s.charAt(i),ms.charAt(i));
        for(int i = b + arr.length;i < c;++i)
            assertEquals(s.charAt(i),ms.charAt(i));
    }
    public static void setArr(final provider p,final appender DATA) {
        final MutableSequence ms = (MutableSequence)DATA.append(p.provide()).build();
        final char[] arr = getMArr();
        final MutableSequence ms1 = ms.mutableCopy().subSequence(1,-1);
        final String s = getSetArrString();
        sarr(arr,s,ms);
        sarr(arr,s.substring(1,s.length() - 1),ms1);
    }
    
    public static String getSetCSString() {return getTestBuilderString();}
    private static void setS(final CharSequence cs,final String s,final MutableSequence ms) {
        final int a = 0,b = (ms.length() - cs.length()) / 2,c = ms.length() - cs.length();
        ms.set(a,cs).set((long)b,cs).set(c,cs);
        for(int i = a,j = b,k = c,l = 0;l < cs.length();++i,++j,++k,++l) {
            final char x = cs.charAt(l);
            assertEquals(x,ms.charAt(i));
            assertEquals(x,ms.charAt(j));
            assertEquals(x,ms.charAt(k));
        }
        for(int i = a + cs.length();i < b;++i)
            assertEquals(s.charAt(i),ms.charAt(i),ms.toString());
        for(int i = b + cs.length();i < c;++i)
            assertEquals(s.charAt(i),ms.charAt(i),ms.toString());
    }
    private static void setS2(final CharSequence cs,final String s,final MutableSequence ms) {
        final MutableSequence ms1 = ms.mutableCopy().subSequence(1,-1);
        setS(cs,s,ms);
        setS(cs.subSequence(0,cs.length() - 1),s.substring(1,s.length() - 1),ms1);
    }
    public static void setCS(final provider p,final appender DATA) {
        final MutableSequence ms = (MutableSequence)DATA.append(p.provide()).build();
        final String s = getSetCSString();
        setS2("***",s,ms.mutableCopy());
        setS2(new ArraySequenceBuilder().data('*','*','*').build(),s,ms.mutableCopy());
        try {
            final File out = Files.createTempFile(null,null).toFile();
            out.deleteOnExit();
            try(BufferedWriter w = new BufferedWriter(new FileWriter(out,StandardCharsets.UTF_8))) {
                w.write("***");
            }
            setS2(new FileSequenceBuilder().data(out).build(),s,ms.mutableCopy());
        } catch(final IOException e) {throw new UncheckedIOException(e);}
        setS2(
            new CompoundSequenceBuilder().data(
                new ArraySequenceBuilder().data('*','*').build(),
                new ArraySequenceBuilder().data('*').build()
            ).build(),
            s,
            ms
        );
    }
    
    public static String getMItrString() {return getTestBuilderString();}
    private static void mitr(final String s,final MutableSequence ms) {
        final int a = 0,b = ms.length() / 2,c = ms.length() - 1;
        try(final MutableSequenceIterator f = ms.mutableCopy().forwardIterator()) {
            int i = 0;
            while(f.hasNext()) {
                if(i == a || i == b || i == c) {
                    f.set('*');
                    assertEquals('*',f.next());
                } else
                    assertEquals(s.charAt(i),f.next());
                ++i;
            }
            final MutableSequence p = f.getParent();
            assertEquals('*',p.charAt(a));
            assertEquals('*',p.charAt(b));
            assertEquals('*',p.charAt(c));
        }
        try(final MutableSequenceIterator r = ms.mutableCopy().reverseIterator()) {
            int i = ms.length() - 1;
            while(r.hasNext()) {
                if(i == a || i == b || i == c) {
                    r.set('*');
                    assertEquals('*',r.next());
                } else
                    assertEquals(s.charAt(i),r.next());
                --i;
            }
            final MutableSequence p = r.getParent();
            assertEquals('*',p.charAt(a));
            assertEquals('*',p.charAt(b));
            assertEquals('*',p.charAt(c));
        }
        try(final MutableSequenceIterator f = ms.mutableCopy().forwardIterator()) {
            final MutableSequence p = f.jumpOffset(1).set(-1L,'*').set(1,'^').getParent();
            assertEquals('*',p.charAt(0));
            assertEquals('^',p.charAt(2));
        }
        try(final MutableSequenceIterator r = ms.reverseIterator()) {
            final MutableSequence p = r.jumpOffset(1).set(-1L,'*').set(1,'^').getParent();
            assertEquals('*',p.charAt(-1));
            assertEquals('^',p.charAt(-3));
        }
    }
    public static void mutableIterator(final provider p,final appender DATA) {
        final MutableSequence ms = (MutableSequence)DATA.append(p.provide()).build();
        final String s = getMItrString();
        mitr(s,ms.mutableCopy());
        mitr(s.substring(1,s.length() - 1),ms.mutableSubSequence(1,-1));
    }
}












































