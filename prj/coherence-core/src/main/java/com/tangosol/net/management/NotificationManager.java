/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.management;


import javax.management.Notification;


/**
* A NotificationManager is responsible for JMX notification delivery from a
* managed node to a set of subscribing managing nodes.
* <p>
* For Standard MBeans that implement the {@link
* javax.management.NotificationEmitter} interface and Platform MBeans registered
* with the Coherence {@link com.tangosol.net.management.Registry}, notifications
* will be automatically sent to the corresponding MBean within the Coherence
* domain (e.g. "Coherence:type=Platform,Domain=java.lang,subType=Memory,nodeId=1").
* Additionally, application logic can send notifications using the
* NotificationManager as follows:
* <pre>
*   Cluster             cluster  = CacheFactory.ensureCluster();
*   Registry            registry = cluster.getManagement();
*   NotificationManager manager  = registry.getNotificationManger();
*   String              sName    = registry.ensureGlobalName("type=CustomMBean");
*
*   if (manager.isSubscribedTo(sName))
*      {
*      manager.trigger(sName, "custom.notification.type", "Custom notification message");
*      }
* </pre>
*
* @author ew 2010.02.05
* @since Coherence 3.6
*/
public interface NotificationManager
    {
    /**
    * Determine if any subscriptions exist for the specified MBean.
    *
    * @param sName  the MBean name to check for subscriptions
    *
    * @return true iff the specified name identifies the MBean that was
    *         registered by the caller's node and subscriptions exist for that
    *         MBean
    */
    public boolean isSubscribedTo(String sName);

    /**
    * Trigger the notification on subscribers for a given MBean.
    *
    * @param sName     the MBean name
    * @param sType     the notification type
    * @param sMessage  the notification message
    *
    * @throws IllegalArgumentException if an MBean with the specified name
    *         does not exists
    */
    public void trigger(String sName, String sType, String sMessage)
        throws IllegalArgumentException;

    /**
    * Trigger the notification on subscribers for a given MBean.
    * <p>
    * Note: if the specified Notification object has a negative
    * {@link Notification#getSequenceNumber() SequenceNumber}, it will be
    * automatically assigned.
    *
    * @param sName        the MBean name
    * @param notification the notification object
    *
    * @throws IllegalArgumentException if an MBean with the specified name
    *         does not exists
    */
    public void trigger(String sName, Notification notification)
        throws IllegalArgumentException;
    }
