package test;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.Consumer;

import sequence.Sequence;
import sequence.Sequence.SequenceIterator;

final class TestUtils {
    private TestUtils() {}
    
    static final String str = "0123456789ABCDEF";
    static final char[] arr = str.toCharArray();
    static final String ws = "x abc \t\n x";
    static final String wslimit = "          asdf          ";
    static final String skipNull = "   ";
    static final int start = 5,
                     end = 10,
                     length = 8,
                     fake_start = 6,
                     fake_end = 11,
                     fake_length = 9;
    
    static void streq(String a,Sequence b) {
        assertEquals(
            a,
            b.toString()
        );
    }
    
    static void length(Sequence a,Sequence b,Sequence c) {
        assertEquals(arr.length,a.length());
        assertEquals(arr.length,b.length());
        assertEquals(length,c.length());
    }
    
    static void charAt(Sequence a,Sequence b) {
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
    static void subSequence(Sequence a) {
        ssbase(str,a,start,end);
        ssbase(
            str.substring(1,5),
            a.subSequence(1,5),
            2,4
        );
    }
    
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
    static void iterator(Sequence a) {
        itrbase(str,a);
        itrbase(str.substring(start,end),a.subSequence(start,end));
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
    
    public static String getItr1() {return str;}
    public static void itr1(final Sequence a) {
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
    public static String getItr2() {return ws;}
    public static void itr2(final Sequence a) {
        final String test = getItr2(),unwrapped = test.substring(1,test.length() - 1);
        itrskip(test,a);
        itrskip(unwrapped,a.subSequence(1,-1));
        
        itrskiplim(test,a);
        itrskiplim(unwrapped,a.subSequence(1,-1));
    }
    public static String getItr3() {return skipNull;}
    public static void itr3(final Sequence a) {
        itrskipnull(a);
        itrskipnull(a.subSequence(1,-1));
    }
}












































