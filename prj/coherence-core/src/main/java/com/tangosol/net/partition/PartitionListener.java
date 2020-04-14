/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.partition;

import java.util.EventListener;


/**
* The listener interface for receiving PartitionEvents.
*
* @author cp  2007.05.18
* @since Coherence 3.3
*/
public interface PartitionListener
        extends EventListener
    {
    /**
    * Invoked when a partition event has occurred.
    * <p>
    * This interface should be considered as a very advanced feature, so a
    * PartitionListener implementation  must exercise extreme caution during
    * event processing since any delay or unhandled exception could cause a
    * delay in or complete shutdown of the corresponding partitioned service.
    *
    * @param evt  the PartitionEvent object containing the information about
    *             the event that has occurred
    */
    public void onPartitionEvent(PartitionEvent evt);
    }
