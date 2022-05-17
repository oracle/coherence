/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.internal.cdi;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicLong;
import com.oracle.coherence.concurrent.atomic.AsyncLocalAtomicLong;
import com.oracle.coherence.concurrent.atomic.AsyncRemoteAtomicLong;
import com.oracle.coherence.concurrent.atomic.Atomics;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;

import javax.enterprise.inject.spi.InjectionPoint;

/**
 * CDI producers for {@link AsyncAtomicLong} values.
 *
 * @author Aleks Seovic  2020.12.07
 * @since 21.12
 */
@ApplicationScoped
public class AsyncAtomicLongProducer
        extends AbstractAtomicProducer
    {
    /**
     * Returns either a local or remote {@link AsyncAtomicLong} for the provided {@link InjectionPoint}.
     * <p>
     * If the injection point is annotated with the {@link Remote} qualifier a remote
     * {@link AsyncAtomicLong} will be returned, otherwise a local {@link AsyncAtomicLong}
     * will be returned.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local or remote {@link AsyncAtomicLong} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Remote
    AsyncAtomicLong getAtomicLong(InjectionPoint ip)
        {
        if (ip.getQualifiers().contains(Remote.Literal.INSTANCE))
            {
            return getRemoteAtomicLong(ip);
            }
        return getLocalAtomicLong(ip);
        }

    /**
     * Returns an {@link AsyncLocalAtomicLong} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link AsyncLocalAtomicLong} for the provided {@link InjectionPoint}
     */
    @Produces
    AsyncLocalAtomicLong getUnqualifiedLocalAtomicLong(InjectionPoint ip)
        {
        return getLocalAtomicLong(ip);
        }

    /**
     * Returns a {@link AsyncLocalAtomicLong} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link AsyncLocalAtomicLong} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(AsyncLocalAtomicLong.class)
    AsyncLocalAtomicLong getLocalAtomicLong(InjectionPoint ip)
        {
        return Atomics.localAtomicLong(getName(ip)).async();
        }

    /**
     * Returns an {@link AsyncRemoteAtomicLong} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link AsyncRemoteAtomicLong} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(AsyncRemoteAtomicLong.class)
    AsyncRemoteAtomicLong getUnqualifiedRemoteAtomicLong(InjectionPoint ip)
        {
        return getRemoteAtomicLong(ip);
        }

    /**
     * Returns a {@link AsyncRemoteAtomicLong} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link AsyncRemoteAtomicLong} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(AsyncRemoteAtomicLong.class)
    AsyncRemoteAtomicLong getRemoteAtomicLong(InjectionPoint ip)
        {
        return Atomics.remoteAtomicLong(getName(ip)).async();
        }
    }
