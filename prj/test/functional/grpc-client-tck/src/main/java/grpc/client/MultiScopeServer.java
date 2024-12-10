/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package grpc.client;

import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.SessionConfiguration;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A class that runs a multi-scoped Coherence server.
 */
public class MultiScopeServer
    {
    public static void main(String[] args)
        {
        List<SessionConfiguration> list = SCOPE_NAMES.stream()
                .map(sScope -> SessionConfiguration.builder()
                        .named(sScope)
                        .withScopeName(sScope)
                        .build())
                .collect(Collectors.toList());

        CoherenceConfiguration cfg = CoherenceConfiguration.builder()
                .withSessions(list)
                .build();

        Coherence coherence = Coherence.clusterMember(cfg);
        coherence.start();
        coherence.whenClosed().join();
        }

    public static final Set<String> SCOPE_NAMES = Set.of("scope-one", "scope-two");
    }
