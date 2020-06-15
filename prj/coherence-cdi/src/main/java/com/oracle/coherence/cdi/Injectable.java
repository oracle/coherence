/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.tangosol.io.SerializationSupport;

import java.io.ObjectStreamException;

import javax.enterprise.context.spi.CreationalContext;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.InjectionTarget;

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
    @SuppressWarnings({"unchecked", "rawtypes"})
    default Object readResolve() throws ObjectStreamException
        {
        BeanManager          bm              = CDI.current().getBeanManager();
        Class<?>             clz             = this.getClass();
        AnnotatedType<?>     annotatedType   = bm.createAnnotatedType(clz);
        InjectionTarget      injectionTarget = bm.createInjectionTarget(annotatedType);
        CreationalContext<?> context         = bm.createCreationalContext(null);

        injectionTarget.inject(this, context);
        injectionTarget.postConstruct(this);

        return this;
        }

    @Override
    default Object writeReplace() throws ObjectStreamException
        {
        return this;
        }
    }
