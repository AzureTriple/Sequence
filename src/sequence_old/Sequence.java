package sequence_old;

import java.util.Iterator;

/**
 * An arbitrary sequence of characters. Types extending this class are meant to
 * wrap a representation of their character data.
 * 
 * @author AzureTriple
 */
public abstract class Sequence implements CharSequence,Iterable<Character>,Comparable<CharSequence> {
    protected static final boolean DEFAULT_MUTABLE = false;
    
    /**A shared instance of a purely empty sequence.*/
    public static final Sequence EMPTY = new Sequence() {
        @Override public Sequence subSequence(int start,int end) {return EMPTY;}
        @Override public int length() {return 0;}
        @Override public char charAt(int index) {return 0;}
        
        final class EMPTYITR implements SequenceIterator {
            @Override public long index() {return 0L;}
            @Override public Sequence getParent() {return EMPTY;}
            
            @Override public Character peek() {return null;}
            @Override public Character peek(int offset) {return null;}
            @Override public Character peek(long offset) {return null;}
            
            @Override public boolean hasNext() {return false;}
            @Override public Character next() {return null;}
            
            @Override public Character skipWS() {return null;}
            @Override public Character peekNextNonWS() {return null;}
            @Override public Character nextNonWS() {return null;}
            
            @Override public void mark() {}
            @Override public void mark(int offset) {}
            @Override public void mark(long offset) {}
            
            @Override public Sequence subSequence() {return EMPTY;}
            
            @Override public EMPTYITR jumpTo(int index) {return this;}
            @Override public EMPTYITR jumpTo(long index) {return this;}
            @Override public EMPTYITR jumpOffset(int offset) {return this;}
            @Override public EMPTYITR jumpOffset(long offset) {return this;}
        }
        @Override public SequenceIterator iterator() {return new EMPTYITR();}
        @Override public SequenceIterator reverseIterator() {return new EMPTYITR();}
    };
    
    protected boolean mutable;
    
    /**
     * Creates a new sequence.
     * 
     * @param mutable <code>false</code> iff functions that change the start and end
     *                indices apply modifications to a new object.
     */
    protected Sequence(final boolean mutable) {this.mutable = mutable;}
    /**Creates a new immutable sequence.*/
    protected Sequence() {this(DEFAULT_MUTABLE);}
    
    /**
     * @return <code>false</code> iff functions that change the start and end
     *         indices apply modifications to a new object.
     */
    public boolean mutable() {return mutable;}
    /**
     * @param mutable <code>false</code> iff functions that change the start and end
     *                indices apply modifications to a new object.
     * 
     * @return <code>this</code>
     */
    @SuppressWarnings("unchecked")
    public <S extends Sequence> S mutable(final boolean mutable) {
        this.mutable = mutable;
        return (S)this;
    }
    
    /**
     * @return The actual size of this sequence. This method is a cheap hack to get
     *         around the {@linkplain CharSequence} interface's
     *         {@linkplain #length()} returning an int in cases where the sequence
     *         could actually be larger.
     */
    public long size() {return (int)length();}
    
    /**
     * @throws IndexOutOfBoundsException <code>|index| &gt size()</code>
     * 
     * @see java.lang.CharSequence#charAt(int)
     */
    @Override public abstract char charAt(int index) throws IndexOutOfBoundsException;
    
    @Override
    public int compareTo(final CharSequence o) {
        if(o == null) return 1;
        final Iterator<Character> a = iterator();
        if(o instanceof Sequence) {
            for(
                final Iterator<Character> b = ((Sequence)o).iterator();
                a.hasNext() && b.hasNext();
            ) {
                final int diff = Character.compare(a.next(),b.next());
                if(diff != 0) return diff;
            }
            return Long.compare(size(),((Sequence)o).size());
        } else {
            for(int i = 0;a.hasNext() && i < o.length();++i) {
                final int diff = Character.compare(a.next(),o.charAt(i));
                if(diff != 0) return diff;
            }
            return Long.compare(size(),o.length());
        }
    }
    
    /**An iterator which traverses the characters in a {@linkplain Sequence}.*/
    public static interface SequenceIterator extends Iterator<Character> {
        /**
         * @return The index of the character last returned by
         *         {@linkplain SequenceIterator#next()}, adjusted to the current
         *         sequence's range.
         */
        long index();
        /**@return The sequence being iterated over.*/
        Sequence getParent();
        
        /**
         * @return The value at the cursor without advancing, or <code>null</code> if
         *         {@linkplain #next()} hasn't been called for the first
         *         time.
         */
        Character peek();
        /**
         * @return The value at the cursor's position offset by the argument without
         *         advancing, or <code>null</code> if
         *         {@linkplain SequenceIterator#next()} hasn't been called for the first
         *         time.
         */
        Character peek(int offset);
        /**
         * @return The value at the cursor's position offset by the argument without
         *         advancing, or <code>null</code> if
         *         {@linkplain SequenceIterator#next()} hasn't been called for the first
         *         time.
         */
        Character peek(long offset);
        
        /**
         * @return The next element in the iteration, or <code>null</code> if the end is
         *         reached.
         * 
         * @see java.util.Iterator#next()
         */
        @Override Character next();
        
        /**@return The closest non-whitespace character, or <code>null</code> if the end is reached.*/
        Character skipWS();
        /**
         * @return The next non-whitespace character without advancing, or
         *         <code>null</code> if the end is reached.
         */
        Character peekNextNonWS();
        /**@return The next non-whitespace character, or <code>null</code> if the end is reached.*/
        Character nextNonWS();
        
        /**Saves the current position.*/
        void mark();
        /**
         * Saves the current position offset by the argument.
         * 
         * @throws IndexOutOfBoundsException <code>|index| &gt size()</code>
         */
        void mark(int offset) throws IndexOutOfBoundsException;
        /**
         * Saves the current position offset by the argument.
         * 
         * @throws IndexOutOfBoundsException <code>|index| &gt size()</code>
         */
        void mark(long offset) throws IndexOutOfBoundsException;
        
        /**@return The sub-sequence between the first marked position to the current index.*/
        Sequence subSequence();
        
        /**
         * Sets the cursor to the specified position.
         * 
         * @return <code>this</code>
         * 
         * @throws IndexOutOfBoundsException <code>|index| &gt size()</code>
         */
        SequenceIterator jumpTo(int index) throws IndexOutOfBoundsException;
        /**
         * Sets the cursor to the specified position.
         * 
         * @return <code>this</code>
         * 
         * @throws IndexOutOfBoundsException <code>|index| &gt size()</code>
         */
        SequenceIterator jumpTo(long index) throws IndexOutOfBoundsException;
        /**
         * Offsets the current position.
         * 
         * @return <code>this</code>
         * 
         * @throws IndexOutOfBoundsException <code>|index| &gt size()</code>
         */
        SequenceIterator jumpOffset(int offset) throws IndexOutOfBoundsException;
        /**
         * Offsets the current position.
         * 
         * @return <code>this</code>
         * 
         * @throws IndexOutOfBoundsException <code>|index| &gt size()</code>
         */
        SequenceIterator jumpOffset(long offset) throws IndexOutOfBoundsException;
    }
    
    /**
     * @return A {@linkplain SequenceIterator}
     * 
     * @see java.lang.Iterable#iterator()
     * @see SequenceIterator
     */
    @Override public abstract SequenceIterator iterator();
    /**
     * @return An iterator which iterates in the opposite direction as the one
     *         returned by {@linkplain #iterator()}.
     *         
     * @see java.lang.Iterable#iterator()
     * @see SequenceIterator
     */
    public abstract SequenceIterator reverseIterator();
}
































