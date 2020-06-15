/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import java.util.Map;
import java.util.Set;


/**
* InvocationService implementation that delegates to a wrapped
* InvocationService instance.
*
* @author jh  2010.03.17
*/
public class WrapperInvocationService
        extends WrapperService
        implements InvocationService
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new WrapperInvocationService that delegates to the given
    * InvocationService instance.
    *
    * @param service the InvocationService to wrap
    */
    public WrapperInvocationService(InvocationService service)
        {
        super(service);
        }


    // ----- InvocationService interface ------------------------------------

    /**
    * {@inheritDoc}
    */
    public void execute(Invocable task, Set setMembers, InvocationObserver observer)
        {
        getInvocationService().execute(task, setMembers, observer);
        }

    /**
    * {@inheritDoc}
    */
    public Map query(Invocable task, Set setMembers)
        {
        return getInvocationService().query(task, setMembers);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public String toString()
        {
        return "WrapperInvocationService{" + getInvocationService() + '}';
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return the wrapped InvocationService.
    *
    * @return  the wrapped InvocationService
    */
    public InvocationService getInvocationService()
        {
        return (InvocationService) getService();
        }
    }
