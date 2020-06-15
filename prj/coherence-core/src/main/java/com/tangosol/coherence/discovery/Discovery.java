/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.discovery;

import com.tangosol.net.management.AnnotatedStandardMBean;

import com.tangosol.util.Base;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

/**
 * Implementation of {@link DiscoveryMBean} which allows Coherence to be discovered by Oracle Enterprise Manager
 * and FMW Control management tools.
 *
 * @author dag 2012.09.20
 *
 * @since Coherence 12.1.2
 */
public class Discovery
        implements DiscoveryMBean
    {
    // ----- DiscoveryMBean methods -------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAddOnName()
        {
        return ADD_ON_NAME;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAddOnDisplayName()
        {
        return ADD_ON_DISPLAY_NAME;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEMDiscoveryPluginName()
        {
        return getAddOnName() + "." + DISCOVERY_PLUGIN_NAME;
        }

    // ----- accessors and helpers ------------------------------------------

    /**
     * Create the DiscoveryMBean
     */
    public static StandardMBean createMBean()
        {
        StandardMBean mbean;
        Discovery     discovery = new Discovery();

        try
            {
            mbean = new AnnotatedStandardMBean(discovery, DiscoveryMBean.class);
            }
        catch (NotCompliantMBeanException e)
            {
            throw Base.ensureRuntimeException(e);
            }

        return mbean;
        }

    // ---- constants -------------------------------------------------------

    // The values of the following constants were supplied by EM and are required for EM Integration.
    // This MBean is considered a "Plugin" by EM.

    /**
     * String representing the root of the "name" part of <tt>ObjectName</tt>
     * for the DiscoveryMBean.
     */
    private static final String ADD_ON_NAME = "oracle.sysman.emas";

    /**
     * The name displayed by the management tool.
     */
    private static final String ADD_ON_DISPLAY_NAME = "Oracle Fusion Middleware";

    /**
     * String representing the Coherence specific part of the "name" value of <tt>ObjectName</tt>
     * for the DiscoveryMBean.
     */
    private static final String DISCOVERY_PLUGIN_NAME = "CoherenceDiscovery";

    /**
     * String representing the "domain" part of <tt>ObjectName</tt> for the
     * DiscoveryMBean.
     */
    private static final String DISCOVERY_DOMAIN = "EMDomain";

    /**
     * String representing the "type" part of <tt>ObjectName</tt> for the
     * DiscoveryMBean.
     */
    public final static String DISCOVERY_TYPE = "type=EMDiscoveryIntegration";
    }
