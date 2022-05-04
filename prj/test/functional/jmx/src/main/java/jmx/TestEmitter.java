/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package jmx;

import com.tangosol.net.CacheFactory;

import javax.management.*;
import java.io.Serializable;

/**
 * The Test Emitter will send out a notification when EmitNotification is called.
 *
 * @author nsa 2012.01.12
 */
public class TestEmitter extends NotificationBroadcasterSupport implements TestEmitterMBean, Serializable
    {
    /**
     * Return the cache size property.
     *
     * @return the cache size property
     */
    public int getCacheSize()
        {
        return m_cacheSize;
        }

    /**
     * Set the cache size.
     *
     * @param cacheSize  the cache size
     */
    public void setCacheSize(int cacheSize)
        {
        int nOld = m_cacheSize;
        m_cacheSize = cacheSize;
        emitNotification(nOld, m_cacheSize);
        }

    /**
     * Emit a notification.
     */
    public void EmitNotification()
        {
        emitNotification(25, 75);
        }

    /**
     * Emit a notification.
     */
    public void emitNotification(int nOld, int nNew)
        {
        Notification notification =
                new AttributeChangeNotification(this, m_sequenceNumber++, System.currentTimeMillis(),
                        "CacheSize changed", "CacheSize", "int", nOld, nNew);

        CacheFactory.log("TestEmitter - Emitting notification: " + notification + " old=" + nOld + " new=" + nNew, CacheFactory.LOG_INFO);
        sendNotification(notification);
        }

    public String getMBeanName()
        {
        return m_sMBeanName;
        }

    public void setMBeanName(String sMBeanName)
        {
        m_sMBeanName = sMBeanName;
        }

    @Override
    public MBeanNotificationInfo[] getNotificationInfo()
        {
        String[] types = new String[]
            {
            AttributeChangeNotification.ATTRIBUTE_CHANGE
            };

        String name = AttributeChangeNotification.class.getName();
        String description = "An attribute of this MBean has changed";
        MBeanNotificationInfo info = new MBeanNotificationInfo(types, name, description);

        return new MBeanNotificationInfo[] {info};
        }

    @Override
    public String toString()
        {
        return "TestEmitter(" +
                "mBeanName='" + m_sMBeanName + '\'' +
                ')';
        }

    /**
     * The size of the cache
     */
    private int m_cacheSize = DEFAULT_CACHE_SIZE;

    /**
     * Default value for cache size
     */
    private static final int DEFAULT_CACHE_SIZE = 200;

    /**
     * The sequence number for the notifications
     */
    private long m_sequenceNumber = 1;

    /**
     * The name of this MBean.
     */
    private String m_sMBeanName;

    /**
     * The name of the bean to register
     */
    public static String EMITTER_NAME = "com.example:Type=TestEmitter";
    }
