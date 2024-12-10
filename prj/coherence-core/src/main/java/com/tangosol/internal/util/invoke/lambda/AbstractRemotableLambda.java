/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.invoke.lambda;

import com.oracle.coherence.common.base.CanonicallyNamed;

import com.oracle.coherence.common.internal.util.CanonicalNames;

import com.tangosol.util.Base;

import com.tangosol.internal.util.invoke.ClassIdentity;
import com.tangosol.internal.util.invoke.RemoteConstructor;
import com.tangosol.internal.util.invoke.Remotable;

import com.tangosol.util.ValueExtractor;

import java.io.ObjectStreamException;
import java.io.Serializable;

import javax.json.bind.annotation.JsonbProperty;

/**
 * Abstract base class for the remote lambdas created by the
 * {@link RemotableLambdaGenerator}.
 *
 * @author as  2015.06.24
 * @since 12.2.1
 */
public abstract class AbstractRemotableLambda<T>
        implements Remotable<T>, CanonicallyNamed, Serializable
    {
    // ---- accessors -------------------------------------------------------

    /**
     * Return the {@link ClassIdentity} associated with this lambda.
     *
     * @return a {@code FunctionIdentity} for this lambda
     */
    public ClassIdentity getId()
        {
        return m_remoteConstructor.getDefinition().getId();
        }

    /**
     * Return true if this lambda represents a method reference.
     *
     * @return {@code true} if this lambda represents a method reference,
     *         {@code false} otherwise
     */
    public boolean isMethodReference()
        {
        return getId() instanceof MethodReferenceIdentity;
        }

    // ---- CanonicallyNamed interface --------------------------------------

    @Override
    public String getCanonicalName()
        {
        String sCName = m_sNameCanon;

        if (sCName == null && isValueExtractor())
            {
            MethodReferenceIdentity id = (MethodReferenceIdentity) getId();
            sCName = m_sNameCanon = CanonicalNames.computeValueExtractorCanonicalName(
                             id.getImplMethod() + CanonicalNames.VALUE_EXTRACTOR_METHOD_SUFFIX);
            }
        return sCName;
        }

    // ---- Remotable interface ---------------------------------------------

    @Override
    public RemoteConstructor<T> getRemoteConstructor()
        {
        return m_remoteConstructor;
        }

    @Override
    public void setRemoteConstructor(RemoteConstructor<T> constructor)
        {
        m_remoteConstructor = constructor;
        }

    // ---- Object methods --------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (o == this)
            {
            return true;
            }
        if (o instanceof AbstractRemotableLambda)
            {
            AbstractRemotableLambda<?> that = (AbstractRemotableLambda) o;
            return Base.equals(m_remoteConstructor, that.m_remoteConstructor);
            }
        else if (o instanceof ValueExtractor)
            {
            ValueExtractor thatExtractor = (ValueExtractor) o;
            String         sCNameThis    = getCanonicalName();
            String         sCNameThat    = thatExtractor.getCanonicalName();

            return Base.equals(sCNameThis, sCNameThat);
            }

        return false;
        }

    /**
     * When this is a {@link com.tangosol.util.ValueExtractor}, return the hashCode of
     * {@link com.tangosol.util.ValueExtractor#getCanonicalName() canonical name};
     * otherwise, hashCode of {@link RemoteConstructor#hashCode()}.
     * @return hashCode for this instance
     */
    @Override
    public int hashCode()
        {
        return isValueExtractor() ? getCanonicalName().hashCode() : m_remoteConstructor.hashCode();
        }

    @Override
    public String toString()
        {
        StringBuilder sb = new StringBuilder();

        sb.append(getClass().getSimpleName());
        sb.append("{remotable=").append(getRemoteConstructor());

        if (isValueExtractor())
            {
            String sCName = getCanonicalName();
            if (sCName != null)
                {
                sb.append(", ValueExtractor(").append(sCName).append(')');
                }
            }
        sb.append('}');
        return sb.toString();
        }

    // ---- SerializationSupport interface ----------------------------------

    /**
     * {@inheritDoc}
     *
     * It ensures that this {@link Remotable} instance is replaced with the
     * appropriate {@link RemoteConstructor} before it is serialized.
     */
    @Override
    public Object writeReplace() throws ObjectStreamException
        {
        return getRemoteConstructor();
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Is this a ValueExtractor.
     *
     * @return true iff this is a ValueExtractor
     */
    protected boolean isValueExtractor()
        {
        return isMethodReference() && this instanceof ValueExtractor;
        }

    // ---- data members ----------------------------------------------------

    /**
     * The {@code RemoteConstructor} for this instance.
     */
    @JsonbProperty("remoteConstructor")
    protected RemoteConstructor<T> m_remoteConstructor;

    /**
     * Cache of the canonical name for this instance if it is a {@link #isMethodReference()};
     * otherwise, it is null.
     * Used to compare equivalence in {#equals(Object)}.
     *
     * @since 12.2.1.4
     */
    protected transient String m_sNameCanon = null;
    }
