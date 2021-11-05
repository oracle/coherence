/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor.internal;

import com.tangosol.net.Member;
import com.tangosol.net.MemberEvent;
import com.tangosol.net.MemberListener;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.UID;

/**
 * A {@link ClusterMemberAware} object is aware of its cluster {@link Member} wishes
 * to receive Member lifecycle events. See {@link MemberListener}.
 *
 * @author phf
 * @since 21.12
 */
public interface ClusterMemberAware
    {
    /**
     * The {@link UID} of the {@link Member} for which this object wishes to receive
     * membership events. May return {@code null} which means do not receive membership
     * events.
     *
     * @return a {@link UID}
     */
    UID getUid();

    /**
     * A callback invoked as part of an {@link InvocableMap.EntryProcessor} to perform
     * custom operations when the {@link Member} has joined the cluster.
     *
     * @return <code>true</code> if the {@link ClusterMemberAware} state was mutated
     *         and should be saved, <code>false</code> otherwise
     *
     * @see MemberListener#memberJoined(MemberEvent)
     */
    boolean onMemberJoined();

    /**
     * A callback invoked as part of an {@link InvocableMap.EntryProcessor} to perform
     * custom operations when the {@link Member} is leaving the cluster.
     *
     * @return <code>true</code> if the {@link ClusterMemberAware} state was mutated
     *         and should be saved, <code>false</code> otherwise
     */
    boolean onMemberLeaving();

    /**
     * A callback invoked as part of an {@link InvocableMap.EntryProcessor} to perform
     * custom operations when the {@link Member} is leaving the cluster.
     * <p>
     * See {@link MemberListener#memberLeft(MemberEvent)}.
     *
     * @return <code>true</code> if the {@link ClusterMemberAware} state was mutated
     *         and should be saved, <code>false</code> otherwise
     */
    boolean onMemberLeft();
    }
