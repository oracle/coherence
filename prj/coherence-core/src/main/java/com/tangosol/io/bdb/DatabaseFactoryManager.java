/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.bdb;

import com.tangosol.util.Base;

import com.sleepycat.je.DatabaseException;

import java.util.HashMap;
import java.util.Map;


/**
* A manager for Berkeley DB Database factories.
* <p>
* The factory manager is used to allocate and find DatabaseFactory objects.
*
* @author mf 2005.10.05
*/
public class DatabaseFactoryManager
       extends Base
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new DatabaseFactoryManager.
    */
    public DatabaseFactoryManager()
        {
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Return a human readable description of the DatabaseFactoryManager.
    *
    * @return a human readable description of the DatabaseFactoryManager
    */
    public String toString()
        {
        return "DatabaseFactoryManager {" + " Entries: " +
                m_mapFactories.size() + '}';
        }

    // ----- methods --------------------------------------------------------

    /**
    * Find, or if needed create a DatabaseFactory for the specified
    * manager.  If a DatabaseFactory has already been created for the
    * specified manager then it will be returned, otherwise a new
    * instance will be created.
    *
    * @param bdbMgr the manager to find the store for
    *
    * @return DatabaseFactory  an instance of a DatabaseFactory
    *
    * @throws DatabaseException if the DatabaseFactory cannot be created
    */
    public DatabaseFactory ensureFactory(BerkeleyDBBinaryStoreManager bdbMgr)
            throws DatabaseException
        {
        // check if we already have an Environment for this manager
        // note manager's are temporary objects, but two manager's with
        // the same configuration are considered equal and will use the same
        // factory to produce databases.
        Map mapFactories = m_mapFactories;
        synchronized(mapFactories)
            {
            DatabaseFactory factory = (DatabaseFactory) mapFactories.get(bdbMgr);
            if (factory == null)
                {
                factory = new DatabaseFactory(bdbMgr);
                mapFactories.put(bdbMgr, factory);
                }
            return factory;
            }
        }


    // ----- accessor methods -----------------------------------------------

    /**
    * Get the Map of Database factories.
    *
    * @return the Map of Database factories
    */
    public Map getMapFactories()
        {
        return m_mapFactories;
        }


    // ----- data members ---------------------------------------------------

    /**
    * Map of DatabaseFactory objects keyed by XmlElement configuration.
    */
    protected Map m_mapFactories = new HashMap();
    }

