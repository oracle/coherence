/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.ssl;

import com.oracle.coherence.common.net.SSLSocketProvider;
import com.oracle.coherence.common.net.SocketProvider;

import com.tangosol.coherence.config.ParameterMacroExpressionParser;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.xml.OperationalConfigNamespaceHandler;
import com.tangosol.coherence.config.xml.processor.PasswordProviderBuilderProcessor;
import com.tangosol.config.xml.DefaultProcessingContext;
import com.tangosol.config.xml.DocumentProcessor;
import com.tangosol.net.PasswordProvider;
import com.tangosol.net.SocketProviderFactory;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.run.xml.XmlValue;

import com.tangosol.util.Base;

import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Provider;
import java.security.SecureRandom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import java.util.concurrent.Executor;

import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * Dependency parses the ssl xml snippet that defines the
 * SSL Socket Provider configuration.
 *
 * Replaced by injectable {@link SSLSocketProviderDefaultDependencies}.
 *
 * @author bb 2011.11.21
 * @since Coherence 12.1.2
 * @deprecated
 */
public class LegacyXmlSSLSocketProviderDependencies
        extends SSLSocketProvider.DefaultDependencies
    {
    /**
     * Construct LegacyXmlSSLSocketProviderDependencies object based on the given XMLElement
     *
     * @param xml XMLElement for ssl config
     */
    public LegacyXmlSSLSocketProviderDependencies(XmlElement xml)
        {
        this(xml, null);
        }

    /**
     * Construct LegacyXmlSSLSocketProviderDependencies object
     *
     * @param xml           XMLElement for ssl config
     * @param dependencies  SocketProviderFactory dependencies
     */
    public LegacyXmlSSLSocketProviderDependencies(XmlElement xml, SocketProviderFactory.Dependencies dependencies)
        {
        if (xml == null)
            {
            throw new IllegalArgumentException("Null xml");
            }

        m_xml = xml;
        m_DependenciesProviderFactory = (dependencies == null)
                                            ? new SocketProviderFactory.DefaultDependencies()
                                            : dependencies;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketProvider getDelegateSocketProvider()
        {
        ensureConfigured();
        return super.getDelegateSocketProvider();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public SSLContext getSSLContext()
        {
        ensureConfigured();
        return super.getSSLContext();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public SSLSocketProvider.ClientAuthMode getClientAuth()
        {
        ensureConfigured();
        return super.getClientAuth();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public HostnameVerifier getHostnameVerifier()
        {
        ensureConfigured();
        return super.getHostnameVerifier();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getEnabledCipherSuites()
        {
        ensureConfigured();
        return super.getEnabledCipherSuites();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getEnabledProtocolVersions()
        {
        ensureConfigured();
        return super.getEnabledProtocolVersions();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Executor getExecutor()
        {
        ensureConfigured();
        return super.getExecutor();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Logger getLogger()
        {
        ensureConfigured();
        return super.getLogger();
        }

    /**
     * Check if xml snippet has been parsed
     */
    protected void ensureConfigured()
        {
        if (!m_fConfigured)
            {
            synchronized (m_xml)
                {
                applyConfig(m_xml);
                m_fConfigured = true;
                }
            }
        }

    /**
     *
     * Parse XMLElement
     */
    protected void applyConfig(XmlElement xml)
        {
        StringBuffer sbDesc = new StringBuffer();
        try
            {
            SSLContext     ctx           = null;
            KeyManager[]   aKeyManager   = null;
            TrustManager[] aTrustManager = null;

            // <protocol>TLS</protocol>
            String sProtocol = xml.ensureElement("protocol").getString(
                DEFAULT_SSL_PROTOCOL);

            // <provider>
            //   <name>MyProvider</name>
            //   <class-name>com.foo.bar.SSLProvider</class-name>
            //   <class-factory-name>com.foo.bar.SSLProviderFactory</class-factory-name>
            //   <method-name>createInstance</method-name>
            //   <init-params>
            //     <init-param>
            //       <param-type>String</param-type>
            //       <param-value>MyValue</param-value>
            //     </init-param>
            //   </init-params>
            // </provider>
            XmlElement xmlChild = xml.getElement("provider");
            if (xmlChild != null)
                {
                String   sProvider = xmlChild.ensureElement("name").getString(null);
                Provider provider  = instantiateProvider(xmlChild);

                if (provider == null)
                    {
                    if (sProvider != null)
                        {
                        ctx = SSLContext.getInstance(sProtocol, sProvider);
                        }
                    }
                else
                    {
                    ctx = SSLContext.getInstance(sProtocol, provider);
                    }
                }

            if (ctx == null)
                {
                ctx = SSLContext.getInstance(sProtocol);
                }
            m_ctx = ctx;

            // <executor>
            //   <class-name>com.foo.bar.Executor</class-name>
            //   <class-factory-name>com.foo.bar.ExecutorFactory</class-factory-name>
            //   <method-name>createInstance</method-name>
            //   <init-params>
            //     <init-param>
            //       <param-type>String</param-type>
            //       <param-value>MyValue</param-value>
            //     </init-param>
            //   </init-params>
            //  </executor>
            xmlChild = xml.getElement("executor");
            if (xmlChild != null)
                {
                if (XmlHelper.isInstanceConfigEmpty(xmlChild))
                    {
                    m_executor = SSLSocketProviderDefaultDependencies.DEFAULT_EXECUTOR;
                    }
                else
                    {
                    m_executor = (Executor)
                            XmlHelper.createInstance(xmlChild, null, null);
                    }
                }

            // <identity-manager>
            //   <algorithm>SunX509</algorithm>
            //   <provider>
            //     <name>MyProvider</name>
            //     <class-name>com.foo.bar.SSLProvider</class-name>
            //     <class-factory-name>com.foo.bar.SSLProviderFactory</class-factory-name>
            //     <method-name>createInstance</method-name>
            //     <init-params>
            //       <init-param>
            //         <param-type>String</param-type>
            //         <param-value>MyValue</param-value>
            //       </init-param>
            //     </init-params>
            //   </provider>
            //   <key-store>
            //     <url>file:.identity.jks</url>
            //     <password>password</password>
            //     <type>JKS</type>
            //   </key-store>
            //   <password>password</password>
            // </identity-manager>
            xmlChild = xml.getElement("identity-manager");
            if (xmlChild == null)
                {
                sbDesc.append("identity=unspecified");
                }
            else
                {
                sbDesc.append("identity=");

                String sAlgorithm = xmlChild.ensureElement("algorithm")
                        .getString(DEFAULT_IDENTITY_ALGORITHM);
                sbDesc.append(sAlgorithm);

                KeyManagerFactory factory = null;

                XmlElement xmlProvider = xmlChild.getElement("provider");
                if (xmlProvider != null)
                    {
                    String   sProvider = xmlProvider.ensureElement("name").getString(null);
                    Provider provider  = instantiateProvider(xmlProvider);

                    if (provider == null)
                        {
                        if (sProvider != null)
                            {
                            factory = KeyManagerFactory.getInstance(sAlgorithm, sProvider);
                            }
                        }
                    else
                        {
                        factory = KeyManagerFactory.getInstance(sAlgorithm, provider);
                        }
                    }

                if (factory == null)
                    {
                    factory = KeyManagerFactory.getInstance(sAlgorithm);
                    }

                XmlElement xmlStore    = xmlChild.ensureElement("key-store");
                String     sPassword   = xmlChild.ensureElement("password").getString(null);
                char[]     achPassword = null;
                if (sPassword == null)
                    {
                    achPassword = getPwdFromProvider(xmlChild);
                    }
                else
                    {
                    achPassword = sPassword.toCharArray();
                    }

                String sURL             = xmlStore.ensureElement("url").getString(null);
                String sStorePassword   = xmlStore.ensureElement("password").getString(null);
                char[] achStorePassword = null;
                if (sStorePassword == null)
                    {
                    achStorePassword = getPwdFromProvider(xmlStore);
                    }
                else
                    {
                    achStorePassword = sStorePassword.toCharArray();
                    }

                KeyStore keyStore = loadKeyStore(sURL,
                    achStorePassword,
                    xmlStore.ensureElement("type").getString(DEFAULT_KEYSTORE_TYPE));

                if (sURL != null && sURL.length() > 0)
                    {
                    sbDesc.append('/')
                    .append(sURL);
                    }

                // clear out passwords
                if (sPassword != null)
                    {
                    xmlChild.ensureElement("password").setString(null);
                    }
                if (sStorePassword != null)
                    {
                    xmlStore.ensureElement("password").setString(null);
                    }
                factory.init(keyStore, achPassword);
                aKeyManager = factory.getKeyManagers();
                }

            // <trust-manager>
            //   <algorithm>SunX509</algorithm>
            //   <provider>
            //     <name>MyProvider</name>
            //     <class-name>com.foo.bar.SSLProvider</class-name>
            //     <class-factory-name>com.foo.bar.SSLProviderFactory</class-factory-name>
            //     <method-name>createInstance</method-name>
            //     <init-params>
            //       <init-param>
            //         <param-type>String</param-type>
            //         <param-value>MyValue</param-value>
            //       </init-param>
            //     </init-params>
            //   </provider>
            //   <key-store>
            //     <url>file:.trust.jks</url>
            //     <password>password</password>
            //     <type>JKS</type>
            //   </key-store>
            // </trust-manager>
            xmlChild = xml.getElement("trust-manager");
            if (xmlChild == null || xmlChild.getElementList().isEmpty())
                {
                sbDesc.append(", trust=unspecified");
                }
            else
                {
                sbDesc.append(", trust=");

                String sAlgorithm = xmlChild.ensureElement("algorithm")
                        .getString(DEFAULT_TRUST_ALGORITHM);
                sbDesc.append(sAlgorithm);

                TrustManagerFactory factory = null;

                XmlElement xmlProvider = xmlChild.getElement("provider");
                if (xmlProvider != null)
                    {
                    String   sProvider = xmlProvider.ensureElement("name").getString(null);
                    Provider provider  = instantiateProvider(xmlProvider);

                    if (provider == null)
                        {
                        if (sProvider != null)
                            {
                            factory = TrustManagerFactory.getInstance(sAlgorithm, sProvider);
                            }
                        }
                    else
                        {
                        factory = TrustManagerFactory.getInstance(sAlgorithm, provider);
                        }
                    }

                if (factory == null)
                    {
                    factory = TrustManagerFactory.getInstance(sAlgorithm);
                    }

                XmlElement xmlStore         = xmlChild.ensureElement("key-store");
                String     sURL             = xmlStore.ensureElement("url").getString(null);
                String     sTrustPassword   = xmlStore.ensureElement("password").getString(null);
                char[]     achTrustPassword = null;
                if (sTrustPassword == null)
                    {
                    achTrustPassword = getPwdFromProvider(xmlStore);
                    }
                else
                    {
                    achTrustPassword = sTrustPassword.toCharArray();
                    }

                KeyStore   keyStore = loadKeyStore(sURL,
                    achTrustPassword,
                    xmlStore.ensureElement("type").getString(DEFAULT_KEYSTORE_TYPE));

                if (sURL != null && sURL.length() > 0)
                    {
                    sbDesc.append('/')
                    .append(sURL);
                    }

                // clear out passwords
                if (sTrustPassword != null)
                    {
                    xmlStore.ensureElement("password").setString(null);
                    }
                factory.init(keyStore);

                aTrustManager = factory.getTrustManagers();
                }

            // <hostname-verifier>
            //   <class-name>com.foo.bar.HostnameVerifier</class-name>
            //   <class-factory-name>com.foo.bar.HostnameVerifierFactory</class-factory-name>
            //   <method-name>createInstance</method-name>
            //   <init-params>
            //     <init-param>
            //       <param-type>String</param-type>
            //       <param-value>MyValue</param-value>
            //     </init-param>
            //   </init-params>
            // </hostname-verifier>
            xmlChild = xml.getElement("hostname-verifier");
            if (xmlChild != null)
                {
                m_hostnameVerifier = (HostnameVerifier)
                        XmlHelper.createInstance(xmlChild, null, null);
                sbDesc.append(", hostname-verifier=enabled");
                }

            // intialize a random number source
            SecureRandom random = new SecureRandom();
            random.nextInt();

            // initialize the SSLContext
            ctx.init(aKeyManager, aTrustManager, random);

            // <cipher-suites>
            //   <name>cipher-1</name>
            //   <name>cipher-2</name>
            //   ...
            //   <name></name>
            // </cipher-suites>
            xmlChild = xml.getElement("cipher-suites");
            if (xmlChild != null)
                {
                ArrayList listCipher = new ArrayList();
                for (Iterator iter = xmlChild.getElements("name");
                     iter.hasNext(); )
                    {
                    listCipher.add(((XmlElement) iter.next()).getValue());
                    }

                XmlValue xmlValue = xmlChild.getAttribute("usage");
                if (xmlValue != null && xmlValue.getString().equals("black-list"))
                    {
                    SSLEngine engine             = ctx.createSSLEngine();
                    ArrayList listDefaultCiphers = new ArrayList(Arrays.asList(engine.getEnabledCipherSuites()));
                    listDefaultCiphers.removeAll(listCipher);
                    listCipher = listDefaultCiphers;
                    }

                m_asCipherSuitesEnabled = (String[]) listCipher.toArray(
                            new String[listCipher.size()]);
                }


            // <protocol-versions>
            //   <name>protocol-version1</name>
            //   <name>protocol-version2</name>
            //   ...
            //   <name></name>
            // </protocols>
            xmlChild = xml.getElement("protocol-versions");
            if (xmlChild != null)
                {
                ArrayList listProtocol = new ArrayList();
                for (Iterator iter = xmlChild.getElements("name");
                     iter.hasNext(); )
                    {
                    listProtocol.add(((XmlElement) iter.next()).getValue());
                    }

                XmlValue xmlValue = xmlChild.getAttribute("usage");
                if (xmlValue != null && xmlValue.getString().equals("black-list"))
                    {
                    SSLEngine engine               = ctx.createSSLEngine();
                    ArrayList listDefaultProtocols = new ArrayList(Arrays.asList(engine.getEnabledProtocols()));
                    listDefaultProtocols.removeAll(listProtocol);
                    listProtocol = listDefaultProtocols;
                    }

                m_asProtocolVersionsEnabled = (String[]) listProtocol.toArray(
                        new String[listProtocol.size()]);
                }

            // delegate socket-provider
            // check if socket-provider element is present
            xmlChild = xml.getElement("socket-provider");
            if (xmlChild == null)
                {
                setDelegate(m_DependenciesProviderFactory.getSocketProviderFactory().DEFAULT_SOCKET_PROVIDER);
                }
            else
                {
                setDelegate(m_DependenciesProviderFactory.getSocketProviderFactory().getSocketProvider(xmlChild));
                }

            SSLSocketProvider.ClientAuthMode clientAuthMode = null;

            String sAuthDesc;
            if (aKeyManager == null && aTrustManager == null)
                {
                clientAuthMode = SSLSocketProvider.ClientAuthMode.none;
                sAuthDesc = "none";
                }
            else
                {
                xmlChild = xml.getElement("client-auth");
                String sAuthName = xmlChild == null ? null : xmlChild.getString();
                if (sAuthName == null)
                    {
                    clientAuthMode = aTrustManager == null
                            ? SSLSocketProvider.ClientAuthMode.none
                            : SSLSocketProvider.ClientAuthMode.required;

                    sAuthDesc =
                            aKeyManager == null && aTrustManager == null ? "none"
                                    : aKeyManager == null && aTrustManager != null ? "one-way client"
                                            : aKeyManager != null && aTrustManager == null ? "one-way server"
                                                    : "two-way";
                    }
                else
                    {
                    clientAuthMode = SSLSocketProvider.ClientAuthMode.valueOf(sAuthName);
                    sAuthDesc = "client-auth " + clientAuthMode.name();
                    }
                }

            m_clientAuthMode = clientAuthMode;

            m_sDescription = sbDesc.insert(0,"SSLSocketProvider(auth=" + sAuthDesc + ", ")
                    .append(')').toString();
            }
        catch (GeneralSecurityException e)
            {
            throw new IllegalArgumentException("Invalid configuration: " + xml, e);
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    // ----- helpers -----------------------------------------------------

    /**
     * Instantiate and return an instance of a security provider using an
     * XML configuration of the following form:
     * <br><br>
     * <tt><pre>
     * &lt;provider&gt;
     *   &lt;class-name&gt;com.foo.bar.Provider&lt;/class-name&gt;
     *   &lt;class-factory-name&gt;com.foo.bar.ProviderFactory&lt;/class-factory-name&gt;
     *   &lt;method-name&gt;createInstance&lt;/method-name&gt;
     *   &lt;init-params&gt;
     *     &lt;init-param&gt;
     *       &lt;param-type&gt;String&lt;/param-type&gt;
     *       &lt;param-value&gt;MyValue&lt;/param-value&gt;
     *     &lt;/init-param&gt;
     *   &lt;/init-params&gt;
     * &lt;/provider&gt;
     * </pre></tt>
     *
     * @param xml  the XML configuration describing the provider to instantiate
     *
     * @return a new security provider
     */
    protected Provider instantiateProvider(XmlElement xml)
        {
        if (XmlHelper.isInstanceConfigEmpty(xml))
            {
            return null;
            }
        return (Provider) XmlHelper.createInstance(xml, null, null);
        }

    /**
     * Utility method for loading a keystore.
     *
     * @param sURL       the URL of the keystore to load
     * @param sPassword  the opitonal password for the keystore
     * @param sType      the keystore type
     *
     * @throws GeneralSecurityException on keystore access error
     * @throws IOException on I/O error
     *
     * @return the keystore
     */
    protected KeyStore loadKeyStore(String sURL, String sPassword, String sType)
            throws GeneralSecurityException, IOException
        {
        return loadKeyStore(sURL,
                sPassword == null || sPassword.length() == 0 ? null : sPassword.toCharArray(), sType);
        }

    /**
     * Utility method for loading a keystore.
     *
     * @param sURL         the URL of the keystore to load
     * @param achPassword  the opitonal password for the keystore
     * @param sType        the keystore type
     *
     * @throws GeneralSecurityException on keystore access error
     * @throws IOException on I/O error
     *
     * @return the keystore
     */
    protected KeyStore loadKeyStore(String sURL, char[] achPassword, String sType)
            throws GeneralSecurityException, IOException
            {
            if (sURL == null || sURL.length() == 0)
                {
                return null;
                }

            KeyStore    keyStore = KeyStore.getInstance(sType);
            InputStream in       = null;
            try
                {
                ClassLoader loader = this.getClass().getClassLoader();
                in = loader.getResourceAsStream(new URL(sURL).getFile());
                if (in == null)
                    {
                    in = new URL(sURL).openStream();
                    }
                keyStore.load(in, achPassword);
                }
            finally
            {
            if (in != null)
                {
                try
                    {
                    in.close();
                    }
                catch (IOException e) {}
                }
            }

            return keyStore;
            }

    protected char[] getPwdFromProvider(XmlElement xmlProvider)
        {
        XmlElement xmlPwdProvider = xmlProvider.ensureElement("password-provider");
        if (xmlPwdProvider != null)
            {
            OperationalConfigNamespaceHandler nsHandler    = new OperationalConfigNamespaceHandler();
            DocumentProcessor.Dependencies    dependencies =
                    new DocumentProcessor.DefaultDependencies(nsHandler)
                            .setExpressionParser(new ParameterMacroExpressionParser());
            DefaultProcessingContext          ctx          = new DefaultProcessingContext(dependencies, null);
            ctx.ensureNamespaceHandler("", nsHandler);

            ParameterizedBuilder<PasswordProvider> bldr = new PasswordProviderBuilderProcessor().process(ctx, xmlPwdProvider);
            return bldr.realize(null, null, null).get();
            }

        return null;
        }

    // ----- Object methods ----------------------------------------------

   /**
    * {@inheritDoc}
    */
    public String toString()
        {
        return m_sDescription;
        }

    // ----- data members ------------------------------------------------
    /**
     * The description of the provider.
     */
    protected String m_sDescription = "SSLSocketProvider()";

    /**
     * Check if the xml has been parsed
     */
    protected volatile boolean m_fConfigured;

    /**
     * SSL Xml config snippet
     */
    protected XmlElement m_xml;

    /**
     * SocketProviderFactory dependencies
     */
    protected SocketProviderFactory.Dependencies m_DependenciesProviderFactory;

    // ----- constants ------------------------------------------------------

    /**
     * The name of the XmlElement in which the provider configuration is
     * specified.
     */
     public static final String XML_NAME = "ssl";

     /**
     * The default SSL protocol.
     */
     public static final String DEFAULT_SSL_PROTOCOL = "TLS";

     /**
     * The default identity management algorithm.
     */
     public static final String DEFAULT_IDENTITY_ALGORITHM = "SunX509";

     /**
     * The default trust management algorithm.
     */
     public static final String DEFAULT_TRUST_ALGORITHM = "SunX509";

     /**
     * The default keystore type.
     */
     public static final String DEFAULT_KEYSTORE_TYPE = "JKS";
}
