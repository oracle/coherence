/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.management.jmx;


import com.tangosol.net.management.MBeanHelper;

import javax.management.MBeanServer;
import javax.management.MBeanServerBuilder;
import javax.management.MBeanServerDelegate;


/**
* The WrapperMBeanServerBuilder is responsible for integrating the
* {@link WrapperMBeanServer} into the JMX runtime.
*
* @since Coherence 3.4
* @author ew 2007.07.18
*/
public class WrapperMBeanServerBuilder
        extends MBeanServerBuilder
    {
    /**
    * {@inheritDoc}
    */
    public MBeanServerDelegate newMBeanServerDelegate()
        {
        return new WrapperMBeanServerDelegate();
        }

    /**
    * {@inheritDoc}
    */
    public MBeanServer newMBeanServer(String defaultDomain, MBeanServer outer,
            MBeanServerDelegate delegate)
        {
        if (defaultDomain == null || defaultDomain.equals(MBeanHelper.getDefaultDomain()))
            {
            WrapperMBeanServer serverWrapper = new WrapperMBeanServer();

            MBeanServer serverInner = super.newMBeanServer(defaultDomain, outer == null ?
                    serverWrapper : outer, delegate);

            serverWrapper.setWrappedServer(serverInner);
            return serverWrapper;
            }
        else
            {
            return super.newMBeanServer(defaultDomain, outer, delegate);
            }
        }
    }
