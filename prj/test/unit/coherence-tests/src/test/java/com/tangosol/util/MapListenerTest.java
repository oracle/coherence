/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.cache.WrapperNamedCache;

import com.tangosol.util.filter.MapEventFilter;
import com.tangosol.util.filter.GreaterFilter;

import com.tangosol.util.listener.SimpleMapListener;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;


/**
* Test of using AbstractMapListener to make simple inner classes.
*
* @author cp  2006.01.18
*/
public class MapListenerTest
        extends Base
    {
    /**
    * Command-line test.
    * <p/>
    * TODO mark - please chop this and move it (or better) to the new test for MapEventFilter
    *
    * @param asArg  command line arguments
    */
    public static void main(String[] asArg)
        {
        NamedCache map = new WrapperNamedCache(new ObservableHashMap(), "test");
        for (int i = 0; i < 10; ++i)
            {
            map.put(i, i);
            }

        // find and keep track of everything over 5
        final Set    setKeys   = new HashSet();
        final Filter filterMap = new GreaterFilter("intValue", 5);
        final Filter filterEvt = new MapEventFilter(filterMap);
        MapListener  listener  = new MultiplexingMapListener()
            {
            protected void onMapEvent(MapEvent evt)
                {
                Object oKey = evt.getKey();
                if (filterMap.evaluate(evt.getNewValue()))
                    {
                    setKeys.add(oKey);
                    }
                else
                    {
                    setKeys.remove(oKey);
                    }
                }
            };
        map.addMapListener(listener, filterEvt, true);
        setKeys.addAll(map.keySet(filterMap));

        out("initial contents (with value=key):=" + setKeys);

        for (int i = 0; i < 10; ++i)
            {
            map.put(i, 10 - i);
            }
        out("changed to value=10-key:" + setKeys);

        map.put(3, 0);
        map.put(7, 0);
        out("after setting 3 and 7 to 0: " + setKeys);

        map.put(3, 50);
        map.put(7, 50);
        out("after setting 3 and 7 to 50: " + setKeys);

        map.put(3, 5);
        map.put(7, 5);
        out("after setting 3 and 7 to 5: " + setKeys);
        }


    // ----- unit tests -----------------------------------------------------

    @Test
    public void testInsertCounter()
        {
        ObservableMap cache = new LocalCache();
        cache.addMapListener(new AbstractMapListener()
            {
            public void entryInserted(MapEvent evt)
                {
                ++m_cInserts;
                }
            });

        m_cInserts = 0;
        for (int i = 0; i < 3; ++i)
            {
            for (int j = 0; j < 3; ++j)
                {
                cache.put(i, "test i=" + i + ", j=" + j);
                }
            }

        assertEquals(3, m_cInserts);
        }

    @Test
    public void testSimpleMapListener()
        {
        ObservableMap cache = new LocalCache();
        cache.addMapListener(new SimpleMapListener<>()
                                     .addInsertHandler((evt) -> m_cInserts++)
                                     .addDeleteHandler((evt) -> m_cInserts--));

        m_cInserts = 0;
        for (int i = 0; i < 3; ++i)
            {
            for (int j = 0; j < 3; ++j)
                {
                cache.put(i, "test i=" + i + ", j=" + j);
                }
            }

        assertEquals(3, m_cInserts);

        for (int i = 0; i < 3; ++i)
            {
            for (int j = 0; j < 3; ++j)
                {
                cache.remove(i);
                }
            }

        assertEquals(0, m_cInserts);
        }

    // ----- helper methods -------------------------------------------------

    /**
    * Helper method
    *
    * @param cache
    */
    public static void testEventSize(ObservableMap cache, boolean fLite)
        {
        cache.addMapListener(new MultiplexingMapListener()
            {
            protected void onMapEvent(MapEvent evt)
                {
                out("event has occurred: " + evt);
                out("(the wire-size of the event would have been "
                    + ExternalizableHelper.toBinary(evt).length()
                    + " bytes.)");
                }
            }, null, fLite);

        // insert a 1KB value
        cache.put("test", new byte[1024]);

        // update with a 2KB value
        cache.put("test", new byte[2048]);

        // remove the 2KB value
        cache.remove("test");
        }


    // ----- data members ---------------------------------------------------

    private int m_cInserts;
    }
