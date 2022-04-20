/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import com.tangosol.io.Serializer;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.ServiceListener;


/**
* Service implementation that delegates to a wrapped Service instance.
*
* @author jh  2010.03.17
*/
public class WrapperService
        implements Service
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new WrapperService that delegates to the given Service
    * instance.
    *
    * @param service the Service to wrap
    */
    public WrapperService(Service service)
        {
        if (service == null)
            {
            throw new IllegalArgumentException("Service must be specified");
            }
        m_service = service;
        }


    // ----- Service interface ----------------------------------------------

    /**
    * {@inheritDoc}
    */
    public ServiceDependencies getDependencies()
        {
        return getService().getDependencies();
        }

    /**
    * {@inheritDoc}
    */
    public Cluster getCluster()
        {
        return getService().getCluster();
        }

    /**
    * {@inheritDoc}
    */
    public ServiceInfo getInfo()
        {
        return getService().getInfo();
        }

    /**
    * {@inheritDoc}
    */
    public void addMemberListener(MemberListener listener)
        {
        getService().addMemberListener(listener);
        }

    /**
    * {@inheritDoc}
    */
    public void removeMemberListener(MemberListener listener)
        {
        getService().removeMemberListener(listener);
        }

    /**
    * {@inheritDoc}
    */
    public Object getUserContext()
        {
        return getService().getUserContext();
        }

    /**
    * {@inheritDoc}
    */
    public void setUserContext(Object oCtx)
        {
        getService().setUserContext(oCtx);
        }

    /**
    * {@inheritDoc}
    */
    public Serializer getSerializer()
        {
        return getService().getSerializer();
        }

    /**
    * {@inheritDoc}
    */
    public void addServiceListener(ServiceListener listener)
        {
        getService().addServiceListener(listener);
        }

    /**
    * {@inheritDoc}
    */
    public void removeServiceListener(ServiceListener listener)
        {
        getService().removeServiceListener(listener);
        }

    /**
    * {@inheritDoc}
    */
    public void configure(XmlElement xml)
        {
        getService().configure(xml);
        }

    /**
    * {@inheritDoc}
    */
    public void start()
        {
        getService().start();
        }

    /**
    * {@inheritDoc}
    */
    public boolean isRunning()
        {
        return getService().isRunning();
        }

    /**
    * {@inheritDoc}
    */
    public void shutdown()
        {
        getService().shutdown();
        }

    /**
    * {@inheritDoc}
    */
    public void stop()
        {
        getService().stop();
        }

    /**
    * {@inheritDoc}
    */
    public ClassLoader getContextClassLoader()
        {
        return getService().getContextClassLoader();
        }

    /**
    * {@inheritDoc}
    */
    public void setContextClassLoader(ClassLoader loader)
        {
        getService().setContextClassLoader(loader);
        }

    /**
    * {@inheritDoc}
    */
    public void setDependencies(ServiceDependencies deps)
        {
        getService().setDependencies(deps);
        }

    /**
    * {@inheritDoc}
    */
    public ResourceRegistry getResourceRegistry()
        {
        return getService().getResourceRegistry();
        }

    @Override
    public boolean isSuspended()
        {
        return getService().isSuspended();
        }


    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public String toString()
        {
        return "WrapperService{" + getService() + '}';
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return the wrapped Service.
    *
    * @return  the wrapped Service
    */
    public Service getService()
        {
        return m_service;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The (wrapped) Service.
    */
    protected Service m_service;
    }
