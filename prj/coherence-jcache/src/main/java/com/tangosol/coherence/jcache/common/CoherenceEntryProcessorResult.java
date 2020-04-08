/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.common;

import com.tangosol.coherence.jcache.serialization.SerializationHelper;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.cache.CacheException;

import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;

/**
 * A Coherence-based {@link javax.cache.processor.EntryProcessorResult}.
 *
 * @param <T>  the type of the returned m_oResult
 *
 * @author bo  2013.11.11
 * @since Coherence 12.1.3
 */
public class CoherenceEntryProcessorResult<T>
        implements EntryProcessorResult<T>, ExternalizableLite, PortableObject
    {
    // ----- constructors -----------------------------------------------------

    /**
     * Constructs a {@link CoherenceEntryProcessorResult}
     * (for serialization).
     */
    public CoherenceEntryProcessorResult()
        {
        // required for serialization
        }

    /**
     * Constructs a {@link CoherenceEntryProcessorResult} for an
     * {@link Exception}.
     *
     * @param exception  the {@link Exception}
     */
    public CoherenceEntryProcessorResult(Exception exception)
        {
        m_oResult   = null;
        m_exception = exception;
        }

    /**
     * Constructs an {@link CoherenceEntryProcessorResult} for a
     * specific value.
     *
     * @param oResult  the result
     */
    public CoherenceEntryProcessorResult(T oResult)
        {
        m_oResult   = oResult;
        m_exception = null;
        }

    // ----- EntryProcessorResult interface ---------------------------------

    @Override
    public T get()
            throws EntryProcessorException
        {
        if (m_exception == null)
            {
            return m_oResult;
            }
        else
            {
            Throwable throwable = Helper.unwrap(m_exception);

            if (throwable instanceof CacheException)
                {
                throw(CacheException) throwable;
                }
            else
                {
                throw new EntryProcessorException(throwable);
                }
            }
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_oResult   = (T) ExternalizableHelper.readObject(in);
        m_exception = (Exception) ExternalizableHelper.fromBinary(new Binary(in));
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeObject(out, m_oResult);
        ExternalizableHelper.toBinary(m_exception).writeExternal(out);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader reader)
            throws IOException
        {
        m_oResult = (T) reader.readObject(0);

        boolean fUseJavaSerialization = reader.readBoolean(1);

        if (fUseJavaSerialization)
            {
            m_exception = SerializationHelper.fromByteArray(reader.readByteArray(2), Exception.class);
            }
        else
            {
            m_exception = (Exception) reader.readObject(2);
            }
        }

    @Override
    public void writeExternal(PofWriter writer)
            throws IOException
        {
        writer.writeObject(0, m_oResult);

        boolean fUseJavaSerialization = !(m_exception instanceof PortableObject
                                          || m_exception instanceof ExternalizableLite);

        writer.writeBoolean(1, fUseJavaSerialization);

        if (fUseJavaSerialization)
            {
            writer.writeByteArray(2, SerializationHelper.toByteArray(m_exception));
            }
        else
            {
            writer.writeObject(2, m_exception);
            }
        }

    // ------ data members --------------------------------------------------

    /**
     * The result of processing the entry.  This may be <code>null</code>.
     */
    private T m_oResult;

    /**
     * The {@link Exception} that occurred executing an {@link javax.cache.processor.EntryProcessor}.
     * When <code>null</code> no exception has occurred.
     */
    private Exception m_exception;
    }
