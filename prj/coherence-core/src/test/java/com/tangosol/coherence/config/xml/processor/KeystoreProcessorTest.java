/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.ParameterMacroExpressionParser;
import com.tangosol.coherence.config.builder.InstanceBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.xml.OperationalConfigNamespaceHandler;
import com.tangosol.coherence.config.xml.processor.PasswordProviderBuilderProcessor.DefaultPasswordProvider;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.xml.DefaultProcessingContext;
import com.tangosol.config.xml.DocumentProcessor;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.internal.net.ssl.DefaultKeystoreDependencies;
import com.tangosol.internal.net.ssl.SSLSocketProviderDefaultDependencies;
import com.tangosol.net.PasswordProvider;
import com.tangosol.net.ssl.CertificateLoader;
import com.tangosol.net.ssl.KeyStoreLoader;
import com.tangosol.net.ssl.PrivateKeyLoader;
import com.tangosol.net.ssl.URLCertificateLoader;
import com.tangosol.net.ssl.URLKeyStoreLoader;
import com.tangosol.net.ssl.URLPrivateKeyLoader;
import com.tangosol.run.xml.SimpleElement;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;
import org.junit.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class KeystoreProcessorTest
    {
    @Test
    public void shouldProcessEmptyXml()
        {
        XmlElement                  xml          = new SimpleElement("key-store");
        DefaultKeystoreDependencies dependencies = processXML(xml);

        assertThat(dependencies, is(notNullValue()));
        assertThat(dependencies.getKeyStoreLoader(), is(nullValue()));
        assertThat(dependencies.getType(), is(SSLSocketProviderDefaultDependencies.DEFAULT_KEYSTORE_TYPE));
        assertNullPasswordProvider(dependencies);
        }

    @Test
    public void shouldProcessKeyStoreFromURL()
        {
        XmlElement xml  = new SimpleElement("key-store");
        String     sURL = "file:/coherence/server.jks";

        xml.addElement("url").setString(sURL);

        DefaultKeystoreDependencies dependencies = processXML(xml);

        assertThat(dependencies, is(notNullValue()));
        KeyStoreLoader loader = dependencies.getKeyStoreLoader();
        assertThat(loader, is(instanceOf(URLKeyStoreLoader.class)));
        assertThat(((URLKeyStoreLoader) loader).getName(), is(sURL));

        assertThat(dependencies.getType(), is(SSLSocketProviderDefaultDependencies.DEFAULT_KEYSTORE_TYPE));
        assertNullPasswordProvider(dependencies);
        }

    @Test
    public void shouldProcessKeyStoreFromURLWithPassword()
        {
        XmlElement xml  = new SimpleElement("key-store");
        String     sURL = "file:/coherence/server.jks";
        String     sPwd = "secret";

        xml.addElement("url").setString(sURL);
        xml.addElement("password").setString(sPwd);

        DefaultKeystoreDependencies dependencies = processXML(xml);

        assertThat(dependencies, is(notNullValue()));
        KeyStoreLoader loader = dependencies.getKeyStoreLoader();
        assertThat(loader, is(instanceOf(URLKeyStoreLoader.class)));
        assertThat(((URLKeyStoreLoader) loader).getName(), is(sURL));

        PasswordProvider provider = dependencies.getPasswordProvider();
        assertThat(provider, is(notNullValue()));
        char[] aChar = provider.get();
        assertThat(aChar, is(sPwd.toCharArray()));

        assertThat(dependencies.getType(), is(SSLSocketProviderDefaultDependencies.DEFAULT_KEYSTORE_TYPE));
        }

    @Test
    public void shouldProcessKeyStoreFromURLWithType()
        {
        XmlElement xml   = new SimpleElement("key-store");
        String     sURL  = "file:/coherence/server.jks";
        String     sType = "Foo";

        xml.addElement("url").setString(sURL);
        xml.addElement("type").setString(sType);

        DefaultKeystoreDependencies dependencies = processXML(xml);

        assertThat(dependencies, is(notNullValue()));
        KeyStoreLoader loader = dependencies.getKeyStoreLoader();
        assertThat(loader, is(instanceOf(URLKeyStoreLoader.class)));
        assertThat(((URLKeyStoreLoader) loader).getName(), is(sURL));

        assertThat(dependencies.getType(), is(sType));
        assertNullPasswordProvider(dependencies);
        }

    @Test
    public void shouldProcessKeyStoreWithKeyStoreLoader()
        {
        XmlElement xml       = new SimpleElement("key-store");
        XmlElement xmlLoader = xml.addElement("key-store-loader");

        xmlLoader.addElement("class-name").setString(CustomLoader.class.getName());

        DefaultKeystoreDependencies dependencies = processXML(xml);

        assertThat(dependencies, is(notNullValue()));
        KeyStoreLoader loader = dependencies.getKeyStoreLoader();
        assertThat(loader, is(instanceOf(CustomLoader.class)));

        assertThat(dependencies.getType(), is(SSLSocketProviderDefaultDependencies.DEFAULT_KEYSTORE_TYPE));
        assertNullPasswordProvider(dependencies);
        }

    @Test
    public void shouldProcessKeyStoreWithCustomLoaderElement()
        {
        XmlElement xml = new SimpleElement("key-store");

        xml.addElement("my-loader");

        OperationalConfigNamespaceHandler namespaceHandler = new OperationalConfigNamespaceHandler();
        namespaceHandler.registerProcessor("my-loader", new CustomProcessor());

        DefaultKeystoreDependencies dependencies = processXML(xml, namespaceHandler);

        assertThat(dependencies, is(notNullValue()));
        KeyStoreLoader loader = dependencies.getKeyStoreLoader();
        assertThat(loader, is(instanceOf(CustomLoader.class)));

        assertThat(dependencies.getType(), is(SSLSocketProviderDefaultDependencies.DEFAULT_KEYSTORE_TYPE));
        assertNullPasswordProvider(dependencies);
        }

    // ----- helper methods -------------------------------------------------

    private DefaultKeystoreDependencies processXML(XmlElement xml)
        {
        return processXML(xml, new OperationalConfigNamespaceHandler());
        }

    private DefaultKeystoreDependencies processXML(XmlElement xml, OperationalConfigNamespaceHandler namespaceHandler)
        {
        DocumentProcessor.DefaultDependencies dependencies =
                new DocumentProcessor.DefaultDependencies();

        ResourceRegistry registry = new SimpleResourceRegistry();

        dependencies.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dependencies.setResourceRegistry(registry);

        DefaultProcessingContext context = new DefaultProcessingContext(dependencies, null);
        context.ensureNamespaceHandler("", namespaceHandler);

        return (DefaultKeystoreDependencies) new KeystoreProcessor().process(context, xml);
        }

    private void assertNullPasswordProvider(DefaultKeystoreDependencies dependencies)
        {
        PasswordProvider provider = dependencies.getPasswordProvider();
        assertThat(provider, is(notNullValue()));
        char[] aChar = provider.get();
        assertThat(aChar, is(notNullValue()));
        assertThat(aChar.length, is(0));
        }

    // ----- inner class: CustomLoader --------------------------------------

    public static class CustomLoader
            implements KeyStoreLoader
        {
        @Override
        public KeyStore load(String sType, PasswordProvider password) throws GeneralSecurityException, IOException
            {
            return null;
            }
        }

    // ----- inner class: CustomProcessor -----------------------------------

    public static class CustomProcessor
            implements ElementProcessor<ParameterizedBuilder<CustomLoader>>
        {
        @Override
        public ParameterizedBuilder<CustomLoader> process(ProcessingContext context, XmlElement xmlElement) throws ConfigurationException
            {
            InstanceBuilder<CustomLoader> builder = new InstanceBuilder<>();
            builder.setClassName(new LiteralExpression<>(CustomLoader.class.getName()));
            
            return builder;
            }
        }

    }
