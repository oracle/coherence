/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;

import com.tangosol.util.Base;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;

import java.util.Map;


/**
* A map listener that follows the "seppuku" pattern, invalidating the Map
* entries when MapEvents for another related ObservableMap are delivered.
*
* @see <a href="http://www.theserverside.com/patterns/thread.jsp?thread_id=11280">
*      "Seppuku pattern" by Dimitri Rakitine</a>
*
* @author gg 2002.09.10
*/
public class SeppukuMapListener
        extends Base
        implements MapListener
    {
    /**
    * Construct a SeppukuMapListener for the specified front  map.
    *
    * @param map  the Map object to modify based on events issued to this
    *             Listener from a different ObservableMap object
    */
    public SeppukuMapListener(Map map)
        {
        azzert(map != null);

        m_map = map;
        }


    // ----- accessors ------------------------------------------------------
    
    /**
    * Returns the front Map invalidated by this listener.
    * 
    * @return the front Map invalidated by this listener
    */
    public Map getMap()
        {
        return m_map;
        }


    // ----- MapListener interface ------------------------------------------

    /**
    * Invoked when a map entry has been inserted.
    *
    * @param evt  the MapEvent
    */
    public void entryInserted(MapEvent evt)
        {
        validate(evt.getKey(), evt.getNewValue());
        }

    /**
    * Invoked when a map entry has been updated.
    *
    * @param evt  the MapEvent
    */
    public void entryUpdated(MapEvent evt)
        {
        validate(evt.getKey(), evt.getNewValue());
        }

    /**
    * Invoked when a map entry has been removed.
    *
    * @param evt  the MapEvent
    */
    public void entryDeleted(MapEvent evt)
        {
        Object oKey = evt.getKey();
        Map    map  = getMap();

        synchronized (map)
            {
            map.remove(oKey);
            }
        }
    

    // ----- inheritance support --------------------------------------------

    /**
    * Validate the specified entry and remove it from the Map object affected
    * by this listener if and only if the value is different (implying that
    * the entry has been modified elsewhere).
    *
    * @param oKey    the entry key
    * @param oValue  the "new" entry value; this is potentially different
    *                from the one in the map maintained by this Suppuku
    *                listener
    */
    protected void validate(Object oKey, Object oValue)
        {
        Map map = getMap();
        
        synchronized (map)
            {
            if (map.containsKey(oKey) && 
                    !Base.equalsDeep(map.get(oKey), oValue))
                {
                map.remove(oKey);
                }
            }
        }


    // ----- data fields ----------------------------------------------------

    /**
    * The Map object that this Seppuku listener affects.
    */
    private Map m_map;
    }