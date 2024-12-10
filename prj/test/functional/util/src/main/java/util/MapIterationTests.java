/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package util;


import com.oracle.coherence.common.base.Blocking;

import com.tangosol.util.Base;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.*;


/**
* Unit test for various concurrent map implementations.
*
* @author dr
*/
@RunWith(Parameterized.class)
public class MapIterationTests
        extends Base
    {
    @Parameterized.Parameters(name = "implClass={0}")
    public static Collection<String[]> parameters()
        {
        return Arrays.asList(new String[][]
            {
            {"com.tangosol.util.SafeHashMap"},
            {"com.tangosol.util.SafeSortedMap"},
            {"com.tangosol.util.SegmentedHashMap"}
            });
        }

    /**
     * Test's constructor.
     *
     * @param sImplClass the implementation class name
     */
    public MapIterationTests(String sImplClass)
      {
      m_sImplClass = sImplClass;
      }
    private String m_sImplClass;

    /**
    * Test the concurrent use of Map iterators.
    */
    @Test
    public void testIterator()
        {
        final int  COUNT_UPDATERS  = 5;
        final int  COUNT_ITERATORS = 2;
        final long TEST_DURATION   = 20000l;

        Map map;
        try
            {
            map = (Map) Class.forName(m_sImplClass).newInstance();
            }
        catch (Throwable e)
            {
            throw new RuntimeException(e);
            }

        System.out.println(map.getClass().getSimpleName() + "Test: will run for " +
            (TEST_DURATION/1000) + " seconds");

        Thread[] aThreadUpdater  = new Thread[COUNT_UPDATERS];
        Thread[] aThreadIterator = new Thread[COUNT_ITERATORS];

        long ldtStop = getSafeTimeMillis() + TEST_DURATION;

        Runnable taskUpdate = () -> {
          Random random = new Random();

          while (getSafeTimeMillis() < ldtStop)
              {
              Object  oKey = random.nextInt(100000);
              boolean fPut = random.nextBoolean();

              if (fPut)
                  {
                  map.put(oKey, oKey);
                  }
              else
                  {
                  map.remove(oKey);
                  }
              }
          };

        Runnable taskIterate = () -> {
          while (getSafeTimeMillis() < ldtStop)
              {
              try
                  {
                  Set setKeys = new HashSet();
                  for (Iterator iter = map.entrySet().iterator(); iter.hasNext();)
                     {
                      Map.Entry entry = (Map.Entry) iter.next();
                      assertFalse(setKeys.contains(entry.getKey()));
                      setKeys.add(entry.getKey());
                      }
                  }
              catch (ConcurrentModificationException cme)
                  {
                  err(cme);
                  }
              }
          };

        for (int i = 0; i < COUNT_UPDATERS; i++)
            {
            aThreadUpdater[i] = new Thread(taskUpdate);
            aThreadUpdater[i].setDaemon(true);
            aThreadUpdater[i].start();
            }

        for (int i = 0; i < COUNT_ITERATORS; i++)
            {
            aThreadIterator[i] = new Thread(taskIterate);
            aThreadIterator[i].setDaemon(true);
            aThreadIterator[i].start();
            }

        try
            {
            Blocking.sleep(TEST_DURATION);

            // the time is up; everyone has to be out shortly

            for (int i = 0; i < COUNT_UPDATERS; i++)
                {
                aThreadUpdater[i].join(1000l);
                assertFalse(aThreadUpdater[i].isAlive());
                }

            for (int i = 0; i < COUNT_ITERATORS; i++)
                {
                aThreadIterator[i].join(1000l);
                assertFalse(aThreadIterator[i].isAlive());
                }

            System.out.println(
                map.getClass().getSimpleName() + "Test: competed successfully");
            }
        catch (InterruptedException e)
            {
            throw ensureRuntimeException(e);
            }
        }
    }
