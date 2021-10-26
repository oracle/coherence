/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.cdi;

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
class AtomicIntegerProducer
        extends AbstractAtomicProducer
    {
    /**
     * Returns a local {@link AtomicInteger} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link AtomicInteger} for the provided {@link InjectionPoint}
     */
    @Produces
    AtomicInteger getUnqualifiedAtomicInteger(InjectionPoint ip)
        {
        return getLocalAtomicInteger(ip);
        }

    /**
     * Returns a local {@link AtomicInteger} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link AtomicInteger} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    AtomicInteger getAtomicInteger(InjectionPoint ip)
        {
        return getLocalAtomicInteger(ip);
        }

    /**
     * Returns a local {@link AtomicInteger} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link AtomicInteger} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Remote
    AtomicInteger getAtomicIntegerWithRemoteAnnotation(InjectionPoint ip)
        {
        return getRemoteAtomicInteger(ip);
        }

    /**
     * Returns a local {@link LocalAtomicInteger} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link LocalAtomicInteger} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(LocalAtomicInteger.class)
    LocalAtomicInteger getUnqualifiedLocalAtomicInteger(InjectionPoint ip)
        {
        return getLocalAtomicInteger(ip);
        }

    /**
     * Returns a local {@link LocalAtomicInteger} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link LocalAtomicInteger} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(LocalAtomicInteger.class)
    LocalAtomicInteger getLocalAtomicInteger(InjectionPoint ip)
        {
        return Atomics.getLocalAtomicInteger(getName(ip));
        }

    /**
     * Returns a local {@link RemoteAtomicInteger} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link RemoteAtomicInteger} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(RemoteAtomicInteger.class)
    RemoteAtomicInteger getUnqualifiedRemoteAtomicInteger(InjectionPoint ip)
        {
        return getRemoteAtomicInteger(ip);
        }

    /**
     * Returns a local {@link RemoteAtomicInteger} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link RemoteAtomicInteger} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(RemoteAtomicInteger.class)
    RemoteAtomicInteger getRemoteAtomicInteger(InjectionPoint ip)
        {
        return Atomics.getRemoteAtomicInteger(getName(ip));
        }
    }
