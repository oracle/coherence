/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.invoke;

import com.tangosol.io.SerializationSupport;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * An interface that must be implemented by all remotable classes.
 * <p>
 * This interface extends {@link SerializationSupport} and overrides
 * {@link #writeReplace()} method to ensure that all {@code Remotable} instances
 * return associated {@link RemoteConstructor} during serialization.
 *
 * @see SerializationSupport
 * @see RemoteConstructor#readResolve()
 *
 * @author as  2015.08.21
 * @since 12.2.1
 */
@SuppressWarnings("unchecked")
public interface Remotable<T>
        extends SerializationSupport
    {
    /**
     * Return a {@link RemoteConstructor} for this object.
     *
     * @return a RemoteConstructor for this object
     */
    RemoteConstructor<T> getRemoteConstructor();

    /**
     * Set a {@link RemoteConstructor} for this object.
     *
     * @param constructor  a RemoteConstructor for this object
     */
    void setRemoteConstructor(RemoteConstructor<T> constructor);

    // ---- SerializationSupport interface ----------------------------------

    /**
     * {@inheritDoc}
     *
     * It ensures that this {@link Remotable} instance is replaced with the
     * appropriate {@link RemoteConstructor} before it is serialized.
     */
    @Override
    default Object writeReplace() throws ObjectStreamException
        {
        return getRemoteConstructor();
        }

    // ---- static helpers --------------------------------------------------

    /**
     * Ensure that a specified lambda is remotable.
     *
     * @param lambda  a lambda to convert to a remotable lambda if necessary
     * @param <T>     the lambda type
     *
     * @return a {@code Remotable} instance of the specified lambda
     */
    static <T extends Serializable> T lambda(T lambda)
        {
        return Lambdas.ensureRemotable(lambda);
        }
    }
