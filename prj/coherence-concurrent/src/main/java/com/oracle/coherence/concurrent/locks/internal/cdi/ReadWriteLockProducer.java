/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.locks.internal.cdi;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import com.oracle.coherence.concurrent.locks.RemoteReadWriteLock;
import com.oracle.coherence.concurrent.locks.Locks;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;

import javax.enterprise.inject.spi.InjectionPoint;

/**
 * CDI producers for {@link ReadWriteLock} values.
 *
 * @author Aleks Seovic  2021.11.18
 * @since 21.12
 */
@ApplicationScoped
public class ReadWriteLockProducer
        extends AbstractLockProducer
    {
    /**
     * Returns either a local or remote {@link ReadWriteLock} for the provided {@link InjectionPoint}.
     * <p>
     * If the injection point is annotated with the {@link Remote} qualifier a remote
     * {@link ReadWriteLock} will be returned, otherwise a local {@link ReadWriteLock}
     * will be returned.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local or remote {@link ReadWriteLock} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Remote
    ReadWriteLock getReadWriteLock(InjectionPoint ip)
        {
        if (ip.getQualifiers().contains(Remote.Literal.INSTANCE))
            {
            return getRemoteReadWriteLock(ip);
            }
        return getReentrantReadWriteLock(ip);
        }

    /**
     * Returns an {@link ReentrantReadWriteLock} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link ReentrantReadWriteLock} for the provided {@link InjectionPoint}
     */
    @Produces
    ReentrantReadWriteLock getUnqualifiedReentrantReadWriteLock(InjectionPoint ip)
        {
        return getReentrantReadWriteLock(ip);
        }

    /**
     * Returns a {@link ReentrantReadWriteLock} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link ReentrantReadWriteLock} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(ReentrantReadWriteLock.class)
    ReentrantReadWriteLock getReentrantReadWriteLock(InjectionPoint ip)
        {
        return Locks.localReadWriteLock(getName(ip));
        }

    /**
     * Returns an {@link RemoteReadWriteLock} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link RemoteReadWriteLock} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(RemoteReadWriteLock.class)
    RemoteReadWriteLock getUnqualifiedRemoteReadWriteLock(InjectionPoint ip)
        {
        return getRemoteReadWriteLock(ip);
        }

    /**
     * Returns a {@link RemoteReadWriteLock} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link RemoteReadWriteLock} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(RemoteReadWriteLock.class)
    RemoteReadWriteLock getRemoteReadWriteLock(InjectionPoint ip)
        {
        return Locks.remoteReadWriteLock(getName(ip));
        }
    }
