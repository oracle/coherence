/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.ssl;

import org.junit.Test;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class URLKeyStoreLoaderTest
        extends AbstractKeyAndCertTest
    {
    @Test
    public void shouldLoadKeyStoreJKS() throws Exception
        {
        URLKeyStoreLoader loader   = new URLKeyStoreLoader(s_keyAndCert.getKeystoreURI());
        KeyStore          keyStore = loader.load("JKS", () -> s_keyAndCert.storePassword());

        assertThat(keyStore, is(notNullValue()));
        assertThat(keyStore.getType(), is("JKS"));
        }

    @Test
    public void shouldLoadKeyStorePKCS12() throws Exception
        {
        URLKeyStoreLoader loader   = new URLKeyStoreLoader(s_keyAndCert.getP12KeystoreURI());
        KeyStore          keyStore = loader.load("PKCS12", () -> s_keyAndCert.storePassword());

        assertThat(keyStore, is(notNullValue()));
        assertThat(keyStore.getType(), is("PKCS12"));
        }

    @Test
    public void shouldLoadKeyStoreWithKeyAndCert() throws Exception
        {
        KeyStoreLoader       keyStoreLoader = EmptyKeyStoreLoader.INSTANCE;
        URLPrivateKeyLoader  keyLoader      = new URLPrivateKeyLoader(s_keyAndCert.getKeyPEMNoPassURI());
        PrivateKey           key            = keyLoader.load(null);
        URLCertificateLoader certLoader     = new URLCertificateLoader(s_keyAndCert.getCertURI());
        Certificate[]        aCert          = certLoader.load();
        KeyStore             keyStore       = keyStoreLoader.load("JKS", null, key, null, aCert);

        assertThat(keyStore, is(notNullValue()));
        assertThat(keyStore.getType(), is("JKS"));
        assertThat(keyStore.size(), is(1));
        assertThat(keyStore.isKeyEntry("key"), is(true));
        assertThat(keyStore.getKey("key", new char[0]), is(key));
        assertThat(keyStore.getCertificate("key"), is(aCert[0]));
        }

    @Test
    public void shouldLoadKeyStoreWithEncryptedKeyAndCert() throws Exception
        {
        KeyStoreLoader       keyStoreLoader = EmptyKeyStoreLoader.INSTANCE;
        URLPrivateKeyLoader  keyLoader      = new URLPrivateKeyLoader(s_keyAndCert.getKeyPEMURI());
        PrivateKey           key            = keyLoader.load(() -> s_keyAndCert.keyPassword());
        URLCertificateLoader certLoader     = new URLCertificateLoader(s_keyAndCert.getCertURI());
        Certificate[]        aCert          = certLoader.load();
        KeyStore             keyStore       = keyStoreLoader.load("JKS", null, key, () -> s_keyAndCert.keyPassword(), aCert);

        assertThat(keyStore, is(notNullValue()));
        assertThat(keyStore.getType(), is("JKS"));
        assertThat(keyStore.size(), is(1));
        assertThat(keyStore.isKeyEntry("key"), is(true));
        assertThat(keyStore.getKey("key", s_keyAndCert.keyPassword()), is(key));
        assertThat(keyStore.getCertificate("key"), is(aCert[0]));
        }

    @Test
    public void shouldLoadKeyStoreWithCerts() throws Exception
        {
        KeyStoreLoader       keyStoreLoader = EmptyKeyStoreLoader.INSTANCE;
        URLCertificateLoader certLoader     = new URLCertificateLoader(s_keyAndCert.getCertURI());
        URLCertificateLoader caCertLoader   = new URLCertificateLoader(s_caCert.getCertURI());
        Certificate          cert           = certLoader.load()[0];
        Certificate          caCert         = caCertLoader.load()[0];
        Certificate[]        aCert          = new Certificate[]{cert, caCert};
        KeyStore             keyStore       = keyStoreLoader.load("PKCS12", null, aCert);

        assertThat(keyStore, is(notNullValue()));
        assertThat(keyStore.getType(), is("PKCS12"));
        assertThat(keyStore.size(), is(2));
        assertThat(keyStore.isCertificateEntry("cert-0"), is(true));
        assertThat(keyStore.getCertificate("cert-0"), is(aCert[0]));
        assertThat(keyStore.isCertificateEntry("cert-1"), is(true));
        assertThat(keyStore.getCertificate("cert-1"), is(aCert[1]));
        }
    }
