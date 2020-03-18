/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.discovery;

import com.tangosol.net.management.annotation.Description;

/**
 * A {@link DiscoveryMBean} is an MBean interface which defines attributes required by Oracle Enterprise Manager
 * and FMW Control management tools to discover running instances of Coherence.
 *
 * @author dag 2012.09.20
 *
 * @since Coherence 12.1.2
 */
@Description("Allows Coherence to be discovered by Oracle Enterprise Manager and FMW Control management tools.")
public interface DiscoveryMBean
    {
    /**
     * Return the name of the Add On Plugin.
     *
     * @return the name of the Add On Plugin.
     */
    @Description("The name of the Add On Plugin.")
    public String getAddOnName();

    /**
     * Return the name displayed by the management tool.
     *
     * @return the name displayed by the management tool
     */
    @Description("The name displayed by the management tool.")
    public String getAddOnDisplayName();

    /**
     * Return the full name of the Plugin.
     *
     * @return the full name of the Plugin
     */
    @Description("The full name of the Plugin.")
    public String getEMDiscoveryPluginName();
    }
