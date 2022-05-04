/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package extend;


import com.tangosol.net.Member;

import com.tangosol.net.proxy.DefaultProxyServiceLoadBalancer;
import com.tangosol.net.proxy.ProxyServiceLoad;


import java.util.Comparator;


/**
* A custom Comparator to be used by the
* DefaultProxyServiceLoadBalancer.
* <p>
* This implementation calls the default load balancing
* algorithm. Used by the DefaultProxyServiceLoadBalancer.getMemberList().
*
* @author par  2013.08.13
* @since @BUILDVERSION@
*/
public class MyCustomProxyServiceLoadComparator
        implements java.util.Comparator
    {
    // ----- contructors ----------------------------------------------------

    /**
    * Default constructor.
    */
    public MyCustomProxyServiceLoadComparator()
        {
        }

    // ----- Comparator interface -----------------------------

    /**
     * Compare two ProxyServiceLoad instances.
     */
   public int compare(Object o1, Object o2)
        {
        ProxyServiceLoad load1 = (ProxyServiceLoad) o1;
        ProxyServiceLoad load2 = (ProxyServiceLoad) o2;

        // compare the same as the default
        return load1.compareTo(load2);
        }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj)
        {
        return (obj instanceof MyCustomProxyServiceLoadComparator);
        } 
    }
