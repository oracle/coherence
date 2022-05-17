/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.internal.cdi;

import com.oracle.coherence.concurrent.atomic.Atomics;
import com.oracle.coherence.concurrent.atomic.AtomicBoolean;
import com.oracle.coherence.concurrent.atomic.LocalAtomicBoolean;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicBoolean;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;

import javax.enterprise.inject.spi.InjectionPoint;

/**
 * CDI producers for {@link AtomicBoolean} values.
 *
 * @author Aleks Seovic  2020.12.07
 * @since 21.12
 */
@ApplicationScoped
public class AtomicBooleanProducer
        extends AbstractAtomicProducer
    {
    /**
     * Returns a local or remote {@link AtomicBoolean} for the provided {@link InjectionPoint}.
     * <p>
     * If the injection point is annotated with the {@link Remote} qualifier a remote
     * {@link AtomicBoolean} will be returned, otherwise a local {@link AtomicBoolean}
     * will be returned.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local or remote {@link AtomicBoolean} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Remote
    AtomicBoolean getAtomicBoolean(InjectionPoint ip)
        {
        if (ip.getQualifiers().contains(Remote.Literal.INSTANCE))
            {
            return getRemoteAtomicBoolean(ip);
            }
        return getLocalAtomicBoolean(ip);
        }

    /**
     * Returns an {@link LocalAtomicBoolean} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link LocalAtomicBoolean} for the provided {@link InjectionPoint}
     */
    @Produces
    LocalAtomicBoolean getUnqualifiedLocalAtomicLong(InjectionPoint ip)
        {
        return Atomics.localAtomicBoolean(getName(ip));
        }

    /**
     * Returns a {@link LocalAtomicBoolean} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link LocalAtomicBoolean} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(LocalAtomicBoolean.class)
    LocalAtomicBoolean getLocalAtomicBoolean(InjectionPoint ip)
        {
        return Atomics.localAtomicBoolean(getName(ip));
        }

    /**
     * Returns an {@link RemoteAtomicBoolean} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link RemoteAtomicBoolean} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(RemoteAtomicBoolean.class)
    RemoteAtomicBoolean getUnqualifiedRemoteAtomicLong(InjectionPoint ip)
        {
        return Atomics.remoteAtomicBoolean(getName(ip));
        }

    /**
     * Returns a {@link RemoteAtomicBoolean} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link RemoteAtomicBoolean} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(RemoteAtomicBoolean.class)
    RemoteAtomicBoolean getRemoteAtomicBoolean(InjectionPoint ip)
        {
        return Atomics.remoteAtomicBoolean(getName(ip));
        }
    }
