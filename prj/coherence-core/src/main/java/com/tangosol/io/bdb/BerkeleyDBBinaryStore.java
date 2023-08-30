/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.bdb;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.OperationStatus;

import com.tangosol.io.AbstractBinaryStore;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;

import java.util.Iterator;
import java.util.NoSuchElementException;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
* An implementation of the BinaryStore interface using Sleepycat Berkeley
* DB Java Edition.
*
* @see <a href="http://www.berkeleydb.com/jedocs/java/index.html">Berkeley DB
* JE JavaDoc</a>
*
* @author mf 2005.09.29
*/
public class BerkeleyDBBinaryStore
       extends AbstractBinaryStore
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new Berkeley DB BinaryStore using the supplied DatabaseFactory.
    *
    * @param sDbName    the name of the table to store the cache's data in,
    *                    <tt>null</tt> indicates a temporary table name.
    * @param dbFactory  the factory to use to create the Database
    *
    * @throws DatabaseException if the Database creation failed
    */
    public BerkeleyDBBinaryStore(String sDbName, DatabaseFactory dbFactory)
            throws DatabaseException
        {
        azzert(dbFactory != null);

        init(sDbName, dbFactory);
        }

    /**
    * Initialize the BinaryStore.
    *
    * @param sDbName    the name of the table to store the cache's data in,
    *                    <tt>null</tt> indicates a temporary table name.
    * @param dbFactory  the factory to use to create the Database
    *
    * @throws DatabaseException if the Database creation failed
    */
    protected void init(String sDbName, DatabaseFactory dbFactory)
            throws DatabaseException
        {
        m_factory = dbFactory;
        m_db      = new DatabaseHolder(sDbName);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Return a human readable description of the BinaryStore.
    *
    * @return a human readable description of the BinaryStore
    */
    public String toString()
        {
        return "BerkeleyDBBinaryStore {" + " DB Holder: " + getDbHolder() +
                " Environment Directory: " +
                m_factory.getEnvHolder().getDirEnv() + '}';
        }

    // ----- BinaryStore interface ------------------------------------------

   /**
   * Return the value associated with the specified key, or null if the key
   * does not have an associated value in the underlying store.
   *
   * @param binKey key whose associated value is to be returned
   *
   * @return the value associated with the specified key, or <tt>null</tt>
   *          if no value is available for that key
   */
    public Binary load(Binary binKey)
        {
        DatabaseEntry  dbeKey   = new DatabaseEntry(binKey.toByteArray());
        DatabaseEntry  dbeValue = new DatabaseEntry();
        DatabaseHolder db       = getDbHolder();

        // COH-27981: temporary fix to ensure single-thread access until
        //            BDB deadlock issues with virtual threads are fixed.
        f_lock.lock();
        try
            {
            OperationStatus status = db.getDb().get(null /*tx*/,
                                                    dbeKey,
                                                    dbeValue,
                                                    null /* default lock mode */);
            if (status == OperationStatus.SUCCESS)
                {
                return new Binary(dbeValue.getData());
                }
            else if (status == OperationStatus.NOTFOUND)
                {
                // no value found
                return null;
                }
            else
                {
                // check if the DB was swapped from beneath us, if so
                // retry the operation, otherwise raise an exception
                throw new IllegalStateException(
                        "Unexpected status result from Berkeley DB load, status="
                                + status);
                }
            }
        catch (DatabaseException e)
            {
            // check if the DB was swapped from beneath us, if so
            // retry the operation, otherwise raise an exception
            throw Base.ensureRuntimeException(e, "Berkeley DB load operation failed");
            }
        finally
            {
            f_lock.unlock();
            }
        }

    /**
    * Store the specified value under the specific key in the underlying store.
    * <p>
    * This method is supports both key/value creation and value update for a
    * specific key.
    *
    * @param binKey   key to store the value under
    * @param binValue value to be stored
    */
    public void store(Binary binKey, Binary binValue)
        {
        DatabaseEntry  dbeKey   = new DatabaseEntry(binKey.toByteArray());
        DatabaseEntry  dbeValue = new DatabaseEntry(binValue.toByteArray());
        DatabaseHolder db       = getDbHolder();

        // COH-27981: temporary fix to ensure single-thread access until
        //            BDB deadlock issues with virtual threads are fixed.
        f_lock.lock();
        try
            {
            OperationStatus status = db.getDb().put(null /*tx*/, dbeKey, dbeValue);

            if (status != OperationStatus.SUCCESS)
                {
                throw new IllegalStateException(
                        "Unexpected Berkeley DB status result while storing an entry, status="
                                + status);
                }
            }
        catch (DatabaseException e)
            {
            throw Base.ensureRuntimeException(e, "Berkeley DB store operation failed.");
            }
        finally
            {
            f_lock.unlock();
            }
        }


    /**
    * Remove the specified key from the underlying store if present.
    *
    * @param binKey key whose mapping is to be removed from the map
    */
    public void erase(Binary binKey)
        {
        DatabaseEntry  dbeKey = new DatabaseEntry(binKey.toByteArray());
        DatabaseHolder db     = getDbHolder();

        // COH-27981: temporary fix to ensure single-thread access until
        //            BDB deadlock issues with virtual threads are fixed.
        f_lock.lock();
        try
            {
            OperationStatus status = db.getDb().delete(null /*tx*/, dbeKey);

            if (status != OperationStatus.SUCCESS &&
                status != OperationStatus.NOTFOUND)
                {
                throw new IllegalStateException(
                        "Unexpected Berkeley DB status result while deleting an entry, status="
                         + status);
                }
            }
        catch (DatabaseException e)
            {
            throw Base.ensureRuntimeException(e, "Berkeley DB delete operation failed.");
            }
        finally
            {
            f_lock.unlock();
            }
        }

    /**
    * Remove all data from the underlying store.
    */
    public void eraseAll()
        {
        if (m_db.isTemporary())
            {
            // temporary databases can be quickly cleared by simply
            // switching to a new database reference
            // the old data will be deleted as part of the holder's
            // finalization
            try
                {
                m_db = new DatabaseHolder(null /*dbName*/);
                // let GC cleanup the old DB, see DatabaseHolder.finalize
                }
            catch (DatabaseException e)
                {
                // failed to create a new DB, thus our logical
                // eraseAll operation has failed, i.e. no data
                // will be cleared
                throw Base.ensureRuntimeException(e,
                        "Failed to allocate a new Berkeley DB as part of the eraseAll operation.");
                }
            }
        else
            {
            // persistent databases cannot be deleted by simply swapping
            // the db reference, instead we must do a deletion of
            // all the keys, preserving the current DB reference

            // COH-27981: temporary fix to ensure single-thread access until
            //            BDB deadlock issues with virtual threads are fixed.
            f_lock.lock();
            try
                {
                DatabaseHolder db     = getDbHolder();
                Cursor         cursor = null;
                try
                    {
                    cursor = db.getDb().openCursor(null /*tx*/,
                              CursorConfig.READ_UNCOMMITTED);

                    OperationStatus status;
                    DatabaseEntry   dbeTmp = new DatabaseEntry();
                    while (true)
                        {
                        status = cursor.getNext(dbeTmp, dbeTmp,
                                null /*default lock mode*/);
                        if (status == OperationStatus.NOTFOUND)
                            {
                            // end of cursor
                            break;
                            }
                        else if (status == OperationStatus.SUCCESS)
                            {
                            status = cursor.delete();
                            if (status != OperationStatus.SUCCESS)
                                {
                                throw new IllegalStateException(
                                        "Unexpected Berkeley DB status result while performing eraseAll, status="
                                                + status);
                                }
                            }
                        else
                            {
                            throw new IllegalStateException(
                                    "Unexpected Berkeley DB status result while performing eraseAll, status="
                                            + status);
                            }
                        }
                    }
                finally
                    {
                    if (cursor != null)
                        {
                        cursor.close();
                        }
                    }
                }
            catch (DatabaseException e)
                {
                throw Base.ensureRuntimeException(e, "Error while performing eraseAll on Berkeley DB.");
                }
            finally
                {
                f_lock.unlock();
                }
            }
        }

    /**
    * Iterate all keys in the underlying store.
    *
    * @return a read-only iterator of the keys in the underlying store
    */
    public Iterator keys()
        {
        try
            {
            final DatabaseHolder db     = getDbHolder();
            final Cursor         cursor;

            // COH-27981: temporary fix to ensure single-thread access until
            //            BDB deadlock issues with virtual threads are fixed.
            f_lock.lock();
            try
                {
                cursor = db.getDb().openCursor(null /*tx*/,
                           CursorConfig.READ_UNCOMMITTED);
                }
            finally
                {
                f_lock.unlock();
                }

            // return an Iterator which is based by a DB cursor
            return new Iterator()
                {
                public boolean hasNext()
                    {
                    return m_binNext != null;
                    }
                public Object next()
                    {
                    Binary binKey = m_binNext;
                    if (binKey == null)
                        {
                        throw new NoSuchElementException();
                        }
                    m_binNext = readNext();
                    return binKey;
                    }
                public void remove()
                    {
                    throw new UnsupportedOperationException();
                    }
                private Binary readNext()
                    {
                    byte[] abKey = null;
                    // COH-27981: temporary fix to ensure single-thread access until
                    //            BDB deadlock issues with virtual threads are fixed.
                    f_lock.lock();
                    try
                        {
                        DatabaseEntry dbeKey   = m_dbeKey;
                        DatabaseEntry dbeValue = m_dbeValue;

                        // avoid reading the value from disk onto heap
                        dbeValue.setPartial(0, 0, true);

                        OperationStatus status = cursor.getNext(dbeKey,
                                                    m_dbeValue,
                                                    null /*default lock mode*/);

                        if (status == OperationStatus.SUCCESS)
                            {
                            abKey = dbeKey.getData();
                            }
                        }
                    catch (DatabaseException e)  {}
                    finally
                        {
                        f_lock.unlock();
                        }

                    // on error, or DB change trigger iteration end
                    if (abKey == null || db != getDbHolder())
                        {
                        invalidate();
                        return null;
                        }
                    return new Binary(abKey);
                    }
                private void invalidate()
                    {
                    // COH-27981: temporary fix to ensure single-thread access until
                    //            BDB deadlock issues with virtual threads are fixed.
                    f_lock.lock();
                    try
                        {
                        cursor.close();
                        }
                    catch (Throwable e) {}
                    finally
                        {
                        f_lock.unlock();
                        }
                    }
                protected void finalize()
                        throws Throwable
                    {
                    invalidate();
                    super.finalize();
                    }
                private DatabaseEntry m_dbeKey   = new DatabaseEntry();
                private DatabaseEntry m_dbeValue = new DatabaseEntry();
                private Binary        m_binNext  = readNext();
                };
            }
        catch (DatabaseException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }


    // ----- methods --------------------------------------------------------

    /**
    * Close the BinaryStore.
    */
    public void close()
        {
        m_db = null;
        // let the Java GC do the cleanup, see DatabaseHolder.finalize
        }


    // ----- accessor methods -----------------------------------------------

    /**
    * Get the DatabaseHolder.
    * To prevent the underlying Database from being deleted during usage, hold
    * onto this handle while using the Database object.
    *
    * @return the DatabaseHolder
    */
    public DatabaseHolder getDbHolder()
        {
        DatabaseHolder db = m_db;
        if (db == null)
            {
            throw new IllegalStateException(
                    "The Berkeley DB BinaryStore has been closed.");
            }
        return db;
        }

    /**
    * Get the DatabaseFactory used to create the underlying Database.
    *
    * @return the DatabaseFactory
    */
    public DatabaseFactory getFactory()
        {
        return m_factory;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The Database handle.
    */
    protected volatile DatabaseHolder m_db;


    /**
    * Factory used to create this Database.
    */
    protected DatabaseFactory m_factory;

    /**
     * The lock to hold during read and write operations.
     */
    protected final Lock f_lock = new ReentrantLock();

    // ----- inner class: DatabaseHolder ------------------------------------

    /**
    * The DatabaseHolder class is used as a wrapper around
    * a Berkeley DB Database object.
    * <p>
    * Database objects cannot be closed if they are in use by other threads.
    * The Java garbage collector and this holder are utilized to delay
    * closing the Database until it is guaranteed to not be in use.
    */
    protected class DatabaseHolder
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a DatabaseHolder, including a Database.
        *
        * @param sDbName  if non <tt>null</tt> specifies the name of a persistent
        *                  database.
        */
        public DatabaseHolder(String sDbName)
                throws DatabaseException
            {
            DatabaseFactory factory = BerkeleyDBBinaryStore.this.getFactory();

            if (sDbName == null)
                {
                sDbName      = factory.getUniqueDbName();
                m_fTemporary = true;
                }
            else
                {
                m_fTemporary = false;
                }

            m_db = factory.createNamedDatabase(sDbName);
            m_sDbName = sDbName; // COH-1176
            }


        // ----- Object methods -----------------------------------------

        /**
        * Return the Holder's human readable description.
        *
        * @return the Holder's human readable description
        */
        public String toString()
            {
            return "DatabaseHolder {" + "DB Name: " + getName() +
                    " Temporary: " + isTemporary() + '}';
            }

        /**
        * Finalize the holder, deleting the database if it is temporary.
        *
        * @throws Throwable if the removal fails
        */
        protected void finalize()
                throws Throwable
            {
            closeDb();
            super.finalize();
            }


        // ----- accessor methods ---------------------------------------

        /**
        * Get the underlying Database handle.
        *
        * @return the Database handle
        */
        public Database getDb()
            {
            return m_db;
            }

        /**
        * Get the name of the underlying Database.
        *
        * @return the Database name
        */
        public String getName()
            {
            return m_sDbName;
            }

        /**
        * Return if the database is temporary.
        *
        * @return true if the database is temporary
        */
        public boolean isTemporary()
            {
            return m_fTemporary;
            }


        // ----- helper methods ------------------------------------------

        /**
        * Close the Database.
        * <p>
        * If this is a temporary, or empty persistent DB, then it will
        * be deleted.
        */
        protected void closeDb()
                throws DatabaseException
            {
            Database db  = m_db;

            // instruct the factory to stop tracking this DB handle
            BerkeleyDBBinaryStore.this.getFactory().forgetDatabase(db);

            boolean fRemove;
            if (isTemporary())
                {
                fRemove = true;
                }
            else
                {
                // persistent store
                // if empty delete it, otherwise flush the data
                DatabaseEntry   dbEntry = new DatabaseEntry();
                Cursor          csr     = db.openCursor(null /*tx*/,
                                            CursorConfig.READ_UNCOMMITTED);
                OperationStatus status  = csr.getNext(dbEntry, dbEntry,
                                            null /*lock mode*/);
                csr.close();
                // if empty, then mark for removal
                fRemove = (status == OperationStatus.NOTFOUND);
                }

            if (fRemove)
                {
                // close DB & delete the from the environment
                Environment env = db.getEnvironment();
                db.close();
                env.removeDatabase(null/*tx*/, m_sDbName);
                }
            else
                {
                // flush the persistent database
                db.close();
                }
            }


        // ----- data members -------------------------------------------

        /**
        * The underlying Database Handle.
        */
        protected Database m_db;

        /**
        * The name of the underlying Database.
        * <p>
        * The name is maintained externally from the Database as calls to
        * Database.getDatabaseName() are costly.
        */
        protected String m_sDbName;

        /**
        * Flag indicating if the database is temporary.
        * <p>
        * Temporary databases are automatically deleted on shutdown or GC.
        */
        protected boolean m_fTemporary;
        }
    }

