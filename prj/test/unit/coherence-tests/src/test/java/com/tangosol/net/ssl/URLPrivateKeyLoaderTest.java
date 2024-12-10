/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.ssl;

import com.tangosol.net.PasswordProvider;
import org.junit.Test;

import java.security.PrivateKey;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class URLPrivateKeyLoaderTest
        extends AbstractKeyAndCertTest
    {
    @Test
    public void shouldLoadPrivateKey() throws Exception
        {
        URLPrivateKeyLoader loader = new URLPrivateKeyLoader(s_keyAndCert.getKeyPEMNoPassURI());
        PrivateKey          key    = loader.load(PasswordProvider.NullImplementation);

        assertThat(key, is(notNullValue()));
        }

    @Test
    public void shouldLoadEncryptedPrivateKey() throws Exception
        {
        URLPrivateKeyLoader loader = new URLPrivateKeyLoader(s_keyAndCert.getKeyPEMURI());
        PrivateKey          key    = loader.load(() -> s_keyAndCert.keyPassword());

        assertThat(key, is(notNullValue()));
        }
    }
