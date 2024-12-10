/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.cluster;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.net.InetAddresses;

import com.tangosol.coherence.config.Config;
import com.tangosol.coherence.config.ParameterMacroExpressionParser;
import com.tangosol.coherence.config.builder.AddressProviderBuilder;
import com.tangosol.coherence.config.builder.ListBasedAddressProviderBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry;
import com.tangosol.coherence.config.builder.ServiceFailurePolicyBuilder;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.coherence.config.xml.OperationalConfigNamespaceHandler;
import com.tangosol.coherence.config.xml.processor.AddressProviderBuilderProcessor;
import com.tangosol.coherence.config.xml.processor.SocketProviderProcessor;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.ChainedParameterResolver;
import com.tangosol.config.expression.ScopedParameterResolver;
import com.tangosol.config.expression.SystemEnvironmentParameterResolver;
import com.tangosol.config.expression.SystemPropertyParameterResolver;

import com.tangosol.config.xml.DefaultProcessingContext;
import com.tangosol.config.xml.DocumentProcessor;
import com.tangosol.config.xml.NamespaceHandler;

import com.tangosol.internal.net.InetAddressRangeFilter;
import com.tangosol.internal.net.LegacyXmlConfigHelper;

import com.tangosol.io.WrapperStreamFactory;

import com.tangosol.net.AddressProvider;
import com.tangosol.net.AddressProviderFactory;
import com.tangosol.net.CompositeAddressProvider;
import com.tangosol.net.ConfigurableAddressProviderFactory;
import com.tangosol.net.InetAddressHelper;
import com.tangosol.net.SocketOptions;

import com.tangosol.net.internal.SubstitutionAddressProvider;

import com.tangosol.persistence.ConfigurableSnapshotArchiverFactory;
import com.tangosol.persistence.SnapshotArchiverFactory;

import com.tangosol.run.xml.SimpleElement;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.LiteMap;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SafeLinkedList;
import com.tangosol.util.SimpleResourceRegistry;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * LegacyXmlClusterDependencies parses the <cluster-config> XML to populate the DefaultClusterDependencies.
 *
 * NOTE: This code will eventually be replaced by CODI.
 *
 * @author pfm  2011.05.08
 * @since Coherence 3.7.1
 */
