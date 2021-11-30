/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.internal.cdi;

import com.oracle.coherence.concurrent.atomic.AtomicStampedReference;
import com.oracle.coherence.concurrent.atomic.Atomics;
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
class AtomicStampedReferenceProducer
        extends AbstractAtomicProducer
    {
    /**
     * Returns a {@link AtomicStampedReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link AtomicStampedReference} for the provided {@link InjectionPoint}
     */
    @Produces
    <V> AtomicStampedReference<V> getUnqualifiedAtomicStampedReference(InjectionPoint ip)
        {
        return getLocalAtomicStampedReference(ip);
        }

    /**
     * Returns a {@link AtomicStampedReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link AtomicStampedReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    <V> AtomicStampedReference<V> getAtomicStampedReference(InjectionPoint ip)
        {
        return getLocalAtomicStampedReference(ip);
        }

    /**
     * Returns a {@link AtomicStampedReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link AtomicStampedReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Remote
    <V> AtomicStampedReference<V> getAtomicStampedReferenceWithRemoteAnnotation(InjectionPoint ip)
        {
        return getRemoteAtomicStampedReference(ip);
        }

    /**
     * Returns a {@link LocalAtomicStampedReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link LocalAtomicStampedReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(LocalAtomicStampedReference.class)
    <V> LocalAtomicStampedReference<V> getUnqualifiedLocalAtomicStampedReference(InjectionPoint ip)
        {
        return getLocalAtomicStampedReference(ip);
        }

    /**
     * Returns a {@link LocalAtomicStampedReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
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
     * Returns a {@link RemoteAtomicStampedReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
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
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
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
