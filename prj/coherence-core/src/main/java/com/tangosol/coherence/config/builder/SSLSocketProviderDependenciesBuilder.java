/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.internal.net.ssl.SSLCertUtility;
import com.oracle.coherence.common.net.SSLSocketProvider;
import com.oracle.coherence.common.net.SocketProvider;

import com.oracle.coherence.common.util.Duration;

import com.tangosol.coherence.config.Config;
import com.tangosol.coherence.config.ParameterList;

import com.tangosol.coherence.config.unit.Seconds;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.internal.net.ssl.DefaultManagerDependencies;
import com.tangosol.internal.net.ssl.ManagerDependencies;
import com.tangosol.internal.net.ssl.SSLContextDependencies;
import com.tangosol.internal.net.ssl.SSLContextProvider;
import com.tangosol.internal.net.ssl.SSLSocketProviderDefaultDependencies;

import com.tangosol.net.InetAddressHelper;
import com.tangosol.net.SocketProviderFactory;
import com.tangosol.net.security.SecurityProvider;

import com.tangosol.net.ssl.RefreshPolicy;

import java.net.InetAddress;

import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.SecureRandom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import java.util.concurrent.Executor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

/**
 * {@link SSLSocketProviderDependenciesBuilder} enables lazy instantiation of SSL SocketProvider.
 * <p>
 * This builder includes methods that allows one to specify whether to get a datagram or demultiplexed
 * {@link SocketProvider} and what subport to use for the socketprovider.
 *
 * @author jf  2015.11.11
 * @author Jonathan Knight 2022.01.25
 *
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
        m_bldrDelegateSocketProvider = new SocketProviderBuilder(SocketProviderFactory.DEFAULT_SOCKET_PROVIDER, false);
        m_sNameProtocol              = SSLSocketProviderDefaultDependencies.DEFAULT_SSL_PROTOCOL;
        }


    // ----- SSLSocketProviderDependenciesBuilder methods --------------------------------

    /**
     * Set the SSL protocol name
     *
     * @param sName the protocol name
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
    public ProviderBuilder getProviderBuilder()
        {
        return m_bldrProvider;
        }

    /**
     * Realize the SSL provider.
     *
     * @return the SSL provider
     */
    protected Provider realizeProvider()
        {
        return m_bldrProvider == null ? null : m_bldrProvider.realize(null, null, null);
        }

    /**
     * Returns the SSL provider name.
     *
     * @return the SSL provider name
     */
    protected String getProviderName()
        {
        return m_bldrProvider == null ? null : m_bldrProvider.getName();
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
    public ManagerDependencies getIdentityManager()
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
     * Set the client auth mode to use.
     *
     * @param sAuthMode  the client auth mode to use
     */
    @Injectable("client-auth")
    public void setClientAuth(String sAuthMode)
        {
        if (sAuthMode == null || sAuthMode.isEmpty())
            {
            m_clientAuthMode = SSLSocketProvider.ClientAuthMode.none;
            }
        else
            {
            try
                {
                m_clientAuthMode = SSLSocketProvider.ClientAuthMode.valueOf(sAuthMode);
                }
            catch (IllegalArgumentException e)
                {
                throw new IllegalArgumentException("Invalid client auth configuration", e);
                }
            }
        }

    /**
     * Set the auto-refresh period.
     *
     * @param refreshPeriod  the period to use to auto-refresh keys and certs
     */
    @Injectable("refresh-period")
    public void setRefreshPeriod(Seconds refreshPeriod)
        {
        m_refreshPeriod = refreshPeriod == null ? NO_REFRESH : refreshPeriod;
        }

    /**
     * Return the period to use to auto-refresh keys and certs.
     *
     * @return the period to use to auto-refresh keys and certs
     */
    public Seconds getRefreshPeriod()
        {
        return m_refreshPeriod;
        }

    /**
     * Set the {@link RefreshPolicy} to use to determine whether keys and
     * certs should be refreshed.
     *
     * @param policy  the {@link RefreshPolicy} to use
     */
    @Injectable("refresh-policy")
    public void setRefreshPolicy(RefreshPolicy policy)
        {
        m_refreshPolicy = policy == null ? RefreshPolicy.Always : policy;
        }

    /**
     * Returns the {@link RefreshPolicy} to use to determine whether keys and
     * certs should be refreshed.
     *
     * @return the {@link RefreshPolicy} to use
     */
    public RefreshPolicy getRefreshPolicy()
        {
        return m_refreshPolicy;
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
     * <p>
     * Note: unlike typical builders, this is realized once, since sensitive password data is nullified after realizing.
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
        SSLSocketProviderDefaultDependencies deps = m_deps;

        try
            {
            KeyManager[]        aKeyManager   = null;
            TrustManager[]      aTrustManager = null;
            String              sProtocol     = getProtocol();
            ManagerDependencies depsIdMgr     = getIdentityManager();
            ManagerDependencies depsTrustMgr  = getTrustManager();
            Provider            provider      = realizeProvider();
            String              sProviderName = getProviderName();
            RefreshPolicy       refreshPolicy = deps.getRefreshPolicy();

            if (depsIdMgr != null)
                {
                depsIdMgr.addListener(refreshPolicy);
                }

            if (depsTrustMgr != null)
                {
                depsTrustMgr.addListener(refreshPolicy);
                }

            if (provider instanceof DependenciesAware)
                {
                ((DependenciesAware) provider).setDependencies(deps, depsIdMgr, depsTrustMgr);
                }

            if (m_bldrExecutor == null)
                {
                deps.setExecutor(SSLSocketProviderDefaultDependencies.DEFAULT_EXECUTOR);
                }
            else
                {
                deps.setExecutor(m_bldrExecutor.realize(new NullParameterResolver(), null, null));
                }

            deps.setRefreshPeriod(m_refreshPeriod);
            deps.setRefreshPolicy(m_refreshPolicy);
            deps.setClientAuth(m_clientAuthMode);

            ParameterizedBuilder<HostnameVerifier> bldrHostnameVerifier = getHostnameVerifierBuilder();

            if (bldrHostnameVerifier != null)
                {
                deps.setHostnameVerifier(bldrHostnameVerifier.realize(null, null, null));
                }

            // intialize a random number source
            SecureRandom random = new SecureRandom();
            random.nextInt();

            SSLContextDependencies sslContextDependencies = new SSLContextDependencies(null);
            sslContextDependencies.setProvider(provider, sProviderName);
            sslContextDependencies.setDependencies(deps, depsIdMgr, depsTrustMgr);
            sslContextDependencies.setClientAuth(deps.getClientAuth());
            sslContextDependencies.setRefreshPeriodInMillis(m_refreshPeriod.as(Duration.Magnitude.MILLI));
            sslContextDependencies.setSecureRandom(random);
            deps.setSSLContextDependencies(sslContextDependencies);

            SSLContext ctx = SSLContext.getInstance(sProtocol, new SSLContextProvider(sProtocol, deps));
            deps.setSSLContext(ctx);

            // initialize the SSLContext
            ctx.init(aKeyManager, aTrustManager, random);

            SSLEngine engine = ctx.createSSLEngine();

            if (m_depsCipherSuite != null)
                {
                List<String> listCipher = m_depsCipherSuite.getNameList();

                if (m_depsCipherSuite.isBlackList())
                    {
                    ArrayList<String> listDefaultCipher = new ArrayList<>(Arrays.asList(engine.getEnabledCipherSuites()));
                    listDefaultCipher.removeAll(listCipher);
                    listCipher = listDefaultCipher;
                    }

                deps.setEnabledCipherSuites(listCipher.toArray(new String[0]));
                }

            if (m_depsProtocolVersion != null)
                {
                List<String> listProtocol = m_depsProtocolVersion.getNameList();

                if (m_depsProtocolVersion.isBlackList())
                    {
                    ArrayList<String> listDefaultProtocols = new ArrayList<>(Arrays.asList(engine.getEnabledProtocols()));
                    listDefaultProtocols.removeAll(listProtocol);
                    listProtocol = listDefaultProtocols;
                    }

                deps.setEnabledProtocolVersions(listProtocol.toArray(new String[0]));
                }

            deps.setDelegateSocketProviderBuilder(m_bldrDelegateSocketProvider);

            m_fRealized = true;
            }
        catch (GeneralSecurityException e)
            {
            throw new IllegalArgumentException("Invalid configuration ", e);
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

    // ----- inner interface: DependenciesAware -----------------------------

    /**
     * Implemented by {@link java.security.Provider} classes that need
     * to be aware of the SSL dependencies.
     */
    public interface DependenciesAware
        {
        /**
         * Set the SSL Socket dependencies.
         *
         * @param deps          the {@link SSLSocketProvider.Dependencies socket provider dependencies}
         * @param depsIdMgr     the {@link ManagerDependencies identity
         *                      manager dependencies} or {@code null} if no identity manger has been configured
         * @param depsTrustMgr  the {@link ManagerDependencies trust
         *                      manager dependencies} or {@code null} if no trust manger has been configured
         */
        void setDependencies(SSLSocketProvider.Dependencies deps,
                             ManagerDependencies depsIdMgr,
                             ManagerDependencies depsTrustMgr);
        }

    // ----- inner class: HostnameVerifierBuilder ---------------------------

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

    // ----- inner class: DefaultHostnameVerifier ---------------------------

    /**
     * The default {@link HostnameVerifier} to use if none is specified in the configuration.
     * <p>
     * Verify peer hostname against peer certificate of the SSL session, allowing
     * wildcarded certificates.  Hostname verification has two phases:
     * <ul>
     *   <li>verification with wildcarding
     *   <li>if verification with wildcarding fails, then verification without wildcarding is performed.
     * </ul>
     * <p>
     * Verification with Wildcarding
     * <p>
     * If the peer certificate of the SSL session's peer certificate SubjectDN CommonName
     * attribute supports wildcarding, the CommonName attribute must meet the following:
     * <ul>
     *   <li>the CN must have at least two dot ('.') characters
     *   <li>the CN must start with "*."
     *   <li>the CN can have only one "*" character
     * </ul>
     * In addition, the non-wildcarded portion of the CommonName attribute must equal domain portion
     * of the urlhostname parameter, in a case-sensitive String comparison. The domain portion of
     * the urlhostname parameter string is the urlhostname substring left after the 'hostname'
     * substring is removed.  The 'hostname' portion of the urlhostname is the substring
     * up to and excluding the first '.' of the urlhostname parameter string. For example:
     * <ul>
     *   <li>
     *   urlhostname:  mymachine.oracle.com
     *   CommonName:   *.oracle.com
     *      '.oracle.com' will compare successfully with '.oracle.com'
     *  <li>
     *  urlhostname:  mymachine.uk.oracle.com
     *  CommonName:   *.oracle.com
     *      '.uk.oracle.com' will not compare successfully with '.oracle.com'
     * </ul>
     * <p>
     *  DNSNames obtained from the peer certificate's SubjectAlternativeNames extension may be wildcarded.
     * <br><br>
     * Verification without Wildcarding
     * <br><br>
     * If wildcarded hostname verification fails, this method performs non-wildcarded verification.
     * This verifier verifies the CommonName attribute of the peer certificate's
     * SubjectDN or the DNSNames of the peer certificate's SubjectAlternativeNames
     * extension against the urlhostname.  The certificate attribute must
     * (case insensitively) match the urlhostname.
     * <br><br>
     * The SubjectDN CommonName attribute is verified first, and if successful,
     * the SubjectAlternativeNames attributes are not verified.  If the peer certificate
     * doesn't have a SubjectDN, or the SubjectDN doesn't have a CommonName attribute,
     * then the SubjectAlternativeName attributes of type DNSNames are compared to the
     * urlhostname. The first successful comparison to a DNSName causes this method
     * to return true without comparing any other DNSNames.
     * <br><br>
     * To verify successfully the url hostname must be case-insensitively equal to the
     * certificate attribute being compared.
     * <br><br>
     * Alternatively, this method will return true if one of the following
     * is true:
     * <ul>
     * <li>the SSL session's peer certificate is a WebLogic Demo certificate
     * <li>the SSL session's peer certificate's SubjectDN CommonName attribute is equal to
     *    the local machine's hostname AND the local machine's hostname or ip address
     *    matches the urlhostname parameter.
     * <li>the urlhostname parameter can be verified to be a loopback address or the local hostname.
     * <li>the SSL session's peer certificate is an Oracle Key Store Service (KSS)
     *    Demo certificate for the current Weblogic Server domain and Weblogic Server is
     *    enabled to use KSS Demo certificates.
     * </ul>
     */
    static class DefaultHostnameVerifier
            implements HostnameVerifier
        {
        @Override
        public boolean verify(String sUrlHostname, SSLSession sslSession)
            {
            boolean fMatched = false;

            if (sUrlHostname != null && !sUrlHostname.isEmpty() && sslSession != null)
                {
                Collection<String> colWildcardDNSNames = SSLCertUtility.getDNSSubjAltNames(sslSession, true, false);
                String             sCertHostname       = SSLCertUtility.getCommonName(sslSession);

                if (colWildcardDNSNames != null && !colWildcardDNSNames.isEmpty())
                    {
                    fMatched = VERIFY_CN_AFTER_SAN
                            ? verifySANWildcardDNSNames(sUrlHostname, colWildcardDNSNames)
                                || isLegalWildcarded(sUrlHostname, sCertHostname)
                            : verifySANWildcardDNSNames(sUrlHostname, colWildcardDNSNames);
                    }
                else
                    {
                    // seek match against wildcard CN if SAN is not found
                    fMatched = isLegalWildcarded(sUrlHostname, sCertHostname);
                    }

                if (!fMatched)
                    {
                    // non-wildcard SAN DNS Names
                    Collection<String> colSubAltNames = SSLCertUtility.getDNSSubjAltNames(sslSession, false, true);

                    if (colSubAltNames != null && !colSubAltNames.isEmpty())
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
                }

            if (!fMatched)
                {
                Logger.err("DefaultHostnameVerifier rejecting hostname " + sUrlHostname);
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

            if (sCertHostname.indexOf(".") < 0 && sUrlHostname.indexOf(".") > 0)
                {
                int domainIndex = sUrlHostname.indexOf(".");
                if (domainIndex == sCertHostname.length()
                    && sCertHostname.compareToIgnoreCase(sUrlHostname.substring(0, domainIndex)) == 0)
                    {
                    return true;
                    }

                }

            if (!ALLOW_LOCALHOST)
                {
                return false;
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
        private boolean doDNSSubjAltNamesVerify(String sUrlhostname, Collection<String> colDnsAltNames)
            {
            if (colDnsAltNames != null && (!colDnsAltNames.isEmpty()))
                {
                // peer cert has DNS subject alternative names, check them.
                for (String dnsName : colDnsAltNames)
                    {
                    if (dnsName.equalsIgnoreCase(sUrlhostname))
                        {
                        return true;
                        }
                    }
                }
            return false;
            }

        private static boolean isLegalWildcarded(String sURL, String sCommonName)
            {
            if (sCommonName != null)
                {
                // If the cn doesn't have an asterisk, wildcarding doesn't matter.
                if (!sCommonName.contains("*"))
                    {
                    return false;
                    }
                else
                    {
                    /*
                     * has an asterisk in cn, must pass wildcarding validation, these must be true:
                     *  - cn has two dot characters
                     *  - cn must start with "*."
                     *  - cn must have one '*'
                     *  - domains must match:
                     *      take off cn's '*', leaving the cn domain
                     *      url's domain must match the cn domain
                     *      non-domain part of url cannot have a '.'; eliminates sub-domains.
                     */

                    if ((sCommonName.indexOf(".") != sCommonName.lastIndexOf(".")) &&   // has at least two dots.
                            sCommonName.startsWith("*.") &&
                            (sCommonName.indexOf("*") == sCommonName.lastIndexOf("*")) &&   // allowed one star
                            domainMatchesDomain(sURL, sCommonName))
                        {
                        // passes wildcard validation
                        return true;
                        }
                    }
                }

            return false;
            }

        private static boolean domainMatchesDomain(String sUrl, String sCommonName)
            {
            // strip leading "*" off the cn string to get the remaining domain
            int nIndex = sCommonName.indexOf("*");
            if (nIndex == -1)
                {
                // shouldn't happen, already checked this.
                return false;
                }

            // strip off star and convert cn to lower case
            String sStrippedCN = sCommonName.substring(nIndex + 1).toLowerCase();

            // convert URL to lower case
            String sUrlLower = sUrl.toLowerCase();

            // check that url domain is the same as cn domain
            if (!sUrlLower.endsWith(sStrippedCN))
                {
                return false;
                }

            // now check the non-domain part of the url

            // check that the length we want to strip off the URL is > 0
            if (sUrlLower.lastIndexOf(sStrippedCN) == -1)
                {
                // also shouldn't happen, just checked that above.
                return false;
                }

            // get the beginning (non-domain) part of the url
            String sUrlBeginning = sUrlLower.substring(0, sUrlLower.length() - sStrippedCN.length());
            if (sUrlBeginning.length() <= 0)
                {
                return false;
                }
            if (sUrlBeginning.contains("."))
                {
                // beginning part is supposed to be the host, not a subdomain.  fails.
                return false;
                }

            return true;
            }

        // Match against wildcard DNS names
        private static boolean verifySANWildcardDNSNames(String sUrlHostname, Collection<String> colWildcardDNSNames)
            {
            boolean fMatched = false;

            if (colWildcardDNSNames != null && (!colWildcardDNSNames.isEmpty()))
                {
                Matcher urlHostnameMatcher = URL_HOSTNAME_PATTERN.matcher(sUrlHostname);
                boolean fURLHostnameValid = urlHostnameMatcher.matches();  // valid hostname ends with proper domain name
                for (String sDnsName : colWildcardDNSNames)
                    {
                    Matcher wildCardDNSNameMatcher = WILDCARD_DNSNAME_PATTERN.matcher(sDnsName);
                    if (wildCardDNSNameMatcher.matches())
                        {
                        String sDomainOfWildcardDNS = wildCardDNSNameMatcher.group(1);
                        if (fURLHostnameValid)
                            {
                            String sDomainOfURL = urlHostnameMatcher.group(1);
                            if (sDomainOfWildcardDNS != null && sDomainOfWildcardDNS.equalsIgnoreCase(sDomainOfURL))
                                {
                                fMatched = true;
                                break;
                                }
                            }
                        }
                    }
                }
            return fMatched;
            }
        }

    // ----- inner class: ProviderBuilder -----------------------------------

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

        private String m_sName;

        private ParameterizedBuilder<Provider> m_builder;

        @SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})
        static private boolean m_fRegisteredCoherenceSecurityProvider = false;
        }

    // ----- inner class: NameListDependencies ------------------------------

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

        public enum USAGE
            {
            WHITE_LIST("white-list"),
            BLACK_LIST("black-list");

            USAGE(String s)
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
                return f_value.equals(otherName);
                }

            // ----- constants ---------------------------------------------------

            private final String f_value;
            }

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

        private final List<String> m_lstNames = new LinkedList<>();
        private USAGE              m_usage    = USAGE_DEFAULT;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The name of the Coherence provider.
     */
    public static final String NAME = "CoherenceSSLContextProvider";

    /**
     * The type of the SSLContext service.
     */
    public static final String SERVICE_TYPE = "SSLContext";

    /**
     * The value of the hostname-verifier action to allow all connections.
     */
    public static final String ACTION_ALLOW = "allow";

    // RFC 6125 indicates not to seek a match against CN if SAN DNS Names are present
    // Setting this system property to true allows CN to be checked if SAN is present but has no match
    private final static boolean VERIFY_CN_AFTER_SAN = Config.getBoolean("coherence.security.ssl.verifyCNAfterSAN", true);

    /**
     * Flag, when set to {@code true} allows certificate matching for localhost.
     */
    private final static boolean ALLOW_LOCALHOST = Config.getBoolean("coherence.security.ssl.allowLocalhost", false);

    private final static String LOCALHOST_HOSTNAME = "localhost";

    private final static String LOCALHOST_IPADDRESS = "127.0.0.1";

    private static final String WILDCARD_DNSNAME_REGEX = "^\\*((\\.[^*.]+){2,})$";

    private static final Pattern WILDCARD_DNSNAME_PATTERN = Pattern.compile(WILDCARD_DNSNAME_REGEX);

    private static final String URL_HOSTNAME_REGEX = "^[^*.\\s]+((\\.[^*.]+){2,})$";

    private static final Pattern URL_HOSTNAME_PATTERN = Pattern.compile(URL_HOSTNAME_REGEX);

    /**
     * The default auto-refresh period - no refresh.
     */
    public static final Seconds NO_REFRESH = new Seconds(0);

    // ----- data members ---------------------------------------------------

    /**
     * Delegate socket provider builder
     */
    private SocketProviderBuilder m_bldrDelegateSocketProvider;

    /**
     * Customized executor or default executors
     */
    private ParameterizedBuilder<Executor> m_bldrExecutor;

    /**
     * Hostname verifier builder
     */
    private ParameterizedBuilder<HostnameVerifier> m_bldrHostnameVerifier;

    /**
     * Provider buidler
     */
    private ProviderBuilder m_bldrProvider;

    /**
     * Dependencies that are being built up.
     */
    private final SSLSocketProviderDefaultDependencies m_deps;

    /**
     * cipher suites white-list, black-list or null to use defaults.
     */
    private NameListDependencies m_depsCipherSuite;

    /**
     * Identity manager config and defaults.
     */
    private ManagerDependencies m_depsIdentityManager;

    /**
     * protocol versions white-list, black-list or null to use defaults.
     */
    private NameListDependencies m_depsProtocolVersion;

    /**
     * Trust manager config and/or defaults
     */
    private ManagerDependencies m_depsTrustManager;

    /**
     * Realize once since sensitive password data is cleared after dependencies are realized.
     */
    private boolean m_fRealized;

    /**
     * SSL Socket provider protocol.
     */
    private String m_sNameProtocol;

    /**
     * The client authentication mode.
     */
    private SSLSocketProvider.ClientAuthMode m_clientAuthMode;

    /**
     * The period to use to auto-refresh keys and certs.
     */
    private Seconds m_refreshPeriod = NO_REFRESH;

    /**
     * The {@link RefreshPolicy} to use to determine whether keys and certs should be
     * refreshed when the scheduled refresh time is reached.
     */
    private RefreshPolicy m_refreshPolicy = RefreshPolicy.Always;
    }
