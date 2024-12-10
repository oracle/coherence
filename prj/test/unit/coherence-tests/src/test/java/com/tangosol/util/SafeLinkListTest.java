/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;


/**
* Unit test for the {@link com.tangosol.util.SafeLinkedList} class.
*/
public class SafeLinkListTest
        extends Base
    {
    // ----- test methods ---------------------------------------------------

    /**
    * Test random operations.
    */
    @Test
    public void testRandom()
        {
        final List listTest = new SafeLinkedList();
        final List listCtrl = new ArrayList();

        Random random = getRandom();

        for (int i = 0; i < ITER; i++)
            {
            Object o = Integer.valueOf(random.nextInt(SIZE));
            switch (random.nextInt(5))
                {
                case 0:
                    listTest.add(o);
                    listCtrl.add(o);
                    break;

                case 1:
                    listTest.remove(o);
                    listCtrl.remove(o);
                    break;

                case 2:
                    {
                    int iPos = random.nextInt(listTest.size() + 1);
                    listTest.add(iPos, o);
                    listCtrl.add(iPos, o);
                    }
                    break;

                case 3:
                    if (!listTest.isEmpty())
                        {
                        int iPos = random.nextInt(listTest.size());
                        listTest.remove(iPos);
                        listCtrl.remove(iPos);
                        }
                    break;

                case 4:
                    if (!listTest.isEmpty())
                        {
                        int iPos = random.nextInt(listTest.size());
                        Object oTest = listTest.get(iPos);
                        Object oCtrl = listCtrl.get(iPos);
                        assertTrue("not equal at position " + iPos,
                                equals(oTest, oCtrl));
                        }
                    break;
                }
            }
        assertTrue("not equal", listCtrl.equals(listTest));
        }

    /**
    * Test concurrent operations.
    */
    @Test
    public void testConcurrent()
        {
        final int  THREADS  = 4;
        final List listTest = new SafeLinkedList();

        Runnable task = new Runnable()
            {
            public void run()
                {
                Random random = getRandom();

                for (int i = 0; i < ITER/THREADS; i++)
                    {
                    Object o = Integer.valueOf(random.nextInt(SIZE));

                    switch (random.nextInt(4))
                        {
                        case 0:
                            listTest.add(o);
                            break;

                        case 1:
                            listTest.remove(o);
                            break;

                        case 2:
                            listTest.add(0, o);
                            break;

                        case 3:
                            synchronized (listTest)
                                {
                                if (!listTest.isEmpty())
                                    {
                                    listTest.remove(0);
                                    }
                                }
                            break;
                        }
                    }
                }
            };

        try
            {
            Thread[] aThread = new Thread[THREADS];

            for (int i = 0; i < THREADS; i++)
                {
                aThread[i] = new Thread(task);
                aThread[i].setDaemon(true);
                aThread[i].start();
                }

            for (int i = 0; i < THREADS; i++)
                {
                aThread[i].join();
                }
            }
        catch (Throwable e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
    * Test a well known pre-Coherence 3.6 failure scenario.
    */
    @Test
    public void testCOH2750()
        {
        List list = new SafeLinkedList();
        list.add("a");
        list.add("b");
        list.add("c");
        list.remove("c");
        list.add("c");
        list.get(1);
        }


    // ----- constants ------------------------------------------------------

    final static int SIZE = 1000;
    final static int ITER = 100000;
    }