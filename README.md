# Sequence
A library for handling character sequences.

## Getting Started
Simply import the `Sequence.jar` file from the `Releases` page into your project. To create a `Sequence` object, you should first create a `SequenceBuilder`, either by calling one of the static methods in the `Sequence` interface or directly using one of their constructors.

## Types of Sequences
Sequences can take one of a few different forms, with each form tailored to fit the data type backing it:
   Sequence Type    |  Backing Type
   -------------    |  ------------
  `ArraySequence`   | `char[]`
   `FileSequence`   | `RandomAccessFile`
 `CompoundSequence` | `Sequence[]`
 
Each sequence type also has a `MutableSequence` form, where the type name is the same except with the word `Mutable` prepended. In the case of `MutableCompoundSequence`, the backing type changes to `MutableSequence[]`.

The `SequenceBuilder`s and `subSequence`/`mutableSubSequence` methods do not guarantee which type will be constructed. If the input represents an empty sequence, `Sequence.EMPTY` is returned (which is its own type). Additionally, `CompoundSequence`s may return a sub-sequence of an input child sequence if that is the only used sequence in the input.

Since `FileSequence` and `MutableFileSequence` objects obviously use I/O operations, several methods in the `Sequence` interface are declared with the `throws UncheckedIOException` clause. Methods in certain types which are guaranteed to never cause I/O issues are marked with the `@NoIO` annotation in the source code. Additionally, the `@NoIO` annotation can also specify a `suppresses` argument, which indicates that the method cannot cause a specific issue (e.g. something annotated `@NoIO(suppresses = Suppresses.EXCEPTIONS)` cannot raise I/O related exceptions, but may still leak resources if the object is never closed). Unless guaranteed to be unnecessary by the `@NoIO` annotation, it is the user's responsibility to ensure that the object's `close()` method is eventually called before the object is deallocated or when an un-recoverable exception is thrown (i.e. the `close()` method is unnecessary if and only if the object is equal to `Sequence.EMPTY`, is an `ArraySequence`, or is a `CompoundSequence` which contains only `ArraySequence`s).

## The `FileSequence` Implementation
In order to increase the speed of random access to characters in `FileSequence` objects, files passed to their builder are first decoded (using the specified charset, or UTF-8 by default) and then re-encoded using a `FixedSizeCharset` in a new file located in the `<user.dir>/sequence-tmp/` directory. This directory and the files within are marked for deletion on exit, but no guarantee can be made. If the sequence is immutable and contains only characters between `\u0000` and `\u00FF`, inclusive (i.e. can be represented in one byte), then each character represents exactly one byte. Otherwise, each character is exactly two bytes, not accounting for surrogate pairs. `MutableFileSequence`s always use a two-byte/character format to guarantee that modification does not cause an issue.
