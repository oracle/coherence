/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid;


import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;


/**
* Unit test for the ProxyServiceLoad TDE implementation.
*
* @author jh  2010.12.13
*/
public class ProxyServiceLoadTest
    {
    @Test
    public void testEquals()
        {
        ProxyService.ServiceLoad load1 = new ProxyService.ServiceLoad();
        assertEquals(load1, load1);

        ProxyService.ServiceLoad load2 = new ProxyService.ServiceLoad();
        assertEquals(load1, load2);

        load1.setConnectionCount(1);
        assertFalse(load1.equals(load2));

        load1.setConnectionCount(0);
        assertTrue(load1.equals(load2));
        load1.setConnectionPendingCount(1);
        assertFalse(load1.equals(load2));

        load1.setConnectionPendingCount(0);
        assertTrue(load1.equals(load2));
        load1.setDaemonCount(2);
        load1.setDaemonActiveCount(1);
        assertFalse(load1.equals(load2));

        load1.setDaemonCount(0);
        load1.setDaemonActiveCount(0);
        assertTrue(load1.equals(load2));
        load1.setMessageBacklogIncoming(1);
        assertFalse(load1.equals(load2));

        load1.setMessageBacklogIncoming(0);
        assertTrue(load1.equals(load2));
        load1.setMessageBacklogOutgoing(1);
        assertFalse(load1.equals(load2));

        load1.setMessageBacklogOutgoing(0);
        assertTrue(load1.equals(load2));
        }

    @Test
    public void testCompareTo()
        {
        ProxyService.ServiceLoad load1 = new ProxyService.ServiceLoad();
        assertTrue(load1.compareTo(load1) == 0);

        ProxyService.ServiceLoad load2 = new ProxyService.ServiceLoad();
        assertTrue(load1.compareTo(load2) == 0);
        assertTrue(load2.compareTo(load1) == 0);

        load1.setConnectionCount(2);
        assertTrue(load1.compareTo(load2) > 0);
        assertTrue(load2.compareTo(load1) < 0);

        load2.setConnectionCount(1);
        assertTrue(load1.compareTo(load2) > 0);
        assertTrue(load2.compareTo(load1) < 0);

        load2.setConnectionLimit(1);
        assertTrue(load1.compareTo(load2) < 0);
        assertTrue(load2.compareTo(load1) > 0);

        load1.setConnectionLimit(3);
        assertTrue(load1.compareTo(load2) < 0);
        assertTrue(load2.compareTo(load1) > 0);

        load1.setConnectionPendingCount(1);
        assertTrue(load1.compareTo(load2) == 0);
        assertTrue(load2.compareTo(load1) == 0);

        load1.setDaemonCount(4);
        load2.setDaemonCount(2);

        load1.setDaemonActiveCount(3);
        assertTrue(load1.compareTo(load2) > 0);
        assertTrue(load2.compareTo(load1) < 0);

        load2.setDaemonActiveCount(1);
        assertTrue(load1.compareTo(load2) > 0);
        assertTrue(load2.compareTo(load1) < 0);

        load2.setDaemonActiveCount(2);
        assertTrue(load1.compareTo(load2) < 0);
        assertTrue(load2.compareTo(load1) > 0);

        load1.setDaemonActiveCount(4);
        assertTrue(load1.compareTo(load2) == 0);
        assertTrue(load2.compareTo(load1) == 0);

        load1.setMessageBacklogIncoming(1);
        assertTrue(load1.compareTo(load2) > 0);
        assertTrue(load2.compareTo(load1) < 0);

        load2.setMessageBacklogOutgoing(2);
        assertTrue(load1.compareTo(load2) < 0);
        assertTrue(load2.compareTo(load1) > 0);

        load1.setMessageBacklogIncoming(2);
        assertTrue(load1.compareTo(load2) == 0);
        assertTrue(load2.compareTo(load1) == 0);

        load1.setMessageBacklogOutgoing(1);
        assertTrue(load1.compareTo(load2) > 0);
        assertTrue(load2.compareTo(load1) < 0);

        load1.setMessageBacklogIncoming(1);
        load1.setMessageBacklogOutgoing(2);
        load2.setMessageBacklogIncoming(2);
        load2.setMessageBacklogOutgoing(1);

        assertTrue(load1.compareTo(load2) == 0);
        assertTrue(load2.compareTo(load1) == 0);
        }

    @Test
    public void testPassivation()
        {
        ProxyService.ServiceLoad load1 = new ProxyService.ServiceLoad();
        ProxyService.ServiceLoad load2 = new ProxyService.ServiceLoad();

        load1.setConnectionLimit(1);
        load1.setConnectionCount(2);
        load1.setConnectionPendingCount(3);
        load1.setDaemonCount(4);
        load1.setDaemonActiveCount(5);
        load1.setMessageBacklogIncoming(6);
        load1.setMessageBacklogOutgoing(7);

        Map map = load1.toMap();
        assertTrue(map.size() == 7);

        assertFalse(load1.equals(load2));
        load2.fromMap(map);
        assertTrue(load1.equals(load2));
        }
    }
