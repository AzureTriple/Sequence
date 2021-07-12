package sequence;

import java.util.Iterator;

public class CompoundSequence implements Sequence {
    protected Sequence[] data;
    
    protected CompoundSequence(final Sequence[] data) {this.data = data;}
    
    public static class CompoundSequenceBuilder {
        private Sequence[] data;
        
    }
    
    @Override
    public int length() {
        return 0;
    }
    
    @Override
    public Iterator<Character> iterator() {
        return null;
    }
    
    @Override
    public SequenceIterator forwardIterator() {
        return null;
    }
    
    @Override
    public SequenceIterator reverseIterator() {
        return null;
    }
    
    @Override
    public char charAt(int index) throws IndexOutOfBoundsException {
        return 0;
    }
    
    @Override
    public Sequence subSequence(int start,int end) throws IndexOutOfBoundsException {
        return null;
    }
    
    @Override
    public Sequence subSequence(long start,long end) throws IndexOutOfBoundsException {
        return null;
    }
    
}
