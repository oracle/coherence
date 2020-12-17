/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.internal.net.ssl.SSLCertUtility;
import com.oracle.coherence.common.net.SocketProvider;

import com.tangosol.coherence.config.Config;
import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.xml.processor.PasswordProviderBuilderProcessor;

import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.internal.net.ssl.SSLSocketProviderDefaultDependencies;

import com.tangosol.net.InetAddressHelper;
import com.tangosol.net.PasswordProvider;
import com.tangosol.net.SocketProviderFactory;
import com.tangosol.net.security.SecurityProvider;

import com.tangosol.util.Base;

import java.io.IOException;
import java.io.InputStream;

import java.net.InetAddress;
import java.net.URL;

import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Provider;
import java.security.SecureRandom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import java.util.concurrent.Executor;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * {@link SSLSocketProviderDependenciesBuilder} enables lazy instantiation of SSL SocketProvider.
 *
 * Builder includes methods that allows one to specify whether to get a datagram or demultiplexed
 * {@link SocketProvider} and what subport to use for the socketprovider.
 *
 * @author jf  2015.11.11
 * @since Coherence 12.2.1.1
 */
public class SSLSocketProviderDependenciesBuilder
        implements ParameterizedBuilder<SSLSocketProviderDefaultDependencies>
    {
    // ----- constructors ----------------------------------------------------

    /**
     * Constructs {@link SSLSocketProviderDependenciesBuilder}
     *
     * @param deps  {@link SSLSocketProviderDefaultDependencies} defaults for cluster
     */
    public SSLSocketProviderDependenciesBuilder(SSLSocketProviderDefaultDependencies deps)
        {
        m_deps                       = deps;
        m_bldrDelegateSocketProvider = new SocketProviderBuilder(SocketProviderFactory.DEFAULT_SOCKET_PROVIDER);
        m_sNameProtocol              = SSLSocketProviderDefaultDependencies.DEFAULT_SSL_PROTOCOL;
        }


    // ----- SSLSocketProviderDependenciesBuilder methods --------------------------------

    /**
     * Set the SSL protocol name
     *
     * @param sName
     */
    @Injectable("protocol")
    public void setProtocol(String sName)
        {
        m_sNameProtocol = sName;
        }

    /**
     * Get the SSL protocol name
     *
     * @return protocol name
     */
    public String getProtocol()
        {
        return m_sNameProtocol;
        }

    /**
     * Set the SSL provider builder.
     *
     * @param builder SSL provider builder
     */
    @Injectable("provider")
    public void setProviderBuilder(ProviderBuilder builder)
        {
        m_bldrProvider = builder;
        }

    /**
     * Get the SSL provider builder.
     *
     * @return the provider builder
     */
    public ProviderBuilder getProvider()
        {
        return m_bldrProvider;
        }

    /**
     * Set SSL executors builder.
     *
     * @param bldr  builder for SSL executors.
     */
    @Injectable("executor")
    public void setExecutor(ParameterizedBuilder<Executor> bldr)
        {
        m_bldrExecutor = bldr;
        }

    /**
     * Set the SSL identity manager dependencies.
     *
     * @param deps configured or defaulted values for identity manager dependencies
     */
    @Injectable("identity-manager")
    public void setIdentityManager(DefaultManagerDependencies deps)
        {
        m_depsIdentityManager = deps;
        }

    /**
     * Get the SSL identity manager dependencies
     *
     * @return identity manager configured/defaulted values
     */
    public DefaultManagerDependencies getIdentityManager()
        {
        return m_depsIdentityManager;
        }

    /**
     * Get the SSL trust manager
     *
     * @return the trust manager
     */
    public ManagerDependencies getTrustManager()
        {
        return m_depsTrustManager;
        }

    /**
     * Set the SSL trust manager
     *
     * @param deps trust manager configured/defaulted values
     */
    @Injectable("trust-manager")
    public void setTrustManager(ManagerDependencies deps)
        {
        m_depsTrustManager = deps;
        }

    /**
     * Set the customized HostnameVerifierBuilder
     *
     * @param bldr HostnameVerifierBuilder
     */
    @Injectable("hostname-verifier")
    public void setHostnameVerifierBuilder(ParameterizedBuilder<HostnameVerifier> bldr)
        {
        m_bldrHostnameVerifier = bldr;
        }

    /**
     * Get customized HostnameVerifierBuilder
     *
     * @return {@link HostnameVerifier} or null
     */
    public ParameterizedBuilder<HostnameVerifier> getHostnameVerifierBuilder()
        {
        return m_bldrHostnameVerifier;
        }

    /**
     * Set cipher-suites dependencies
     *
     * @param deps cipher-suites config info
     */
    @Injectable("cipher-suites")
    public void setCipherSuitesNameList(NameListDependencies deps)
        {
        m_depsCipherSuite = deps;
        }

    /**
     * Set protocol-versions dependencies
     *
     * @param deps protocol-versions config info
     */
    @Injectable("protocol-versions")
    public void setProtocolVersionsNameList(NameListDependencies deps)
        {
        m_depsProtocolVersion = deps;
        }

    /**
     * Set delegate SocketProviderBuilder
     *
     * @param bldr  delegate socket provider builder
     */
    @Injectable("socket-provider")
    public void setDelegate(SocketProviderBuilder bldr)
        {
        m_bldrDelegateSocketProvider = bldr;
        }


    /**
     * Get delegate socket provider builder
     *
     * @return socket provider builder
     */
    public SocketProviderBuilder getSocketProviderBuilder()
        {
        return m_bldrDelegateSocketProvider;
        }

    /**
     * Realize a SSLSocketProviderDefaultDependencies based on configured/defaulted values for config element ssl.
     *
     * Note: unlike typical builders, this is realize once since sensitive password data is nulled after realizing.
     *
     * @return {@link SSLSocketProviderDefaultDependencies}
     */
    public synchronized SSLSocketProviderDefaultDependencies realize()
        {
        if (m_fRealized)
            {
            return m_deps;
            }

        // realize once
        SSLSocketProviderDefaultDependencies deps    = m_deps;

        try
            {
            KeyManager[]   aKeyManager   = null;
            TrustManager[] aTrustManager = null;
            SSLContext     ctx           = null;
            StringBuffer   sbDesc        = new StringBuffer();

            Provider provider = m_bldrProvider == null ? null : (Provider) m_bldrProvider.realize(null, null, null);

            if (provider == null)
                {
                if (m_bldrProvider != null && m_bldrProvider.getName() != null)
                    {
                    ctx = SSLContext.getInstance(getProtocol(), m_bldrProvider.getName());
                    }
                }
            else
                {
                ctx = SSLContext.getInstance(getProtocol(), provider);
                }

            if (ctx == null)
                {
                ctx = SSLContext.getInstance(getProtocol());
                }

            deps.setSSLContext(ctx);


            if (m_bldrExecutor == null)
                {
                deps.setExecutor(SSLSocketProviderDefaultDependencies.DEFAULT_EXECUTOR);
                }
            else
                {
                deps.setExecutor(m_bldrExecutor.realize(new NullParameterResolver(), null, null));
                }

            DefaultManagerDependencies depsIdMgr = getIdentityManager();

            if (depsIdMgr == null)
                {
                sbDesc.append("identity=unspecified");
                }
            else
                {
                KeyManagerFactory factory      = null;
                ProviderBuilder   bldrProvider = depsIdMgr.getProviderBuilder();
                String            sAlgorithm   = depsIdMgr.getAlgorithm();

                sbDesc.append("identity=").append(sAlgorithm);

                if (bldrProvider != null)
                    {
                    provider = bldrProvider.realize(null, null, null);
                    if (provider == null)
                        {
                        if (bldrProvider.getName() != null)
                            {
                            factory = KeyManagerFactory.getInstance(sAlgorithm, bldrProvider.getName());
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

                DefaultKeystoreDependencies depsKeystore = depsIdMgr.getKeystoreDependencies();
                String                      sURL         = depsKeystore.getURL();
                KeyStore                    keyStore     = loadKeyStore(sURL, depsKeystore.getPasswordProvider(), depsKeystore.getType());

                if (sURL != null && sURL.length() > 0)
                    {
                    sbDesc.append('/').append(sURL);
                    }

                char[] achPassword = depsIdMgr.getPasswordProvider().get();
                factory.init(keyStore, achPassword);
                aKeyManager = factory.getKeyManagers();

                //Zero the password
                if (achPassword != null)
                    {
                    Arrays.fill(achPassword, '0');
                    }
                }

            ManagerDependencies depsTrustMgr = getTrustManager();

            if (depsTrustMgr == null)
                {
                sbDesc.append(", trust=unspecified");
                }
            else
                {
                TrustManagerFactory factory      = null;
                ProviderBuilder     bldrProvider = depsTrustMgr.getProviderBuilder();
                String              sAlgorithm   = depsTrustMgr.getAlgorithm();

                sbDesc.append(", trust=").append(sAlgorithm);

                if (bldrProvider != null)
                    {
                    provider = bldrProvider.realize(null, null, null);

                    String sProvider = bldrProvider.getName();

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

                DefaultKeystoreDependencies depsKeystore = depsTrustMgr.getKeystoreDependencies();
                String                      sURL         = depsKeystore.getURL();
                KeyStore                    keyStore     = loadKeyStore(sURL, depsKeystore.getPasswordProvider(), depsKeystore.getType());

                if (sURL != null && sURL.length() > 0)
                    {
                    sbDesc.append('/').append(sURL);
                    }

                factory.init(keyStore);

                aTrustManager = factory.getTrustManagers();
                deps.setClientAuthenticationRequired(aTrustManager != null);
                }


            ParameterizedBuilder<HostnameVerifier> bldrHostnameVerifier = getHostnameVerifierBuilder();

            if (bldrHostnameVerifier != null)
                {
                deps.setHostnameVerifier(bldrHostnameVerifier.realize(null, null, null));
                sbDesc.append(", hostname-verifier=custom");
                }

            // intialize a random number source
            SecureRandom random = new SecureRandom();

            random.nextInt();

            // initialize the SSLContext
            ctx.init(aKeyManager, aTrustManager, random);

            if (m_depsCipherSuite != null)
                {
                List<String>    listCipher       = m_depsCipherSuite.getNameList();

                if (m_depsCipherSuite.isBlackList())
                    {
                    SSLEngine engine            = ctx.createSSLEngine();
                    ArrayList listDefaultCipher = new ArrayList(Arrays.asList(engine.getEnabledCipherSuites()));

                    listDefaultCipher.removeAll(listCipher);
                    listCipher = listDefaultCipher;
                    }

                deps.setEnabledCipherSuites((String[]) listCipher.toArray(new String[listCipher.size()]));
                }

            if (m_depsProtocolVersion != null)
                {
                List<String>    listProtocol        = m_depsProtocolVersion.getNameList();

                if (m_depsProtocolVersion.isBlackList())
                    {
                    SSLEngine engine            = ctx.createSSLEngine();
                    ArrayList listDefaultProtocols = new ArrayList(Arrays.asList(engine.getEnabledProtocols()));

                    listDefaultProtocols.removeAll(listProtocol);
                    listProtocol = listDefaultProtocols;
                    }

                deps.setEnabledProtocolVersions((String[]) listProtocol.toArray(new String[listProtocol.size()]));
                }

            deps.setDelegateSocketProviderBuilder(m_bldrDelegateSocketProvider);

            String sAuth = aKeyManager == null && aTrustManager == null
                           ? "none"
                           : aKeyManager == null && aTrustManager != null
                             ? "one-way client"
                             : aKeyManager != null && aTrustManager == null ? "one-way server" : "two-way";

            deps.setDescription(sbDesc.insert(0, "SSLSocketProvider(auth=" + sAuth + ", ").append(')').toString());
            Logger.fine("instantiated SSLSocketProviderDependencies: " + sbDesc.toString());
            m_fRealized = true;
            }
        catch (GeneralSecurityException e)
            {
            throw new IllegalArgumentException("Invalid configuration ", e);
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }

        return deps;
        }

    // ----- ParameterizedBuilder methods ------------------------------------

    /**
     * Realize {@link SSLSocketProviderDefaultDependencies} from this builder
     *
     * @param resolver        a resolver
     * @param loader          class loader
     * @param listParameters  parameter list
     *
     * @return SSLSocketProviderDefaultDependencies
     */
    @Override
    public SSLSocketProviderDefaultDependencies realize(ParameterResolver resolver, ClassLoader loader,
        ParameterList listParameters)
        {
        return realize();
        }

    // ----- helpers ---------------------------------------------------------

    /**
     * Utility method for loading a keystore.
     *
     * @param sURL              the URL of the keystore to load
     * @param passwordProvider  the opitonal password for the keystore (passwordProvider)
     * @param sType             the keystore type
     *
     * @throws GeneralSecurityException on keystore access error
     * @throws IOException on I/O error
     *
     * @return the keystore
     */
    private KeyStore loadKeyStore(String sURL, PasswordProvider passwordProvider, String sType)
            throws GeneralSecurityException, IOException
        {
        if (sURL == null || sURL.length() == 0)
            {
            return null;
            }

        KeyStore    keyStore    = KeyStore.getInstance(sType);
        InputStream in          = null;
        char[]      achPassword = null;

        try
            {
            ClassLoader loader = this.getClass().getClassLoader();

            in = loader.getResourceAsStream(new URL(sURL).getFile());

            if (in == null)
                {
                in = new URL(sURL).openStream();
                }

            achPassword = passwordProvider.get();

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
                catch (IOException e)
                    {
                    }
                }
            //Zero the password[]
            if (achPassword != null)
                {
                Arrays.fill(achPassword, '0');
                }
            }

        return keyStore;
        }

    // ----- inner classes ---------------------------------------------------

    /**
     * key-store configuration
     */
    public interface KeystoreDependencies
        {
        /**
         * get URL
         *
         * @return url
         */
        public String getURL();

        /**
         * get key-store type, defaults to JKS
         *
         * @return get key-store type
         */
        public String getType();

        /**
         * Get passwordProvider for this Manager.
         *
         * @return passwordProvider for this Manager.
         */
        PasswordProvider getPasswordProvider();
        }

    /**
     * trust-manager or identity-manager configuration
     */
    public interface ManagerDependencies
        {
        /**
         * Get algorithm name for this {@link ManagerDependencies}
         *
         * @return algorithm name
         */
        String getAlgorithm();

        /**
         * Get provider builder for this {@link ManagerDependencies}
         *
         * @return provider builder
         */
        ProviderBuilder getProviderBuilder();

        /**
         * Get {@link KeystoreDependencies} for this {@link ManagerDependencies}
         *
         * @return {@link KeystoreDependencies} representing configured/defaulted values for keystore
         */
        DefaultKeystoreDependencies getKeystoreDependencies();

        /**
         * Get passwordProvider for this Manager.
         *
         * @return passwordProvider for this Manager.
         */
        PasswordProvider getPasswordProvider();
        }

    /**
     * key-store config and defaults
     */
    public static class DefaultKeystoreDependencies
            implements KeystoreDependencies
        {
        // ----- KeystoreDependencies methods --------------------------------

        /**
         * Get the configured/defaulted keystore url.
         *
         * @return the keystore url.
         */
        @Override
        public String getURL()
            {
            return m_sURL;
            }

        /**
         * Get the configured/defaulted keystore defaults.
         *
         * @return the keystore type
         */
        @Override
        public String getType()
            {
            return m_sType;
            }

        /**
         * Get the configured keystore passwordProvider.
         *
         * @return keystore passwordProvider.
         */
        @Override
        public PasswordProvider getPasswordProvider()
            {
            if (null == m_passProvider)
                {
                ParameterizedBuilder<PasswordProvider> bldr =
                        PasswordProviderBuilderProcessor.getNullPasswordProviderBuilder();
                m_passProvider = bldr.realize(null, null, null);
                }
            return m_passProvider;
            }

        // ----- DefaultKeystoreDependencies methods -------------------------

        /**
         * Set the keystore dependencies url.
         *
         * @param sURL  keystore url
         */
        @Injectable("url")
        public void setURL(String sURL)
            {
            m_sURL = sURL;
            }

        /**
         * Set the keystore type.
         *
         * @param sType the keystore type
         */
        @Injectable("type")
        public void setType(String sType)
            {
            m_sType = sType;
            }

        /**
         * Set the keystore password using a PasswordProvider.
         *
         * @param sPassword the keystore password
         */
        @Injectable("password")
        public void setPassword(String sPassword)
            {
            ParameterizedBuilder<PasswordProvider> bldr =
                    PasswordProviderBuilderProcessor.getPasswordProviderBuilderForPasswordStr(sPassword);
            m_passProvider = bldr.realize(null, null, null);
            }

        /**
         * Set the keystore password-provider
         *
         * @param bldrPassProvider the keystore password provider
         */
        @Injectable("password-provider")
        public void setPasswordProvider(ParameterizedBuilder<PasswordProvider> bldrPassProvider)
            {
            ParameterizedBuilder<PasswordProvider> bldr =
                    bldrPassProvider == null
                    ? PasswordProviderBuilderProcessor.getNullPasswordProviderBuilder()
                    : bldrPassProvider;
            m_passProvider = bldr.realize(null, null, null);
            }

        // ----- data members ------------------------------------------------

        /**
         * passwordProvider for keyStore to fetch password
         */
        private PasswordProvider m_passProvider;

        /**
         * keystore url
         */
        private String m_sURL;

        /**
         * keystore type
         */
        private String m_sType = SSLSocketProviderDefaultDependencies.DEFAULT_KEYSTORE_TYPE;
        }

    /**
     * Represents either identity-manager or trust-manager config and defaults.
     */
    static public class DefaultManagerDependencies
            implements ManagerDependencies
        {
        // ----- constructors ------------------------------------------------

        /**
         * Constructs {@link DefaultManagerDependencies}
         *
         * @param sNameManagerKind either identity-manager or trust-manager
         */
        public DefaultManagerDependencies(String sNameManagerKind)
            {
            f_sNameManagerKind = sNameManagerKind;
            }

        /**
         * Get algorithm
         *
         * @return configured algorithm or default
         */
        @Override
        public String getAlgorithm()
            {
            if (m_sAlgorithm == null)
                {
                // compute default
                if (f_sNameManagerKind.equals("trust-manager"))
                    {
                    m_sAlgorithm = SSLSocketProviderDefaultDependencies.DEFAULT_TRUST_ALGORITHM;
                    }
                else if (f_sNameManagerKind.equals("identity-manager"))
                    {
                    m_sAlgorithm = SSLSocketProviderDefaultDependencies.DEFAULT_IDENTITY_ALGORITHM;
                    }
                else
                    {
                    throw new IllegalArgumentException("unknown manager: " + f_sNameManagerKind + "; expected either identity-manager or trust-manager");
                    }
                }
            return m_sAlgorithm;
            }

        /**
         * get key-store provider builder
         *
         * @return provider builder
         */
        @Override
        public ProviderBuilder getProviderBuilder()
            {
            return m_bldrProvider;
            }

        /**
         * get manager keystore dependencies
         *
         * @return key-store dependencies
         */
        @Override
        public DefaultKeystoreDependencies getKeystoreDependencies()
            {
            return m_depsKeystore;
            }

        /**
         * Get the configured keystore passwordProvider.
         *
         * @return keystore passwordProvider.
         */
        @Override
        public PasswordProvider getPasswordProvider()
            {
            if (null == m_passProvider)
                {
                ParameterizedBuilder<PasswordProvider> bldr =
                        PasswordProviderBuilderProcessor.getNullPasswordProviderBuilder();
                m_passProvider = bldr.realize(null, null, null);
                }
            return m_passProvider;
            }

        /**
         * set key-store password using a PasswordProvider
         *
         * @param sPassword password
         */
        @Injectable("password")
        public void setPassword(String sPassword)
            {
            ParameterizedBuilder<PasswordProvider> bldr =
                    PasswordProviderBuilderProcessor.getPasswordProviderBuilderForPasswordStr(sPassword);
            m_passProvider = bldr.realize(null, null, null);
            }

        /**
         * set key-store password-provider
         *
         * @param bldrPasswordProvider password-provider builder
         */
        @Injectable("password-provider")
        public void setPasswordProvider(ParameterizedBuilder<PasswordProvider> bldrPasswordProvider)
            {
            ParameterizedBuilder<PasswordProvider> bldr =
                        bldrPasswordProvider == null
                        ? PasswordProviderBuilderProcessor.getNullPasswordProviderBuilder()
                        : bldrPasswordProvider;
            m_passProvider = bldr.realize(null, null, null);
            }

        /**
         * set key-store algorithm
         *
         * @param sAlgorithm algorithm
         */
        @Injectable("algorithm")
        public void setAlgorithm(String sAlgorithm)
            {
            this.m_sAlgorithm = sAlgorithm;
            }

        /**
         * set key-store dependencies
         *
         * @param deps key-store configured and defaulted dependencies
         */
        @Injectable("key-store")
        public void setKeystore(DefaultKeystoreDependencies deps)
            {
            m_depsKeystore = deps;
            }

        /**
         * set manager provider builder
         *
         * @param m_bldrProvider provider builder
         */
        @Injectable("provider")
        public void setProviderBuilder(ProviderBuilder m_bldrProvider)
            {
            this.m_bldrProvider = m_bldrProvider;
            }

        // ----- constants -------------------------------------------------------

        /**
         * Either identity-manager or trust-manager.
         */
        private final String                f_sNameManagerKind;

        // ----- data members ----------------------------------------------------

        private ProviderBuilder             m_bldrProvider;
        private DefaultKeystoreDependencies m_depsKeystore;
        private String                      m_sAlgorithm;
        private PasswordProvider            m_passProvider;
        }

    /**
     * HostnameVerifier dependencies
     */
    static public class HostnameVerifierBuilder
            implements ParameterizedBuilder<HostnameVerifier>
        {
        /**
         * The action to take on a name mismatch.
         *
         * @param sAction action to take on a name mismatch
         */
        @Injectable("action")
        public void setAction(String sAction)
            {
            m_sAction = sAction;
            }

        /**
         * The action to take on a name mismatch.
         *
         * @return the action to take on a name mismatch
         */
        public String getAction()
            {
            return m_sAction;
            }

        @Injectable("instance")
        public void setBuilder(ParameterizedBuilder<HostnameVerifier> builder)
            {
            m_builder = builder;
            }

        @Override
        public HostnameVerifier realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
            {
            if (m_builder != null)
                {
                // A custom verifier was specified so use it
                return m_builder.realize(resolver, loader, listParameters);
                }
            else if (ACTION_ALLOW.equals(m_sAction))
                {
                // the action was set to allow - so allow all connections
                return (s, sslSession) -> true;
                }
            // the action is "default" or no builder or action was specified - so use the default verifier
            return new DefaultHostnameVerifier();
            }

        // ----- data members ------------------------------------------------

        private String m_sAction;

        private ParameterizedBuilder<HostnameVerifier> m_builder;
        }

    /**
     * The default {@link HostnameVerifier} to use if none is specified in the configuration.
     * <p>
     * This {@link HostnameVerifier} verifies the CommonName attribute of the peer certificate's SubjectDN or
     * the DNSNames of the peer certificate's SubjectAlternativeNames extension against the url hostname.
     * The certificate attribute must (case insensitively) match the url hostname.
     * <p>
     * The SubjectDN CommonName attribute is verified first, and if successful, the SubjectAlternativeNames
     * attributes are not verified.  If the peer certificate doesn't have a SubjectDN, or the SubjectDN doesn't have
     * a CommonName attribute, then the SubjectAlternativeName attributes of type DNSNames are compared to the
     * url hostname. The first successful comparison to a DNSName causes this method to return true without comparing
     * any other DNSNames.
     * <p>
     * To verify successfully the url hostname must be case-insensitively equal to the certificate attribute being
     * compared.
     * <p>
     * Alternatively, this method will return true if one of the following is true:
     * <ul>
     * <li>the SSL session's peer certificate's SubjectDN CommonName attribute is equal to
     *     the local machine's hostname AND the local machine's hostname or ip address
     *     matches the url hostname parameter.
     * <li>the url hostname parameter can be verified to be a loopback address or the local hostname.
     * </ul>
     */
    static class DefaultHostnameVerifier
            implements HostnameVerifier
        {
        @Override
        public boolean verify(String sUrlHostname, SSLSession sslSession)
            {
            boolean fMatched = false;

            if (sUrlHostname != null && sUrlHostname.length() > 0 && sslSession != null)
                {
                // non-wildcard SAN DNS Names
                Collection<String> colSubAltNames = SSLCertUtility.getDNSSubjAltNames(sslSession, false, true);
                String             sCertHostname  = SSLCertUtility.getCommonName(sslSession);
                if (colSubAltNames != null && colSubAltNames.size() > 0)
                    {
                    fMatched = VERIFY_CN_AFTER_SAN
                            ? doDNSSubjAltNamesVerify(sUrlHostname, colSubAltNames) || doVerify(sUrlHostname, sCertHostname)
                            : doDNSSubjAltNamesVerify(sUrlHostname, colSubAltNames);
                    }
                else
                    {
                    // seek match against CN if there are no non-wildcard DNS Names
                    fMatched = doVerify(sUrlHostname, sCertHostname);
                    }
                }

            return fMatched;
            }

        private boolean doVerify(String sUrlHostname, String sCertHostname)
            {
            if (sCertHostname == null || sCertHostname.length() == 0)
                {
                return false;
                }

            if (sUrlHostname.equalsIgnoreCase(sCertHostname))
                {
                return true;
                }

            try
                {
                // get this machine's local host's host name
                InetAddress addrLocalhost = InetAddressHelper.getLocalHost();
                String      sHostname     = addrLocalhost.getHostName();

                // see if the cert's hostname matches this machine's local host's host name
                if (sHostname.equalsIgnoreCase(sCertHostname))
                    {
                    // it does.  if the url maps to this machine, then we're the same

                    // first see if the local hostname happens to be an ipaddress
                    if (addrLocalhost.getHostAddress().equalsIgnoreCase(sUrlHostname))
                        {
                        return true;
                        }

                    // need to figure out if the urlhostname is "localhost" or "127.0.0.1"
                    // there are two approaches:
                    //
                    // a) look for these strings explicitly
                    //    positive : quick - avoids DNS
                    //    negative : what if a machine configures the loopback addr a different way?
                    //
                    // b) map the urlhostname to an InetAddress and see if it's the loopback addr
                    //    positive : doesn't matter how machine's loopback addr is configured
                    //    negative : uses DNS (but this should be fast since whoever called the
                    //               hostname verifier probably already did something with the url
                    //    negative : is it possible to hack DNS to make another ipaddress look
                    //               like localhost?  I'm assuming the answer is NO.
                    //
                    // The decision is that if the server allows reverse DNS lookups, use (b)
                    // since it covers cases where the customer can have an alternate hostname
                    // for localhost.  If reverse DNS lookups are not allowed, then use (a).
                    //
                    // Note that we do this check as a last resort - this should help with
                    // performance.

                    if (LOCALHOST_HOSTNAME.equalsIgnoreCase(sUrlHostname) ||
                            LOCALHOST_IPADDRESS.equalsIgnoreCase(sUrlHostname))
                        {
                        return true;
                        }
                    }
                }
            catch (UnknownHostException e)
                {
                Logger.err("HostnameVerifier: " + e.getMessage());
                }
            return false;
            }

        // match against non-wildcard DNS names
        private boolean doDNSSubjAltNamesVerify(String urlhostname, Collection<String> dnsAltNames)
            {
            if (dnsAltNames != null && (!dnsAltNames.isEmpty()))
                {
                // peer cert has DNS subject alternative names, check them.
                for (String dnsName : dnsAltNames)
                    {
                    if (dnsName.equalsIgnoreCase(urlhostname))
                        {
                        return true;
                        }
                    }
                }
            return false;
            }
        }

    /**
     * Provider dependencies
     */
    static public class ProviderBuilder
            implements ParameterizedBuilder<Provider>
        {
        /**
         * Provider name
         *
         * @param sName named provided
         */
        @Injectable("name")
        public void setName(String sName)
            {
            m_sName = sName;
            if (! m_fRegisteredCoherenceSecurityProvider && "CoherenceSecurityProvider".equals(sName))
                {
                // make sure the Coherence security provider is loaded since it has been referenced.
                SecurityProvider.ensureRegistration();
                }
            }

        /**
         * Referenced provider name
         *
         * @return provider name
         */
        public String getName()
            {
            return m_sName;
            }

        /**
         * Customized provider builder
         *
         * @param builder provider builder
         */
        @Injectable("provider")
        public void setBuilder(ParameterizedBuilder<Provider> builder)
            {
            m_builder = builder;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Provider realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
            {
            return m_builder == null ? null : m_builder.realize(resolver, loader, listParameters);
            }

        // ----- data members ------------------------------------------------

        private String                         m_sName;
        private ParameterizedBuilder<Provider> m_builder;
        static private boolean                 m_fRegisteredCoherenceSecurityProvider = false;
        }

    /**
     * SSL encipher-suites and protocol-versions are both a list of names with a usage attribute of the value "white-list" or "black-list"
     */
    static public class NameListDependencies
        {
        // ----- constructors ----------------------------------------------------

        public NameListDependencies(String sDescription)
            {
            f_sDescription = sDescription;
            }

        public static enum USAGE
            {
            WHITE_LIST("white-list"),
            BLACK_LIST("black-list");

            private USAGE(String s)
            {
            f_value = s;
            }

            public String toString()
            {
            return f_value;
            }

            public static USAGE myValueOf(String v)
                {
                if ("white-list".equals(v))
                    {
                    return WHITE_LIST;
                    }
                else if ("black-list".equals(v))
                    {
                    return BLACK_LIST;
                    }
                else
                    {
                    throw new IllegalArgumentException("unknown usage value of " + v + "; expected either \"white-list\" or \"black-list\"");
                    }
                }

            public boolean equalsName(String otherName)
                {
                return (otherName == null) ? false : f_value.equals(otherName);
                }

            // ----- constants ---------------------------------------------------

            private final String f_value;
            };

        // ----- NameListDependencies methods ----------------------------------------


        public void add(String sName)
            {
            m_lstNames.add(sName);
            }

        public List<String> getNameList()
            {
            return m_lstNames;
            }

        public void setUsage(String v)
            {
            m_usage = USAGE.myValueOf(v);
            }

        public boolean isBlackList()
            {
            return m_usage == USAGE.BLACK_LIST;
            }

        // ----- constants -------------------------------------------------------

        final String f_sDescription;

        static public final USAGE USAGE_DEFAULT = USAGE.WHITE_LIST;

        // ----- data members ----------------------------------------------------

        private List<String> m_lstNames = new LinkedList<>();
        private USAGE        m_usage    = USAGE_DEFAULT;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The value of the hostname-verifier action to allow all connections.
     */
    public static final String ACTION_ALLOW = "allow";

    // RFC 6125 indicates not to seek a match against CN if SAN DNS Names are present
    // Setting this system property to true allows CN to be checked if SAN is present but has no match
    private final static boolean VERIFY_CN_AFTER_SAN = Config.getBoolean("coherence.security.ssl.verifyCNAfterSAN", true);

    private final static String LOCALHOST_HOSTNAME = "localhost";

    private final static String LOCALHOST_IPADDRESS = "127.0.0.1";

    // ----- data members ---------------------------------------------------

    /**
     * Delegate socket provider builder
     */
    private SocketProviderBuilder                  m_bldrDelegateSocketProvider;

    /**
     * Customized executor or default executors
     */
    private ParameterizedBuilder<Executor>         m_bldrExecutor;

    /**
     * Hostname verifier builder
     */
    private ParameterizedBuilder<HostnameVerifier> m_bldrHostnameVerifier;

    /**
     * Provider buidler
     */
    private ProviderBuilder                        m_bldrProvider;

    /**
     * Dependencies that are being built up.
     */
    private SSLSocketProviderDefaultDependencies   m_deps;

    /**
     * cipher suites white-list, black-list or null to use defaults.
     */
    private NameListDependencies                   m_depsCipherSuite;

    /**
     * Identity manager config and defaults.
     */
    private DefaultManagerDependencies             m_depsIdentityManager;

    /**
     * protocol versions white-list, black-list or null to use defaults.
     */
    private NameListDependencies                   m_depsProtocolVersion;

    /**
     * Trust manager config and/or defaults
     */
    private ManagerDependencies                    m_depsTrustManager;

    /**
     * Realize once since sensitive password data is cleared after dependencies are realized.
     */
    private boolean                                m_fRealized;

    /**
     * SSL Socket provider protocol.
     */
    private String                                 m_sNameProtocol;
    }
