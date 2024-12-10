/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.internal.cdi;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicMarkableReference;
import com.oracle.coherence.concurrent.atomic.AsyncLocalAtomicMarkableReference;
import com.oracle.coherence.concurrent.atomic.AsyncRemoteAtomicMarkableReference;
import com.oracle.coherence.concurrent.atomic.Atomics;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import com.oracle.coherence.concurrent.atomic.LocalAtomicMarkableReference;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicMarkableReference;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;

import javax.enterprise.inject.spi.InjectionPoint;

/**
 * CDI producers for {@link AsyncAtomicMarkableReference} values.
 *
 * @author Aleks Seovic  2020.12.09
 * @since 21.12
 */
@ApplicationScoped
public class AsyncAtomicMarkableReferenceProducer
        extends AbstractAtomicProducer
    {
    /**
     * Returns either a local or remote {@link AsyncAtomicMarkableReference} for the provided {@link InjectionPoint}.
     * <p>
     * If the injection point is annotated with the {@link Remote} qualifier a remote
     * {@link AsyncAtomicMarkableReference} will be returned, otherwise a local {@link AsyncAtomicMarkableReference}
     * will be returned.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local or remote {@link AsyncAtomicMarkableReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Remote
    <V> AsyncAtomicMarkableReference<V> getAtomicMarkableReference(InjectionPoint ip)
        {
        if (ip.getQualifiers().contains(Remote.Literal.INSTANCE))
            {
            return getRemoteAtomicMarkableReference(ip);
            }
        return getLocalAtomicMarkableReference(ip);
        }

    /**
     * Returns an {@link AsyncLocalAtomicMarkableReference} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link AsyncLocalAtomicMarkableReference} for the provided {@link InjectionPoint}
     */
    @Produces
    <V> AsyncLocalAtomicMarkableReference<V> getUnqualifiedLocalAtomicMarkableReference(InjectionPoint ip)
        {
        return getLocalAtomicMarkableReference(ip);
        }

    /**
     * Returns a {@link AsyncLocalAtomicMarkableReference} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link AsyncLocalAtomicMarkableReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(AsyncLocalAtomicMarkableReference.class)
    <V> AsyncLocalAtomicMarkableReference<V> getLocalAtomicMarkableReference(InjectionPoint ip)
        {
        LocalAtomicMarkableReference<V> ref = Atomics.localAtomicMarkableReference(getName(ip));
        return ref.async();
        }

    /**
     * Returns an {@link AsyncRemoteAtomicMarkableReference} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link AsyncRemoteAtomicMarkableReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(AsyncRemoteAtomicMarkableReference.class)
    <V> AsyncRemoteAtomicMarkableReference<V> getUnqualifiedRemoteAtomicMarkableReference(InjectionPoint ip)
        {
        return getRemoteAtomicMarkableReference(ip);
        }

    /**
     * Returns a {@link AsyncRemoteAtomicMarkableReference} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link AsyncRemoteAtomicMarkableReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(AsyncRemoteAtomicMarkableReference.class)
    <V> AsyncRemoteAtomicMarkableReference<V> getRemoteAtomicMarkableReference(InjectionPoint ip)
        {
        RemoteAtomicMarkableReference<V> ref = Atomics.remoteAtomicMarkableReference(getName(ip));
        return ref.async();
        }
    }
