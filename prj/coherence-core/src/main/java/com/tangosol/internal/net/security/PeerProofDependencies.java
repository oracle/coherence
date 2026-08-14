/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.security;

import com.tangosol.net.PasswordProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bounded configuration for the built-in X.509 PEER proof provider.
 *
 * @author Aleks Seovic  2026.07.15
 * @since 26.1.0.0
 */
public class PeerProofDependencies
    {
    public String getProviderId() {return m_sProviderId;}
    public PeerProofDependencies setProviderId(String s) {m_sProviderId = normalize(s); return this;}
    public String getIdentityStoreUrl() {return m_sIdentityStoreUrl;}
    public PeerProofDependencies setIdentityStoreUrl(String s) {m_sIdentityStoreUrl = normalize(s); return this;}
    public String getIdentityStoreType() {return m_sIdentityStoreType;}
    public PeerProofDependencies setIdentityStoreType(String s) {m_sIdentityStoreType = normalize(s); return this;}
    public String getIdentityAlias() {return m_sIdentityAlias;}
    public PeerProofDependencies setIdentityAlias(String s) {m_sIdentityAlias = normalize(s); return this;}
    public PasswordProvider getIdentityPasswordProvider() {return m_passwordIdentity;}
    public PeerProofDependencies setIdentityPasswordProvider(PasswordProvider p) {m_passwordIdentity = p; return this;}
    public List<String> getSubjectCertificateUrls() {return Collections.unmodifiableList(m_listSubjectCertUrls);}
    public PeerProofDependencies addSubjectCertificateUrl(String s) {m_listSubjectCertUrls.add(require(s, "subject authority certificate URL")); return this;}
    public List<String> getSeniorCertificateUrls() {return Collections.unmodifiableList(m_listSeniorCertUrls);}
    public PeerProofDependencies addSeniorCertificateUrl(String s) {m_listSeniorCertUrls.add(require(s, "senior authority certificate URL")); return this;}
    public AuthorityStore getSubjectAuthorityStore() {return m_storeSubject;}
    public PeerProofDependencies setSubjectAuthorityStore(AuthorityStore s) {m_storeSubject = s; return this;}
    public AuthorityStore getSeniorAuthorityStore() {return m_storeSenior;}
    public PeerProofDependencies setSeniorAuthorityStore(AuthorityStore s) {m_storeSenior = s; return this;}
    public String getAlgorithmId() {return m_sAlgorithmId;}
    public PeerProofDependencies setAlgorithmId(String s) {m_sAlgorithmId = normalize(s); return this;}
    public long getRefreshMillis() {return m_cRefreshMillis;}
    public PeerProofDependencies setRefreshMillis(long c) {m_cRefreshMillis = c; return this;}
    public boolean isSubjectProofRequired() {return m_fSubjectProofRequired;}
    public PeerProofDependencies setSubjectProofRequired(boolean f) {m_fSubjectProofRequired = f; return this;}
    public boolean isSeniorMetadataProofRequired() {return m_fSeniorMetadataProofRequired;}
    public PeerProofDependencies setSeniorMetadataProofRequired(boolean f) {m_fSeniorMetadataProofRequired = f; return this;}

    /** Validate the closed first-profile configuration. */
    public PeerProofDependencies validate()
        {
        if (!X509PeerProofProvider.PROVIDER_ID.equals(m_sProviderId))
            {
            throw new IllegalArgumentException("unknown peer-proof provider: " + m_sProviderId);
            }
        if (!X509PeerProofProvider.ALGORITHM_ID.equals(m_sAlgorithmId))
            {
            throw new IllegalArgumentException("unsupported peer-proof algorithm: " + m_sAlgorithmId);
            }
        require(m_sIdentityStoreUrl, "identity key-store URL");
        require(m_sIdentityStoreType, "identity key-store type");
        if (!("JKS".equalsIgnoreCase(m_sIdentityStoreType) || "PKCS12".equalsIgnoreCase(m_sIdentityStoreType)))
            {
            throw new IllegalArgumentException("peer-proof identity key-store type must be JKS or PKCS12");
            }
        require(m_sIdentityAlias, "identity alias");
        if (m_passwordIdentity == null)
            {
            throw new IllegalArgumentException("peer-proof identity requires a named PasswordProvider");
            }
        validateAuthorities("subject", m_listSubjectCertUrls, m_storeSubject);
        validateAuthorities("senior", m_listSeniorCertUrls, m_storeSenior);
        if (m_cRefreshMillis != 60_000L)
            {
            throw new IllegalArgumentException("peer-proof refresh-period must be 1m");
            }
        return this;
        }

    private static void validateAuthorities(String sRole, List<String> list, AuthorityStore store)
        {
        if (list.isEmpty() == (store == null))
            {
            throw new IllegalArgumentException("peer-proof " + sRole
                    + " authorities require exactly one of cert or key-store");
            }
        if (store != null)
            {
            store.validate(sRole);
            }
        }

    private static String normalize(String s) {return s == null ? "" : s.trim();}
    private static String require(String s, String n)
        {
        s = normalize(s);
        if (s.isEmpty()) {throw new IllegalArgumentException("missing peer-proof " + n);}
        return s;
        }

    /** Protected JKS/PKCS12 authority-store descriptor. */
    public static class AuthorityStore
        {
        public AuthorityStore(String sUrl, String sType, PasswordProvider password)
            {m_sUrl = normalize(sUrl); m_sType = normalize(sType); m_password = password;}
        public String getUrl() {return m_sUrl;}
        public String getType() {return m_sType;}
        public PasswordProvider getPasswordProvider() {return m_password;}
        private void validate(String sRole)
            {
            require(m_sUrl, sRole + " authority key-store URL");
            if (!("JKS".equalsIgnoreCase(m_sType) || "PKCS12".equalsIgnoreCase(m_sType)))
                {throw new IllegalArgumentException("peer-proof " + sRole + " authority store must be JKS or PKCS12");}
            if (m_password == null)
                {throw new IllegalArgumentException("peer-proof " + sRole + " authority store requires a named PasswordProvider");}
            }
        private final String m_sUrl;
        private final String m_sType;
        private final PasswordProvider m_password;
        }

    private String m_sProviderId = X509PeerProofProvider.PROVIDER_ID;
    private String m_sIdentityStoreUrl;
    private String m_sIdentityStoreType = "PKCS12";
    private String m_sIdentityAlias;
    private PasswordProvider m_passwordIdentity;
    private final List<String> m_listSubjectCertUrls = new ArrayList<>();
    private final List<String> m_listSeniorCertUrls = new ArrayList<>();
    private AuthorityStore m_storeSubject;
    private AuthorityStore m_storeSenior;
    private String m_sAlgorithmId = X509PeerProofProvider.ALGORITHM_ID;
    private long m_cRefreshMillis = 60_000L;
    private boolean m_fSubjectProofRequired;
    private boolean m_fSeniorMetadataProofRequired;
    }
