/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.ssl;

import com.tangosol.net.PasswordProvider;
import org.junit.Test;

import java.net.URL;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class URLPrivateKeyLoaderTest
        extends AbstractKeyAndCertTest
    {
    @Test
    public void shouldLoadPrivateKey() throws Exception
        {
        URL                 url    = s_keyAndCert.m_fileKeyPEMNoPass.toURI().toURL();
        URLPrivateKeyLoader loader = new URLPrivateKeyLoader(url.toExternalForm());
        PrivateKey          key    = loader.load(PasswordProvider.NullImplementation);

        assertThat(key, is(notNullValue()));
        }

    @Test
    public void shouldLoadEncryptedPrivateKey() throws Exception
        {
        URL                 url    = s_keyAndCert.m_fileKeyPEM.toURI().toURL();
        URLPrivateKeyLoader loader = new URLPrivateKeyLoader(url.toExternalForm());
        PrivateKey          key    = loader.load(() -> s_keyAndCert.keyPassword());

        assertThat(key, is(notNullValue()));
        }
    }
