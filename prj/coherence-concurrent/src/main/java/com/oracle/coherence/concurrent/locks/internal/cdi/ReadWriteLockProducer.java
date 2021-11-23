/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.locks.internal.cdi;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import com.oracle.coherence.concurrent.locks.DistributedReadWriteLock;
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
class ReadWriteLockProducer
        extends AbstractLockProducer
    {
    /**
     * Returns a local {@link ReadWriteLock} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link ReadWriteLock} for the provided {@link InjectionPoint}
     */
    @Produces
    ReadWriteLock getUnqualifiedReadWriteLock(InjectionPoint ip)
        {
        return getReentrantReadWriteLock(ip);
        }

    /**
     * Returns a local {@link ReadWriteLock} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link ReadWriteLock} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    ReadWriteLock getReadWriteLock(InjectionPoint ip)
        {
        return getReentrantReadWriteLock(ip);
        }

    /**
     * Returns a distributed {@link ReadWriteLock} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link ReadWriteLock} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Remote
    ReadWriteLock getReadWriteLockWithRemoteAnnotation(InjectionPoint ip)
        {
        return getDistributedReadWriteLock(ip);
        }

    /**
     * Returns a local {@link ReentrantReadWriteLock} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link ReentrantReadWriteLock} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(ReentrantReadWriteLock.class)
    ReentrantReadWriteLock getUnqualifiedReentrantReadWriteLock(InjectionPoint ip)
        {
        return getReentrantReadWriteLock(ip);
        }

    /**
     * Returns a local {@link ReentrantReadWriteLock} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link ReentrantReadWriteLock} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(ReentrantReadWriteLock.class)
    ReentrantReadWriteLock getReentrantReadWriteLock(InjectionPoint ip)
        {
        return Locks.localReadWriteLock(getName(ip));
        }

    /**
     * Returns a local {@link DistributedReadWriteLock} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link DistributedReadWriteLock} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(DistributedReadWriteLock.class)
    DistributedReadWriteLock getUnqualifiedDistributedReadWriteLock(InjectionPoint ip)
        {
        return getDistributedReadWriteLock(ip);
        }

    /**
     * Returns a {@link DistributedReadWriteLock} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link DistributedReadWriteLock} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(DistributedReadWriteLock.class)
    DistributedReadWriteLock getDistributedReadWriteLock(InjectionPoint ip)
        {
        return Locks.remoteReadWriteLock(getName(ip));
        }
    }
