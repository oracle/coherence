/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.internal.cdi;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicReference;
import com.oracle.coherence.concurrent.atomic.AsyncLocalAtomicReference;
import com.oracle.coherence.concurrent.atomic.AsyncRemoteAtomicReference;
import com.oracle.coherence.concurrent.atomic.Atomics;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import com.oracle.coherence.concurrent.atomic.LocalAtomicReference;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicReference;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;

import javax.enterprise.inject.spi.InjectionPoint;

/**
 * CDI producers for {@link AsyncAtomicReference} values.
 *
 * @author Aleks Seovic  2020.12.08
 * @since 21.12
 */
@ApplicationScoped
public class AsyncAtomicReferenceProducer
        extends AbstractAtomicProducer
    {
    /**
     * Returns either a local or remote {@link AsyncAtomicReference} for the provided {@link InjectionPoint}.
     * <p>
     * If the injection point is annotated with the {@link Remote} qualifier a remote
     * {@link AsyncAtomicReference} will be returned, otherwise a local {@link AsyncAtomicReference}
     * will be returned.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local or remote {@link AsyncAtomicReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Remote
    <V> AsyncAtomicReference<V> getAtomicReference(InjectionPoint ip)
        {
        if (ip.getQualifiers().contains(Remote.Literal.INSTANCE))
            {
            return getRemoteAtomicReference(ip);
            }
        return getLocalAtomicReference(ip);
        }

    /**
     * Returns an {@link AsyncLocalAtomicReference} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link AsyncLocalAtomicReference} for the provided {@link InjectionPoint}
     */
    @Produces
    <V> AsyncLocalAtomicReference<V> getUnqualifiedLocalAtomicReference(InjectionPoint ip)
        {
        return getLocalAtomicReference(ip);
        }

    /**
     * Returns a {@link AsyncLocalAtomicReference} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link AsyncLocalAtomicReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(AsyncLocalAtomicReference.class)
    <V> AsyncLocalAtomicReference<V> getLocalAtomicReference(InjectionPoint ip)
        {
        LocalAtomicReference<V> ref = Atomics.localAtomicReference(getName(ip));
        return ref.async();
        }

    /**
     * Returns an {@link AsyncRemoteAtomicReference} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link AsyncRemoteAtomicReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(AsyncRemoteAtomicReference.class)
    <V> AsyncRemoteAtomicReference<V> getUnqualifiedRemoteAtomicReference(InjectionPoint ip)
        {
        return getRemoteAtomicReference(ip);
        }

    /**
     * Returns a {@link AsyncRemoteAtomicReference} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link AsyncRemoteAtomicReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(AsyncRemoteAtomicReference.class)
    <V> AsyncRemoteAtomicReference<V> getRemoteAtomicReference(InjectionPoint ip)
        {
        RemoteAtomicReference<V> ref = Atomics.remoteAtomicReference(getName(ip));
        return ref.async();
        }
    }
