/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.acceptor;

import com.tangosol.coherence.config.builder.SocketProviderBuilder;

import com.tangosol.internal.net.LegacyXmlSocketProviderFactoryDependencies;

import com.tangosol.net.OperationalContext;
import com.tangosol.net.SocketProviderFactory;

import com.tangosol.net.grpc.GrpcAcceptorController;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

/**
 * LegacyXmlGrpcAcceptorHelper parses XML to populate a {@link DefaultGrpcAcceptorDependencies}
 * object.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
@Deprecated(since = "22.06.2")
@SuppressWarnings("DeprecatedIsStillUsed")
public class LegacyXmlGrpcAcceptorHelper
    {
    /**
     * Populate the DefaultGrpcAcceptorDependencies object from the given XML configuration.
     *
     * @param xml     the XML parent element that contains the <grpc-acceptor> XML fragment
     * @param deps    the DefaultGrpcAcceptorDependencies to be populated
     * @param ctx     the OperationalContext
     * @param loader  the class loader for the current context
     *
     * @return the {@link DefaultGrpcAcceptorDependencies} object that was passed in
     */
    public static DefaultGrpcAcceptorDependencies fromXml(XmlElement xml,
        DefaultGrpcAcceptorDependencies deps, OperationalContext ctx, ClassLoader loader)
        {
        LegacyXmlAcceptorHelper.fromXml(xml, deps, ctx, loader);

        // <grpc-acceptor>
        XmlElement xmlAcceptor = xml.getSafeElement("grpc-acceptor");

        // set the grpc controller
        // <class-name> <init-params>
        GrpcAcceptorController controller;
        if (XmlHelper.isInstanceConfigEmpty(xmlAcceptor))
            {
            controller = GrpcAcceptorController.discoverController();
            }
        else
            {
            controller = (GrpcAcceptorController) XmlHelper.createInstance(xmlAcceptor, loader, null, GrpcAcceptorController.class);
            }
        deps.setController(controller);

        // <socket-provider/>
        deps.setSocketProviderBuilder(createSocketProviderBuilder(ctx,
            xmlAcceptor.getElement("socket-provider")));

        // <in-process-namek>
        String sName = xmlAcceptor.getSafeElement("in-process-name").getString(deps.getInProcessName());
        deps.setInProcessName(sName);

        // <auth-method>
        deps.setAuthMethod(xmlAcceptor.getSafeElement("auth-method").getString(deps.getAuthMethod()));

        // <channelz>
        deps.setChannelz(xmlAcceptor.getSafeElement("channelz").getString(deps.getChannelz()));

        // <error-disclosure>
        deps.setErrorDisclosure(xmlAcceptor.getSafeElement("error-disclosure").getString(deps.getErrorDisclosure()));

        // <secure-transport>
        deps.setSecureTransport(xmlAcceptor.getSafeElement("secure-transport").getString(deps.getSecureTransport()));

        // <local-address>
        XmlElement xmlLocal = xmlAcceptor.getSafeElement("local-address");

        // <address>
        String sAddr = xmlLocal.getSafeElement("address").getString(deps.getLocalAddress());
        deps.setLocalAddress(sAddr);

        // <port>
        int nPort = xmlLocal.getSafeElement("port").getInt(deps.getLocalPort());
        deps.setLocalPort(nPort);

        return deps;
        }

    /**
     * Create a dependency-backed socket provider builder from legacy XML.
     *
     * @param ctx                the operational context
     * @param xmlSocketProvider  the socket-provider XML element
     *
     * @return a dependency-backed socket provider builder
     */
    private static SocketProviderBuilder createSocketProviderBuilder(OperationalContext ctx, XmlElement xmlSocketProvider)
        {
        SocketProviderFactory factory = ctx.getSocketProviderFactory();
        String                sId     = LegacyXmlSocketProviderFactoryDependencies.getProviderId(xmlSocketProvider);

        if (sId == null)
            {
            return factory.getDefaultSocketProviderBuilder();
            }

        if (SocketProviderFactory.UNNAMED_PROVIDER_ID.equals(sId))
            {
            LegacyXmlSocketProviderFactoryDependencies deps =
                    new LegacyXmlSocketProviderFactoryDependencies(sId, xmlSocketProvider);
            deps.setSocketProviderFactory(factory);
            return new SocketProviderBuilder(sId, deps, false);
            }

        return new SocketProviderBuilder(sId, factory.getDependencies(), false);
        }
    }
