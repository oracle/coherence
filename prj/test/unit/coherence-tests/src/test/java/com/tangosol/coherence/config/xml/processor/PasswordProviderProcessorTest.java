/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.oracle.coherence.common.net.SSLSettings;
import com.oracle.coherence.common.net.SSLSocketProvider;
import com.oracle.coherence.common.net.SocketProvider;
import com.oracle.coherence.common.net.TcpSocketProvider;
import com.tangosol.coherence.config.ParameterMacroExpressionParser;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.coherence.config.xml.OperationalConfigNamespaceHandler;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.xml.DefaultProcessingContext;
import com.tangosol.config.xml.DocumentProcessor;
import com.tangosol.config.xml.NamespaceHandler;
import com.tangosol.internal.net.cluster.DefaultClusterDependencies;
import com.tangosol.net.PasswordProvider;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.util.AssertionException;
import com.tangosol.util.LiteMap;
import com.tangosol.util.SimpleResourceRegistry;
import com.tangosol.util.WrapperException;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Before;
import org.junit.rules.ExpectedException;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit test of the {@link PasswordProviderBuilderProcessor} class.
 *
 * @author spuneet
 * @since Coherence 12.3.1
 */
public class PasswordProviderProcessorTest
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

    static XmlElement getPasswordProviderXML (PASSWORD_PROVIDER_XML type)
        {
        String retStr = "";
        String sClassName = "com.tangosol.coherence.config.xml.processor.GetPassword";

        switch (type)
            {
            case NO_PROVIDER_ENCLOSED:
                retStr ="<password-providers>" +
                        "</password-providers>";
                break;

            case PROVIDER_IS_EMPTY:
                retStr ="<password-providers>" +
                        "  <password-provider/>" +
                        "</password-providers>";
                break;

            case PROVIDER_HAS_NO_CLASS_NAME:
                retStr ="<password-providers>" +
                        "  <password-provider id=\"myPwdProvider\">" +
                        "  </password-provider>" +
                        "</password-providers>";
                break;

            case PROVIDER_HAS_EMPTY_CLASS_NAME:
                retStr ="<password-providers>" +
                        "  <password-provider id=\"myPwdProvider\">" +
                        "    <class-name/>" +
                        "  </password-provider>" +
                        "</password-providers>";
                break;

            case PROVIDER_HAS_NO_ID:
                retStr ="<password-providers>" +
                        "  <password-provider>" +
                        "    <class-name>" + sClassName + "</class-name>" +
                        "  </password-provider>" +
                        "</password-providers>";
                break;

            case PROVIDER_HAS_STRING_CLASS:
                retStr ="<password-providers>" +
                        "  <password-provider id=\"trustMgrPass\">" +
                        "    <class-name>com.tangosol.coherence.config.xml.processor.PasswordProviderBuilderProcessor$DefaultPasswordProvider</class-name>" +
                        "    <init-params>" +
                        "      <init-param>" +
                        "        <param-name>param_1</param-name>" +
                        "        <param-value>storepassword</param-value>" +
                        "      </init-param>" +
                        "    </init-params>" +
                        "  </password-provider>" +
                        "  <password-provider id=\"identityMgrPass\">" +
                        "    <class-name>com.tangosol.coherence.config.xml.processor.PasswordProviderBuilderProcessor$DefaultPasswordProvider</class-name>" +
                        "    <init-params>" +
                        "      <init-param>" +
                        "         <param-name>param_1</param-name>" +
                        "         <param-value>keypassword</param-value>" +
                        "       </init-param>" +
                        "    </init-params>" +
                        "  </password-provider>" +
                        "</password-providers>";
                break;

            case PROVIDER_HAS_PARAMS_BUT_NO_ID:
                retStr ="<password-providers>" +
                        "  <password-provider>" +
                        "    <class-name>" + sClassName + "</class-name>" +
                        "    <init-params>" +
                        "      <init-param>" +
                        "        <param-name>param_1</param-name>" +
                        "        <param-value>storepassword</param-value>" +
                        "      </init-param>" +
                        "    </init-params>" +
                        "  </password-provider>" +
                        "</password-providers>";
                break;

            case PROVIDER_HAS_NO_INIT_PARAMS:
                retStr ="<password-providers>" +
                        "  <password-provider id=\"myPwdProvider\">" +
                        "    <class-name>" + sClassName + "</class-name>" +
                        "  </password-provider>" +
                        "</password-providers>";
                break;

            case PROVIDER_HAS_NO_INIT_PARAM:
                retStr ="<password-providers>" +
                        "  <password-provider id=\"myPwdProvider\">" +
                        "    <class-name>" + sClassName + "</class-name>" +
                        "    <init-params/>" +
                        "  </password-provider>" +
                        "</password-providers>";
                break;

            case PROVIDERS_HAVE_REQUIRED_INFO:
                retStr ="<password-providers>" +
                        "  <password-provider id=\"trustMgrPass\">" +
                        "    <class-name>" + sClassName + "</class-name>" +
                        "    <init-params>" +
                        "      <init-param>" +
                        "        <param-name>param_1</param-name>" +
                        "        <param-value>storepassword</param-value>" +
                        "      </init-param>" +
                        "    </init-params>" +
                        "  </password-provider>" +
                        "  <password-provider id=\"identityMgrPass\">" +
                        "    <class-name>" + sClassName + "</class-name>" +
                        "    <init-params>" +
                        "      <init-param>" +
                        "         <param-name>param_2</param-name>" +
                        "         <param-value>keypassword</param-value>" +
                        "       </init-param>" +
                        "    </init-params>" +
                        "  </password-provider>" +
                        "</password-providers>";
                break;

            case PROVIDER_HAS_REQUIRED_INFO:
                retStr ="  <password-provider id=\"myPwdProvider\">" +
                        "    <class-name>" + sClassName + "</class-name>" +
                        "    <init-params>" +
                        "      <init-param>" +
                        "        <param-name>param_1</param-name>" +
                        "        <param-value>store</param-value>" +
                        "      </init-param>" +
                        "      <init-param>" +
                        "        <param-name>param_2</param-name>" +
                        "        <param-value>password</param-value>" +
                        "      </init-param>" +
                        "    </init-params>" +
                        "  </password-provider>";
                break;

            default:
            }
        return XmlHelper.loadXml(retStr);
        }

    public XmlElement getSocketProviderXml (SSL_PASSWORD_TYPE type)
        {
        StringBuffer sTrustMgrStrBuf    = new StringBuffer();
        StringBuffer sIdentityMgrStrBuf = new StringBuffer();
        String sTrustMgrPassword        = "storepassword";
        String sIdentityMgrPasword      = "keypassword";

        switch (type)
            {
            case USE_PASSWORD:
                sTrustMgrStrBuf.append("<password>").append(sTrustMgrPassword).append("</password>");
                sIdentityMgrStrBuf.append("<password>").append(sIdentityMgrPasword).append("</password>");
                break;

            case USE_PASSWORD_WITH_INCORRECT_VALUE:
                sTrustMgrStrBuf.append("<password>").append(sTrustMgrPassword).append("XYZ").append("</password>");
                sIdentityMgrStrBuf.append("<password>").append(sIdentityMgrPasword).append("XYZ").append("</password>");
                break;

            case USE_PASSWORD_PROVIDER_INLINE:
                sTrustMgrStrBuf.append("<password-provider>").
                        append("<class-name>com.tangosol.coherence.config.xml.processor.GetPassword</class-name>").
                        append("<init-params>").
                        append("<init-param>").
                        append("<param-name>param_1</param-name>").
                        append("<param-value>").append(sTrustMgrPassword).append("</param-value >").
                        append("</init-param>").
                        append("</init-params>").
                        append("</password-provider>");
                sIdentityMgrStrBuf.append("<password-provider>").
                        append("<class-name>com.tangosol.coherence.config.xml.processor.GetPassword</class-name>").
                        append("<init-params>").
                        append("<init-param>").
                        append("<param-name>param_2</param-name>").
                        append("<param-value>").append(sIdentityMgrPasword).append("</param-value >").
                        append("</init-param>").
                        append("</init-params>").
                        append("</password-provider>");
                break;

            case USE_NO_PASSWORD_PROVIDER_OR_PASSWORD:
                sTrustMgrStrBuf.append("");
                sIdentityMgrStrBuf.append("<password>").append(sIdentityMgrPasword).append("</password>");
                break;

            case USE_PASSWORD_PROVIDER_WITH_DEFAULTS:
                sTrustMgrStrBuf.append("<password-provider>").
                        append("<name>trustMgrPass</name>").
                        append("</password-provider>");
                sIdentityMgrStrBuf.append("<password-provider>").
                        append("<name>identityMgrPass</name>").
                        append("</password-provider>");
                break;

            case USE_PASSWORD_PROVIDER_WITH_DEFAULTS_WITH_INVALID_ID:
                sTrustMgrStrBuf.append("<password-provider>").
                        append("<name>trustMgrPass_INVALID</name>").
                        append("</password-provider>");
                sIdentityMgrStrBuf.append("<password-provider>").
                        append("<name>identityMgrPass</name>").
                        append("</password-provider>");
                break;

                case USE_PASSWORD_PROVIDER_WITH_BLANK_ID:
                    sTrustMgrStrBuf.append("<password-provider>").
                            append("<name></name>").
                            append("</password-provider>");
                    sIdentityMgrStrBuf.append("<password>").append(sIdentityMgrPasword).append("</password>");
                    break;

            case USE_PASSWORD_PROVIDER_WITH_OVERRIDES:
                sTrustMgrStrBuf.append("<password-provider>").
                        append("<name>trustMgrPass</name>").
                        append("<init-params>").
                        append("<init-param>").
                        append("<param-name>param_1</param-name>").
                        append("<param-value>").append(sTrustMgrPassword).append("</param-value >").
                        append("</init-param>").
                        append("</init-params>").
                        append("</password-provider>");
                sIdentityMgrStrBuf.append("<password-provider>").
                        append("<name>identityMgrPass</name>").
                        append("<init-params>").
                        append("<init-param>").
                        append("<param-name>param_2</param-name>").
                        append("<param-value>").append(sIdentityMgrPasword).append("</param-value >").
                        append("</init-param>").
                        append("</init-params>").
                        append("</password-provider>");
                break;

            case USE_PASSWORD_PROVIDER_WITH_OVERRIDES_WITH_INCORRECT_VALUE:
                sTrustMgrStrBuf.append("<password-provider>").
                        append("<name>trustMgrPass</name>").
                        append("<init-params>").
                        append("<init-param>").
                        append("<param-name>param_1</param-name>").
                        append("<param-value>").append(sTrustMgrPassword).append("XYZ").append("</param-value >").
                        append("</init-param>").
                        append("</init-params>").
                        append("</password-provider>");
                sIdentityMgrStrBuf.append("<password-provider>").
                        append("<name>identityMgrPass</name>").
                        append("<init-params>").
                        append("<init-param>").
                        append("<param-name>param_2</param-name>").
                        append("<param-value>").append(sIdentityMgrPasword).append("XYZ").append("</param-value >").
                        append("</init-param>").
                        append("</init-params>").
                        append("</password-provider>");
                break;

            case USE_PASSWORD_PROVIDER_WITH_MULTIPLE_PARAMS:
                sTrustMgrStrBuf.
                        append("<password-provider>").
                        append("  <name>trustMgrPass</name>").
                        append("  <init-params>").
                        append("    <init-param>").
                        append("      <param-name>param_1</param-name>").
                        append("      <param-value>sto</param-value>").
                        append("    </init-param>").
                        append("    <init-param>").
                        append("      <param-name>param_2</param-name>").
                        append("      <param-value>repass</param-value>").
                        append("    </init-param>").
                        append("    <init-param>").
                        append("      <param-name>param_3</param-name>").
                        append("      <param-value>word</param-value>").
                        append("    </init-param>").
                        append("  </init-params>").
                        append("</password-provider>");

                sIdentityMgrStrBuf.
                        append("<password-provider>").
                        append("  <name>identityMgrPass</name>").
                        append("  <init-params>").
                        append("    <init-param>").
                        append("      <param-name>param_2</param-name>").
                        append("      <param-value>key</param-value>").
                        append("    </init-param>").
                        append("    <init-param>").
                        append("      <param-name>param_4</param-name>").
                        append("      <param-value>password</param-value>").
                        append("    </init-param>").
                        append("  </init-params>").
                        append("</password-provider>");
                break;

            case USE_PASSWORD_PROVIDER_WITH_MULTIPLE_PARAMS_WITH_INCORRECT_VALUE:
                sTrustMgrStrBuf.
                        append("<password-provider>").
                        append("  <name>trustMgrPass</name>").
                        append("  <init-params>").
                        append("    <init-param>").
                        append("      <param-name>param_1</param-name>").
                        append("      <param-value>sto</param-value>").
                        append("    </init-param>").
                        append("    <init-param>").
                        append("      <param-name>param_2</param-name>").
                        append("      <param-value>repass</param-value>").
                        append("    </init-param>").
                        append("    <init-param>").
                        append("      <param-name>param_3</param-name>").
                        append("      <param-value>word_XYZ</param-value>").
                        append("    </init-param>").
                        append("  </init-params>").
                        append("</password-provider>");

                sIdentityMgrStrBuf.
                        append("<password-provider>").
                        append("  <name>identityMgrPass</name>").
                        append("  <init-params>").
                        append("    <init-param>").
                        append("      <param-name>param_2</param-name>").
                        append("      <param-value>key</param-value>").
                        append("    </init-param>").
                        append("    <init-param>").
                        append("      <param-name>param_4</param-name>").
                        append("      <param-value>password_XYZ</param-value>").
                        append("    </init-param>").
                        append("  </init-params>").
                        append("</password-provider>");
                break;

            case USE_PASSWORD_PROVIDER_WITH_ADDITIONAL_PARAMS:
                sTrustMgrStrBuf.    // 4 parameters passed (invalid, constructor not available)
                        append("<password-provider>").
                        append("  <name>trustMgrPass</name>").
                        append("  <init-params>").
                        append("    <init-param>").
                        append("      <param-name>param_1</param-name>").
                        append("      <param-value>sto</param-value>").
                        append("    </init-param>").
                        append("    <init-param>").
                        append("      <param-name>param_2</param-name>").
                        append("      <param-value>repass</param-value>").
                        append("    </init-param>").
                        append("    <init-param>").
                        append("      <param-name>param_3</param-name>").
                        append("      <param-value>word</param-value>").
                        append("    </init-param>").
                        append("    <init-param>").
                        append("      <param-name>param_4</param-name>").
                        append("      <param-value>XYZ</param-value>").
                        append("    </init-param>").
                        append("  </init-params>").
                        append("</password-provider>");

                sIdentityMgrStrBuf.
                        append("<password-provider>").
                        append("  <name>identityMgrPass</name>").
                        append("  <init-params>").
                        append("    <init-param>").
                        append("      <param-name>param_2</param-name>").
                        append("      <param-value>key</param-value>").
                        append("    </init-param>").
                        append("    <init-param>").
                        append("      <param-name>param_3</param-name>").
                        append("      <param-value>password</param-value>").
                        append("    </init-param>").
                        append("    <init-param>").
                        append("      <param-name>param_4</param-name>").
                        append("      <param-value>XYZ</param-value>").
                        append("    </init-param>").
                        append("  </init-params>").
                        append("</password-provider>");
                break;
            default:
            }

        XmlElement xml = XmlHelper.loadXml(
            "<socket-provider>" +
            " <ssl>" +
            "  <protocol>TLS</protocol>" +
            "  <identity-manager>" +
            "    <algorithm>SunX509</algorithm>" +
            "    <key-store>" +
            "      <url>file:internal/testkeystore.jks</url>" +
            sTrustMgrStrBuf.toString() +
            "      <type>JKS</type>" +
            "    </key-store>" +
            sIdentityMgrStrBuf.toString() +
            "  </identity-manager>" +
            "  <trust-manager>" +
            "    <algorithm>SunX509</algorithm>" +
            "    <key-store>" +
            "      <url>file:internal/testkeystore.jks</url>" +
            sTrustMgrStrBuf.toString() +
            "    </key-store>" +
            "  </trust-manager>" +
            "  <socket-provider>tcp</socket-provider>" +
            " </ssl>" +
            "</socket-provider>");
        return xml;
        }

    /*
     * Adding tests for <password-provider>
     */

    @Test
    public void whenPasswordProviderHasNoIDThenCreateProvider()
        {
        XmlElement xml =
                getPasswordProviderXML(PASSWORD_PROVIDER_XML.PROVIDER_HAS_NO_ID);

        DefaultProcessingContext ctxClusterConfig = new DefaultProcessingContext(m_ctxClusterConfig, xml);

        // process the <password-providers> definitions
        DefaultProcessingContext ctxPassProviders = new DefaultProcessingContext(ctxClusterConfig, xml);
        Object obj = ctxPassProviders.processDocument(xml);
        assertNull(obj);
        Map<String, ParameterizedBuilder<PasswordProvider>> mPassProvider = getPasswordProviderMap();
        assertNotNull(mPassProvider);
        assertEquals(1, mPassProvider.size());

        // Only one PasswordProvider is registered, but the key is unknown, hence the loop.
        for (ParameterizedBuilder<PasswordProvider> passwordProviderBldr : mPassProvider.values())
            {
            PasswordProvider bldr = passwordProviderBldr.realize(null,null,null);
            assertNotNull(bldr);

            char[] password = bldr.get();
            assertNotNull(password);
            assertArrayEquals(new char[]{}, password);
            }
        }

    @Test
    public void whenEmptyProviderShouldReturnNullBuilder()
        {

        XmlElement xml = XmlHelper.loadXml("<password-provider/>");

        DefaultProcessingContext ctxPasswordProviders = new DefaultProcessingContext(m_ctxClusterConfig, xml);
        ParameterizedBuilder<PasswordProvider> builder =
                (ParameterizedBuilder<PasswordProvider>) ctxPasswordProviders.processDocument(xml);
        assertNull(builder);

        Map<String, ParameterizedBuilder<PasswordProvider>> mPassProvider = getPasswordProviderMap();
        assertNotNull(mPassProvider);
        assertEquals(0, mPassProvider.size());
        }

    @Test
    public void whenPasswordProviderHasNoClassNameThenThrowEx()
        {
        thrown.expect(ConfigurationException.class);
        thrown.expectMessage("The specified <password-provider>");

        XmlElement xml =
                getPasswordProviderXML(PASSWORD_PROVIDER_XML.PROVIDER_HAS_NO_CLASS_NAME);

        DefaultProcessingContext ctxClusterConfig = new DefaultProcessingContext(m_ctxClusterConfig, xml);

        // process the <password-providers> definitions
        DefaultProcessingContext ctxPassProviders = new DefaultProcessingContext(ctxClusterConfig, xml);
        Object obj = ctxPassProviders.processDocument(xml);
        assertNull(obj);
        }

    @Test
    public void whenPasswordProviderHasEmptyClassNameThenCreateInvalidProvider()
        {
        thrown.expect(WrapperException.class); // Original Exception throw is : ClassNotFoundException.class
        thrown.expectMessage("undefined");

        XmlElement xml =
                getPasswordProviderXML(PASSWORD_PROVIDER_XML.PROVIDER_HAS_EMPTY_CLASS_NAME);

        DefaultProcessingContext ctxClusterConfig = new DefaultProcessingContext(m_ctxClusterConfig, xml);

        // process the <password-providers> definitions
        DefaultProcessingContext ctxPassProviders = new DefaultProcessingContext(ctxClusterConfig, xml);
        Object obj = ctxPassProviders.processDocument(xml);
        assertNull(obj);

        Map<String, ParameterizedBuilder<PasswordProvider>> mPassProvider = getPasswordProviderMap();
        assertNotNull(mPassProvider);
        assertEquals(1, mPassProvider.size());

        // The builder is created with no class-name; so it fails at runtime while instantiation.
        mPassProvider.get("myPwdProvider").realize(null, null, null);
        }

    @Test
    public void whenPasswordProviderHasNoInitParamsThenCreateProvider()
        {
        XmlElement xml =
                getPasswordProviderXML(PASSWORD_PROVIDER_XML.PROVIDER_HAS_NO_INIT_PARAMS);

        DefaultProcessingContext ctxClusterConfig = new DefaultProcessingContext(m_ctxClusterConfig, xml);

        // process the <password-providers> definitions
        DefaultProcessingContext ctxPassProviders = new DefaultProcessingContext(ctxClusterConfig, xml);
        Object obj = ctxPassProviders.processDocument(xml);
        assertNull(obj);

        Map<String, ParameterizedBuilder<PasswordProvider>> mPassProvider = getPasswordProviderMap();
        assertNotNull(mPassProvider);
        assertEquals(1, mPassProvider.size());

        // The builder is created with no init-params; so it should get invoked and return a password.
        // In our implementation it will return a 0-sized char-array
        PasswordProvider bldr = mPassProvider.get("myPwdProvider").realize(null, null, null);
        assertNotNull(bldr);

        char[] password = bldr.get();
        assertNotNull(password);
        assertArrayEquals(new char[]{}, password);
        }

    @Test
    public void whenPasswordProviderHasNoInitParamThenCreateProvider()
        {
        XmlElement xml =
                getPasswordProviderXML(PASSWORD_PROVIDER_XML.PROVIDER_HAS_NO_INIT_PARAM);

        DefaultProcessingContext ctxClusterConfig = new DefaultProcessingContext(m_ctxClusterConfig, xml);

        // process the <password-providers> definitions
        DefaultProcessingContext ctxPassProviders = new DefaultProcessingContext(ctxClusterConfig, xml);
        Object obj = ctxPassProviders.processDocument(xml);
        assertNull(obj);

        Map<String, ParameterizedBuilder<PasswordProvider>> mPassProvider = getPasswordProviderMap();
        assertNotNull(mPassProvider);
        assertEquals(1, mPassProvider.size());

        // The builder is created with no init-param block; so it should get invoked and return a password.
        // In our implementation it will return a 0-sized char-array
        PasswordProvider bldr = mPassProvider.get("myPwdProvider").realize(null, null, null);
        assertNotNull(bldr);

        char[] password = bldr.get();
        assertNotNull(password);
        assertArrayEquals(new char[]{}, password);
        }

    @Test
    public void whenPasswordProviderHasInitParamThenCreateProvider()
        {
        XmlElement xml =
                getPasswordProviderXML(PASSWORD_PROVIDER_XML.PROVIDER_HAS_REQUIRED_INFO);

        DefaultProcessingContext ctxClusterConfig = new DefaultProcessingContext(m_ctxClusterConfig, xml);

        // process the <password-provider> definitions
        DefaultProcessingContext ctxPassProviders = new DefaultProcessingContext(ctxClusterConfig, xml);
        ParameterizedBuilder<PasswordProvider> bldr = (ParameterizedBuilder<PasswordProvider>) ctxPassProviders.processDocument(xml);
        assertNotNull(bldr);

        // Realize the provider to get the password
        PasswordProvider provider = bldr.realize(null,null,null);
        assertNotNull(provider);

        // Validate the password
        char[] password = provider.get();
        assertNotNull(password);
        assertArrayEquals("storepassword".toCharArray(), password);
        }

    // ======================================================================== //
    //  Involving SSL Config to verify the PasswordProviders functionality
    // ======================================================================== //

    @Test
    public void testSSLConfigWithDefaults_NoPasswordProviderUsed()
        {
        XmlElement sslXml = getSocketProviderXml(SSL_PASSWORD_TYPE.USE_PASSWORD);
        DefaultProcessingContext ctxPasswordProviders = new DefaultProcessingContext(m_ctxClusterConfig, sslXml);

        // SSL Config Load
        SocketProviderBuilder sock_builder = (SocketProviderBuilder) ctxPasswordProviders.processDocument(sslXml);
        assertNotNull(sock_builder);
        assertTrue(SocketProviderBuilder.UNNAMED_PROVIDER_ID.equals(sock_builder.getId()));
        SSLSettings sslSettings = sock_builder.getSSLSettings();
        SocketProvider sockProvider = sock_builder.realize(new NullParameterResolver(), null, null);
        SocketProvider sockDelegate = ((SSLSocketProvider) sockProvider).getDependencies().getDelegateSocketProvider();
        assertTrue(sockDelegate.getDelegate() instanceof TcpSocketProvider);
        }

    @Test
    public void testPasswordProvidersWithDefaultsWithSSLConfig()
        {
        XmlElement xml = getPasswordProviderXML(PASSWORD_PROVIDER_XML.PROVIDERS_HAVE_REQUIRED_INFO);
        // Password Provider Config Load
        DefaultProcessingContext ctxPasswordProviders = new DefaultProcessingContext(m_ctxClusterConfig, xml);
        ctxPasswordProviders.processDocument(xml);

        ParameterizedBuilderRegistry bldrReg = m_ctxClusterConfig.getCookie(ParameterizedBuilderRegistry.class);
        assertNotNull(bldrReg);

        // Using "id" to get the pwd-provider
        ParameterizedBuilder<PasswordProvider> builder = (ParameterizedBuilder) bldrReg.getBuilder(PasswordProvider.class, "trustMgrPass");

        PasswordProvider provider = builder.realize(null, null, null);
        assertArrayEquals("storepassword".toCharArray(), provider.get());

        // Using "id" to get the pwd-provider
        ParameterizedBuilder<PasswordProvider> builder2 = (ParameterizedBuilder) bldrReg.getBuilder(PasswordProvider.class, "identityMgrPass");
        PasswordProvider provider2 = builder2.realize(null, null, null);
        assertArrayEquals("keypassword".toCharArray(), provider2.get());

        // SSL Config Load
        XmlElement sslXml = getSocketProviderXml(SSL_PASSWORD_TYPE.USE_PASSWORD_PROVIDER_WITH_DEFAULTS);
        SocketProviderBuilder sock_builder = (SocketProviderBuilder) ctxPasswordProviders.processDocument(sslXml);
        assertNotNull(sock_builder);
        assertTrue(SocketProviderBuilder.UNNAMED_PROVIDER_ID.equals(sock_builder.getId()));
        SSLSettings sslSettings = sock_builder.getSSLSettings();
        SocketProvider sockProvider = sock_builder.realize(new NullParameterResolver(), null, null);
        SocketProvider sockDelegate = ((SSLSocketProvider) sockProvider).getDependencies().getDelegateSocketProvider();
        assertTrue(sockDelegate.getDelegate() instanceof TcpSocketProvider);
        }

    @Test
    public void testNullKeyStorePasswordProvidersWithInSSLConfig()
        {
        // SSL Config Load
        XmlElement sslXml = getSocketProviderXml(SSL_PASSWORD_TYPE.USE_NO_PASSWORD_PROVIDER_OR_PASSWORD);
        DefaultProcessingContext ctxSocketProviders = new DefaultProcessingContext(m_ctxClusterConfig, sslXml);
        SocketProviderBuilder sock_builder = (SocketProviderBuilder) ctxSocketProviders.processDocument(sslXml);
        assertNotNull(sock_builder);
        assertThat(sock_builder.getId(), is(SocketProviderBuilder.UNNAMED_PROVIDER_ID));
        SSLSettings sslSettings = sock_builder.getSSLSettings();
        SocketProvider sockProvider = sock_builder.realize(new NullParameterResolver(), null, null);
        SocketProvider sockDelegate = ((SSLSocketProvider) sockProvider).getDependencies().getDelegateSocketProvider();
        assertTrue(sockDelegate.getDelegate() instanceof TcpSocketProvider);
        }

    @Test
    public void whenPasswordProvidersWithDefaultsWithSSLConfigUsingInvalidLookupIDThenThrowEx()
        {
        thrown.expect(ConfigurationException.class);
        thrown.expectMessage("<password-provider> fails to correctly define a PasswordProvider implementation: <password-provider>");

        XmlElement xml = getPasswordProviderXML(PASSWORD_PROVIDER_XML.PROVIDERS_HAVE_REQUIRED_INFO);
        // Password Provider Config Load
        DefaultProcessingContext ctxPasswordProviders = new DefaultProcessingContext(m_ctxClusterConfig, xml);
        ctxPasswordProviders.processDocument(xml);

        ParameterizedBuilderRegistry bldrReg = m_ctxClusterConfig.getCookie(ParameterizedBuilderRegistry.class);
        assertNotNull(bldrReg);

        // Using "id" to get the pwd-provider
        ParameterizedBuilder<PasswordProvider> builder = (ParameterizedBuilder) bldrReg.getBuilder(PasswordProvider.class, "trustMgrPass");

        PasswordProvider provider = builder.realize(null, null, null);
        assertArrayEquals("storepassword".toCharArray(), provider.get());

        // Using "id" to get the pwd-provider
        ParameterizedBuilder<PasswordProvider> builder2 = (ParameterizedBuilder) bldrReg.getBuilder(PasswordProvider.class, "identityMgrPass");
        PasswordProvider provider2 = builder2.realize(null, null, null);
        assertArrayEquals("keypassword".toCharArray(), provider2.get());

        // SSL Config Load
        XmlElement sslXml = getSocketProviderXml(SSL_PASSWORD_TYPE.USE_PASSWORD_PROVIDER_WITH_DEFAULTS_WITH_INVALID_ID);
        SocketProviderBuilder sock_builder = (SocketProviderBuilder) ctxPasswordProviders.processDocument(sslXml);
        assertNotNull(sock_builder);
        }

    @Test
    public void testPasswordProvidersWithSSLConfigUsingOverrides()
        {
        XmlElement xml = getPasswordProviderXML(PASSWORD_PROVIDER_XML.PROVIDERS_HAVE_REQUIRED_INFO);
        // Password Provider Config Load
        DefaultProcessingContext ctxPasswordProviders = new DefaultProcessingContext(m_ctxClusterConfig, xml);
        ctxPasswordProviders.processDocument(xml);

        ParameterizedBuilderRegistry bldrReg = m_ctxClusterConfig.getCookie(ParameterizedBuilderRegistry.class);
        assertNotNull(bldrReg);

        // Using "id" to get the pwd-provider
        ParameterizedBuilder<PasswordProvider> builder = (ParameterizedBuilder) bldrReg.getBuilder(PasswordProvider.class, "trustMgrPass");

        PasswordProvider provider = builder.realize(null, null, null);
        assertArrayEquals("storepassword".toCharArray(), provider.get());

        // Using "id" to get the pwd-provider
        ParameterizedBuilder<PasswordProvider> builder2 = (ParameterizedBuilder) bldrReg.getBuilder(PasswordProvider.class, "identityMgrPass");
        PasswordProvider provider2 = builder2.realize(null, null, null);
        assertArrayEquals("keypassword".toCharArray(), provider2.get());

        // SSL Config Load
        XmlElement sslXml = getSocketProviderXml(SSL_PASSWORD_TYPE.USE_PASSWORD_PROVIDER_WITH_OVERRIDES);
        SocketProviderBuilder sock_builder = (SocketProviderBuilder) ctxPasswordProviders.processDocument(sslXml);
        assertNotNull(sock_builder);
        assertTrue(SocketProviderBuilder.UNNAMED_PROVIDER_ID.equals(sock_builder.getId()));
        SSLSettings sslSettings = sock_builder.getSSLSettings();
        SocketProvider sockProvider = sock_builder.realize(new NullParameterResolver(), null, null);
        SocketProvider sockDelegate = ((SSLSocketProvider) sockProvider).getDependencies().getDelegateSocketProvider();
        assertTrue(sockDelegate.getDelegate() instanceof TcpSocketProvider);
        }

    @Test
    public void testPasswordProvidersWithSSLConfigUsingMultiParams()
        {
        XmlElement xml = getPasswordProviderXML(PASSWORD_PROVIDER_XML.PROVIDERS_HAVE_REQUIRED_INFO);
        // Password Provider Config Load
        DefaultProcessingContext ctxPasswordProviders = new DefaultProcessingContext(m_ctxClusterConfig, xml);
        ctxPasswordProviders.processDocument(xml);

        ParameterizedBuilderRegistry bldrReg = m_ctxClusterConfig.getCookie(ParameterizedBuilderRegistry.class);
        assertNotNull(bldrReg);

        // Using "id" to get the pwd-provider
        ParameterizedBuilder<PasswordProvider> builder = (ParameterizedBuilder) bldrReg.getBuilder(PasswordProvider.class, "trustMgrPass");

        PasswordProvider provider = builder.realize(null, null, null);
        assertArrayEquals("storepassword".toCharArray(), provider.get());

        // Using "id" to get the pwd-provider
        ParameterizedBuilder<PasswordProvider> builder2 = (ParameterizedBuilder) bldrReg.getBuilder(PasswordProvider.class, "identityMgrPass");
        PasswordProvider provider2 = builder2.realize(null, null, null);
        assertArrayEquals("keypassword".toCharArray(), provider2.get());

        // SSL Config Load
        XmlElement sslXml = getSocketProviderXml(SSL_PASSWORD_TYPE.USE_PASSWORD_PROVIDER_WITH_MULTIPLE_PARAMS);
        SocketProviderBuilder sock_builder = (SocketProviderBuilder) ctxPasswordProviders.processDocument(sslXml);
        assertNotNull(sock_builder);
        assertTrue(SocketProviderBuilder.UNNAMED_PROVIDER_ID.equals(sock_builder.getId()));
        SSLSettings sslSettings = sock_builder.getSSLSettings();
        SocketProvider sockProvider = sock_builder.realize(new NullParameterResolver(), null, null);
        SocketProvider sockDelegate = ((SSLSocketProvider) sockProvider).getDependencies().getDelegateSocketProvider();
        assertTrue(sockDelegate.getDelegate() instanceof TcpSocketProvider);
        }

    @Test
    public void whenPasswordProvidersWithSSLConfigProvidesInvalidPassTheThrowEx()
        {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            {
            XmlElement xml = getPasswordProviderXML(PASSWORD_PROVIDER_XML.PROVIDERS_HAVE_REQUIRED_INFO);
            // Password Provider Config Load
            DefaultProcessingContext ctxPasswordProviders = new DefaultProcessingContext(m_ctxClusterConfig, xml);
            ctxPasswordProviders.processDocument(xml);

            ParameterizedBuilderRegistry bldrReg = m_ctxClusterConfig.getCookie(ParameterizedBuilderRegistry.class);
            assertNotNull(bldrReg);

            // Using "id" to get the pwd-provider
            ParameterizedBuilder<PasswordProvider> builder = (ParameterizedBuilder) bldrReg.getBuilder(PasswordProvider.class, "trustMgrPass");

            PasswordProvider provider = builder.realize(null, null, null);
            assertArrayEquals("storepassword".toCharArray(), provider.get());

            // Using "id" to get the pwd-provider
            ParameterizedBuilder<PasswordProvider> builder2 = (ParameterizedBuilder) bldrReg.getBuilder(PasswordProvider.class, "identityMgrPass");
            PasswordProvider provider2 = builder2.realize(null, null, null);
            assertArrayEquals("keypassword".toCharArray(), provider2.get());

            // SSL Config Load
            XmlElement sslXml = getSocketProviderXml(SSL_PASSWORD_TYPE.USE_PASSWORD_WITH_INCORRECT_VALUE);
            SocketProviderBuilder sock_builder = (SocketProviderBuilder) ctxPasswordProviders.processDocument(sslXml);
            assertNotNull(sock_builder);
            assertTrue(SocketProviderBuilder.UNNAMED_PROVIDER_ID.equals(sock_builder.getId()));
            SSLSettings sslSettings = sock_builder.getSSLSettings();
            });

        assertCauseMessage(ex, is("Password verification failed"));
        }

    @Test
    public void whenPasswordProvidersWithSSLConfigUsingOverridesProvidesInvalidPassThenThrowEx()
        {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            {
            XmlElement xml = getPasswordProviderXML(PASSWORD_PROVIDER_XML.PROVIDERS_HAVE_REQUIRED_INFO);
            // Password Provider Config Load
            DefaultProcessingContext ctxPasswordProviders = new DefaultProcessingContext(m_ctxClusterConfig, xml);
            ctxPasswordProviders.processDocument(xml);

            ParameterizedBuilderRegistry bldrReg = m_ctxClusterConfig.getCookie(ParameterizedBuilderRegistry.class);
            assertNotNull(bldrReg);

            // Using "id" to get the pwd-provider
            ParameterizedBuilder<PasswordProvider> builder = (ParameterizedBuilder) bldrReg.getBuilder(PasswordProvider.class, "trustMgrPass");

            PasswordProvider provider = builder.realize(null, null, null);
            assertArrayEquals("storepassword".toCharArray(), provider.get());

            // Using "id" to get the pwd-provider
            ParameterizedBuilder<PasswordProvider> builder2 = (ParameterizedBuilder) bldrReg.getBuilder(PasswordProvider.class, "identityMgrPass");
            PasswordProvider provider2 = builder2.realize(null, null, null);
            assertArrayEquals("keypassword".toCharArray(), provider2.get());

            // SSL Config Load
            XmlElement sslXml = getSocketProviderXml(SSL_PASSWORD_TYPE.USE_PASSWORD_PROVIDER_WITH_OVERRIDES_WITH_INCORRECT_VALUE);
            //        SocketProvider socketProvider = SSLSocketProviderBuilderHelper.loadSocketProvider(sslXml);

            SocketProviderBuilder sock_builder = (SocketProviderBuilder) ctxPasswordProviders.processDocument(sslXml);
            assertNotNull(sock_builder);
            assertTrue(SocketProviderBuilder.UNNAMED_PROVIDER_ID.equals(sock_builder.getId()));
            sock_builder.getSSLSettings(); // This line results in exception
            });

        assertCauseMessage(ex, is("Password verification failed"));
        }

    @Test
    public void whenPasswordProvidersWithSSLConfigUsingMultiParamsHavingInvalidPassThenThrowEx()
        {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            {
            XmlElement xml = getPasswordProviderXML(PASSWORD_PROVIDER_XML.PROVIDERS_HAVE_REQUIRED_INFO);
            // Password Provider Config Load
            DefaultProcessingContext ctxPasswordProviders = new DefaultProcessingContext(m_ctxClusterConfig, xml);
            ctxPasswordProviders.processDocument(xml);

            ParameterizedBuilderRegistry bldrReg = m_ctxClusterConfig.getCookie(ParameterizedBuilderRegistry.class);
            assertNotNull(bldrReg);

            // Using "id" to get the pwd-provider
            ParameterizedBuilder<PasswordProvider> builder = (ParameterizedBuilder) bldrReg.getBuilder(PasswordProvider.class, "trustMgrPass");

            PasswordProvider provider = builder.realize(null, null, null);
            assertArrayEquals("storepassword".toCharArray(), provider.get());

            // Using "id" to get the pwd-provider
            ParameterizedBuilder<PasswordProvider> builder2 = (ParameterizedBuilder) bldrReg.getBuilder(PasswordProvider.class, "identityMgrPass");
            PasswordProvider provider2 = builder2.realize(null, null, null);
            assertArrayEquals("keypassword".toCharArray(), provider2.get());

            // SSL Config Load
            XmlElement sslXml = getSocketProviderXml(SSL_PASSWORD_TYPE.USE_PASSWORD_PROVIDER_WITH_MULTIPLE_PARAMS_WITH_INCORRECT_VALUE);
            SocketProviderBuilder sock_builder = (SocketProviderBuilder) ctxPasswordProviders.processDocument(sslXml);
            assertNotNull(sock_builder);
            assertTrue(SocketProviderBuilder.UNNAMED_PROVIDER_ID.equals(sock_builder.getId()));

            sock_builder.getSSLSettings(); // This line results in exception
            });
        
        assertCauseMessage(ex, is("Password verification failed"));
        }

    @Test
    public void testPasswordProviderInlinedInSSLConfig()
        {
        XmlElement xml = getSocketProviderXml(SSL_PASSWORD_TYPE.USE_PASSWORD_PROVIDER_INLINE);
        DefaultProcessingContext ctxPasswordProviders = new DefaultProcessingContext(m_ctxClusterConfig, xml);

        SocketProviderBuilder builder = (SocketProviderBuilder) ctxPasswordProviders.processDocument(xml);
        assertNotNull(builder);
        assertTrue(SocketProviderBuilder.UNNAMED_PROVIDER_ID.equals(builder.getId()));
        SSLSettings sslSettings = builder.getSSLSettings();
        SocketProvider provider = builder.realize(new NullParameterResolver(), null, null);
        SocketProvider delegate = ((SSLSocketProvider) provider).getDependencies().getDelegateSocketProvider();
        assertTrue(delegate.getDelegate() instanceof TcpSocketProvider);
        }

    @Test
    public void testPasswordProvidersWithAdditionalParamsInProviderConfigTheThrowEx()
        {
        thrown.expect(ConfigurationException.class);    // Throws java.lang.NoSuchMethodException wrapped in WrapperException.class
        thrown.expectMessage("Configuration Exception");//Unable to find a compatible constructor for [com.tangosol.coherence.config.xml.processor.GetPassword] with the parameters");

        XmlElement xml = getPasswordProviderXML(PASSWORD_PROVIDER_XML.PROVIDERS_HAVE_REQUIRED_INFO);
        DefaultProcessingContext ctxPasswordProviders = new DefaultProcessingContext(m_ctxClusterConfig, xml);
        Object o1 = ctxPasswordProviders.processDocument(xml);

        XmlElement xml2 = getSocketProviderXml(SSL_PASSWORD_TYPE.USE_PASSWORD_PROVIDER_WITH_ADDITIONAL_PARAMS);
        SocketProviderBuilder builder3 = (SocketProviderBuilder) ctxPasswordProviders.processDocument(xml2);
        assertNotNull(builder3);
        assertTrue(SocketProviderBuilder.UNNAMED_PROVIDER_ID.equals(builder3.getId()));
        SSLSettings sslSettings = builder3.getSSLSettings();
        }

    @Test
    public void whenPasswordProviderHasNoIDAndSSLConfigUsesWithBlankID()
        {
        /*
         * This test is to show that by password-providers without any ID, are put in map. But can't be accessed
         * using "" / null etc. They reside in map against a UUID , and can't be accessed
         * (unless iterated, but will have no way to know if that is the provider we are looking for).
         * Hence ID plays an important role in fetching of the provider.
         */
        thrown.expect(AssertionException.class);
        thrown.expectMessage("<name>valid-id</name> is missing/empty. Failed to lookup a builder for PasswordProvider");

        XmlElement xml = getPasswordProviderXML(PASSWORD_PROVIDER_XML.PROVIDER_HAS_PARAMS_BUT_NO_ID);
        // Password Provider Config Load
        DefaultProcessingContext ctxPasswordProviders = new DefaultProcessingContext(m_ctxClusterConfig, xml);
        ctxPasswordProviders.processDocument(xml);

        ParameterizedBuilderRegistry bldrReg = m_ctxClusterConfig.getCookie(ParameterizedBuilderRegistry.class);
        assertNotNull(bldrReg);

        // Using "id" to get the pwd-provider
        ParameterizedBuilder<PasswordProvider> builder = (ParameterizedBuilder) bldrReg.getBuilder(PasswordProvider.class);
        assertNull(builder);

        // SSL Config Load
        XmlElement sslXml = getSocketProviderXml(SSL_PASSWORD_TYPE.USE_PASSWORD_PROVIDER_WITH_BLANK_ID);
        SocketProviderBuilder sock_builder = (SocketProviderBuilder) ctxPasswordProviders.processDocument(sslXml);
        assertNotNull(sock_builder);
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

    // ----- helper methods -------------------------------------------------

    void assertCauseMessage(Throwable throwable, Matcher<String> matcher)
        {
        while (throwable != null)
            {
            String sMessage = throwable.getMessage();
            if (matcher.matches(sMessage))
                {
                return;
                }
            throwable = throwable.getCause();
            }
        Description description = new StringDescription();
        matcher.describeMismatch(throwable, description);
        fail(description.toString());
        }

    // ----- inner enums ----------------------------------------------------

    /*
     * Various combinations that are tested for <password-provider> in context of <ssl>
     */
    public enum SSL_PASSWORD_TYPE
        {
        USE_PASSWORD,
        USE_PASSWORD_PROVIDER_INLINE,
        USE_PASSWORD_PROVIDER_WITH_DEFAULTS,
        USE_NO_PASSWORD_PROVIDER_OR_PASSWORD,
        USE_PASSWORD_PROVIDER_WITH_DEFAULTS_WITH_INVALID_ID,
        USE_PASSWORD_PROVIDER_WITH_OVERRIDES,
        USE_PASSWORD_PROVIDER_WITH_MULTIPLE_PARAMS,
        USE_PASSWORD_PROVIDER_WITH_ADDITIONAL_PARAMS,
        USE_PASSWORD_WITH_INCORRECT_VALUE,
        USE_PASSWORD_PROVIDER_WITH_OVERRIDES_WITH_INCORRECT_VALUE,
        USE_PASSWORD_PROVIDER_WITH_MULTIPLE_PARAMS_WITH_INCORRECT_VALUE,
        USE_PASSWORD_PROVIDER_WITH_BLANK_ID,
        }

    /*
     * Various combinations that are tested for <password-provider>
     */
    public enum PASSWORD_PROVIDER_XML
        {
        NO_PROVIDER_ENCLOSED,
        PROVIDER_IS_EMPTY,
        PROVIDER_HAS_NO_ID,
        PROVIDER_HAS_PARAMS_BUT_NO_ID,
        PROVIDER_HAS_EMPTY_CLASS_NAME,
        PROVIDER_HAS_NO_CLASS_NAME,
        PROVIDER_HAS_NO_INIT_PARAMS,
        PROVIDER_HAS_NO_INIT_PARAM,
        PROVIDER_HAS_REQUIRED_INFO,
        PROVIDERS_HAVE_REQUIRED_INFO,
        PROVIDER_HAS_STRING_CLASS,
        }

    // ----- data members ---------------------------------------------------

    // local variables
    private DefaultClusterDependencies m_deps;
    private DefaultProcessingContext   m_ctxClusterConfig;

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    }
