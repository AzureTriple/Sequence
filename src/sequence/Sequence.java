package sequence;

import java.util.Iterator;

public abstract class Sequence implements CharSequence,Iterable<Character>,Comparable<CharSequence> {
    protected static final boolean DEFAULT_MUTABLE = false;
    
    public static final Sequence EMPTY = new Sequence() {
        @Override public Sequence subSequence(int start,int end) {return EMPTY;}
        @Override public int length() {return 0;}
        @Override public char charAt(int index) {return 0;}
        
        final class EMPTYITR implements SequenceIterator {
            @Override public Character next() {return null;}
            @Override public boolean hasNext() {return false;}
            @Override public Sequence subSequence() {return EMPTY;}
            @Override public Character skipWS() {return null;}
            @Override public Character peekNextNonWS() {return null;}
            @Override public Character peek() {return null;}
            @Override public Character nextNonWS() {return null;}
            @Override public void mark() {}
            @Override public Sequence getParent() {return EMPTY;}
        }
        @Override public SequenceIterator iterator() {return new EMPTYITR();}
        @Override public SequenceIterator reverseIterator() {return new EMPTYITR();}
    };
    
    protected boolean mutable;
    protected Sequence(final boolean mutable) {this.mutable = mutable;}
    protected Sequence() {this(DEFAULT_MUTABLE);}
    
    public boolean mutable() {return mutable;}
    @SuppressWarnings("unchecked")
    public <S extends Sequence> S mutable(final boolean mutable) {
        this.mutable = mutable;
        return (S)this;
    }
    
    /**
     * @return The actual size of this sequence. This method is a cheap hack to get
     *         around the CharSequence interface's {@linkplain #length()} returning
     *         an int, while the sequence could actually be much larger.
     */
    public long size() {return (int)length();}
    
    @Override public abstract char charAt(int index) throws IndexOutOfBoundsException;
    
    @Override
    public int compareTo(final CharSequence o) {
        if(o == null) return 1;
        for(int i = 0;i < Math.min(length(),o.length());++i) {
            final char a = charAt(i),b = o.charAt(i);
            if(a != b) return a - b;
        }
        return length() - o.length();
    }
    
    /**A basic iterator which traverses a {@linkplain Sequence}.*/
    public static interface SequenceIterator extends Iterator<Character> {
        /**
         * @return The value at the cursor without advancing, or <code>null</code> if
         *         {@linkplain SequenceIterator#next()} hasn't been called for the first
         *         time.
         */
        public Character peek();
        /**
         * @return The next non-whitespace character without advancing, or
         *         <code>null</code> if the end is reached.
         */
        public Character peekNextNonWS();
        /**@return The next non-whitespace character, or <code>null</code> if the end is reached.*/
        public Character nextNonWS();
        
        /**@return The closest non-whitespace character, or <code>null</code> if the end is reached.*/
        public Character skipWS();
        /**@return The sequence being iterated over.*/
        public Sequence getParent();
        /**Saves the current position.*/
        public void mark();
        /**@return The sub-sequence between the first marked position to the current index.*/
        public Sequence subSequence();
    }
    
    @Override public abstract SequenceIterator iterator();
    public abstract SequenceIterator reverseIterator();
}
































