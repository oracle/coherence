/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.internal.cdi;

import com.oracle.coherence.concurrent.atomic.Atomics;
import com.oracle.coherence.concurrent.atomic.AtomicInteger;
import com.oracle.coherence.concurrent.atomic.LocalAtomicInteger;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicInteger;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;

import javax.enterprise.inject.spi.InjectionPoint;

/**
 * CDI producers for {@link AtomicInteger} values.
 *
 * @author Aleks Seovic  2020.12.07
 * @since 21.12
 */
@ApplicationScoped
public class AtomicIntegerProducer
        extends AbstractAtomicProducer
    {
    /**
     * Returns either a local or remote {@link AtomicInteger} for the provided {@link InjectionPoint}.
     * <p>
     * If the injection point is annotated with the {@link Remote} qualifier a remote
     * {@link AtomicInteger} will be returned, otherwise a local {@link AtomicInteger}
     * will be returned.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local or remote {@link AtomicInteger} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Remote
    AtomicInteger getAtomicInteger(InjectionPoint ip)
        {
        if (ip.getQualifiers().contains(Remote.Literal.INSTANCE))
            {
            return getRemoteAtomicInteger(ip);
            }
        return getLocalAtomicInteger(ip);
        }

    /**
     * Returns an {@link LocalAtomicInteger} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link LocalAtomicInteger} for the provided {@link InjectionPoint}
     */
    @Produces
    LocalAtomicInteger getUnqualifiedLocalAtomicInteger(InjectionPoint ip)
        {
        return Atomics.localAtomicInteger(getName(ip));
        }

    /**
     * Returns a {@link LocalAtomicInteger} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link LocalAtomicInteger} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(LocalAtomicInteger.class)
    LocalAtomicInteger getLocalAtomicInteger(InjectionPoint ip)
        {
        return Atomics.localAtomicInteger(getName(ip));
        }

    /**
     * Returns an {@link RemoteAtomicInteger} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link RemoteAtomicInteger} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(RemoteAtomicInteger.class)
    RemoteAtomicInteger getUnqualifiedRemoteAtomicInteger(InjectionPoint ip)
        {
        return Atomics.remoteAtomicInteger(getName(ip));
        }

    /**
     * Returns a {@link RemoteAtomicInteger} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link RemoteAtomicInteger} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(RemoteAtomicInteger.class)
    RemoteAtomicInteger getRemoteAtomicInteger(InjectionPoint ip)
        {
        return Atomics.remoteAtomicInteger(getName(ip));
        }
    }
