/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.acceptor;

import com.tangosol.coherence.config.builder.SocketProviderBuilder;

import com.tangosol.coherence.http.HttpServer;
import com.tangosol.coherence.http.GenericHttpServer;

import com.tangosol.net.OperationalContext;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * LegacyXmlHttpAcceptorHelper parses XML to populate a DefaultHttpAcceptorDependencies
 * object.
 *
 * @author pfm 2011.06.27
 * @since Coherence 12.1.2
 */
@Deprecated
public class LegacyXmlHttpAcceptorHelper
    {
    /**
     * Populate the DefaultHttpAcceptorDependencies object from the given XML configuration.
     *
     * @param xml     the XML parent element that contains the <http-acceptor> XML fragment
     * @param deps    the DefaultHttpAcceptorDependencies to be populated
     * @param ctx     the OperationalContext
     * @param loader  the class loader for the current context
     *
     * @return the DefaultHttpAcceptorDependencies object that was passed in
     */
    @SuppressWarnings({"rawtypes"})
    public static DefaultHttpAcceptorDependencies fromXml(XmlElement xml,
            DefaultHttpAcceptorDependencies deps, OperationalContext ctx, ClassLoader loader)
        {
        LegacyXmlAcceptorHelper.fromXml(xml, deps, ctx, loader);

        // <http-acceptor>
        XmlElement xmlAcceptor = xml.getSafeElement("http-acceptor");

        // <class-name> <init-params>
        GenericHttpServer<?> httpServer;
        if (XmlHelper.isInstanceConfigEmpty(xmlAcceptor))
            {
            httpServer = HttpServer.create();
            }
        else
            {
            httpServer = (GenericHttpServer) XmlHelper.createInstance(xmlAcceptor, loader, null, GenericHttpServer.class);
            }
        deps.setHttpServer(httpServer);

        // <socket-provider/>
        deps.setSocketProviderBuilder(new SocketProviderBuilder(ctx.getSocketProviderFactory().getSocketProvider(
            xmlAcceptor.getSafeElement("socket-provider"))));

        // <local-address>
        XmlElement xmlLocal = xmlAcceptor.getSafeElement("local-address");

        // <address>
        String sAddr = xmlLocal.getSafeElement("address").getString(deps.getLocalAddress());
        deps.setLocalAddress(sAddr);

        // <port>
        int nPort = xmlLocal.getSafeElement("port").getInt(deps.getLocalPort());
        deps.setLocalPort(nPort);

        // <resource-config>
        Class<?>            clsResourceConfig = httpServer.getResourceType();
        Map<String, Object> mapConfig         = new HashMap<String, Object>();
        for (Iterator iter = xmlAcceptor.getElements("resource-config"); iter.hasNext(); )
            {
            XmlElement xmlConfig = (XmlElement) iter.next();
            if (XmlHelper.isInstanceConfigEmpty(xmlConfig))
                {
                XmlElement xmlInstance = xmlConfig.ensureElement("instance");
                XmlElement xmlClass    = xmlInstance.ensureElement("class-name");
                xmlClass.setString("com.tangosol.coherence.rest.server.DefaultResourceConfig");
                }

            String sContext = xmlConfig.getSafeElement("context-path").getString(null);
            Object oResourceConfig = XmlHelper.createInstance(xmlConfig, loader, null, clsResourceConfig);
            if (sContext == null)
                {
                ApplicationPath path = oResourceConfig.getClass().getAnnotation(ApplicationPath.class);
                sContext = path == null ? "/" : path.value();
                }

            mapConfig.put(sContext, oResourceConfig);
            }

        if (mapConfig.isEmpty())
            {
            ServiceLoader<Application> loaderApps = ServiceLoader.load(Application.class);

            for (Application application : loaderApps)
                {
                ApplicationPath annotation = application.getClass().getAnnotation(ApplicationPath.class);
                String sPath = annotation == null ? "/" : annotation.value();

                if (sPath.charAt(0) != '/')
                    {
                    sPath = '/' + sPath;
                    }

                mapConfig.put(sPath, application);
                }
            }

        deps.setResourceConfig(mapConfig);

        // <auth-method>
        deps.setAuthMethod(xmlAcceptor.getSafeElement("auth-method").getString("none"));

        return deps;
        }
    }
