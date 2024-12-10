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
 * Test SSL using custom key and certificate loader.
 *
 * @author Tim Middleton 2022.06.16
 */
public class CustomKeyAndCertSSLExampleTest
        extends AbstractSSLExampleTest {

    @Test
    public void testCustomKeyAndCertSocketProvider() {
        runTest("sslCustomKeyAndCert");  // <1>
    }
}
// #end::test[]
