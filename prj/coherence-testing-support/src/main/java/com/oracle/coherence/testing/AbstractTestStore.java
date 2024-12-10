/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.testing;


import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.cache.ReadWriteBackingMap;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ObservableMap;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
* Abstract base class for testing {@link ReadWriteBackingMap} CacheStore and
* BinaryEntryStore functionality.
* <p>
* Several system properties control the <i>default</i> behavior of the
* AbstractTestStore:
* <ul>
* <li>
* If the <tt>verbose</tt> system property is set to <tt>true</tt> the
* AbstractTestStore will log information about all operations.
* </li>
* <li>
* If the <tt>load.verbose</tt> system property is set to <tt>true</tt> the
* AbstractTestStore will log information about all <tt>load</tt> operations.
* </li>
* <li>
* If the <tt>loadAll.verbose</tt> system property is set to <tt>true</tt> the
* AbstractTestStore will log information about all <tt>loadAll</tt> operations.
* </li>
* <li>
* If the <tt>store.verbose</tt> system property is set to <tt>true</tt> the
* AbstractTestStore will log information about all <tt>store</tt> operations.
* </li>
* <li>
* If the <tt>storeAll.verbose</tt> system property is set to <tt>true</tt> the
* AbstractTestStore will log information about all <tt>storeAll</tt> operations.
* </li>
* <li>
* If the <tt>erase.verbose</tt> system property is set to <tt>true</tt> the
* AbstractTestStore will log information about all <tt>erase</tt> operations.
* </li>
* <li>
* If the <tt>eraseAll.verbose</tt> system property is set to <tt>true</tt> the
* AbstractTestStore will log information about all <tt>eraseAll</tt> operations.
* </li>
* <li>
* The <tt>load.duration</tt> system property can be used to control the length
* of time (in milliseconds) that <tt>load</tt> operations take.
* </li>
* <li>
* The <tt>loadAll.duration</tt> system property can be used to control the
* length of time (in milliseconds) that <tt>loadAll</tt> operations take.
* </li>
* <li>
* The <tt>store.duration</tt> system property can be used to control the length
* of time (in milliseconds) that <tt>store</tt> operations take.
* </li>
* <li>
* The <tt>storeAll.duration</tt> system property can be used to control the
* length of time (in milliseconds) that <tt>storeAll</tt> operations take.
* </li>
* <li>
* The <tt>erase.duration</tt> system property can be used to control the length
* of time (in milliseconds) that <tt>erase</tt> operations take.
* </li>
* <li>
* The <tt>eraseAll.duration</tt> system property can be used to control the
* length of time (in milliseconds) that <tt>eraseAll</tt> operations take.
* </li>
* <li>
* The <tt>load.failure.key</tt> system property specifies the key that will
* result in a <tt>load</tt> failure.
* </li>
* <li>
* The <tt>loadAll.failure.key</tt> system property specifies the key that will
* result in a <tt>loadAll</tt> failure.
* </li>
* <li>
* The <tt>store.failure.key</tt> system property specifies the key that will
* result in a <tt>store</tt> failure.
* </li>
* <li>
* The <tt>storeAll.failure.key</tt> system property specifies the key that will
* result in a <tt>storeAll</tt> failure.
* </li>
* <li>
* The <tt>erase.failure.key</tt> system property specifies the key that will
* result in a <tt>erase</tt> failure.
* </li>
* <li>
* The <tt>eraseAll.failure.key</tt> system property specifies the key that will
* result in a <tt>eraseAll</tt> failure.
* </li>
* <li>
* The <tt>load.miss.key</tt> system property specifies the key that will
* cause <tt>load</tt> to return <tt>null</tt>.
* </li>
* <li>
* The <tt>loadAll.miss.key</tt> system property specifies the key that will
* cause <tt>loadAll</tt> to return <tt>null</tt> for the specified key.
* </li>
* <li>
* The <tt>interrupt.threshold</tt> system property specifies how many times cachestore
* operations need to be interrupted before returning, or 0 for un-interruptible
* operations.
* </li>
* </ul>
* Each of these properties can be changed via the appropriate mutator method.
*/
public abstract class AbstractTestStore
        extends Base
    {
    /**
    * Default constructor.
    */
    public AbstractTestStore(ObservableMap mapStorage)
        {
        setVerbose        (Boolean.getBoolean("test.verbose"));
        setVerboseErase   (Boolean.getBoolean("test.erase.verbose"));
        setVerboseEraseAll(Boolean.getBoolean("test.eraseAll.verbose"));
        setVerboseLoad    (Boolean.getBoolean("test.load.verbose"));
        setVerboseLoadAll (Boolean.getBoolean("test.loadAll.verbose"));
        setVerboseStore   (Boolean.getBoolean("test.store.verbose"));
        setVerboseStoreAll(Boolean.getBoolean("test.storeAll.verbose"));

        setDurationErase   (Long.getLong("test.erase.duration",    0).longValue());
        setDurationEraseAll(Long.getLong("test.eraseAll.duration", 0).longValue());
        setDurationLoad    (Long.getLong("test.load.duration",     0).longValue());
        setDurationLoadAll (Long.getLong("test.loadAll.duration",  0).longValue());
        setDurationStore   (Long.getLong("test.store.duration",    0).longValue());
        setDurationStoreAll(Long.getLong("test.storeAll.duration", 0).longValue());

        setHeartbeatDuration(Long.getLong("test.heartbeat.duration", 0).longValue());

        setFailureKeyErase   (System.getProperty("test.erase.failure.key"));
        setFailureKeyEraseAll(System.getProperty("test.eraseAll.failure.key"));
        setFailureKeyLoad    (System.getProperty("test.load.failure.key"));
        setFailureKeyLoadAll (System.getProperty("test.loadAll.failure.key"));
        setFailureKeyStore   (System.getProperty("test.store.failure.key"));
        setFailureKeyStoreAll(System.getProperty("test.storeAll.failure.key"));

        setInterruptThreshold(Integer.getInteger("test.interrupt.threshold", 0).intValue());

        azzert(mapStorage != null, "Storage Map cannot be null.");
        m_mapStorage = mapStorage;
        m_mapStats   = new ConcurrentHashMap<>();
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Reset the method invocation statistics.
    */
    public void resetStats()
        {
        getStatsMap().clear();
        }

    /**
    * Return the simple (unqualified) class name of this AbstractTestStore.
    *
    * @return the simple (unqualified) class name
    */
    protected String getClassName()
        {
        return ClassHelper.getSimpleName(getClass());
        }

    /**
    * Log the given message if the given flag is <tt>true</tt>.
    *
    * @param fVerbose if <tt>true</tt> the message will be logged
    * @param sMsg     the message to log
    */
    protected void log(boolean fVerbose, String sMsg)
        {
        if (fVerbose)
            {
            Logger.log(getClassName() + " (" + new Date() + "): " + sMsg, Logger.ALWAYS);
            }
        }

    /**
    * Put the calling thread to sleep for the specified number of millseconds.
    *
    * @param cMillis the number of milliseconds to put the calling thread to
    *                sleep for
    */
    protected void delay(long cMillis)
        {
        int cInterrupts    = 0;
        int cMaxInterrupts = getInterruptThreshold();

        cMaxInterrupts = cMaxInterrupts == 0 ? Integer.MAX_VALUE : cMaxInterrupts;
        while (cMillis > 0)
            {
            long ldtStart = Base.getSafeTimeMillis();
            try
                {
                Blocking.sleep(cMillis);
                }
            catch (InterruptedException e)
                {
                if (++cInterrupts >= cMaxInterrupts)
                    {
                    throw ensureRuntimeException(e);
                    }

                if (isVerbose())
                    {
                    out(getClassName() + " interrupted");
                    }
                }

            cMillis -= Base.getSafeTimeMillis() - ldtStart;
            }
        }

    /**
    * If the string representations of the two given keys are equal, throw an
    * exception.
    *
    * @param oKeyFailure the first key to test
    * @param oKey        the second key to test
    */
    protected void checkForFailure(Object oKeyFailure, Object oKey)
        {
        boolean fEquals;

        if (oKey == null)
            {
            fEquals = oKeyFailure == null;
            }
        else if (oKeyFailure == null)
            {
            fEquals = false;
            }
        else
            {
            fEquals = oKeyFailure.equals(oKey.toString());
            }

        if (fEquals)
            {
            throw new RuntimeException("Expected " + getClassName() + " failure.");
            }
        }

    /**
    * Log a method invocation.
    *
    * @param sMethod  the name of the method that was invoked
    */
    protected void logMethodInvocation(String sMethod)
        {
        getStatsMap().compute(sMethod, (k, v) -> v == null ? 1 : v + 1);
        }


    // ----- accessors ------------------------------------------------------

    /**
     * Get the heartbeat duration.
     *
     * @return the heartbeat duration
     */
    public long getHeartbeatDuration()
        {
        return m_cDurationHeartbeat;
        }

    /**
     * Set the heartbeat duration.
     *
     * @param cDuration the new heartbeat duration
     */
    public void setHeartbeatDuration(long cDuration)
        {
        m_cDurationHeartbeat = cDuration;
        }

    /**
    * Return the value of the verbose flag for all operations.
    *
    * @return true if information about all operations will be logged
    */
    public boolean isVerbose()
        {
        return m_fVerbose;
        }

    /**
    * Set the verbose flag for all operations.
    *
    * @param fVerbose  true to log information about all operations
    */
    public void setVerbose(boolean fVerbose)
        {
        m_fVerbose = fVerbose;
        }

    /**
    * Return the value of the verbose flag for all erase operations.
    *
    * @return true if information about all erase operations will be logged
    */
    public boolean isVerboseErase()
        {
        return isVerbose() || m_fVerboseErase;
        }

    /**
    * Set the verbose flag for all erase operations.
    *
    * @param fVerbose  true to log information about all erase operations
    */
    public void setVerboseErase(boolean fVerbose)
        {
        m_fVerboseErase = fVerbose;
        }

    /**
    * Return the value of the verbose flag for all eraseAll operations.
    *
    * @return true if information about all eraseAll operations will be logged
    */
    public boolean isVerboseEraseAll()
        {
        return isVerbose() || m_fVerboseEraseAll;
        }

    /**
    * Set the verbose flag for all eraseAll operations.
    *
    * @param fVerbose  true to log information about all eraseAll operations
    */
    public void setVerboseEraseAll(boolean fVerbose)
        {
        m_fVerboseEraseAll = fVerbose;
        }

    /**
    * Return the value of the verbose flag for all load operations.
    *
    * @return true if information about all load operations will be logged
    */
    public boolean isVerboseLoad()
        {
        return isVerbose() || m_fVerboseLoad;
        }

    /**
    * Set the verbose flag for all load operations.
    *
    * @param fVerbose  true to log information about all load operations
    */
    public void setVerboseLoad(boolean fVerbose)
        {
        m_fVerboseLoad = fVerbose;
        }

    /**
    * Return the value of the verbose flag for all loadAll operations.
    *
    * @return true if information about all loadAll operations will be logged
    */
    public boolean isVerboseLoadAll()
        {
        return isVerbose() || m_fVerboseLoadAll;
        }

    /**
    * Set the verbose flag for all loadAll operations.
    *
    * @param fVerbose  true to log information about all loadAll operations
    */
    public void setVerboseLoadAll(boolean fVerbose)
        {
        m_fVerboseLoadAll = fVerbose;
        }

    /**
    * Return the value of the verbose flag for all store operations.
    *
    * @return true if information about all store operations will be logged
    */
    public boolean isVerboseStore()
        {
        return isVerbose() || m_fVerboseStore;
        }

    /**
    * Set the verbose flag for all store operations.
    *
    * @param fVerbose  true to log information about all store operations
    */
    public void setVerboseStore(boolean fVerbose)
        {
        m_fVerboseStore = fVerbose;
        }

    /**
    * Return the value of the verbose flag for all storeAll operations.
    *
    * @return true if information about all storeAll operations will be logged
    */
    public boolean isVerboseStoreAll()
        {
        return isVerbose() || m_fVerboseStoreAll;
        }

    /**
    * Set the verbose flag for all storeAll operations.
    *
    * @param fVerbose  true to log information about all storeAll operations
    */
    public void setVerboseStoreAll(boolean fVerbose)
        {
        m_fVerboseStoreAll = fVerbose;
        }

    /**
    * Return the duration (in milliseconds) of erase operations.
    *
    * @return the number of milliseconds that an erase operation will take
    */
    public long getDurationErase()
        {
        return m_cDurationErase;
        }

    /**
    * Set the duration (in milliseconds) of erase operations.
    *
    * @param cMillis  the number of milliseconds that an erase operation will
    *                 take
    */
    public void setDurationErase(long cMillis)
        {
        m_cDurationErase = cMillis;
        }

    /**
    * Return the duration (in milliseconds) of eraseAll operations.
    *
    * @return the number of milliseconds that an eraseAll operation will take
    */
    public long getDurationEraseAll()
        {
        return m_cDurationEraseAll;
        }

    /**
    * Set the duration (in milliseconds) of eraseAll operations.
    *
    * @param cMillis  the number of milliseconds that an eraseAll operation will
    *                 take
    */
    public void setDurationEraseAll(long cMillis)
        {
        m_cDurationEraseAll = cMillis;
        }

    /**
    * Return the duration (in milliseconds) of load operations.
    *
    * @return the number of milliseconds that a load operation will take
    */
    public long getDurationLoad()
        {
        return m_cDurationLoad;
        }

    /**
    * Set the duration (in milliseconds) of load operations.
    *
    * @param cMillis  the number of milliseconds that a load operation will take
    */
    public void setDurationLoad(long cMillis)
        {
        m_cDurationLoad = cMillis;
        }

    /**
    * Return the duration (in milliseconds) of loadAll operations.
    *
    * @return the number of milliseconds that a loadAll operation will take
    */
    public long getDurationLoadAll()
        {
        return m_cDurationLoadAll;
        }

    /**
    * Set the duration (in milliseconds) of loadAll operations.
    *
    * @param cMillis  the number of milliseconds that a loadAll operation will
    *                 take
    */
    public void setDurationLoadAll(long cMillis)
        {
        m_cDurationLoadAll = cMillis;
        }

    /**
    * Return the duration (in milliseconds) of store operations.
    *
    * @return the number of milliseconds that a store operation will take
    */
    public long getDurationStore()
        {
        return m_cDurationStore;
        }

    /**
    * Set the duration (in milliseconds) of store operations.
    *
    * @param cMillis  the number of milliseconds that a store operation will
    *                 take
    */
    public void setDurationStore(long cMillis)
        {
        m_cDurationStore = cMillis;
        }

    /**
    * Return the duration (in milliseconds) of storeAll operations.
    *
    * @return the number of milliseconds that a storeAll operation will take
    */
    public long getDurationStoreAll()
        {
        return m_cDurationStoreAll;
        }

    /**
    * Set the duration (in milliseconds) of storeAll operations.
    *
    * @param cMillis  the number of milliseconds that a storeAll operation will
    *                 take
    */
    public void setDurationStoreAll(long cMillis)
        {
        m_cDurationStoreAll = cMillis;
        }

    /**
    * Return the key that will cause an exception to be thrown by erase
    * operations.
    *
    * @return  the key that will cause erase operations to fail
    */
    public Object getFailureKeyErase()
        {
        return m_oFailureKeyErase;
        }

    /**
    * Set the key that will cause an exception to be thrown by erase
    * operations.
    *
    * @param oKey  the key that will cause erase operations to fail
    */
    public void setFailureKeyErase(Object oKey)
        {
        m_oFailureKeyErase = oKey;
        }

    /**
    * Return the key that will cause an exception to be thrown by eraseAll
    * operations.
    *
    * @return  the key that will cause eraseAll operations to fail
    */
    public Object getFailureKeyEraseAll()
        {
        return m_oFailureKeyEraseAll;
        }

    /**
    * Set the key that will cause an exception to be thrown by eraseAll
    * operations.
    *
    * @param oKey  the key that will cause eraseAll operations to fail
    */
    public void setFailureKeyEraseAll(Object oKey)
        {
        m_oFailureKeyEraseAll = oKey;
        }

    /**
    * Return the key that will cause an exception to be thrown by load
    * operations.
    *
    * @return  the key that will cause load operations to fail
    */
    public Object getFailureKeyLoad()
        {
        return m_oFailureKeyLoad;
        }

    /**
    * Set the key that will cause an exception to be thrown by load
    * operations.
    *
    * @param oKey  the key that will cause load operations to fail
    */
    public void setFailureKeyLoad(Object oKey)
        {
        m_oFailureKeyLoad = oKey;
        }

    /**
    * Return the key that will cause an exception to be thrown by loadAll
    * operations.
    *
    * @return  the key that will cause loadAll operations to fail
    */
    public Object getFailureKeyLoadAll()
        {
        return m_oFailureKeyLoadAll;
        }

    /**
    * Set the key that will cause an exception to be thrown by loadAll
    * operations.
    *
    * @param oKey  the key that will cause loadAll operations to fail
    */
    public void setFailureKeyLoadAll(Object oKey)
        {
        m_oFailureKeyLoadAll = oKey;
        }

    /**
    * Return the key that will cause an exception to be thrown by store
    * operations.
    *
    * @return  the key that will cause store operations to fail
    */
    public Object getFailureKeyStore()
        {
        return m_oFailureKeyStore;
        }

    /**
    * Set the key that will cause an exception to be thrown by store
    * operations.
    *
    * @param oKey  the key that will cause store operations to fail
    */
    public void setFailureKeyStore(Object oKey)
        {
        m_oFailureKeyStore = oKey;
        }

    /**
    * Return the key that will cause an exception to be thrown by storeAll
    * operations.
    *
    * @return  the key that will cause storeAll operations to fail
    */
    public Object getFailureKeyStoreAll()
        {
        return m_oFailureKeyStoreAll;
        }

    /**
    * Set the key that will cause an exception to be thrown by storeAll
    * operations.
    *
    * @param oKey  the key that will cause storeAll operations to fail
    */
    public void setFailureKeyStoreAll(Object oKey)
        {
        m_oFailureKeyStoreAll = oKey;
        }

    /**
    * Return the number of times a cachestore operation can be interrupted
    * before returning, or 0 for un-interruptible operations.
    *
    * @return the interrupt threshold, or 0 for un-interruptible operations
    */
    public int getInterruptThreshold()
        {
        return m_cInterruptThreshold;
        }

    /**
    * Set the number of times a cachestore operation can be interrupted before
    * returning, or 0 for un-interruptible operations.
    *
    * @param cInterrupt  the interrupt threshold, or 0 for un-interruptible operations
    */
    public void setInterruptThreshold(int cInterrupt)
        {
        m_cInterruptThreshold = cInterrupt;
        }

    /**
    * Return the ObservableMap used by the AbstractTestStore to load, store, and
    * erase persisted objects.
    *
    * @return the ObservableMap used to load, store, and erase objects
    */
    public ObservableMap getStorageMap()
        {
        return m_mapStorage;
        }

    /**
    * Return the Map used to log method invocations.
    *
    * @return the Map used to log method invocations.
    */
    public ConcurrentMap<String, Integer> getStatsMap()
        {
        return m_mapStats;
        }

    // ----- data members ---------------------------------------------------

    /**
    * Verbose flag: all operations
    */
    private boolean m_fVerbose;

    /**
    * Verbose flag: erase operations
    */
    private boolean m_fVerboseErase;

    /**
    * Verbose flag: eraseAll operations
    */
    private boolean m_fVerboseEraseAll;

    /**
    * Verbose flag: load operations
    */
    private boolean m_fVerboseLoad;

    /**
    * Verbose flag: loadAll operations
    */
    private boolean m_fVerboseLoadAll;

    /**
    * Verbose flag: store operations
    */
    private boolean m_fVerboseStore;

    /**
    * Verbose flag: storeAll operations
    */
    private boolean m_fVerboseStoreAll;

    /**
    * Duration (in millseconds) of erase operations.
    */
    private long m_cDurationErase;

    /**
    * Duration (in millseconds) of eraseAll operations.
    */
    private long m_cDurationEraseAll;

    /**
    * Duration (in millseconds) of load operations.
    */
    private long m_cDurationLoad;

    /**
    * Duration (in millseconds) of loadAll operations.
    */
    private long m_cDurationLoadAll;

    /**
    * Duration (in millseconds) of store operations.
    */
    private long m_cDurationStore;

    /**
    * Duration (in millseconds) of storeAll operations.
    */
    private long m_cDurationStoreAll;

    /**
    * Failure key for erase operations.
    */
    private Object m_oFailureKeyErase;

    /**
    * Failure key for eraseAll operations.
    */
    private Object m_oFailureKeyEraseAll;

    /**
    * Failure key for load operations.
    */
    private Object m_oFailureKeyLoad;

    /**
    * Failure key for loadAll operations.
    */
    private Object m_oFailureKeyLoadAll;

    /**
    * Failure key for store operations.
    */
    private Object m_oFailureKeyStore;

    /**
    * Failure key for storeAll operations.
    */
    private Object m_oFailureKeyStoreAll;

    private long m_cDurationHeartbeat;

    /**
    * The interrupt threshold.
    */
    private int m_cInterruptThreshold;

    /**
    * The ObservableMap used to load, store, and erase objects.
    */
    private final ObservableMap m_mapStorage;

    /**
    * The Map<String sMethod, Integer cInvokes> used to track method
    * invocation stats.
    */
    private final ConcurrentMap<String, Integer> m_mapStats;
    }
