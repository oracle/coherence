/*
 * Copyright (c) 2022, 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.queries;

import com.oracle.coherence.guides.queries.model.Country;
import com.oracle.coherence.guides.queries.utils.CoherenceHelper;
import com.tangosol.net.Coherence;

import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;

import com.tangosol.util.Filter;
import com.tangosol.util.Filters;

import com.tangosol.util.aggregator.BigDecimalSum;
import com.tangosol.util.aggregator.ReducerAggregator;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A simple test class showing basic Coherence {@link com.tangosol.net.NamedMap}
 * CRUD operations.
 *
 * @author Gunnar Hillert  2022.02.25
 */
class QueryTests {
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

    // # tag::testGreaterEqualsFilter[]
    @Test
    void testGreaterEqualsFilter() {

        NamedMap<String, Country> map = getMap("countries"); // <1>
        Filter<Country> filter = Filters.greaterEqual(Country::getPopulation, 60.0); // <2>

        final Set<Map.Entry<String, Country>> results = map.entrySet(filter); // <3>

        assertThat(results).hasSize(2); // <4>

        map.entrySet(filter).forEach(entry -> { // <5>
            assertThat(entry.getKey()).containsAnyOf("de", "fr");
            assertThat(entry.getValue().getPopulation()).isGreaterThan(60.0);
        });
    }
    // # end::testGreaterEqualsFilter[]

    // # tag::testValueExtractor[]
    @Test
    void testValueExtractor() {

        NamedMap<String, Country> map = getMap("countries"); // <1>
        Filter<Country> filter = Filters.greaterEqual(Country::getPopulation, 60.0); // <2>

        ReducerAggregator<String, Country, Country, String> aggregator
                = new ReducerAggregator<>(Country::getName); // <3>

        Map<String, String> result = map.aggregate(filter, aggregator); // <4>

        result.forEach((key, value) -> { // <5>
            assertThat(key).containsAnyOf("de", "fr");
            assertThat(value).containsAnyOf("Germany", "France");
        });
    }
    // # end::testValueExtractor[]

    // # tag::testAggregator[]
    @Test
    void testAggregate() {

        NamedMap<String, Country> map = getMap("countries"); // <1>
        Filter<Country> filter = Filters.greaterEqual(Country::getPopulation, 60.0); // <2>
        BigDecimalSum<BigDecimal> aggregator = new BigDecimalSum<>("getPopulation"); // <3>
        BigDecimal result = map.aggregate(filter, aggregator); // <4>
        String resultAsString = result.setScale(2, RoundingMode.HALF_UP) // <5>
                .stripTrailingZeros() // <6>
                .toPlainString(); // <7>
        assertThat(resultAsString).isEqualTo("150.6"); // <8>
    }
    // # end::testAggregator[]

    // # tag::get-map[]
    <K, V> NamedMap<K, V> getMap(String name) {
        Coherence coherence = Coherence.getInstance(); // <1>
        Session   session   = coherence.getSession(); // <2>
        return session.getMap(name); // <3>
    }
    // # end::get-map[]
}
