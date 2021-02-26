/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.invoke.lambda;

import com.oracle.coherence.common.base.Classes;

import com.oracle.coherence.common.internal.util.CanonicalNames;

import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.SerializationSupport;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.AbstractExtractor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectStreamException;

import java.lang.invoke.SerializedLambda;

import java.lang.reflect.Method;
import javax.json.bind.annotation.JsonbProperty;

/**
 * An implementation of a {@link ValueExtractor} that is used to capture and
 * serialize method reference-based lambda extractors.
 *
 * @author Aleks Seovic  2021.02.25
 */
public class MethodReferenceExtractor<T, E>
        extends AbstractExtractor<T, E>
        implements ExternalizableLite, PortableObject, SerializationSupport
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    @SuppressWarnings("unused")
    public MethodReferenceExtractor()
        {
        }

    /**
     * Package-private constructor, used for testing.
     *
     * @param extractor  a method reference that satisfies ValueExtractor
     *                   functional interface
     */
    MethodReferenceExtractor(ValueExtractor<? super T, ? extends E> extractor)
        {
        this(Lambdas.getSerializedLambda(extractor));
        }

    /**
     * Construct {@link MethodReferenceExtractor} instance.
     *
     * @param lambda  serialized lambda to create this instance from
     */
    public MethodReferenceExtractor(SerializedLambda lambda)
        {
        m_sMethodName = lambda.getImplMethodName();
        m_sClassName  = lambda.getImplClass().replace('/', '.');

        init();
        }

    // ---- ValueExtractor interface ----------------------------------------

    @Override
    public E extract(T oTarget)
        {
        return m_extractor.extract(oTarget);
        }

    // ---- helper methods --------------------------------------------------

    /**
     * Initialize this extractor based on the captured method reference metadata.
     *
     * @return this instance
     */
    private MethodReferenceExtractor<T, E> init()
        {
        try
            {
            m_sNameCanon = CanonicalNames.computeValueExtractorCanonicalName(
                    m_sMethodName + CanonicalNames.VALUE_EXTRACTOR_METHOD_SUFFIX);

            Class<?> clazz  = Base.getContextClassLoader(this).loadClass(m_sClassName);
            Method   method = Classes.findMethod(clazz, m_sMethodName, null, false);
            if (method == null)
                {
                throw new IllegalArgumentException("MethodReferenceExtractor for " + toString() + " cannot be constructed");
                }
            m_extractor = ValueExtractor.forMethod(method);
            return this;
            }
        catch (Throwable e)
            {
            throw new IllegalArgumentException("MethodReferenceExtractor for " + toString() + " cannot be constructed", e);
            }
        }

    // ---- Object methods --------------------------------------------------

    public String toString()
        {
        return m_sClassName + "::" + m_sMethodName;
        }

    // ---- SerializationSupport interface ----------------------------------

    public Object readResolve() throws ObjectStreamException
        {
        return init();
        }

    // ---- ExternalizableLite interface ------------------------------------

    public void readExternal(DataInput in) throws IOException
        {
        m_sClassName  = in.readUTF();
        m_sMethodName = in.readUTF();
        }

    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeUTF(m_sClassName);
        out.writeUTF(m_sMethodName);
        }

    // ---- PortableObject interface ----------------------------------------

    public void readExternal(PofReader in) throws IOException
        {
        m_sClassName  = in.readString(0);
        m_sMethodName = in.readString(1);
        }

    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, m_sClassName);
        out.writeString(1, m_sMethodName);
        }

    // ---- data members ----------------------------------------------------

    /**
     * Method reference lambda to use for extraction.
     */
    private transient ValueExtractor<? super T, ? extends E> m_extractor;

    /**
     * The name of the class referenced by the method reference.
     */
    @JsonbProperty("className")
    private String m_sClassName;

    /**
     * The name of the method referenced by the method reference.
     */
    @JsonbProperty("methodName")
    private String m_sMethodName;
    }
