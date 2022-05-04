/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.tutorials.persistence;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.oracle.coherence.common.base.Blocking;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.management.Registry;
import com.tangosol.persistence.CachePersistenceHelper;

import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

/**
 * A class to listen for Persistence Notifications and record the
 * duration of the operations. Please see {@link com.tangosol.persistence.PersistenceManagerMBean} for
 * descriptions of each of the JMX notifications.<br>
 * The supported notifications are:
 * <ul>
 *   <li>recover.begin</li>
 *   <li>recover.end</li>
 *   <li>create.snapshot.begin</li>
 *   <li>create.snapshot.end</li>
 *   <li>recover.snapshot.begin</li>
 *   <li>recover.snapshot.end</li>
 *   <li>remove.snapshot.begin</li>
 *   <li>remove.snapshot.end</li>
 *   <li>archive.snapshot.begin</li>
 *   <li>archive.snapshot.end</li>
 *   <li>retrieve.archived.snapshot.begin</li>
 *   <li>retrieve.archived.snapshot.end</li>
 *   <li>remove.archived.snapshot.begin</li>
 *   <li>remove.archived.snapshot.end</li>
 * </ul>
 *
 * @author tam  2022-04-11
 */
public class NotificationWatcher {

    /**
     * map containing the registered listeners and their object name.
     */
    private static final Map<ObjectName, NotificationListener> mapListeners = new HashMap<>();

    /**
     * Run the example.
     *
     * @param args array of services to list to for notifications.
     */
    // #tag::main[]
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide a list of services to listen for notifications on");
            System.exit(1);
        }
        
        Set<String> setServices = new HashSet<>(Arrays.asList(args));

        System.out.println("\n\nGetting MBeanServer...");

        Cluster cluster = CacheFactory.ensureCluster(); // <1>
        MBeanServer server = MBeanHelper.findMBeanServer();
        Registry registry = cluster.getManagement();

        if (server == null) {
            throw new RuntimeException("Unable to find MBeanServer");
        }

        try {
            for (String serviceName : setServices) {
                System.out.println("Registering listener for " + serviceName);

                String mBeanName = "Coherence:" + CachePersistenceHelper.getMBeanName(serviceName);  // <2>

                waitForRegistration(registry, mBeanName);  // <3>

                ObjectName           beanName = new ObjectName(mBeanName);
                NotificationListener listener = new PersistenceNotificationListener(serviceName);

                server.addNotificationListener(beanName, listener, null, null);  // <4>
                mapListeners.put(beanName, listener);  
            }

            System.out.println("Waiting for notifications. Use CTRL-C to interrupt.");

            Thread.sleep(Long.MAX_VALUE);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            // unregister all registered notifications
            mapListeners.forEach((k, v) -> {
                try {
                    server.removeNotificationListener(k, v);
                }
                catch (Exception eIgnore) {
                    // ignore
                }
            });
        }
    }
    // #end::main[]

    /**
     * Wait for the given MBean to be registered.
     *
     * @param registry    registry to use
     * @param mBeanName  the MBean to wait for
     *
     * @throws InterruptedException if the Mbean is not registered
     */
    public static void waitForRegistration(Registry registry, String mBeanName)
            throws InterruptedException {
        int nMaxRetries = 3 * 1000;  // 30 seconds , 3,000 * 10ms wait;

        while (!registry.getMBeanServerProxy().isMBeanRegistered(mBeanName)) {
            Blocking.sleep(10L);

            if (--nMaxRetries == 0) {
                throw new RuntimeException("Unable to find registered MBean " + mBeanName);
            }
        }
    }

    /**
     * Class to respond to JMX notifications.
     */
    // #tag::listener1[]
    public static class PersistenceNotificationListener
            implements NotificationListener {
    // #end::listener1[]
        /**
         * Suffix for notification begin.
         */
        private static final String BEGIN = ".begin";

        /**
         * Suffix for notification end.
         */
        private static final String END = ".end";

        /**
         * Service name for the listener.
         */
        private final String serviceName;

        /**
         * Map of notifications and the start time.
         */
        private final Map<String, Long> mapNotify = new ConcurrentHashMap<>();

        /**
         * Event counter.
         */
        private static final AtomicInteger counter = new AtomicInteger();

        /**
         * Construct a new listener for the given service.
         *
         * @param serviceName  service name to listen for
         */
        public PersistenceNotificationListener(String serviceName) {
            this.serviceName = serviceName;
        }

        // #tag::handleNotification[]
        @Override
        public synchronized void handleNotification(Notification notification, Object oHandback) {
            counter.incrementAndGet();
            
            String userData = notification.getUserData().toString();
            String message  = notification.getMessage() + " " + notification.getUserData();    // default

            // determine if it's a begin or end notification
            String type = notification.getType();

            if (type.indexOf(BEGIN) > 0) {  // <1>
                // handle begin notification and save the start time
                mapNotify.put(type, notification.getTimeStamp());
                message = notification.getMessage();
            }
            else if (type.indexOf(END) > 0) {  // <2>
                // handle end notification and try and find the matching begin notification
                String begin = type.replaceAll(END, BEGIN);
                Long   start = mapNotify.get(begin);

                if (start != null) {
                    message = "  " + notification.getMessage()
                              + (userData == null || userData.isEmpty() ? "" : userData) + " (Duration="
                              + (notification.getTimeStamp() - start) + "ms)";
                    mapNotify.remove(begin);
                }
            }
            else {
                message = serviceName + ": " + type + "";
            }

            System.out.println(new Date(notification.getTimeStamp()) + " : " + serviceName + " (" + type + ") " + message);
        }
        // #end::handleNotification[]

        public int getEventCount() {
            return counter.get();
        }
    }
}
