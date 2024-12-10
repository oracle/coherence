/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.federation;

import com.tangosol.net.events.federation.FederatedChangeEvent;

import com.tangosol.util.Binary;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;

/**
 * Provides a mechanism to represent and mutate a Cache Entry that was changed
 * in a Federated Cache.
 * <p>
 * {@link ChangeRecord}s are typically provided to {@link com.tangosol.net.events.EventInterceptor}s
 * as part of a {@link FederatedChangeEvent} for the purpose of inspecting and
 * possibly modifying an entry prior to it being committed.
 * <p>
 * {@link com.tangosol.net.events.EventInterceptor}s may choose to
 * {@link #rejectChange() reject the change} or
 * {@link #setValue(Object) change the value}.  When no change is specified
 * the modified value is committed.
 * <p>
 * Any updates to a {@link ChangeRecord} will overwrite the modified Entry and
 * thus the updated value when the change is committed.
 * <p>
 * When {@link ChangeRecord} is for a local participant, the original and
 * local entry will be the same.
 *
 * @author bb
 */
public interface ChangeRecord<K, V>
    {
    /**
     * Obtain the key of the changed {@link Entry}.
     *
     * @return  the entry key
     */
    public K getKey();

    /**
     * Obtain a representation of the modified {@link Entry}.
     * <p>
     * This {@link Entry} will be used to modify the associated {@link Entry}
     * in the destination participant.
     *
     * @return modified entry
     */
    public Entry<K, V> getModifiedEntry();

    /**
     * Obtain a representation of the {@link Entry} prior to any changes
     * occurring.
     * <p>
     * This is typically a representation of the {@link Entry} at the
     * source participant prior to a change occurring.
     *
     * @return  the original entry
     */
    public Entry<K, V> getOriginalEntry();

    /**
     * Obtains a representation of {@link Entry} as it appears in the local
     * participant.
     *
     * @return  the local entry
     */
    public Entry<K, V> getLocalEntry();

    /**
     * The type of the FederatedChangeEvent.
     *
     * @return the type of the FederatedChangeEvent
     *
     * @since 12.2.1.0.2
     */
    FederatedChangeEvent.Type getEventType();

    /**
     * Update the modified {@link Entry} value.
     * <p>
     * This method can potentially cause the change to be propagated back to
     * the origin participant. Care must be taken to avoid any recursive
     * ping-pong cycles.
     *
     * @param newValue  new value for the modified entry
     */
    public void setValue(V newValue);

    /**
     * Update the modified {@link Entry} value with a synthetic flag.
     * <p>
     * This method can potentially cause the change to be propagated back
     * to the origin participant. Care must be taken to avoid any recursive
     * ping-pong cycles.
     *
     * <p>
     * Note: Marking a change as synthetic will have different behavior
     *       depending on the federation event type:
     *
     *       COMMITTING_LOCAL - the change will not be federated to
     *       other clusters and CacheStores will not be called.
     *
     *       REPLICATING - setting the change synthetic is not allowed.
     *       An UnsupportedOperationException will be thrown.
     *
     *       COMMITTING_REMOTE - the change will be applied as
     *       a synthetic change. CacheStores will not be triggered and
     *       federation will not forward the change to other clusters
     *       from this destination cluster.
     *
     * @param newValue    new value for the modified entry
     * @param fSynthetic  a flag to indicate whether the update is synthetic
     *
     * @since 12.2.1.4.1
     */
    public void setValue(V newValue, boolean fSynthetic);

    /**
     * Update the modified Entry's value using the given {@link ValueUpdater}.
     *
     * @param updater  a ValueUpdater used to modify the Entry's value
     * @param oValue   the new value for this Entry
     */
    public <T> void update(ValueUpdater<V, T> updater, T oValue);

    /**
     * Signal that the modified {@link Entry} should not be applied.
     * The local {@link Entry} should remain unchanged.
     */
    public void rejectChange();

    /**
     * Determine if the modified entry in the ChangeRecord was deleted.
     *
     * @return true if the entry was deleted
     */
    public boolean isDeleted();

    /**
     * Obtain a {@link Binary} representation of the {@link ChangeRecord}, with
     * the key and values in {@link Binary} format.
     *
     * @return  Binary ChangeRecord
     */
    public ChangeRecord<Binary, Binary> getBinaryChangeRecord();

    /**
     * Set the local only flag to indicate that the change is not
     * federated.
     * <p>
     * Note: this setting is only applicable to {@link FederatedChangeEvent.Type#COMMITTING_LOCAL}
     * events.
     *
     * @param bLocal  a flag to indicate whether to keep the change local only
     *
     * @since 12.2.1.0.3
     */
    void setLocalOnly(boolean bLocal);

    /**
     * Determine whether the change represented by this ChangeRecord is
     * local only, therefore not federated.
     * <p>
     * Note: this setting is only applicable to {@link FederatedChangeEvent.Type#COMMITTING_LOCAL}
     * events.
     *
     * @return  a boolean value to indicate whether the change is local only
     *
     * @since 12.2.1.0.3
     */
    boolean isLocalOnly();

    /**
     * Determine if this Entry has been synthetically mutated. This method
     * returns {@code false} if either a non-synthetic update was made or
     * the entry has not been modified.
     *
     * @return true if the Entry has been synthetically mutated
     *
     * @since 12.2.1.4.1
     */
    boolean isSynthetic();

    /**
     * Represents the key, value and name of the {@link Participant} from
     * which the entry originated.
     */
    public interface Entry<K, V>
        {
        /**
         * Obtain the name of the {@link Participant} from which the entry
         * originated.
         *
         * @return  name of the source {@link Participant}
         */
        public String getSource();

        /**
         * Obtain the key corresponding to this entry.
         *
         * @return  key
         */
        public K getKey();

        /**
         * Obtain the value corresponding to this entry.
         *
         * @return  value
         */
        public V getValue();

        /**
         * Extract a value from the entry value using the {@link ValueExtractor}.
         *
         * @param <T>        the type of value that will be extracted
         * @param extractor  a ValueExtractor to apply to the Entry's key or value
         *
         * @return the extracted value
         */
        public <T> T extract(ValueExtractor<V, T> extractor);
        }
    }
