/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package security;


import java.security.PrivilegedAction;

import com.tangosol.internal.net.security.AccessAdapter;

import com.tangosol.net.Service;

import com.tangosol.net.security.IdentityAsserter;

import com.tangosol.util.Base;

import javax.security.auth.Subject;


/**
* A test implementation of the IdentityAsserter interface that validates the
* service and asserts that the token is a Subject.
*
* @author dag 2011.01.18
*/
public class TestIdentityAsserter
        extends Base
        implements IdentityAsserter, AccessAdapter
    {
    // ----- IdentityAsserter implementation --------------------------------

    /**
    * {@inheritDoc}
    */
    public Subject assertIdentity(Object oToken, Service service)
            throws SecurityException
        {
        if ((service instanceof
                 com.tangosol.coherence.component.util.NameService))
            {
            throw new IllegalArgumentException("NameService should not call this Asserter");
            }

        if (!(service instanceof
                 com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService))
            {
            throw new IllegalArgumentException("invalid service type");
            }

        String sServiceName = service.getInfo().getServiceName();

        // if the service name in the test server config changes, this code
        // must be changed
        if (!sServiceName.equals("TcpProxyService"))
            {
            throw new SecurityException("incorrect service name");
            }

        if (oToken == null || oToken instanceof Subject)
            {
            return (Subject) oToken;
            }

        err("DefaultIdentityAsserter expected Subject but found: " + oToken);
        throw new SecurityException("identity token is unsupported type");
        }

    // ----- AccessAdapter implementation -----------------------------------

    @Override
    public void doAs(Subject subject, PrivilegedAction action)
        {
        Subject.doAs(subject, action);
        }
    }
