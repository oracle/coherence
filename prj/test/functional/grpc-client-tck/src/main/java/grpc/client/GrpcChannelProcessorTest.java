/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package grpc.client;

import com.oracle.coherence.grpc.client.common.ChannelProvider;
import com.oracle.coherence.grpc.client.common.config.GrpcChannelProcessor;
import com.tangosol.coherence.config.ParameterMacroExpressionParser;
import com.tangosol.coherence.config.xml.CacheConfigNamespaceHandler;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.DefaultProcessingContext;
import com.tangosol.config.xml.DocumentProcessor;
import com.tangosol.net.grpc.GrpcChannelDependencies;
import com.tangosol.net.grpc.GrpcTransportSecurity;
import com.tangosol.run.xml.SimpleElement;
import com.tangosol.run.xml.SimpleValue;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;
import io.grpc.Channel;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class GrpcChannelProcessorTest
    {
    @Test
    public void shouldProcessEmptyXml()
        {
        String     sChannelName = "foo";
        XmlElement xml          = new SimpleElement("grpc-channel");

        xml.setAttribute("id", new SimpleValue(sChannelName));

        GrpcChannelDependencies channelDependencies = processXML(xml);

        assertThat(channelDependencies, is(notNullValue()));
        assertThat(channelDependencies.getTarget(), is(nullValue()));
        assertThat(channelDependencies.getSocketProviderBuilder(), is(nullValue()));
        assertThat(channelDependencies.getRemoteAddressProviderBuilder(), is(nullValue()));
        assertThat(channelDependencies.isNameServiceAddressProvider(), is(true));
        assertThat(channelDependencies.getDefaultLoadBalancingPolicy(), is(GrpcChannelDependencies.DEFAULT_LOAD_BALANCER_POLICY));
        assertThat(channelDependencies.getSecureTransport(), is(GrpcTransportSecurity.SECURE_TRANSPORT_OPTIONAL));
        assertThat(channelDependencies.getAuthorityOverride(), is(notNullValue()));
        assertThat(channelDependencies.getAuthorityOverride().isPresent(), is(false));
        assertThat(channelDependencies.getConfigurer(), is(notNullValue()));
        assertThat(channelDependencies.getConfigurer().isPresent(), is(false));
        assertThat(channelDependencies.getChannelProvider(), is(notNullValue()));
        assertThat(channelDependencies.getChannelProvider().isPresent(), is(false));
        }

    @Test
    public void shouldProcessTarget()
        {
        String     sChannelName = "foo";
        XmlElement xml          = new SimpleElement("grpc-channel");

        xml.setAttribute("id", new SimpleValue(sChannelName));
        xml.addElement("target").setString("coherence.oracle.com");

        GrpcChannelDependencies channelDependencies = processXML(xml);
        assertThat(channelDependencies.getTarget(), is("coherence.oracle.com"));

        assertThat(channelDependencies, is(notNullValue()));
        assertThat(channelDependencies.getSocketProviderBuilder(), is(nullValue()));
        assertThat(channelDependencies.getRemoteAddressProviderBuilder(), is(nullValue()));
        assertThat(channelDependencies.isNameServiceAddressProvider(), is(true));
        assertThat(channelDependencies.getAuthorityOverride(), is(notNullValue()));
        assertThat(channelDependencies.getAuthorityOverride().isPresent(), is(false));
        assertThat(channelDependencies.getConfigurer(), is(notNullValue()));
        assertThat(channelDependencies.getConfigurer().isPresent(), is(false));
        assertThat(channelDependencies.getChannelProvider(), is(notNullValue()));
        assertThat(channelDependencies.getChannelProvider().isPresent(), is(false));
        }

    @Test
    public void shouldProcessOverrideAuthority()
        {
        String     sChannelName = "foo";
        XmlElement xml          = new SimpleElement("grpc-channel");

        xml.setAttribute("id", new SimpleValue(sChannelName));
        xml.addElement("override-authority").setString("coherence.oracle.com");

        GrpcChannelDependencies channelDependencies = processXML(xml);
        assertThat(channelDependencies.getAuthorityOverride(), is(notNullValue()));
        assertThat(channelDependencies.getAuthorityOverride().isPresent(), is(true));
        assertThat(channelDependencies.getAuthorityOverride().get(), is("coherence.oracle.com"));

        assertThat(channelDependencies, is(notNullValue()));
        assertThat(channelDependencies.getTarget(), is(nullValue()));
        assertThat(channelDependencies.getSocketProviderBuilder(), is(nullValue()));
        assertThat(channelDependencies.getRemoteAddressProviderBuilder(), is(nullValue()));
        assertThat(channelDependencies.isNameServiceAddressProvider(), is(true));
        assertThat(channelDependencies.getConfigurer(), is(notNullValue()));
        assertThat(channelDependencies.getConfigurer().isPresent(), is(false));
        assertThat(channelDependencies.getChannelProvider(), is(notNullValue()));
        assertThat(channelDependencies.getChannelProvider().isPresent(), is(false));
        }

    @Test
    public void shouldProcessLoadBalancer()
        {
        String     sChannelName = "foo";
        XmlElement xml          = new SimpleElement("grpc-channel");

        xml.setAttribute("id", new SimpleValue(sChannelName));
        xml.addElement("load-balancer-policy").setString("pick_first");

        GrpcChannelDependencies channelDependencies = processXML(xml);
        assertThat(channelDependencies.getDefaultLoadBalancingPolicy(), is("pick_first"));

        assertThat(channelDependencies, is(notNullValue()));
        assertThat(channelDependencies.getTarget(), is(nullValue()));
        assertThat(channelDependencies.getSocketProviderBuilder(), is(nullValue()));
        assertThat(channelDependencies.getRemoteAddressProviderBuilder(), is(nullValue()));
        assertThat(channelDependencies.isNameServiceAddressProvider(), is(true));
        assertThat(channelDependencies.getAuthorityOverride(), is(notNullValue()));
        assertThat(channelDependencies.getAuthorityOverride().isPresent(), is(false));
        assertThat(channelDependencies.getConfigurer(), is(notNullValue()));
        assertThat(channelDependencies.getConfigurer().isPresent(), is(false));
        assertThat(channelDependencies.getChannelProvider(), is(notNullValue()));
        assertThat(channelDependencies.getChannelProvider().isPresent(), is(false));
        }

    @Test
    public void shouldProcessSecureTransport()
        {
        String     sChannelName = "foo";
        XmlElement xml          = new SimpleElement("grpc-channel");

        xml.setAttribute("id", new SimpleValue(sChannelName));
        xml.addElement("secure-transport").setString("required");

        GrpcChannelDependencies channelDependencies = processXML(xml);
        assertThat(channelDependencies.getSecureTransport(), is(GrpcTransportSecurity.SECURE_TRANSPORT_REQUIRED));
        }

    @Test
    public void shouldRejectInvalidSecureTransport()
        {
        XmlElement xml = new SimpleElement("grpc-channel");
        xml.addElement("secure-transport").setString("mandatory");

        ConfigurationException thrown = org.junit.jupiter.api.Assertions.assertThrows(ConfigurationException.class, () -> processXML(xml));
        assertThat(thrown.getCause(), is(instanceOf(ReflectiveOperationException.class)));
        assertThat(thrown.getCause().getCause(), is(instanceOf(IllegalArgumentException.class)));
        }

    @Test
    public void shouldOverrideSecureTransportFromNamedChannelSystemProperty()
        {
        String     sChannelName = "foo";
        XmlElement xml          = new SimpleElement("grpc-channel");

        xml.setAttribute("id", new SimpleValue(sChannelName));

        String sProperty = String.format(GrpcChannelDependencies.PROP_SECURE_TRANSPORT, sChannelName);
        String sOld      = System.getProperty(sProperty);
        try
            {
            System.setProperty(sProperty, GrpcTransportSecurity.SECURE_TRANSPORT_REQUIRED);
            GrpcChannelDependencies channelDependencies = processXML(xml);
            assertThat(channelDependencies.getSecureTransport(), is(GrpcTransportSecurity.SECURE_TRANSPORT_REQUIRED));
            }
        finally
            {
            if (sOld == null)
                {
                System.clearProperty(sProperty);
                }
            else
                {
                System.setProperty(sProperty, sOld);
                }
            }
        }

    @Test
    public void shouldProcessInstance()
        {
        String     sChannelName = "foo";
        XmlElement xml          = new SimpleElement("grpc-channel");

        xml.setAttribute("id", new SimpleValue(sChannelName));

        XmlElement xmlInstance = xml.addElement("instance");
        xmlInstance.addElement("class-name").setString(ChannelProviderStub.class.getName());

        Channel channel = mock(Channel.class);
        ChannelProviderStub.setChannel(channel);

        GrpcChannelDependencies   channelDependencies = processXML(xml);
        Optional<ChannelProvider> optional            = channelDependencies.getChannelProvider();

        assertThat(optional, is(notNullValue()));
        assertThat(optional.isPresent(), is(true));

        ChannelProvider provider = optional.get();
        assertThat(provider, is(notNullValue()));

        Optional<Channel> optionalChannel = provider.getChannel("foo");
        assertThat(optionalChannel.isPresent(), is(true));
        assertThat(optionalChannel.get(), is(sameInstance(channel)));

        assertThat(channelDependencies, is(notNullValue()));
        assertThat(channelDependencies.getTarget(), is(nullValue()));
        assertThat(channelDependencies.getSocketProviderBuilder(), is(nullValue()));
        assertThat(channelDependencies.getRemoteAddressProviderBuilder(), is(nullValue()));
        assertThat(channelDependencies.isNameServiceAddressProvider(), is(true));
        assertThat(channelDependencies.getAuthorityOverride(), is(notNullValue()));
        assertThat(channelDependencies.getAuthorityOverride().isPresent(), is(false));
        assertThat(channelDependencies.getConfigurer(), is(notNullValue()));
        assertThat(channelDependencies.getConfigurer().isPresent(), is(false));
        }

    @Test
    public void shouldProcessConfigurer()
        {
        String     sChannelName = "foo";
        XmlElement xml          = new SimpleElement("grpc-channel");

        xml.setAttribute("id", new SimpleValue(sChannelName));
        XmlElement xmlConfigurer = xml.addElement("configurer");
        xmlConfigurer.addElement("class-name").setString(GrpcChannelConfigurerStub.class.getName());

        GrpcChannelDependencies channelDependencies = processXML(xml);
        assertThat(channelDependencies.getConfigurer(), is(notNullValue()));
        assertThat(channelDependencies.getConfigurer().isPresent(), is(true));
        assertThat(channelDependencies.getConfigurer().get(), is(instanceOf(GrpcChannelConfigurerStub.class)));

        assertThat(channelDependencies, is(notNullValue()));
        assertThat(channelDependencies.getTarget(), is(nullValue()));
        assertThat(channelDependencies.getSocketProviderBuilder(), is(nullValue()));
        assertThat(channelDependencies.getRemoteAddressProviderBuilder(), is(nullValue()));
        assertThat(channelDependencies.isNameServiceAddressProvider(), is(true));
        assertThat(channelDependencies.getAuthorityOverride(), is(notNullValue()));
        assertThat(channelDependencies.getAuthorityOverride().isPresent(), is(false));
        assertThat(channelDependencies.getChannelProvider(), is(notNullValue()));
        assertThat(channelDependencies.getChannelProvider().isPresent(), is(false));
        }

    // ----- helper methods -------------------------------------------------

    private GrpcChannelDependencies processXML(XmlElement xml)
        {
        return processXML(xml, new CacheConfigNamespaceHandler());
        }

    private GrpcChannelDependencies processXML(XmlElement xml, CacheConfigNamespaceHandler namespaceHandler)
        {
        DocumentProcessor.DefaultDependencies dependencies =
                new DocumentProcessor.DefaultDependencies();

        ResourceRegistry registry = new SimpleResourceRegistry();

        dependencies.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dependencies.setResourceRegistry(registry);

        DefaultProcessingContext context = new DefaultProcessingContext(dependencies, null);
        context.ensureNamespaceHandler("", namespaceHandler);

        return new GrpcChannelProcessor().process(context, xml);
        }
    }
