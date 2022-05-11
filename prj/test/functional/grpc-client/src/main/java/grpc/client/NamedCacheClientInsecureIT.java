/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.client;

import com.oracle.coherence.client.NamedCacheClient;
import com.oracle.coherence.grpc.Requests;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * An integration test for {@link NamedCacheClient} that creates instances of {@link NamedCacheClient}
 * with a TLS enabled server and insecure client (i.e. uses TLS without CA check).
 *
 * @author Jonathan Knight  2019.11.07
 * @since 20.06
 */
class NamedCacheClientInsecureIT
        extends BaseNamedCacheClientIT
    {
    public NamedCacheClientInsecureIT()
        {
        super(s_serverHelper);
        }

    @RegisterExtension
    static ServerHelper s_serverHelper = new ServerHelper()
            .setProperty("coherence.ttl", "0")
            .setProperty("coherence.clustername", "GrpcServer")
            .setProperty("coherence.override", "coherence-json-override.xml")
            .setProperty("coherence.pof.config", "test-pof-config.xml")
            .setProperty("coherence.cacheconfig", "coherence-config.xml")
            .setProperty(Requests.PROP_CREDENTIALS, "tls")
            .setProperty(Requests.PROP_TLS_CERT, "ssl/server1.pem")
            .setProperty(Requests.PROP_TLS_KEY, "ssl/server1.key")
            .setProperty("coherence.grpc.channels.default.credentials", Requests.CREDENTIALS_INSECURE)
            ;
    }
