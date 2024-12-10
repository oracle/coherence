/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import com.tangosol.util.UID;
import com.tangosol.util.UUID;

import java.net.InetAddress;

/**
* The Member interface represents a cluster member.
*
* @author gg  2002.02.08
*
* @since Coherence 1.1
*/
public interface Member
        extends MemberIdentity
    {
    /**
    * Return the IP address of the Member's DatagramSocket for
    * point-to-point communication.
    *
    * @return the IP address of the Member's DatagramSocket
    */
    public InetAddress getAddress();

    /**
    * Return the port of the Member's DatagramSocket for
    * point-to-point communication.
    *
    * @return the port of the Member's DatagramSocket
    */
    public int getPort();

    /**
    * Return the date/time value (in cluster time) that the Member joined.
    *
    * @return the cluster date/time value that the Member joined
    */
    public long getTimestamp();

    /**
    * Return the unique identifier of the Member.
    *
    * @return the unique identifier of the Member
    */
    public UID getUid();

    /**
    * Return a small number that uniquely identifies the Member at this point
    * in time and does not change for the life of this Member.
    * <p>
    * This value sometimes referred to as a "mini-id" in comparison to the
    * "Uid" returned by {@link #getUid()}. It does not uniquely identify the
    * Member throughout the duration of the cluster because Members that
    * existed but left the cluster before this Member existed may have had
    * the same mini-id value and the same goes for Members that may join the
    * cluster after this Member leaves the cluster.
    *
    * @return the mini-id of the Member
    *
    * @since Coherence 1.2
    */
    public int getId();

    /**
     * Return the universal unique id of the Member.
     *
     * @return the universal unique id of the Member
     *
     * @since 22.06
     */
    default public UUID getUuid()
        {
        throw new UnsupportedOperationException();
        }

    /**
     * Check whether or not this {@code Member} represents a remote client.
     *
     * @return {@code true} if this {@code Member} is a remote client; {@code false} otherwise
     *
     * @since 22.06
     */
    default boolean isRemoteClient()
        {
        return getId() == 0;
        }
    }
