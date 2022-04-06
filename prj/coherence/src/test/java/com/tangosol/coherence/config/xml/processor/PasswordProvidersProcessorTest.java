/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.ParameterMacroExpressionParser;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry;
import com.tangosol.coherence.config.xml.OperationalConfigNamespaceHandler;
import com.tangosol.coherence.config.xml.processor.PasswordProviderProcessorTest.PASSWORD_PROVIDER_XML;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.xml.DefaultProcessingContext;
import com.tangosol.config.xml.DocumentProcessor;
import com.tangosol.config.xml.NamespaceHandler;

import com.tangosol.internal.net.cluster.DefaultClusterDependencies;

import com.tangosol.net.PasswordProvider;
import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.LiteMap;
import com.tangosol.util.SimpleResourceRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;
import java.util.Map;

/**
 * Unit Tests for {@link PasswordProvidersProcessor}s
 *
 * Following is a sample for a <password-providers> block:
 *
 * <password-providers>
 *    <password-provider id="trustMgrPass">
 *        <class-name>com.tangosol.coherence.config.xml.processor.GetPassword</class-name>
 *        <init-params>
 *            <init-param>
 *                <param-name>param_1</param-name>
 *                <param-value>storepassword</param-value>
 *            </init-param>
 *        </init-params>
 *    </password-provider>
 *    <password-provider id="identityMgrPass">
 *        <class-name>com.tangosol.coherence.config.xml.processor.GetPassword</class-name>
 *        <init-params>
 *            <init-param>
 *                <param-name>param_1</param-name>
 *                <param-value>key</param-value>
 *            </init-param>
 *            <init-param>
 *                <param-name>param_2</param-name>
 *                <param-value>password</param-value>
 *            </init-param>
 *        </init-params>
 *    </password-provider>
 *    <password-provider id="storeMgrPass">
 *        <class-name>com.tangosol.coherence.config.xml.processor.GetPassword</class-name>
 *    </password-provider>
 *  </password-providers>
 *
 * @author spuneet
 * @since Coherence 12.3.1
 */
