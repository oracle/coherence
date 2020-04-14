/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


/**
* A ProxyService is a clustered service that accepts connections from
* external clients (e.g. Coherence*Extend) and proxies requests to
* clustered services.
*
* @author rhl 2009.10.07
*
* @since Coherence 3.6
*/
public interface ProxyService
        extends Service
    {
    // ----- inner interface: ProxyAction -----------------------------------

    /**
    * ProxyAction represents a type of action taken by a ProxyService.
    */
    public interface ProxyAction
            extends Action
        {
        /**
        * Singleton action for accepting a client connection.
        */
        public static final Action CONNECT = new ProxyAction(){};
        }


    // ----- constants ------------------------------------------------------

    /**
    * Proxy service type constant.
    *
    * @see Cluster#ensureService(String, String)
    */
    public static final String TYPE_DEFAULT = "Proxy";
    }
