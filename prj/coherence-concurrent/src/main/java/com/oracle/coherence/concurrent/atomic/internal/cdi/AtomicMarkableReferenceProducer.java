/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.internal.cdi;

import com.oracle.coherence.concurrent.atomic.AtomicMarkableReference;
import com.oracle.coherence.concurrent.atomic.Atomics;
import com.oracle.coherence.concurrent.atomic.LocalAtomicMarkableReference;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicMarkableReference;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

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
class AtomicMarkableReferenceProducer
        extends AbstractAtomicProducer
    {
    /**
     * Returns a local {@link AtomicMarkableReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a local {@link AtomicMarkableReference} for the provided {@link InjectionPoint}
     */
    @Produces
    <V> AtomicMarkableReference<V> getUnqualifiedAtomicMarkableReference(InjectionPoint ip)
        {
        return getLocalAtomicMarkableReference(ip);
        }

    /**
     * Returns a local {@link AtomicMarkableReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a local {@link AtomicMarkableReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    <V> AtomicMarkableReference<V> getAtomicMarkableReference(InjectionPoint ip)
        {
        return getLocalAtomicMarkableReference(ip);
        }

    /**
     * Returns a local {@link AtomicMarkableReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a local {@link AtomicMarkableReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Remote
    <V> AtomicMarkableReference<V> getAtomicMarkableReferenceWithRemoteAnnotation(InjectionPoint ip)
        {
        return getRemoteAtomicMarkableReference(ip);
        }

    /**
     * Returns a local {@link LocalAtomicMarkableReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a local {@link LocalAtomicMarkableReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(LocalAtomicMarkableReference.class)
    <V> LocalAtomicMarkableReference<V> getUnqualifiedLocalAtomicMarkableReference(InjectionPoint ip)
        {
        return getLocalAtomicMarkableReference(ip);
        }

    /**
     * Returns a local {@link LocalAtomicMarkableReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a local {@link LocalAtomicMarkableReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(LocalAtomicMarkableReference.class)
    <V> LocalAtomicMarkableReference<V> getLocalAtomicMarkableReference(InjectionPoint ip)
        {
        return Atomics.localAtomicMarkableReference(getName(ip));
        }

    /**
     * Returns a local {@link RemoteAtomicMarkableReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a local {@link RemoteAtomicMarkableReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(RemoteAtomicMarkableReference.class)
    <V> RemoteAtomicMarkableReference<V> getUnqualifiedRemoteAtomicMarkableReference(InjectionPoint ip)
        {
        return getRemoteAtomicMarkableReference(ip);
        }

    /**
     * Returns a local {@link RemoteAtomicMarkableReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a local {@link RemoteAtomicMarkableReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(RemoteAtomicMarkableReference.class)
    <V> RemoteAtomicMarkableReference<V> getRemoteAtomicMarkableReference(InjectionPoint ip)
        {
        return Atomics.remoteAtomicMarkableReference(getName(ip));
        }
    }
