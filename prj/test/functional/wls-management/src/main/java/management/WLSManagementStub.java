/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package management;

import com.oracle.coherence.common.base.Logger;
import com.tangosol.coherence.http.DefaultHttpServer;
import com.tangosol.coherence.management.RestManagement;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.management.MBeanHelper;
import org.glassfish.jersey.jettison.JettisonConfig;
import org.glassfish.jersey.jettison.JettisonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.management.MBeanServer;
import javax.ws.rs.ApplicationPath;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A server stub that runs Coherence and configures a Management over REST endpoint
 * the same way that WLS would.
 */
public class WLSManagementStub
    {
    public static void main(String[] args) throws Exception
        {
        DefaultCacheServer.startServerDaemon().waitForServiceStart();

        Map<String, ResourceConfig> mapConfig = new HashMap<>();

        mapConfig.put(WlsManagementInfoResourceTests.URI_ROOT, new ManagementApp());

        s_server = new DefaultHttpServer();
        s_server.setLocalAddress("0.0.0.0");
        s_server.setLocalPort(0);
        s_server.setResourceConfig(mapConfig);
        s_server.start();

        Logger.info("Management server listening on http://localhost:" + s_server.getListenPort());
        s_fRunning = true;
        }

    public static boolean isRunning()
        {
        return s_fRunning;
        }

    public static int getHttpPort()
        {
        return s_server.getListenPort();
        }

    // ----- inner class: ManagementApp -------------------------------------

    @ApplicationPath(WlsManagementInfoResourceTests.URI_ROOT)
    public static class ManagementApp
            extends ResourceConfig
        {
        public ManagementApp()
            {
            register(JettisonConfig.DEFAULT);
            register(JettisonFeature.class);

            String      sName      = CacheFactory.getCluster().getClusterName();
            Set<String> setCluster = Collections.singleton(sName);
            boolean     fMetrics   = Boolean.getBoolean("test.management.metrics");

            if (fMetrics)
                {
                RestManagement.configureMetrics(this, new MBeanServerSupplier(), () -> setCluster);
                }
            else
                {
                RestManagement.configure(this, new MBeanServerSupplier(), () -> setCluster);
                }
            }
        }

    // ----- inner class: MBeanServerSupplier -------------------------------

    public static class MBeanServerSupplier
            implements Supplier<MBeanServer>
        {
        @Override
        public MBeanServer get()
            {
            return MBeanHelper.findMBeanServer();
            }
        }

    // ----- data members ---------------------------------------------------

    private static DefaultHttpServer s_server;

    private static volatile boolean s_fRunning = false;
    }
