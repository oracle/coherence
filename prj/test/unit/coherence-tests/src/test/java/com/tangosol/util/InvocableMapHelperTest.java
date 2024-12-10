/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.util.extractor.IdentityExtractor;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.GreaterFilter;
import com.tangosol.util.filter.LimitFilter;

import org.junit.Test;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;


/**
* Test of InvocableMapHepler methods.
*
* @author tb 2009.03.08
*/
public class InvocableMapHelperTest
        extends Base
    {
    /**
    * Test query.
    */
    @Test
    public void testQuery()
        {
        Map map = new HashMap();
        map.put("one",   1);
        map.put("two",   2);
        map.put("three", 3);
        map.put("four",  4);
        map.put("five",  5);

        // simple query for the key set with an always filter
        Set set = InvocableMapHelper.query(map, AlwaysFilter.INSTANCE,
                false, false, null);

        assertTrue(map.keySet().equals(set));

        // simple query for the key set with an greater filter
        set = InvocableMapHelper.query(map,
                new GreaterFilter(new IdentityExtractor(), 3),
                false, false, null);

        assertTrue(set.size() == 2);
        assertTrue(set.contains("four"));
        assertTrue(set.contains("five"));

        // simple query for the entery set with an always filter
        set = InvocableMapHelper.query(map, AlwaysFilter.INSTANCE,
                true, false, null);

        assertTrue(map.entrySet().equals(set));

        // simple query for the entery set with an always filter and sort
        set = InvocableMapHelper.query(map, AlwaysFilter.INSTANCE,
                true, true, null);

        assertTrue(set.size() == 5);
        assertTrue(checkEntrySetValue(set, 1, 0));
        assertTrue(checkEntrySetValue(set, 2, 1));
        assertTrue(checkEntrySetValue(set, 3, 2));
        assertTrue(checkEntrySetValue(set, 4, 3));
        assertTrue(checkEntrySetValue(set, 5, 4));


        // simple query for the entry set with an greater filter
        set = InvocableMapHelper.query(map,
                new GreaterFilter(new IdentityExtractor(), 3),
                true, false, null);

        assertTrue(set.size() == 2);
        assertTrue(checkEntrySetValue(set, 4, -1));
        assertTrue(checkEntrySetValue(set, 5, -1));


        // query for the entery set with a limit filter
        LimitFilter filter = new LimitFilter(AlwaysFilter.INSTANCE, 2);
        // page 1
        set = InvocableMapHelper.query(map, filter,
                true, false, null);

        assertTrue(set.size() == 2);

        // page 2
        filter.nextPage();
        set = InvocableMapHelper.query(map, filter,
                true, false, null);

        assertTrue(set.size() == 2);

        // page 3
        filter.nextPage();
        set = InvocableMapHelper.query(map, filter,
                true, false, null);

        assertTrue(set.size() == 1);


        // query for the entery set with a limit filter with sort
        filter = new LimitFilter(AlwaysFilter.INSTANCE, 2);

        // page 1
        set = InvocableMapHelper.query(map, filter,
                true, true, null);

        assertTrue(set.size() == 2);
        assertTrue(checkEntrySetValue(set, 1, 0));
        assertTrue(checkEntrySetValue(set, 2, 1));

        // page 2
        filter.nextPage();
        set = InvocableMapHelper.query(map, filter,
                true, true, null);

        assertTrue(set.size() == 2);
        assertTrue(checkEntrySetValue(set, 3, 0));
        assertTrue(checkEntrySetValue(set, 4, 1));

        // page 3
        filter.nextPage();
        set = InvocableMapHelper.query(map, filter,
                true, true, null);

        assertTrue(set.size() == 1);
        assertTrue(checkEntrySetValue(set, 5, 0));


        // query for the entery set with a limit filter with sort and comparator
        filter = new LimitFilter(AlwaysFilter.INSTANCE, 2);
        Comparator comparator = new Comparator()
            {
            public int compare(Object o1, Object o2)
                {
                return ((Comparable)o1).compareTo(o2);
                }
            };

        // page 1
        set = InvocableMapHelper.query(map, filter,
                true, true, comparator);

        assertTrue(set.size() == 2);
        assertTrue(checkEntrySetValue(set, 1, 0));
        assertTrue(checkEntrySetValue(set, 2, 1));

        // page 2
        filter.nextPage();
        set = InvocableMapHelper.query(map, filter,
                true, true, comparator);

        assertTrue(set.size() == 2);
        assertTrue(checkEntrySetValue(set, 3, 0));
        assertTrue(checkEntrySetValue(set, 4, 1));

        // page 3
        filter.nextPage();
        set = InvocableMapHelper.query(map, filter,
                true, true, comparator);

        assertTrue(set.size() == 1);
        assertTrue(checkEntrySetValue(set, 5, 0));
        }

    /**
    * Test query with index support.
    */
    @Test
    public void testQueryWithIndex()
        {
        Map map = new HashMap();
        map.put("one",   1);
        map.put("two",   2);
        map.put("three", 3);
        map.put("four",  4);
        map.put("five",  5);

        IdentityExtractor extractor  = new IdentityExtractor();
        Comparator        comparator = new Comparator()
            {
            public int compare(Object o1, Object o2)
                {
                return ((Comparable)o1).compareTo(o2);
                }
            };

        Map            mapIndex = new HashMap();
        SimpleMapIndex index    = new SimpleMapIndex(extractor, true, comparator, null);

        for (Iterator iter = map.entrySet().iterator(); iter.hasNext();)
            {
            Map.Entry entry = (Map.Entry) iter.next();
            index.insert(entry);
            }

        mapIndex.put(extractor, index);

        // simple query for the key set with an greater filter
        Set set = InvocableMapHelper.query(map, mapIndex,
                new GreaterFilter(extractor, 3),
                false, false, null);

        assertTrue(set.size() == 2);
        assertTrue(set.contains("four"));
        assertTrue(set.contains("five"));

        // simple query for the key set with an greater filter
        set = InvocableMapHelper.query(map, mapIndex,
                new GreaterFilter(extractor, 3),
                true, false, null);

        assertTrue(set.size() == 2);
        assertTrue(checkEntrySetValue(set, 4, -1));
        assertTrue(checkEntrySetValue(set, 5, -1));
        }

    private static boolean checkEntrySetValue(Set entrySet,
                                              Object value, int index)
        {
        int i = 0;
        for (Iterator iterator = entrySet.iterator(); iterator.hasNext() &&
                (index == -1 || i <= index); ++i)
            {
            Map.Entry entry = (Map.Entry) iterator.next();
            if (index == -1)
                {
                if (entry.getValue().equals(value))
                    {
                    return true;
                    }
                }
            else if (i==index )
                {
                return entry.getValue().equals(value);
                }
            }
        return false;
        }
    }
