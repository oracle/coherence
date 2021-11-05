/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.function;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.function.Remote.Predicate;

import java.io.IOException;

/**
 * Represents a portable {@link Predicate} (boolean-valued function) with a single argument; a convenience interface for
 * an implementation with no properties that require serialization.
 *
 * @param <T>  the type of input to the {@link Predicate}
 *
 * @author lh
 * @since 21.12
 */
public interface PortablePredicate<T>
        extends Predicate<T>, PortableObject
    {
    // ----- PortableObject interface ---------------------------------------

    @Override
    default void readExternal(PofReader pofReader) throws IOException
        {
        }

    @Override
    default void writeExternal(PofWriter pofWriter) throws IOException
        {
        }
    }
