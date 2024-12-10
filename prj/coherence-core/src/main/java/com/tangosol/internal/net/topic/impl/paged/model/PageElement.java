/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.model;

import com.oracle.coherence.common.base.Converter;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.Serializer;

import com.tangosol.io.SerializerAware;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.time.Instant;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import java.util.function.Function;

/**
 * An {@link com.tangosol.net.topic.Subscriber.Element} from a topic.
 * <p>
 * This implementation is not {@link #commitAsync() commitable} and will throw an
 * {@link UnsupportedOperationException} if committed.
 * <p>
 * Deserialization of values and metadata is lazily performed the first time
 * the value or metadata values are requested.
 *
 * @param <V>  the type of this element's value
 *
 * @author Jonathan Knight  2021.05.16
 * @since 21.06
 */
public class PageElement<V>
        implements Subscriber.Element<V>, PortableObject, ExternalizableLite, SerializerAware
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for serialization.
     */
    public PageElement()
        {
        }

    /**
     * Create a {@link PageElement}.
     *
     * @param binary      the binary value decorated with metadata
     * @param serializer  the {@link Serializer} to use to deserialize the value
     */
    PageElement(Binary binary, Serializer serializer)
        {
        this(binary, b -> ExternalizableHelper.fromBinary(b, serializer));
        m_serializer = serializer;
        }

    /**
     * Create a {@link PageElement}.
     *
     * @param binary     the binary value decorated with metadata
     * @param converter  the {@link Converter} to use to deserialize the value
     */
    PageElement(Binary binary, Converter<Binary, V> converter)
        {
        m_binValue  = binary;
        m_converter = converter;
        }

    // ----- Subscriber.Element methods -------------------------------------

    @Override
    public V getValue()
        {
        if (m_oValue == null)
            {
            m_oValue = m_converter.convert(m_binValue.toBinary());
            }
        return m_oValue;
        }

    @Override
    public Binary getBinaryValue()
        {
        if (m_binUndecorated == null)
            {
            m_binUndecorated = ExternalizableHelper.undecorate(m_binValue, DECO_RSVD_1);
            }
        return m_binUndecorated.toBinary();
        }

    @Override
    public int getChannel()
        {
        ensureMetadata();
        return m_nChannel;
        }

    @Override
    public Position getPosition()
        {
        ensureMetadata();
        return m_position;
        }

    @Override
    public Instant getTimestamp()
        {
        ensureMetadata();
        return Instant.ofEpochMilli(m_lTimestamp);
        }

    @Override
    public CompletableFuture<Subscriber.CommitResult> commitAsync()
        {
        throw new UnsupportedOperationException();
        }

    /**
     * Returns the timestamp of this element in epoch millis.
     * <p>
     * The timestamp is the epoch time that the element was accepted into the topic taken from the
     * {@link com.tangosol.net.Cluster#getTimeMillis() cluster time} on the member that accepted the
     * published element.
     *
     * @return the timestamp of this element in epoch millis
     */
    public long getTimestampMillis()
        {
        ensureMetadata();
        return m_lTimestamp;
        }

    // ----- SerializerAware methods ----------------------------------------

    @Override
    public Serializer getContextSerializer()
        {
        return m_serializer;
        }

    @Override
    public void setContextSerializer(Serializer serializer)
        {
        m_serializer = serializer;
        m_converter  = b -> ExternalizableHelper.fromBinary(b, serializer);
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_binValue = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_binValue);
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_binValue = in.readBinary(0);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeBinary(0, m_binValue.toBinary());
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }
        PageElement<?> element = (PageElement<?>) o;
        return Objects.equals(m_binValue, element.m_binValue);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_binValue);
        }

    @Override
    public String toString()
        {
        return "Element(" +
                "channel=" + getChannel() +
                ", position=" + getPosition() +
                ", timestamp=" + getTimestamp() +
                ", value=" + getValue() +
                ')';
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Create a binary representation of a {@link PageElement}.
     *
     * @param nChannel    the channel the element was published to
     * @param lPage       the page within the channel the element was published to
     * @param nOffset     the offset in the page the element was published to
     * @param lTimestamp  the timestamp that the receiving member accepted the element
     * @param binValue    the serialized element value
     *
     * @return a binary representation of a {@link PageElement}
     */
    public static Binary toBinary(int nChannel, long lPage, int nOffset, long lTimestamp, Binary binValue)
        {
        byte[] ab = new byte[5 + 10 + 5 + 10]; // 10 for each long, 5 for each int
        int    cb = 0;

        cb += pack(nChannel, ab, cb);
        cb += pack(lPage, ab, cb);
        cb += pack(nOffset, ab, cb);
        cb += pack(lTimestamp, ab, cb);

        Binary binMetadata  = new Binary(ab, 0, cb);
        return ExternalizableHelper.decorate(binValue, ExternalizableHelper.DECO_RSVD_1, binMetadata);
        }

    /**
     * Returns a {@link PageElement} from a binary serialized element.
     *
     * @param binary      the binary representation of the {@link PageElement}
     * @param serializer  the {@link Serializer} to deserialize the element's value
     * @param <V>         the type of the element's value
     *
     * @return a {@link PageElement} from a binary serialized element
     */
    public static <V> PageElement<V> fromBinary(Binary binary, Serializer serializer)
        {
        return new PageElement<>(binary, serializer);
        }

    /**
     * Returns a {@link PageElement} from a binary serialized element.
     *
     * @param binary     the binary representation of the {@link PageElement}
     * @param converter  the {@link Converter} to deserialize the element's value
     * @param <V>        the type of the element's value
     *
     * @return a {@link PageElement} from a binary serialized element
     */
    public static <V> PageElement<V> fromBinary(Binary binary, Converter<Binary, V> converter)
        {
        return new PageElement<>(binary, converter);
        }

    /**
     * Return a binary representation this {@link PageElement} with the value converted
     * using the converter function or returns {@code null} if the converter function
     * returns {@code null}.
     *
     * @param fn          the {@link Function} to convert the value
     * @param serializer  the serializer to serialize the converted value to a {@link Binary}
     * @param <U>         the type of the converted value
     *
     * @return a binary representation this {@link PageElement} with the value converted
     *         using the converter function or {@code null} if the converter function
     *         returns {@code null}
     */
    public <U> Binary convert(Function<V, U> fn, Converter<U, Binary> serializer)
        {
        ensureMetadata();
        U oConverted = fn.apply(getValue());
        if (oConverted != null)
            {
            Binary binValue  = serializer.convert(oConverted);
            return ExternalizableHelper.decorate(binValue, ExternalizableHelper.DECO_RSVD_1, m_binMetadata).toBinary();
            }
        return null;
        }

    private void ensureMetadata()
        {
        try
            {
            if (m_binMetadata == null)
                {
                ReadBuffer             binDeco = ExternalizableHelper.getDecoration(m_binValue, DECO_RSVD_1);
                ReadBuffer.BufferInput in      = binDeco.getBufferInput();

                int  nChannel   = in.readPackedInt();
                long lPage      = in.readPackedLong();
                int  nOffset    = in.readPackedInt();
                long lTimestamp = in.readPackedLong();

                m_nChannel    = nChannel;
                m_position    = new PagedPosition(lPage, nOffset);
                m_lTimestamp  = lTimestamp;
                m_binMetadata = binDeco;
                }
            }
        catch (IOException e)
            {
            throw new IllegalStateException(e);
            }
        }

    /**
     * Write out the specified long in packed format.
     *
     * @param l  the value to write
     * @param ab the byte array to write to
     * @param of the offset to write at
     *
     * @return the number of bytes written
     */
    private static int pack(long l, byte[] ab, int of)
        {
        int cb = 0;

        // first byte contains sign bit (bit 7 set if neg)
        int b = 0;
        if (l < 0)
            {
            b = 0x40;
            l = ~l;
            }

        // first byte contains only 6 data bits
        b |= (byte) (((int) l) & 0x3F);
        l >>>= 6;

        while (l != 0)
            {
            b |= 0x80;          // bit 8 is a continuation bit
            ab[of + cb++] = (byte) b;

            b = (((int) l) & 0x7F);
            l >>>= 7;
            }

        ab[of + cb++] = (byte) b;

        return cb;
        }

    // ----- constants ------------------------------------------------------

    private final int DECO_RSVD_1 = ExternalizableHelper.DECO_RSVD_1;

    // ----- data members ---------------------------------------------------

    private ReadBuffer m_binValue;

    private Converter<Binary, V> m_converter;

    private transient Serializer m_serializer;

    private ReadBuffer m_binUndecorated;

    private ReadBuffer m_binMetadata;

    private V m_oValue;

    private int m_nChannel;

    private PagedPosition m_position;

    private long m_lTimestamp;
    }
