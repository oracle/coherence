/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.extractor;

import org.junit.Test;
import static org.junit.Assert.*;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.Filter;
import com.tangosol.util.ConditionalIndex;
import com.tangosol.util.MapIndex;
import com.tangosol.util.filter.GreaterFilter;

import java.util.Map;
import java.util.HashMap;

import data.Person;

/**
* Unit test of the {@link ConditionalExtractor} implementation.
*
* @author tb 02/10/2010
*/
public class ConditionalExtractorTest
    {
    /**
    * Test createIndex
    */
    @Test
    public void testCreateIndex()
        {
        ValueExtractor extractor = new IdentityExtractor();
        Filter filter            = new GreaterFilter(extractor, 5);
        Map    map               = new HashMap();

        ConditionalExtractor condExtractor = new ConditionalExtractor(filter, extractor, true);

        MapIndex index = condExtractor.createIndex(false, null, map, null);
        assertTrue(index instanceof ConditionalIndex);
        assertEquals(filter, ((ConditionalIndex)index).getFilter());
        assertEquals(extractor, index.getValueExtractor());

        // make sure that the index map has been updated with the created
        // index
        MapIndex index2 = (MapIndex) map.get(extractor);
        assertNotNull(index2);
        assertEquals(index, index2);
        }

    /**
    * Test destroyIndex
    */
    @Test
    public void testDestroyIndex()
        {
        ValueExtractor extractor = new IdentityExtractor();
        Filter filter            = new GreaterFilter(extractor, 5);
        Map    map               = new HashMap();

        ConditionalExtractor condExtractor = new ConditionalExtractor(filter, extractor, true);

        MapIndex index = condExtractor.createIndex(false, null, map, null);

        // make sure that the index map has been updated with the created
        // index
        MapIndex index2 = (MapIndex) map.get(extractor);
        assertNotNull(index2);
        assertEquals(index, index2);

        condExtractor.destroyIndex(map);

        // make sure that the index has been removed from the index map
        assertNull(map.get(extractor));
        }

    /**
    * Test extract
    */
    @Test
    public void testExtract()
        {
        ValueExtractor extractor = new ReflectionExtractor("getId");
        Filter filter            = new GreaterFilter(extractor, 5);

        ConditionalExtractor condExtractor = new ConditionalExtractor(filter, extractor, true);
        
        String sId    = "123456789";
        Person person = new Person(sId);

        try
            {
            condExtractor.extract(person);
            fail("Expected an UnsupportedOperationException.");
            }
        catch (UnsupportedOperationException e)
            {
            // expected
            }
        }
    }
