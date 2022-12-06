/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.io;


import com.oracle.coherence.common.internal.io.AbstractBufferManager;
import com.oracle.coherence.common.internal.io.SegmentedBufferManager;
import com.oracle.coherence.common.internal.io.CheckedBufferManager;
import com.oracle.coherence.common.internal.io.SlabBufferManager;
import com.oracle.coherence.common.internal.io.WrapperBufferManager;
import com.oracle.coherence.common.util.MemorySize;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * BufferManagers provides access to pre-defined system-wide managers.
 * <p>
 * The default size of each of the pools may be specified via the <tt>com.oracle.common.io.BufferManagers.pool</tt>
 * system property.  Additionally <tt>com.oracle.common.io.BufferManagers.checked</tt> can be used to default all
 * managers to utilize checked implementations to watch for pool usage issues.
 * </p>
 *
 * @author mf  2010.12.02
 */
public final class BufferManagers
    {
    /**
     * Return the heap ByteBuffer based BufferManager.
     * <p>
     * The maximum size of this buffer manager pool may be specified via the
     * <tt>com.oracle.coherence.common.io.BufferManagers.heap.pool</tt> system property. Setting this
     * value to <tt>0</tt> results in a non-pooled implementation. The default
     * value is a small percentage of the JVM's total heap size.
     * <p>
     * For pooled implementations setting <tt>com.oracle.coherence.common.io.BufferManagers.heap.checked</tt>
     * to <tt>true</tt> will provide pool which provides more stringent checks
     * in an attempt to ensure that the application doesn't misuse the pool,
     * for instance by double releasing a buffer. The default value is <tt>false</tt>.
     *
     * @return the heap BufferManager
     */
    public static BufferManager getHeapManager()
        {
        return HeapManagerHolder.INSTANCE;
        }

    /**
     * Return the direct ByteBuffer based BufferManager.
     * <p>
     * The maximum size of this buffer manager pool may be specified via the
     * <tt>com.oracle.coherence.common.io.BufferManagers.direct.pool</tt> system property. Setting this
     * value to <tt>0</tt> results in a non-pooled implementation. The default
     * value is a small percentage of the JVM's total heap size.
     * <p>
     * For pooled implementations setting <tt>com.oracle.coherence.common.io.BufferManagers.direct.checked</tt>
     * to <tt>true</tt> will provide pool which provides more stringent checks
     * in an attempt to ensure that the application doesn't misuse the pool,
     * for instance by double releasing a buffer. The default value is <tt>false</tt>.
     *
     * @return the direct BufferManager
     */
    public static BufferManager getDirectManager()
        {
        return DirectManagerHolder.INSTANCE;
        }

    /**
     * Return the network optimized direct ByteBuffer based BufferManager.
     * <p>
     * Compared with the {@link #getDirectManager DirectManager} this implementation
     * may provide buffers which are more optimal for use in network operations.
     * On some platforms this method may simply return the DirectManager.
     * <p>
     * The maximum size of this buffer manager pool may be specified via the
     * <tt>com.oracle.coherence.common.io.BufferManagers.network.pool</tt> system property. Setting this
     * value to <tt>0</tt> results in a non-pooled implementation. The default
     * value is a small percentage of the JVM's total heap size.
     * <p>
     * For pooled implementations setting <tt>com.oracle.coherence.common.io.BufferManagers.network.checked</tt>
     * to <tt>true</tt> will provide pool which provides more stringent checks
     * in an attempt to ensure that the application doesn't misuse the pool,
     * for instance by double releasing a buffer. The default value is <tt>false</tt>.
     *
     * @return the network direct BufferManager
     */
    public static BufferManager getNetworkDirectManager()
        {
        return NetworkDirectManagerHolder.INSTANCE;
        }

    /**
     * Holder for the heap based BufferManager.
     */
    private static class HeapManagerHolder
        {
        /**
         * The heap based BufferManager.
         */
        public static final BufferManager INSTANCE;


        static
            {
            BufferManager mgr;
            String        sHeap = System.getProperty(BufferManagers.class.getName()
                                            + ".heap.pool", System.getProperty(BufferManagers.class.getName()
                                            + ".pool", Long.toString(DEFAULT_POOL_SIZE)));
            int           cbBuf = (int) new MemorySize(System.getProperty(BufferManagers.class.getName() + ".heap.base",
                                                        System.getProperty(BufferManagers.class.getName() + ".base",
                                                                Long.toString(SegmentedBufferManager.DEFAULT_BUF_SIZE)))).getByteCount();

            if (sHeap.startsWith("-")) // negative implies infinite which maps
                                       // to the WeakBufferManager
                {
                //TODO initialize the WeakSegmentedBufferManager here
                throw new IllegalStateException("Negative heap pool size");
                }
            else  // sHeap is positive
                {
                long lcbPool = new MemorySize(sHeap).getByteCount();
                if (lcbPool > 0L)
                    {
                    SegmentedBufferManager mgrSeg = new SegmentedBufferManager(new SegmentedBufferManager.BufferAllocator()
                        {
                        public ByteBuffer allocate(int cb)
                            {
                            return ByteBuffer.allocate(cb);
                            }

                        public void release(ByteBuffer buff)
                            {
                            if (buff.isDirect())
                                {
                                throw new IllegalArgumentException();
                                }
                            // nothing to do
                            }

                        public String toString()
                            {
                            return "HeapBufferAllocator";
                            }
                        }, cbBuf, lcbPool);

                    mgrSeg.setName("HeapBufferManager");
                    mgr = mgrSeg;
                    }
                else // lcbPool == 0
                    {
                    mgr = new AbstractBufferManager()
                        {
                        public ByteBuffer acquire(int cb)
                            {
                            return ByteBuffer.allocate(cb);
                            }

                        public void dispose()
                            {
                            // no-op; top level manager
                            }

                        protected void ensureCompatibility(ByteBuffer buff)
                            {
                            if (buff.isDirect())
                                {
                                throw new IllegalArgumentException();
                                }
                            }
                        };
                    }
                }

            mgr      = new NonDisposableBufferManager(mgr);
            LOGGER.log(Level.FINE, "initialized HeapBufferManager " + mgr);
            INSTANCE = Boolean.valueOf(System.getProperty(BufferManagers.class.getName() + ".heap.checked",
                            System.getProperty(BufferManagers.class.getName() + ".checked")))
                            ? new CheckedBufferManager(mgr) : mgr;
            }
        }

    /**
     * Holder for the direct BufferManager.
     */
    private static class DirectManagerHolder
        {
        /**
         * The direct BufferManager.
         */
        public static final BufferManager INSTANCE;

        static
            {
            BufferManager mgr;
            long          lcbPool = new MemorySize(System.getProperty(BufferManagers.class.getName() + ".direct.pool",
                                                        System.getProperty(BufferManagers.class.getName() + ".pool",
                                                                Long.toString(DEFAULT_POOL_SIZE)))).getByteCount(); // default is doced above
            int           cbBuf   = (int) new MemorySize(System.getProperty(BufferManagers.class.getName() + ".direct.base",
                                                        System.getProperty(BufferManagers.class.getName() + ".base",
                                                                Long.toString(SegmentedBufferManager.DEFAULT_BUF_SIZE)))).getByteCount();

            if (lcbPool > 0L)
                {
                // for DirectByteBuffers we use a slab based buffer manager as DBB allocations are page aligned and thus
                // consume significantly more memory then is requested.
                SlabBufferManager mgrSlab = new SlabBufferManager(
                        new SlabBufferManager.DirectBufferAllocator(), cbBuf, lcbPool);

                mgrSlab.setName("DirectBufferManager");
                mgr = mgrSlab;
                }
            else // lcbPool == 0
                {
                mgr = new AbstractBufferManager()
                    {
                    public ByteBuffer acquire(int cb)
                        {
                        return ByteBuffer.allocateDirect(cb);
                        }

                    public void dispose()
                        {
                        // no-op; top level manager
                        }

                    protected void ensureCompatibility(ByteBuffer buff)
                        {
                        if (!buff.isDirect())
                            {
                            throw new IllegalArgumentException();
                            }
                        }
                    };
                }

            mgr      = new NonDisposableBufferManager(mgr);
            LOGGER.log(Level.FINE, "initialized DirectBufferManager " + mgr);
            INSTANCE = Boolean.valueOf(System.getProperty(BufferManagers.class.getName() + ".direct.checked",
                    System.getProperty(BufferManagers.class.getName() + ".checked")))
                            ? new CheckedBufferManager(mgr) : mgr;
            }
        }

    /**
     * Holder for the network direct BufferManager.
     */
    private static class NetworkDirectManagerHolder
        {
        /**
         * The network direct BufferManager.
         */
        public static final BufferManager INSTANCE = DirectManagerHolder.INSTANCE;
        }

    /**
     * A {@link WrapperBufferManager} which cannot be disposed.
     */
    private static class NonDisposableBufferManager
        extends WrapperBufferManager
        {
        /**
         * Create a new NonDisposableBufferManager wrapping the passed
         * BufferManager.
         *
         * @param delegate the BufferManager to wrap
         */
        public NonDisposableBufferManager(BufferManager delegate)
            {
            super(delegate);
            }

        @Override
        public void dispose()
            {
            throw new UnsupportedOperationException();
            }
        }

    /**
     * The default amount of memory for each pool.
     *
     * The goal is to select a size which won't consume too much of the total java heap.  We target a limit
     * which is 5% of the max heap size -Xmx, but also defend against configurations which may not have specified
     * -Xmx in which case we limit ourselves to decent portion of the initial heap size, since the heap has no
     * apparent upper bound this is also a small portion.
     */
    private static final long DEFAULT_POOL_SIZE = Math.min(/*~Xms*/ Runtime.getRuntime().totalMemory() / 4,
                                                           /* Xmx*/Runtime.getRuntime().maxMemory() / 20);

    /**
     * The Logger for the managers.
     */
    private static final Logger LOGGER = Logger.getLogger(BufferManagers.class.getName());

    /**
     * Indicates if buffers should have their contents zeroed out upon release.
     *
     * There is a performance impact to enabling this, it effectively negates the benefits of zcopy
     */
    public static final boolean ZERO_ON_RELEASE = Boolean.getBoolean(
            BufferManagers.class.getName() + ".zeroed");
    }
