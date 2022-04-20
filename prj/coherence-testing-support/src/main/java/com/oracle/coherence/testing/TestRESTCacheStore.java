/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.testing;

import com.tangosol.util.ObservableMap;

import java.lang.Integer;

/**
* {@link CacheStore} implementation for testing {@link ReadWriteBackingMap}
* functionality.
* <p>
* This CacheStore implementation is used for the test cases of Bug21356685.
*
* @author PAR
*/
public class TestRESTCacheStore
          extends TestCacheStore
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public TestRESTCacheStore()
        {
          super();
        }

    /**
    * Create a new TestCacheStore that will use the specified ObservableMap to
    * store items.
    *
    * @param mapStorage  the ObservableMap used for the underlying storage
    */
    public TestRESTCacheStore(ObservableMap mapStorage)
        {
        super(mapStorage);
        }

    // ----- CacheStore interface -------------------------------------------

    /**
    * Return the value associated with the specified key.
    *
    * if the key is the specific Integer value, it returns "testREST".  This 
    * verifies to the caller that this CacheStore has been accessed.
    *
    * @param oKey key whose associated value is to be returned
    *
    * @return the value associated with the specified key, or "testREST" if
    *         no value is available for that key
    */
    public Object load(Object oKey)
        {
        log(isVerboseLoad(), "load(" + oKey + ")");
        logMethodInvocation("load");

        Object loadedObj = super.load(oKey);

        if (  (loadedObj == null) 
           && (oKey instanceof Integer)
           && ((Integer) oKey).intValue() == 2015)
            {
            return "testREST";
            }
        return loadedObj;
        }

    /**
    * Store the specified value under the specific Integer key in the underlying
    * store, after a 2 second delay intended to simulate an inaccessible
    * database. This method is intended to show that the REST client blocks,
    * waiting for the database to become accessible.
    *
    * @param oKey   key to store the value under
    * @param oValue value to be stored
    *
    * @throws UnsupportedOperationException if this implementation or the
    *                                       underlying store is read-only
    */
    public void store(Object oKey, Object oValue)
        {
        log(isVerboseStore(), "store(" + oKey + ", " + oValue + ")");
        logMethodInvocation("store");

        if ((oKey instanceof Integer) && ((Integer)oKey).intValue() == 2015)
            {
            // sleep 2 seconds to simulate inaccessible database.
            sleep(2000);
            }

        super.store(oKey, oValue);
        }
    }
