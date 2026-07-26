/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.security;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.net.ClusterPermission;
import com.tangosol.util.ExternalizableHelper;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import javax.security.auth.x500.X500PrivateCredential;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignedObject;
import java.security.cert.CertPath;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThrows;

/**
 * Unit tests for {@link DefaultController}.
 *
 * @author Aleks Seovic  2026.05.19
 * @since 26.04
 */
public class DefaultControllerTest
    {
    @Rule
    public TemporaryFolder m_folder = new TemporaryFolder();

    @After
    public void cleanup()
        {
        CoherenceModeHelper.restore(m_sMode);
        restoreProperty(DefaultController.PROPERTY_CONFIG, m_sConfig);
        }

    @Test
    public void shouldUseIndependentSignatureInstances()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.legacy())
            {
            DefaultController controller = controller(jksKeystore(), permissions(), "password");

            Signature signature1 = controller.createSignature();
            Signature signature2 = controller.createSignature();

            assertNotSame(signature1, signature2);
            assertEquals(DefaultController.SIGNATURE_ALGORITHM, signature1.getAlgorithm());
            assertEquals(DefaultController.SIGNATURE_ALGORITHM, signature2.getAlgorithm());
            }
        }

    @Test
    public void shouldAllowHonestManagerSubject()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.legacy())
            {
            DefaultController controller = controller(jksKeystore(), permissions(), "password");
            KeyStore          store      = keyStore(jksKeystore(), "JKS", "password");
            Subject           subject    = subject(store, "manager", "manager");
            ClusterPermission permission = new ClusterPermission("*", "all");

            SignedObject signed    = controller.encrypt(permission, subject);
            Object       decrypted = controller.decrypt(signed, subject, null);

            assertEquals(permission, decrypted);
            controller.checkPermission((ClusterPermission) decrypted, subject);
            }
        }

    @Test
    public void shouldAllowLegacyCertificateSubjectWithUnboundExtraPrincipal()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.legacy())
            {
            DefaultController controller = controller(jksKeystore(), permissions(), "password");
            KeyStore          store      = keyStore(jksKeystore(), "JKS", "password");
            Subject           subject    = subjectWithCertificates(store, "worker",
                    new String[] {"worker", "manager"}, new String[] {"worker"});
            ClusterPermission permission = new ClusterPermission("service=Management", "join");

            controller.checkPermission(permission, subject);
            }
        }

    @Test
    public void shouldDenyHonestWorkerHigherPrivilegePermission()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.legacy())
            {
            DefaultController controller = controller(jksKeystore(), permissions(), "password");
            KeyStore          store      = keyStore(jksKeystore(), "JKS", "password");
            Subject           subject    = subject(store, "worker", "worker");
            ClusterPermission permission = new ClusterPermission("service=Management", "join");

            SignedObject signed    = controller.encrypt(permission, subject);
            Object       decrypted = controller.decrypt(signed, subject, null);

            assertEquals(permission, decrypted);
            assertThrows(PermissionException.class,
                    () -> controller.checkPermission((ClusterPermission) decrypted, subject));
            }
        }

    @Test
    public void shouldRejectWorkerCertificateWithManagerPrincipal()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.legacy())
            {
            DefaultController controller = controller(jksKeystore(), permissions(), "password");
            KeyStore          store      = keyStore(jksKeystore(), "JKS", "password");
            Subject           subject    = subject(store, "worker", "manager");
            ClusterPermission permission = new ClusterPermission("service=Management", "join");
            SignedObject      signed     = controller.encrypt(permission, subject);

            assertThrows(Exception.class, () -> controller.decrypt(signed, subject, null));
            }
        }

    @Test
    public void shouldAllowLegacyExtraManagerPrincipalAfterWorkerSignerVerified()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.legacy())
            {
            DefaultController controller = controller(jksKeystore(), permissions(), "password");
            KeyStore          store      = keyStore(jksKeystore(), "JKS", "password");
            Subject           subject    = subjectWithCertificates(store, "worker",
                    new String[] {"worker", "manager"}, new String[] {"worker"});
            ClusterPermission permission = new ClusterPermission("service=Management", "join");

            SignedObject signed    = controller.encrypt(permission, subject);
            Object       decrypted = controller.decrypt(signed, subject, null);

            assertEquals(permission, decrypted);
            controller.checkPermission((ClusterPermission) decrypted, subject);
            }
        }

    @Test
    public void shouldAllowLegacyExtraManagerPrincipalWithAdditionalCertificateAfterWorkerSignerVerified()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.legacy())
            {
            DefaultController controller = controller(jksKeystore(), permissions(), "password");
            KeyStore          store      = keyStore(jksKeystore(), "JKS", "password");
            Subject           subject    = subjectWithCertificates(store, "worker",
                    new String[] {"worker", "manager"}, new String[] {"worker", "manager"});
            ClusterPermission permission = new ClusterPermission("service=Management", "join");

            SignedObject signed    = controller.encrypt(permission, subject);
            Object       decrypted = controller.decrypt(signed, subject, null);

            assertEquals(permission, decrypted);
            controller.checkPermission((ClusterPermission) decrypted, subject);
            }
        }

    @Test
    public void shouldRejectExtraManagerPrincipalAfterWorkerSignerVerifiedInDev()
            throws Exception
        {
        assertProbe(0, "dev", rsaConfig(), null, "expect-signed-extra-principal-rejected",
                path(rsaKeystore()), path(permissions()), "PKCS12");
        }

    @Test
    public void shouldRejectCertificateSubjectWithUnboundExtraPrincipalWithoutSignerVerificationInDev()
            throws Exception
        {
        assertProbe(0, "dev", rsaConfig(), null, "expect-extra-principal-rejected",
                path(rsaKeystore()), path(permissions()), "PKCS12");
        }

    @Test
    public void shouldRejectMutableSubjectCacheBypass()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.legacy())
            {
            DefaultController controller = controller(jksKeystore(), permissions(), "password");
            KeyStore          store      = keyStore(jksKeystore(), "JKS", "password");
            Subject           subject    = subject(store, "worker", "worker");
            ClusterPermission permission = new ClusterPermission("*", "all");

            controller.decrypt(controller.encrypt(permission, subject), subject, null);

            X509Certificate certManager = (X509Certificate) store.getCertificate("manager");
            subject.getPrincipals().clear();
            subject.getPrincipals().add(certManager.getSubjectX500Principal());

            SignedObject signed = controller.encrypt(permission, subject);

            assertThrows(Exception.class, () -> controller.decrypt(signed, subject, null));
            }
        }

    @Test
    public void shouldRoundTripPermissionInfoWithCertificateSubject()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.legacy())
            {
            DefaultController controller = controller(jksKeystore(), permissions(), "password");
            KeyStore          store      = keyStore(jksKeystore(), "JKS", "password");
            Subject           subject    = subjectWithCertPath(store, "manager", "manager");
            ClusterPermission permission = new ClusterPermission("service=Management", "join");
            PermissionInfo    info       = new PermissionInfo(permission, permission.getServiceName(),
                    controller.encrypt(permission, subject), subject);

            PermissionInfo copy      = externalizableRoundTrip(info);
            Object         decrypted = controller.decrypt(copy.getSignedPermission(), copy.getSubject(), null);

            assertEquals(permission, decrypted);
            controller.checkPermission((ClusterPermission) decrypted, copy.getSubject());

            PermissionInfo binaryCopy = binaryRoundTrip(info);
            Object         binary     = controller.decrypt(binaryCopy.getSignedPermission(), binaryCopy.getSubject(), null);

            assertEquals(permission, binary);
            controller.checkPermission((ClusterPermission) binary, binaryCopy.getSubject());
            }
        }

    @Test
    public void shouldPreserveLegacyDefaultDsaCompatibilityInForkedVm()
            throws Exception
        {
        assertProbe(0, null, null, null, "round-trip", path(jksKeystore()), path(permissions()), "JKS",
                "manager", "manager");
        }

    @Test
    public void shouldUseModernDefaultInProdInForkedVm()
            throws Exception
        {
        assertProbe(0, "prod", null, null, "algorithm", "SHA256withRSA");
        }

    @Test
    public void shouldAllowExplicitModernAlgorithmInProdInForkedVm()
            throws Exception
        {
        assertProbe(0, "prod", rsaConfig(), null, "round-trip", path(rsaKeystore()), path(permissions()), "PKCS12",
                "manager", "manager");
        }

    @Test
    public void shouldAllowExplicitWeakAlgorithmInLegacyInForkedVm()
            throws Exception
        {
        assertProbe(0, "legacy", weakConfig("SHA1withDSA"), null,
                "round-trip", path(jksKeystore()), path(permissions()), "JKS", "manager", "manager");
        }

    @Test
    public void shouldRejectExplicitWeakAlgorithmInDevInForkedVm()
            throws Exception
        {
        assertProbe(0, "dev", weakConfig("SHA1withDSA"), null, "expect-init-failure", null, null, null);
        }

    @Test
    public void shouldRejectExplicitWeakAlgorithmInProdInForkedVm()
            throws Exception
        {
        assertProbe(0, "prod", weakConfig("SHA1withDSA"), null, "expect-init-failure", null, null, null);
        }

    @Test
    public void shouldRejectDashedShaOneAlgorithmInDevInForkedVm()
            throws Exception
        {
        assertProbe(0, "dev", weakConfig("SHA-1withDSA"), null, "expect-init-failure", null, null, null);
        }

    @Test
    public void shouldRejectTrailingShaOneAlgorithmInDevInForkedVm()
            throws Exception
        {
        assertProbe(0, "dev", weakConfig("DSAwithSHA1"), null, "expect-init-failure", null, null, null);
        }

    @Test
    public void shouldRejectKnownWeakDsaAliasesInDevInForkedVm()
            throws Exception
        {
        for (String sAlgorithm : new String[] {"DSA", "DSS", "SHA/DSA", "SHAwithDSA"})
            {
            assertProbe(0, "dev", weakConfig(sAlgorithm), null, "expect-init-failure", null, null, null);
            }
        }

    @Test
    public void shouldRejectKnownWeakDsaAliasesInProdInForkedVm()
            throws Exception
        {
        for (String sAlgorithm : new String[] {"DSA", "DSS", "SHA/DSA", "SHAwithDSA"})
            {
            assertProbe(0, "prod", weakConfig(sAlgorithm), null, "expect-init-failure", null, null, null);
            }
        }

    @Test
    public void shouldRejectMissedWeakDsaAliasesInDevInForkedVm()
            throws Exception
        {
        for (String sAlgorithm : missedWeakDsaAliases())
            {
            assertProbe(0, "dev", weakConfig(sAlgorithm), null, "expect-init-failure", null, null, null);
            }
        }

    @Test
    public void shouldRejectMissedWeakDsaAliasesInProdInForkedVm()
            throws Exception
        {
        for (String sAlgorithm : missedWeakDsaAliases())
            {
            assertProbe(0, "prod", weakConfig(sAlgorithm), null, "expect-init-failure", null, null, null);
            }
        }

    @Test
    public void shouldRejectMismatchedSubjectInDevInForkedVm()
            throws Exception
        {
        assertProbe(0, "dev", rsaConfig(), null, "expect-mismatch-rejected",
                path(rsaKeystore()), path(permissions()), "PKCS12", "worker", "manager");
        }

    @Test
    public void shouldRejectMismatchedSubjectInProdInForkedVm()
            throws Exception
        {
        assertProbe(0, "prod", rsaConfig(), null, "expect-mismatch-rejected",
                path(rsaKeystore()), path(permissions()), "PKCS12", "worker", "manager");
        }

    private void assertProbe(int nExpected, String sMode, File fileConfig, String sExtraProperty,
            String... asArg)
            throws Exception
        {
        List<String> listCommand = new ArrayList<>();
        listCommand.add(new File(System.getProperty("java.home"), "bin/java").getAbsolutePath());
        listCommand.add("-cp");
        listCommand.add(System.getProperty("java.class.path"));

        if (sMode != null)
            {
            listCommand.add("-Dcoherence.mode=" + sMode);
            }
        if (fileConfig != null)
            {
            listCommand.add("-D" + DefaultController.PROPERTY_CONFIG + "=" + fileConfig.toURI().toURL());
            }
        if (sExtraProperty != null)
            {
            listCommand.add(sExtraProperty);
            }

        listCommand.add(AlgorithmProbe.class.getName());
        for (String sArg : asArg)
            {
            listCommand.add(sArg == null ? "" : sArg);
            }

        Process process = new ProcessBuilder(listCommand)
                .redirectErrorStream(true)
                .start();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = process.getInputStream())
            {
            in.transferTo(out);
            }

        int    nExit   = process.waitFor();
        String sOutput = out.toString(StandardCharsets.UTF_8);

        assertEquals(sOutput, nExpected, nExit);
        }

    private File weakConfig(String sAlgorithm)
            throws Exception
        {
        File file = m_folder.newFile("DefaultControllerWeak-" + sAlgorithm.replaceAll("[^A-Za-z0-9]", "_") + ".xml");
        Files.write(file.toPath(), (
                "<?xml version=\"1.0\"?>\n"
                + "<config>\n"
                + "  <keystore-type>JKS</keystore-type>\n"
                + "  <signature-algorithm>" + sAlgorithm + "</signature-algorithm>\n"
                + "</config>\n").getBytes(StandardCharsets.UTF_8));
        return file;
        }

    private File rsaConfig()
        {
        return repoFile("test/functional/security/src/main/resources/DefaultControllerRsa.xml");
        }

    private static String[] missedWeakDsaAliases()
        {
        return new String[]
            {
            "SHA1/DSA",
            "SHA-1/DSA",
            "1.2.840.10040.4.3",
            "OID.1.2.840.10040.4.3",
            "1.3.14.3.2.13",
            "1.3.14.3.2.27"
            };
        }

    private static DefaultController controller(File fileKeystore, File filePermissions, String sPassword)
            throws Exception
        {
        return new DefaultController(fileKeystore, filePermissions, false, sPassword);
        }

    private static KeyStore keyStore(File file, String sType, String sPassword)
            throws Exception
        {
        KeyStore store = KeyStore.getInstance(sType);
        try (InputStream in = new FileInputStream(file))
            {
            store.load(in, sPassword.toCharArray());
            }
        return store;
        }

    private static Subject subject(KeyStore store, String sKeyAlias, String sPrincipalAlias)
            throws Exception
        {
        return subjectWithCertificates(store, sKeyAlias, new String[] {sPrincipalAlias}, new String[] {sKeyAlias});
        }

    private static Subject subjectWithCertificates(KeyStore store, String sKeyAlias, String[] asPrincipalAlias,
            String[] asCertificateAlias)
            throws Exception
        {
        X509Certificate certSigner = (X509Certificate) store.getCertificate(sKeyAlias);
        PrivateKey      keyPrivate = privateKey(store, sKeyAlias);

        Set<X500Principal> setPrincipal = new HashSet<>();
        for (String sPrincipalAlias : asPrincipalAlias)
            {
            X509Certificate certPrincipal = (X509Certificate) store.getCertificate(sPrincipalAlias);
            setPrincipal.add(certPrincipal.getSubjectX500Principal());
            }

        Set<X509Certificate> setPublic = new HashSet<>();
        for (String sCertificateAlias : asCertificateAlias)
            {
            setPublic.add((X509Certificate) store.getCertificate(sCertificateAlias));
            }

        Set<X500PrivateCredential> setPrivate = new HashSet<>();
        setPrivate.add(new X500PrivateCredential(certSigner, keyPrivate, sKeyAlias));

        return new Subject(false, setPrincipal, setPublic, setPrivate);
        }

    private static Subject subjectWithCertPath(KeyStore store, String sKeyAlias, String sPrincipalAlias)
            throws Exception
        {
        X509Certificate certSigner    = (X509Certificate) store.getCertificate(sKeyAlias);
        X509Certificate certPrincipal = (X509Certificate) store.getCertificate(sPrincipalAlias);
        PrivateKey      keyPrivate    = privateKey(store, sKeyAlias);
        CertPath        certPath      = CertificateFactory.getInstance("X.509")
                .generateCertPath(Collections.singletonList(certSigner));

        Set<X500Principal> setPrincipal = new HashSet<>();
        setPrincipal.add(certPrincipal.getSubjectX500Principal());

        Set<CertPath> setPublic = new HashSet<>();
        setPublic.add(certPath);

        Set<X500PrivateCredential> setPrivate = new HashSet<>();
        setPrivate.add(new X500PrivateCredential(certSigner, keyPrivate, sKeyAlias));

        return new Subject(false, setPrincipal, setPublic, setPrivate);
        }

    private static PermissionInfo externalizableRoundTrip(PermissionInfo info)
            throws Exception
        {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(outBytes))
            {
            info.writeExternal(out);
            }

        PermissionInfo copy = new PermissionInfo();
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(outBytes.toByteArray())))
            {
            copy.readExternal(in);
            }
        return copy;
        }

    private static PermissionInfo binaryRoundTrip(PermissionInfo info)
        {
        return (PermissionInfo) ExternalizableHelper.fromBinary(ExternalizableHelper.toBinary(info));
        }

    private static PrivateKey privateKey(KeyStore store, String sAlias)
            throws Exception
        {
        try
            {
            return (PrivateKey) store.getKey(sAlias, "private".toCharArray());
            }
        catch (Exception e)
            {
            return (PrivateKey) store.getKey(sAlias, "password".toCharArray());
            }
        }

    private static File jksKeystore()
        {
        return repoFile("test/functional/security/keystore.jks");
        }

    private static File rsaKeystore()
        {
        return repoFile("test/functional/security/keystoreRsa.p12");
        }

    private static File permissions()
        {
        return repoFile("test/functional/security/permissions.xml");
        }

    private static String path(File file)
        {
        return file.getAbsolutePath();
        }

    private static void restoreProperty(String sProperty, String sValue)
        {
        if (sValue == null)
            {
            System.clearProperty(sProperty);
            }
        else
            {
            System.setProperty(sProperty, sValue);
            }
        }

    private static File repoFile(String sPath)
        {
        File fileDir = new File(System.getProperty("user.dir"));
        while (fileDir != null)
            {
            File file = new File(fileDir, sPath);
            if (file.exists())
                {
                return file;
                }
            fileDir = fileDir.getParentFile();
            }
        throw new IllegalStateException("Unable to locate " + sPath);
        }

    private final String m_sMode = System.getProperty("coherence.mode");

    private final String m_sConfig = System.getProperty(DefaultController.PROPERTY_CONFIG);

    /**
     * Helper entry point that isolates {@link DefaultController} static
     * initialization in a forked VM.
     */
    public static class AlgorithmProbe
        {
        public static void main(String[] asArg)
                throws Exception
            {
            String sAction = asArg[0];
            switch (sAction)
                {
                case "algorithm":
                    assertAlgorithm(asArg[1]);
                    return;
                case "round-trip":
                    roundTrip(asArg[1], asArg[2], asArg[3], asArg[4], asArg[5]);
                    return;
                case "expect-init-failure":
                    expectInitFailure();
                    return;
                case "expect-mismatch-rejected":
                    expectMismatchRejected(asArg[1], asArg[2], asArg[3], asArg[4], asArg[5]);
                    return;
                case "expect-extra-principal-rejected":
                    expectExtraPrincipalRejected(asArg[1], asArg[2], asArg[3]);
                    return;
                case "expect-signed-extra-principal-rejected":
                    expectSignedExtraPrincipalRejected(asArg[1], asArg[2], asArg[3]);
                    return;
                default:
                    throw new IllegalArgumentException("Unknown action " + sAction);
                }
            }

        private static void assertAlgorithm(String sAlgorithm)
            {
            if (!sAlgorithm.equals(DefaultController.SIGNATURE_ALGORITHM))
                {
                throw new AssertionError("Expected " + sAlgorithm + " but was "
                        + DefaultController.SIGNATURE_ALGORITHM);
                }
            }

        private static void roundTrip(String sKeystore, String sPermissions, String sType,
                String sKeyAlias, String sPrincipalAlias)
                throws Exception
            {
            DefaultController controller = controller(new File(sKeystore), new File(sPermissions), "password");
            KeyStore          store      = keyStore(new File(sKeystore), sType, "password");
            Subject           subject    = subject(store, sKeyAlias, sPrincipalAlias);
            ClusterPermission permission = new ClusterPermission("*", "all");

            Object decrypted = controller.decrypt(controller.encrypt(permission, subject), subject, null);
            controller.checkPermission((ClusterPermission) decrypted, subject);
            }

        private static void expectInitFailure()
            {
            try
                {
                String ignored = DefaultController.SIGNATURE_ALGORITHM;
                throw new AssertionError("DefaultController initialized with " + ignored);
                }
            catch (ExceptionInInitializerError e)
                {
                Throwable cause = e.getCause();
                if (cause == null || cause.getMessage() == null
                        || !cause.getMessage().contains("Weak DefaultController signature algorithm"))
                    {
                    throw e;
                    }
                }
            }

        private static void expectMismatchRejected(String sKeystore, String sPermissions, String sType,
                String sKeyAlias, String sPrincipalAlias)
                throws Exception
            {
            DefaultController controller = controller(new File(sKeystore), new File(sPermissions), "password");
            KeyStore          store      = keyStore(new File(sKeystore), sType, "password");
            Subject           subject    = subject(store, sKeyAlias, sPrincipalAlias);
            ClusterPermission permission = new ClusterPermission("*", "all");
            SignedObject      signed     = controller.encrypt(permission, subject);

            try
                {
                controller.decrypt(signed, subject, null);
                throw new AssertionError("Mismatched subject was accepted");
                }
            catch (Exception expected)
                {
                }
            }

        private static void expectExtraPrincipalRejected(String sKeystore, String sPermissions, String sType)
                throws Exception
            {
            DefaultController controller = controller(new File(sKeystore), new File(sPermissions), "password");
            KeyStore          store      = keyStore(new File(sKeystore), sType, "password");
            Subject           subject    = subjectWithCertificates(store, "worker",
                    new String[] {"worker", "manager"}, new String[] {"worker"});
            ClusterPermission permission = new ClusterPermission("service=Management", "join");

            try
                {
                controller.checkPermission(permission, subject);
                throw new AssertionError("Extra unbound principal was accepted");
                }
            catch (PermissionException expected)
                {
                }
            }

        private static void expectSignedExtraPrincipalRejected(String sKeystore, String sPermissions, String sType)
                throws Exception
            {
            DefaultController controller = controller(new File(sKeystore), new File(sPermissions), "password");
            KeyStore          store      = keyStore(new File(sKeystore), sType, "password");
            Subject           subject    = subjectWithCertificates(store, "worker",
                    new String[] {"worker", "manager"}, new String[] {"worker"});
            ClusterPermission permission = new ClusterPermission("service=Management", "join");

            Object decrypted = controller.decrypt(controller.encrypt(permission, subject), subject, null);

            try
                {
                controller.checkPermission((ClusterPermission) decrypted, subject);
                throw new AssertionError("Extra unbound principal was accepted");
                }
            catch (PermissionException expected)
                {
                }
            }
        }
    }
