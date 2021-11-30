/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.internal.cdi;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicMarkableReference;
import com.oracle.coherence.concurrent.atomic.AsyncLocalAtomicMarkableReference;
import com.oracle.coherence.concurrent.atomic.AsyncRemoteAtomicMarkableReference;
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
 * CDI producers for {@link AsyncAtomicMarkableReference} values.
 *
 * @author Aleks Seovic  2020.12.09
 * @since 21.12
 */
@ApplicationScoped
class AsyncAtomicMarkableReferenceProducer
        extends AbstractAtomicProducer
    {
    /**
     * Returns a local {@link AsyncAtomicMarkableReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a local {@link AsyncAtomicMarkableReference} for the provided {@link InjectionPoint}
     */
    @Produces
    <V> AsyncAtomicMarkableReference<V> getUnqualifiedAtomicMarkableReference(InjectionPoint ip)
        {
        return getLocalAtomicMarkableReference(ip);
        }

    /**
     * Returns a local {@link AsyncAtomicMarkableReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a local {@link AsyncAtomicMarkableReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    <V> AsyncAtomicMarkableReference<V> getAtomicMarkableReference(InjectionPoint ip)
        {
        return getLocalAtomicMarkableReference(ip);
        }

    /**
     * Returns a remote {@link AsyncAtomicMarkableReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a remote {@link AsyncAtomicMarkableReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Remote
    <V> AsyncAtomicMarkableReference<V> getAtomicMarkableReferenceWithRemoteAnnotation(InjectionPoint ip)
        {
        return getRemoteAtomicMarkableReference(ip);
        }

    /**
     * Returns a {@link AsyncLocalAtomicMarkableReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link AsyncLocalAtomicMarkableReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(AsyncLocalAtomicMarkableReference.class)
    <V> AsyncLocalAtomicMarkableReference<V> getUnqualifiedLocalAtomicMarkableReference(InjectionPoint ip)
        {
        return getLocalAtomicMarkableReference(ip);
        }

    /**
     * Returns a {@link AsyncLocalAtomicMarkableReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link AsyncLocalAtomicMarkableReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(AsyncLocalAtomicMarkableReference.class)
    <V> AsyncLocalAtomicMarkableReference<V> getLocalAtomicMarkableReference(InjectionPoint ip)
        {
        LocalAtomicMarkableReference<V> ref = Atomics.localAtomicMarkableReference(getName(ip));
        return ref.async();
        }

    /**
     * Returns a {@link AsyncRemoteAtomicMarkableReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link AsyncRemoteAtomicMarkableReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(AsyncRemoteAtomicMarkableReference.class)
    <V> AsyncRemoteAtomicMarkableReference<V> getUnqualifiedRemoteAtomicMarkableReference(InjectionPoint ip)
        {
        return getRemoteAtomicMarkableReference(ip);
        }

    /**
     * Returns a {@link AsyncRemoteAtomicMarkableReference} for the provided {@link InjectionPoint}.
     *
     * @param ip   the CDI {@link InjectionPoint}
     * @param <V>  the type of object referred to by this reference
     *
     * @return a {@link AsyncRemoteAtomicMarkableReference} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(AsyncRemoteAtomicMarkableReference.class)
    <V> AsyncRemoteAtomicMarkableReference<V> getRemoteAtomicMarkableReference(InjectionPoint ip)
        {
        RemoteAtomicMarkableReference<V> ref = Atomics.remoteAtomicMarkableReference(getName(ip));
        return ref.async();
        }
    }
