/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import java.io.ObjectStreamException;


/**
 * The SerializationSupport interface provides the ability for objects that are
 * serialized by any {@link Serializer} to affect the initial stage of serialization
 * and the final stage of deserialization.
 * <p>
 * The semantics of this interface's methods are identical to the
 * <code>writeReplace</code> and <code>readResolve</code> pseudo-interface methods
 * defined by the {@link java.io.Serializable} interface.
 * <p>
 * This interface provides trivial implementations of both methods to allow
 * implementors to override only the necessary one.
 *
 * @author as,gg 2014.12.22
 *
 * @since Coherence 12.2.1
 */
public interface SerializationSupport
    {
    /**
     * Designate an alternative object to be used when writing an object to the
     * buffer.
     * <p>
     * This method is invoked by the {@link Serializer} if the object implements
     * <code>SerializationSupport</code> interface.
     *
     * @return an object which state should be written instead of the original one
     */
    default public Object writeReplace()
            throws ObjectStreamException
        {
        return this;
        }

    /**
     * Designate a replacement for an object after an instance of it is read from
     * the buffer.
     * <p>
     * This method is invoked by the {@link Serializer} if the object implements
     * <code>SerializationSupport</code> interface.
     *
     * @return an object that should be returned instead of the deserialized one
     */
    default public Object readResolve()
            throws ObjectStreamException
        {
        return this;
        }
    }
