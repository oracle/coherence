/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.bdb;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.ThreadInterruptedException;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.nio.channels.FileLock;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
* Factory for Berkeley DB Environments and Databases.
* <p>
* Temporary Environments will be automatically deleted upon JVM shutdown or
* GC.
*
* @author mf 2005.10.04
*/
public class DatabaseFactory
       extends Base
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a DatabaseFactory for a given Berkeley DB Environment.
    * <p>
    * Berkeley DB Environments are bound to a single directory.  Java File
    * locking is used to ensure that only one JVM uses the
    * Environment.
    *
    * @param bdbManager  the BinaryStoreManager to create Databases for
    *
    * @see <a href = "http://www.sleepycat.com/jedocs/GettingStartedGuide/administration.html#propertyfile">
    * Berkeley DB Configuration</a>
    *
    * @throws DatabaseException if the Berkeley DB Environment could not be
    *                            created
    */
    public DatabaseFactory(BerkeleyDBBinaryStoreManager bdbManager)
            throws DatabaseException
        {
        m_envHolder = instantiateEnvironment(bdbManager);

        // specify that DB creation is allowed
        m_dbConfig.setAllowCreate(true);

        // my default have DB transactional setting mirror the Env transactional
        // setting
        m_dbConfig.setTransactional(m_envHolder.getEnvironment().
                                    getConfig().getTransactional());
        // all other custom Berkeley DB configuration can be done by using je.* settings
        // within the BerkeleyDBBinaryStoreManager init-params
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Output the DatabaseFactory state as a String.
    */
    public String toString()
        {
        return "DatabaseFactory {" + " Holder: " + getEnvHolder() +
                " DB Id Counter: " + getDbIdCounter() + '}';
        }

    /**
    * Finalize for the DatabaseFactory.
    * <p>
    * When the factory is finalized it will force the
    * EnvironmentHolder to be destroyed if it is temporary, and deregister the
    * associated shutdown hook.
    */
    protected void finalize()
            throws Throwable
        {
        // this is needed otherwise the EnvironmentHolder will
        // not get GC'd as the shutdown hook will keep it alive.
        m_envHolder.closeEnvironment(true/*deregister*/);

        super.finalize();
        }


    // ----- methods --------------------------------------------------------

    /**
    * Create a named Database instance.
    *
    * @param sName the name of the database to create
    *
    * @return new a Database instance
    *
    * @throws DatabaseException if the Database creation failed
    */
    public Database createNamedDatabase(String sName)
            throws DatabaseException
        {
        EnvironmentHolder env = m_envHolder;
        Database          db  = env.getEnvironment().
                openDatabase(null/*tx*/, sName, m_dbConfig);

        // add tracking to the EnvironmentHolder
        // this allows a database to be closed prior
        // to environment closure
        env.trackDatabase(db);

        return db;
        }

    /**
    * Get a unique (within the Factory) database name.
    *
    * @return a unique database name
    */
    public synchronized String getUniqueDbName()
        {
        return Long.toString(m_cDbId++);
        }


    // ----- helper methods -------------------------------------------------

    /**
    * Create a EnvironmentHolder within the specified parent directory.
    *
    * @param bdbManager  the BinaryStore to create the environment for
    *
    * @return a new EnvironmentHolder
    *
    * @throws DatabaseException if the Environment could not be created
    */
    protected EnvironmentHolder instantiateEnvironment
            (BerkeleyDBBinaryStoreManager bdbManager)
            throws DatabaseException
        {
        return new EnvironmentHolder(bdbManager);
        }


    /**
    * Remove a database handle from the cleanup list.
    *
    * @param db the Database to remove from list
    */
    public void forgetDatabase(Database db)
        {
        m_envHolder.forgetDatabase(db);
        }


    // ----- accessor methods -----------------------------------------------

    /**
    * Get the EnvironmentHolder.
    *
    * @return the EnvironmentHolder
    */
    public EnvironmentHolder getEnvHolder()
        {
        return m_envHolder;
        }

    /**
    * Get the DatabaseConfiguration used to create Databases.
    *
    * @return the DatabaseConfiguration
    */
    public DatabaseConfig getDbConfig()
        {
        return m_dbConfig;
        }

    /**
    * Get the DB ID Counter value.
    *
    * @return the DB ID Counter value
    */
    public synchronized long getDbIdCounter()
        {
        return m_cDbId;
        }


    // ----- data members ---------------------------------------------------

    /**
    * Holder for Environment used to create Databases.
    */
    protected EnvironmentHolder m_envHolder;

    /**
    * Configuration to use when creating new Databases.
    */
    protected DatabaseConfig m_dbConfig = new DatabaseConfig();

    /**
    * Counter used to generate unique Database names for this Environment.
    */
    protected long m_cDbId;


    // ----- inner class: EnvironmentHolder ------------------------

    /**
    * Holder for Berkeley DB Environment and its Databases.
    * <p>
    * The Environment and Databases will be automatically flushed upon JVM
    * shutdown or GC of the associated DatabaseFactory.  For temporary
    * Environments, the data will also be deleted.  These operations are
    * accomplished by registering this class as a JVM shutdown hook.
    * <p>
    * This logic is maintained externally from DatabaseFactory to allow
    * for the Factory to be GC'd.  If the Factory acted as the shutdown
    * hook then it would never be GC'd as the Runtime would always hold
    * a reference to it via the registered shutdown hook.
    *
    * @author mf 2005.10.04
    */
    protected static class EnvironmentHolder
            implements Runnable
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a EnvironmentHolder for a given Berkeley DB
        * Environment.
        * <p>
        * Berkeley DB Environments are bound to a single directory.  Java File
        * locking is used to ensure that only one JVM uses the Environment.
        * The new environment will be created in a unique sub directory of the
        * specified parent directory.
        *
        * @param bdbManager  the BinaryStoreManager to create an environment for
        *
        * @throws DatabaseException if the Berkeley DB Environment could not
        *                            be created
        */
        public EnvironmentHolder(BerkeleyDBBinaryStoreManager bdbManager)
                throws DatabaseException
            {
            configure(bdbManager);

            if (isTemporary())
                {
                createTemporaryEnvironment();
                }
            else
                {
                createPersistentEnvironment();
                }

            // register shutdown hook
            m_threadShutdownHook = makeThread(null, this, "BdbShutdownHook");
            Runtime.getRuntime().addShutdownHook(m_threadShutdownHook);
            }


        // ----- Object methods -----------------------------------------

        /**
        * Return a human readable description of the EnvironmentHolder.
        *
        * @return a human readable description of the
        *          EnvironmentHolder
        */
        public String toString()
            {
            return "EnvironmentHolder {" + " Directory: " + getDirEnv() +
                    " Temporary: " + isTemporary() + " Config: " +
                    getXmlConfig() + '}';
            }

        // ----- methods ------------------------------------------------

        /**
        * Remove a database handle from the cleanup list.
        * <p>
        * Databases which are closed externally may be removed from tracking.
        *
        * @param db the Database to remove from list
        */
        public void forgetDatabase(Database db)
            {
            Collection colDbs = m_colRegisteredDbs;
            synchronized (colDbs)
                {
                colDbs.remove(db);
                }
            }

        /**
        * Shutdown hook runnable method.
        * <p>
        * This is responsible for cleaning up the databases and associated
        * directories when the JVM exits.
        */
        public void run()
            {
            try
                {
                closeEnvironment(false/*deregister*/);
                }
            catch (Throwable e)
                {
                // COH-2926: can't do anything here as the JVM is exiting
                Base.log("Ignoring exception during shutdown: " + e);
                }
            }

        /**
        * Close an Environment.
        * <p>
        * This includes closing the environment and databases, and if
        * temporary deleting the associated files.
        *
        * @param fDeregister indicates if the shutdown hook should be
        *                     deregistered
        */
        public void closeEnvironment(boolean fDeregister)
            {
            if (fDeregister)
                {
                Runtime.getRuntime().removeShutdownHook(m_threadShutdownHook);
                }

            boolean fTemporary = isTemporary();

            // close DBs and Environment
            Environment env = m_env;
            if (env != null)
                {
                // iterate over each open Database in the Environment
                // and close it.  All Database handles for the Environment
                // must be closed in order to close the Environment.
                Collection colDbs = m_colRegisteredDbs;
                synchronized (colDbs)
                    {
                    if (fTemporary)
                        {
                        // simply close all DBs
                        for (Iterator iter = colDbs.iterator(); iter.hasNext(); )
                            {
                            try
                                {
                                ((Database) iter.next()).close();
                                }
                            catch (DatabaseException e)
                                {
                                // eat and continue with next db
                                }
                            }
                        }
                    else
                        {
                        // for persistent environments, remove empty DBs
                        // flush non-empty DBs
                        DatabaseEntry dbEntry = new DatabaseEntry();

                        for (Iterator iter = colDbs.iterator(); iter.hasNext(); )
                            {
                            try
                                {
                                Database        db     = (Database) iter.next();
                                Cursor          csr    = db.openCursor(null /*tx*/,
                                                            CursorConfig.READ_UNCOMMITTED);
                                OperationStatus status = csr.getNext(dbEntry, dbEntry,
                                                            null /*lock mode*/);

                                csr.close();

                                if (status == OperationStatus.NOTFOUND)
                                    {
                                    // empty persistent, delete it from env
                                    String sDbName = db.getDatabaseName();
                                    db.close();
                                    env.removeDatabase(null /*tx*/, sDbName);
                                    }
                                else
                                    {
                                    // persistent and non-empty, flush data
                                    db.close();
                                    }
                                }
                            catch (DatabaseException e)
                                {
                                // eat and continue with next db
                                }
                            }
                        }
                    }

                try
                    {
                    // close the Environment
                    env.close();
                    }
                catch (DatabaseException e)
                    {
                    // can't do anything here as the JVM is exiting
                    }
                }

            // release locks
            DirectoryLock dirLock = m_dirLock;
            if (dirLock != null)
                {
                // release ownership of the directory
                try
                    {
                    dirLock.tryUnlock();
                    }
                catch (IOException e)
                    {
                    Base.log("Error releasing lock on Berkeley DB Environment. " + e);
                    // can't do anything here as the JVM is exiting
                    }
                }

            // delete temporary files
            File dirEnv = m_dirEnv;
            if (dirEnv != null && fTemporary)
                {
                // delete dirEnv, and it's children
                File[]  aFile          = dirEnv.listFiles();
                boolean fErrorReported = false;

                for (int i = 0, c = aFile.length; i < c; i++)
                    {
                    File file = aFile[i];
                    if (file != null)
                        {
                        if (!file.delete() && !fErrorReported)
                            {
                            fErrorReported = true;
                            Base.log("Error deleting contents of Berkeley DB Environment directory "
                                    + dirEnv);
                            }
                        // after first failure don't log similar errors
                        }
                    }

                // delete the parent directory
                if (!dirEnv.delete() && !fErrorReported)
                    {
                    Base.log("Error deleting Berkeley DB Environment directory "
                            + dirEnv);
                    }
                }
            }


        /**
        * Add a database handle to the tracking list.
        * <p>
        * Databases for the environment must be tracked so that they
        * may be closed prior to closing the environment.
        *
        * @param db the Database to add to the list
        */
        public void trackDatabase(Database db)
            {
            Collection colDbs = m_colRegisteredDbs;
            synchronized (colDbs)
                {
                colDbs.add(db);
                }
            }


        // ----- helper methods -----------------------------------------

        /**
        * Configure the new Environment.
        *
        * @param bdbManager  the BinaryStoreManager to create an environment for
        */
        protected void configure(BerkeleyDBBinaryStoreManager bdbManager)
            {
            File       dirParent  = bdbManager.getParentDirectory();
            XmlElement xmlConfig  = bdbManager.getConfig();

            m_fTemporary = bdbManager.isTemporary();

            if (dirParent == null)
                {
                if (isTemporary())
                    {
                    // default directory for temporary stores is
                    // $TEMP/coherence/bdb, where $TEMP is the system
                    // temporary directory
                    // if the system temporary directory cannot be determined
                    // then use ./coherence/bdb
                    // see computeTmpDir for resolution of $tmp
                    dirParent = new File(computeTmpDir(), SUB_DIR_NAME);
                    }
                else
                    {
                    // default directory for persistent stores is
                    // ./coherence/bdb
                    dirParent = new File((File) null, SUB_DIR_NAME);
                    }
                }

            if (dirParent.isFile())
                {
                throw new IllegalArgumentException(
                        "The specified parent directory for Berkeley DB \""
                                + dirParent + "\" is a file.");
                }

            // directory will be auto-created later if needed
            m_dirParent = dirParent;

            EnvironmentConfig envConfig = m_envConfig;
            m_xmlConfig = xmlConfig;

            // set some reasonable defaults for Environment's config
            // this may be overridden by custom settings from XML
            // below
            envConfig.setAllowCreate(true);
            // minimize Berkeley DB in-memory data caching
            // there is no switch to completely turn it off
            envConfig.setCachePercent(1);

            if (!isTemporary())
                {
                // auto recovery of persistent stores requires transactions enabled
                envConfig.setTransactional(true);
                }

            // extract configuration from XML
            // xmlConfig will be null if no init-params was specified
            if (xmlConfig != null)
                {
                 // Setup BDB configuration
                // extract all je.* elements and save them off for use
                // in configuring Environments
                List listElement = xmlConfig.getElementList();
                if (!listElement.isEmpty())
                    {
                    // iterate over each setting and check for je.*
                    for (Iterator iter = listElement.iterator(); iter.hasNext(); )
                        {
                        XmlElement xmlParam = (XmlElement) iter.next();

                        // extract the setting's key
                        String sKey = xmlParam.getName();
                        if (sKey.startsWith(JE_PROPERTY_PREFIX) || sKey.startsWith(SLEEPYCAT_JE_PROPERTY_PREFIX))
                            {
                            // Berkeley DB setting found
                            envConfig.setConfigParam(sKey, xmlParam.getString());
                            }
                        }
                    }
                }
            }

        /**
        * compute the system's temp directory.
        *
        * @return the system temp directory, or <code>null</code> if it
        *          could not be determined
        */
        protected File computeTmpDir()
            {
            // This is done via File.createTemporaryFile() and then looking up
            // the temporary files parent directory.  This is preferred over
            // utilization of the System property java.io.tmpdir as it avoids
            // direct usage of a System property, as well as any SecurityManager
            // issues with the property retrieval.
            try
                {
                File fileTmp = File.createTempFile(TEMP_FILE_NAME_PREFIX,
                        null /*suffix*/);
                fileTmp.delete();
                return fileTmp.getParentFile();
                }
            catch (IOException e)
                {
                return null;
                }
            }

        /**
        * Create a temporary Environment.
        *
        * @throws DatabaseException if environment creation fails
        */
        protected void createTemporaryEnvironment()
                throws DatabaseException
            {
            File dirEnv = null;
            try
                {
                File          dirParent = m_dirParent;
                DirectoryLock dirLock;
                do
                    {
                    do
                        {
                        // setup a unique sub-dir within the configured parent
                        dirEnv = new File(dirParent, generateEnvironmentName());

                        // loop if the "unique" directory already exists
                        }
                    while (dirEnv.exists());

                    // make the env dir and any missing parents
                    if (!dirEnv.mkdirs())
                        {
                        throw new IllegalStateException(
                                "Unable to create TemporaryEnvironment directory "
                                        + dirEnv);
                        }

                    dirLock = new DirectoryLock(dirEnv,
                            "Locked Coherence temporary Berkeley DB directory, will be auto-deleted.");

                    // loop if we cannot lock the directory
                    }
                while (!dirLock.tryLock());

                // we've created and locked a temp directory

                m_env     = new Environment(dirEnv, m_envConfig);
                m_dirEnv  = dirEnv;
                m_dirLock = dirLock;
                }
            catch (IOException e)
                {
                throw Base.ensureRuntimeException(e, "Error locking directory " + dirEnv);
                }
            }


       /**
        * Create a persistent Environment.
        *
        * @throws DatabaseException if environment creation fails
        */
        protected void createPersistentEnvironment()
                throws DatabaseException
            {
            File dirEnv = m_dirEnv = m_dirParent;

            // make the env dir and any missing parents
            if (!dirEnv.exists() && !dirEnv.mkdirs())
                {
                throw new IllegalStateException(
                        "Unable to create Environment directory "
                                + dirEnv);
                }

            DirectoryLock dirLock = new DirectoryLock(dirEnv,
                    "Locked Coherence persistent Berkeley DB directory.");

            try
                {
                if (dirLock.tryLock())
                    {
                    m_env     = new Environment(dirEnv, m_envConfig);
                    m_dirEnv  = dirEnv;
                    m_dirLock = dirLock;
                    }
                else
                    {
                    throw new UnsupportedOperationException("Unable to open environment " + dirEnv + " already locked.");
                    }
                }
            catch (IOException e)
                {
                throw Base.ensureRuntimeException(e, "Error locking directory " + dirEnv);
                }
            }

        /**
        * Generate a potentially unique Environment name.
        *
        * @return an environment name
        */
        protected String generateEnvironmentName()
            {
            return ENVIRONMENT_NAME_PREFIX + getRandom().nextInt();
            }


        // ----- accessor methods ---------------------------------------

        /**
        * Get underlying Berkeley DB Environment.
        *
        * @return the Berkeley DB Environment held by this
        *         EnvironmentHolder
        */
        public Environment getEnvironment()
                throws DatabaseException
            {
            try
                {
                // check if environment is valid
                m_env.getDatabaseNames();
                }
            catch (ThreadInterruptedException e)
                {
                closeEnvironment(false/*deregister*/);

                if (isTemporary())
                    {
                    createTemporaryEnvironment();
                    }
                else
                    {
                    createPersistentEnvironment();
                    }
                }

            return m_env;
            }

        /**
        * Get the XML Configuration.
        *
        * @return the XML Configuration for this Environments.
        */
        public XmlElement getXmlConfig()
            {
            return m_xmlConfig;
            }

        /**
        * Get the Parent Directory.
        *
        * @return the Environment's parent directory
        */
        public File getDirParent()
            {
            return m_dirParent;
            }

        /**
        * Get the Environment Directory.
        *
        * @return the Environment's directory
        */
        public File getDirEnv()
            {
            return m_dirEnv;
            }

        /**
        * Get the Environment Configuration.
        *
        * @return the Environment's configuration
        */
        public EnvironmentConfig getEnvConfig()
            {
            return m_envConfig;
            }

        /**
        * Get the registered Databases.
        *
        * @return a collection of open Databases from this Environment
        */
        public Collection getRegisteredDbs()
            {
            return m_colRegisteredDbs;
            }

        /**
        * Get the DirectoryLock held on a temporary Environment.
        *
        * @return the DirectoryLock held on the temporary Environment
        */
        public DirectoryLock getDirLock()
            {
            return m_dirLock;
            }

        /**
        * Return true if this is a temporary environment.
        *
        * @return true if this is a temporary environment
        */
        public boolean isTemporary()
            {
            return m_fTemporary;
            }

        // ----- constants ----------------------------------------------

        /**
        * Default directory name for berkeley db environments.
        */
        public static final String SUB_DIR_NAME =
                "coherence" + File.separator + "bdb";

        /**
        * Prefix for temporary environment names.
        */
        public static final String ENVIRONMENT_NAME_PREFIX = "bdbtemp";

        /**
        * Prefix for temporary file names.
        */
        public static final String TEMP_FILE_NAME_PREFIX = "cohbdb";

        /**
        * Prefix for all Berkeley DB JE configuration settings.
        */
        public static final String JE_PROPERTY_PREFIX = "je.";

        /**
         * Another prefix for Berkeley DB JE configuration settings.
         */
        public static final String SLEEPYCAT_JE_PROPERTY_PREFIX = "com.sleepycat.je.";

        // ----- data members -------------------------------------------

        /**
        * Configuration.
        */
        protected XmlElement m_xmlConfig;

        /**
        * Configuration setting for parent directory.
        */
        protected File m_dirParent;

        /**
        * Environment directory.
        */
        protected File m_dirEnv;

        /**
        * Berkeley DB Environment Configuration.
        */
        protected EnvironmentConfig m_envConfig = new EnvironmentConfig();

        /**
        * Berkeley DB Environment for managing Databases.
        */
        protected Environment m_env;

        /**
        * Databases to close prior to deleting the Environment.
        */
        protected Collection m_colRegisteredDbs = new LinkedList();

        /**
        * Lock held on the directory associated with the Environment.
        */
        protected DirectoryLock m_dirLock;

        /**
        * Flag indicating if this is a temporary environment.
        */
        protected boolean m_fTemporary;

        /**
        * Shutdown hook for closing environment.
        */
        protected Thread m_threadShutdownHook;

        // ----- inner class: DirectoryLock -------------------------

        /**
        * Directory based lock.
        * <p>
        * A single instance of the DirectoryLock is not intended for inter
        * thread locking.  Multiple instance referring to the same File should
        * be used instead.
        */
        protected static class DirectoryLock
            {
            /**
            * Create a DirectoryLock which can be used to try to lock a
            * directory.  The object is created in an unlocked state.
            *
            * @param dir        the directory to lock
            * @param sLockText  the text to include in the lock file
            */
            public DirectoryLock(File dir, String sLockText)
                {
                m_dir       = dir;
                m_sLockText = sLockText;
                }

            /**
            * Try to lock the directory.
            *
            * @return true if a lock is obtained, false if it is locked by
            *               another DirectoryLock
            *
            * @throws IOException if an IO error occurs while creating the
            *                      lock file
            */
            public boolean tryLock()
                    throws IOException
                {
                // create a file to lock the directory
                File             fileLocked    = new File(m_dir, LOCK_FILE_NAME);
                FileOutputStream fstreamLocked = new FileOutputStream(fileLocked);
                FileLock         lockDir;

                try
                    {
                    lockDir = fstreamLocked.getChannel().tryLock();
                    }
                catch (IOException e)
                    {
                    fstreamLocked.close();
                    throw e;
                    }

                if (lockDir == null)
                    {
                    // unable to obtain lock
                    fstreamLocked.close();
                    return false;
                    }

                // lock obtained, output lock text

                // include lock text
                if (m_sLockText != null)
                    {
                    PrintStream pstreamLocked = new PrintStream(fstreamLocked);

                    pstreamLocked.println(m_sLockText);
                    pstreamLocked.flush();
                    }

                // hold onto this stream to keep the channel open
                // and lock valid
                m_fstreamLocked = fstreamLocked;
                m_lockDir       = lockDir;
                return true;
                }


            /**
            * Try to unlock the directory.
            *
            * @return true if a lock is released, false if the directory was
            *               not locked by this DirectoryLock
            *
            * @throws IOException if an IO error occurs while deleting the
            *                      lock file, the lock may be left in an
            *                      unlocked state
            */
            public boolean tryUnlock()
                    throws IOException
                {
                if (m_lockDir == null)
                    {
                    return false;
                    }

                // release the lock
                m_lockDir.release();
                m_lockDir = null;

                // close the stream as well
                // we could not do this earlier as it would
                // release the lock automatically
                m_fstreamLocked.close();

                // delete the lock file
                File fileLocked = new File(m_dir, LOCK_FILE_NAME);
                fileLocked.delete();

                return true;
                }

            /**
            * Automatically release the lock on finalization.
            * <p>
            * The OS will ensure that the lock is released regardless
            * but this allows the lock file to be deleted as well.
            *
            * @throws Throwable  if an error occurs
            */
            protected void finalize()
                    throws Throwable
                {
                tryUnlock();
                super.finalize();
                }


            // ----- constants --------------------------------------

            /**
            * Name of warning file.
            */
            public static final String LOCK_FILE_NAME = "coherence.lck";


            // ----- data members -----------------------------------

            /**
            * Directory to lock.
            */
            protected File m_dir;

            /**
            * The FileStream which the actual lock is held on.
            */
            protected FileOutputStream m_fstreamLocked;

            /**
            * The actual FileLock.
            */
            protected FileLock m_lockDir;

            /**
            * Text to include in the lock file.
            */
            protected String m_sLockText;
            }
        }
    }

