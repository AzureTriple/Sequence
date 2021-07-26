package sequence;

import java.io.UncheckedIOException;
import sequence.MutableSequence.MutableSequenceIterator;
import sequence.Sequence.SequenceIterator;

/**
 * A base implementation of {@linkplain MutableSequenceIterator}.
 * 
 * @param <I> The type of {@linkplain SequenceIterator} which this iterator is
 *            composed of.
 * 
 * @author AzureTriple
 */
abstract class MSI<I extends SequenceIterator> implements MutableSequenceIterator {
    /**
     * The "super" iterator. Through this object, an extending class can get access
     * to the unprotected data.
     */
    final I sooper;
    
    MSI(final I sooper) {this.sooper = sooper;}
    
    @Override public long index() {return sooper.index();}
    @Override public long offset() {return sooper.offset();}
    @Override public MutableSequence getParent() {return (MutableSequence)sooper.getParent();}
    
    @Override public Character peek() throws UncheckedIOException {return sooper.peek();}
    @Override public Character peek(final int offset) throws UncheckedIOException {return sooper.peek(offset);}
    @Override public Character peek(final long offset) throws UncheckedIOException {return sooper.peek(offset);}
    
    @Override public boolean hasNext() {return sooper.hasNext();}
    @Override public Character next() throws UncheckedIOException {return sooper.next();}
    
    @Override public Character skipWS() throws UncheckedIOException {return sooper.skipWS();}
    @Override public Character skipWS(final int limit) throws UncheckedIOException {return sooper.skipWS(limit);}
    @Override public Character skipWS(final long limit) throws UncheckedIOException {return sooper.skipWS(limit);}
    
    @Override public Character peekNonWS() throws UncheckedIOException {return sooper.peekNonWS();}
    @Override public Character peekNonWS(final int limit) throws UncheckedIOException {return sooper.peekNonWS(limit);}
    @Override public Character peekNonWS(final long limit) throws UncheckedIOException {return sooper.peekNonWS(limit);}
    
    @Override public Character peekNextNonWS() throws UncheckedIOException {return sooper.peekNextNonWS();}
    @Override public Character peekNextNonWS(final int limit) throws UncheckedIOException {return sooper.peekNextNonWS(limit);}
    @Override public Character peekNextNonWS(final long limit) throws UncheckedIOException {return sooper.peekNextNonWS(limit);}
    
    @Override public Character nextNonWS() throws UncheckedIOException {return sooper.nextNonWS();}
    @Override public Character nextNonWS(final int limit) throws UncheckedIOException {return sooper.nextNonWS(limit);}
    @Override public Character nextNonWS(final long limit) throws UncheckedIOException {return sooper.nextNonWS(limit);}
    
    @Override public boolean find(final char c) throws UncheckedIOException {return sooper.find(c);}
    @Override public boolean find(final int limit,final char c) throws UncheckedIOException {return sooper.find(limit,c);}
    @Override public boolean find(final long limit,final char c) throws UncheckedIOException {return sooper.find(limit,c);}
    
    @Override
    public MutableSequenceIterator mark() {
        sooper.mark();
        return this;
    }
    @Override
    public MutableSequenceIterator mark(final int offset) throws IndexOutOfBoundsException {
        sooper.mark(offset);
        return this;
    }
    @Override
    public MutableSequenceIterator mark(final long offset) throws IndexOutOfBoundsException {
        sooper.mark(offset);
        return this;
    }
    
    @Override
    public MutableSequenceIterator jumpTo(final int index) throws IndexOutOfBoundsException,
                                                                  UncheckedIOException {
        sooper.jumpTo(index);
        return this;
    }
    @Override
    public MutableSequenceIterator jumpTo(final long index) throws IndexOutOfBoundsException,
                                                                   UncheckedIOException {
        sooper.jumpTo(index);
        return this;
    }
    @Override
    public MutableSequenceIterator jumpOffset(final int offset) throws IndexOutOfBoundsException,
                                                                       UncheckedIOException {
        sooper.jumpOffset(offset);
        return this;
    }
    @Override
    public MutableSequenceIterator jumpOffset(final long offset) throws IndexOutOfBoundsException,
                                                                        UncheckedIOException {
        sooper.jumpOffset(offset);
        return this;
    }
    
    @Override public String toString() {return sooper.toString();}
}