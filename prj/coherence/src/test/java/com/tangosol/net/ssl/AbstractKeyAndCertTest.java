/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.ssl;

import org.junit.BeforeClass;
import util.KeyTool;

/**
 * A base class for tests requiring a simple self-signed key and cert.
 */
public class AbstractKeyAndCertTest
    {
    @BeforeClass
    public static void setup() throws Exception
        {
        KeyTool.assertCanCreateKeys();
        s_caCert     = KeyTool.createCACert("test-ca", "PKCS12");
        s_keyAndCert = KeyTool.createKeyCertPair(s_caCert, "test");
        }

    // ----- data members ---------------------------------------------------

    protected static KeyTool.KeyAndCert s_caCert;

    protected static KeyTool.KeyAndCert s_keyAndCert;
    }
