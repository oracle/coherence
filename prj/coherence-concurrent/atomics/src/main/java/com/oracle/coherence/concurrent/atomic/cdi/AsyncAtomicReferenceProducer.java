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
import com.oracle.coherence.concurrent.atomic.AsyncAtomicReference;
import com.oracle.coherence.concurrent.atomic.AsyncLocalAtomicReference;
import com.oracle.coherence.concurrent.atomic.AsyncRemoteAtomicReference;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;

import javax.enterprise.inject.spi.InjectionPoint;

/**
 * CDI producers for {@link AsyncAtomicReference} values.
 *
 * @author Aleks Seovic  2020.12.08
 * @since 21.12
 */
@ApplicationScoped
class AsyncAtomicReferenceProducer
        extends AbstractAtomicProducer
    {
    /**
     * Returns a {@link AsyncAtomicReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link AsyncAtomicReference} for the provided {@link InjectionPoint}
     */
    @Produces
    <V> AsyncAtomicReference<V> getUnqualifiedAtomicReference(InjectionPoint ip)
        {
        return getLocalAtomicReference(ip);
        }

    /**
     * Returns a {@link AsyncAtomicReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link AsyncAtomicReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    <V> AsyncAtomicReference<V> getAtomicReference(InjectionPoint ip)
        {
        return getLocalAtomicReference(ip);
        }

    /**
     * Returns a {@link AsyncAtomicReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link AsyncAtomicReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Remote
    <V> AsyncAtomicReference<V> getAtomicReferenceWithRemoteAnnotation(InjectionPoint ip)
        {
        return getRemoteAtomicReference(ip);
        }

    /**
     * Returns a {@link AsyncLocalAtomicReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link AsyncLocalAtomicReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(AsyncLocalAtomicReference.class)
    <V> AsyncLocalAtomicReference<V> getUnqualifiedLocalAtomicReference(InjectionPoint ip)
        {
        return getLocalAtomicReference(ip);
        }

    /**
     * Returns a {@link AsyncLocalAtomicReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link AsyncLocalAtomicReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(AsyncLocalAtomicReference.class)
    <V> AsyncLocalAtomicReference<V> getLocalAtomicReference(InjectionPoint ip)
        {
        LocalAtomicReference<V> ref = Atomics.getLocalAtomicReference(getName(ip));
        return ref.async();
        }

    /**
     * Returns a {@link AsyncRemoteAtomicReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link AsyncRemoteAtomicReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(AsyncRemoteAtomicReference.class)
    <V> AsyncRemoteAtomicReference<V> getUnqualifiedRemoteAtomicReference(InjectionPoint ip)
        {
        return getRemoteAtomicReference(ip);
        }

    /**
     * Returns a {@link AsyncRemoteAtomicReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link AsyncRemoteAtomicReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(AsyncRemoteAtomicReference.class)
    <V> AsyncRemoteAtomicReference<V> getRemoteAtomicReference(InjectionPoint ip)
        {
        RemoteAtomicReference<V> ref = Atomics.getRemoteAtomicReference(getName(ip));
        return ref.async();
        }
    }
