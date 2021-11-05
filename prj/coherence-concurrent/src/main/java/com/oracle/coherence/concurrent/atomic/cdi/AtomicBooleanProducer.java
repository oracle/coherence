/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.cdi;

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
class AtomicBooleanProducer
        extends AbstractAtomicProducer
    {
    /**
     * Returns a local {@link AtomicBoolean} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link AtomicBoolean} for the provided {@link InjectionPoint}
     */
    @Produces
    AtomicBoolean getUnqualifiedAtomicBoolean(InjectionPoint ip)
        {
        return getLocalAtomicBoolean(ip);
        }

    /**
     * Returns a local {@link AtomicBoolean} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link AtomicBoolean} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    AtomicBoolean getAtomicBoolean(InjectionPoint ip)
        {
        return getLocalAtomicBoolean(ip);
        }

    /**
     * Returns a local {@link AtomicBoolean} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link AtomicBoolean} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Remote
    AtomicBoolean getAtomicBooleanWithRemoteAnnotation(InjectionPoint ip)
        {
        return getRemoteAtomicBoolean(ip);
        }

    /**
     * Returns a local {@link LocalAtomicBoolean} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link LocalAtomicBoolean} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(LocalAtomicBoolean.class)
    LocalAtomicBoolean getUnqualifiedLocalAtomicBoolean(InjectionPoint ip)
        {
        return getLocalAtomicBoolean(ip);
        }

    /**
     * Returns a local {@link LocalAtomicBoolean} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link LocalAtomicBoolean} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(LocalAtomicBoolean.class)
    LocalAtomicBoolean getLocalAtomicBoolean(InjectionPoint ip)
        {
        return Atomics.getLocalAtomicBoolean(getName(ip));
        }

    /**
     * Returns a local {@link RemoteAtomicBoolean} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link RemoteAtomicBoolean} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(RemoteAtomicBoolean.class)
    RemoteAtomicBoolean getUnqualifiedRemoteAtomicBoolean(InjectionPoint ip)
        {
        return getRemoteAtomicBoolean(ip);
        }

    /**
     * Returns a local {@link RemoteAtomicBoolean} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link RemoteAtomicBoolean} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(RemoteAtomicBoolean.class)
    RemoteAtomicBoolean getRemoteAtomicBoolean(InjectionPoint ip)
        {
        return Atomics.getRemoteAtomicBoolean(getName(ip));
        }
    }
