/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.listener;

import com.tangosol.net.NamedMap;
import com.tangosol.net.PartitionedService;

import com.tangosol.net.cache.CacheEvent;

import com.tangosol.net.partition.DefaultVersionedPartitions;
import com.tangosol.net.partition.VersionAwareMapListener;
import com.tangosol.net.partition.VersionedPartitions;

import com.tangosol.util.Base;
import com.tangosol.util.LongArray;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.PrimitiveSparseArray;
import com.tangosol.util.SparseArray;

import java.util.function.Consumer;

/**
 * A helper class to create some default {@link VersionAwareMapListener} implementations
 * for common cases including:
 * <ol>
 *     <li>Tracking the version from the {@link #createListener(MapListener)
 *     current version}</li>
 *     <li>Tracking the version from the {@link #createListener(MapListener, long, Object, NamedMap)
 *     provided version for a given key}</li>
 *     <li>Tracking the version from the {@link #createListener(MapListener, long, int)
 *     provided version for a given partition}</li>
 *     <li>Tracking the version from the {@link #createListener(MapListener, DefaultVersionedPartitions)
 *     provided partition versions}</li>
 * </ol>
 *
 * @author hr  2020.08.03
 */
public class VersionAwareListeners
    {
    /**
     * Return a {@link MapListener} that implements {@link VersionAwareMapListener}
     * if the provided listener is {@link MapListener#isVersionAware() version
     * aware}.
     *
     * @param listener  the {@link MapListener} to interrogate
     *
     * @param <K> key for the map
     * @param <V> value for the map
     *
     * @return a MapListener that implements {@link VersionAwareMapListener}
     *         if the provided listener is {@link MapListener#isVersionAware()
     *         version aware}
     */
    public static <K,V> MapListener<K, V> createListener(MapListener<K, V> listener)
        {
        return createListener(listener, (DefaultVersionedPartitions) null);
        }

    /**
     * Return a {@link MapListener} that implements {@link VersionAwareMapListener}
     * if the provided listener is {@link MapListener#isVersionAware() version
     * aware} and will request replay of events from source from the given version
     * for the provided key.
     *
     * @param listener  the {@link MapListener} to interrogate
     * @param lVersion  the version to receive events after
     * @param key       the key the listener will be registered against
     * @param cache     the cache the listener will be registered against
     *
     * @param <K> key for the map
     * @param <V> value for the map
     *
     * @return a MapListener that implements {@link VersionAwareMapListener}
     *         if the provided listener is {@link MapListener#isVersionAware()
     *         version aware}
     */
    public static <K,V> MapListener<K, V> createListener(MapListener<K, V> listener, long lVersion, K key, NamedMap<K, V> cache)
        {
        PartitionedService service = (PartitionedService) cache.getService();
        int iPart = service.getKeyPartitioningStrategy().getKeyPartition(key);

        return createListener(listener, lVersion, iPart);
        }

    /**
     * Return a {@link MapListener} that implements {@link VersionAwareMapListener}
     * if the provided listener is {@link MapListener#isVersionAware() version
     * aware} and will request replay of events from source from the given version
     * for the provided partition.
     * <p>
     * This helper method converts the provided partition and version to
     * the required {@link VersionedPartitions} data structure.
     *
     * @param listener  the {@link MapListener} to interrogate
     * @param lVersion  the version to receive events after
     * @param iPart     the partition to receive events for
     *
     * @param <K> key for the map
     * @param <V> value for the map
     *
     * @return a MapListener that implements {@link VersionAwareMapListener}
     *         if the provided listener is {@link MapListener#isVersionAware()
     *         version aware}
     */
    public static <K,V> MapListener<K, V> createListener(MapListener<K, V> listener, long lVersion, int iPart)
        {
        DefaultVersionedPartitions versions = new DefaultVersionedPartitions();
        versions.setPartitionVersion(iPart, lVersion);

        return createListener(listener, versions);
        }

    /**
     * Return a {@link MapListener} that implements {@link VersionAwareMapListener}
     * if the provided listener is {@link MapListener#isVersionAware() version
     * aware} and will request replay of events from source from the given partition
     * versions.
     * <p>
     * This helper method converts the provided {@link PrimitiveSparseArray} to
     * the required {@link VersionedPartitions} data structure.
     *
     * @param listener    the {@link MapListener} to interrogate
     * @param laVersions  the partition versions
     *
     * @param <K> key for the map
     * @param <V> value for the map
     *
     * @return a MapListener that implements {@link VersionAwareMapListener}
     *         if the provided listener is {@link MapListener#isVersionAware()
     *         version aware}
     */
    public static <K,V> MapListener<K, V> createListener(MapListener<K, V> listener, PrimitiveSparseArray laVersions)
        {
        DefaultVersionedPartitions versions = new DefaultVersionedPartitions();

        for (PrimitiveSparseArray.Iterator iter = laVersions.iterator(); iter.hasNext(); )
            {
            long lVersion = iter.nextPrimitive();
            versions.setPartitionVersion((int) iter.getIndex(), lVersion);
            }

        return createListener(listener, versions);
        }

    /**
     * Return a {@link MapListener} that implements {@link VersionAwareMapListener}
     * if the provided listener is {@link MapListener#isVersionAware() version
     * aware} and will request replay of events from source from the given partition
     * versions.
     *
     * @param listener  the {@link MapListener} to interrogate
     * @param versions  the partition versions
     *
     * @param <K> key for the map
     * @param <V> value for the map
     *
     * @return a MapListener that implements {@link VersionAwareMapListener}
     *         if the provided listener is {@link MapListener#isVersionAware()
     *         version aware}
     */
    public static <K,V> MapListener<K, V> createListener(MapListener<K, V> listener, DefaultVersionedPartitions versions)
        {
        if ((!listener.isVersionAware() || listener instanceof VersionAwareMapListener) &&
                versions == null)
            {
            // a VersionAware listener asks Coherence to ensure the listener receives
            // all events; either this listener has chosen to not be version aware
            // or already implements the required interface
            return listener;
            }

        return listener.isSynchronous() || listener instanceof MapListenerSupport.SynchronousListener
            ? new DefaultVersionedSynchronousListener<>(listener, versions)
            : new DefaultVersionedListener<>(listener, versions);
        }

    // ----- inner class: DefaultVersionedListener --------------------------

    /**
     * A default {@link VersionAwareMapListener} implementation that tracks
     * received versions.
     *
     * @param <K> key of the map
     * @param <V> value of the map
     */
    public static class DefaultVersionedListener<K,V>
            extends MapListenerSupport.WrapperListener<K,V>
            implements VersionAwareMapListener<K, V>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a DefaultVersionedListener with the provided listener.
         *
         * @param listener  the listener to delegate to
         */
        protected DefaultVersionedListener(MapListener<K, V> listener)
            {
            this(listener, null);
            }

        /**
         * Create a DefaultVersionedListener with the provided listener and
         * partition versions.
         *
         * @param listener  the listener to delegate to
         * @param versions  the versions to start receiving events from
         */
        protected DefaultVersionedListener(MapListener<K, V> listener, DefaultVersionedPartitions versions)
            {
            super(listener);

            f_partVersions = versions == null
                    ? new DefaultVersionedPartitions() : versions;
            }

        // ----- MapListener methods ----------------------------------------

        @Override
        public void entryInserted(MapEvent<K, V> evt)
            {
            process(evt, f_listener::entryInserted);
            }

        @Override
        public void entryUpdated(MapEvent<K, V> evt)
            {
            process(evt, f_listener::entryUpdated);
            }

        @Override
        public void entryDeleted(MapEvent<K, V> evt)
            {
            process(evt, f_listener::entryDeleted);
            }

        @Override
        public int characteristics()
            {
            return ASYNCHRONOUS | VERSION_AWARE;
            }

        // ----- VersionAwareMapListener methods ----------------------------

        @Override
        public long getCurrentVersion()
            {
            // if the caller is asking for the current version without a partition
            // we can safely assume there is only a single partition in question
            synchronized (f_partVersions)
                {
                return VersionAwareMapListener.super.getCurrentVersion();
                }
            }

        @Override
        public VersionedPartitions getVersions()
            {
            return f_partVersions;
            }

        // ----- object methods ---------------------------------------------

        @Override
        public boolean equals(Object oThat)
            {
            if (oThat == null || !((MapListener) oThat).isVersionAware())
                {
                return false;
                }

            return Base.equals(f_listener, MapListenerSupport.unwrap((MapListener) oThat));
            }

        // ----- helpers ----------------------------------------------------

        /**
         * Provess the given event by updating the tracked versions and delegating
         * to the wrapped if listener iff the event has not been seen.
         *
         * @param event     received event
         * @param delegate  consumer of event
         */
        protected void process(MapEvent<K, V> event, Consumer<MapEvent<K, V>> delegate)
            {
            long lEventVersion    = event.getVersion();
            int  iPart            = event.getPartition();
            long lExpectedVersion = getCurrentVersion(iPart);

            if (lExpectedVersion <= VersionAwareMapListener.HEAD)
                {
                // accept this event as the first event (for the associated partition,
                // dropping previous events and recording gaps for future events
                lExpectedVersion = lEventVersion;
                }

            long lPartVersion = encodePartitionVersion(iPart, lEventVersion);
            if (lEventVersion >= lExpectedVersion && !f_laProcessedEvents.exists(lPartVersion))
                {
                // process the event
                try
                    {
                    // detect a version update and set future expectations
                    // accordingly, but do not delegate to the user map listener
                    if (!(event instanceof CacheEvent &&
                            ((CacheEvent<K, V>) event).isVersionUpdate()))
                        {
                        delegate.accept(event);
                        }
                    }
                finally
                    {
                    boolean fExpected = lEventVersion == lExpectedVersion;
                    if (fExpected)
                        {
                        removeProcessed(iPart, lEventVersion);
                        }
                    else
                        {
                        synchronized (f_laProcessedEvents)
                            {
                            f_laProcessedEvents.set(lPartVersion, null);
                            }

                        // check if we received an event that is larger than the gap threshold
                        if (lEventVersion - lExpectedVersion > GAP_THRESHOLD)
                            {
                            // count all processed events
                            int  cProcessed = 0;
                            long lNextPart  = encodePartitionVersion(iPart + 1, 0L);
                            for (LongArray.Iterator iter = f_laProcessedEvents.iterator(encodePartitionVersion(iPart, 0L));
                                 iter.hasNext(); )
                                {
                                iter.next();

                                if (iter.getIndex() >= lNextPart)
                                    {
                                    break;
                                    }

                                cProcessed++;
                                }

                            // if we are really have surpassed the gap threshold
                            // then start dropping events
                            if (cProcessed >= GAP_THRESHOLD)
                                {
                                removeProcessed(iPart, lExpectedVersion);
                                }
                            }
                        }
                    }
                }
            // else - skip
            }

        /**
         * Remove all processed events greater than the provided event version.
         *
         * @param iPart          the partition
         * @param lEventVersion  the event version
         */
        protected void removeProcessed(int iPart, long lEventVersion)
            {
            // Note: if the partition slice had no mutations the server will
            //       return VAML.ALL as the event version; this allows re-registration
            //       to request all versions if an event is not received prior
            //       to disconnect
            long lExpectedVersion = lEventVersion >= 0
                    ? lEventVersion + 1
                    : lEventVersion;

            if (lExpectedVersion > 0)
                {
                long lExpectedPartVersion = encodePartitionVersion(iPart, lExpectedVersion);

                synchronized (f_laProcessedEvents)
                    {
                    long    lMinPartVersion = f_laProcessedEvents.ceilingIndex(encodePartitionVersion(iPart, 0L));
                    boolean fWrap           = lMinPartVersion < lExpectedPartVersion;
                    for (LongArray.Iterator iter = f_laProcessedEvents.iterator(lExpectedPartVersion); iter.hasNext(); )
                        {
                        iter.next();

                        long    lPartVersion = iter.getIndex();
                        boolean fPartsMatch  = decodePartition(lPartVersion) == iPart;
                        if (lPartVersion != lExpectedPartVersion || !fPartsMatch)
                            {
                            if (!fWrap || fPartsMatch)
                                {
                                break;
                                }

                            iter                 = f_laProcessedEvents.iterator(lMinPartVersion);
                            lExpectedPartVersion = lMinPartVersion;
                            fWrap                = false;
                            }
                        //else
                        iter.remove();
                        lExpectedPartVersion++;
                        lExpectedVersion++; // increment the non truncated version
                        }
                    }
                }

            setCurrentVersion(iPart, lExpectedVersion);
            }

        // ----- internal methods -------------------------------------------

        /**
         * Return the current version for the given partition.
         *
         * @param lPart  the partition being requested
         *
         * @return the last version processed for the given partition
         */
        private long getCurrentVersion(long lPart)
            {
            return f_partVersions.getVersion((int) lPart);
            }

        /**
         * Set the current version for the provided partition.
         *
         * @param lPart     the partition to update
         * @param lVersion  the last received version
         */
        private void setCurrentVersion(long lPart, long lVersion)
            {
            f_partVersions.setPartitionVersion((int) lPart, lVersion);
            }

        // ----- static helpers ---------------------------------------------

        /**
         * Return a long with both the provided partition and version encoded.
         *
         * @param iPart     the partition to encode
         * @param lVersion  the version to encode
         *
         * @return a long with both the provided partition and version encoded
         */
        protected static long encodePartitionVersion(int iPart, long lVersion)
            {
            // we only hold the lower 40-bits / 5 bytes of the version as we
            // do not need to support gaps of that size.
            // there is a potential for versions to roll over this boundary
            // and therefore versions stored are *not* comparable and can only
            // be used for direct retrieval and removal

            return ((long) iPart << SHIFT_PARTITION) |
                    (lVersion & MASK_VERSION); // drop the upper 24-bits
            }

        /**
         * Return the partition in the provided encoded partition & version.
         *
         * @param lPartVersion  the partition & version
         *
         * @return the partition
         */
        protected static int decodePartition(long lPartVersion)
            {
            return (int) (lPartVersion >> SHIFT_PARTITION);
            }

        /**
         * Return a potentially lossy version that was encoded in the provided
         * partition & version.
         *
         * @param lPartVersion  the partition & version
         *
         * @return a potentially lossy version
         */
        protected static long decodeVersion(long lPartVersion)
            {
            return lPartVersion & MASK_VERSION; // drop the upper 24-bits
            }

        // ----- constants --------------------------------------------------

        /**
         * The number of shits required to encode (or decode) the partition
         * from an encoded partition & version.
         */
        protected static final int SHIFT_PARTITION = 40;

        /**
         * The number of shits required to encode (or decode) the version
         * from an encoded partition & version.
         */
        protected static final int SHIFT_VERSION = 0;

        /**
         * A bit-mask that can be applied to an encoded partition & version
         * to realize the potentially lossy version.
         */
        protected static final long MASK_VERSION = 0xFFFFFFFFFFL;

        /**
         * A threshold to allow gaps in MapEvent versions. Surpassing this threshold
         * results in events being dropped.
         */
        protected static final int GAP_THRESHOLD = 0xFFFF;

        // ----- data members -----------------------------------------------

        /**
         * A data structure to track partition -> version.
         */
        protected final DefaultVersionedPartitions f_partVersions;

        /**
         * A LongArray of received events.
         */
        protected final LongArray f_laProcessedEvents = new SparseArray();
        }

    // ----- inner class: DefaultVersionedSynchronousListener ---------------

    /**
     * Identical to {@link DefaultVersionedListener} except synchronous.
     *
     * @param <K>  the key of the map
     * @param <V>  the value of the map
     */
    public static class DefaultVersionedSynchronousListener<K,V>
            extends DefaultVersionedListener<K, V>
            implements MapListenerSupport.SynchronousListener<K, V>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a DefaultVersionedListener with the provided listener and
         * partition versions.
         *
         * @param listener  the listener to delegate to
         * @param versions  the versions to start receiving events from
         */
        protected DefaultVersionedSynchronousListener(MapListener<K, V> listener, DefaultVersionedPartitions versions)
            {
            super(listener, versions);
            }

        // ----- MapListener methods ----------------------------------------
        
        @Override
        public int characteristics()
            {
            return SYNCHRONOUS | VERSION_AWARE;
            }
        }
    }
