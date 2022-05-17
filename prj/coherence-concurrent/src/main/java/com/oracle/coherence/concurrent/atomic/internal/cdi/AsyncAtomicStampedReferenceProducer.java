/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.internal.cdi;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicStampedReference;
import com.oracle.coherence.concurrent.atomic.AsyncLocalAtomicStampedReference;
import com.oracle.coherence.concurrent.atomic.AsyncRemoteAtomicStampedReference;
import com.oracle.coherence.concurrent.atomic.Atomics;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import com.oracle.coherence.concurrent.atomic.LocalAtomicStampedReference;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicStampedReference;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;

import javax.enterprise.inject.spi.InjectionPoint;

/**
 * CDI producers for {@link AsyncAtomicStampedReference} values.
 *
 * @author Aleks Seovic  2020.12.09
 * @since 21.12
 */
@ApplicationScoped
public class AsyncAtomicStampedReferenceProducer
        extends AbstractAtomicProducer
    {
    /**
     * Returns either a local or remote {@link AsyncAtomicStampedReference} for the provided {@link InjectionPoint}.
     * <p>
     * If the injection point is annotated with the {@link Remote} qualifier a remote
     * {@link AsyncAtomicStampedReference} will be returned, otherwise a local {@link AsyncAtomicStampedReference}
     * will be returned.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local or remote {@link AsyncAtomicStampedReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Remote
    <V> AsyncAtomicStampedReference<V> getAtomicStampedReference(InjectionPoint ip)
        {
        if (ip.getQualifiers().contains(Remote.Literal.INSTANCE))
            {
            return getRemoteAtomicStampedReference(ip);
            }
        return getLocalAtomicStampedReference(ip);
        }

    /**
     * Returns an {@link AsyncLocalAtomicStampedReference} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link AsyncLocalAtomicStampedReference} for the provided {@link InjectionPoint}
     */
    @Produces
    <V> AsyncLocalAtomicStampedReference<V> getUnqualifiedLocalAtomicStampedReference(InjectionPoint ip)
        {
        return getLocalAtomicStampedReference(ip);
        }

    /**
     * Returns a {@link AsyncLocalAtomicStampedReference} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link AsyncLocalAtomicStampedReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(AsyncLocalAtomicStampedReference.class)
    <V> AsyncLocalAtomicStampedReference<V> getLocalAtomicStampedReference(InjectionPoint ip)
        {
        LocalAtomicStampedReference<V> ref = Atomics.localAtomicStampedReference(getName(ip));
        return ref.async();
        }

    /**
     * Returns an {@link AsyncRemoteAtomicStampedReference} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link AsyncRemoteAtomicStampedReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(AsyncRemoteAtomicStampedReference.class)
    <V> AsyncRemoteAtomicStampedReference<V> getUnqualifiedRemoteAtomicStampedReference(InjectionPoint ip)
        {
        return getRemoteAtomicStampedReference(ip);
        }

    /**
     * Returns a {@link AsyncRemoteAtomicStampedReference} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link AsyncRemoteAtomicStampedReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(AsyncRemoteAtomicStampedReference.class)
    <V> AsyncRemoteAtomicStampedReference<V> getRemoteAtomicStampedReference(InjectionPoint ip)
        {
        RemoteAtomicStampedReference<V> ref = Atomics.remoteAtomicStampedReference(getName(ip));
        return ref.async();
        }
    }
