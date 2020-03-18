/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.internal;

import com.tangosol.net.Member;
import com.tangosol.net.Service;

import java.util.Set;


/**
* Stream which utilizes information regarding the state of the TCMP protocol
* when streaming messages.  WrapperStreamFactory implementations may return
* streams that implement this interface in order to obtain information on the
* state of the protocol.
*
* @see com.tangosol.io.WrapperStreamFactory
*
* @deprecated As of Coherence 3.7, deprecated with no replacement.
*
* @author mf 2006.08.09
*/
@Deprecated
public interface ProtocolAwareStream
    {
    /**
    * Set the current protocol context.
    *
    * @param context the current context of the protocol.
    */
    public void setProtocolContext(ProtocolContext context);


    // ---- interface ProtocolContext ---------------------------------------

    /**
    * Context describing the current state of the protocol with respect to
    * an incoming or outgoing message.
    */
    public interface ProtocolContext
        {
        /**
        * Return the Service associated with the message.
        *
        * @return  the service
        */
        public Service getService();

        /**
        * Indicate if the Service is the ClusterService.
        *
        * @return  true if this context applies to the ClusterService
        */
        public boolean isClusterService();

        /**
        * Return the Member from which the Message originates.
        * <p>
        * For certain incoming messages this value may not be available until
        * after the message has been deserialized.
        *
        * @return  the sending Member
        */
        public Member getFromMember();

        /**
        * Return the set of Members to which the Message is addressed. This
        * method has meaning only for outgoing messages.
        *
        * @return  the recipient members
        */
        public Set getToMemberSet();

        /**
        * Indicate if the context applies to the induction message.
        * <p>
        * The induction message is sent by the cluster senior to a new member
        * once the new member has joined the cluster.
        *
        * @return  true if the context applies to an induction message
        */
        public boolean isInductionMessage();
        }
    }
