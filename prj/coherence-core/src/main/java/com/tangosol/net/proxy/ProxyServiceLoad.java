/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.proxy;

import com.tangosol.net.ServiceLoad;


/**
* A ProxyServiceLoad encapsulates information about the current utilization
* of a ProxyService. It can be used to implement load balancing algorithms
* that control the distribution of clients across individual instances of a
* clustered ProxyService.
*
* @author jh  2010.12.07
*
* @since Coherence 3.7
*
* @see ProxyServiceLoadBalancer
*/
public interface ProxyServiceLoad
        extends ServiceLoad
    {
    }
