/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.inject;

import com.tangosol.io.SerializationSupport;

import java.io.ObjectStreamException;

/**
 * An interface that should be implemented by classes that require CDI injection
 * upon deserialization.
 *
 * @author Aleks Seovic  2019.10.02
 * @since 20.06
 */
public interface Injectable
        extends SerializationSupport
    {
    @Override
    default Object readResolve() throws ObjectStreamException
        {
        Injector injector = InjectorProvider.getInstance();
        injector.inject(this);
        return this;
        }

    @Override
    default Object writeReplace() throws ObjectStreamException
        {
        return this;
        }
    }
