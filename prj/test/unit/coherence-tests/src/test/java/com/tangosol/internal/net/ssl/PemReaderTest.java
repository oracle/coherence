/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.ssl;

import com.tangosol.net.PasswordProvider;
import com.tangosol.net.ssl.AbstractKeyAndCertTest;

import org.junit.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class PemReaderTest
        extends AbstractKeyAndCertTest
    {
    @Test
    public void shouldReadPrivateKey() throws Exception
        {
        try (InputStream in = new FileInputStream(s_keyAndCert.getKeyPEMNoPass()))
            {
            PrivateKey key = PemReader.readPrivateKey(in, PasswordProvider.NullImplementation);
            assertThat(key, is(notNullValue()));
            }
        }

    @Test
    public void shouldReadEncodedPrivateKey() throws Exception
        {
        try (InputStream in = new FileInputStream(s_keyAndCert.getKeyPEM()))
            {
            PrivateKey key = PemReader.readPrivateKey(in, () -> s_keyAndCert.keyPassword());
            assertThat(key, is(notNullValue()));
            }
        }

    @Test
    public void shouldReadCertificate() throws Exception
        {
        try (InputStream in = new FileInputStream(s_keyAndCert.getCert()))
            {
            Certificate[] aCert = PemReader.readCertificates(in);
            assertThat(aCert, is(notNullValue()));
            assertThat(aCert.length, is(1));
            assertThat(aCert[0], is(notNullValue()));
            }
        }

    @Test
    public void shouldReadCACertificate() throws Exception
        {
        try (InputStream in = new FileInputStream(s_caCert.getCert()))
            {
            Certificate[] aCert = PemReader.readCertificates(in);
            assertThat(aCert, is(notNullValue()));
            assertThat(aCert.length, is(1));
            assertThat(aCert[0], is(notNullValue()));
            }
        }
    }
