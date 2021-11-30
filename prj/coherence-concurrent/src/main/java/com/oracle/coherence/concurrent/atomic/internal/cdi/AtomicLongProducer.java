/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.internal.cdi;

import com.oracle.coherence.concurrent.atomic.AtomicLong;
import com.oracle.coherence.concurrent.atomic.Atomics;
import com.oracle.coherence.concurrent.atomic.LocalAtomicLong;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicLong;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;

import javax.enterprise.inject.spi.InjectionPoint;

/**
 * CDI producers for {@link AtomicLong} values.
 *
 * @author Aleks Seovic  2020.12.07
 * @since 21.12
 */
@ApplicationScoped
class AtomicLongProducer
        extends AbstractAtomicProducer
    {
    /**
     * Returns a local {@link AtomicLong} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link AtomicLong} for the provided {@link InjectionPoint}
     */
    @Produces
    AtomicLong getUnqualifiedAtomicLong(InjectionPoint ip)
        {
        return getLocalAtomicLong(ip);
        }

    /**
     * Returns a local {@link AtomicLong} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link AtomicLong} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    AtomicLong getAtomicLong(InjectionPoint ip)
        {
        return getLocalAtomicLong(ip);
        }

    /**
     * Returns a local {@link AtomicLong} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link AtomicLong} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Remote
    AtomicLong getAtomicLongWithRemoteAnnotation(InjectionPoint ip)
        {
        return getRemoteAtomicLong(ip);
        }

    /**
     * Returns a local {@link LocalAtomicLong} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link LocalAtomicLong} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(LocalAtomicLong.class)
    LocalAtomicLong getUnqualifiedLocalAtomicLong(InjectionPoint ip)
        {
        return getLocalAtomicLong(ip);
        }

    /**
     * Returns a local {@link LocalAtomicLong} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link LocalAtomicLong} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(LocalAtomicLong.class)
    LocalAtomicLong getLocalAtomicLong(InjectionPoint ip)
        {
        return Atomics.localAtomicLong(getName(ip));
        }

    /**
     * Returns a local {@link RemoteAtomicLong} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link RemoteAtomicLong} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(RemoteAtomicLong.class)
    RemoteAtomicLong getUnqualifiedRemoteAtomicLong(InjectionPoint ip)
        {
        return getRemoteAtomicLong(ip);
        }

    /**
     * Returns a local {@link RemoteAtomicLong} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link RemoteAtomicLong} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(RemoteAtomicLong.class)
    RemoteAtomicLong getRemoteAtomicLong(InjectionPoint ip)
        {
        return Atomics.remoteAtomicLong(getName(ip));
        }
    }
