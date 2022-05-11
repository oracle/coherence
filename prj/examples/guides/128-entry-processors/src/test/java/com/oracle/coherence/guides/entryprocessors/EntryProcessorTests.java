/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.entryprocessors;

import com.oracle.coherence.guides.entryprocessors.model.Country;
import com.oracle.coherence.guides.entryprocessors.utils.CoherenceHelper;
import com.tangosol.net.Coherence;

import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.util.Filter;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.GreaterEqualsFilter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A test class showing the usage of Coherence {@link com.tangosol.util.InvocableMap.EntryProcessor}.
 *
 * @author Gunnar Hillert  2022.05.04
 */
class EntryProcessorTests {
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

    @BeforeEach
    void prepareCountriesCache() {
        NamedCache<String, Country> countries = getMap("countries");
        countries.put("de", new Country("Germany", "Berlin", 83.2));
        countries.put("fr", new Country("France", "Paris", 67.4));
        countries.put("ua", new Country("Ukraine", "Kyiv", 41.2));
        countries.put("co", new Country("Colombia", "Bogotá", 50.4));
        countries.put("au", new Country("Australia", "Canberra", 26));
    }

    @AfterEach
    void cleanupCountriesCache() {
        NamedCache<String, Country> countries = getMap("countries");
        countries.clear();
    }

    // # tag::testIncreasePopulationWithoutEntryProcessor[]
    @Test
    void testIncreasePopulationWithoutEntryProcessor() {

        NamedCache<String, Country> map = getMap("countries"); // <1>
        Filter filter = new GreaterEqualsFilter("getPopulation", 60.0); // <2>

        Set<String> filteredKeys = map.keySet(filter); // <3>
        assertThat(filteredKeys).hasSize(2); // <4>

        for (String key : filteredKeys) {  // <5>
            map.lock(key, 0); // <6>
            try {
                Country country = map.get(key);
                country.setPopulation(country.getPopulation() + 1); // <7>
                map.put(key, country); // <8>
            }
            finally {
                map.unlock(key);
            }
        }

        assertThat(map).hasSize(5);
        Country germany = map.get("de");
        Country france = map.get("fr");
        assertThat(germany.getPopulation()).isEqualTo(84.2d);
        assertThat(france.getPopulation()).isEqualTo(68.4d);
    }
    // # end::testIncreasePopulationWithoutEntryProcessor[]

    // # tag::testIncreasePopulationWithCustomEntryProcessor[]
    @Test
    void testIncreasePopulationWithCustomEntryProcessor() {
        NamedCache<String, Country> map = getMap("countries"); // <1>
        Filter filter = new GreaterEqualsFilter("getPopulation", 60.0); // <2>

        final Map<String, Double> results = map.invokeAll(filter, new IncrementingEntryProcessor()); // <3>

        assertThat(results).hasSize(2); // <4>
        assertThat(results.get("de")).isEqualTo(84.2d);
        assertThat(results.get("fr")).isEqualTo(68.4d);
    }
    // # end::testIncreasePopulationWithCustomEntryProcessor[]

    // # tag::testIncreasePopulationForSingleEntry[]
    @Test
    void testIncreasePopulationForSingleEntry() {
        NamedCache<String, Country> map = getMap("countries"); // <1>
        final Double result = map.invoke("de", new IncrementingEntryProcessor()); // <2>
        assertThat(result).isEqualTo(84.2d);
    }
    // # end::testIncreasePopulationForSingleEntry[]

    // # tag::testIncreasePopulationUsingLambdaExpression[]
    @Test
    void testIncreasePopulationUsingLambdaExpression() {
        NamedCache<String, Country> map = getMap("countries"); // <1>
        Filter filter = new GreaterEqualsFilter("getPopulation", 60.0); // <2>

        final Map<String, Double> results = map.invokeAll(filter, entry -> {  // <3>
            Country country = entry.getValue();
            country.setPopulation(country.getPopulation() + 1);
            return country.getPopulation();
        });

        assertThat(results).hasSize(2); // <4>
        assertThat(results.get("de")).isEqualTo(84.2d);
        assertThat(results.get("fr")).isEqualTo(68.4d);
    }
    // # end::testIncreasePopulationUsingLambdaExpression[]

    // # tag::testIncreasePopulationUsingInvokeForSingleCountry[]
    @Test
    void testIncreasePopulationUsingInvokeForSingleCountry() {
        NamedCache<String, Country> map = getMap("countries"); // <1>

        final Double results = map.invoke("de", entry -> {  // <2>
            Country country = entry.getValue();
            country.setPopulation(country.getPopulation() + 1);
            entry.setValue(country);  // <3>
            return country.getPopulation();
        });

        assertThat(results).isEqualTo(84.2d);
        assertThat(map.get("de").getPopulation()).isEqualTo(84.2d);
    }
    // # end::testIncreasePopulationUsingInvokeForSingleCountry[]

    // # tag::testIncreasePopulationUsingComputeForSingleCountry[]
    @Test
    void testIncreasePopulationUsingComputeForSingleCountry() {
        NamedCache<String, Country> map = getMap("countries"); // <1>

        final Country results = map.compute("de", (key, country) -> { // <2>
            country.setPopulation(country.getPopulation() + 1);  // <3>
            return country;
        });

        assertThat(results.getPopulation()).isEqualTo(84.2d);
        assertThat(map.get("de").getPopulation()).isEqualTo(84.2d);
    }
    // # end::testIncreasePopulationUsingComputeForSingleCountry[]

    // # tag::testIncreasePopulationForAllCountries[]
    @Test
    void testIncreasePopulationForAllCountries() {

        NamedCache<String, Country> map = getMap("countries"); // <1>
        Filter filter = AlwaysFilter.INSTANCE(); // <2>

        final Map<String, Double> results = map.invokeAll(filter, entry -> { // <3>
            Country country = entry.getValue();
            country.setPopulation(country.getPopulation() + 1);
            return country.getPopulation();
        });

        assertThat(results).hasSize(5); // <4>

        assertThat(results.get("ua")).isEqualTo(42.2d);
        assertThat(results.get("co")).isEqualTo(51.4d);
        assertThat(results.get("au")).isEqualTo(27d);
        assertThat(results.get("de")).isEqualTo(84.2d);
        assertThat(results.get("fr")).isEqualTo(68.4d);
    }
    // # end::testIncreasePopulationForAllCountries[]

    // # tag::get-map[]
    <K, V> NamedCache<K, V> getMap(String name) {
        Coherence coherence = Coherence.getInstance(); // <1>
        Session   session   = coherence.getSession(); // <2>
        return session.getCache(name); // <3>
    }
    // # end::get-map[]
}
