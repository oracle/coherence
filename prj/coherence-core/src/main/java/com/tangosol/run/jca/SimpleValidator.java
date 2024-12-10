/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.jca;

import com.tangosol.net.cache.CacheMap;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.TransactionMap;
import com.tangosol.util.Versionable;

import com.tangosol.util.filter.PresentFilter;

import com.tangosol.util.processor.ConditionalProcessor;

import java.io.Serializable;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
* Simple generic Validator implementation that uses hashCode values
* for the enlisted resources to resolve optimistic transaction conflicts.
*
* @author gg 2003.12.19
* @since Coherence 2.3
*/
public class SimpleValidator
        extends    Base
        implements TransactionMap.Validator
    {
    /**
    * Default constructor.
    */
    public SimpleValidator()
        {
        }

    // ----- accessors ------------------------------------------------------

    /**
    * Obtain a map that holds the version objects for resources enlisted
    * into a transaction.
    *
    * @return the versions map
    */
    public Map getVersionMap()
        {
        return m_mapVersion;
        }


    // ----- Validator interface --------------------------------------------

    /**
    * Enlist the resource with the specified transaction.
    * <p>
    * This method is invoked for all resources that are "used" by the
    * transaction immediately before the resource value is copied into
    * the local map. If the resource value implements the {@link Versionable}
    * interface, the corresponding version indicator will be placed into
    * the version map; otherwise the value's hashCode will be used.
    *
    * <p>
    * It is this method's responsibility to call a next Validator in the
    * validation chain.
    *
    * @param mapTx  the TransactionMap to enlist the resource with
    * @param oKey   the resource key to be enlisted with the transaction
    */
    public void enlist(TransactionMap mapTx, Object oKey)
        {
        Map mapVersion = getVersionMap();
        if (!mapVersion.containsKey(oKey))
            {
            mapVersion.put(oKey, calculateVersion(mapTx.getBaseMap().get(oKey)));

            TransactionMap.Validator validatorNext = getNextValidator();
            if (validatorNext != null)
                {
                validatorNext.enlist(mapTx, oKey);
                }
            }
        }

    /**
    * Validate that the content of the TransactionMap is "fit" to be
    * committed into the base map.
    * <p>
    * This method is invoked during "prepare" phase after all the resources
    * involved in this transaction are successfully locked at the base map.
    * The Validator is expected to retrieve the "old" and "new" values
    * (using <code>map.get(oKey)</code>, <code>map.getBaseMap().get(oKey)</code>)
    * and use the information gathered during "enlist" calls to make
    * the determination whether or not commit should be allowed to proceed.
    * <p>
    * To force a roll back it should throw an exception indicating the
    * reason this transaction cannot be committed.
    * When that happens, the sets are expected to hold just the keys
    * of the "offending" resources.
    * <p>
    * It is this method's responsibility to call a next Validator in the
    * validation chain (i.e. <code>
    * getNextValidator().validate(map, setI, setU, setD, setR, setF);</code>)
    *
    * @param mapTx      the TransactionMap that is being prepared
    * @param setInsert  the set of inserted resources
    * @param setUpdate  the set of updated resources
    * @param setDelete  the set of deleted resources
    * @param setRead    the set of read resources. It is always empty for
    *                   TRANSACTION_GET_COMMITTED isolation level.
    * @param setPhantom the set of phantom resources, that is resources that
    *                   were added to the base map, but were not known to the
    *                   transaction. This set can be not empty only for
    *                   TRANSACTION_GET_SERIALIZED isolation level.
    *
    * @exception ConcurrentModificationException if the validator detects
    *            an unresolveable conflict between the resources
    */
    public void validate(TransactionMap mapTx,
                         Set setInsert, Set setUpdate, Set setDelete,
                         Set setRead, Set setPhantom)
            throws ConcurrentModificationException
        {
        TransactionMap.Validator validatorNext = getNextValidator();
        if (validatorNext != null)
            {
            validatorNext.validate(mapTx,
                setInsert, setUpdate, setDelete, setRead, setPhantom);
            }

        if (!setInsert.isEmpty())
            {
            // inserts -- there should be no value at the base
            validateInsert(mapTx, setInsert);
            }

        if (!setUpdate.isEmpty())
            {
            // updates -- the versions should be the same
            validateVersion(mapTx, setUpdate);
            }

        if (!setDelete.isEmpty())
            {
            // deletes -- the versions should be the same
            validateVersion(mapTx, setDelete);
            }

        if (!setRead.isEmpty())
            {
            // reads -- the values should be the same
            validateValue(mapTx, setRead);
            }

        if (!setPhantom.isEmpty())
            {
            // phantoms -- should be empty
            throw new ConcurrentModificationException(
                "Phantom set is not empty: " + setPhantom);
            }
        }

    /**
    * Retrive the next Validator.
    *
    * @return the next Validator
    */
    public TransactionMap.Validator getNextValidator()
        {
        return m_validatorNext;
        }

    /**
    * Set the next Validator.
    *
    * @param validator the Validator to be added
    */
    public void setNextValidator(TransactionMap.Validator validator)
        {
        m_validatorNext = validator;
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Validate the set of the inserts.
    * <p>
    * If the inserts do not exist in the base map (as they should not),
    * remove them from the specified key set, so only keys corresponding to
    * the conflicting inserts are left in the key set.
    *
    * @param mapTx   the TransactionMap
    * @param setKey  the key set of inserted resources
    *
    * @throws ConcurrentModificationException if conflicts are detected
    */
    protected void validateInsert(TransactionMap mapTx, Set setKey)
        {
        Map mapBase = mapTx.getBaseMap();

        if (mapBase instanceof InvocableMap)
            {
            InvocableMap cacheBase = (InvocableMap) mapBase;
            Map mapExists = cacheBase.invokeAll(setKey,
                new ConditionalProcessor(PresentFilter.INSTANCE,
                    NullImplementation.getEntryProcessor()));

            for (Iterator iter = mapExists.entrySet().iterator(); iter.hasNext();)
                {
                Map.Entry entry = (Map.Entry) iter.next();

                if (entry.getValue() == null)
                    {
                    // that key is indeed not present
                    setKey.remove(entry.getKey());
                    }
                }
            }
        else
            {
            for (Iterator iter = setKey.iterator(); iter.hasNext();)
                {
                Object oKey = iter.next();

                if (!mapBase.containsKey(oKey))
                    {
                    // that key is indeed not present
                    iter.remove();
                    }
                }
            }

        if (!setKey.isEmpty())
            {
            throw new ConcurrentModificationException(
                "Insert conflict for: " + setKey);
            }
        }

    /**
    * Validate the set of the updates or deletes.
    * <p>
    * If the version of an updated or removed resource matches to the version
    * stored off in the version map at the time when the resource was enlisted,
    * remove it from the specified key set, so only keys corresponding to
    * the conflicting updates, removes or reads are left in the key set.
    *
    * @param mapTx   the TransactionMap
    * @param setKey  the key set of updated or removed resources
    *
    * @throws ConcurrentModificationException if conflicts are detected
    */
    protected void validateVersion(TransactionMap mapTx, Set setKey)
        {
        Map mapBase    = mapTx.getBaseMap();
        Map mapVersion = getVersionMap();
        Map mapCurrent = mapBase instanceof CacheMap ?
            ((CacheMap) mapBase).getAll(setKey) : mapBase;

        for (Iterator iter = setKey.iterator(); iter.hasNext();)
            {
            Object oKey = iter.next();

            Comparable oOrig = (Comparable) mapVersion.get(oKey);
            Comparable oCurr = calculateVersion(mapCurrent.get(oKey));

            if (oCurr.compareTo(oOrig) == 0)
                {
                iter.remove();
                }
            }

        if (!setKey.isEmpty())
            {
            throw new ConcurrentModificationException(
                "Version conflict for: " + setKey);
            }
        }

    /**
    * Validate the set of the read values.
    * <p>
    * If the values that were read during transaction are equal to the values
    * as they exist in the base map, remove them from the specified key set,
    * so only keys corresponding to the conflicting reads are left in the set.
    * <p>
    * Note: this can occur only for repeatable or serializable isolation level.
    *
    * @param mapTx   the TransactionMap
    * @param setKey  the key set of read resources
    *
    * @throws ConcurrentModificationException if conflicts are detected
    */
    protected void validateValue(TransactionMap mapTx, Set setKey)
        {
        Map mapBase    = mapTx.getBaseMap();
        Map mapCurrent = mapBase instanceof CacheMap ?
            ((CacheMap) mapBase).getAll(setKey) : mapBase;

        for (Iterator iter = setKey.iterator(); iter.hasNext();)
            {
            Object oKey = iter.next();

            Object oValTx   = mapTx.get(oKey);
            Object oValCurr = mapCurrent.get(oKey);

            if (equals(oValTx, oValCurr))
                {
                iter.remove();
                }
            }

        if (!setKey.isEmpty())
            {
            throw new ConcurrentModificationException(
                "Value conflict for: " + setKey);
            }
        }

    /**
    * Generate the Comparable version indicator for a given resource value.
    * <p>
    * If the value implements the {@link Versionable} interface, the
    * corresponding version indicator is returned; otherwise the value's
    * hashCode is used.
    *
    * @return a Comparable version indicator
    */
    protected Comparable calculateVersion(Object oValue)
        {
        return oValue == null ? Integer.valueOf(0) :
               oValue instanceof Versionable ?
                   ((Versionable) oValue).getVersionIndicator() :
               oValue instanceof Serializable ?
                   Integer.valueOf(ExternalizableHelper.toBinary(oValue).hashCode()) :
                   Integer.valueOf(oValue.hashCode());
        }


    // ----- data fields ----------------------------------------------------

    /**
    * The next validator.
    */
    private TransactionMap.Validator m_validatorNext;

    /**
    * The map that holds version objects for enlisted resources.
    */
    protected Map m_mapVersion = new HashMap();
    }
