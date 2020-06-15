/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.io.AbstractByteArrayReadBuffer;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.ReadBuffer;

import com.tangosol.io.WrapperInputStream;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.net.URL;
import java.nio.ByteBuffer;

import java.nio.channels.FileChannel;

import java.util.Arrays;

/**
* A thread-safe immutable binary object.
*
* @author cp  2002.01.25
*/
public final class Binary
        extends AbstractByteArrayReadBuffer
        implements Comparable, Externalizable, ExternalizableLite
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor for a binary object. Supports deserialization.
    */
    public Binary()
        {
        super(NO_BYTES, 0, 0);
        }

    /**
    * Construct a binary object from a byte array.
    *
    * @param ab  an array of bytes
    */
    public Binary(byte[] ab)
        {
        this(ab, 0, ab.length, true);
        }

    /**
    * Construct a binary object from a portion of a byte array.
    *
    * @param ab  an array of bytes
    * @param of  the offset into the byte array
    * @param cb  the number of bytes to extract
    */
    public Binary(byte[] ab, int of, int cb)
        {
        this(ab, of, cb, true);
        }

    /**
    * Construct a Binary object from a portion of a byte array. This
    * constructor allows internal methods to efficiently create Binary
    * objects without forcing a copy.
    *
    * @param ab     a byte array to construct this Binary object from
    * @param of     the offset into the byte array
    * @param cb     the number of bytes
    * @param fCopy  true to force a copy of any mutable data
    */
    private Binary(byte[] ab, int of, int cb, boolean fCopy)
        {
        super();

        if (cb == 0)
            {
            m_ab = NO_BYTES;
            }
        else if (fCopy)
            {
            byte[] abNew;
            try
                {
                abNew = new byte[cb];
                System.arraycopy(ab, of, abNew, 0, cb);
                }
            catch (Exception e)
                {
                azzert(ab != null, "byte array is null");
                azzert(of >= 0, "offset is negative");
                // note: of == ab.length iff cb == 0
                azzert(of <= ab.length, "offset > array length");
                azzert(cb >= 0, "length is negative");
                azzert(of + cb <= ab.length, "offset + length > array length");
                throw ensureRuntimeException(e);
                }
            m_ab = abNew;
            m_cb = cb;
            }
        else
            {
            m_ab = ab;
            m_of = of;
            m_cb = cb;
            }
        }

    /**
    * Construct a Binary object from a Binary object.
    *
    * @param that  another Binary object
    *
    * @see java.lang.String#String(String)
    */
    public Binary(Binary that)
        {
        super();

        // the purpose could be to shrink the buffer (see String); no reason
        // to reallocate though unless shrinkage is major (at least 25% and
        // at least 1024 bytes)
        int cbAlloc = that.m_ab.length;
        int cbUsed  = that.m_cb;

        if (cbAlloc - 1024 > cbUsed && (cbAlloc >>> 1) + (cbAlloc >>> 2) > cbUsed)
            {
            byte[] abNew = new byte[cbUsed];
            System.arraycopy(that.m_ab, that.m_of, abNew, 0, cbUsed);

            m_ab = abNew;
            m_cb = cbUsed;
            }
        else
            {
            this.m_ab    = that.m_ab;
            this.m_of    = that.m_of;
            this.m_cb    = that.m_cb;
            this.m_nHash = that.m_nHash;
            }
        }

    /**
    * Construct a Binary object from the contents of a ByteArrayOutputStream.
    *
    * @param stream  the ByteArrayOutputStream that holds the value that
    *                this Binary object will represent
    */
    public Binary(ByteArrayOutputStream stream)
        {
        super();

        // the toByteArray method of ByteArrayOutputStream is specified as
        // always creating a new byte array, thus maintaining the immutable
        // contract of this class
        byte[] ab = stream.toByteArray();

        // paranoia: only take it as immutable if it comes from the real
        // ByteArrayOutputStream
        if (stream.getClass() != ByteArrayOutputStream.class)
            {
            ab = ab.clone();
            }

        m_ab = ab;
        m_cb = ab.length;
        }

    /**
    * Construct a binary object from a DataInput interface.
    *
    * @param stream  the object implementing DataInput from which this
    *                Binary object will load its data
    *
    * @throws IOException  if an I/O error occurs reading the Binary
    */
    public Binary(DataInput stream)
            throws IOException
        {
        super();
        readExternal(stream);
        }

    /**
    * Construct a Binary object from a portion of another Binary object.
    * This is similar to the package-private String constructor.
    *
    * @param that  a Binary object to construct this Binary object from
    * @param of    the offset into the byte array
    * @param cb    the number of bytes to extract
    */
    Binary(Binary that, int of, int cb)
        {
        super(that.m_ab, that.m_of + of, cb);
        }

    /**
    * Construct a Binary object from a BinaryWriteBuffer.
    *
    * @param buf  a BinaryWriteBuffer to construct this Binary object from
    */
    Binary(BinaryWriteBuffer buf)
        {
        byte[] ab      = buf.getInternalByteArray();
        int    cbData  = buf.length();
        int    cbTotal = ab.length;
        int    cbWaste = cbTotal - cbData;

        // tolerate up to 12.5% waste
        if (cbWaste <= Math.max(16, cbTotal >>> 3))
            {
            m_ab = ab;
            }
        else
            {
            byte[] abNew = new byte[cbData];
            System.arraycopy(ab, 0, abNew, 0, cbData);
            m_ab = abNew;
            }
        m_cb = cbData;
        }


    // ----- String-like methods --------------------------------------------

    /**
    * Tests if two Binary regions are equal.
    *
    * @param ofThis  the starting offset of the subregion in this Binary
    *                object
    * @param that    the Binary object containing the subregion to compare to
    * @param ofThat  the starting offset of the subregion in the passed
    *                Binary object <code>that</code>
    * @param cb      the number of bytes to compare
    *
    * @return <code>true</code> if the specified subregion of this Binary
    *         object exactly matches the specified subregion of
    *         the <code>that</code> Binary object; <code>false</code>
    *         otherwise
    *
    * @exception java.lang.NullPointerException if <code>that</code> is
    *            <code>null</code>
    *
    * @see java.lang.String#regionMatches
    */
    public boolean regionMatches(int ofThis, Binary that, int ofThat, int cb)
        {
        if (   ofThis < 0 || ofThis > (long) this.m_cb - cb
            || ofThat < 0 || ofThat > (long) that.m_cb - cb)
            {
            return false;
            }

        return equals(this.m_ab, this.m_of + ofThis,
                      that.m_ab, that.m_of + ofThat, cb);
        }

    /**
    * Tests if this Binary object starts with the specified prefix beginning
    * a specified offset.
    *
    * @param bin     the prefix
    * @param ofFrom  the offset to begin looking in this Binary object
    *
    * @return <code>true</code> if the byte sequence represented by the
    *         <code>bin</code> argument is a prefix of the substring of
    *         this Binary object starting at offset <code>ofFrom</code>;
    *         <code>false</code> otherwise
    *
    * @exception java.lang.NullPointerException if <code>bin</code> is
    *            <code>null</code>.
    *
    * @see java.lang.String#startsWith
    */
    public boolean startsWith(Binary bin, int ofFrom)
        {
        return regionMatches(ofFrom, bin, 0, bin.m_cb);
        }

    /**
    * Tests if this Binary object starts with the specified prefix.
    *
    * @param bin  the prefix
    *
    * @return <code>true</code> if the byte sequence represented by the
    *         <code>bin</code> argument is a prefix of this Binary object;
    *         <code>false</code> otherwise
    *
    * @exception java.lang.NullPointerException if <code>bin</code> is
    *            <code>null</code>.
    *
    * @see java.lang.String#startsWith
    */
    public boolean startsWith(Binary bin)
        {
        return startsWith(bin, 0);
        }

    /**
    * Tests if this Binary object ends with the specified suffix.
    *
    * @param bin  the suffix
    *
    * @return <code>true</code> if the byte sequence represented by the
    *         <code>bin</code> argument is a suffix of this Binary object;
    *         <code>false</code> otherwise
    *
    * @exception java.lang.NullPointerException if <code>bin</code> is
    *            <code>null</code>.
    *
    * @see java.lang.String#endsWith
    */
    public boolean endsWith(Binary bin)
        {
        return startsWith(bin, this.m_cb - bin.m_cb);
        }

    /**
    * Returns the offset within this Binary object of the first occurrence of
    * the specified byte.
    * <p>
    * This method is identical in its functionality to the corresponding
    * method in String.
    *
    * @param b  the byte to search for
    *
    * @return the offset of the first occurrence of the specified byte in the
    *         byte sequence represented by this Binary object, or
    *         <code>-1</code> if the byte does not occur in the sequence
    *
    * @see java.lang.String#indexOf(int)
    */
    public int indexOf(byte b)
        {
        int ofThis   = m_of;
        int ofResult = memchr(m_ab, ofThis, m_cb, b);
        return ofResult < 0 ? ofResult : ofResult - ofThis;
        }

    /**
    * Returns the offset within this Binary object of the first occurrence of
    * the specified byte, starting the search at the specified offset.
    * <p>
    * This method is identical in its functionality to the corresponding
    * method in String.
    *
    * @param b       the byte to search for
    * @param ofFrom  the offset to search from
    *
    * @return the offset of the first occurrence of the specified byte in the
    *         byte sequence represented by this Binary object that is greater
    *         than or equal to <code>ofFrom</code>, or <code>-1</code> if the
    *         byte does not occur from that offset to the end of the sequence
    *
    * @see java.lang.String#indexOf(int, int)
    */
    public int indexOf(byte b, int ofFrom)
        {
        if (ofFrom < 0)
            {
            ofFrom = 0;
            }

        int ofThis   = m_of;
        int ofResult = memchr(m_ab, ofThis + ofFrom, m_cb - ofFrom, b);
        return ofResult < 0 ? ofResult : ofResult - ofThis;
        }

    /**
    * Returns the offset within this Binary object of the first occurrence of
    * the specified Binary.
    * <p>
    * This method is identical in its functionality to the corresponding
    * method in String.
    *
    * @param bin  the Binary to search for
    *
    * @return the offset of the first occurrence of the byte sequence
    *         represented by the specified Binary in the byte sequence
    *         represented by this Binary object, or <code>-1</code> if the
    *         byte sequence does not occur
    *
    * @see java.lang.String#indexOf(String)
    *
    * @since 3.5
    */
    public int indexOf(Binary bin)
        {
        final Binary that = bin;

        int ofThis   = this.m_of;
        int ofResult = memmem(this.m_ab, ofThis, this.m_cb,
                              that.m_ab, that.m_of, that.m_cb);
        return ofResult < 0 ? ofResult : ofResult - ofThis;
        }

    /**
    * Returns the offset within this Binary object of the first occurrence of
    * the specified Binary, starting the search at the specified offset.
    * <p>
    * This method is identical in its functionality to the corresponding
    * method in String. <b>Note that one side-effect of maintaining
    * compatibility with the String behavior is that zero-length Strings can
    * be found even when the from-index is out-of-bounds.</b> Given any
    * Binary value "<tt>value</tt>" and a zero-length binary value
    * "<tt>empty</tt>", the following will always hold true:
    * <pre>
    * int rnd = new Random().nextInt(Integer.MAX_VALUE - value.length());
    * assert value.indexOf(empty, value.length() + rnd) == value.length();
    * </pre>
    *
    * @param bin     the Binary to search for
    * @param ofFrom  the offset to search from
    *
    * @return the offset of the first occurrence of the byte sequence
    *         represented by the specified Binary in the byte sequence
    *         represented by this Binary object that is greater than or equal
    *         to <code>ofFrom</code>, or <code>-1</code> if the byte sequence
    *         does not occur from that offset to the end of this Binary
    *
    * @see java.lang.String#indexOf(String, int)
    *
    * @since 3.5
    */
    public int indexOf(Binary bin, int ofFrom)
        {
        final Binary that = bin;

        if (ofFrom < 0)
            {
            ofFrom = 0;
            }

        int ofThis   = this.m_of;
        int cbThis   = this.m_cb;
        int cbThat   = that.m_cb;
        int cbRemain = cbThis - ofFrom;
        if (cbRemain < 0)
            {
            return cbThat == 0 ? cbThis : -1;
            }

        int ofResult = memmem(this.m_ab, ofThis + ofFrom, cbRemain,
                              that.m_ab, that.m_of, cbThat);
        return ofResult < 0 ? ofResult : ofResult - ofThis;
        }

    /**
    * Returns the offset within this Binary object of the last occurrence of
    * the specified byte. The byte sequence of the Binary object is searched
    * in backwards order.
    * <p>
    * This method is identical in its functionality to the corresponding
    * method in String.
    *
    * @param b  the byte to search for
    *
    * @return the offset of the last occurrence of the specified byte in the
    *         byte sequence represented by this Binary object, or
    *         <code>-1</code> if the byte does not occur in the sequence
    *
    * @see java.lang.String#lastIndexOf(int)
    */
    public int lastIndexOf(byte b)
        {
        int ofThis   = m_of;
        int ofResult = memchr(m_ab, ofThis, m_cb, b, true);
        return ofResult < 0 ? ofResult : ofResult - ofThis;
        }

    /**
    * Returns the offset within this Binary object of the last occurrence of
    * the specified byte, starting the search at the specified offset and
    * searching backwards.
    * <p>
    * This method is identical in its functionality to the corresponding
    * method in String.
    *
    * @param b       the byte to search for
    * @param ofFrom  the offset to search backwards from (inclusive)
    *
    * @return the offset of the last occurrence of the specified byte in the
    *         byte sequence represented by this Binary object that is less
    *         than or equal to <code>ofFrom</code>, or <code>-1</code> if the
    *         byte does not occur from that offset to the start of the
    *         sequence
    *
    * @see java.lang.String#lastIndexOf(int, int)
    */
    public int lastIndexOf(byte b, int ofFrom)
        {
        int ofThis   = m_of;
        int ofResult = memchr(m_ab, ofThis, Math.min(m_cb, ofFrom + 1), b, true);
        return ofResult < 0 ? ofResult : ofResult - ofThis;
        }

    /**
    * Returns the offset within this Binary object of the last occurrence of
    * the specified Binary.
    * <p>
    * This method is identical in its functionality to the corresponding
    * method in String.
    *
    * @param bin  the Binary to search for
    *
    * @return the offset of the last occurrence of the byte sequence
    *         represented by the specified Binary in the byte sequence
    *         represented by this Binary object, or <code>-1</code> if the
    *         byte sequence does not occur
    *
    * @see java.lang.String#lastIndexOf(String)
    *
    * @since 3.5
    */
    public int lastIndexOf(Binary bin)
        {
        final Binary that = bin;

        int ofThis   = this.m_of;
        int ofResult = memmem(this.m_ab, ofThis, this.m_cb,
                              that.m_ab, that.m_of, that.m_cb, true);
        return ofResult < 0 ? ofResult : ofResult - ofThis;
        }

    /**
    * Returns the offset within this Binary object of the last occurrence of
    * the specified Binary, starting the search at the specified offset.
    * <p>
    * This method is identical in its functionality to the corresponding
    * method in String.
    *
    * @param bin     the Binary to search for
    * @param ofFrom  the offset to search from
    *
    * @return the offset of the last occurrence of the byte sequence
    *         represented by the specified Binary in the byte sequence
    *         represented by this Binary object that is less than or equal
    *         to <code>ofFrom</code>, or <code>-1</code> if the byte sequence
    *         does not occur from that offset to the beginning of this Binary
    *
    * @see java.lang.String#lastIndexOf(String, int)
    *
    * @since 3.5
    */
    public int lastIndexOf(Binary bin, int ofFrom)
        {
        final Binary that = bin;

        int ofThis   = this.m_of;
        int cbThat   = that.m_cb;
        int ofResult = memmem(this.m_ab, ofThis, Math.min(this.m_cb, ofFrom + cbThat),
                              that.m_ab, that.m_of, cbThat, true);
        return ofResult < 0 ? ofResult : ofResult - ofThis;
        }

    /**
    * Replace all occurrences of one specified byte with another in this
    * Binary object. (This method does not alter the state of this Binary;
    * if replacement must occur, it is accomplished by creating a new Binary
    * instance.)
    *
    * @param bOld  the byte value to replace
    * @param bNew  the byte value to replace <tt>bOld</tt> with
    *
    * @return a Binary whose value is the same as this Binary's value, except
    *         that all occurrences of <tt>bOld</tt> will have been replaced
    *         with <tt>bNew</tt>
    */
    public Binary replace(byte bOld, byte bNew)
        {
        // verify that the replace is actually changing from one byte to a
        // different byte (if not, there is nothing to replace)
        if (bOld == bNew)
            {
            return this;
            }

        // verify that there is at least one instance of the byte to replace
        byte[] abThis = m_ab;
        int    ofThis = m_of;
        int    cbThis = m_cb;
        int    ofByte = memchr(abThis, ofThis, cbThis, bOld);
        if (ofByte < 0)
            {
            return this;
            }

        // allocate new byte[] to hold the result
        byte[] abResult = clone(abThis, ofThis, cbThis);

        // adjust from the region starting at "ofThis" to the region starting
        // at zero (in the new byte array)
        ofByte -= ofThis;

        // replace all occurrences
        do
            {
            abResult[ofByte++] = bNew;
            ofByte = memchr(abResult, ofByte, cbThis - ofByte, bOld);
            }
        while (ofByte >= 0);

        return new Binary(abResult, 0, cbThis, false);
        }

    /**
    * Replace all occurrences of one specified Binary with another in this
    * Binary object. (This method does not alter the state of this Binary;
    * if replacement must occur, it is accomplished by creating a new Binary
    * instance.)
    *
    * @param binOld  the Binary value to replace
    * @param binNew  the Binary value to replace <tt>binOld</tt> with
    *
    * @return a Binary whose value is the same as this Binary's value, except
    *         that all occurrences of <tt>binOld</tt> will have been replaced
    *         with <tt>binNew</tt>
    */
    public Binary replace(Binary binOld, Binary binNew)
        {
        // replacing nothing with something is considered a no-op
        int cbOld = binOld.m_cb;
        if (cbOld == 0)
            {
            return this;
            }

        // if both the old and new are 1 byte, then use the optimized 1-byte
        // replace() method
        int cbNew = binNew.m_cb;
        if (cbOld == 1 && cbNew == 1)
            {
            return replace(binOld.byteAt(0), binNew.byteAt(0));
            }

        // verify that there is at least one instance of the value that is
        // being replaced
        byte[] abThis = m_ab;
        int    ofThis = m_of;
        int    cbThis = m_cb;
        byte[] abOld  = binOld.m_ab;
        int    ofOld  = binOld.m_of;
        byte[] abNew  = binNew.m_ab;
        int    ofNew  = binNew.m_of;
        int    ofNext = memmem(abThis, ofThis, cbThis, abOld, ofOld, cbOld);
        if (ofNext < 0)
            {
            return this;
            }

        // easy optimization if the before and after are the same length
        if (cbOld == cbNew)
            {
            // allocate new byte[] to hold the result
            byte[] abResult = clone(abThis, ofThis, cbThis);

            // adjust from the region starting at "ofThis" to the region starting
            // at zero (in the new byte array)
            ofNext -= ofThis;

            // replace all occurrences
            do
                {
                memcpy(abNew, ofNew, abResult, ofNext, cbNew);
                ofNext += cbNew;
                ofNext  = memmem(abResult, ofNext, cbThis - ofNext, abOld, ofOld, cbOld);
                }
            while (ofNext >= 0);

            return new Binary(abResult, 0, cbThis, false);
            }

        // presize it as if only one region were changing (it's as
        // reasonable a guess as any)
        BinaryWriteBuffer buf = new BinaryWriteBuffer(cbThis + cbNew - cbOld);
        BinaryWriteBuffer.BufferOutput out = buf.getBufferOutput();
        try
            {
            int ofPrev = ofThis;
            int cbRemain;
            do
                {
                // write the portion from the previous match up to the
                // current match
                int cbCopy = ofNext - ofPrev;
                if (cbCopy > 0)
                    {
                    out.write(abThis, ofPrev, cbCopy);
                    }

                if (cbNew > 0)
                    {
                    out.write(abNew, ofNew, cbNew);
                    }

                ofPrev   = ofNext + cbOld;
                cbRemain = cbThis - (ofPrev - ofThis);
                ofNext   = memmem(abThis, ofPrev, cbRemain, abOld, ofOld, cbOld);
                }
            while (ofNext >= 0);

            // copy any trailing bytes
            if (cbRemain > 0)
                {
                out.write(abThis, ofPrev, cbRemain);
                }
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        return buf.toBinary();
        }

    /**
    * Replace a region within this Binary with another Binary.
    * <p>
    * This method does not alter the state of this Binary; it creates a new
    * Binary instance instead.
    *
    * @param of      the offset of the range to replace within this Binary
    * @param cb      the length of the range to replace in bytes
    * @param binNew  the Binary value to replace the range with
    *
    * @return a Binary whose value is the same as this Binary's value,
    *         except that the specified range will have been replaced
    *         with <tt>binNew</tt>
    *
    * @since 3.5
    */
    public Binary replaceRegion(int of, int cb, Binary binNew)
        {
        byte[] abThis = m_ab;
        int    ofThis = m_of;
        int    cbThis = m_cb;
        byte[] abNew  = binNew.m_ab;
        int    ofNew  = binNew.m_of;
        int    cbNew  = binNew.m_cb;

        // optimization if the range and the new value are the same length
        if (cb == cbNew)
            {
            // allocate new byte[] to hold the result
            byte[] abResult = clone(abThis, ofThis, cbThis);

            // replace region
            memcpy(abNew, ofNew, abResult, of, cb);
            return new Binary(abResult, 0, cbThis, false);
            }

        BinaryWriteBuffer buf = new BinaryWriteBuffer(cbThis + cbNew - cb);
        BinaryWriteBuffer.BufferOutput out = buf.getBufferOutput();
        try
            {
            this.writeTo(out, 0, of);
            binNew.writeTo(out);
            final int ofRemain = of + cb;
            this.writeTo(out, ofRemain, length() - ofRemain);
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        return buf.toBinary();
        }

    /**
    * Concatenate the passed Binary onto this Binary.
    *
    * @param bin  the Binary to concatenate to this Binary
    *
    * @return a Binary containing the byte sequence of this Binary object
    *         followed immediately by the byte sequence of the passed Binary
    *         object
    *
    * @since 3.5
    */
    public Binary concat(Binary bin)
        {
        final Binary that = bin;

        int cbThis = this.m_cb;
        if (cbThis == 0)
            {
            return that;
            }

        int cbThat = that.m_cb;
        if (cbThat == 0)
            {
            return this;
            }

        int    cbNew = cbThis + cbThat;
        byte[] abNew = new byte[cbNew];
        System.arraycopy(this.m_ab, this.m_of, abNew, 0, cbThis);
        System.arraycopy(that.m_ab, that.m_of, abNew, cbThis, cbThat);

        return new Binary(abNew, 0, cbNew, false);
        }

    /**
    * Reverse this Binary object's byte sequence such that the byte at offset
    * zero of this sequence occurs at offset <tt>length()-1</tt> in the
    * resulting sequence, the byte at offset one of this sequence occurs at
    * offset <tt>length()-2</tt> in the resulting sequence, and so on. The
    * resulting Binary object will have the same length as this Binary
    * object.
    *
    * @return a Binary whose byte sequence contains the same byte values as
    *         this Binary, but in reverse sequence
    *
    * @since 3.5
    */
    public Binary reverse()
        {
        int cb = m_cb;
        if (cb < 2)
            {
            return this;
            }

        byte[] abOld = m_ab;
        byte[] abNew = new byte[cb];

        int    ofOld = m_of;
        int    ofNew = cb - 1;

        while (ofNew >= 0)
            {
            abNew[ofNew--] = abOld[ofOld++];
            }

        return new Binary(abNew, 0, cb, false);
        }

    /**
     * Split a Binary into an array of Binaries each of which does not exceed
     * the specified size.
     *
     * @param nSize  the size of result Binaries
     *
     * @return an array of Binaries
     *
     * @since 12.2.1.4
     */
    public Binary[] split(int nSize)
        {
        int      nLen      = m_cb / nSize;
        int      remainder = m_cb % nSize;
        Binary[] aBinaries = new Binary[remainder == 0 ? nLen : nLen + 1];

        for (int i = 0; i < nLen; i++)
            {
            aBinaries[i] = new Binary(m_ab, m_of + nSize * i, nSize);
            }
        if (remainder > 0)
            {
            aBinaries[nLen] = new Binary(m_ab, m_of + nSize * nLen, remainder);
            }

        return aBinaries;
        }


    // ----- ReadBuffer methods ---------------------------------------------

    /**
    * {@inheritDoc}
    */
    public Binary toBinary()
        {
        return this;
        }

    /**
    * {@inheritDoc}
    */
    public Binary toBinary(int of, int cb)
        {
        int cbBuf = m_cb;
        if (of == 0 && cb == cbBuf)
            {
            return this;
            }

        // validate parameters
        if (of < 0 || cb < 0 || of + cb > cbBuf)
            {
            throw new IndexOutOfBoundsException("of=" + of + ", cb=" + cb
                    + ", length()=" + cbBuf);
            }

        return cb == 0 ? NO_BINARY
                       : new Binary(this, of, cb);
        }


    // ----- AbstractByteArrayReadBuffer methods ----------------------------

    /**
    * {@inheritDoc}
    */
    protected ReadBuffer instantiateReadBuffer(int of, int cb)
        {
        return toBinary(of, cb);
        }

    /**
    * {@inheritDoc}
    */
    protected boolean isByteArrayPrivate()
        {
        return true;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Create a clone of this Binary object.
    *
    * @return a Binary object with the same contents as this Binary object
    */
    public Object clone()
        {
        // it is self-evident that cloning an immutable object is ridiculous
        return this;
        }

    /**
    * Provide a human-readable representation of the Binary object.
    *
    * @return a String whose contents represent the value of this Binary object
    */
    public String toString()
        {
        // "Binary(length=4, value=0x01F03DA7)"
        int       cb     = m_cb;
        final int MAX    = 256;
        boolean   fTrunc = cb > MAX;
        return "Binary(length=" + cb + ", value="
            + toHexEscape(m_ab, m_of, fTrunc ? MAX : cb)
            + (fTrunc ? "...)" : ")");
        }

    /**
    * Returns a hash code value for the object. This method is supported
    * for the benefit of hashed data structures.
    * <p>
    * The Binary object uses a CRC32 algorithm to determine the hash code.
    *
    * @return  a hash code value for this object
    *
    * @see Base#toCrc(byte[], int, int)
    */
    public int hashCode()
        {
        int nHash = m_nHash;

        if (nHash == 0)
            {
            // cache the CRC32 result
            nHash = toCrc(m_ab, m_of, m_cb);
            if (nHash == 0)
                {
                // to allow for caching of the hashcode
                nHash = 17;
                }
            m_nHash = nHash;
            }

        return nHash;
        }

    /**
    * Compares this Binary object with another object for equality.
    *
    * @param  o  an object reference or null
    *
    * @return  true iff the passed object reference is a Binary object
    *          representing the same exact sequence of byte values
    */
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }

        if (o instanceof Binary)
            {
            Binary that = (Binary) o;

            // compare length (a quick way to disprove equality)
            int cbThis = this.m_cb;
            int cbThat = that.m_cb;
            if (cbThis == cbThat)
                {
                // 0-length binary values are identical
                if (cbThis == 0)
                    {
                    return true;
                    }

                // compare hash-code (another quick way to disprove equality)
                int nThisHash = this.m_nHash;
                int nThatHash = that.m_nHash;
                if (nThisHash == 0 || nThatHash == 0 || nThisHash == nThatHash)
                    {
                    // COH-2279 - Use Arrays.equals if the entire buffers should
                    // be equal. Intentionally only using cbThis since the equality
                    // is checked previously.
                    if (this.m_of == 0 && that.m_of == 0
                        && this.m_ab.length == cbThis
                        && that.m_ab.length == cbThis)
                        {
                        return Arrays.equals(this.m_ab, that.m_ab);
                        }
                    else
                        {
                        // brute force byte-by-byte comparison
                        return equals(this.m_ab, this.m_of,
                              that.m_ab, that.m_of, cbThat);
                        }
                    }
                }

            return false;
            }
        else
            {
            return super.equals(o);
            }
        }


    // ----- partitioning helpers -------------------------------------------

    /**
    * Calculate the partition ID to which the specified Binary should be
    * naturally assigned. This calculation should not be applied to Binary
    * objects {@link ExternalizableHelper#decorateBinary decorated} with
    * artificially assigned partitions.
    * <p>
    * The resulting partition ID will be in the range <tt>[0..cPartitions)</tt>.
    * <p>
    * <b>Note:</b> if the specified partition count is zero, this method will
    * return a hash code fo the binary, thus allowing clients that don't have a
    * concept of partitioning or knowledge of the partition count to defer the
    * partition id calculation until the partition count is known.
    *
    * @param cPartitions  the partition count
    *
    * @return the partition that the this Binary is naturally assigned to
    *         or a natural hash code if the partition count is not specified
    */
    public int calculateNaturalPartition(int cPartitions)
        {
        long lHash = ((long) hashCode()) & 0xFFFFFFFFL;
        return cPartitions == 0 ? (int) lHash : (int) (lHash % (long) cPartitions);
        }


    // ----- Comparable interface -------------------------------------------

    /**
    * Compares this object with the specified object for order.  Returns a
    * negative integer, zero, or a positive integer as this object is less
    * than, equal to, or greater than the specified object.
    *
    * @param   o the Object to be compared.
    *
    * @return  a negative integer, zero, or a positive integer as this object
    *          is less than, equal to, or greater than the specified object
    *
    * @throws ClassCastException if the specified object's type prevents it
    *         from being compared to this Object
    * @throws NullPointerException if the specified object is
    *         <code>null</code>
    */
    public int compareTo(Object o)
        {
        Binary that = (Binary) o;       // ClassCastException
        return memcmp(this.m_ab, this.m_of, this.m_cb,
                      that.m_ab, that.m_of, that.m_cb);
        }


    // ----- Externalizable interface ---------------------------------------

    /**
    * The object implements the readExternal method to restore its
    * contents by calling the methods of DataInput for primitive
    * types and readObject for objects, strings and arrays.  The
    * readExternal method must read the values in the same sequence
    * and with the same types as were written by writeExternal.
    *
    * @param in the stream to read data from in order to restore the object
    *
    * @exception IOException  if an I/O exception occurs
    */
    public void readExternal(ObjectInput in)
            throws IOException
        {
        readExternal((DataInput) in);
        }

    /**
    * The object implements the writeExternal method to save its contents
    * by calling the methods of DataOutput for its primitive values or
    * calling the writeObject method of ObjectOutput for objects, strings,
    * and arrays.
    *
    * @param out the stream to write the object to
    *
    * @exception IOException if an I/O exception occurs
    */
    public void writeExternal(ObjectOutput out)
            throws IOException
        {
        writeExternal((DataOutput) out);
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        if (m_cb > 0)
            {
            throw new NotActiveException();
            }

        int    cb = in.readInt();
        byte[] ab = new byte[cb];
        in.readFully(ab);

        m_ab = ab;
        m_cb = cb;
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        out.writeInt(m_cb);
        out.write(m_ab, m_of, m_cb);
        }

    /**
    * Write the contents of the specified ReadBuffer to the specified DataOutput
    * stream in a format that can be restored as a Binary via {@link #readExternal}
    *
    * @param out  the DataOutput stream to write to
    * @param buf  the ReadBuffer to write the contents of
    *
    * @throws IOException if an I/O exception occurs
    */
    public static void writeExternal(DataOutput out, ReadBuffer buf)
            throws IOException
        {
        out.writeInt(buf.length());
        buf.writeTo(out);
        }


    // ----- misc i/o optimizations -----------------------------------------

    /**
    * Get an InputStream to read the Binary object's contents from.
    *
    * @return an InputStream backed by this Binary object
    */
    public InputStream getInputStream()
        {
        return (InputStream) getBufferInput();
        }

    /**
    * Read a Binary of the specified length at the specified offset from the
    * specified FileChannel.
    *
    * @param channel  the FileChannel to read from
    * @param of       the offset within the FileChannel to read from
    * @param cb       the number of bytes to read from the FileChannel, which
    *                 will be the length of the Binary
    * @param ab2      reserved; pass in null
    *
    * @return a Binary containing the specified sequence of bytes from the
    *         channel
    *
    * @throws IOException  if an I/O exception or unexpected EOF occurs
    */
    public static Binary readBinary(FileChannel channel, long of, int cb, byte[] ab2)
            throws IOException
        {
        if (cb == 0)
            {
            return NO_BINARY;
            }

        byte[]     ab     = new byte[cb];
        ByteBuffer buf    = ByteBuffer.wrap(ab);
        int        cbRead = 0;
        do
            {
            int cbChunk = channel.read(buf, of + cbRead);
            if (cbChunk < 0)
                {
                throw new EOFException("succeeded in only reading " + cbRead
                        + " bytes out of a requested " + cb + " bytes at offset "
                        + of + " from channel: " + channel.toString());
                }
            cbRead += cbChunk;
            }
        while (cbRead < cb);
        assert cb == cbRead;

        // decrypt bytes
        if (ab2 != null && ab2.length == 0x100)
            {
            for (int ofCur = 0, ofOv = (int) (of & 0xFF); ofCur < cb; ++ofCur, ++ofOv)
                {
                ab[ofCur] = (byte) (ab[ofCur] ^ ab2[ofOv & 0xFF]);
                }
            }

        return new Binary(ab, 0, cb, false);
        }

    /**
     * Read a Binary from the specified InputStream.
     *
     * @param in the InputStream to read from
     *
     * @return a Binary containing all the bytes from the specified InputStream
     *
     * @throws IOException if an I/O exception or unexpected EOF occurs
     */
    public static Binary readBinary(InputStream in)
            throws IOException
        {
        BinaryWriteBuffer buf = new BinaryWriteBuffer(in.available());

        try (WrapperInputStream stream = new WrapperInputStream(in))
            {
            buf.getBufferOutput().writeStream(stream);
            return buf.toBinary();
            }
        }

    /**
     * Read a Binary from the specified InputStream.
     *
     * @param in  the InputStream to read from
     * @param cb  the exact number of bytes to read from the stream
     *
     * @return a Binary containing {@code cb} bytes
     *
     * @throws IOException if an I/O exception or unexpected EOF occurs
     */
    public static Binary readBinary(InputStream in, int cb)
            throws IOException
        {
        BinaryWriteBuffer buf = new BinaryWriteBuffer(cb);

        try (WrapperInputStream stream = new WrapperInputStream(in))
            {
            buf.getBufferOutput().writeStream(stream, cb);
            return buf.toBinary();
            }
        }

    /**
    * Read a Binary from the specified File.
    *
    * @param file  the File to read from
    *
    * @return a Binary containing all the bytes from the specified file
    *
    * @throws IOException  if an I/O exception or unexpected EOF occurs
    */
    public static Binary readBinary(File file)
        throws IOException
        {
        return readBinary(new FileInputStream(file));
        }

    /**
    * Read a Binary from the specified URL.
    *
    * @param url  the URL to read from
    *
    * @return a Binary containing all the bytes from the specified URL
    *
    * @throws IOException  if an I/O exception or unexpected EOF occurs
    */
    public static Binary readBinary(URL url)
        throws IOException
        {
        byte[] ab = read(url);
        return new Binary(ab, 0, ab.length, false);
        }

    /**
    * Read a Binary from the specified DataInput.
    *
    * @param input  the DataInput to read from
    *
    * @return a Binary containing all the bytes from the specified DataInput
    *
    * @throws IOException  if an I/O exception or unexpected EOF occurs
    */
    public static Binary readBinary(DataInput input)
        throws IOException
        {
        byte[] ab = read(input);
        return new Binary(ab, 0, ab.length, false);
        }

    /**
    * Read a Binary from the specified DataInputStream.
    *
    * @param in  the DataInputStream to read from
    *
    * @return a Binary containing all the bytes from the specified DataInputStream
    *
    * @throws IOException  if an I/O exception or unexpected EOF occurs
    */
    public static Binary readBinary(DataInputStream in)
        throws IOException
        {
        // this method resolves the ambiguity between the readBinary(InputStream)
        // and readBinary(DataInput) methods for DataInputStreams and its derivatives
        return readBinary((InputStream) in);
        }


    // ----- C Runtime Library functions ------------------------------------

    /**
    * Find the specified byte (a "needle") in the specified binary region
    * ("the haystack").
    *
    * @param abHaystack  the byte array containing the binary region to
    *                    search within (the "haystack")
    * @param ofHaystack  the offset of the binary region within
    *                    <tt>abHaystack</tt>
    * @param cbHaystack  the size in bytes of the binary region within
    *                    <tt>abHaystack</tt>
    * @param bNeedle     the byte to search for (a "needle")
    *
    * @return the offset within the binary region (the "haystack") at which
    *         the specified byte (the "needle") was found, or <tt>-1</tt> if
    *         the specified byte does not occur within the binary region
    *
    * @since 3.5
    */
    public static int memchr(byte[] abHaystack, int ofHaystack, int cbHaystack,
                             byte bNeedle)
        {
        return memchr(abHaystack, ofHaystack, cbHaystack, bNeedle, false);
        }

    /**
    * Find the specified byte (a "needle") in the specified binary region
    * ("the haystack").
    *
    * @param abHaystack  the byte array containing the binary region to
    *                    search within (the "haystack")
    * @param ofHaystack  the offset of the binary region within
    *                    <tt>abHaystack</tt>
    * @param cbHaystack  the size in bytes of the binary region within
    *                    <tt>abHaystack</tt>
    * @param bNeedle     the byte to search for (a "needle")
    * @param fBackwards  pass false to find the first occurrence, or true to
    *                    find the last occurrence
    *
    * @return the offset within the binary region (the "haystack") at which
    *         the specified byte (the "needle") was found, or <tt>-1</tt> if
    *         the specified byte does not occur within the binary region
    *
    * @since 3.5
    */
    public static int memchr(byte[] abHaystack, int ofHaystack, int cbHaystack,
                             byte bNeedle, boolean fBackwards)
        {
        try
            {
            if (fBackwards)
                {
                for (int ofStop = ofHaystack, of = ofStop + cbHaystack - 1; of >= ofStop; --of)
                    {
                    if (abHaystack[of] == bNeedle)
                        {
                        return of;
                        }
                    }
                }
            else
                {
                for (int of = ofHaystack, ofStop = of + cbHaystack; of < ofStop; ++of)
                    {
                    if (abHaystack[of] == bNeedle)
                        {
                        return of;
                        }
                    }
                }
            return -1;
            }
        catch (RuntimeException e)
            {
            throw new WrapperException(e, "abHaystack=" + toString(abHaystack)
                    + ", ofHaystack=" + ofHaystack + ", cbHaystack=" + cbHaystack);
            }
        }

    /**
    * Find the second binary region (a "needle") in the first binary region
    * ("the haystack").
    *
    * @param abHaystack  the byte array containing the binary region to
    *                    search within (the "haystack")
    * @param ofHaystack  the offset of the binary region within
    *                    <tt>abHaystack</tt>
    * @param cbHaystack  the size in bytes of the binary region within
    *                    <tt>abHaystack</tt>
    * @param abNeedle    the byte array containing the binary region to
    *                    search for (a "needle")
    * @param ofNeedle    the offset of the binary region within
    *                    <tt>abNeedle</tt>
    * @param cbNeedle    the size in bytes of the binary region within
    *                    <tt>abNeedle</tt>
    *
    * @return the offset within the first binary region (the "haystack") at
    *         which the second binary region (the "needle") was found, or
    *         <tt>-1</tt> if the second binary region does not occur within
    *         the first
    *
    * @since 3.5
    */
    public static int memmem(byte[] abHaystack, int ofHaystack, int cbHaystack,
                             byte[] abNeedle, int ofNeedle, int cbNeedle)
        {
        return memmem(abHaystack, ofHaystack, cbHaystack,
                      abNeedle,   ofNeedle,   cbNeedle,   false);
        }

    /**
    * Find the second binary region (a "needle") in the first binary region
    * ("the haystack").
    *
    * @param abHaystack  the byte array containing the binary region to
    *                    search within (the "haystack")
    * @param ofHaystack  the offset of the binary region within
    *                    <tt>abHaystack</tt>
    * @param cbHaystack  the size in bytes of the binary region within
    *                    <tt>abHaystack</tt>
    * @param abNeedle    the byte array containing the binary region to
    *                    search for (a "needle")
    * @param ofNeedle    the offset of the binary region within
    *                    <tt>abNeedle</tt>
    * @param cbNeedle    the size in bytes of the binary region within
    *                    <tt>abNeedle</tt>
    * @param fBackwards  pass false to find the first occurrence, or true to
    *                    find the last occurrence
    *
    * @return the offset within the first binary region (the "haystack") at
    *         which the second binary region (the "needle") was found, or
    *         <tt>-1</tt> if the second binary region does not occur within
    *         the first
    *
    * @since 3.5
    */
    public static int memmem(byte[] abHaystack, int ofHaystack, int cbHaystack,
                             byte[] abNeedle, int ofNeedle, int cbNeedle,
                             boolean fBackwards)
        {
        try
            {
            // handle the various edge conditions
            if (cbNeedle >= cbHaystack)
                {
                // if the needle is larger than the haystack, then it cannot
                // be found within the haystack, and if it is the same size,
                // then the needle has to be identical to the haystack
                return (cbNeedle == cbHaystack && equals(abHaystack, ofHaystack,
                                                         abNeedle, ofNeedle, cbNeedle))
                        ? ofHaystack
                        : -1;
                }
            else if (cbNeedle <= 1)
                {
                // the only legal values for cbNeedle at this point are zero
                // and one; if zero, then an empty region matches immediately
                // and the result is thus at the very start (or end) of the
                // haystack, and if one, then switch to memchr() since it is
                // more efficient for searching for a single byte; the
                // multiplication just asserts that the cbNeedle value was
                // not less than zero
                return cbNeedle == 0
                        ? ofHaystack + (fBackwards ? cbHaystack : 0)
                        : memchr(abHaystack, ofHaystack, cbHaystack,
                                 abNeedle[ofNeedle * cbNeedle], fBackwards);
                }

            int bNeedle = abNeedle[ofNeedle];
            if (fBackwards)
                {
                for (int ofStop = ofHaystack, of = ofStop + cbHaystack - cbNeedle;
                        of >= ofStop; --of)
                    {
                    if (abHaystack[of] == bNeedle &&
                            equals(abHaystack, of+1, abNeedle, ofNeedle+1, cbNeedle-1))
                        {
                        return of;
                        }
                    }
                }
            else
                {
                for (int of = ofHaystack, ofStop = of + cbHaystack - cbNeedle;
                        of <= ofStop; ++of)
                    {
                    if (abHaystack[of] == bNeedle &&
                            equals(abHaystack, of+1, abNeedle, ofNeedle+1, cbNeedle-1))
                        {
                        return of;
                        }
                    }
                }
            return -1;
            }
        catch (RuntimeException e)
            {
            throw new WrapperException(e, "abHaystack=" + toString(abHaystack)
                    + ", ofHaystack=" + ofHaystack + ", cbHaystack=" + cbHaystack
                    + ", abNeedle=" + toString(abNeedle)
                    + ", ofNeedle=" + ofNeedle + ", cbNeedle=" + cbNeedle);
            }
        }

    /**
    * Compare two binary regions.
    *
    * @param ab1  the byte array containing the first binary region to
    *             compare
    * @param of1  the offset of the binary region within <tt>ab1</tt>
    * @param cb1  the size in bytes of the binary region within <tt>ab1</tt>
    * @param ab2  the byte array containing the second binary region to
    *             compare
    * @param of2  the offset of the binary region within <tt>ab2</tt>
    * @param cb2  the size in bytes of the binary region within <tt>ab2</tt>
    *
    * @return a value less than zero, zero or greater than zero if the first
    *         binary region is "less than," "equal to" or "greater than" the
    *         second binary region
    *
    * @since 3.5
    */
    public static int memcmp(byte[] ab1, int of1, int cb1, byte[] ab2, int of2, int cb2)
        {
        try
            {
            for (int i = 0, c = Math.min(cb1, cb2); i < c; ++i)
                {
                if (ab1[of1 + i] != ab2[of2 + i])
                    {
                    // byte is implemented in Java as a signed value, but
                    // the expected result of comparison is as if the bytes
                    // were unsigned
                    return (ab1[of1 + i] & 0xFF) - (ab2[of2 + i] & 0xFF);
                    }
                }
            return cb1 - cb2;
            }
        catch (RuntimeException e)
            {
            throw new WrapperException(e, "ab1=" + toString(ab1)
                    + ", of1=" + of1 + ", cb1=" + cb1 + ", ab2=" + toString(ab2)
                    + ", of2=" + of2 + ", cb2=" + cb2);
            }
        }

    /**
    * Copy binary data from one binary region to another. This is safe for
    * copying a region from an array to itself, even if the regions are
    * overlapping; as such, it is equivalent to both memmov() and memcpy().
    * The implementation uses the arraycopy() method of the System class;
    * the only difference between this method and the raw arraycopy() is that
    * this method is typed and decorates any exception with debugging
    * information about the arguments that caused the exception.
    *
    * @param abSrc   the byte array containing the binary region to copy from
    * @param ofSrc   the offset of the binary region within <tt>abSrc</tt>
    * @param abDest  the byte array containing the binary region to copy to
    * @param ofDest  the offset of the binary region within <tt>abDest</tt>
    * @param cbCopy  the size in bytes of the binary region to copy
    *
    * @since 3.5
    */
    public static void memcpy(byte[] abSrc, int ofSrc, byte[] abDest, int ofDest, int cbCopy)
        {
        try
            {
            System.arraycopy(abSrc, ofSrc, abDest, ofDest, cbCopy);
            }
        catch (RuntimeException e)
            {
            throw new WrapperException(e, "abSrc=" + toString(abSrc)
                    + ", ofSrc=" + ofSrc + ", abDest=" + toString(abDest)
                    + ", ofDest=" + ofDest + ", cbCopy=" + cbCopy);
            }
        }

    /**
    * Compare two binary regions, testing for equality.
    *
    * @param ab1  the byte array containing the first binary region to
    *             compare
    * @param of1  the offset of the binary region within <tt>ab1</tt>
    * @param ab2  the byte array containing the second binary region to
    *             compare
    * @param of2  the offset of the binary region within <tt>ab2</tt>
    * @param cb   the size of the binary regions, which is the number of
    *             bytes to compare
    *
    * @return true iff the two specified binary regions are identical
    *
    * @since 3.5
    */
    public static boolean equals(byte[] ab1, int of1, byte[] ab2, int of2, int cb)
        {
        try
            {
            while (--cb >= 0)
                {
                if (ab1[of1++] != ab2[of2++])
                    {
                    return false;
                    }
                }
            return true;
            }
        catch (RuntimeException e)
            {
            throw new WrapperException(e, "ab1=" + toString(ab1)
                    + ", of1=" + of1 + ", ab2=" + toString(ab2)
                    + ", of2=" + of2 + ", cb=" + cb);
            }
        }

    /**
    * Create a clone of the specified binary region.
    *
    * @param ab  the byte array containing the binary region to copy from
    * @param of  the offset of the binary region within <tt>ab</tt>
    * @param cb  the size in bytes of the binary region to copy
    *
    * @return a copy of the specified binary region
    *
    * @since 3.5
    */
    public static byte[] clone(byte[] ab, int of, int cb)
        {
        try
            {
            byte[] abNew = new byte[cb];
            System.arraycopy(ab, of, abNew, 0, cb);
            return abNew;
            }
        catch (RuntimeException e)
            {
            throw new WrapperException(e, "ab=" + toString(ab)
                    + ", of=" + of + ", cb=" + cb);
            }
        }

    /**
    * For debugging purposes, convert the passed byte array into a string
    * that contains the information regarding whether the reference is null,
    * and if it is not null, what the length of the byte array is.
    *
    * @param ab  a byte array; may be null
    *
    * @return a String; never null
    *
    * @since 3.5
    */
    public static String toString(byte[] ab)
        {
        return ab == null ? "null" : ("byte[" + ab.length + "]");
        }

    /**
     * Join an array of Binaries into a single Binary.
     *
     * @param aBinaries  an array of Binaries to join
     *
     * @since 12.2.1.4
     */
    public static Binary join(Binary[] aBinaries)
        {
        int cbNew = 0;
        for (int i = 0; i < aBinaries.length; i++)
            {
            cbNew += aBinaries[i].m_cb;
            }

        byte[] abNew  = new byte[cbNew];
        int    offset = 0;
        for (int i = 0; i < aBinaries.length; i++)
            {
            System.arraycopy(aBinaries[i].m_ab, aBinaries[i].m_of, abNew, offset, aBinaries[i].m_cb);
            offset += aBinaries[i].m_cb;
            }

        return new Binary(abNew, 0, cbNew, false);
        }


    // ----- inner class: Unsafe --------------------------------------------

    static void registerUnsafe(com.tangosol.util.Unsafe unsafe)
        {
        /**
         * Method is intentionally not javadoc'd.
         *
         * Register an instance of the Binary accessor with the specified {@link
         * Unsafe} instance.
         * <p>
         * This "double-dispatch" pattern is used in order to centralize the access
         * checks to all "unsafe" utilities.  See
         *
         * @param unsafe
         */

        unsafe.register(Unsafe.INSTANCE);
        }

    static class Unsafe
        {
        /**
         * Class is intentionally not javadoc'd.
         *
         * Unsafe accessors for Binary.
         * @see com.tangosol.util.Unsafe
         */

        // ----- constructors -----------------------------------------------

        /**
         * Hidden constructor
         */
        private Unsafe()
            {}

        // ----- Unsafe methods ---------------------------------------------

        /**
         * Return the underlying byte[] for the specified binary.
         *
         * @param bin  the binary
         *
         * @return the underlying byte[]
         */
        public byte[] getByteArray(Binary bin)
            {
            return bin.m_ab;
            }

        /**
         * Return the offset into the {@link #getByteArray(Binary) underlying
         * byte array} of the specified binary.
         *
         * @param bin  the binary
         *
         * @return the offset into the underlying byte[]
         */
        public int getArrayOffset(Binary bin)
            {
            return bin.m_of;
            }

        /**
         * Return a new {@link Binary} instance backed by the specified byte[]
         * beginning at the specified offset and of the specified length.
         * <p>
         * Note: unlike the {@link #Binary(byte[],int,int) analagous constructor},
         * this method does not create a copy of the passed array; it <b>is the
         * caller's responsibility</b> not to mutate the contents of the array.
         *
         * @param ab  the byte array
         * @param of  the starting offset
         * @param cb  the length of the binary
         *
         * @return a new Binary based on the specified array
         */
        public Binary newBinary(byte[] ab, int of, int cb)
            {
            if (of < 0 || of + cb > ab.length)
                {
                throw new IndexOutOfBoundsException();
                }

            return new Binary(ab, of, cb, false);
            }

        // ----- constants --------------------------------------------------

        /**
         * The singleton instance.
         */
        private static final Unsafe INSTANCE = new Unsafe();
        }

    // ----- data members ---------------------------------------------------

    /**
    * Cached hash code.
    */
    private transient int m_nHash;
    }
