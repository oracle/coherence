/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.vectors;


import com.oracle.bedrock.junit.CoherenceClusterExtension;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.junit.TestLogsExtension;
import com.oracle.coherence.ai.QueryResult;
import com.oracle.coherence.io.json.JsonObject;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;
import com.tangosol.util.Extractors;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.Resources;
import com.tangosol.util.ValueExtractor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URL;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class VectorsIT {
    /**
     * The test cluster name.
     */
    public static final String CLUSTER_NAME = "vector-store";

    /**
     * A JUnit 5 extension to capture cluster member logs.
     */
    @RegisterExtension
    static final TestLogsExtension testLogs = new TestLogsExtension(VectorsIT.class);

    /**
     * A JUnit 5 extension that runs a Coherence cluster.
     */
    @RegisterExtension
    static final CoherenceClusterExtension clusterRunner = new CoherenceClusterExtension()
            .with(WellKnownAddress.of("127.0.0.1"),
                    ClusterName.of(CLUSTER_NAME),
                    IPv4Preferred.autoDetect(),
                    LocalHost.only())
            .include(3, CoherenceClusterMember.class,
                    LocalStorage.enabled(),
                    testLogs,
                    RoleName.of("storage"),
                    DisplayName.of("storage"));

    static Coherence coherence;

    static Session session;

    @BeforeAll
    static void setup() throws Exception {
        System.setProperty("coherence.cluster", CLUSTER_NAME);
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.role", "client");

        coherence = Coherence.client().startAndWait();
        session   = coherence.getSession();
    }

    @Test
    void shouldStoreVectors() throws Exception {
        NamedMap<String, JsonObject> movies  = session.getMap("movies");
        MovieRepository              movieDb = new MovieRepository(movies);

        URL url = Resources.findFileOrResource("movies.json.gzip", null);
        try (GZIPInputStream in = new GZIPInputStream(url.openStream())) {
            movieDb.load(in);
        }

        List<QueryResult<String, JsonObject>> results = movieDb.search("star travel and space ships", 5);
        for (QueryResult<String, JsonObject> result : results) {
            JsonObject json = result.getValue();
            String title = json.getString("title");
            String plot  = json.getString("plot");
            double distance = result.getDistance();
            System.out.printf("%s: %f\n%s\n", title, distance, plot);
        }

        ValueExtractor<JsonObject, List<String>> castExtractor = Extractors.extract("cast");
        Filter<JsonObject> filter = Filters.contains(castExtractor, "Harrison Ford");

        results = movieDb.search("star travel and space ships", filter, 2);
        for (QueryResult<String, JsonObject> result : results) {
            JsonObject json = result.getValue();
            String title = json.getString("title");
            String plot  = json.getString("plot");
            double distance = result.getDistance();
            System.out.printf("%s: %f\n%s\n", title, distance, plot);
        }
    }

}
