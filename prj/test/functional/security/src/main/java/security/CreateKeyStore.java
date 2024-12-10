/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package security;

import com.oracle.coherence.testing.AbstractTestInfrastructure;
import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;

/**
 * This class creates a set of keystores used by the Security tests.
 *
 * Note: This class dependends on sun.security.x509.* which is no longer
 *       available in JDK11.  The class needs to be built using JDK8.
 *       To build with JDK 8, modify the <configuration></configuration>
 *       of <maven-compiler-plugin></maven-compiler-plugin>
 *       section in the pom.xml by replacing
 *
 * <testExcludes>
 *   <testExclude>**\/CreateKeyStore.java</testExclude>
 * </testExcludes>
 *
 * with
 *
 * <source>${java.version}</source>
 * <target>${java.version}</target>
 *
 * where java.version = 1.8
 *
 * @author lh 2018.1l.01
 */
public class CreateKeyStore
    {
    // ----- static methods -------------------------------------------------

    public static void main(final String[] args)
            throws Exception
        {
        try
            {
            createKeyStore("workerRsa.p12", "PKCS12", "password", "worker", "private", "CN=Worker, OU=MyUnit");
            createKeyStore("adminRsa.p12", "PKCS12", "password", "admin", "private", "CN=Administrator, O=MyCompany, L=MyCity, ST=MyState");
            createKeyStore("managerRsa.p12", "PKCS12", "password", "manager", "private", "CN=Manager, OU=MyUnit");
            createKeyStore("serverRsa.p12", "PKCS12", "password", "server", "private", "CN=server, O=Oracle, L=Burlington, ST=MA, C=US");
            }
        catch (Exception e)
            {
            System.out.println("CreateKeyStore() got exception: " + e.getMessage());
            }
        }

    /**
     * Create a key store with the given information.
     *
     * @param storeName the key store name
     * @param storeType the key store type
     * @param storePass the key store password
     * @param keyName   the key name
     * @param keyPass   the key password
     * @param sDnName   the DN name
     *
     */
    public static void createKeyStore(String storeName, String storeType, String storePass, String keyName, String keyPass, String sDnName)
        {
        try {
            JavaKeyStore ks = new JavaKeyStore(storeName, storeType, storePass);
            ks.createEmptyKeyStore();
            ks.loadKeyStore();

            // Generate the key pair
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            // Generate a self signed certificate
            X509Certificate certificate = generateSelfSignedCertificate(sDnName, keyPair);

            X509Certificate[] certificateChain = new X509Certificate[1];
            certificateChain[0] = certificate;
            ks.setKeyEntry(keyName, keyPair.getPrivate(), keyPass, certificateChain);

            ks.saveKeystore();
            if (!keyName.equals("server"))
                {
                ks = new JavaKeyStore("keystoreRsa.p12", storeType, storePass);
                ks.loadKeyStore();
                ks.deleteEntry(keyName);
                ks.setKeyEntry(keyName, keyPair.getPrivate(), keyPass, certificateChain);
                ks.saveKeystore();
                }
            }
        catch (Exception e)
            {
            System.out.println("Got exception in createKeyStore(): " + e.getMessage());
            }
        }

    /**
     * Generate a self-signed X509 certificate.
     *
     * @param sDnName  the DN name
     * @param keyPair  the key pair
     *
     * @return a X509 certificate.
     */
    private static X509Certificate generateSelfSignedCertificate(String sDnName, KeyPair keyPair)
            throws CertificateException, IOException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException
        {
        X509CertInfo certInfo = new X509CertInfo();
        // Serial number and version
        certInfo.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(new BigInteger(64, new SecureRandom())));
        certInfo.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));

        // Subject & Issuer
        X500Name owner = new X500Name(sDnName);
        certInfo.set(X509CertInfo.SUBJECT, owner);
        certInfo.set(X509CertInfo.ISSUER, owner);

        // Key and algorithm
        certInfo.set(X509CertInfo.KEY, new CertificateX509Key(keyPair.getPublic()));
        AlgorithmId algorithm = new AlgorithmId(AlgorithmId.sha256WithRSAEncryption_oid);
        certInfo.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algorithm));

        // Validity
        Date validFrom = new Date();
        Date validTo = new Date(validFrom.getTime() + 50L * 365L * 24L * 60L * 60L * 1000L); //50 years
        CertificateValidity validity = new CertificateValidity(validFrom, validTo);
        certInfo.set(X509CertInfo.VALIDITY, validity);

        // Create certificate and sign it
        X509CertImpl cert = new X509CertImpl(certInfo);
        cert.sign(keyPair.getPrivate(), SHA256WITHRSA);

        // Since the SHA256withRSA provider may have a different algorithm ID to what we think it should be,
        // we need to reset the algorithm ID, and resign the certificate
        AlgorithmId actualAlgorithm = (AlgorithmId) cert.get(X509CertImpl.SIG_ALG);
        certInfo.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, actualAlgorithm);
        X509CertImpl newCert = new X509CertImpl(certInfo);
        newCert.sign(keyPair.getPrivate(), SHA256WITHRSA);

        return newCert;
        }

    /**
     * A Java key store class.
     */
    static class JavaKeyStore
        {
        private KeyStore keyStore;

        private String keyStoreName;
        private String keyStoreType;
        private String keyStorePassword;

        JavaKeyStore(String keyStoreName, String keyStoreType, String keyStorePassword) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException
            {
            this.keyStoreName = keyStoreName;
            this.keyStoreType = keyStoreType;
            this.keyStorePassword = keyStorePassword;
            }

        void createEmptyKeyStore() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException
            {
            if(keyStoreType ==null || keyStoreType.isEmpty())
                {
                keyStoreType = KeyStore.getDefaultType();
                }

            keyStore = KeyStore.getInstance(keyStoreType);
            char[] pwdArray = keyStorePassword == null ? null : keyStorePassword.toCharArray();

            //load
            keyStore.load(null, pwdArray);

            // Save the keyStore
            saveKeystore();
            }

        void loadKeyStore() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException
            {
            if (keyStore == null)
                {
                keyStore = KeyStore.getInstance(keyStoreType);
                }

            char[] pwdArray = keyStorePassword == null ? null : keyStorePassword.toCharArray();
            keyStore.load(new FileInputStream(getFileName()), pwdArray);
            }

        void setEntry(String alias, KeyStore.SecretKeyEntry secretKeyEntry, KeyStore.ProtectionParameter protectionParameter) throws KeyStoreException
            {
            keyStore.setEntry(alias, secretKeyEntry, protectionParameter);
            }

        KeyStore.Entry getEntry(String alias) throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException
            {
            char[]                       pwdArray  = keyStorePassword == null ? null : keyStorePassword.toCharArray();
            KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(pwdArray);
            return keyStore.getEntry(alias, protParam);
            }

        void setKeyEntry(String alias, PrivateKey privateKey, String keyPassword, Certificate[] certificateChain) throws KeyStoreException
            {
            keyStore.setKeyEntry(alias, privateKey, keyPassword.toCharArray(), certificateChain);
            }

        void setCertificateEntry(String alias, Certificate certificate) throws KeyStoreException
            {
            keyStore.setCertificateEntry(alias, certificate);
            }

        Certificate getCertificate(String alias) throws KeyStoreException
            {
            return keyStore.getCertificate(alias);
            }

        void deleteEntry(String alias) throws KeyStoreException
            {
            keyStore.deleteEntry(alias);
            }

        void deleteKeyStore() throws KeyStoreException, IOException
            {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements())
                {
                String alias = aliases.nextElement();
                keyStore.deleteEntry(alias);
                }
            keyStore = null;
            Files.delete(Paths.get(getFileName()));
            }

        /**
         * Save the KeyStore (to a file).
         */
        void saveKeystore() throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException
            {
            // Save the keyStore
            FileOutputStream fos      = new FileOutputStream(getFileName());
            char[]           pwdArray = keyStorePassword == null ? null : keyStorePassword.toCharArray();

            keyStore.store(fos, pwdArray);
            fos.close();
            }

        KeyStore getKeyStore()
            {
            return this.keyStore;
            }

        /**
         * Returns the KeyStore file name; it's in the Security functional tests directory.
         */
        String getFileName() throws IOException
            {
            return AbstractTestInfrastructure.getProjectDir("security").getCanonicalPath() + File.separator + keyStoreName;
            }
        }

    // ----- data members ---------------------------------------------------

    private static final String SHA256WITHRSA = "SHA256withRSA";
    }
