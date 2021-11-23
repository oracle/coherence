/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.locks.internal.cdi;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import com.oracle.coherence.concurrent.locks.DistributedLock;
import com.oracle.coherence.concurrent.locks.Locks;

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
class LockProducer
        extends AbstractLockProducer
    {
    /**
     * Returns a local {@link Lock} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link Lock} for the provided {@link InjectionPoint}
     */
    @Produces
    Lock getUnqualifiedLock(InjectionPoint ip)
        {
        return getReentrantLock(ip);
        }

    /**
     * Returns a local {@link Lock} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link Lock} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    Lock getLock(InjectionPoint ip)
        {
        return getReentrantLock(ip);
        }

    /**
     * Returns a distributed {@link Lock} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link Lock} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Remote
    Lock getLockWithRemoteAnnotation(InjectionPoint ip)
        {
        return getDistributedLock(ip);
        }

    /**
     * Returns a local {@link ReentrantLock} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link ReentrantLock} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(ReentrantLock.class)
    ReentrantLock getUnqualifiedReentrantLock(InjectionPoint ip)
        {
        return getReentrantLock(ip);
        }

    /**
     * Returns a local {@link ReentrantLock} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link ReentrantLock} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(ReentrantLock.class)
    ReentrantLock getReentrantLock(InjectionPoint ip)
        {
        return Locks.localLock(getName(ip));
        }

    /**
     * Returns a local {@link DistributedLock} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link DistributedLock} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(DistributedLock.class)
    DistributedLock getUnqualifiedDistributedLock(InjectionPoint ip)
        {
        return getDistributedLock(ip);
        }

    /**
     * Returns a {@link DistributedLock} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link DistributedLock} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(DistributedLock.class)
    DistributedLock getDistributedLock(InjectionPoint ip)
        {
        return Locks.remoteLock(getName(ip));
        }
    }
