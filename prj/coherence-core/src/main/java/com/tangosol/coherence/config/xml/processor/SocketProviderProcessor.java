/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.SSLSocketProviderDependenciesBuilder;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.internal.net.cluster.DefaultClusterDependencies;
import com.tangosol.internal.net.ssl.SSLSocketProviderDefaultDependencies;
import com.tangosol.net.OperationalContext;
import com.tangosol.net.SocketProviderFactory;
import com.tangosol.net.TcpDatagramSocketProvider;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.run.xml.XmlValue;

import java.util.Iterator;

/**
 * An {@link ElementProcessor} that will parse an &lt;socket-provider&gt; and
 * produce a {@link SocketProviderBuilder}.
 *
 * @author bo  2013.07.02
 * @since Coherence 12.1.3
 */
@XmlSimpleName("socket-provider")
public class SocketProviderProcessor
        implements ElementProcessor<SocketProviderBuilder>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public SocketProviderBuilder process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        String  sId                                    = getProviderDefinitionId(xmlElement);
        boolean fInlinedProvider                       = SocketProviderFactory.UNNAMED_PROVIDER_ID.equals(sId);
        SocketProviderFactory factory                  = getSocketProviderFactory(context, xmlElement);
        SocketProviderFactory.DefaultDependencies deps =
                (SocketProviderFactory.DefaultDependencies) factory.getDependencies();

        if (XmlHelper.isEmpty(xmlElement))
            {
            return new SocketProviderBuilder(null, factory.getDependencies());
            }

        if (fInlinedProvider)
            {
            // check for reference to a socket-provider definition from <socket-providers>.
            // The <socket-provider> element is _not_ empty; ie: it contains
            // a string value (e.g. <socket-provider>system</socket-provider>).
            // it must be a named/registered SocketProvider, so let's look it up
            String sName = getProviderIdReference(xmlElement);

            if (sName != null && sName.length() > 0)
                {
                return new SocketProviderBuilder(sName, factory.getDependencies());
                }

            // inlined, anonymous socket-provider, create an anonymous dependencies
            deps = new SocketProviderFactory.DefaultDependencies();
            deps.setSocketProviderFactory(factory);
            }

        for (Iterator itr = xmlElement.getElementList().iterator(); itr.hasNext(); )
            {
            XmlElement xmlProvider = (XmlElement) itr.next();
            String     sType       = xmlProvider.getName();

            if (sType.equals(SocketProviderFactory.Dependencies.ProviderType.SYSTEM.getName()))
                {
                deps.addNamedProviderType(sId, SocketProviderFactory.Dependencies.ProviderType.SYSTEM);
                }
            else if (sType.equals(SocketProviderFactory.Dependencies.ProviderType.TCP.getName()))
                {
                deps.addNamedProviderType(sId, SocketProviderFactory.Dependencies.ProviderType.TCP);

                XmlElement                             xmlCat       = xmlElement.getSafeElement("datagram-socket");
                TcpDatagramSocketProvider.Dependencies depsDatagram = new TcpDatagramSocketProvider.DefaultDependencies();

                context.inject(depsDatagram, xmlCat);
                deps.addNamedTCPDatagramDependencies(sId, depsDatagram);
                }
            else if (sType.equals(SocketProviderFactory.Dependencies.ProviderType.SSL.getName()))
                {
                deps.addNamedProviderType(sId, SocketProviderFactory.Dependencies.ProviderType.SSL);
                SSLSocketProviderDefaultDependencies depsSSL = new SSLSocketProviderDefaultDependencies(deps);

                context.addCookie(SSLSocketProviderDefaultDependencies.class, depsSSL);
                SSLSocketProviderDependenciesBuilder builder = new SSLSocketProviderDependenciesBuilder(depsSSL);
                context.inject(builder, xmlProvider);

                deps.addNamedSSLDependenciesBuilder(sId, builder);

                // check if datagram-socket element is present
                XmlElement xmlCat = xmlProvider.getElement("datagram-socket");

                if (xmlCat != null)
                    {
                    TcpDatagramSocketProvider.Dependencies dependencies =
                        new TcpDatagramSocketProvider.DefaultDependencies();

                    context.inject(dependencies, xmlCat);
                    deps.addNamedTCPDatagramDependencies(sId, dependencies);
                    }
                }
            else if (sType.equals(SocketProviderFactory.Dependencies.ProviderType.SDP.getName()))
                {
                deps.addNamedProviderType(sId, SocketProviderFactory.Dependencies.ProviderType.SDP);
                }
            else
                {
                throw new IllegalArgumentException("Unsupported socket provider: " + sType);
                }
            }

        return new SocketProviderBuilder(sId, deps);
        }

    // ----- helpers ---------------------------------------------------------

    /**
     * Return the cluster's {@link SocketProviderFactory}.
     * @param ctx  Cluster operational context
     * @param xml  socket-provider xml fragment being processed.
     * @return the cluster's {@link SocketProviderFactory}
     */
    private static SocketProviderFactory getSocketProviderFactory(ProcessingContext ctx, XmlElement xml)
        {
        // grab the operational context from which we can lookup the socket provider factory
        OperationalContext ctxOperational = ctx.getCookie(OperationalContext.class);

        if (ctxOperational == null)
            {
            DefaultClusterDependencies deps = ctx.getCookie(DefaultClusterDependencies.class);
            if (deps == null)
                {
                throw new ConfigurationException("Attempted to resolve the OperationalContext in [" + xml
                        + "] but it was not defined", "The registered ElementHandler for the <"
                        + xml.getName()
                        + "> element is not operating in an OperationalContext");
                }
            return deps.getSocketProviderFactory();
            }
        else
            {
            return ctxOperational.getSocketProviderFactory();
            }
        }

    /**
     * Get the reference to a ProviderId stored as text on the element.
     *
     * @param xmlSocketProvider a socket-provider element
     *
     * @return the provider id reference found in socket-provider element or null if no reference found.
     */
    private static String getProviderIdReference(XmlElement xmlSocketProvider)
        {
        String sRef;

        if (xmlSocketProvider == null)
            {
            return null;
            }
        else if (xmlSocketProvider.getElementList().isEmpty())
            {
            // socket-provider reference
            sRef = xmlSocketProvider.getString();

            if (sRef.length() == 0)
                {
                // empty configuration
                return null;
                }
            }
        else
            {
            return null;
            }

        return sRef;
        }

    /**
     * Get the identifier of a SocketProvider definition.
     *
     * @param xmlSocketProvider   a socket provider xml element
     *
     * @return the socket provider definitions id or {SocketProviderFactory#UNNAMED_PROVIDER_ID} if an unnamed, inlined socket-provider.
     */
    private static String getProviderDefinitionId(XmlElement xmlSocketProvider)
        {
        XmlValue valueIdAttribute = xmlSocketProvider != null ? xmlSocketProvider.getAttribute("id") : null;
        return valueIdAttribute == null ? SocketProviderFactory.UNNAMED_PROVIDER_ID : valueIdAttribute.getString(SocketProviderFactory.UNNAMED_PROVIDER_ID);
        }
    }
