/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ReadBuffer;

import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BitHelper;
import com.tangosol.util.ClassHelper;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import java.io.IOException;

import org.junit.Test;

/**
 * Performance tests for simple cache persistence.
 *
 * @author jh  2013.04.09
 */
public abstract class AbstractPerformancePersistenceTests
        extends AbstractFunctionalTest
    {

    // ----- constructors ---------------------------------------------------

    public AbstractPerformancePersistenceTests(String sPath)
        {
        super(sPath);
        }

    // ----- tests ----------------------------------------------------------

    @Test
    public void testWarmupPersistent()
            throws IOException
        {
        System.out.println(ClassHelper.getSimpleName(getClass())
                + ".testWarmupPersistent():");
        testWarmup(getPersistentCache());
        }

    @Test
    public void testWarmupTransient()
            throws IOException
        {
        System.out.println(ClassHelper.getSimpleName(getClass())
                + ".testWarmupTransient():");
        testWarmup(getTransientCache());
        }

    protected void testWarmup(NamedCache cache)
        {
        ReadBuffer[] abKeys = createKeySequence(100);
        for (int c = 0; c < 4; ++c)
            {
            populateCache(cache, abKeys, BINARY_LARGE_VALUE);
            }

        Base.randomize(abKeys);
        for (int c = 0; c < 4; ++c)
            {
            populateCache(cache, abKeys, BINARY_LARGE_VALUE);
            }
        }

    @Test
    public void testContinuousLoadPersistent()
        {
        System.out.println(ClassHelper.getSimpleName(getClass())
                + ".testContinuousLoadPersistent():");
        testContinuousLoad(getPersistentCache());
        }

//    @Test
    public void testContinuousLoadTransient()
        {
        System.out.println(ClassHelper.getSimpleName(getClass())
                + ".testContinuousLoadTransient():");
        testContinuousLoad(getTransientCache());
        }

    protected void testContinuousLoad(NamedCache cache)
        {
        // create a sequence of serialized integer keys
        ReadBuffer[] abKeys = createKeySequence(1000);

        // populate the cache with initial data
        populateCache(cache, abKeys, BINARY_MEDIUM_VALUE);

        long ldtStart = System.currentTimeMillis();
        for (int nNew = abKeys.length, c = 0; nNew < Integer.MAX_VALUE; ++nNew)
            {
            ReadBuffer abKeyCreate = convertIntToReadBuffer(nNew);
            ReadBuffer abKeyUpdate = convertIntToReadBuffer(nNew - (abKeys.length / 2));
            ReadBuffer abKeyErase  = convertIntToReadBuffer(nNew - abKeys.length);
            ReadBuffer abValue     = Base.getRandomBinary(BINARY_MEDIUM_VALUE.length(),
                    BINARY_MEDIUM_VALUE.length());

            // store a new mapping
            cache.put(abKeyCreate, abValue);

            // update an existing mapping
            cache.put(abKeyUpdate, abValue);

            // erase an existing mapping
            cache.remove(abKeyErase);

            if (++c == abKeys.length)
                {
                long ldtEnd = System.currentTimeMillis();
                outputResults("continuous", ldtStart, ldtEnd, abKeys.length);

                ldtStart = System.currentTimeMillis();
                c = 0;
                }
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return a persistent cache.
     *
     * @return a persistent cache
     */
    protected NamedCache getPersistentCache()
        {
        return getNamedCache("simple-persistent");
        }

    /**
     * Return a non-persistent (transient) cache.
     *
     * @return a transient cache
     */
    protected NamedCache getTransientCache()
        {
        return getNamedCache("simple-transient");
        }

    /**
     * Write the given integer to a buffer, returning a read-only view.
     *
     * @param n  the integer to convert to a buffer
     *
     * @return a read only buffer
     */
    protected ReadBuffer convertIntToReadBuffer(int n)
        {
        return new ByteArrayReadBuffer(BitHelper.toBytes(n));
        }

    /**
     * Create and return a sequence of serialized integers: [0..c)
     *
     * @param c  the number of integers in the sequence
     *
     * @return the sequence of serialized integers
     */
    protected ReadBuffer[] createKeySequence(int c)
        {
        if (c < 0)
            {
            throw new IllegalArgumentException("negative sequence size");
            }

        ReadBuffer[] ab = new ReadBuffer[c];
        for (int i = 0; i < c; ++i)
            {
            ab[i] = convertIntToReadBuffer(i);
            }

        return ab;
        }

    /**
     * Populate the given cache with the given keys and associated value.
     *
     * @param cache     the cache to populate
     * @param abKeys    the keys to store
     * @param binValue  the value for each key to store
     */
    protected void populateCache(NamedCache cache, ReadBuffer[] abKeys, Binary binValue)
        {
        for (int i = 0, c = abKeys.length; i < c; ++i)
            {
            cache.put(abKeys[i], binValue);
            }
        }

    /**
     * Output profiling results.
     *
     * @param sDesc     a short description of the results
     * @param ldtStart  the time the test started
     * @param ldtEnd    the time the test completed
     * @param cOps      the number of operations tested
     */
    protected void outputResults(String sDesc, long ldtStart, long ldtEnd, int cOps)
        {
        long ldtDelta = ldtEnd - ldtStart;
        long cRate    = (long)((cOps * 1000.0)/ldtDelta);
        System.out.println(sDesc + ": " + ldtDelta + "ms, " + cRate + "ops/s");
        }

    /**
     * Profile storing the given keys and associated value into the given
     * cache.
     *
     * @param cache     the cache to store the keys and values in
     * @param abKeys    the keys to store
     * @param binValue  the value for each key to store
     */
    protected void profilePut(NamedCache cache, ReadBuffer[] abKeys, Binary binValue)
        {
        long ldtStart = System.currentTimeMillis();
        populateCache(cache, abKeys, binValue);
        long ldtEnd = System.currentTimeMillis();

        outputResults("put", ldtStart, ldtEnd, abKeys.length);
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the project name.
     */
    public static String getProjectName()
        {
        return "persistence";
        }

    // ----- constants ------------------------------------------------------

    /**
     * Test Binary values.
     */
    public static final Binary BINARY_SMALL_VALUE  = Base.getRandomBinary(64, 64);         // 64 bytes
    public static final Binary BINARY_MEDIUM_VALUE = Base.getRandomBinary(10240, 10240);   // 10k
    public static final Binary BINARY_LARGE_VALUE  = Base.getRandomBinary(102400, 102400); // 100k
    }
