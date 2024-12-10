/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.bdb;


import com.tangosol.io.BinaryStoreManager;
import com.tangosol.io.BinaryStore;

import com.tangosol.run.xml.XmlConfigurable;
import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;

import com.sleepycat.je.DatabaseException;

import java.io.File;


/**
* An implementation of the BinaryStoreManager interface using Sleepycat
* Berkeley DB Java Edition.
* <p>
* The usage pattern for BinaryStoreManagers is simply to instantiate them, use
* them to allocate a single BinaryStore instance, and forget about them.
* This implementation maintains knowledge of previously created Berkeley DB
* Environments via a static DatabaseFactoryManager, thus ensuring a minimum
* number of Environments are created, regardless of the life-cycle of the
* BinaryStoreManager.
*
* @see <a href="http://www.berkeleydb.com/jedocs/java/index.html">Berkeley DB
* JE JavaDoc</a>
*
* @author mf 2005.09.29
*/
public class BerkeleyDBBinaryStoreManager
       extends    Base
       implements BinaryStoreManager,
                  XmlConfigurable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    * <p>
    * The temporary Environment directory will be placed under
    * $tmp/coherence/bdb, where $tmp is the system defined temp directory.
    * <p>
    * Configuration is performed via XmlConfigurable interface.
    *
    * @see #setConfig
    */
    public BerkeleyDBBinaryStoreManager()
        {
        this(null /*dir*/, null /*DbName*/);
        }

    /**
    * Construct a Berkeley DB BinaryStoreManager for the specified directory.
    * <p>
    * A temporary directory will be created beneath the specified parent
    * directory, for use by the Berkeley Environment.
    * <p>
    * Additional configuration is performed via XmlConfigurable interface.
    *
    * @param dirParent the parent directory for the Environment
    * @param sDbName   the name of the database to store the cache's data
    *                   within.  This value is only specified when using
    *                   a persistent store.
    *
    * @see #setConfig
    */
    public BerkeleyDBBinaryStoreManager(File dirParent, String sDbName)
        {
        m_dirParent  = (dirParent == null ? null : dirParent.getAbsoluteFile());

        if (sDbName == null || sDbName.length() == 0)
            {
            // temporary store
            m_sDbName    = null;
            m_fTemporary = true;
            }
        else
            {
            // persistent store
            m_sDbName    = sDbName;
            m_fTemporary = false;
            }
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Return a human readable description of the BinaryStoreManager.
    *
    * @return  human readable description of the BinaryStoreManager
    */
    public String toString()
        {
        // db name is intentionally not included, as it only goes into
        // identifying an individual store, not the store manager

        return "BerkeleyDBBinaryStoreManager {" + " Parent Dir: " +
                m_dirParent + " Temporary: " + m_fTemporary + " Config: " +
                m_xmlConfig + '}';
        }

    /**
    * Compares two BerkeleyDBBinaryStoreManagers for equality.
    * <p>
    * Two instances are considered to be equal if their underlying
    * configuration is identical, or if they have reference equality.
    *
    * @param that the object to compare equality against
    *
    * @return true if they are equal, false otherwise
    */
    public boolean equals(Object that)
        {
        if (this == that)
            {
            return true;
            }
        else if (that == null || !(that instanceof BerkeleyDBBinaryStoreManager))
            {
            return false;
            }
        else
            {
            BerkeleyDBBinaryStoreManager bdbThat = (BerkeleyDBBinaryStoreManager) that;

            // db name is intentionally not included, as it only goes into
            // identifying an individual store, not the store manager

            return (m_xmlConfig  == null ? bdbThat.m_xmlConfig  == null
                                         : m_xmlConfig.equals(bdbThat.m_xmlConfig)) &&
                   (m_dirParent  == null ? bdbThat.m_dirParent  == null
                                         : m_dirParent.equals(bdbThat.m_dirParent)) &&
                   (m_fTemporary == bdbThat.m_fTemporary);
            }
        }

    /**
    * Computes the hash code of the BerkeleyDBBinaryStoreManager.
    * <p>
    * The hash code is computed as the sum of the hash codes of the Objects
    * making up the BerkeleyDBBinaryStoreManager's configuration.
    *
    * @return the hash code
    */
    public int hashCode()
        {
        // db name is intentionally not included, as it only goes into
        // identifying an individual store, not the store manager

        return (m_xmlConfig  == null ? 0 : m_xmlConfig.hashCode()) +
               (m_dirParent  == null ? 0 : m_dirParent.hashCode()) +
               Boolean.valueOf(m_fTemporary).hashCode();
        }


    // ----- BinaryStoreManager interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public BinaryStore createBinaryStore()
        {
        try
            {
            return new BerkeleyDBBinaryStore(m_sDbName,
                    s_factoryManager.ensureFactory(this));
            }
        catch (DatabaseException e)
            {
            throw Base.ensureRuntimeException(e, "Failed to create a Berkeley DB Binary Store.");
            }
        }

    /**
    * Destroy a BinaryStore previously created by this manager.
    *
    * @param store a BinaryStore object previously created by this manager
    */
    public void destroyBinaryStore(BinaryStore store)
        {
        if (store != null)
            {
            BerkeleyDBBinaryStore scStore = (BerkeleyDBBinaryStore) store;
            scStore.close();
            }
        }


    // ----- XmlConfigurable interface --------------------------------------

    /**
    * Retrieve the manager's configuration.
    *
    * @return XmlElement containing the configuration
    */
    public XmlElement getConfig()
        {
        return m_xmlConfig;
        }

    /**
    * Specify the manager's configuration.
    * <p>
    * Any configuration setting prefixed with je. will be passed through to
    * Berkeley DB Configuration.
    *
    * @param xmlConfig the new configuration
    *
    * @see <a href = "http://www.sleepycat.com/jedocs/GettingStartedGuide/administration.html#propertyfile">
    * Berkeley DB Configuration</a>
    */
    public void setConfig(XmlElement xmlConfig)
        {
        m_xmlConfig = xmlConfig;
        }


    // ----- accessor methods -----------------------------------------------

    /**
    * Get the DatabaseFactoryManager.
    * <p>
    * This manager is used to find pre-existing DatabaseFactory objects.
    *
    * @return the DatabaseFactoryManager
    */
    public static DatabaseFactoryManager getFactoryManager()
        {
        return s_factoryManager;
        }

    /**
    * Get the configured parent directory.
    * <p>
    * This is the directory in which Berkeley DB Environment sub-directories
    * will be created.
    *
    * @return the parent directory
    */
    public File getParentDirectory()
        {
        return m_dirParent;
        }

    /**
    * Return true if this is a manager for temporary stores.
    *
    * @return true if this is a manager for temporary stores
    */
    public boolean isTemporary()
        {
        return m_fTemporary;
        }


    // ----- static members -------------------------------------------------

    /**
    * Static DatabaseFactoryManager for tracking previously created Factories.
    */
    private static DatabaseFactoryManager s_factoryManager =
            new DatabaseFactoryManager();


    // ----- data members ---------------------------------------------------

    /**
    *  Stored configuration for this Manager.
    */
    protected XmlElement m_xmlConfig;

    /**
    * Parent directory for creating Environments.
    */
    protected File m_dirParent;

    /**
    * Database name, used for persistent stores.
    */
    protected String m_sDbName;

    /**
    * Flag indicating if this store manages temporary data.
    */
    protected boolean m_fTemporary;
    }

