/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.listener;

import com.tangosol.net.NamedMap;
import com.tangosol.net.PartitionedService;

import com.tangosol.net.partition.DefaultVersionedPartitions;
import com.tangosol.net.partition.VersionAwareMapListener;
import com.tangosol.net.partition.VersionedPartitions;

import com.tangosol.util.Base;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.PrimitiveSparseArray;

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

        // TODO: we MUST initialize the wrapper listener with the relevant starting
        //       versions in case a restart occurs prior to receiving any events
        //       for that partition, as we will end up getting the tip which could
        //       miss events that occurred during the restart

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
            long lEventVersion = event.getVersion();
            long lPart         = event.getPartition();

            if (lEventVersion >= getCurrentVersion(lPart))
                {
                try
                    {
                    delegate.accept(event);
                    }
                finally
                    {
                    setCurrentVersion(lPart, lEventVersion + 1);
                    }
                }
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

        // ----- data members -----------------------------------------------

        /**
         * A data structure to track partition -> version.
         */
        protected final DefaultVersionedPartitions f_partVersions;
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
