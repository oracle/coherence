/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.internal.cdi;

import com.oracle.coherence.concurrent.atomic.Atomics;
import com.oracle.coherence.concurrent.atomic.AtomicMarkableReference;
import com.oracle.coherence.concurrent.atomic.LocalAtomicMarkableReference;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicMarkableReference;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import com.oracle.coherence.concurrent.atomic.LocalAtomicMarkableReference;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicMarkableReference;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;

import javax.enterprise.inject.spi.InjectionPoint;

/**
 * CDI producers for {@link AtomicMarkableReference} values.
 *
 * @author Aleks Seovic  2020.12.09
 * @since 21.12
 */
@ApplicationScoped
public class AtomicMarkableReferenceProducer
        extends AbstractAtomicProducer
    {
    /**
     * Returns either a local or remote {@link AtomicMarkableReference} for the provided {@link InjectionPoint}.
     * <p>
     * If the injection point is annotated with the {@link Remote} qualifier a remote
     * {@link AtomicMarkableReference} will be returned, otherwise a local {@link AtomicMarkableReference}
     * will be returned.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local or remote {@link AtomicMarkableReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Remote
    <V> AtomicMarkableReference<V> getAtomicMarkableReference(InjectionPoint ip)
        {
        if (ip.getQualifiers().contains(Remote.Literal.INSTANCE))
            {
            return getRemoteAtomicMarkableReference(ip);
            }
        return getLocalAtomicMarkableReference(ip);
        }

    /**
     * Returns an {@link LocalAtomicMarkableReference} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link LocalAtomicMarkableReference} for the provided {@link InjectionPoint}
     */
    @Produces
    <V> LocalAtomicMarkableReference<V> getUnqualifiedLocalAtomicMarkableReference(InjectionPoint ip)
        {
        return getLocalAtomicMarkableReference(ip);
        }

    /**
     * Returns a {@link LocalAtomicMarkableReference} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link LocalAtomicMarkableReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(LocalAtomicMarkableReference.class)
    <V> LocalAtomicMarkableReference<V> getLocalAtomicMarkableReference(InjectionPoint ip)
        {
        LocalAtomicMarkableReference<V> ref = Atomics.localAtomicMarkableReference(getName(ip));
        return ref;
        }

    /**
     * Returns an {@link RemoteAtomicMarkableReference} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link RemoteAtomicMarkableReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(RemoteAtomicMarkableReference.class)
    <V> RemoteAtomicMarkableReference<V> getUnqualifiedRemoteAtomicMarkableReference(InjectionPoint ip)
        {
        return getRemoteAtomicMarkableReference(ip);
        }

    /**
     * Returns a {@link RemoteAtomicMarkableReference} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link RemoteAtomicMarkableReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(RemoteAtomicMarkableReference.class)
    <V> RemoteAtomicMarkableReference<V> getRemoteAtomicMarkableReference(InjectionPoint ip)
        {
        RemoteAtomicMarkableReference<V> ref = Atomics.remoteAtomicMarkableReference(getName(ip));
        return ref;
        }
    }
