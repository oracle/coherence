/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package security;


import com.tangosol.net.Service;

import com.tangosol.net.security.IdentityTransformer;

import javax.security.auth.Subject;


/**
* A test implementation of the IdentityTransformer interface that
* validates the service and then returns the Subject that is passed to it.
*
* @author dag 2011.01.18
*/
public class TestIdentityTransformer
        implements IdentityTransformer
    {
    // ----- IdentityTransformer implementation -----------------------------

    /**
    * {@inheritDoc}
    */
    public Object transformIdentity(Subject subject, Service service)
        {
        if (!(service instanceof
                com.tangosol.coherence.component.net.extend.RemoteService))
            {
            throw new IllegalArgumentException("invalid service type");
            }

        String sServiceName = service.getInfo().getServiceName();

        // if the service name in the test client config changes, this code
        // must be changed
        if (!(sServiceName.equals("ExtendTcpCacheService") ||
              sServiceName.equals("ExtendTcpInvocationService") ||
              sServiceName.equals("TcpProxyService") ||
              sServiceName.equals("TcpProxyService:RemoteNameService")))
            {
            throw new SecurityException("incorrect service name: " + sServiceName);
            }

        return subject;
        }
    }
