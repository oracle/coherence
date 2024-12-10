/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.locks.internal.cdi;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import com.oracle.coherence.concurrent.locks.Locks;
import com.oracle.coherence.concurrent.locks.RemoteLock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;

import javax.enterprise.inject.spi.InjectionPoint;

/**
 * CDI producers for {@link Lock} values.
 *
 * @author Aleks Seovic  2021.11.18
 * @since 21.12
 */
@ApplicationScoped
public class LockProducer
        extends AbstractLockProducer
    {
    /**
     * Returns either a local or remote {@link Lock} for the provided {@link InjectionPoint}.
     * <p>
     * If the injection point is annotated with the {@link Remote} qualifier a remote
     * {@link Lock} will be returned, otherwise a local {@link Lock}
     * will be returned.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local or remote {@link Lock} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Remote
    Lock getLock(InjectionPoint ip)
        {
        if (ip.getQualifiers().contains(Remote.Literal.INSTANCE))
            {
            return getRemoteLock(ip);
            }
        return getReentrantLock(ip);
        }

    /**
     * Returns an {@link ReentrantLock} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link ReentrantLock} for the provided {@link InjectionPoint}
     */
    @Produces
    ReentrantLock getUnqualifiedReentrantLock(InjectionPoint ip)
        {
        return getReentrantLock(ip);
        }

    /**
     * Returns a {@link ReentrantLock} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link ReentrantLock} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(ReentrantLock.class)
    ReentrantLock getReentrantLock(InjectionPoint ip)
        {
        return Locks.localLock(getName(ip));
        }

    /**
     * Returns an {@link RemoteLock} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link RemoteLock} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(RemoteLock.class)
    RemoteLock getUnqualifiedRemoteLock(InjectionPoint ip)
        {
        return getRemoteLock(ip);
        }

    /**
     * Returns a {@link RemoteLock} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link RemoteLock} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(RemoteLock.class)
    RemoteLock getRemoteLock(InjectionPoint ip)
        {
        return Locks.remoteLock(getName(ip));
        }
    }
