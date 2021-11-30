/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.internal.cdi;

import com.oracle.coherence.concurrent.atomic.Atomics;
import com.oracle.coherence.concurrent.atomic.AsyncAtomicInteger;
import com.oracle.coherence.concurrent.atomic.AsyncLocalAtomicInteger;
import com.oracle.coherence.concurrent.atomic.AsyncRemoteAtomicInteger;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;

import javax.enterprise.inject.spi.InjectionPoint;

/**
 * CDI producers for {@link AsyncAtomicInteger} values.
 *
 * @author Aleks Seovic  2020.12.07
 * @since 21.12
 */
@ApplicationScoped
class AsyncAtomicIntegerProducer
        extends AbstractAtomicProducer
    {
    /**
     * Returns a local {@link AsyncAtomicInteger} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link AsyncAtomicInteger} for the provided {@link InjectionPoint}
     */
    @Produces
    AsyncAtomicInteger getUnqualifiedAtomicInteger(InjectionPoint ip)
        {
        return getLocalAtomicInteger(ip);
        }

    /**
     * Returns a local {@link AsyncAtomicInteger} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link AsyncAtomicInteger} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    AsyncAtomicInteger getAtomicInteger(InjectionPoint ip)
        {
        return getLocalAtomicInteger(ip);
        }

    /**
     * Returns a remote {@link AsyncAtomicInteger} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a remote {@link AsyncAtomicInteger} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Remote
    AsyncAtomicInteger getAtomicIntegerWithRemoteAnnotation(InjectionPoint ip)
        {
        return getRemoteAtomicInteger(ip);
        }

    /**
     * Returns a {@link AsyncLocalAtomicInteger} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link AsyncLocalAtomicInteger} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(AsyncLocalAtomicInteger.class)
    AsyncLocalAtomicInteger getUnqualifiedLocalAtomicInteger(InjectionPoint ip)
        {
        return getLocalAtomicInteger(ip);
        }

    /**
     * Returns a {@link AsyncLocalAtomicInteger} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link AsyncLocalAtomicInteger} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(AsyncLocalAtomicInteger.class)
    AsyncLocalAtomicInteger getLocalAtomicInteger(InjectionPoint ip)
        {
        return Atomics.localAtomicInteger(getName(ip)).async();
        }

    /**
     * Returns a {@link AsyncRemoteAtomicInteger} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link AsyncRemoteAtomicInteger} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(AsyncRemoteAtomicInteger.class)
    AsyncRemoteAtomicInteger getUnqualifiedRemoteAtomicInteger(InjectionPoint ip)
        {
        return getRemoteAtomicInteger(ip);
        }

    /**
     * Returns a {@link AsyncRemoteAtomicInteger} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link AsyncRemoteAtomicInteger} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(AsyncRemoteAtomicInteger.class)
    AsyncRemoteAtomicInteger getRemoteAtomicInteger(InjectionPoint ip)
        {
        return Atomics.remoteAtomicInteger(getName(ip)).async();
        }
    }
