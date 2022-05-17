/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.internal.cdi;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicBoolean;
import com.oracle.coherence.concurrent.atomic.AsyncLocalAtomicBoolean;
import com.oracle.coherence.concurrent.atomic.AsyncRemoteAtomicBoolean;
import com.oracle.coherence.concurrent.atomic.Atomics;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;

import javax.enterprise.inject.spi.InjectionPoint;

/**
 * CDI producers for {@link AsyncAtomicBoolean} values.
 *
 * @author Aleks Seovic  2020.12.07
 * @since 21.12
 */
@ApplicationScoped
public class AsyncAtomicBooleanProducer
        extends AbstractAtomicProducer
    {
    /**
     * Returns a local or remote {@link AsyncAtomicBoolean} for the provided {@link InjectionPoint}.
     * <p>
     * If the injection point is annotated with the {@link Remote} qualifier a remote
     * {@link AsyncAtomicBoolean} will be returned, otherwise a local {@link AsyncAtomicBoolean}
     * will be returned.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local or remote {@link AsyncAtomicBoolean} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Remote
    AsyncAtomicBoolean getAtomicBoolean(InjectionPoint ip)
        {
        if (ip.getQualifiers().contains(Remote.Literal.INSTANCE))
            {
            return getRemoteAtomicBoolean(ip);
            }
        return getLocalAtomicBoolean(ip);
        }

    /**
     * Returns an {@link AsyncLocalAtomicBoolean} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link AsyncLocalAtomicBoolean} for the provided {@link InjectionPoint}
     */
    @Produces
    AsyncLocalAtomicBoolean getUnqualifiedLocalAtomicBoolean(InjectionPoint ip)
        {
        return Atomics.localAtomicBoolean(getName(ip)).async();
        }

    /**
     * Returns a {@link AsyncLocalAtomicBoolean} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link AsyncLocalAtomicBoolean} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(AsyncLocalAtomicBoolean.class)
    AsyncLocalAtomicBoolean getLocalAtomicBoolean(InjectionPoint ip)
        {
        return Atomics.localAtomicBoolean(getName(ip)).async();
        }

    /**
     * Returns an {@link AsyncRemoteAtomicBoolean} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link AsyncRemoteAtomicBoolean} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(AsyncRemoteAtomicBoolean.class)
    AsyncRemoteAtomicBoolean getUnqualifiedRemoteAtomicBoolean(InjectionPoint ip)
        {
        return Atomics.remoteAtomicBoolean(getName(ip)).async();
        }

    /**
     * Returns a {@link AsyncRemoteAtomicBoolean} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link AsyncRemoteAtomicBoolean} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(AsyncRemoteAtomicBoolean.class)
    AsyncRemoteAtomicBoolean getRemoteAtomicBoolean(InjectionPoint ip)
        {
        return Atomics.remoteAtomicBoolean(getName(ip)).async();
        }
    }
