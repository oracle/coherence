/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.partition;

import com.tangosol.net.NamedCache;
import com.tangosol.util.MapListener;
import com.tangosol.util.PrimitiveSparseArray;

/**
 * Direct implementations of this interface are generally considered advanced
 * use cases, such as handling event receiver (client process) restarts. Commonly
 * {@link MapListener} implementations simply need to suggest that they are
 * {@link MapListener#VERSION_AWARE version aware}. Coherence will automatically
 * create an implementation of this interface and track partition versions for
 * relevant partitions and re-register to replay missed events. A more detailed
 * description of the semantics for implementors of this interface is described
 * below.
 * <p>
 * Implementations of this interface suggest that they are aware of event versions
 * ({@link com.tangosol.util.MapEvent#getPartition() partition} &
 * {@link com.tangosol.util.MapEvent#getVersion() version}), such that this
 * implementation can be asked for all partition version that were last received
 * or is interested in receiving. Additionally, there is a convenient {@link
 * VersionAwareMapListener#getCurrentVersion() current version} method, which
 * is generally applicable when there is a single partition being tracked.
 * <p>
 * The primary reason to be version aware is to allow the receiver/client to track the
 * last event it received. If the receiver ever becomes disconnected from the
 * source of events, it allows the receiver to ask the source for all events
 * that were missed. The source may replay:
 * <ol>
 *     <li>all the events from the last event received to the latest</li>
 *     <li>only the events that were retained, if some were purged</li>
 *     <li>an event representing the latest modification</li>
 *     <li>no events if the source does not support event history</li>
 * </ol>
 * There are several reasons why the source of events (ownership enabled members)
 * may become disconnected from the receiver of events.
 * <ol>
 *     <li><b>Network disruption</b> - event receiver becomes disconnected from
 *     the source / the rest of the cluster</li>
 *     <li><b>Proxy (Extend/gRPC) disruption</b> - a proxy the client was connected
 *     to dies and therefore reconnects to a different proxy</li>
 *     <li><b>Client death</b> - client process dies but the same logical process
 *     restarts</li>
 * </ol>
 * For cases 1 & 2, having to "reconnect" to the source is automatically handled
 * by Coherence, thus this interface allows Coherence to which version for
 * each partition was last read and request the replay with the aforementioned
 * considerations.
 * <p>
 * For the 3rd case it requires a call to {@link NamedCache#addMapListener(MapListener)
 * addMapListener} passing an implementation of this interface that returns the
 * relevant versions. Clearly for the 3rd case it requires the client to persist
 * the partition version information such that it can be recovered and replay
 * requested from the event source.
 *
 * @author hr  2021.01.29
 * @since Coherence 21.06
 */
public interface VersionAwareMapListener<K, V>
        extends MapListener<K, V>
    {
    /**
     * Return a data structure that holds a mapping of {@code partition -> version}.
     * The absence of a partition suggests no interest in receiving events emanating
     * from said partition.
     * <p>
     * The version represents the last version that was read. If this listener
     * is being registered (either for the first or n<sup>th</sup> time) this
     * method will be called and passed to the source to allow any missed events
     * to be replayed. Additionally, implementations can return any of the following
     * formally defined constants (the behavior is performed in addition to
     * the registration of this listener with the source):
     * <ol>
     *     <li>{@link #HEAD} - send all future versions</li>
     *     <li>{@link #PRIMING} - send current and all future versions</li>
     *     <li>{@link #ALL} - send all known versions</li>
     * </ol>
     * <p>
     * A listener is always registered under the context of a key or a filter.
     * A key maps to a single partition while a filter generally maps to all
     * partitions, therefore the registrations are either targeted to a single
     * node or multiple nodes respectively.
     * <p>
     * When replaying events if the listener was registered under the context
     * of a key (or many keys) the events that are replayed will only be for
     * said key(s) and not other keys owned by the same partition. Conversely,
     * when events are replayed for filter-based registrations events relating
     * to <b>all</b> keys for the returned partitions are replayed. In both cases,
     * only future events for the given partition versions are replayed.
     *
     * @return a data structure that holds a mapping of {@code partition -> version}
     *         representing the last received versions thus can be sent to the
     *         source to replay missing events
     */
    public VersionedPartitions getVersions();

    /**
     * A convenience method that returns a single version with the same rules
     * as described {@link #getVersions() here}. Callers of this method can generally
     * assume the partition(s) this version relates to. Often this is due to
     * the listener being registered with a key.
     *
     * @return the last version received
     */
    public default long getCurrentVersion()
        {
        VersionedPartitions versions = getVersions();

        long lVersionMin = Long.MAX_VALUE;
        for (VersionedPartitions.VersionedIterator iter = versions.iterator(); iter.hasNext(); )
            {
            lVersionMin = Math.min(lVersionMin, iter.nextVersion());
            }
        return lVersionMin;
        }

    /**
     * A constant that suggests to an event source to send all future events.
     */
    public static long HEAD    =  0L;

    /**
     * A constant that suggests to an event source to send the current version
     * and all future versions.
     */
    public static long PRIMING = -1L;

    /**
     * A constant that suggests to an event source to send all known versions
     * (subject to the event source's retention policy) and all future versions.
     */
    public static long ALL     = -2L;
    }
