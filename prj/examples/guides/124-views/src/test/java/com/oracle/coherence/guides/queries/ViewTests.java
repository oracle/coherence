/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.queries;

import com.oracle.coherence.guides.queries.model.Country;
import com.oracle.coherence.guides.queries.utils.CoherenceHelper;
import com.tangosol.net.Coherence;

import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.net.cache.ContinuousQueryCache;
import com.tangosol.util.Filter;
import com.tangosol.util.MapListener;
import com.tangosol.util.aggregator.BigDecimalSum;
import com.tangosol.util.aggregator.ReducerAggregator;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.filter.GreaterEqualsFilter;
import com.tangosol.util.listener.SimpleMapListener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A simple test class showing basic Coherence {@link com.tangosol.net.NamedMap}
 * CRUD operations.
 *
 * @author Gunnar Hillert  2022.02.25
 */
class ViewTests {
    // # tag::bootstrap[]
    @BeforeAll
    static void boostrapCoherence() {
        CoherenceHelper.startCoherence(); // <1>
    }
    // # end::bootstrap[]

    // # tag::cleanup[]
    @AfterAll
    static void shutdownCoherence() {
        Coherence coherence = Coherence.getInstance(); //<1>
        coherence.close();
    }
    // # end::cleanup[]

    @AfterEach
    void cleanupCountriesCache() {
        NamedCache<String, Country> map = getMap("countries");
        map.remove("mx");
    }

    // # tag::testGreaterEqualsFilterWithChanges[]
    @Test
    void testGreaterEqualsFilterWithChanges() {

        NamedCache<String, Country> map = getMap("countries"); // <1>
        Filter filter = new GreaterEqualsFilter("getPopulation", 60.0); // <2>

        Set<Map.Entry<String, Country>> results = map.entrySet(filter); // <3>

        assertThat(results).hasSize(2); // <4>

        Country mexico = new Country("Mexico", "Ciudad de México", 126.01); // <5>
        map.put("mx", mexico);

        assertThat(results).hasSize(2); // <6>

    }
    // # end::testGreaterEqualsFilterWithChanges[]

    // # tag::testGreaterEqualsFilterWithContinuousQueryCache[]
    @Test
    void testGreaterEqualsFilterWithContinuousQueryCache() {

        NamedCache<String, Country> map = getMap("countries"); // <1>
        Filter filter = new GreaterEqualsFilter("getPopulation", 60.0); // <2>

        ContinuousQueryCache results = new ContinuousQueryCache(map, filter); // <3>

        assertThat(results).hasSize(2); // <1>

        Country mexico = new Country("Mexico", "Ciudad de México", 126.01);
        map.put("mx", mexico);

        assertThat(results).hasSize(3); // <2>
    }
    // # end::testGreaterEqualsFilterWithContinuousQueryCache[]

    // # tag::testObservingContinuousQueryCache[]
    @Test
    void testObservingContinuousQueryCache() {

        NamedCache<String, Country> map = getMap("countries"); // <1>
        Filter filter = new GreaterEqualsFilter("getPopulation", 60.0); // <2>

        ReducerAggregator<String, Country, String, String> aggregator
                = new ReducerAggregator<>("getName"); // <3>

        ContinuousQueryCache<String, Country, Country> results = new ContinuousQueryCache(map, filter); // <3>
        ContinuousQueryCache<String, Country, Country> results2 = new ContinuousQueryCache(map, filter, new ReflectionExtractor("getName")); // <3>


        Map<String, String> result = results.aggregate(aggregator); // <4>

        result.forEach((key, value) -> { // <5>
            assertThat(key).containsAnyOf("de", "fr");
            assertThat(value).containsAnyOf("Germany", "France");
        });

        Country mexico = new Country("Mexico", "Ciudad de México", 126.01);
        map.put("mx", mexico);

        result.forEach((key, value) -> { // <5>
            assertThat(key).containsAnyOf("de", "fr", "mx");
            assertThat(value).containsAnyOf("Germany", "France", "Mexico");
        });
    }
    // # end::testObservingContinuousQueryCache[]

    // # tag::testContinuousQueryCacheWithListener[]
    @Test
    void testContinuousQueryCacheWithListener() {

        NamedCache<String, Country> map = getMap("countries");
        Filter filter = new GreaterEqualsFilter("getPopulation", 60.0);

        ContinuousQueryCache results = new ContinuousQueryCache(map, filter);

        AtomicInteger counter = new AtomicInteger(0); // <1>

        MapListener<String, Double> listener = new SimpleMapListener<String, Double>() // <2>
                .addInsertHandler((event) -> {
                    counter.incrementAndGet();
                });
        results.addMapListener(listener);  // <3>


        assertThat(results).hasSize(2); // <4>

        Country mexico = new Country("Mexico", "Ciudad de México", 126.01);  // <5>
        map.put("mx", mexico);

        assertThat(results).hasSize(3); // <6>
        assertThat(counter.get()).isEqualTo(1); // <7>
    }
    // # end::testContinuousQueryCacheWithListener[]

    // # tag::testAggregate[]
    @Test
    void testAggregate() {

        NamedCache<String, Country> map = getMap("countries");
        Filter filter = new GreaterEqualsFilter("getPopulation", 60.0);
        ReflectionExtractor<Country, Double> extractor = new ReflectionExtractor<>("getPopulation");
        ContinuousQueryCache<String, Country, Double> results = new ContinuousQueryCache(map, filter, extractor);

        BigDecimalSum<BigDecimal> aggregator = new BigDecimalSum(new IdentityExtractor<>()); // <1>
        AtomicReference<BigDecimal> aggregatedPopulation = new AtomicReference<>(formatNumber(results.aggregate(aggregator))); // <4>

        MapListener<String, Double> listener = new SimpleMapListener<String, Double>()
                .addInsertHandler((event) -> {
                    aggregatedPopulation.set(formatNumber(results.aggregate(aggregator)));
                });
        results.addMapListener(listener);

        assertThat(aggregatedPopulation.get()).isEqualTo(formatNumber(150.6)); // <8>

        Country mexico = new Country("Mexico", "Ciudad de México", 126.01);
        map.put("mx", mexico);

        assertThat(results).hasSize(3);
        assertThat(aggregatedPopulation.get()).isEqualTo(formatNumber(276.61)); // <8>

    }
    // # end::testAggregator[]

    private BigDecimal formatNumber(double input) {
        return formatNumber(BigDecimal.valueOf(input));
    }

    private BigDecimal formatNumber(BigDecimal input) {
        return input.setScale(2, RoundingMode.HALF_UP) // <5>
             .stripTrailingZeros();
    }

    // # tag::get-map[]
    <K, V> NamedCache<K, V> getMap(String name) {
        Coherence coherence = Coherence.getInstance(); // <1>
        Session   session   = coherence.getSession(); // <2>
        return session.getCache(name); // <3>
    }
    // # end::get-map[]
}
