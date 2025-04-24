/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.security;

import com.oracle.bedrock.OptionsByType;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.Platform;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.runtime.coherence.callables.IsCoherenceRunning;

import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.MemberName;
import com.oracle.bedrock.runtime.coherence.options.OperationalOverride;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;

import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.java.options.HeapSize;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.runtime.options.Discriminator;
import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.testsupport.MavenProjectFileUtils;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.testsupport.junit.TestLogsExtension;
import com.oracle.coherence.common.base.Exceptions;

import com.tangosol.net.security.PermissionException;

import com.tangosol.util.Resources;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.net.URL;
import java.util.Set;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CertSecurityTests
    {
    @BeforeAll
    public static void setup() throws Exception
        {
        KeyTool.assertCanCreateKeys();

        File fileBuild = MavenProjectFileUtils.locateBuildFolder(CertSecurityTests.class);
        File fileRoot  = fileBuild.getParentFile();

        s_caCertTrusted      = KeyTool.createCACert(fileRoot, "test-ca1", "PKCS12");
        s_keyAndCertStorage1 = KeyTool.createKeyCertPair(fileRoot, s_caCertTrusted, "member-1");
        s_keyAndCertStorage2 = KeyTool.createKeyCertPair(fileRoot, s_caCertTrusted, "member-2");
        s_keyAndCertAllowed  = KeyTool.createKeyCertPair(fileRoot, s_caCertTrusted, "member-3");
        s_keyAndCertDenied   = KeyTool.createKeyCertPair(fileRoot, s_caCertTrusted, "member-4");
        s_keyAndCertMissing  = KeyTool.createKeyCertPair(fileRoot, s_caCertTrusted, "member-5");

        // Create a cert with a principal name that is allowed to join but signed with a different CA
        s_caCertUntrusted     = KeyTool.createCACert(fileRoot, "test-ca2", "PKCS12");
        s_keyAndCertUntrusted = KeyTool.createKeyCertPair(fileRoot, "untrusted", s_caCertUntrusted, "member-1");

        s_urlLogin   = Resources.findFileOrResource("cert-login.config", null);
        s_urlPerm    = Resources.findFileOrResource("cert-permissions.xml", null);
        s_urlPermAll = Resources.findFileOrResource("cert-permissions-all.xml", null);

        // Common Bedrock options for all processes
        s_commonOptions = OptionsByType.of(ClusterName.of("CertSecurityTests"),
                                LocalHost.only(),
                                WellKnownAddress.loopback(),
                                HeapSize.of(64, HeapSize.Units.MB, 128, HeapSize.Units.MB),
                                OperationalOverride.of("cert-override.xml"),
                                CacheConfig.of("cert-cache.config.xml"),
                                SystemProperty.of("coherence.storage.authorizer", "capture"),
                                SystemProperty.of("java.security.auth.login.config", s_urlLogin.getFile()),
                                SystemProperty.of("coherence.security.config", "cert-security-config.xml"),
                                SystemProperty.of("coherence.security.truststore", s_caCertTrusted.getKeystore().getAbsolutePath()),
                                SystemProperty.of("coherence.security.truststore.password", s_caCertTrusted.storePasswordString()),
                                Logging.atMax(),
                                IPv4Preferred.autoDetect(),
                                s_testLogs);

        // Start the first storage/proxy member
        OptionsByType options1 = OptionsByType.of(s_commonOptions)
                .addAll(ClassName.of(SecureCoherence.class),
                        LocalStorage.enabled(),
                        MemberName.of("member-1"),
                        SystemProperty.of("coherence.security.keystore", s_keyAndCertStorage1.getP12Keystore().getAbsolutePath()),
                        SystemProperty.of("coherence.security.keystore.password", s_keyAndCertStorage1.storePasswordString()),
                        SystemProperty.of("coherence.security.permissions", s_urlPerm.getFile()),
                        DisplayName.of("storage-1"),
                        RoleName.of("storage"));

        s_storage1 = LocalPlatform.get().launch(CoherenceClusterMember.class, options1.asArray());

        // Start the second storage/proxy member
        OptionsByType options2 = OptionsByType.of(s_commonOptions)
                .addAll(ClassName.of(SecureCoherence.class),
                        LocalStorage.enabled(),
                        MemberName.of("member-2"),
                        SystemProperty.of("coherence.security.keystore", s_keyAndCertStorage2.getP12Keystore().getAbsolutePath()),
                        SystemProperty.of("coherence.security.keystore.password", s_keyAndCertStorage2.storePasswordString()),
                        SystemProperty.of("coherence.security.permissions", s_urlPerm.getFile()),
                        DisplayName.of("storage-2"),
                        RoleName.of("storage"));

        s_storage2 = LocalPlatform.get().launch(CoherenceClusterMember.class, options2.asArray());

        // ensure both members start
        Eventually.assertDeferred(() -> s_storage1.invoke(IsCoherenceRunning.instance()), is(true));
        Eventually.assertDeferred(() -> s_storage2.invoke(IsCoherenceRunning.instance()), is(true));
        // they should have formed a cluster
        Eventually.assertDeferred(() -> s_storage1.getClusterSize(), is(2));
        Eventually.assertDeferred(() -> s_storage2.getClusterSize(), is(2));
        }

    @AfterAll
    public static void tearDown()
        {
        if (s_storage1 != null)
            {
            s_storage1.close();
            }
        if (s_storage2 != null)
            {
            s_storage2.close();
            }
        }

    @Test
    public void shouldJoinClusterWhenInAllowList()
        {
        KeyTool.KeyAndCert keyAndCert = s_keyAndCertAllowed;

        OptionsByType options = OptionsByType.of(s_commonOptions)
                .addAll(MemberName.of(keyAndCert.getName()),
                        ClassName.of(SecureCoherence.class),
                        LocalStorage.disabled(),
                        SystemProperty.of("coherence.security.keystore", keyAndCert.getP12Keystore().getAbsolutePath()),
                        SystemProperty.of("coherence.security.keystore.password", keyAndCert.storePasswordString()),
                        SystemProperty.of("coherence.security.permissions", s_urlPerm.getFile()),
                        DisplayName.of("shouldJoinClusterWhenInAllowList"),
                        RoleName.of("allowed"));

        try (CoherenceClusterMember member = LocalPlatform.get().launch(CoherenceClusterMember.class, options.asArray()))
            {
            Eventually.assertDeferred(() -> member.invoke(IsCoherenceRunning.instance()), is(true));
            // A cache action should be invoked with the correct Subject
            Set<String> set = member.invoke(new GetPrincipalNameProcessor());
            assertThat(set, is(notNullValue()));
            assertThat(set, containsInAnyOrder(getPrincipalName(keyAndCert)));
            }
        }

    @Test
    public void shouldNotJoinClusterWithUntrustedCert()
        {
        KeyTool.KeyAndCert keyAndCert = s_keyAndCertUntrusted;

        OptionsByType options = OptionsByType.of(s_commonOptions)
                .addAll(MemberName.of(keyAndCert.getName()),
                        ClassName.of(SecureCoherence.class),
                        LocalStorage.disabled(),
                        SystemProperty.of("coherence.security.keystore", keyAndCert.getP12Keystore().getAbsolutePath()),
                        SystemProperty.of("coherence.security.keystore.password", keyAndCert.storePasswordString()),
                        SystemProperty.of("coherence.security.truststore", s_caCertUntrusted.getKeystore().getAbsolutePath()),
                        SystemProperty.of("coherence.security.truststore.password", s_caCertUntrusted.storePasswordString()),
                        SystemProperty.of("coherence.security.permissions", s_urlPerm.getFile()),
                        DisplayName.of("shouldNotJoinClusterWithUntrustedCert"),
                        RoleName.of("untrusted"));

        try (CoherenceClusterMember member = LocalPlatform.get().launch(CoherenceClusterMember.class, options.asArray()))
            {
            Eventually.assertDeferred(() -> member.invoke(HasThrown.INSTANCE), is(true));
            }
        }

    @Test
    public void shouldNotJoinClusterWhenNotInPermissionsFile()
        {
        KeyTool.KeyAndCert keyAndCert = s_keyAndCertMissing;

        OptionsByType options = OptionsByType.of(s_commonOptions)
                .addAll(ClassName.of(SecureCoherence.class),
                        LocalStorage.disabled(),
                        MemberName.of(keyAndCert.getName()),
                        SystemProperty.of("coherence.security.keystore", keyAndCert.getP12Keystore().getAbsolutePath()),
                        SystemProperty.of("coherence.security.keystore.password", keyAndCert.storePasswordString()),
                        SystemProperty.of("coherence.security.permissions", s_urlPerm.getFile()),
                        DisplayName.of("shouldNotJoinClusterWhenNotInPermissionsFile"),
                        RoleName.of("not-in-permissions"));

        try (CoherenceClusterMember member = LocalPlatform.get().launch(CoherenceClusterMember.class, options.asArray()))
            {
            Eventually.assertDeferred(() -> member.invoke(HasThrown.INSTANCE), is(true));
            }
        }

    @Test
    public void shouldNotJoinClusterWhenInDenyList()
        {
        KeyTool.KeyAndCert keyAndCert = s_keyAndCertDenied;

        OptionsByType options = OptionsByType.of(s_commonOptions)
                .addAll(ClassName.of(SecureCoherence.class),
                        LocalStorage.disabled(),
                        MemberName.of(keyAndCert.getName()),
                        SystemProperty.of("coherence.security.keystore", keyAndCert.getP12Keystore().getAbsolutePath()),
                        SystemProperty.of("coherence.security.keystore.password", keyAndCert.storePasswordString()),
                        SystemProperty.of("coherence.security.permissions", s_urlPerm.getFile()),
                        DisplayName.of("shouldNotJoinClusterWhenInDenyList"),
                        RoleName.of("denied"));

        try (CoherenceClusterMember member = LocalPlatform.get().launch(CoherenceClusterMember.class, options.asArray()))
            {
            Eventually.assertDeferred(() -> member.invoke(HasThrown.INSTANCE), is(true));
            }
        }

    @Test
    public void shouldNotJoinClusterWhenInOwnAllowListButNotInSeniorsPermissionsFile()
        {
        KeyTool.KeyAndCert keyAndCert = s_keyAndCertMissing;

        OptionsByType options = OptionsByType.of(s_commonOptions)
                .addAll(ClassName.of(SecureCoherence.class),
                        LocalStorage.disabled(),
                        MemberName.of(keyAndCert.getName()),
                        SystemProperty.of("coherence.security.keystore", keyAndCert.getP12Keystore().getAbsolutePath()),
                        SystemProperty.of("coherence.security.keystore.password", keyAndCert.storePasswordString()),
                        SystemProperty.of("coherence.security.permissions", s_urlPermAll.getFile()),
                        DisplayName.of("shouldNotJoinClusterWhenInOwnAllowListButNotInSeniorsPermissionsFile"),
                        RoleName.of("not-in-permissions"));

        try (CoherenceClusterMember member = LocalPlatform.get().launch(CoherenceClusterMember.class, options.asArray()))
            {
            Eventually.assertDeferred(() -> member.invoke(HasThrown.INSTANCE), is(true));
            }
        }

    @Test
    public void shouldNotJoinClusterWhenInOwnAllowListButInSeniorsDenyList()
        {
        KeyTool.KeyAndCert keyAndCert = s_keyAndCertDenied;

        OptionsByType options = OptionsByType.of(s_commonOptions)
                .addAll(ClassName.of(SecureCoherence.class),
                        LocalStorage.enabled(),
                        MemberName.of(keyAndCert.getName()),
                        SystemProperty.of("coherence.security.keystore", keyAndCert.getP12Keystore().getAbsolutePath()),
                        SystemProperty.of("coherence.security.keystore.password", keyAndCert.storePasswordString()),
                        SystemProperty.of("coherence.security.permissions", s_urlPermAll.getFile()),
                        DisplayName.of("shouldNotJoinClusterWhenInOwnAllowListButInSeniorsDenyList"),
                        RoleName.of("denied"));

        try (CoherenceClusterMember member = LocalPlatform.get().launch(CoherenceClusterMember.class, options.asArray()))
            {
            Eventually.assertDeferred(() -> member.invoke(HasThrown.INSTANCE), is(true));
            }
        }

    @Test
    public void shouldConnectClientWhenInAllowList()
        {
        KeyTool.KeyAndCert keyAndCert = s_keyAndCertAllowed;

        OptionsByType options = OptionsByType.of(s_commonOptions)
                .addAll(MemberName.of(keyAndCert.getName()),
                        ClassName.of(SecureCoherence.class),
                        LocalStorage.disabled(),
                        SystemProperty.of("coherence.client","remote"),
                        SystemProperty.of("coherence.security.keystore", keyAndCert.getP12Keystore().getAbsolutePath()),
                        SystemProperty.of("coherence.security.keystore.password", keyAndCert.storePasswordString()),
                        SystemProperty.of("coherence.security.permissions", s_urlPerm.getFile()),
                        DisplayName.of("shouldConnectClientWhenInAllowList"),
                        RoleName.of("client-allowed"));

        try (CoherenceClusterMember member = LocalPlatform.get().launch(CoherenceClusterMember.class, options.asArray()))
            {
            Eventually.assertDeferred(() -> member.invoke(IsCoherenceRunning.instance()), is(true));
            // A cache action should be invoked with the correct Subject
            Set<String> set = member.invoke(new GetPrincipalNameProcessor());
            assertThat(set, is(notNullValue()));
            assertThat(set, containsInAnyOrder(getPrincipalName(keyAndCert)));
            }
        }

    @Test
    public void shouldNotConnectClientWhenNotInPermissionsFile()
        {
        KeyTool.KeyAndCert keyAndCert = s_keyAndCertMissing;

        OptionsByType options = OptionsByType.of(s_commonOptions)
                .addAll(MemberName.of(keyAndCert.getName()),
                        ClassName.of(SecureCoherence.class),
                        LocalStorage.disabled(),
                        SystemProperty.of("coherence.client","remote"),
                        SystemProperty.of("coherence.security.keystore", keyAndCert.getP12Keystore().getAbsolutePath()),
                        SystemProperty.of("coherence.security.keystore.password", keyAndCert.storePasswordString()),
                        SystemProperty.of("coherence.security.permissions", s_urlPerm.getFile()),
                        DisplayName.of("shouldNotConnectClientWhenNotInProxyPermissionsFile"),
                        RoleName.of("client-not-in-permissions"));

        try (CoherenceClusterMember member = LocalPlatform.get().launch(CoherenceClusterMember.class, options.asArray()))
            {
            Eventually.assertDeferred(() -> member.invoke(IsCoherenceRunning.instance()), is(true));
            // The cache action should be invoked with the correct Subject, which will be denied

            Throwable thrown = assertThrows(Throwable.class, () -> member.invoke(new GetPrincipalNameProcessor()));
            Throwable cause = Exceptions.getRootCause(thrown);
            assertThat(cause, is(instanceOf(PermissionException.class)));
            }
        }

    @Test
    public void shouldNotConnectClientWhenInDenyList()
        {
        KeyTool.KeyAndCert keyAndCert = s_keyAndCertDenied;

        OptionsByType options = OptionsByType.of(s_commonOptions)
                .addAll(MemberName.of(keyAndCert.getName()),
                        ClassName.of(SecureCoherence.class),
                        LocalStorage.disabled(),
                        SystemProperty.of("coherence.client","remote"),
                        SystemProperty.of("coherence.security.keystore", keyAndCert.getP12Keystore().getAbsolutePath()),
                        SystemProperty.of("coherence.security.keystore.password", keyAndCert.storePasswordString()),
                        SystemProperty.of("coherence.security.permissions", s_urlPerm.getFile()),
                        DisplayName.of("shouldNotConnectClientWhenInProxyDenyList"),
                        RoleName.of("client-denied"));

        try (CoherenceClusterMember member = LocalPlatform.get().launch(CoherenceClusterMember.class, options.asArray()))
            {
            Eventually.assertDeferred(() -> member.invoke(IsCoherenceRunning.instance()), is(true));
            // The cache action should be invoked with the correct Subject, which will be denied
            Throwable thrown = assertThrows(Throwable.class, () -> member.invoke(new GetPrincipalNameProcessor()));
            Throwable cause = Exceptions.getRootCause(thrown);
            assertThat(cause, anyOf(instanceOf(SecurityException.class), instanceOf(PermissionException.class)));
            }
        }

    @Test
    public void shouldNotConnectClientWhenInOwnAllowListButNotInProxyPermissionsFile()
        {
        KeyTool.KeyAndCert keyAndCert = s_keyAndCertMissing;

        OptionsByType options = OptionsByType.of(s_commonOptions)
                .addAll(MemberName.of(keyAndCert.getName()),
                        ClassName.of(SecureCoherence.class),
                        LocalStorage.disabled(),
                        SystemProperty.of("coherence.client","remote"),
                        SystemProperty.of("coherence.security.keystore", keyAndCert.getP12Keystore().getAbsolutePath()),
                        SystemProperty.of("coherence.security.keystore.password", keyAndCert.storePasswordString()),
                        SystemProperty.of("coherence.security.permissions", s_urlPermAll.getFile()),
                        DisplayName.of("shouldNotConnectClientWhenInOwnAllowListButNotInProxyPermissionsFile"),
                        RoleName.of("client-not-in-permissions"));

        try (CoherenceClusterMember member = LocalPlatform.get().launch(CoherenceClusterMember.class, options.asArray()))
            {
            Eventually.assertDeferred(() -> member.invoke(IsCoherenceRunning.instance()), is(true));
            // The cache action should be invoked with the correct Subject, which will be denied
            Throwable thrown = assertThrows(Throwable.class, () ->
                {
                Set<String> set = member.invoke(new GetPrincipalNameProcessor());
                System.out.println("Principals: " + set);
                });
            Throwable cause = Exceptions.getRootCause(thrown);
            assertThat(cause, is(instanceOf(SecurityException.class)));
            }
        }

    @Test
    public void shouldNotConnectClientWhenInOwnAllowListButInProxyDenyList()
        {
        KeyTool.KeyAndCert keyAndCert = s_keyAndCertDenied;

        OptionsByType options = OptionsByType.of(s_commonOptions)
                .addAll(MemberName.of(keyAndCert.getName()),
                        ClassName.of(SecureCoherence.class),
                        LocalStorage.disabled(),
                        SystemProperty.of("coherence.client","remote"),
                        SystemProperty.of("coherence.security.keystore", keyAndCert.getP12Keystore().getAbsolutePath()),
                        SystemProperty.of("coherence.security.keystore.password", keyAndCert.storePasswordString()),
                        SystemProperty.of("coherence.security.permissions", s_urlPermAll.getFile()),
                        DisplayName.of("shouldNotConnectClientWhenInOwnAllowListButInProxyDenyList"),
                        RoleName.of("client-denied"));

        try (CoherenceClusterMember member = LocalPlatform.get().launch(CoherenceClusterMember.class, options.asArray()))
            {
            Eventually.assertDeferred(() -> member.invoke(IsCoherenceRunning.instance()), is(true));
            // The cache action should be invoked with the correct Subject, which will be denied
            Throwable thrown = assertThrows(Throwable.class, () ->
                {
                Set<String> set = member.invoke(new GetPrincipalNameProcessor());
                System.out.println("Principals: " + set);
                });
            Throwable cause = Exceptions.getRootCause(thrown);
            assertThat(cause, is(instanceOf(SecurityException.class)));
            }
        }

    @Test
    public void shouldNotConnectUntrustedClient()
        {
        KeyTool.KeyAndCert keyAndCert = s_keyAndCertUntrusted;

        OptionsByType options = OptionsByType.of(s_commonOptions)
                .addAll(MemberName.of(keyAndCert.getName()),
                        ClassName.of(SecureCoherence.class),
                        LocalStorage.disabled(),
                        SystemProperty.of("coherence.client","remote"),
                        SystemProperty.of("coherence.security.keystore", keyAndCert.getP12Keystore().getAbsolutePath()),
                        SystemProperty.of("coherence.security.keystore.password", keyAndCert.storePasswordString()),
                        SystemProperty.of("coherence.security.permissions", s_urlPermAll.getFile()),
                        DisplayName.of("shouldNotConnectClientWhenInOwnAllowListButInProxyDenyList"),
                        RoleName.of("client-denied"));

        try (CoherenceClusterMember member = LocalPlatform.get().launch(CoherenceClusterMember.class, options.asArray()))
            {
            Eventually.assertDeferred(() -> member.invoke(IsCoherenceRunning.instance()), is(true));
            // The cache action should be invoked with the untrusted Subject, which will be denied
            Throwable thrown = assertThrows(Throwable.class, () ->
                {
                Set<String> set = member.invoke(new GetPrincipalNameProcessor());
                System.out.println("Principals: " + set);
                });
            Throwable cause = Exceptions.getRootCause(thrown);
            assertThat(cause, is(instanceOf(SecurityException.class)));
            }
        }

    // ----- inner class: DiscriminatorProperty -----------------------------

    public static class DiscriminatorProperty
            implements SystemProperty.ContextSensitiveValue
        {
        public DiscriminatorProperty(String sPattern)
            {
            f_sPattern = sPattern;
            }

        @Override
        public Object resolve(String s, Platform platform, OptionsByType optionsByType)
            {
            Discriminator discriminator = optionsByType.get(Discriminator.class);
            if (discriminator != null)
                {
                return String.format(f_sPattern, discriminator.getValue());
                }
            return String.format(f_sPattern, "");
            }

        private final String f_sPattern;
        }

    protected String getPrincipalName(KeyTool.KeyAndCert keyAndCert)
        {
        return String.format("CN=%s", keyAndCert.getName());
        }

    // ----- data members ---------------------------------------------------

    protected static URL s_urlLogin;

    protected static URL s_urlPerm;

    protected static URL s_urlPermAll;

    protected static OptionsByType s_commonOptions;

    protected static KeyTool.KeyAndCert s_caCertTrusted;

    protected static KeyTool.KeyAndCert s_caCertUntrusted;

    protected static KeyTool.KeyAndCert s_keyAndCertStorage1;

    protected static KeyTool.KeyAndCert s_keyAndCertStorage2;

    protected static KeyTool.KeyAndCert s_keyAndCertAllowed;

    protected static KeyTool.KeyAndCert s_keyAndCertDenied;

    protected static KeyTool.KeyAndCert s_keyAndCertMissing;

    protected static KeyTool.KeyAndCert s_keyAndCertUntrusted;

    protected  static CoherenceClusterMember s_storage1;

    protected  static CoherenceClusterMember s_storage2;

    @RegisterExtension
    static final TestLogsExtension s_testLogs = new TestLogsExtension(CertSecurityTests.class);
    }
