/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package filter;

import common.AbstractFunctionalTest;
import data.Person;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.comparator.SafeComparator;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.filter.AllFilter;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.BetweenFilter;
import com.tangosol.util.filter.LimitFilter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.junit.BeforeClass;
import org.junit.Test;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;

public class MiscTests
    extends AbstractFunctionalTest
    {
    /**
     * Default constructor.
     */
    public MiscTests()
        {
        super();
        }

    // ----- test lifecycle -------------------------------------------------
    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("tangosol.coherence.distributed.localstorage", "true");
        AbstractFunctionalTest._startup();
        }

    // ----- test methods ---------------------------------------------------
    @Test
    public void testNullFirst()
        {
        Comparator compName = new SafeComparator(new ReflectionExtractor("getName"), false);

        NamedCache<String, SimplePerson> cache = CacheFactory.getCache("dist");

        cache.clear();

        // normal entries
        SimplePerson person1 = new SimplePerson("111");
        person1.setName("Aaaa");
        cache.put(person1.getSsn(), person1);
        SimplePerson person2 = new SimplePerson("222");
        person2.setName("Bbbb");
        cache.put(person2.getSsn(), person2);

        // Person with null name
        SimplePerson person = new SimplePerson("999");
        cache.put(person.getSsn(), person);

        SortedSet<Map.Entry<String, SimplePerson>> s =
                (SortedSet) cache.entrySet(new LimitFilter(AlwaysFilter.INSTANCE(), 20), compName);

        assertEquals(s.last().getValue().getName(), null);
        }

    @Test
    public void testBetweenFilter()
        {
        ValueExtractor extrFirst = new ReflectionExtractor("getFirstName");
        ValueExtractor extrLast  = new ReflectionExtractor("getLastName");
        ValueExtractor extrYear  = new ReflectionExtractor("getBirthYear");

        NamedCache<Integer, Person> cache = CacheFactory.getCache("dist");
        cache.addIndex(extrFirst, false, null);
        cache.addIndex(extrLast, true, null);
        cache.addIndex(extrYear, false, null);

        Map<Integer, Person> map = new HashMap();

        for (int i = 0; i < 100; i++)
            {
            Person person = new Person();
            person.setFirstName("FirstName"+i);
            person.setLastName("LastName"+i);
            person.setBirthYear(1900 + i );

            if (i % 2 == 0 )
                {
                person.setId(""+i);
                }
            else
                {
                person.setId(null);
                }

            map.put(i, person);
            cache.put(Integer.valueOf(i), person);
            }

        cache.putAll(map);

        Filter filter1 = new BetweenFilter(extrFirst, "FirstName1", "FirstName1");
        Filter filter2 = new BetweenFilter(extrYear, 1901, 1901);
        Filter filter  = new AllFilter(new Filter[]{filter1, filter2});

        long ldtStop = Base.getLastSafeTimeMillis() + 60000L;

        final Object[] af= {Boolean.valueOf(false), null};
        Runnable updateTask = () ->
            {
            while (!((Boolean) af[0]).booleanValue() && Base.getLastSafeTimeMillis() < ldtStop)
                {
                try
                    {
                    cache.remove(1);
                    cache.put(1, map.get(1));
                    }
                catch (Exception e)
                    {
                    af[0] = Boolean.valueOf(true);
                    af[1] = e;
                    break;
                    }
                }
            };

        Runnable queryTask = () ->
            {
            while (!((Boolean) af[0]).booleanValue() && Base.getLastSafeTimeMillis() < ldtStop)
                {
                try
                    {
                    cache.entrySet(filter);
                    }
                catch (Exception e)
                    {
                    af[0] = Boolean.valueOf(true);
                    af[1] = e;
                    break;
                    }
                }
            };

        Thread t1 = new Thread(updateTask);
        Thread t2 = new Thread(queryTask);

        t1.start();
        t2.start();

        try
            {
            t1.join();
            t2.join();
            if (((Boolean)af[0]).booleanValue())
                {
                fail("Test failed with exception! " + af[1]);
                }
            }
        catch (InterruptedException e)
            {
            fail("Test run interrupted");
            }

        cache.removeIndex(extrFirst);
        cache.removeIndex(extrLast);
        cache.removeIndex(extrYear);
        }
    }
