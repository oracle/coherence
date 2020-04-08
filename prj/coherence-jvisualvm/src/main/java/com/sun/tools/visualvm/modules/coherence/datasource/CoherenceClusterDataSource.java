/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.datasource;

import com.sun.tools.visualvm.core.datasource.DataSource;

import com.sun.tools.visualvm.core.datasource.Storage;
import java.net.MalformedURLException;

import java.net.URL;

/**
 * The {@link DataSource} for a single Coherence cluster.
 *
 * @author sr 12.10.2017
 *
 * @since Coherence 12.2.1.4.0
 */
public class CoherenceClusterDataSource
        extends DataSource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a Coherence cluster data source with the provided URL as the management URL.
     *
     * @param  sManagementUrl  the management service REST URL
     * @param  sClusterName    the name of the Coherence cluster
     * @param  storage         the storage of the data source
     */
    public CoherenceClusterDataSource(String sManagementUrl, String sClusterName, Storage storage)
        {
        this.f_sManagementUrl = sManagementUrl;
        this.f_storage        = storage;
        this.f_sClusterName   = sClusterName;
        }

    // ----- DataSource methods ---------------------------------------------

    @Override
    public boolean supportsUserRemove()
        {
        return true;
        }

    @Override
    protected void remove()
        {
        f_storage.deleteCustomPropertiesStorage();
        }

    // ----- accessors ------------------------------------------------------

    /**
     * The management URL.
     *
     * @return the REST management URL
     */
    public String getUrl()
        {
        return f_sManagementUrl;
        }

    /**
     * Return the name to be used for this particular Data source. The name will be shown
     * in the LHS tree.
     *
     * @return the name for the data source
     */
    public String getName()
        {
        try
            {
            URL url = new URL(f_sManagementUrl);
            return f_sClusterName + "[" + url.getHost() + ":" + url.getPort() + "]";
            }
        catch (MalformedURLException e)
            {
            // ignore the exceptions and return the URL as it is.
            }

        return f_sClusterName + "[" + f_sManagementUrl + "]";
        }

    // ----- data members ---------------------------------------------------

    /**
     * The management REST URL for the Coherence cluster.
     */
    private final String f_sManagementUrl;

    /**
     * The name for Coherence cluster.
     */
    private final String f_sClusterName;

    /**
     * The persistent storage of the data source.
     */
    private final Storage f_storage;
    }
