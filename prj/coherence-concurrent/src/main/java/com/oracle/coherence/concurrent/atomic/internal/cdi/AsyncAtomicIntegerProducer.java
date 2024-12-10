/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.internal.cdi;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicInteger;
import com.oracle.coherence.concurrent.atomic.AsyncLocalAtomicInteger;
import com.oracle.coherence.concurrent.atomic.AsyncRemoteAtomicInteger;
import com.oracle.coherence.concurrent.atomic.Atomics;

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
public class AsyncAtomicIntegerProducer
        extends AbstractAtomicProducer
    {
    /**
     * Returns either a local or remote {@link AsyncAtomicInteger} for the provided {@link InjectionPoint}.
     * <p>
     * If the injection point is annotated with the {@link Remote} qualifier a remote
     * {@link AsyncAtomicInteger} will be returned, otherwise a local {@link AsyncAtomicInteger}
     * will be returned.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local or remote {@link AsyncAtomicInteger} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Remote
    AsyncAtomicInteger getAtomicInteger(InjectionPoint ip)
        {
        if (ip.getQualifiers().contains(Remote.Literal.INSTANCE))
            {
            return getRemoteAtomicInteger(ip);
            }
        return getLocalAtomicInteger(ip);
        }

    /**
     * Returns an {@link AsyncLocalAtomicInteger} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link AsyncLocalAtomicInteger} for the provided {@link InjectionPoint}
     */
    @Produces
    AsyncLocalAtomicInteger getUnqualifiedLocalAtomicInteger(InjectionPoint ip)
        {
        return Atomics.localAtomicInteger(getName(ip)).async();
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
     * Returns an {@link AsyncRemoteAtomicInteger} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link AsyncRemoteAtomicInteger} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(AsyncRemoteAtomicInteger.class)
    AsyncRemoteAtomicInteger getUnqualifiedRemoteAtomicInteger(InjectionPoint ip)
        {
        return Atomics.remoteAtomicInteger(getName(ip)).async();
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
