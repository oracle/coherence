/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;

import com.tangosol.net.Cluster;
import com.tangosol.net.Coherence;
import com.tangosol.net.Service;
import org.junit.Test;

import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ServiceSuspendTests
    {
    @Test
    public void shouldSuspendServices() throws Exception
        {
        Coherence           coherence    = Coherence.clusterMember().start().get(5, TimeUnit.MINUTES);
        Cluster             cluster      = coherence.getCluster();
        Enumeration<String> serviceNames = cluster.getServiceNames();

        while (serviceNames.hasMoreElements())
            {
            String  sName   = serviceNames.nextElement();
            Service service = cluster.getService(sName);
            System.err.println("Suspending service " + sName);
            assertThat(service.isSuspended(), is(false));
            cluster.suspendService(sName);
            assertThat(service.isSuspended(), is(true));
            System.err.println("Resuming service " + sName);
            cluster.resumeService(sName);
            assertThat(service.isSuspended(), is(false));
            }
        }
    }