public class PasswordProvidersProcessorTest
    {
    @Before
    public void init()
        {
        m_deps = new DefaultClusterDependencies();

        DocumentProcessor.DefaultDependencies dependencies =
                new DocumentProcessor.DefaultDependencies(new OperationalConfigNamespaceHandler());
        dependencies.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);

        m_ctxClusterConfig = new DefaultProcessingContext(dependencies, null);

        // add the default namespace handler
        NamespaceHandler handler = dependencies.getDefaultNamespaceHandler();

        if (handler != null)
            {
            m_ctxClusterConfig.ensureNamespaceHandler("", handler);
            }

        dependencies.setResourceRegistry(new SimpleResourceRegistry());

        // add the ParameterizedBuilderRegistry as a Cookie so we can look it up
        m_ctxClusterConfig.addCookie(ParameterizedBuilderRegistry.class, m_deps.getBuilderRegistry());
        m_ctxClusterConfig.addCookie(DefaultClusterDependencies.class, m_deps);
        }

        /*
         * Tests to validate the PassowrdProvderProcessor.class
         */
    @Test
    public void whenPasswordProvidersEmptyThenReturnNull()
        {
        XmlElement xml  = PasswordProviderProcessorTest.getPasswordProviderXML(PASSWORD_PROVIDER_XML.NO_PROVIDER_ENCLOSED);

        DefaultProcessingContext ctxClusterConfig = new DefaultProcessingContext(m_ctxClusterConfig, xml);

        // process the <password-providers> definitions
        DefaultProcessingContext ctxPassProviders = new DefaultProcessingContext(ctxClusterConfig, xml);
        Object obj = ctxPassProviders.processDocument(xml);
        assertNull(obj);
        Map<String, ParameterizedBuilder<PasswordProvider>> mPassProvider = getPasswordProviderMap();
        assertNotNull(mPassProvider);
        assertEquals(0, mPassProvider.size());
        }

    @Test
    public void whenPasswordProvidersHasEmptyProviderThenReturnNull()
        {
        thrown.expect(ConfigurationException.class);
        thrown.expectMessage("The specified <password-provider>");

        XmlElement xml  = PasswordProviderProcessorTest.getPasswordProviderXML(PASSWORD_PROVIDER_XML.PROVIDER_IS_EMPTY);

        DefaultProcessingContext ctxClusterConfig = new DefaultProcessingContext(m_ctxClusterConfig, xml);

        // process the <password-providers> definitions
        DefaultProcessingContext ctxPassProviders = new DefaultProcessingContext(ctxClusterConfig, xml);
        Object obj = ctxPassProviders.processDocument(xml);
        assertNull(obj);
        Map<String, ParameterizedBuilder<PasswordProvider>> mPassProvider = getPasswordProviderMap();
        assertNotNull(mPassProvider);
        assertEquals(0, mPassProvider.size());
        }

    @Test
    public void whenPasswordProviderHasValidXMLThenCreateProviders()
        {
        XmlElement xml  = PasswordProviderProcessorTest.getPasswordProviderXML(PASSWORD_PROVIDER_XML.PROVIDERS_HAVE_REQUIRED_INFO);

        DefaultProcessingContext ctxClusterConfig = new DefaultProcessingContext(m_ctxClusterConfig, xml);

        // process the <password-providers> definitions
        DefaultProcessingContext ctxPassProviders = new DefaultProcessingContext(ctxClusterConfig, xml);
        Object obj = ctxPassProviders.processDocument(xml);
        assertNull(obj);

        // Should have 2 providers as per XMLClusterDependencies
        Map<String, ParameterizedBuilder<PasswordProvider>> mPassProvider = getPasswordProviderMap();
        assertNotNull(mPassProvider);
        assertEquals(2, mPassProvider.size());

        // Realize and validate the passwords
        PasswordProvider provider;
        assertNotNull(mPassProvider.get("trustMgrPass"));
        assertTrue(mPassProvider.get("trustMgrPass") instanceof ParameterizedBuilder);
        // Create the provider. And fetch the password.
        provider = mPassProvider.get("trustMgrPass").realize(null, null, null);
        assertArrayEquals("storepassword".toCharArray(), provider.get());

        assertNotNull(mPassProvider.get("identityMgrPass"));
        assertTrue(mPassProvider.get("identityMgrPass") instanceof ParameterizedBuilder);
        // Create the provider. And fetch the password.
        provider = mPassProvider.get("identityMgrPass").realize(null, null, null);
        assertArrayEquals("keypassword".toCharArray(), provider.get());
        }

    @Test
    public void testPasswordProviderWithOverridingParamsThroughCode()
        {
        XmlElement xml = PasswordProviderProcessorTest.getPasswordProviderXML(PASSWORD_PROVIDER_XML.PROVIDERS_HAVE_REQUIRED_INFO);

        DefaultProcessingContext ctxPasswordProviders = new DefaultProcessingContext(m_ctxClusterConfig, xml);
        ctxPasswordProviders.processDocument(xml);

        ParameterizedBuilderRegistry bldrReg = m_ctxClusterConfig.getCookie(ParameterizedBuilderRegistry.class);
        assertNotNull(bldrReg);

        // Using "id" to get the pwd-provider
        ParameterizedBuilder<PasswordProvider> builder = (ParameterizedBuilder) bldrReg.getBuilder(PasswordProvider.class, "trustMgrPass");

        PasswordProvider provider = builder.realize(null, null, null);
        assertArrayEquals("storepassword".toCharArray(), provider.get());

        // Override using ParameterList
        ParameterList listParameters = new ResolvableParameterList();
        listParameters.add(new Parameter("param_1" , "newPassword"));
        provider = builder.realize(null, null, listParameters);
        assertArrayEquals("newPassword".toCharArray(), provider.get());

        // Using "id" to get the pwd-provider
        ParameterizedBuilder<PasswordProvider> builder2 = (ParameterizedBuilder) bldrReg.getBuilder(PasswordProvider.class, "identityMgrPass");
        PasswordProvider provider2 = builder2.realize(null, null, null);
        assertArrayEquals("keypassword".toCharArray(), provider2.get());
        }

    /**
     * Process password-providers provided via a file.
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a problem with the parsing.
     */
    @Test
    public void testPasswordProvidersDefinitionsProcessingFromFile()
            throws URISyntaxException, ConfigurationException
        {
        XmlDocument xml  =
                XmlHelper.loadFileOrResource("com/tangosol/coherence/config/xml/processor/tangosol-coherence-password-providers.xml", null);

        DefaultProcessingContext ctxClusterConfig = new DefaultProcessingContext(m_ctxClusterConfig, xml);
        ctxClusterConfig.processDocument(xml);

        ParameterizedBuilderRegistry bldrReg = m_ctxClusterConfig.getCookie(ParameterizedBuilderRegistry.class);
        assertNotNull(bldrReg);

        // Using "id" to get the pwd-provider
        ParameterizedBuilder<PasswordProvider> builder = (ParameterizedBuilder) bldrReg.getBuilder(PasswordProvider.class, "trustMgrPass");

        PasswordProvider provider = builder.realize(null, null, null);
        assertArrayEquals("value_1".toCharArray(), provider.get());

        // Using "id" to get the pwd-provider
        ParameterizedBuilder<PasswordProvider> builder2 = (ParameterizedBuilder) bldrReg.getBuilder(PasswordProvider.class, "identityMgrPass");
        PasswordProvider provider2 = builder2.realize(null, null, null);
        assertArrayEquals("value_2value_3".toCharArray(), provider2.get());
        }

    // Local methods
    private Map<String, ParameterizedBuilder<PasswordProvider>> getPasswordProviderMap()
        {
        Map<String, ParameterizedBuilder<PasswordProvider>> mPassProvider = new LiteMap<>();

        for (ParameterizedBuilderRegistry.Registration r : m_deps.getBuilderRegistry())
            {
            if (r.getInstanceClass().isAssignableFrom(PasswordProvider.class))
                {
                mPassProvider.put(r.getName(), r.getBuilder());
                }
            }
        return mPassProvider;
        }

    // Local params
    private DefaultClusterDependencies m_deps;
    private DefaultProcessingContext   m_ctxClusterConfig;

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    }