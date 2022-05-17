/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.internal.cdi;

import com.oracle.coherence.concurrent.atomic.Atomics;
import com.oracle.coherence.concurrent.atomic.AtomicReference;
import com.oracle.coherence.concurrent.atomic.LocalAtomicReference;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicReference;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;

import javax.enterprise.inject.spi.InjectionPoint;

/**
 * CDI producers for {@link AtomicReference} values.
 *
 * @author Aleks Seovic  2020.12.08
 * @since 21.12
 */
@ApplicationScoped
public class AtomicReferenceProducer
        extends AbstractAtomicProducer
    {
    /**
     * Returns either a local or remote {@link AtomicReference} for the provided {@link InjectionPoint}.
     * <p>
     * If the injection point is annotated with the {@link Remote} qualifier a remote
     * {@link AtomicReference} will be returned, otherwise a local {@link AtomicReference}
     * will be returned.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local or remote {@link AtomicReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Remote
    <V> AtomicReference<V> getAtomicReference(InjectionPoint ip)
        {
        if (ip.getQualifiers().contains(Remote.Literal.INSTANCE))
            {
            return getRemoteAtomicReference(ip);
            }
        return getLocalAtomicReference(ip);
        }

    /**
     * Returns an {@link LocalAtomicReference} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link LocalAtomicReference} for the provided {@link InjectionPoint}
     */
    @Produces
    <V> LocalAtomicReference<V> getUnqualifiedLocalAtomicReference(InjectionPoint ip)
        {
        return getLocalAtomicReference(ip);
        }

    /**
     * Returns a {@link LocalAtomicReference} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link LocalAtomicReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(LocalAtomicReference.class)
    <V> LocalAtomicReference<V> getLocalAtomicReference(InjectionPoint ip)
        {
        return Atomics.localAtomicReference(getName(ip));
        }

    /**
     * Returns an {@link RemoteAtomicReference} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link RemoteAtomicReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(RemoteAtomicReference.class)
    <V> RemoteAtomicReference<V> getUnqualifiedRemoteAtomicReference(InjectionPoint ip)
        {
        return getRemoteAtomicReference(ip);
        }

    /**
     * Returns a {@link RemoteAtomicReference} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link RemoteAtomicReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(RemoteAtomicReference.class)
    <V> RemoteAtomicReference<V> getRemoteAtomicReference(InjectionPoint ip)
        {
        return Atomics.remoteAtomicReference(getName(ip));
        }
    }
