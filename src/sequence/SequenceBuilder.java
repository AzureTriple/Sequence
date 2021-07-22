package sequence;

import java.io.UncheckedIOException;

/**
 * An interface for objects which build a sequence.
 * 
 * @author AzureTriple
 */
public interface SequenceBuilder {
    /**
     * Constructs the sequence. The sequence returned is not guaranteed to be of the
     * type suggested by type implementing this interface. For example, a builder
     * with contents that return an empty sequence will be the shared instance of
     * {@linkplain Sequence#EMPTY}.
     */
    Sequence build() throws IllegalArgumentException,
                            UncheckedIOException;
}