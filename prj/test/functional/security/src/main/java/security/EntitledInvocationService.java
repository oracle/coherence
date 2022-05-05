/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package security;


import com.tangosol.net.Invocable;
import com.tangosol.net.InvocationObserver;
import com.tangosol.net.InvocationService;
import com.tangosol.net.WrapperInvocationService;

import com.tangosol.net.security.SecurityHelper;

import java.security.Principal;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;


/**
* Example WrapperInvocationService that demonstrates how entitlements can be
* applied to a wrapped InvocationService using the Subject passed from the
* client via Coherence*Extend. This implementation only allows clients with
* a specified Principal name to access the wrapped InvocationService.
*
* @author jh  2010.03.18
*/
public class EntitledInvocationService
        extends WrapperInvocationService
    {
    /**
    * Create a new EntitledInvocationService.
    *
    * @param service     the wrapped InvocationService
    * @param sPrincipal  the name of the Principal that is allowed to access
    *                    the wrapped InvocationService
    */
    public EntitledInvocationService(InvocationService service, String sPrincipal)
        {
        super(service);

        if (sPrincipal == null || sPrincipal.length() == 0)
            {
            throw new IllegalArgumentException("Principal required");
            }
        m_sPrincipal = sPrincipal;
        }


    // ----- InvocationService interface ------------------------------------

    /**
    * {@inheritDoc}
    */
    public void execute(Invocable task, Set setMembers, InvocationObserver observer)
        {
        checkAccess();
        super.execute(task, setMembers, observer);
        }

    /**
    * {@inheritDoc}
    */
    public Map query(Invocable task, Set setMembers)
        {
        checkAccess();
        return super.query(task, setMembers);
        }


    // ----- helper methods -------------------------------------------------

    /**
    * Assert that a Subject is associated with the calling thread with a
    * Principal with name equal to {@link #getPrincipalName()}.
    *
    * @throws SecurityException if a Subject is not associated with the
    *         calling thread or does not have the specified Principal
    */
    protected void checkAccess()
        {
        Subject subject = SecurityHelper.getCurrentSubject();
        if (subject == null)
            {
            throw new SecurityException("Access denied, authentication required");
            }

        for (Iterator iter = subject.getPrincipals().iterator(); iter.hasNext();)
            {
            Principal principal = (Principal) iter.next();
            if (m_sPrincipal.equals(principal.getName()))
                {
                return;
                }
            }

        throw new SecurityException("Access denied, insufficient privileges");
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return the name of the Principal that is allowed to access the wrapped
    * service.
    *
    * @return the name of the Principal
    */
    public String getPrincipalName()
        {
        return m_sPrincipal;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of the principal that is allowed to access the wrapped
    * service.
    */
    private String m_sPrincipal;
    }
