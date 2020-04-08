/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.nio;


import com.tangosol.coherence.config.Config;

import java.nio.ByteBuffer;


/**
* Manages a growable direct ByteBuffer.
*
* @see ByteBuffer
*
* @author cp  2002.09.16
*
* @since Coherence 2.2
*
* @deprecated use {@link com.tangosol.io.journal.JournalBinaryStore JournalBinaryStore}
*             instead
*/
@Deprecated
public class DirectBufferManager
        extends AbstractBufferManager
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a DirectBufferManager that supports a buffer of a certain
    * initial and maximum size.
    *
    * @param cbInitial  the initial size
    * @param cbMaximum  the maximum size
    */
    public DirectBufferManager(int cbInitial, int cbMaximum)
        {
        super(cbInitial, cbMaximum);
        allocateBuffer();
        }


    // ----- internal -------------------------------------------------------

    /**
    * Allocate a new buffer, copying old data if there is any.
    *
    * @see ByteBuffer#allocateDirect(int) ByteBuffer.allocateDirect()
    */
    protected void allocateBuffer()
        {
        long       lStart = System.currentTimeMillis();
        ByteBuffer bufOld = getBuffer();
        ByteBuffer bufNew = MODE_DEBUG ? ByteBuffer.allocate(getCapacity())
                                       : ByteBuffer.allocateDirect(getCapacity());
        int cbOld         = 0;
        int cbNew         = bufNew.capacity();
        if (bufOld != null)
            {
            int ofOld  = bufOld.position();
            cbOld      = bufOld.capacity();
            int cbCopy = Math.min(cbOld, cbNew);
            bufOld.position(0);
            if (cbOld > cbNew)
                {
                int    cbBuf = Math.max(cbCopy, 16384);
                byte[] abBuf = new byte[cbBuf];
                while (cbCopy > 0)
                    {
                    int cbChunk = Math.min(cbBuf, cbCopy);
                    bufOld.get(abBuf, 0, cbChunk);
                    bufNew.put(abBuf, 0, cbChunk);
                    cbCopy -= cbChunk;
                    }
                }
            else
                {
                bufNew.put(bufOld);
                }

            if (ofOld < cbNew)
                {
                bufNew.position(ofOld);
                }
            }

        // Keep track of the amount of "detached", possibly not freed data;
        // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4857305
        // COH-2495
        s_cbUncollected += cbOld;
        s_cAllocations++;

        setBuffer(bufNew);

        cleanupBuffers();

        long cMillis = System.currentTimeMillis() - lStart;
        if (cMillis > 100L)
            {
            log("Buffer [" + s_cAllocations + "] allocated grew from " +
                    cbOld + " to " + cbNew + " bytes, took " + cMillis + " ms.");
            }
        s_lTotalAllocationTime += cMillis;
        }

    /**
    * Determines if and how to induce the JVM to run finalizers. This allows
    * to somewhat mitigate
    * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4857305">
    * a known JDK bug</a>.
    * <p>
    * When the amount of uncollected garbage reaches the threshold indicated
    * by {@link #getCleanupThreshold() the cleanup threshold} frequency,
    * induce the GC. In a 64-bit VMs this issue is probably reduced to a
    * minimum.
    */
    protected void cleanupBuffers()
        {
        Runtime runtime = Runtime.getRuntime();
        if (s_cbUncollected >
                (runtime.maxMemory() * getCleanupThreshold()))
            {
            switch (getCleanupMethod())
                {
                case CLEANUP_FINALIZERS:
                    runtime.runFinalization();
                    break;

                case CLEANUP_GC:
                    runtime.gc();
                    break;

                default:
                    break;
                }

            s_cbUncollected = 0;
            }
        }


    // ----- accessors  -----------------------------------------------------

    /**
    * Return the total time spent allocating buffers, copying the previous
    * content and cleaning up uncollected buffers.
    *
    * @return  the number of milliseconds spent in allocating, copying and
    *          cleaning up uncollected buffers
    */
    public static long getTotalAllocationTime()
        {
        return s_lTotalAllocationTime;
        }

    /**
    * Return the total number of buffers allocated by the DirectBufferManager.
    *
    * @return the total number of allocations
    */
    public static int getAllocations()
        {
        return s_cAllocations;
        }

    /**
    * Return the method used for buffers cleanup when the threshold is met.
    * <p>
    * The possible return values are:
    * <ul>
    *   <li>CLEANUP_FINALIZERS - use Runtime.runFinalization() (default);</li>
    *   <li>CLEANUP_GC - use Runtime.gc();</li>
    *   <li>CLEANUP_NONE - do not perform any cleanup.</li>
    * </ul>
    *
    * @return one of the CLEANUP_* constants representing the cleanup method
    */
    public static int getCleanupMethod()
        {
        return s_nCleanupMethod;
        }

    /**
    * Set the method to be used for buffers cleanup when the threshold is met.
    *
    * @param nCleanupMethod  a value representing the cleanup method; valid
    *                        values are the CLEANUP_* constants
    *
    * @throws IllegalArgumentException if passed value is not a valid
    *         CLEANUP_* constant
    */
    protected static void setCleanupMethod(int nCleanupMethod)
        {
        if (nCleanupMethod < CLEANUP_NONE || nCleanupMethod > CLEANUP_GC)
            {
            throw new IllegalArgumentException("Invalid cleanup method: " +
                    nCleanupMethod);
            }
        s_nCleanupMethod = nCleanupMethod;
        }

    /**
    * Return the value used to calculate the size threshold for the
    * uncollected buffers that triggers a cleanup operation. Cleanup is
    * performed when the total size of uncollected memory has grown over the
    * product of this value and the maximum Java heap size.
    *
    * @return a value between 0.0 and 1.0 used to calculate the cleanup
    *         frequency
    */
    public static float getCleanupThreshold()
        {
        return s_fCleanupThreshold;
        }

    /**
    * Determines how often, to run the {@link #cleanupBuffers()} method.
    * Cleanup is performed when the uncollected memory has grown over the
    * product of this value and the maximum Java heap size.
    *
    * @param flCleanupThreshold  a value between 0.0 and 1.0 used to
    *                            calculate the cleanup frequency
    *
    * @throws IllegalArgumentException if the passed value is not valid
    */
    protected static void setCleanupThreshold(float flCleanupThreshold)
        {
        if (flCleanupThreshold < 0.0 || flCleanupThreshold > 1.0)
            {
            throw new IllegalArgumentException("Invalid cleanup threshold: " +
                    flCleanupThreshold);
            }
        s_fCleanupThreshold = flCleanupThreshold;
        }


    // ----- constants ------------------------------------------------------

    /**
    * Do not perform any buffer cleanup.
    */
    public static final int CLEANUP_NONE       = 0;

    /**
    * Use Runtime.runFinalization() to trigger finalization (default).
    */
    public static final int CLEANUP_FINALIZERS = 1;

    /**
    * Use Runtime.gc() to trigger finalization.
    */
    public static final int CLEANUP_GC         = 2;

    /**
    * Set to true to use on-heap storage, false to use off-heap.
    */
    protected static final boolean MODE_DEBUG = false;


    // ----- data members ---------------------------------------------------

    /**
    * Total amount of memory that have not been collected yet.
    */
    protected static volatile long s_cbUncollected;

    /**
    * Total time spent while allocating buffers, copying the previous content
    * and cleaning buffers.
    */
    private static volatile long s_lTotalAllocationTime;

    /**
    * Total number of buffer allocations made by the DirectBufferManager.
    */
    private static volatile int s_cAllocations;

    /**
    * The type of cleanup method (CLEANUP_* constants).
    */
    private static int s_nCleanupMethod;

    /**
    * Used to calculate the cleanup frequency.
    */
    private static float s_fCleanupThreshold;

    /**
    * Initialize the cleanup related values.
    */
    static
        {
        setCleanupThreshold(Config.getFloat("coherence.nio.cleanup.frequency", 0.10F));

        String sCleanupMethod = Config.getProperty("coherence.nio.cleanup.method", "FINALIZERS");
        if (sCleanupMethod.equalsIgnoreCase("FINALIZERS"))
            {
            setCleanupMethod(CLEANUP_FINALIZERS);
            }
        else if (sCleanupMethod.equalsIgnoreCase("GC"))
            {
            setCleanupMethod(CLEANUP_GC);
            }
        else
            {
            setCleanupMethod(CLEANUP_NONE);
            }
        }
    }
