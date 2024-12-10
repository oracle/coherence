/*
 * Copyright (c) 2022, 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.queries;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.guides.queries.model.Country;
import com.oracle.coherence.guides.queries.utils.CoherenceHelper;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;

import com.tangosol.net.Session;
import com.tangosol.net.cache.ContinuousQueryCache;

import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.MapListener;

import com.tangosol.util.aggregator.BigDecimalSum;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;

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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * A test class showing showing the usage of Coherence Views using ContinuousQueryCache.
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
        Filter<Country> filter = Filters.greaterEqual(Country::getPopulation, 60.0); // <2>

        Set<Map.Entry<String, Country>> results = map.entrySet(filter); // <3>

        assertThat(results, hasSize(2)); // <4>

        Country mexico = new Country("Mexico", "Ciudad de México", 126.01); // <5>
        map.put("mx", mexico);

        assertThat(results, hasSize(2)); // <6>

    }
    // # end::testGreaterEqualsFilterWithChanges[]

    // # tag::testGreaterEqualsFilterWithContinuousQueryCache[]
    @Test
    void testGreaterEqualsFilterWithContinuousQueryCache() {

        NamedCache<String, Country> map = getMap("countries"); // <1>
        Filter<Country> filter = Filters.greaterEqual(Country::getPopulation, 60.0); // <2>

        ContinuousQueryCache results = new ContinuousQueryCache(map, filter); // <3>

        assertThat(results.size(), is(2)); // <4>

        Country mexico = new Country("Mexico", "Ciudad de México", 126.01); // <5>
        map.put("mx", mexico);

        assertThat(results.size(), is(3)); // <6>
    }
    // # end::testGreaterEqualsFilterWithContinuousQueryCache[]

    // # tag::testContinuousQueryCacheWithListener[]
    @Test
    void testContinuousQueryCacheWithListener() {

        NamedCache<String, Country> map = getMap("countries");
        Filter<Country> filter = Filters.greaterEqual(Country::getPopulation, 60.0); // <2>

        ContinuousQueryCache results = new ContinuousQueryCache(map, filter);

        AtomicInteger counter = new AtomicInteger(0); // <1>

        MapListener<String, Double> listener = new SimpleMapListener<String, Double>() // <2>
                .addInsertHandler((event) -> {
                    counter.incrementAndGet();
                });
        results.addMapListener(listener);  // <3>


        assertThat(results.size(), is(2)); // <4>

        Country mexico = new Country("Mexico", "Ciudad de México", 126.01);  // <5>
        map.put("mx", mexico);

        assertThat(results.size(), is(3)); // <6>
        Eventually.assertDeferred(counter::get, is(1)); // <7>
    }
    // # end::testContinuousQueryCacheWithListener[]

    // # tag::testAggregate[]
    @Test
    void testAggregate() {

        NamedCache<String, Country> map = getMap("countries");
        Filter<Country> filter = Filters.greaterEqual(Country::getPopulation, 60.0); // <2>
        ReflectionExtractor<Country, Double> extractor = new ReflectionExtractor<>("getPopulation");
        ContinuousQueryCache<String, Country, Double> results = new ContinuousQueryCache(map, filter, extractor);

        BigDecimalSum<BigDecimal> aggregator = new BigDecimalSum(new IdentityExtractor<>()); // <1>
        AtomicReference<BigDecimal> aggregatedPopulation = new AtomicReference<>(formatNumber(results.aggregate(aggregator))); // <2>

        MapListener<String, Double> listener = new SimpleMapListener<String, Double>()  // <3>
                .addInsertHandler((event) -> {
                    aggregatedPopulation.set(formatNumber(results.aggregate(aggregator)));
                });
        results.addMapListener(listener); // <4>

        assertThat(aggregatedPopulation.get(), is(formatNumber(150.6))); // <5>

        Country mexico = new Country("Mexico", "Ciudad de México", 126.01); // <6>
        map.put("mx", mexico);

        assertThat(results.size(), is(3)); // <7>
        Eventually.assertDeferred(aggregatedPopulation::get, is(formatNumber(276.61))); // <8>

    }
    // # end::testAggregate[]

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
