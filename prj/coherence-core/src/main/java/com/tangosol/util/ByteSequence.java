/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


/**
* Represents a sequence of bytes.
*
* @author cp 2010-06-06
* @since Coherence 3.7
*/
public interface ByteSequence
    {
    /**
    * Determine the number of bytes of data represented by this
    * ByteSequence.
    *
    * @return the number of bytes represented by this Node
    */
    public int length();

    /**
    * Determine the <tt>n</tt>-th byte of the byte sequence.
    *
    * @param of  the zero-based byte offset within the sequence of bytes
    *            <tt>(0 &lt;= of &lt; {@link #length()})</tt>
    *
    * @return the byte at the specified offset
    *
    * @throws IndexOutOfBoundsException  if
    *         <tt>(of &lt; 0 || of &gt;= {@link #length ()})</tt>
    */
    public byte byteAt(int of);

    /**
    * Returns a new <code>ByteSequence</code> that is a subsequence of this
    * sequence. The subsequence starts with the <code>byte</code> value at the
    * specified index and ends with the <code>byte</code> value at index
    * <tt>ofEnd - 1</tt>. The length (in <code>byte</code>s) of the returned
    * sequence is <tt>ofEnd - ofStart</tt>, so if <tt>ofStart == ofEnd</tt>
    * then an empty sequence is returned.
    *
    * @param   ofStart  the start index, inclusive
    * @param   ofEnd    the end index, exclusive
    *
    * @return  the specified subsequence
    *
    * @throws  IndexOutOfBoundsException
    *          if <tt>ofStart</tt> or <tt>ofEnd</tt> are negative,
    *          if <tt>ofEnd</tt> is greater than <tt>length()</tt>,
    *          or if <tt>ofStart</tt> is greater than <tt>ofEnd</tt>
    */
    public ByteSequence subSequence(int ofStart, int ofEnd);

    /**
    * Returns a Binary object that holds the contents of this ByteSequence.
    *
    * @return  the contents of this ByteSequence as a Binary object
    */
    public Binary toBinary();
    }
