/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.cdi;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicStampedReference;
import com.oracle.coherence.concurrent.atomic.AsyncRemoteAtomicStampedReference;
import com.oracle.coherence.concurrent.atomic.Atomics;
import com.oracle.coherence.concurrent.atomic.AsyncLocalAtomicStampedReference;
import com.oracle.coherence.concurrent.atomic.LocalAtomicStampedReference;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicStampedReference;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 * CDI producers for {@link AsyncAtomicStampedReference} values.
 *
 * @author Aleks Seovic  2020.12.09
 * @since 21.12
 */
@ApplicationScoped
class AsyncAtomicStampedReferenceProducer
        extends AbstractAtomicProducer
    {
    /**
     * Returns a {@link AsyncAtomicStampedReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link AsyncAtomicStampedReference} for the provided {@link InjectionPoint}
     */
    @Produces
    <V> AsyncAtomicStampedReference<V> getUnqualifiedAtomicStampedReference(InjectionPoint ip)
        {
        return getLocalAtomicStampedReference(ip);
        }

    /**
     * Returns a {@link AsyncAtomicStampedReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link AsyncAtomicStampedReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    <V> AsyncAtomicStampedReference<V> getAtomicStampedReference(InjectionPoint ip)
        {
        return getLocalAtomicStampedReference(ip);
        }

    /**
     * Returns a {@link AsyncAtomicStampedReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link AsyncAtomicStampedReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Remote
    <V> AsyncAtomicStampedReference<V> getAtomicStampedReferenceWithRemoteAnnotation(InjectionPoint ip)
        {
        return getRemoteAtomicStampedReference(ip);
        }

    /**
     * Returns a {@link AsyncLocalAtomicStampedReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link AsyncLocalAtomicStampedReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(AsyncLocalAtomicStampedReference.class)
    <V> AsyncLocalAtomicStampedReference<V> getUnqualifiedLocalAtomicStampedReference(InjectionPoint ip)
        {
        return getLocalAtomicStampedReference(ip);
        }

    /**
     * Returns a {@link AsyncLocalAtomicStampedReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link AsyncLocalAtomicStampedReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(AsyncLocalAtomicStampedReference.class)
    <V> AsyncLocalAtomicStampedReference<V> getLocalAtomicStampedReference(InjectionPoint ip)
        {
        LocalAtomicStampedReference<V> ref = Atomics.getLocalAtomicStampedReference(getName(ip));
        return ref.async();
        }

    /**
     * Returns a {@link AsyncRemoteAtomicStampedReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link AsyncRemoteAtomicStampedReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(AsyncRemoteAtomicStampedReference.class)
    <V> AsyncRemoteAtomicStampedReference<V> getUnqualifiedRemoteAtomicStampedReference(InjectionPoint ip)
        {
        return getRemoteAtomicStampedReference(ip);
        }

    /**
     * Returns a {@link AsyncRemoteAtomicStampedReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link AsyncRemoteAtomicStampedReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(AsyncRemoteAtomicStampedReference.class)
    <V> AsyncRemoteAtomicStampedReference<V> getRemoteAtomicStampedReference(InjectionPoint ip)
        {
        RemoteAtomicStampedReference<V> ref = Atomics.getRemoteAtomicStampedReference(getName(ip));
        return ref.async();
        }
    }
