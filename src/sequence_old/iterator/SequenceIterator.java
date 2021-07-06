package sequence_old.iterator;

import java.util.Iterator;

import sequence_old.Sequence;

public interface SequenceIterator extends Iterator<Character> {
    public Sequence parent();
    
    public void mark();
    public void mark(int offset);
    public Sequence subSequence();
    
    public int index();
    
    public SequenceIterator jumpTo(int idx);
    public SequenceIterator jumpOffset(int offset);
    
    public Character peek();
    public Character peek(int offset);
    
    public Character skipWS();
    public Character nextNonWS();
    public Character peekNextNonWS();
}