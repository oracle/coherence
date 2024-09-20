/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.extractor;

import com.tangosol.internal.util.graal.ScriptManager;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.ResolvingObjectInputStream;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.ValueExtractor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;

import org.graalvm.polyglot.Value;

/**
 * ScriptValueExtractor is an {@link AbstractExtractor} that wraps a script
 * written in one of the languages supported by Graal VM.
 *
 * @param <T>  the type of the value to extract from
 * @param <E>  the type of value that will be extracted
 *
 * @author mk 2019.07.26
 * @since 14.1.1.0
 */
public class ScriptValueExtractor<T, E>
        extends AbstractExtractor<T, E>
        implements ExternalizableLite, PortableObject
    {
    // ------ constructors ---------------------------------------------------

    /**
     * Default constructor for deserialization.
     */
    public ScriptValueExtractor()
        {
        }

    /**
     * Create a {@link ValueExtractor} that wraps the specified script.
     *
     * @param sLanguage  the language the script is written
     * @param sName      the name of the {@link ValueExtractor} that needs to
     *                   be evaluated
     * @param aoArgs     the arguments to be passed to the script during
     *                   evaluation
     */
    public ScriptValueExtractor(String sLanguage, String sName, Object... aoArgs)
        {
        m_sLanguage = sLanguage;
        m_sName     = sName;
        m_aoArgs    = aoArgs;
        }

    // ----- ValueExtractor interface ----------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public E extract(T target)
        {
        Value value = ScriptManager.getInstance()
                                   .execute(m_sLanguage, m_sName, m_aoArgs);

        return (E) value.as(ValueExtractor.class).extract(target);
        }

    // ----- ExternalizableLite interface ------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_sLanguage = in.readUTF();
        m_sName     = in.readUTF();
        m_aoArgs    = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeUTF(m_sLanguage);
        out.writeUTF(m_sName);
        ExternalizableHelper.writeObject(out, m_aoArgs);
        }

    // ----- PortableObject interface ----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_sLanguage = in.readString(0);
        m_sName     = in.readString(1);
        m_aoArgs    = in.readArray(2, Object[]::new);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, m_sLanguage);
        out.writeString(1, m_sName);
        out.writeObjectArray(2, m_aoArgs);
        }

    // ----- Serializable methods -------------------------------------------

    /**
     * See {@link java.io.Serializable} for documentation on this method.
     *
     * @param inputStream  the input stream
     *
     * @throws NotSerializableException, ClassNotFoundException, IOException
     *
     * @since 12.2.1.4.22
     */
    @java.io.Serial
    private void readObject(ObjectInputStream inputStream) throws ClassNotFoundException, IOException
        {
        if (inputStream instanceof ResolvingObjectInputStream || ExternalizableHelper.s_tloInEHDeserialize.get())
            {
            // deserialization was initiated via ExternalizableHelper; proceed
            inputStream.defaultReadObject();
            }
        else
            {
            // this class is not intended for "external" use
            throw new NotSerializableException();
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The language the script is written.
     */
    private String m_sLanguage;

    /**
     * The name of the {@link ValueExtractor} to execute.
     */
    private String m_sName;

    /**
     * The arguments to be passed to the script during evaluation.
     */
    private Object[] m_aoArgs;
    }
