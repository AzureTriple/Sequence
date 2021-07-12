package sequence;

public interface MutableSequence extends Sequence {
    MutableSequence set(int index,char c) throws IndexOutOfBoundsException;
    MutableSequence set(long index,char c) throws IndexOutOfBoundsException;
    
    MutableSequence set(int offset,char[] data) throws IndexOutOfBoundsException;
    MutableSequence set(long offset,char[] data) throws IndexOutOfBoundsException;
    
    /**
     * @param offset
     * @param data
     * 
     * @return
     * 
     * @throws IndexOutOfBoundsException
     * 
     * @implSpec If the data happens to be a mutable sequence of the same type as
     *           this object, then it should be re-structured to share its data with
     *           this object.
     */
    MutableSequence set(int offset,CharSequence data) throws IndexOutOfBoundsException;
    MutableSequence set(long offset,CharSequence data) throws IndexOutOfBoundsException;
    
    @Override MutableSequence subSequence(int start,int end) throws IndexOutOfBoundsException;
    @Override MutableSequence subSequence(long start,long end) throws IndexOutOfBoundsException;
    
    interface MutableSequenceIterator extends SequenceIterator {
        MutableSequenceIterator set(char c) throws IndexOutOfBoundsException;
        MutableSequenceIterator set(int offset,char c) throws IndexOutOfBoundsException;
        MutableSequenceIterator set(long offset,char c) throws IndexOutOfBoundsException;
        
        @Override MutableSequence subSequence() throws IndexOutOfBoundsException;
    }
}