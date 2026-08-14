/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.security;

import com.tangosol.net.security.IdentityAsserter;
import com.tangosol.net.security.IdentityTransformer;
import com.tangosol.net.security.Authorizer;

import com.oracle.coherence.common.util.Duration;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry;
import com.tangosol.coherence.config.Config;

import com.tangosol.net.ClusterDependencies;
import com.tangosol.net.PasswordProvider;
import com.tangosol.net.security.StorageAccessAuthorizer;

import com.tangosol.internal.util.CoherenceMode;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

/**
 * LegacyXmlSecurityHelper parses the {@code <security-config>} XML to
 * populate the DefaultSecurityDependencies.
 *
 * NOTE: This code will eventually be replaced by CODI.
 *
 * @author der  2011.12.01
 * @since Coherence 12.1.2
 */
@SuppressWarnings("deprecation")
public class LegacyXmlSecurityHelper
    {
    /**
     * Populate the DefaultSecurityDependencies object from the XML
     * configuration.
     *
     * @param xml   the <{@code <security-config>} XML element
     * @param deps  the DefaultSecurityDependencies to be populated
     *
     * @return the DefaultSecurityDependencies object that was passed in.
     */
    public static DefaultSecurityDependencies fromXml(XmlElement xml, DefaultSecurityDependencies deps)
        {
        return fromXml(xml, deps, null);
        }

    /**
     * Populate security dependencies, including the bounded PEER proof path.
     */
    public static DefaultSecurityDependencies fromXml(XmlElement xml, DefaultSecurityDependencies deps,
            ClusterDependencies depsCluster)
        {
        deps.setEnabled(xml.getSafeElement("enabled").getBoolean(deps.isEnabled()));
        deps.setSubjectScoped(xml.getSafeElement("subject-scope").getBoolean(deps.isSubjectScoped()));
        deps.setModel(xml.getSafeElement("model").getString(deps.getModel()));

        XmlElement xmlIdentityAsserter    = xml.findElement("identity-asserter");
        XmlElement xmlIdentityTransformer = xml.findElement("identity-transformer");
        XmlElement xmlAuthorizer          = xml.findElement("authorizer");

        if (xmlIdentityAsserter != null)
            {
            deps.setIdentityAsserter((IdentityAsserter) XmlHelper.createInstance(
                    xmlIdentityAsserter, null, null));
            }

        if (xmlIdentityTransformer != null)
            {
            deps.setIdentityTransformer((IdentityTransformer) XmlHelper.createInstance(
                    xmlIdentityTransformer, null, null));
            }

        if (xmlAuthorizer != null)
            {
            deps.setAuthorizer((Authorizer) XmlHelper.createInstance(
                    xmlAuthorizer, null, null));
            }

        XmlElement xmlPeerProof = xml.findElement("peer-proof");
        if (xmlPeerProof != null)
            {
            deps.setPeerProofProvider(new X509PeerProofProvider(parsePeerProof(xmlPeerProof, depsCluster)));
            }

        return deps;
        }

    private static PeerProofDependencies parsePeerProof(XmlElement xml, ClusterDependencies depsCluster)
        {
        if (depsCluster == null)
            {
            throw new IllegalArgumentException("peer-proof requires cluster dependencies");
            }

        PeerProofDependencies deps = new PeerProofDependencies()
                .setProviderId(xml.getSafeElement("provider").getString())
                .setAlgorithmId(xml.getSafeElement("algorithm").getString())
                .setRefreshMillis(new Duration(xml.getSafeElement("refresh-period").getString("1m"))
                        .as(Duration.Magnitude.MILLI));

        XmlElement xmlIdentity = xml.getSafeElement("identity");
        XmlElement xmlStore = xmlIdentity.getSafeElement("key-store");
        deps.setIdentityStoreUrl(xmlStore.getSafeElement("url").getString())
                .setIdentityStoreType(xmlStore.getSafeElement("type").getString("PKCS12"))
                .setIdentityAlias(xmlIdentity.getSafeElement("alias").getString())
                .setIdentityPasswordProvider(resolvePasswordProvider(xmlStore, depsCluster, "identity"));

        parseAuthorities(xml.getSafeElement("subject-authorities"), true, deps, depsCluster);
        parseAuthorities(xml.getSafeElement("senior-authorities"), false, deps, depsCluster);
        deps.setSubjectProofRequired(CoherenceMode.isSecurityHardeningEnabled()
                        && isSubjectProofRequired(depsCluster.getBuilderRegistry()))
                .setSeniorMetadataProofRequired(Config.getBoolean(
                        "coherence.security.peer.senior-metadata-proof.required", false));
        return deps.validate();
        }

    private static boolean isSubjectProofRequired(ParameterizedBuilderRegistry registry)
        {
        for (ParameterizedBuilderRegistry.Registration registration : registry)
            {
            if (StorageAccessAuthorizer.class.equals(registration.getInstanceClass())
                    && registration.getBuilder() instanceof StorageAccessAuthorizerBuilder
                    && ((StorageAccessAuthorizerBuilder) registration.getBuilder()).isSubjectProofRequired())
                {
                return true;
                }
            }
        return false;
        }

    private static void parseAuthorities(XmlElement xml, boolean fSubject, PeerProofDependencies deps,
            ClusterDependencies depsCluster)
        {
        java.util.Iterator iter = xml.getElements("cert");
        while (iter.hasNext())
            {
            String sUrl = ((XmlElement) iter.next()).getString();
            if (fSubject) {deps.addSubjectCertificateUrl(sUrl);} else {deps.addSeniorCertificateUrl(sUrl);}
            }

        XmlElement xmlStore = xml.findElement("key-store");
        if (xmlStore != null)
            {
            PeerProofDependencies.AuthorityStore store = new PeerProofDependencies.AuthorityStore(
                    xmlStore.getSafeElement("url").getString(), xmlStore.getSafeElement("type").getString(),
                    resolvePasswordProvider(xmlStore, depsCluster, fSubject ? "subject" : "senior"));
            if (fSubject) {deps.setSubjectAuthorityStore(store);} else {deps.setSeniorAuthorityStore(store);}
            }
        }

    private static PasswordProvider resolvePasswordProvider(XmlElement xml, ClusterDependencies depsCluster,
            String sRole)
        {
        XmlElement xmlProvider = xml.findElement("password-provider");
        String sName = xmlProvider == null ? "" : xmlProvider.getSafeElement("name").getString().trim();
        if (sName.isEmpty())
            {
            throw new IllegalArgumentException("peer-proof " + sRole + " store requires a named PasswordProvider");
            }
        ParameterizedBuilderRegistry registry = depsCluster.getBuilderRegistry();
        ParameterizedBuilder<PasswordProvider> builder = registry.getBuilder(PasswordProvider.class, sName);
        if (builder == null)
            {
            throw new IllegalArgumentException("unknown peer-proof PasswordProvider name: " + sName);
            }
        return builder.realize(null, null, null);
        }
    }
