/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.ParameterMacroExpressionParser;
import com.tangosol.coherence.config.xml.OperationalConfigNamespaceHandler;

import com.tangosol.config.xml.DefaultProcessingContext;
import com.tangosol.config.xml.DocumentProcessor;

import com.tangosol.internal.net.ssl.DefaultManagerDependencies;
import com.tangosol.internal.net.ssl.KeystoreDependencies;
import com.tangosol.internal.net.ssl.SSLSocketProviderDefaultDependencies;

import com.tangosol.net.PasswordProvider;

import com.tangosol.net.ssl.CertificateLoader;
import com.tangosol.net.ssl.PrivateKeyLoader;
import com.tangosol.net.ssl.URLCertificateLoader;
import com.tangosol.net.ssl.URLPrivateKeyLoader;

import com.tangosol.run.xml.SimpleElement;
import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

public class SSLManagerProcessorTest
    {
    @Test
    public void shouldProcessEmptyIdentityManagerXml()
        {
        XmlElement                 xml          = new SimpleElement("identity-manager");
        DefaultManagerDependencies dependencies = processXML(xml);

        assertThat(dependencies, is(notNullValue()));
        assertThat(dependencies.getAlgorithm(), is(SSLSocketProviderDefaultDependencies.DEFAULT_IDENTITY_ALGORITHM));
        assertThat(dependencies.getListeners(), is(empty()));
        assertThat(dependencies.getProviderBuilder(), is(nullValue()));
        assertThat(dependencies.getKeystoreDependencies(), is(KeystoreDependencies.NullImplementation));

        assertThat(dependencies.getPrivateKeyLoader(), is(nullValue()));
        assertThat(dependencies.getCertificateLoaders(), is(nullValue()));

        PasswordProvider provider = dependencies.getPrivateKeyPasswordProvider();
        assertThat(provider, is(notNullValue()));

        char[] ac = provider.get();
        assertThat(ac, is(notNullValue()));
        assertThat(ac.length, is(0));
        }

    @Test
    public void shouldProcessEmptyTrustManagerXml()
        {
        XmlElement                 xml          = new SimpleElement("trust-manager");
        DefaultManagerDependencies dependencies = processXML(xml);

        assertThat(dependencies, is(notNullValue()));
        assertThat(dependencies.getAlgorithm(), is(SSLSocketProviderDefaultDependencies.DEFAULT_TRUST_ALGORITHM));
        assertThat(dependencies.getListeners(), is(empty()));
        assertThat(dependencies.getProviderBuilder(), is(nullValue()));
        assertThat(dependencies.getKeystoreDependencies(), is(KeystoreDependencies.NullImplementation));

        assertThat(dependencies.getPrivateKeyLoader(), is(nullValue()));
        assertThat(dependencies.getCertificateLoaders(), is(nullValue()));

        PasswordProvider provider = dependencies.getPrivateKeyPasswordProvider();
        assertThat(provider, is(notNullValue()));

        char[] ac = provider.get();
        assertThat(ac, is(notNullValue()));
        assertThat(ac.length, is(0));
        }

    @Test
    public void shouldProcessKey()
        {
        XmlElement xml = new SimpleElement("identity-manager");
        xml.addElement("key").setString("one.pem");

        DefaultManagerDependencies dependencies = processXML(xml);
        PrivateKeyLoader loader       = dependencies.getPrivateKeyLoader();

        assertThat(loader, is(instanceOf(URLPrivateKeyLoader.class)));
        assertThat(((URLPrivateKeyLoader) loader).getName(), is("one.pem"));

        assertThat(dependencies.getCertificateLoaders(), is(nullValue()));
        }

    @Test
    public void shouldProcessCert()
        {
        XmlElement xml = new SimpleElement("identity-manager");
        xml.addElement("cert").setString("one.cert");

        DefaultManagerDependencies dependencies = processXML(xml);

        CertificateLoader[] aLoader = dependencies.getCertificateLoaders();
        assertThat(aLoader, is(notNullValue()));
        assertThat(aLoader.length, is(1));
        assertThat(aLoader[0], is(instanceOf(URLCertificateLoader.class)));
        assertThat(((URLCertificateLoader) aLoader[0]).getName(), is("one.cert"));

        assertThat(dependencies.getPrivateKeyLoader(), is(nullValue()));
        }

    @Test
    public void shouldProcessMultipleCerts()
        {
        XmlElement xml = new SimpleElement("identity-manager");
        xml.addElement("cert").setString("one.cert");
        xml.addElement("cert").setString("two.cert");

        DefaultManagerDependencies dependencies = processXML(xml);

        CertificateLoader[] aLoader = dependencies.getCertificateLoaders();
        assertThat(aLoader, is(notNullValue()));
        assertThat(aLoader.length, is(2));
        assertThat(aLoader[0], is(instanceOf(URLCertificateLoader.class)));
        assertThat(((URLCertificateLoader) aLoader[0]).getName(), is("one.cert"));
        assertThat(aLoader[1], is(instanceOf(URLCertificateLoader.class)));
        assertThat(((URLCertificateLoader) aLoader[1]).getName(), is("two.cert"));

        assertThat(dependencies.getPrivateKeyLoader(), is(nullValue()));
        }

    // ----- helper methods -------------------------------------------------

    private DefaultManagerDependencies processXML(XmlElement xml)
        {
        DocumentProcessor.DefaultDependencies dependencies =
                new DocumentProcessor.DefaultDependencies();

        ResourceRegistry registry = new SimpleResourceRegistry();

        dependencies.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dependencies.setResourceRegistry(registry);

        DefaultProcessingContext context = new DefaultProcessingContext(dependencies, null);
        context.ensureNamespaceHandler("", new OperationalConfigNamespaceHandler());

        return (DefaultManagerDependencies) new SSLManagerProcessor().process(context, xml);
        }
    }
