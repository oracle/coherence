/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.management.jmx;


import javax.management.MBeanServerDelegate;


/**
* The WrapperMBeanServerDelegate represents the WrapperMBeanServer
* from the management point of view.
*
* @since Coherence 3.4
* @author ew 2007.07.18
*/
public class WrapperMBeanServerDelegate
        extends MBeanServerDelegate
    {
    /**
    * {@inheritDoc}
    */
    public String getMBeanServerId()
        {
        return "Coherence WrapperMBeanServer";
        }

    /**
    * {@inheritDoc}
    */
    public String getImplementationVersion()
        {
        return "1.0";
        }

    /**
    * {@inheritDoc}
    */
    public String getImplementationVendor()
        {
        return "Oracle Corp.";
        }
    }
