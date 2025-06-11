/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.management;

import org.junit.Test;

import static com.tangosol.net.management.MBeanConnector.RMI_CONNECTION_PORT_ADJUST_PROPERTY;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

public class MBeanConnectorTest
    {
    @Test
    public void getConnectionPortMax() 
        {
        assertThat(MBeanConnector.getConnectionPortMax(), is(lessThanOrEqualTo(65535)));
        assertThat(MBeanConnector.getConnectionPortMax(), is(greaterThan(0)));

        System.setProperty(RMI_CONNECTION_PORT_ADJUST_PROPERTY, "44444");
        assertThat(MBeanConnector.getConnectionPortMax(), is(44444));

        System.setProperty(RMI_CONNECTION_PORT_ADJUST_PROPERTY, "70000");
        assertThat(MBeanConnector.getConnectionPortMax(), is(65535));

        System.setProperty(RMI_CONNECTION_PORT_ADJUST_PROPERTY, "abc");
        Exception expected = null;
        try
            {
            MBeanConnector.getConnectionPortMax();
            }
        catch (RuntimeException e)
            {
            expected = e;
            }
        if (expected == null) 
            {
            throw new RuntimeException("MBeanConnector.getConnectionPortMax() didn't throw an exception for an illegal input.");
            }
        }
    }