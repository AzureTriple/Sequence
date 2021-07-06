package sequence_old;

import java.util.Iterator;

public interface Sequence extends Iterable<Character>,CharSequence,Comparable<Sequence> {
    
    int start();
    int end();
    
    default int idx(final int idx) throws IndexOutOfBoundsException {
        final int end = end(),start = start();
        final int out = idx + ((idx < 0L? end : start) >>> 1);
        if(end <= out || out < start)
            throw new IndexOutOfBoundsException(
                "Index %d is out of bounds [%d,%d) (shifted: %d,[0,%d))."
                .formatted(idx,start,end,out,length())
            );
        return out;
    }
    
    @Override default int length() {return end() - start();}
    
    @Override Sequence subSequence(int start,int end) throws IndexOutOfBoundsException;
    Sequence subSequence(int start) throws IndexOutOfBoundsException;
    
    /**@return This sequence repeated <code>count</code> times.*/
    Sequence repeat(int count);
    
    boolean isWrappedIn(char wrap);
    default Sequence unwrap() {return subSequence(start() + 1,end() - 1);}
    
    Iterator<Character> reverseIterator();
    
    Sequence stripLeading();
    Sequence stripTailing();
    Sequence strip();
    
    @Override
    default int compareTo(final Sequence o) {
        if(o == null) return 1;
        for(
            final Iterator<Character> i = iterator(),j = o.iterator();
            i.hasNext() && j.hasNext();
        ) {
            final char a = i.next(),b = j.next();
            if(a != b) return a - b;
        }
        return length() - o.length();
    }
}