@SuppressWarnings("deprecation")
public class LegacyXmlClusterDependencies
        extends DefaultClusterDependencies
    {
    /**
     * Populate the LegacyXmlClusterDependencies object from the XML DOM.
     *
     * @param xml  the <cluster-config> XML element
     *
     * @return this object
     */
    public LegacyXmlClusterDependencies fromXml(XmlElement xml)
        {
        Base.azzert(xml.getName().equals("cluster-config"));


        // ------------------------------------------------------------------------
        // BEGIN: Use CODI to parse the operational configuration (this shouldn't be here!)
        // Need to process socket-providers definitions before processing unicast/socket-provider
        // since it can reference a socket-provider definition via name.
        // ------------------------------------------------------------------------

        // NOTE: This code should produce the entire ClusterDependencies instance.

        DocumentProcessor.DefaultDependencies dependencies =
            new DocumentProcessor.DefaultDependencies(new OperationalConfigNamespaceHandler());

        // a ResourceRegistry for the cluster (this will be discarded after parsing)
        ResourceRegistry resourceRegistry = new SimpleResourceRegistry();

        // establish a default ParameterResolver based on the System properties
        // COH-9952 wrap the code in privileged block for upper-stack products
        ScopedParameterResolver resolver = AccessController
                .doPrivileged((PrivilegedAction<ScopedParameterResolver>) () ->
                                        new ScopedParameterResolver(new ChainedParameterResolver(
                                                new SystemPropertyParameterResolver(),
                                                new SystemEnvironmentParameterResolver())));

        // finish configuring the dependencies
        dependencies.setResourceRegistry(resourceRegistry);
        dependencies.setDefaultParameterResolver(resolver);
        dependencies.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dependencies.setClassLoader(Base.getContextClassLoader());

        // establish the cluster-config processing context
        DefaultProcessingContext ctxClusterConfig = new DefaultProcessingContext(dependencies, xml);

        // add the default namespace handler
        NamespaceHandler handler = dependencies.getDefaultNamespaceHandler();
        if (handler != null)
            {
            ctxClusterConfig.ensureNamespaceHandler("", handler);
            }

        // add the ParameterizedBuilderRegistry as a Cookie, so we can look it up
        ctxClusterConfig.addCookie(ParameterizedBuilderRegistry.class, getBuilderRegistry());
        ctxClusterConfig.addCookie(DefaultClusterDependencies.class, this);

        // process custom resources
        XmlElement xmlResources = xml.getSafeElement("resources");
        SimpleResourceRegistry customResources = new SimpleResourceRegistry();
        DefaultProcessingContext ctxResources = new DefaultProcessingContext(ctxClusterConfig, xmlResources);
        ctxResources.processDocument(xmlResources);
        setCustomResourcesRegistry(customResources);

        // process the <password-providers> definitions. Could be referenced socket-provider.
        XmlElement xmlPasswordProviders = xml.getSafeElement("password-providers");
        DefaultProcessingContext ctxPasswordProviders = new DefaultProcessingContext(ctxClusterConfig, xmlPasswordProviders);
        ctxPasswordProviders.processDocument(xmlPasswordProviders);
        ctxPasswordProviders.close();

        // process the <license-mode> element.
        String sMode = xml.getSafeElement("license-mode").getString("dev");
        setMode(translateModeName(sMode));

        // process the <socket-providers> definitions.  could be referenced in unicast-listener/socket-provider.
        XmlElement xmlSocketProviders = xml.getSafeElement("socket-providers");
        DefaultProcessingContext ctxSocketProviders = new DefaultProcessingContext(ctxClusterConfig, xmlSocketProviders);
        ctxSocketProviders.processDocument(xmlSocketProviders);
        ctxSocketProviders.close();

        // process the <global-socket-provider> definition. This must be after processing the <socket-providers>
        // definitions because the global provider may be a reference to an exiting provider.
        XmlElement xmlGlobalSocketProvider = xml.getSafeElement("global-socket-provider");
        DefaultProcessingContext ctxGlobalSocketProvider = new DefaultProcessingContext(ctxClusterConfig, xmlGlobalSocketProvider);
        ctxGlobalSocketProvider.processDocument(xmlGlobalSocketProvider);
        ctxGlobalSocketProvider.close();

        XmlElement xmlCommonPool = xml.getSafeElement("common-daemon-pool");
        DefaultProcessingContext ctxCommonPool = new DefaultProcessingContext(ctxClusterConfig, xmlCommonPool);
        ctxCommonPool.processDocument(xmlCommonPool);
        ctxCommonPool.close();

        setVirtualThreadsEnabled(xml.getSafeElement("virtual-threads-enabled").getBoolean(isVirtualThreadsEnabled()));

        // ------------------------------------------------------------------------
        // SUSPEND: Use CODI to parse the operational configuration
        // ------------------------------------------------------------------------

        // the member identity gets data from both member-identity XML and unicast-listener xml.
        DefaultMemberIdentity memberIdentity = new DefaultMemberIdentity();

        configureMemberIdentity(xml, memberIdentity);
        configureMulticastListener(xml);    // this must be called before configureUnicastListener
        configureAddressProviders(xml);     // this must be called before configureUnicastListener

        // COH-14170 replace SocketProviderFactory method calls using getUnicastSocketProviderXml()
        XmlElement xmlUnicastSocketProvider = xml.getSafeElement("unicast-listener").getSafeElement("socket-provider");
        DefaultProcessingContext ctxUnicastSocketProvider = new DefaultProcessingContext(ctxClusterConfig, xmlUnicastSocketProvider);
        SocketProviderBuilder builder = new SocketProviderProcessor().process(ctxUnicastSocketProvider, xmlUnicastSocketProvider);
        setUnicastSocketProviderBuilder(builder);

        configureUnicastListener(xml, memberIdentity);

        setMemberIdentity(memberIdentity);

        configureTcpRingListener(xml);
        configureShutdownListener(xml);
        configureServiceGuardian(xml);
        configurePacketSpeaker(xml);
        configurePacketPublisher(xml);
        configureIncomingMessageHandler(xml);
        configureOutgoingMessageHandler(xml);
        configureAuthorizedHosts(xml);
        configureServices(xml);
        configureFilters(xml);
        configureSnapshotArchivers(xml);
        configureClusterQuorumPolicy(xml);

        // new in version 3.2: support configuration of license info
        // this is from <license-config>
        String sEdition = xml.getSafeElement("edition-name").getString("GE");
        setEdition(translateEditionName(sEdition));

        setLambdasSerializationMode(xml.getSafeElement("lambdas-serialization").getString());

        // ------------------------------------------------------------------------
        // RESUME: Use CODI to parse the operational configuration (this shouldn't be here!)
        // ------------------------------------------------------------------------

        // process the <storage-authorizers>
        XmlElement xmlStorageAuthorizers = xml.getSafeElement("storage-authorizers");
        DefaultProcessingContext ctxStorageAuthorizers = new DefaultProcessingContext(ctxClusterConfig,
                                                             xmlStorageAuthorizers);

        ctxStorageAuthorizers.processDocument(xmlStorageAuthorizers);

        XmlElement xmlSerializers = xml.getSafeElement("serializers");
        DefaultProcessingContext ctxSerializers = new DefaultProcessingContext(ctxClusterConfig, xmlSerializers);
        ctxSerializers.processDocument(xmlSerializers);

        XmlElement xmlEnv = xml.getSafeElement("persistence-environments");
        DefaultProcessingContext ctxPersistenceEnvironments = new DefaultProcessingContext(ctxClusterConfig, xmlEnv);
        ctxPersistenceEnvironments.processDocument(xmlEnv);

        // close the contexts we created
        ctxUnicastSocketProvider.close();
        ctxResources.close();
        ctxSerializers.close();
        ctxStorageAuthorizers.close();
        ctxPersistenceEnvironments.close();
        ctxClusterConfig.close();

        // ------------------------------------------------------------------------
        // END: Use CODI to parse the operational configuration
        // ------------------------------------------------------------------------

        return this;
        }

    // ----- internal methods  ------------------------------------------------------

    /**
     * Configure the member identity fields.
     *
     * @param xml             the member-identity xml element
     * @param memberIdentity  the memberIdentity to configure
     */
    private void configureMemberIdentity(XmlElement xml, DefaultMemberIdentity memberIdentity)
        {
        XmlElement xmlCat = xml.getSafeElement("member-identity");

        memberIdentity.setClusterName(xmlCat.getSafeElement("cluster-name").getString(memberIdentity.getClusterName()));
        memberIdentity.setSiteName(xmlCat.getSafeElement("site-name").getString(memberIdentity.getSiteName()));
        memberIdentity.setRackName(xmlCat.getSafeElement("rack-name").getString(memberIdentity.getRackName()));
        memberIdentity.setMachineName(xmlCat.getSafeElement("machine-name").getString(memberIdentity.getMachineName()));
        memberIdentity.setProcessName(xmlCat.getSafeElement("process-name").getString(memberIdentity.getProcessName()));
        memberIdentity.setMemberName(xmlCat.getSafeElement("member-name").getString(memberIdentity.getMemberName()));
        memberIdentity.setRoleName(xmlCat.getSafeElement("role-name").getString(memberIdentity.getRoleName()));
        memberIdentity.setPriority(xmlCat.getSafeElement("priority").getInt(memberIdentity.getPriority()));
        }

    /**
     * Configure the multicast listener fields.
     *
     * @param xml  the multicast-listener xml element
     */
    private void configureMulticastListener(XmlElement xml)
        {
        XmlElement xmlCat = xml.getSafeElement("multicast-listener");

        // <interface>
        setGroupInterface(resolveGroupInterface(xmlCat.getSafeElement("interface").getString("")));

        // <address>
        setGroupAddress(resolveGroupAddress(xmlCat.getSafeElement("address").getString(DEFAULT_ADDR)));

        // <port>
        setGroupPort(xmlCat.getSafeElement("port").getInt(getGroupPort()));

        // <time-to-live>
        setGroupTimeToLive(xmlCat.getSafeElement("time-to-live").getInt(getGroupTimeToLive()));

        // <packet-buffer>
        XmlElement xmlSub = xmlCat.getSafeElement("packet-buffer");

        XmlElement xmlVal = xmlSub.getElement("maximum-packets");
        if (xmlVal == null)    // size based
            {
            // <size>
            setGroupBufferSize((int) Base.parseMemorySize(
                    xmlSub.getSafeElement("size").getString(Integer.toString(getGroupBufferSize()))));
            }
        else
            // <maximum-packets>
            {
            // recorded as negative size since we may not know the packet size
            // until Cluster.onStart
            setGroupBufferSize(-xmlVal.getInt());
            if (xmlSub.getElement("size") != null)
                {
                throw new IllegalArgumentException("cannot specify maximum-packets and size within packet-buffer");
                }
            }

        // <priority>
        setGroupListenerPriority(xmlCat.getSafeElement("priority").getInt(getGroupListenerPriority()));

        // <join-timeout-milliseconds>
        setClusterAnnounceTimeoutMillis(xmlCat.getSafeElement("join-timeout-milliseconds").getInt(
                getClusterAnnounceTimeoutMillis()));

        // <multicast-threshold-percent>
        setPublisherGroupThreshold(xmlCat.getSafeElement("multicast-threshold-percent").getInt(
                getPublisherGroupThreshold()));
        }

    /**
     * Configure the unicast listener fields.
     *
     * @param xml             the unicast-listener xml element
     * @param memberIdentity  this is needed to set the machine id.
     */
    private void configureUnicastListener(XmlElement xml, DefaultMemberIdentity memberIdentity)
        {
        XmlElement xmlCat = xml.getSafeElement("unicast-listener");

        // Delete when deprecated method getUnicastSocketProviderXml() is removed.
        // <socket-provider>
        setUnicastSocketProviderXml(xmlCat.getSafeElement("socket-provider"));

        // <reliable-transport>
        setReliableTransport(xmlCat.getSafeElement("reliable-transport").getString(getReliableTransport()));

        // <socket-options> - undocumented
        setTcpDatagramSocketOptions(SocketOptions.load(xmlCat.getSafeElement("socket-options")));

        // <machine-id>
        memberIdentity.setMachineId(Integer.parseInt(xmlCat.getSafeElement("machine-id").getString("0")));

        // <address>
        String sAddr = xmlCat.getSafeElement("address").getString();
        if (sAddr != null && !sAddr.trim().isEmpty())
            {
            try
                {
                setLocalAddress(InetAddressHelper.getLocalAddress(sAddr));
                }
            catch (UnknownHostException e)
                {
                throw new IllegalArgumentException("unresolvable localhost " + sAddr, e);
                }
            }

        // <port>
        setLocalPort(xmlCat.getSafeElement("port").getInt(getLocalPort()));

        // <port-auto-adjust>
        XmlElement xmlValue  = xmlCat.getSafeElement("port-auto-adjust");
        String     sPortAuto = xmlValue.getString().trim();
        if (sPortAuto.length() > 0 && Character.isDigit(sPortAuto.charAt(0)))
            {
            setLocalPortAutoAdjust(xmlValue.getInt(getLocalPortAutoAdjust()));
            }
        else
            {
            setLocalPortAutoAdjust(xmlValue.getBoolean(isLocalPortAutoAdjust()));
            }

        // <packet-buffer>
        XmlElement xmlSub = xmlCat.getSafeElement("packet-buffer");

        XmlElement xmlVal = xmlSub.getElement("maximum-packets");
        if (xmlVal == null)    // size based
            {
            // <size>
            setLocalBufferSize((int) Base.parseMemorySize(
                xmlSub.getSafeElement("size").getString(Integer.toString(getLocalBufferSize()))));
            }
        else
            // <maximum-packets>
            {
            // recorded as negative size since we may not know the packet size
            // until Cluster.onStart
            setLocalBufferSize(-xmlVal.getInt());
            if (xmlSub.getElement("size") != null)
                {
                throw new IllegalArgumentException("cannot specify maximum-packets and size within packet-buffer");
                }
            }

        // <priority>
        setLocalListenerPriority(xmlCat.getSafeElement("priority").getInt(getLocalListenerPriority()));

        // <well-known-addresses>
        setWellKnownAddresses(createWkaAddressProvider(xmlCat.getSafeElement("well-known-addresses")));

        // <discovery-address>
        String sAddrDiscovery = xmlCat.getSafeElement("discovery-address").getString();
        if (sAddrDiscovery != null && !sAddrDiscovery.trim().isEmpty())
            {
            try
                {
                setLocalDiscoveryAddress(InetAddressHelper.getLocalAddress(sAddrDiscovery));
                }
            catch (UnknownHostException e)
                {
                throw new IllegalArgumentException("unresolvable discovery address " + sAddrDiscovery, e);
                }
            }
        }

    /**
     * Configure the tcpring listener fields.
     *
     * @param xml  the tcpring-listener xml element
     */
    private void configureTcpRingListener(XmlElement xml)
        {
        XmlElement xmlCat = xml.getSafeElement("tcp-ring-listener");

        // <enabled>
        setTcpRingEnabled(xmlCat.getSafeElement("enabled").getBoolean(isTcpRingEnabled()));

        // <ip-timeout>
        setIpMonitorTimeoutMillis(XmlHelper.parseTime(xmlCat, "ip-timeout", getIpMonitorTimeoutMillis()));

        // <ip-attempts>
        setIpMonitorAttempts(xmlCat.getSafeElement("ip-attempts").getInt(getIpMonitorAttempts()));

        // <listen-backlog>
        setTcpBacklog(xmlCat.getSafeElement("listen-backlog").getInt(getTcpBacklog()));

        // <priority>
        setIpMonitorPriority(xmlCat.getSafeElement("priority").getInt(getIpMonitorPriority()));

        // <socket-options> - undocumented
        setTcpRingSocketOptions(SocketOptions.load(xmlCat.getSafeElement("socket-options")));
        }

    /**
     * Configure the shutdown listener fields.
     *
     * @param xml  the shutdown-listener xml element
     */
    private void configureShutdownListener(XmlElement xml)
        {
        XmlElement xmlCat = xml.getSafeElement("shutdown-listener");

        // <enabled>
        String sShutdownOption = xmlCat.getSafeElement("enabled").getString();
        int    nShutdownOption = getShutdownHookOption();
        switch (sShutdownOption)
            {
            case "force":
            case "true":
                nShutdownOption = ClusterDependencies.SHUTDOWN_FORCE;
                break;
            case "none":
            case "false":
                nShutdownOption = ClusterDependencies.SHUTDOWN_NONE;
                break;
            case "graceful":
                nShutdownOption = ClusterDependencies.SHUTDOWN_GRACEFUL;
                break;
            }
        setShutdownHookOption(nShutdownOption);
        }

    /**
     * Configure the service guardian fields.
     *
     * @param xml  the service-guardian xml element
     */
    private void configureServiceGuardian(XmlElement xml)
        {
        // <service-guardian>
        XmlElement xmlCat = xml.getSafeElement("service-guardian");

        // <service-failure-policy>
        XmlElement                  xmlSub  = xmlCat.getSafeElement("service-failure-policy");
        ServiceFailurePolicyBuilder builder = LegacyXmlConfigHelper.parseServiceFailurePolicyBuilder(xmlSub);

        if (builder != null)
            {
            setServiceFailurePolicyBuilder(builder);
            }

        // <timeout-milliseconds>
        setGuardTimeoutMillis(xmlCat.getSafeElement("timeout-milliseconds").getLong(getGuardTimeoutMillis()));
        }

    /**
     * Configure the packet speaker fields.
     *
     * @param xml  the packet-speaker xml element
     */
    private void configurePacketSpeaker(XmlElement xml)
        {
        XmlElement xmlCat = xml.getSafeElement("packet-speaker");

        // <enabled>
        setSpeakerEnabled(xmlCat.getSafeElement("enabled").getBoolean(isSpeakerEnabled()));

        // <volume-threshold>
        XmlElement xmlSub = xmlCat.getSafeElement("volume-threshold");

        // <minimum-packets>
        setSpeakerVolumeMinimum(xmlSub.getSafeElement("minimum-packets").getInt(getSpeakerVolumeMinimum()));

        // <priority>
        setSpeakerPriority(xmlCat.getSafeElement("priority").getInt(getSpeakerPriority()));
        }

    /**
     * Configure the packet publisher fields.
     *
     * @param xml  the packet-publisher xml element
     */
    private void configurePacketPublisher(XmlElement xml)
        {
        XmlElement xmlCat = xml.getSafeElement("packet-publisher");

        // <packet-size>
        XmlElement xmlSub = xmlCat.getSafeElement("packet-size");

        // <maximum-length>
        setPacketMaxLength(xmlSub.getSafeElement("maximum-length").getInt(getPacketMaxLength()));

        // <preferred-length>
        setPacketPreferredLength(xmlSub.getSafeElement("preferred-length").getInt(getPacketPreferredLength()));

        // <packet-delivery>
        xmlSub = xmlCat.getSafeElement("packet-delivery");

        // <resend-milliseconds>
        setPublisherResendDelayMillis(xmlSub.getSafeElement("resend-milliseconds").getInt(
            getPublisherResendDelayMillis()));

        // <timeout-milliseconds>
        setPublisherResendTimeoutMillis(xmlSub.getSafeElement("timeout-milliseconds").getInt(
            getPublisherResendTimeoutMillis()));

        // <heartbeat-milliseconds>
        setClusterHeartbeatDelayMillis(xmlSub.getSafeElement("heartbeat-milliseconds").getInt(
            getClusterHeartbeatDelayMillis()));

        // <flow-control>
        XmlElement xmlSub2 = xmlSub.getSafeElement("flow-control");

        // <enabled>
        setFlowControlEnabled(xmlSub2.getSafeElement("enabled").getBoolean(isFlowControlEnabled()));

        // <pause-detection/maximum-packets>
        setLostPacketThreshold(xmlSub2.getSafeElement("pause-detection/maximum-packets").getInt(
            getLostPacketThreshold()));

        // <outstanding-packets>
        XmlElement xmlVal = xmlSub2.getSafeElement("outstanding-packets");

        setOutstandingPacketMaximum(xmlVal.getSafeElement("maximum-packets").getInt(getOutstandingPacketMaximum()));

        // read min second so it can be validated against max
        setOutstandingPacketMinimum(xmlVal.getSafeElement("minimum-packets").getInt(getOutstandingPacketMinimum()));

        // <packet-bundling>
        xmlSub2 = xmlSub.getSafeElement("packet-bundling");

        // <maximum-deferral-time>
        String sTime = xmlSub2.getSafeElement("maximum-deferral-time").getString();
        if (sTime.length() > 0)
            {
            setPacketBundlingThresholdNanos(Base.parseTimeNanos(sTime));
            }

        // <aggression-factor>
        setPacketBundlingAggression(xmlSub2.getSafeElement("aggression-factor").getDouble(
                getPacketBundlingAggression()));

        // <notification-queueing>
        xmlSub = xmlCat.getSafeElement("notification-queueing");

        // <ack-delay-milliseconds>
        setPublisherAckDelayMillis(xmlSub.getSafeElement("ack-delay-milliseconds").getInt(
            getPublisherAckDelayMillis()));

        // <nack-delay-milliseconds>
        setPublisherNackDelayMillis(xmlSub.getSafeElement("nack-delay-milliseconds").getInt(
            getPublisherNackDelayMillis()));

        // <traffic-jam>
        xmlSub = xmlCat.getSafeElement("traffic-jam");

        // <maximum-packets>
        setPublisherCloggedCount(xmlSub.getSafeElement("maximum-packets").getInt(getPublisherCloggedCount()));

        // <pause-milliseconds>
        setPublisherCloggedDelayMillis(xmlSub.getSafeElement("pause-milliseconds").getInt(
            getPublisherCloggedDelayMillis()));

        // <packet-buffer>
        xmlSub = xmlCat.getSafeElement("packet-buffer");

        xmlVal = xmlSub.getElement("maximum-packets");
        if (xmlVal == null)    // size based
            {
            // <size>
            setPublisherSocketBufferSize((int) Base.parseMemorySize(
                xmlSub.getSafeElement("size").getString(Integer.toString(getPublisherSocketBufferSize()))));
            }
        else
            // <maximum-packets>
            {
            // recorded as negative size since we may not know the packet size
            // until Cluster.onStart
            setPublisherSocketBufferSize(-xmlVal.getInt());
            if (xmlSub.getElement("size") != null)
                {
                throw new IllegalArgumentException("cannot specify maximum-packets and size within packet-buffer");
                }
            }

        // <priority>
        setPublisherPriority(xmlCat.getSafeElement("priority").getInt(getPublisherPriority()));

        // <enabled>
        setTcmpEnabled(xmlCat.getSafeElement("enabled").getBoolean(isTcmpEnabled()));
        }

    /**
     * Configure the incoming message handler fields.
     *
     * @param xml  the incoming-message-handler xml element
     */
    private void configureIncomingMessageHandler(XmlElement xml)
        {
        XmlElement xmlCat = xml.getSafeElement("incoming-message-handler");

        // <maximum-time-variance>
        setClusterTimestampMaxVarianceMillis(xmlCat.getSafeElement("maximum-time-variance").getInt(
                getClusterTimestampMaxVarianceMillis()));

        // <use-nack-packets>
        setReceiverNackEnabled(xmlCat.getSafeElement("use-nack-packets").getBoolean(isReceiverNackEnabled()));

        // <priority>
        setReceiverPriority(xmlCat.getSafeElement("priority").getInt(getReceiverPriority()));
        }

    /**
     * Configure the outgoing message handler fields.
     *
     * @param xml  the outgoing-message-handler xml element
     */
    @SuppressWarnings("unchecked")
    private void configureOutgoingMessageHandler(XmlElement xml)
        {
        XmlElement xmlCat = xml.getSafeElement("outgoing-message-handler");

        // <use-filters>
        List<String> listFilter = new SafeLinkedList();
        listFilter.addAll(LegacyXmlConfigHelper.parseFilterList(xmlCat));
        setFilterList(listFilter);
        }

    /**
     * Configure the authorized hosts fields.
     *
     * @param xml  the authorized-hosts xml element
     */
    @SuppressWarnings("rawtypes")
    private void configureAuthorizedHosts(XmlElement xml)
        {
        XmlElement xmlCat = xml.getSafeElement("authorized-hosts");

        // Use a custom filter if is specified.
        // <host-filter>
        XmlElement xmlVal = xmlCat.getElement("host-filter");
        if (xmlVal != null && !XmlHelper.isEmpty(xmlVal))
            {
            setAuthorizedHostFilter((Filter) XmlHelper.createInstance(xmlVal,
            /*loader*/ null, /*resolver*/ null, Filter.class));

            // don't process any host-addresses since there is a custom filter.
            return;
            }

        InetAddressRangeFilter filter       = new InetAddressRangeFilter();
        boolean                fFilterAdded = false;

        // <host-address>
        for (Iterator iter = xmlCat.getElements("host-address"); iter.hasNext(); )
            {
            xmlVal = (XmlElement) iter.next();
            if (addAuthorizedHostsToFilter(filter, xmlVal.getString(), /* sAddrTo */ null))
                {
                fFilterAdded = true;
                }
            }

        // <host-range>
        for (Iterator iter = xmlCat.getElements("host-range"); iter.hasNext(); )
            {
            xmlVal = (XmlElement) iter.next();
            if (addAuthorizedHostsToFilter(filter, xmlVal.getSafeElement("from-address").getString(),
                                           xmlVal.getSafeElement("to-address").getString()))
                {
                fFilterAdded = true;
                }
            }

        if (fFilterAdded)
            {
            // the XML successfully specified at least 1 range so set the filter.
            // NOTE: Never call setAuthorizedHostFilter if the filter is empty.
            setAuthorizedHostFilter(filter);
            }
        }

    /**
     * Configure the services fields.
     *
     * @param xml  the services xml element
     */
    @SuppressWarnings("rawtypes")
    private void configureServices(XmlElement xml)
        {
        Map<String, String>       mapService       = new LiteMap<>();
        Map<String, List<String>> mapServiceFilter = new LiteMap<>();
        for (Iterator iter = xml.getSafeElement("services").getElements("service"); iter.hasNext(); )
            {
            XmlElement xmlService = (XmlElement) iter.next();
            String     sType      = xmlService.getSafeElement("service-type").getString();
            String     sComponent = xmlService.getSafeElement("service-component").getString();
            mapService.put(sType, sComponent);
            mapServiceFilter.put(sType, LegacyXmlConfigHelper.parseFilterList(xmlService));

            // TODO - COH-5021 For now the ServiceTemplateMap is not used and it
            // is empty
            // Service.Template template = new Service.Template(sType,
            // XmlHelper.transformInitParams(new SimpleElement(
            // "config"), xmlService.getSafeElement("init-params")));
            // String sType, XmlElement xmlInitParam;
            }
        setServiceMap(mapService);
        // setServiceTemplateMap(mapServiceTemplate);
        setServiceFilterMap(mapServiceFilter);
        }

    /**
     * Configure the filter by creating a map of filters indexed by filter name.
     *
     * @param xml  the filters xml element
     */
    @SuppressWarnings("rawtypes")
    private void configureFilters(XmlElement xml)
        {
        Map<String, WrapperStreamFactory> mapFilter = new LiteMap<>();
        for (Iterator iter = xml.getSafeElement("filters").getElements("filter"); iter.hasNext(); )
            {
            XmlElement xmlFilter = (XmlElement) iter.next();
            addFilterToMap(xmlFilter, mapFilter);
            }
        setFilterMap(mapFilter);
        }

    /**
     * Configure the snapshot archivers by populating a map of snapshot
     * archiver factories indexed by id.
     *
     * @param xml  the snapshot-archivers xml element
     */
    private void configureSnapshotArchivers(XmlElement xml)
        {
        Map<String, SnapshotArchiverFactory> mapSnapshotArchivers = new LiteMap<>();
        for (Object o : xml.getSafeElement("snapshot-archivers").getElementList())
            {
            XmlElement xmlSnapshotArchivers = (XmlElement) o;
            String     sName                = xmlSnapshotArchivers.getSafeAttribute("id").getString();

            ConfigurableSnapshotArchiverFactory factory = new ConfigurableSnapshotArchiverFactory();
            factory.setConfig(xmlSnapshotArchivers);
            mapSnapshotArchivers.put(sName, factory);
            }
        setSnapshotArchiverMap(mapSnapshotArchivers);
        }

    /**
     * Configure the address providers by populating a map of address provider factories
     * indexed by id.
     *
     * @param xml  the address-providers xml element
     */
    @SuppressWarnings("rawtypes")
    private void configureAddressProviders(XmlElement xml)
        {
        Map<String, AddressProviderFactory> mapAddressProvider = new LiteMap<>();

        // construct a "cluster-discovery" address provider based on WKA or MC address; see TcpInitiator.onDependencies
        AddressProviderFactory factoryCluster;
        XmlElement xmlWKA = xml.getSafeElement("unicast-listener").getSafeElement("well-known-addresses");
        if (createWkaAddressProvider(xmlWKA) == null)
            {
            if (isWkaPresent(xmlWKA))
                {
                throw new IllegalArgumentException("Unresolvable WKA address(s) " + xmlWKA);
                }

            ListBasedAddressProviderBuilder factoryMulticast = new ListBasedAddressProviderBuilder();
            factoryMulticast.add(getGroupAddress().getHostAddress(), getGroupPort());
            factoryCluster = factoryMulticast;
            }
        else
            {
            factoryCluster = SubstitutionAddressProvider.createFactory(LegacyXmlConfigHelper
                    .parseAddressProvider("well-known-addresses", xml.getSafeElement("unicast-listener"), mapAddressProvider),
                    getGroupPort());
            }
        mapAddressProvider.put("cluster-discovery", factoryCluster);

        for (Iterator iter = xml.getSafeElement("address-providers").getElements("address-provider"); iter.hasNext(); )
            {
            XmlElement xmlAddressProvider = (XmlElement) iter.next();
            String     sName              = xmlAddressProvider.getSafeAttribute("id").getString();
            XmlElement xmlLocalAddress    = xmlAddressProvider.getElement("local-address");

            if (mapAddressProvider.containsKey(sName))
                {
                throw new ConfigurationException("address provider '" + sName + "' has already been defined",
                        "remove duplicate provider definition");
                }
            else if (xmlLocalAddress == null)
                {
                ConfigurableAddressProviderFactory factory = new ConfigurableAddressProviderFactory();
                factory.setConfig(xmlAddressProvider);
                mapAddressProvider.put(sName, factory);
                }
            else
                {
                @SuppressWarnings("unused")
                AddressProviderBuilder builder =
                    AddressProviderBuilderProcessor.newLocalAddressProviderBuilder(xmlLocalAddress);
                }
            }

        setAddressProviderMap(mapAddressProvider);
        }

    /**
     * Configure the cluster quorum policy.
     *
     * @param xml  the cluster-quorum-policy xml element
     */
    private void configureClusterQuorumPolicy(XmlElement xml)
        {
        XmlElement xmlCat = xml.getSafeElement("cluster-quorum-policy");

        setClusterActionPolicyBuilder(new LegacyXmlConfigurableQuorumPolicy().createPolicyBuilder(xmlCat, null,
                Base.getContextClassLoader()));
        }

    /**
     * Add an authorized host range to the filter.
     *
     * @param sAddrFrom  from address
     * @param sAddrTo    to address
     *
     * @return true if filter added
     */
    private boolean addAuthorizedHostsToFilter(InetAddressRangeFilter filter, String sAddrFrom, String sAddrTo)
        {
        if (sAddrFrom == null || sAddrFrom.length() == 0)
            {
            if (sAddrTo != null && sAddrTo.length() != 0)
                {
                ensureAuthorizedHostFilter();
                Base.azzertFailed("Both <from-ip> and <to-ip> elements must be specified");
                }
            return false;
            }

        InetAddress addrFrom;
        InetAddress addrTo;
        try
            {
            addrFrom = InetAddress.getByName(sAddrFrom);
            addrTo   = sAddrTo == null ? addrFrom : InetAddress.getByName(sAddrTo);
            }
        catch (UnknownHostException e)
            {
            Base.trace("Unresolvable authorized host will be ignored: " + e);
            return false;
            }

        filter.addRange(addrFrom, addrTo);
        return true;
        }


    /**
     * Create a filter factory and add it to the filter map.
     *
     * @param xmlFilter  the xml element
     * @param mapFilter  the map that holds the factory entries
     */
    @SuppressWarnings("unchecked")
    private void addFilterToMap(XmlElement xmlFilter, Map<String, WrapperStreamFactory> mapFilter)
        {
        String sClass = xmlFilter.getSafeElement("filter-class").getString();
        String sName  = xmlFilter.getSafeElement("filter-name").getString();
        if (sName.length() == 0)
            {
            sName = xmlFilter.getSafeAttribute("id").getString();
            }

        XmlElement xmlConfig   = new SimpleElement("filter");
        XmlElement xmlInstance = xmlConfig.ensureElement("instance");
        xmlInstance.ensureElement("class-name").setString(sClass);

        XmlElement xmlParams = xmlFilter.getElement("init-params");
        if (xmlParams != null)
            {
            xmlInstance.getElementList().add(xmlParams);
            }
        try
            {
            WrapperStreamFactory factory = (WrapperStreamFactory) XmlHelper.createInstance(xmlConfig,
                                               Base.getContextClassLoader(),

            /* resolver */
            null, WrapperStreamFactory.class);

            mapFilter.put(sName, factory);
            }
        catch (Throwable e)
            {
            throw new IllegalArgumentException("Error instantiating Filter with name: " + sName);
            }
        }

    /**
     * Parse the well known addresses to produce an AddressProvider.
     *
     * @param xml  the well-known-addresses element
     *
     * @return the WKA address provider, or null if WKA is not enabled
     */
    @SuppressWarnings("ConstantConditions")
    private AddressProvider createWkaAddressProvider(XmlElement xml)
        {
        AddressProviderFactory factory  = LegacyXmlConfigHelper.parseAddressProvider(xml, getAddressProviderMap());
        AddressProvider        provider = new SubstitutionAddressProvider(factory.createAddressProvider(null), getGroupPort());

        if (!(provider instanceof Set))
            {
            CompositeAddressProvider providerComposite = new CompositeAddressProvider();

            providerComposite.addProvider(provider);
            provider = providerComposite;
            }

        Set<?> set = (Set<?>) provider;
        if (set.isEmpty())
            {
            boolean fRetry = Config.getBoolean(PROP_WKA_RESOLVE_RETRY);
            if (fRetry)
                {
                long nTimeout = Config.getLong(PROP_WKA_TIMEOUT, DEFAULT_WKA_RESOLVE_TIMEOUT.toMillis());
                long nRetry   = Config.getLong(PROP_WKA_RESOLVE_FREQUENCY, DEFAULT_WKA_RESOLVE_FREQUENCY.toMillis());
                long nStart   = System.currentTimeMillis();

                Logger.info("Failed to resolve WKA addresses, retrying for " + nTimeout + " millis");
                while (set.isEmpty() && nTimeout > (System.currentTimeMillis() - nStart))
                    {
                    Base.sleep(nRetry);
                    }
                }
            }

        return ((Set<?>) provider).isEmpty() ? null : provider;
        }

    /**
     * Ensure that the authorized host filter
     *
     * NOTE: Do not create this unless there are authorized hosts or else Coherence will
     * reject the member during a join.
     */
    private void ensureAuthorizedHostFilter()
        {
        Filter<?> filter = getAuthorizedHostFilter();
        if (filter == null)
            {
            setAuthorizedHostFilter(new InetAddressRangeFilter());
            }
        }


    /**
     * Resolve the group address string.
     *
     * @param sAddr  the group address string
     *
     * @return the group address
     */
    private InetAddress resolveGroupAddress(String sAddr)
        {
        InetAddress addr = null;
        if (sAddr != null && sAddr.trim().length() > 0)
            {
            try
                {
                addr = InetAddress.getByName(sAddr);
                }
            catch (Exception e)
                {
                throw Base.ensureRuntimeException(e, "Address=" + sAddr);
                }
            }
        return addr;
        }

    /**
     * Resolve the group interface string.
     *
     * @param sAddr  the group interface
     *
     * @return the group interface InetAddress
     */
    private InetAddress resolveGroupInterface(String sAddr)
        {
        InetAddress addr = null;
        if (sAddr != null && sAddr.trim().length() > 0)
            {
            try
                {
                addr = InetAddresses.getLocalAddress(sAddr);
                }
            catch (Exception e)
                {
                throw Base.ensureRuntimeException(e, "Interface=" + sAddr);
                }
            }
        return addr;
        }

    /**
     * Translate the edition name.
     *
     * @param sName  edition name
     *
     * @return edition integer
     */
    private int translateEditionName(String sName)
        {
        Base.checkNotNull(sName, "Edition name");

        String[] asNames =
            {
            "DC", "RTC", "SE", "CE", "EE", "GE"
            };

        for (int i = 0; i < asNames.length; i++)
            {
            if (asNames[i].equalsIgnoreCase(sName))
                {
                return i;
                }
            }
        throw new IllegalArgumentException("Invalid edition name " + sName);
        }

    /**
     * Translate the mode name into an integer
     *
     * @param sName  mode name
     *
     * @return the mode integer
     */
    private int translateModeName(String sName)
        {
        Base.checkNotNull(sName, "Mode ");
        String[] names = {"eval", "dev", "prod"};
        for (int i = 0; i < names.length; i++)
            {
            if (names[i].equalsIgnoreCase(sName))
                {
                return i;
                }
            }
        throw new IllegalArgumentException("Invalid mode " + sName);
        }

    /**
     * Return true if a WKA list is configured.
     *
     * @return true if a WKA list is configured
     */
    @SuppressWarnings("unchecked")
    private boolean isWkaPresent(XmlElement xmlConfig)
        {
        if (xmlConfig != null)
            {
            for (XmlElement xmlAddr : (List<XmlElement>) xmlConfig.getElementList())
                {
                String sAddr;
                switch (xmlAddr.getName())
                    {
                    case "socket-address":
                        sAddr = xmlAddr.getSafeElement("address").getString().trim();
                        break;

                    case "host-address":
                    case "address":
                        sAddr = xmlAddr.getString().trim();
                        break;

                    default:
                        continue;
                    }

                if (!sAddr.isEmpty())
                    {
                    return true;
                    }
                }
            }

        return false;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The name of the System property to configure maximum time to attempt to resolve wka addresses.
     * Set this system property to a number of milliseconds.
     */
    public static final String PROP_WKA_TIMEOUT = "coherence.wka.dns.resolution.timeout";

    /**
     * System property for configuring the frequency to attempt dns resolution of wka addresses.
     * Set this system property to a number of milliseconds.
     */
    public static final String PROP_WKA_RESOLVE_FREQUENCY = "coherence.wka.dns.resolution.frequency";

    /**
     * The System property to set to block WKA resolution until at least one WKA address has been resolved.
     * <p>
     * The default behaviour is not to retry.
     */
    public static final String PROP_WKA_RESOLVE_RETRY = "coherence.wka.dns.resolution.retry";

    /**
     * The default timeout to wait for resolution of at least one WKA address.
     */
    public static final Duration DEFAULT_WKA_RESOLVE_TIMEOUT = Duration.ofMinutes(6);

    /**
     * The default retry frequency to attempt resolution of at least one WKA address.
     */
    public static final Duration DEFAULT_WKA_RESOLVE_FREQUENCY = Duration.ofMillis(10);

    }
