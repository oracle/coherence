/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.cdi;

import com.oracle.coherence.concurrent.atomic.Atomics;
import com.oracle.coherence.concurrent.atomic.LocalAtomicReference;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicReference;
import com.oracle.coherence.concurrent.atomic.AtomicReference;

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
class AtomicReferenceProducer
        extends AbstractAtomicProducer
    {
    /**
     * Returns a {@link AtomicReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link AtomicReference} for the provided {@link InjectionPoint}
     */
    @Produces
    <V> AtomicReference<V> getUnqualifiedAtomicReference(InjectionPoint ip)
        {
        return getLocalAtomicReference(ip);
        }

    /**
     * Returns a {@link AtomicReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link AtomicReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    <V> AtomicReference<V> getAtomicReference(InjectionPoint ip)
        {
        return getLocalAtomicReference(ip);
        }

    /**
     * Returns a {@link AtomicReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link AtomicReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Remote
    <V> AtomicReference<V> getAtomicReferenceWithRemoteAnnotation(InjectionPoint ip)
        {
        return getRemoteAtomicReference(ip);
        }

    /**
     * Returns a {@link LocalAtomicReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link LocalAtomicReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(LocalAtomicReference.class)
    <V> LocalAtomicReference<V> getUnqualifiedLocalAtomicReference(InjectionPoint ip)
        {
        return getLocalAtomicReference(ip);
        }

    /**
     * Returns a {@link LocalAtomicReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link LocalAtomicReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(LocalAtomicReference.class)
    <V> LocalAtomicReference<V> getLocalAtomicReference(InjectionPoint ip)
        {
        return Atomics.getLocalAtomicReference(getName(ip));
        }

    /**
     * Returns a {@link RemoteAtomicReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
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
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link RemoteAtomicReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(RemoteAtomicReference.class)
    <V> RemoteAtomicReference<V> getRemoteAtomicReference(InjectionPoint ip)
        {
        return Atomics.getRemoteAtomicReference(getName(ip));
        }
    }
