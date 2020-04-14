/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


/**
* A simple base class for ByteSequence implementations.
*
* @author cp 2010-06-29
* @since Coherence 3.7
*/
public abstract class AbstractByteSequence
        implements ByteSequence
    {
    /**
    * {@inheritDoc}
    */
    public ByteSequence subSequence(int ofStart, int ofEnd)
        {
        int cbThis = length();
        int cbThat = ofEnd - ofStart;
        if (ofStart < 0 || ofEnd > cbThis || cbThat < 0)
            {
            throw new IllegalArgumentException("ofStart=" + ofStart
                    + ", ofEnd=" + ofEnd + ", length()=" + cbThis);
            }

        if (ofStart == 0 && ofEnd == cbThis)
            {
            return this;
            }

        return new PartialByteSequence(this, ofStart, cbThat);
        }

    /**
    * {@inheritDoc}
    */
    public Binary toBinary()
        {
        int cb = length();
        if (cb == 0)
            {
            return Binary.NO_BINARY;
            }

        BinaryWriteBuffer writer = new BinaryWriteBuffer(cb);
        for (int of = 0; of < cb; ++of)
            {
            writer.write(of, byteAt(of));
            }

        return writer.toBinary();
        }

    /**
    * {@inheritDoc}
    */
    @Override public int hashCode()
        {
        return Base.toCrc(this);
        }

    /**
    * {@inheritDoc}
    */
    @Override public boolean equals(Object o)
        {
        if (!(o instanceof ByteSequence))
            {
            return false;
            }

        ByteSequence that = (ByteSequence) o;
        if (this == that)
            {
            return true;
            }

        int cb = this.length();
        if (cb != that.length())
            {
            return false;
            }

        for (int of = 0; of < cb; ++of)
            {
            if (this.byteAt(of) != that.byteAt(of))
                {
                return false;
                }
            }

        return true;
        }

    /**
    * {@inheritDoc}
    */
    @Override public String toString()
        {
        // "ByteSequence(length=4, value=0x01F03DA7)"
        int       cb     = length();
        final int MAX    = 256;
        boolean   fTrunc = cb > MAX;
        return "ByteSequence(length=" + cb + ", value="
                + Base.toHexEscape(this, 0, fTrunc ? MAX : cb)
                + (fTrunc ? "...)" : ")");
        }


    // ----- inner class: PartialByteSequence --------------------------------

    /**
    * A naive ByteSequence that represents a portion of another ByteSequence.
    *
    * @author cp 2010-06-29
    * @since Coherence 3.7
    */
    public static class PartialByteSequence
            extends AbstractByteSequence
        {
        // ----- constructors --------------------------------------------

        /**
        * Construct a PartialByteSequence from a ByteSequence.
        *
        * @param seq  the ByteSequence
        * @param of   the offset within the ByteSequence
        * @param cb   the number of bytes from the ByteSequence
        */
        public PartialByteSequence(ByteSequence seq, int of, int cb)
            {
            assert seq != null;
            assert of >= 0 && of < seq.length();
            assert cb >= 0 && of + cb <= seq.length();

            m_seq = seq;
            m_of  = of;
            m_cb  = cb;
            }

        // ----- ByteSequence interface ----------------------------------

        /**
        * {@inheritDoc}
        */
        public int length()
            {
            return m_cb;
            }

        /**
        * {@inheritDoc}
        */
        public byte byteAt(int of)
            {
            if (of < 0 || of >= m_cb)
                {
                throw new IndexOutOfBoundsException("of=" + of + ", range=0.." + (m_cb-1));
                }
            return m_seq.byteAt(m_of + of);
            }

        // ----- data members --------------------------------------------

        /**
        * The underlying ByteSequence.
        */
        private ByteSequence m_seq;

        /**
        * The offset into the underlying ByteSequence.
        */
        private int m_of;

        /**
        * The number of bytes to expose from the underlying ByteSequence.
        */
        private int m_cb;
        }


    // ----- inner class: AggregateByteSequence ------------------------------

    /**
    * A naive ByteSequence that glues two ByteSequence instances together.
    *
    * @author cp 2010-06-29
    * @since Coherence 3.7
    */
    public static class AggregateByteSequence
            extends AbstractByteSequence
        {
        // ----- constructors --------------------------------------------

        /**
        * Construct an AggregateByteSequence from two ByteSequence objects.
        *
        * @param seqFirst   the first ByteSequence
        * @param seqSecond  the second ByteSequence
        */
        public AggregateByteSequence(ByteSequence seqFirst, ByteSequence seqSecond)
            {
            m_seq1 = seqFirst;
            m_seq2 = seqSecond;
            }

        // ----- ByteSequence interface ----------------------------------

        /**
        * {@inheritDoc}
        */
        public int length()
            {
            return m_seq1.length() + m_seq2.length();
            }

        /**
        * {@inheritDoc}
        */
        public byte byteAt(int of)
            {
            int cb1 = m_seq1.length();
            return of < cb1 ? m_seq1.byteAt(of) : m_seq2.byteAt(of - cb1);
            }

        /**
        * {@inheritDoc}
        */
        @Override public ByteSequence subSequence(int ofStart, int ofEnd)
            {
            int cbThis = length();
            int cbThat = ofEnd - ofStart;
            if (ofStart == 0 && ofEnd == m_seq1.length())
                {
                return m_seq1;
                }
            else if (ofEnd == cbThis && cbThat == m_seq2.length())
                {
                return m_seq2;
                }
            else
                {
                return super.subSequence(ofStart, ofEnd);
                }
            }

        // ----- data members --------------------------------------------

        /**
        * The ByteSequence that is the start of this AggregateByteSequence.
        */
        private ByteSequence m_seq1;

        /**
        * The ByteSequence that is the end of this AggregateByteSequence.
        */
        private ByteSequence m_seq2;
        }
    }
