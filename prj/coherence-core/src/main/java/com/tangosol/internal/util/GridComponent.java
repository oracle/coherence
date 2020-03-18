/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;


import com.tangosol.net.Service;

import com.tangosol.net.management.NotificationManager;


/**
 * GridComponent is an internal interface to expose internal methods in the Grid
 * TDE component.
 *
 * @author gg 2015.12.10
 */
public interface GridComponent
        extends Service
    {
    /**
     * Dispatch a notification to subscribers of the specified MBean.
     * <p>
     * Note: unlike a direct call to {@link NotificationManager#
     * trigger(String, String, String)}  trigger(...)} this method will invoke
     * the <i>trigger()</i> call on the event dispatcher thread.
     *
     * @param sType      the notification type
     * @param sMessage   the notification message
     * @param oUserData  the notification user data (must be an intrinsic or
     *                   an OpenType value)
     */
    public void dispatchNotification(
        String sMBeanName, String sType, String sMessage, Object oUserData);
    }
