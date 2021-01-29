/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.oracle.coherence.inject.Injector;

import javax.enterprise.context.spi.CreationalContext;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.InjectionTarget;

/**
 * An implementation of {@link Injector} that uses
 * the CDI bean manager to inject dependencies.
 *
 * @author Jonathan Knight  2020.11.19
 * @since 20.12
 */
public class CdiInjector
        implements Injector
    {
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void inject(Object target)
        {
        BeanManager          bm              = CDI.current().getBeanManager();
        Class<?>             clz             = target.getClass();
        AnnotatedType<?>     annotatedType   = bm.createAnnotatedType(clz);
        InjectionTarget      injectionTarget = bm.createInjectionTarget(annotatedType);
        CreationalContext<?> context         = bm.createCreationalContext(null);

        injectionTarget.inject(target, context);
        injectionTarget.postConstruct(target);
        }
    }
