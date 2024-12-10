/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package logging;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.Member;
import com.tangosol.net.management.MBeanServerProxy;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import java.io.Serializable;

public class AbstractLoggerTests
        extends AbstractFunctionalTest
        implements Serializable
    {
    protected Void changeCoherenceLogLevel(int nLevel)
        {
        Cluster          cluster = CacheFactory.getCluster();
        Member           member  = cluster.getLocalMember();
        MBeanServerProxy proxy   = cluster.getManagement().getMBeanServerProxy();

        proxy.setAttribute("type=Node,nodeId=" + member.getId(), "LoggingLevel", nLevel);
        return null;
        }
    }
