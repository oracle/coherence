/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.hnswlib;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class offers a thread-safe alternative for a small-world Index. It
 * allows concurrent item insertion and querying which is not supported by the
 * native Hnswlib implementation.
 * <p>
 * Note: this class relies on a ReadWriteLock with fairness enabled. So, when
 * multi-thread insertions are serialized. To take advantage of parallel
 * insertion, please create an Index instance and then retrieve a ConcurrentIndex
 * one via Index.synchronizedIndex() method call.
 */
public class ConcurrentIndex
        extends Index
    {
    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

    public ConcurrentIndex(SpaceName spaceName, int dimensions)
        {
        super(spaceName, dimensions);
        }

    @Override
    public void addItem(float[] item, int id, boolean replaceDeleted)
        {
        lock.writeLock().lock();
        try
            {
            super.addItem(item, id, replaceDeleted);
            }
        finally
            {
            lock.writeLock().unlock();
            }
        }

    @Override
    public int getLength()
        {
        lock.readLock().lock();
        try
            {
            return super.getLength();
            }
        finally
            {
            lock.readLock().unlock();
            }
        }

    @Override
    public QueryTuple knnQuery(float[] input, int k)
        {
        lock.readLock().lock();
        try
            {
            return super.knnQuery(input, k);
            }
        finally
            {
            lock.readLock().unlock();
            }
        }

    @Override
    public void save(Path path)
        {
        lock.readLock().lock();
        try
            {
            super.save(path);
            }
        finally
            {
            lock.readLock().unlock();
            }
        }

    @Override
    public void load(Path path, int maxNumberOfElements)
        {
        lock.writeLock().lock();
        try
            {
            super.load(path, maxNumberOfElements);
            }
        finally
            {
            lock.writeLock().unlock();
            }
        }

    @Override
    public void clear()
        {
        lock.writeLock().lock();
        try
            {
            super.clear();
            }
        finally
            {
            lock.writeLock().unlock();
            }
        }

    public void close()
        {
        lock.writeLock().lock();
        try
            {
            super.close();
            }
        finally
            {
            lock.writeLock().unlock();
            }
        }

    @Override
    public void setEf(int ef)
        {
        lock.writeLock().lock();
        try
            {
            super.setEf(ef);
            }
        finally
            {
            lock.writeLock().unlock();
            }
        }

    @Override
    public boolean hasId(int id)
        {
        lock.readLock().lock();
        try
            {
            return super.hasId(id);
            }
        finally
            {
            lock.readLock().unlock();
            }
        }

    public void markDeleted(int id)
        {
        lock.writeLock().lock();
        try
            {
            super.markDeleted(id);
            }
        finally
            {
            lock.writeLock().unlock();
            }
        }

    public Optional<float[]> getData(int id)
        {
        lock.readLock().lock();
        try
            {
            return super.getData(id);
            }
        finally
            {
            lock.readLock().unlock();
            }
        }
    }
