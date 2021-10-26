/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.cdi;

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
class AsyncAtomicLongProducer
        extends AbstractAtomicProducer
    {
    /**
     * Returns a local {@link AsyncAtomicLong} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link AsyncAtomicLong} for the provided {@link InjectionPoint}
     */
    @Produces
    AsyncAtomicLong getUnqualifiedAtomicLong(InjectionPoint ip)
        {
        return getLocalAtomicLong(ip);
        }

    /**
     * Returns a local {@link AsyncAtomicLong} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link AsyncAtomicLong} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    AsyncAtomicLong getAtomicLong(InjectionPoint ip)
        {
        return getLocalAtomicLong(ip);
        }

    /**
     * Returns a remote {@link AsyncAtomicLong} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a remote {@link AsyncAtomicLong} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Remote
    AsyncAtomicLong getAtomicLongWithRemoteAnnotation(InjectionPoint ip)
        {
        return getRemoteAtomicLong(ip);
        }

    /**
     * Returns a {@link AsyncLocalAtomicLong} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link AsyncLocalAtomicLong} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(AsyncLocalAtomicLong.class)
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
        return Atomics.getLocalAtomicLong(getName(ip)).async();
        }

    /**
     * Returns a {@link AsyncRemoteAtomicLong} for the provided {@link InjectionPoint}.
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
        return Atomics.getRemoteAtomicLong(getName(ip)).async();
        }
    }
