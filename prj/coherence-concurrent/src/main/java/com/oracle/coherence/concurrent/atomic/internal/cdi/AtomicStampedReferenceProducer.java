/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.internal.cdi;

import com.oracle.coherence.concurrent.atomic.Atomics;
import com.oracle.coherence.concurrent.atomic.AtomicStampedReference;
import com.oracle.coherence.concurrent.atomic.LocalAtomicStampedReference;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicStampedReference;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;

import javax.enterprise.inject.spi.InjectionPoint;

/**
 * CDI producers for {@link AtomicStampedReference} values.
 *
 * @author Aleks Seovic  2020.12.09
 * @since 21.12
 */
@ApplicationScoped
public class AtomicStampedReferenceProducer
        extends AbstractAtomicProducer
    {
    /**
     * Returns either a local or remote {@link AtomicStampedReference} for the provided {@link InjectionPoint}.
     * <p>
     * If the injection point is annotated with the {@link Remote} qualifier a remote
     * {@link AtomicStampedReference} will be returned, otherwise a local {@link AtomicStampedReference}
     * will be returned.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local or remote {@link AtomicStampedReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Remote
    <V> AtomicStampedReference<V> getAtomicStampedReference(InjectionPoint ip)
        {
        if (ip.getQualifiers().contains(Remote.Literal.INSTANCE))
            {
            return getRemoteAtomicStampedReference(ip);
            }
        return getLocalAtomicStampedReference(ip);
        }

    /**
     * Returns an {@link LocalAtomicStampedReference} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link LocalAtomicStampedReference} for the provided {@link InjectionPoint}
     */
    @Produces
    <V> LocalAtomicStampedReference<V> getUnqualifiedLocalAtomicStampedReference(InjectionPoint ip)
        {
        return getLocalAtomicStampedReference(ip);
        }

    /**
     * Returns a {@link LocalAtomicStampedReference} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link LocalAtomicStampedReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(LocalAtomicStampedReference.class)
    <V> LocalAtomicStampedReference<V> getLocalAtomicStampedReference(InjectionPoint ip)
        {
        return Atomics.localAtomicStampedReference(getName(ip));
        }

    /**
     * Returns an {@link RemoteAtomicStampedReference} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link RemoteAtomicStampedReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(RemoteAtomicStampedReference.class)
    <V> RemoteAtomicStampedReference<V> getUnqualifiedRemoteAtomicStampedReference(InjectionPoint ip)
        {
        return getRemoteAtomicStampedReference(ip);
        }

    /**
     * Returns a {@link RemoteAtomicStampedReference} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link RemoteAtomicStampedReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(RemoteAtomicStampedReference.class)
    <V> RemoteAtomicStampedReference<V> getRemoteAtomicStampedReference(InjectionPoint ip)
        {
        return Atomics.remoteAtomicStampedReference(getName(ip));
        }
    }
