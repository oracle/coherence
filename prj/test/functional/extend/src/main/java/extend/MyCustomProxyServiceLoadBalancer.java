/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package extend;

import com.tangosol.net.proxy.DefaultProxyServiceLoadBalancer;

import java.util.Comparator;

/**
* A custom ProxyServiceLoadBalancer that extends the
* DefaultProxyServiceLoadBalancer.
* <p>
* This implementation extends DefaultProxyServiceLoadBalancer, providing
* a custom Comparator.
*
* @author par  2013.08.13
* since @BUILDVERSION@
*/
public class MyCustomProxyServiceLoadBalancer
        extends DefaultProxyServiceLoadBalancer
    {
    // ----- constructors ----------------------------------------------------

    /**
    * Create a new MyCustomProxyServiceLoadBalancer that will order
    * ProxyServiceLoad objects using the specified Comparator. If null, the
    * natural ordering of the ProxyServiceLoad objects will be used.
    *
    * @param comparator  the Comparator used to order ProxyServiceLoad
    *                    objects
    */
    public MyCustomProxyServiceLoadBalancer(String comparatorName)
       throws ClassNotFoundException, InstantiationException,
           IllegalAccessException
        {
        super((Comparator) Class.forName(comparatorName).newInstance());
       }
    }
