/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package grpc.client;

import com.oracle.coherence.testing.util.KeyToolExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

public class LocalSecureDefaultCacheConfigGrpcIT
        extends BaseLocalDefaultCacheConfigGrpcIT
    {
    @BeforeAll
    static void setupCluster() throws Exception
        {
        System.setProperty("coherence.grpc.server.socketprovider", "tls-files");
        System.setProperty("coherence.grpc.socketprovider", "tls-files");
        System.setProperty("coherence.security.key", KEY_TOOL.getKeyAndCert().getKeyPEMNoPassURI());
        System.setProperty("coherence.security.cert", KEY_TOOL.getKeyAndCert().getCertURI());
        System.setProperty("coherence.security.ca.cert", KEY_TOOL.getCaCert().getCertURI());

        runCluster();
        }

    // ----- data members ---------------------------------------------------

    @RegisterExtension
    static final KeyToolExtension KEY_TOOL = new KeyToolExtension();
    }
