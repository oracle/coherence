/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.proxy;


import com.tangosol.net.Member;
import com.tangosol.net.ProxyService;

import java.util.List;

import com.tangosol.net.ServiceLoadBalancer;

/**
* A ProxyServiceLoadBalancer is a pluggable strategy for controlling the
* client load across individual members of a clustered ProxyService.
*
* @author jh  2010.12.07
*
* @since Coherence 3.7
*/
public interface ProxyServiceLoadBalancer
        extends ServiceLoadBalancer<ProxyService, ProxyServiceLoad>
    {
    }
