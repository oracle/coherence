/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.coherence.config.builder.NamedEventInterceptorBuilder;
import com.tangosol.coherence.config.builder.ServiceBuilder;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.Service;
import com.tangosol.net.ServiceDependencies;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.ClassHelper;

import java.lang.ref.WeakReference;

import java.util.Collections;
import java.util.List;

/**
 * The {@link AbstractServiceScheme} provides functionality common to all
 * schemes that use services. Some properties, such as listeners, are optional
 * and may not apply to every scheme.
 *
 * @author pfm  2011.12.28
 * @since Coherence 12.1.2
 */
public abstract class AbstractServiceScheme<D extends ServiceDependencies>
        extends AbstractScheme
        implements ServiceBuilder, ServiceScheme
    {
    // ----- ServiceBuilder interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Service realizeService(ParameterResolver resolver, ClassLoader loader, Cluster cluster)
        {
        validate();

        String  sService = getScopedServiceName();
        Service service  = cluster.ensureService(sService, getServiceType());

        // configure the service if it isn't running
        if (!service.isRunning())
            {
            Service servicePrev = m_refService == null ? null : m_refService.get();
            if (servicePrev != null && servicePrev.isRunning())
                {
                // if another service instance is using the same dependencies, it
                // could lead to unpredictable side effects. For example, two
                // different partitioned services sharing an assignment strategy
                // instance would most likely behave quite erratically or crash.
                String sServicePrev = servicePrev.getInfo().getServiceName();

                CacheFactory.log("This scheme is used to create an instance "
                    + "of service \"" + sService + "\", while another service \""
                    + sServicePrev + "\" created by the same scheme is already running. "
                    + "This could lead to extremely dangerous side effects.",
                    CacheFactory.LOG_ERR);
                }
            else
                {
                m_refService = new WeakReference(service);
                }

            service.setDependencies(getServiceDependencies());
            }

        return service;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract boolean isRunningClusterNeeded();

    /**
     * {@inheritDoc}
     */
    @Override
    public String getScopeName()
        {
        return m_sScopeName;
        }

    /**
     * Set the scope name.
     *
     * @param sName  the scope name
     */
    @Injectable
    public void setScopeName(String sName)
        {
        m_sScopeName = sName;
        }

    /**
     * Deprecated: Set the XML so that we can create a Service using the SafeCluster.ensureService.
     *
     * @param element  the distributed-scheme XML
     */
    @Deprecated
    public void setXml(XmlElement element)
        {
        m_element = element;
        }

    /**
     * Return the XmlElement that contains the Service configuration.
     *
     * @return  the XmlElement
     */
    @Deprecated
    public XmlElement getXml()
        {
        return m_element;
        }

    // ----- ServiceScheme interface ----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAutoStart()
        {
        return m_fAutoStart;
        }

    /**
     * Set the auto-start enabled flag.
     *
     * @param fEnabled  the auto-start enabled flag
     */
    @Injectable("autostart")
    public void setAutoStart(boolean fEnabled)
        {
        m_fAutoStart = fEnabled;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceBuilder getServiceBuilder()
        {
        return this;
        }

    /**
     * Set the service name.
     *
     * @param sName  the service name.
     */
    @Injectable
    public void setServiceName(String sName)
        {
        m_sServiceName = sName;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServiceName()
        {
        String sName = m_sServiceName;

        if (sName == null || sName.trim().isEmpty())
            {
            // the service name defaults to the service type string
            m_sServiceName = sName = getDefaultServiceName();
            }

        return sName;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getScopedServiceName()
        {
        String sServiceName = getServiceName();
        String sScopeName   = getScopeName();

        sServiceName = sScopeName == null || sScopeName.trim().length() == 0 ?
            sServiceName : sScopeName + DELIM_APPLICATION_SCOPE + sServiceName;

        return sServiceName;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NamedEventInterceptorBuilder> getEventInterceptorBuilders()
        {
        return Collections.EMPTY_LIST;
        }

    // ----- AbstractServiceScheme methods ----------------------------------

    /**
     * Get the wrapped {@link Service} from the SafeService and invoke setScopeName()
     * on the wrapped {@link Service}.
     *
     * @param service  The safe service
     */
    protected void injectScopeNameIntoService(Service service)
        {
        String sScopeName = getScopeName();

        if (sScopeName != null && sScopeName.length() > 0)
            {
            try
                {
                Service innerService = (Service) ClassHelper.invoke(service, "getService", null);

                if (innerService == null)
                    {
                    // injectScopeNameIntoService() should only be called with a SafeService
                    // which has an inner RemoteService that has the setScopeName() method on it.
                    CacheFactory.log("Unable to pass scope name \"" + sScopeName + "\" to service \"" + service
                        + "\". The service wrapped by the safe service is null", CacheFactory.LOG_ERR);

                    return;
                    }

                ClassHelper.invoke(innerService, "setScopeName", new Object[] {sScopeName});
                }
            catch (ReflectiveOperationException e)
                {
                // injectScopeNameIntoService() should only be called with a SafeService
                // which has an inner RemoteService that has the setScopeName() method on it.
                CacheFactory.log("Unable to pass scope name \"" + sScopeName + "\" to service \""
                    + service + "\": " + e, CacheFactory.LOG_ERR);
                }
            }
        }

    /**
     * Obtains the {@link ServiceDependencies} that will be used to configure
     * {@link Service} produced by this scheme.
     *
     * @return the {@link ServiceDependencies}
     */
    @Injectable(".")
    public D getServiceDependencies()
        {
        return m_serviceDependencies;
        }

    /**
     * Set the {@link ServiceDependencies} to be used by this scheme
     * when configuring a newly realized {@link Service}.
     *
     * @param dependencies the {@link ServiceDependencies} object
     */
    public void setServiceDependencies(D dependencies)
        {
        m_serviceDependencies = dependencies;
        }

    /**
     * DefaultServiceName to use if none configured.
     *
     * @return default service name
     */
    protected String getDefaultServiceName()
        {
        return getServiceType();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The auto-start flag.
     */
    private boolean m_fAutoStart;

    /**
     * The service builder XML element.
     */
    private XmlElement m_element;

    /**
     * The scope name used by the ServiceBuilder (injected from the registry).
     */
    private String m_sScopeName;

    /**
     * A reference to the Service instance created by this scheme.
     */
    private WeakReference<Service> m_refService;

    /**
     * The service name.
     */
    private String m_sServiceName;

    /**
     * The {@link ServiceDependencies} to be used to configure the services
     * produced by this scheme.
     */
    protected D m_serviceDependencies;
    }
