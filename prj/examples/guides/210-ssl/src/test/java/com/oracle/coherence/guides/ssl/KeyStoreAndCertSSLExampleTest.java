/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.ssl;

import org.junit.jupiter.api.Test;

// #tag::test[]
/**
 * Test SSL using Key and Certificate.
 *
 * @author Tim Middleton 2022.06.15
 */
public class KeyStoreAndCertSSLExampleTest
        extends AbstractSSLExampleTest {

    @Test
    public void testKeyAndCertSocketProvider() throws Exception {
        runTest("sslKeyAndCert");   // <1>
    }
}
// #end::test[]
