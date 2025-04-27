/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package filter;

import com.oracle.coherence.common.collections.NullableSortedMap;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.util.Binary;
import com.tangosol.util.ChainedCollection;
import com.tangosol.util.SubSet;
import com.tangosol.util.comparator.SafeComparator;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.LimitFilter;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.filter.AllFilter;
import com.tangosol.util.filter.BetweenFilter;

import data.Person;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.junit.Assert;
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
        System.setProperty("coherence.distributed.localstorage", "true");
        AbstractFunctionalTest._startup();
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void testRetainAll()
        {
        testSubset(true);
        }

    @Test
    public void testRemoveAll()
        {
        testSubset(false);
        }

    @Test
    public void testNullFirst()
        {
        NamedCache<String, data.repository.Person> cache = CacheFactory.getCache("dist");

        Comparator compName = new SafeComparator(new ReflectionExtractor("getName"), false);

        cache.clear();

        // normal entries
        data.repository.Person person1 = new data.repository.Person("111");
        person1.setName("Aaaa");
        cache.put(person1.getSsn(), person1);
        data.repository.Person person2 = new data.repository.Person("222");
        person2.setName("Bbbb");
        cache.put(person2.getSsn(), person2);

        // Person with null name
        data.repository.Person person = new data.repository.Person("999");
        cache.put(person.getSsn(), person);

        SortedSet<Map.Entry<String, data.repository.Person>> s =
                (SortedSet) cache.entrySet(new LimitFilter(AlwaysFilter.INSTANCE(), 20), compName);

        assertEquals(s.last().getValue().getName(), null);
        }

    @Test
    public void testBetweenFilter()
        {
        // COH23114 test
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

    // ----- helper methods -------------------------------------------------

    /**
     * Test the performance of retainAll() and removeAll() as any deviation
     * can result in query timeouts
     *
     * @param fRetain  Whether retainAll() or removeAll() should be tested
     */
    public void testSubset(boolean fRetain)
        {
        SubSet<Binary> setKeys = new SubSet<>(createSet());
        SubSet<Binary> setKeys2 = new SubSet<>(setKeys);
        NullableSortedMap<Integer, Set<Binary>> mapTail = new NullableSortedMap<>();
        for (int i = 0; i < 43000; i++)
            {
            mapTail.put(i, Collections.singleton(createBinary(i)));
            }

        // new Style
        long start = System.currentTimeMillis();
        List<Set<?>> listGT = new ArrayList<>(mapTail.size());
        for (Object o : mapTail.values())
            {
            Set set = (Set) o;
            listGT.add(ensureSafeSet(set));
            }
        Collection<Object> c = new ChainedCollection<>(listGT.toArray(Set[]::new));

        if (fRetain)
            {
            // test against passed set
            setKeys.retainAll(c);
            // test against "original" set
            setKeys.retainAll(c);
            }
        else
            {
            // test against passed set
            setKeys.removeAll(c);
            // test against "original" set
            setKeys.removeAll(c);
            }

        long newStyle = System.currentTimeMillis() - start;

        // old style:
        start = System.currentTimeMillis();
        NullableSortedMap<Integer, Set<Binary>> mapGE = new NullableSortedMap<>(mapTail);
        Set setGT = new HashSet();
        for (Iterator iterGE = mapGE.values().iterator(); iterGE.hasNext(); )
            {
            Set set = (Set) iterGE.next();
            setGT.addAll(ensureSafeSet(set));
            }
        if (fRetain)
            {
            setKeys2.retainAll(setGT);
            }
        else
            {
            setKeys2.removeAll(setGT);
            }
        long oldStyle = System.currentTimeMillis() - start;
        System.out.println("Time: new style(partitioned index):" + newStyle + " ms, old style(monolithic index):" + oldStyle + " ms");
        Assert.assertEquals(setKeys, setKeys2);
        // new style called twice ~ as good as old style called once
        Assert.assertTrue((newStyle * .25) <= oldStyle);
        }

    private Set<Binary> createSet()
        {
        Set<Binary> s = new HashSet<>();
        for (int i = 0; i < 30000; i++)
            {
            s.add(createBinary(i + 15000));
            }
        return s;
        }

    protected static Set ensureSafeSet(Set set)
        {
        return set == null ? Collections.emptySet() : set;
        }

    private Binary createBinary(int i)
        {
        String s = "randomkey_no_test_gldfkjglkdfgjdflkgjdflkgjlkdfjglkdfjglkdfjglkdf" + i;
        return new Binary(s.getBytes(StandardCharsets.UTF_8));
        }
    }
