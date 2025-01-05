/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.extractor;

import com.tangosol.util.Base;
import com.tangosol.util.Extractors;
import com.tangosol.util.ValueExtractor;

import data.collectionExtractor.City;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Unit test of the {@link CollectionExtractor}.
 *
 * @author Gunnar Hillert 2024.09.15
 */
public class CollectionExtractorTest
        extends Base
    {

    @Test(expected = IllegalArgumentException.class)
    public void testMissingExtractor()
        {
        new CollectionExtractor<String, String>(null);
        }

    @Test
    public void testEmptyCollection()
        {
        ValueExtractor<City, Integer> cityPopulationExtractor = Extractors.extract("population");

        CollectionExtractor<City, Integer> collectionExtractor = new CollectionExtractor<>(cityPopulationExtractor);
        Collection<Integer> populations = collectionExtractor.extract(Collections.emptySet());

        assertTrue("Expected populations to be empty, but got " + populations.size(), populations.isEmpty());
        assertTrue("Expected populations to be an instance of List", populations instanceof List<Integer>);
        }

    @Test
    public void testExtractCollection()
        {
        List<City> cities = new ArrayList<>();
        cities.add(new City("New York", 8_258_035));
        cities.add(new City("Los Angeles", 3_820_914));
        cities.add(new City("Chicago", 2_664_452));

        ValueExtractor<City, Integer> cityPopulationExtractor = Extractors.extract("population");

        CollectionExtractor<City, Integer> collectionExtractor = new CollectionExtractor<>(cityPopulationExtractor);
        Collection<Integer> populations = collectionExtractor.extract(cities);

        assertTrue("Expected populations to contain 3 entries, but got " + populations.size(), populations.size() == 3);
        assertThat(populations, hasItems(8_258_035, 3_820_914, 2_664_452));
        }

    @Test
    public void testUseExtractorsFromCollectionWithStringArguments()
        {
        List<City> cities = new ArrayList<>();
        cities.add(new City("New York", 8_258_035));
        cities.add(new City("Los Angeles", 3_820_914));
        cities.add(new City("Chicago", 2_664_452));

        CollectionExtractor<City, Integer> collectionExtractor = Extractors.fromCollection("population");
        Collection<Integer> populations = collectionExtractor.extract(cities);

        assertTrue("Expected populations to contain 3 entries, but got " + populations.size(), populations.size() == 3);
        assertThat(populations, hasItems(8_258_035, 3_820_914, 2_664_452));
        }

    @Test(expected = IllegalArgumentException.class)
    public void testUseExtractorsFromCollectionWithEmptyStringParams()
        {
        Extractors.fromCollection(new String[]{});
        }

    @Test
    public void testUseExtractorsFromCollection()
        {
        List<City> cities = new ArrayList<>();
        cities.add(new City("New York", 8_258_035));
        cities.add(new City("Los Angeles", 3_820_914));
        cities.add(new City("Chicago", 2_664_452));

        CollectionExtractor<City, Integer> collectionExtractor = Extractors.fromCollection(new UniversalExtractor("population"));
        Collection<Integer> populations = collectionExtractor.extract(cities);

        assertTrue("Expected populations to contain 3 entries, but got " + populations.size(), populations.size() == 3);
        assertThat(populations, hasItems(8_258_035, 3_820_914, 2_664_452));
        }

    @Test(expected = IllegalArgumentException.class)
    public void testUseExtractorsFromCollectionWithEmptyParams()
        {
        Extractors.fromCollection(new UniversalExtractor[]{});
        }
    }


