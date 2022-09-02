/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.ssl;

import org.junit.Test;

import java.security.cert.Certificate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class URLCertificateLoaderTest
        extends AbstractKeyAndCertTest
    {
    @Test
    public void shouldLoadCerts() throws Exception
        {
        URLCertificateLoader loader = new URLCertificateLoader(s_keyAndCert.getCertURI());
        Certificate[]        aCert  = loader.load();

        assertThat(aCert, is(notNullValue()));
        assertThat(aCert.length, is(1));
        assertThat(aCert[0], is(notNullValue()));
        }
    }
