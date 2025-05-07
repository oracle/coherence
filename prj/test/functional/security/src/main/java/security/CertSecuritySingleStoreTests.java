/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package security;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterBuilder;
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
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.runtime.options.StabilityPredicate;
import com.oracle.bedrock.testsupport.MavenProjectFileUtils;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogs;
import com.oracle.coherence.testing.util.KeyTool;
import com.tangosol.coherence.config.Config;
import com.tangosol.util.Resources;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

public class CertSecuritySingleStoreTests
    {
    @BeforeClass
    public static void setup() throws Exception
        {
        KeyTool.assertCanCreateKeys();

        File fileBuild = MavenProjectFileUtils.locateBuildFolder(CertSecuritySingleStoreTests.class);

        s_caCert             = KeyTool.createCACert(fileBuild, "test-ca", "PKCS12");
        s_keyAndCertStorage1 = KeyTool.createKeyCertPair(fileBuild, s_caCert, "member-1");
        s_keyAndCertStorage2 = KeyTool.createKeyCertPair(fileBuild, s_caCert, "member-2");
        s_keyAndCertAllowed  = KeyTool.createKeyCertPair(fileBuild, s_caCert, "member-3");
        s_keyAndCertDenied   = KeyTool.createKeyCertPair(fileBuild, s_caCert, "member-4");
        s_keyAndCertMissing  = KeyTool.createKeyCertPair(fileBuild, s_caCert, "member-5");

        s_keyAndCertSet = KeyTool.merge(fileBuild, "all", s_keyAndCertStorage1, s_keyAndCertStorage2,
                s_keyAndCertAllowed, s_keyAndCertDenied, s_keyAndCertMissing);

        s_urlLogin   = Resources.findFileOrResource("cert-login.config", null);
        s_urlPerm    = Resources.findFileOrResource("cert-permissions.xml", null);
        s_urlPermAll = Resources.findFileOrResource("cert-permissions-all.xml", null);

        String sCertDir = s_caCert.getKey().getParent();
        String sPattern = sCertDir + File.separatorChar + "member-%s.jks";

        // Common Bedrock options for all processes
        s_commonOptions = OptionsByType.of(ClusterName.of("CertSecuritySingleStoreTests"),
                                LocalHost.only(),
                                WellKnownAddress.loopback(),
                                HeapSize.of(64, HeapSize.Units.MB, 128, HeapSize.Units.MB),
                                OperationalOverride.of("cert-default-override.xml"),
                                CacheConfig.of("cert-cache.config.xml"),
                                SystemProperty.of("coherence.storage.authorizer", "capture"),
                                SystemProperty.of("java.security.auth.login.config", s_urlLogin.getFile()),
                                SystemProperty.of("coherence.security.config", "cert-security-config.xml"),
                                SystemProperty.of("coherence.security.login.password", s_keyAndCertStorage1.storePasswordString()),
                                Logging.atMax(),
                                IPv4Preferred.autoDetect(),
                                s_testLogs);

        if (Config.getBoolean("coherence.security.test.local", false))
            {
            System.setProperty("coherence.cluster", "CertSecuritySingleStoreTests");
            System.setProperty("coherence.localhost", "127.0.0.1");
            System.setProperty("coherence.wka", "127.0.0.1");
            System.setProperty("coherence.override", "cert-default-override.xml");
            System.setProperty("coherence.cacheconfig", "cert-cache.config.xml");
            System.setProperty("coherence.storage.authorizer", "capture");
            System.setProperty("java.security.auth.login.config", s_urlLogin.getFile());
            System.setProperty("coherence.security.config", "cert-security-config.xml");
            System.setProperty("coherence.security.keystore", s_keyAndCertStorage1.getP12Keystore().getAbsolutePath());
            System.setProperty("coherence.security.login.password", s_keyAndCertStorage1.storePasswordString());
            System.setProperty("coherence.security.truststore", s_keyAndCertSet.getP12Certs().getAbsolutePath());
            System.setProperty("coherence.security.truststore.password", s_keyAndCertSet.storePasswordString());
            System.setProperty("coherence.security.permissions", s_urlPerm.getFile());
            System.setProperty("coherence.member", s_keyAndCertStorage1.getName());

            SecureCoherence.startAndWait();
            }
        else
            {
            // Start the cluster with two storage/proxy members
            CoherenceClusterBuilder builder = new CoherenceClusterBuilder()
                    .with(s_commonOptions.asArray())
                    .include(2, CoherenceClusterMember.class,
                            ClassName.of(SecureCoherence.class),
                            LocalStorage.enabled(),
                            MemberName.withDiscriminator("member"),
                            SystemProperty.of("coherence.security.keystore", s_keyAndCertSet.getP12Keystore().getAbsolutePath()),
                            SystemProperty.of("coherence.security.login.password", s_keyAndCertSet.storePasswordString()),
                            SystemProperty.of("coherence.security.truststore", s_keyAndCertSet.getP12Keystore().getAbsolutePath()),
                            SystemProperty.of("coherence.security.truststore.password", s_keyAndCertSet.storePasswordString()),
                            SystemProperty.of("coherence.security.permissions", s_urlPerm.getFile()),
                            DisplayName.of("storage"),
                            RoleName.of("storage"),
                            StabilityPredicate.none());

            s_cluster = builder.build();

            for (CoherenceClusterMember member : s_cluster)
                {
                Eventually.assertDeferred(() -> member.invoke(IsCoherenceRunning.instance()), is(true));
                }
            }
        }

    @AfterClass
    public static void tearDown()
        {
        if (s_cluster != null)
            {
            s_cluster.close();
            }
        }

    @Test
    public void shouldJoinClusterWhenInGrantList()
        {
        KeyTool.KeyAndCert keyAndCert = s_keyAndCertAllowed;

        OptionsByType options = OptionsByType.of(s_commonOptions.asArray())
                .addAll(MemberName.of(keyAndCert.getName()),
                        ClassName.of(SecureCoherence.class),
                        LocalStorage.disabled(),
                        OperationalOverride.of("cert-pwd-provider-override.xml"),
                        SystemProperty.of("coherence.security.keystore", keyAndCert.getP12Keystore().getAbsolutePath()),
                        SystemProperty.of("coherence.security.login.password", keyAndCert.storePasswordString()),
                        SystemProperty.of("coherence.security.truststore", s_keyAndCertSet.getP12Keystore().getAbsolutePath()),
                        SystemProperty.of("coherence.security.truststore.password.file", s_keyAndCertSet.getPasswordFile().getAbsolutePath()),
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
    public void shouldNotJoinClusterWhenNotInPermissionsFile()
        {
        KeyTool.KeyAndCert keyAndCert = s_keyAndCertMissing;

        OptionsByType options = OptionsByType.of(s_commonOptions)
                .addAll(ClassName.of(SecureCoherence.class),
                        LocalStorage.disabled(),
                        MemberName.of(keyAndCert.getName()),
                        SystemProperty.of("coherence.security.keystore", keyAndCert.getP12Keystore().getAbsolutePath()),
                        SystemProperty.of("coherence.security.login.password", keyAndCert.storePasswordString()),
                        SystemProperty.of("coherence.security.truststore", s_keyAndCertSet.getP12Keystore().getAbsolutePath()),
                        SystemProperty.of("coherence.security.truststore.password", s_keyAndCertSet.storePasswordString()),
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
                        SystemProperty.of("coherence.security.login.password", keyAndCert.storePasswordString()),
                        SystemProperty.of("coherence.security.truststore", s_keyAndCertSet.getP12Keystore().getAbsolutePath()),
                        SystemProperty.of("coherence.security.truststore.password", s_keyAndCertSet.storePasswordString()),
                        SystemProperty.of("coherence.security.permissions", s_urlPerm.getFile()),
                        DisplayName.of("shouldNotJoinClusterWhenInDenyList"),
                        RoleName.of("denied"));

        try (CoherenceClusterMember member = LocalPlatform.get().launch(CoherenceClusterMember.class, options.asArray()))
            {
            Eventually.assertDeferred(() -> member.invoke(HasThrown.INSTANCE), is(true));
            }
        }

    @Test
    public void shouldNotJoinClusterWhenInOwnGrantListButNotInSeniorsPermissionsFile()
        {
        KeyTool.KeyAndCert keyAndCert = s_keyAndCertMissing;

        OptionsByType options = OptionsByType.of(s_commonOptions)
                .addAll(ClassName.of(SecureCoherence.class),
                        LocalStorage.disabled(),
                        MemberName.of(keyAndCert.getName()),
                        SystemProperty.of("coherence.security.keystore", keyAndCert.getP12Keystore().getAbsolutePath()),
                        SystemProperty.of("coherence.security.login.password", keyAndCert.storePasswordString()),
                        SystemProperty.of("coherence.security.truststore", s_keyAndCertSet.getP12Keystore().getAbsolutePath()),
                        SystemProperty.of("coherence.security.truststore.password", s_keyAndCertSet.storePasswordString()),
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
                        SystemProperty.of("coherence.security.login.password", keyAndCert.storePasswordString()),
                        SystemProperty.of("coherence.security.truststore", s_keyAndCertSet.getP12Keystore().getAbsolutePath()),
                        SystemProperty.of("coherence.security.truststore.password", s_keyAndCertSet.storePasswordString()),
                        SystemProperty.of("coherence.security.permissions", s_urlPermAll.getFile()),
                        DisplayName.of("shouldNotJoinClusterWhenInOwnAllowListButInSeniorsDenyList"),
                        RoleName.of("denied"));

        try (CoherenceClusterMember member = LocalPlatform.get().launch(CoherenceClusterMember.class, options.asArray()))
            {
            Eventually.assertDeferred(() -> member.invoke(HasThrown.INSTANCE), is(true));
            }
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

    protected static KeyTool.KeyAndCertSet s_keyAndCertSet;

    protected static KeyTool.KeyAndCert s_caCert;

    protected static KeyTool.KeyAndCert s_keyAndCertStorage1;

    protected static KeyTool.KeyAndCert s_keyAndCertStorage2;

    protected static KeyTool.KeyAndCert s_keyAndCertAllowed;

    protected static KeyTool.KeyAndCert s_keyAndCertDenied;

    protected static KeyTool.KeyAndCert s_keyAndCertMissing;

    protected static CoherenceCluster s_cluster;

    @ClassRule
    public static TestLogs s_testLogs = new TestLogs(CertSecuritySingleStoreTests.class);
    }
