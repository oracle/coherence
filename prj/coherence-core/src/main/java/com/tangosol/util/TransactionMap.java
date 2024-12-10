/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.util.ConcurrentModificationException;
import java.util.Set;

/**
* ConcurrentMap with additional transaction support.
* <p>
* TransactionMap is a thread safe map that could be used by transactions.
* It maintains two copies of the data: the <i>base</i> [committed]
* and <i>local</i> [uncommitted].
* <p>
* In all cases, "get" operation reads the local map first and, if not found,
* the base map second. The "put" and "remove" always use the local map
* (using a special mark to denote the removed resources). Additionally,
* depending on a concurrency mode and transaction isolation level the
* processing strategy for various operations differs.
*
* <ul>
* <li> {@link #CONCUR_PESSIMISTIC}
*   <ul>
*   <li>{@link #TRANSACTION_GET_COMMITTED}<br>
*      "put" and "remove" operations lock the corresponding resources on
*       the base map.
*   <li>{@link #TRANSACTION_REPEATABLE_GET}<br>
*      same as TRANSACTION_GET_COMMITTED plus "get" operations lock the
*      corresponding resources on the base map.
*   <li>{@link #TRANSACTION_SERIALIZABLE}<br>
*      same as TRANSACTION_REPEATABLE_GET plus "keySet" operations lock the
*      entire base map to prevent it from adding or removing resources
*   </ul><br>
*   During the "prepare" operation the locks for all the resources contained
*   in the local map are checked (to make sure the locks did not expire).
*   Upon successful validation the "commit" phase copies the local data
*   into the base map and unlock everything.
*   Any exception during "prepare" or "commit" causes the rollback which
*   clears the local map and unlocks all relevant resources.
* <p>
* <li> {@link #CONCUR_OPTIMISTIC}
*   <ul>
*   <li>{@link #TRANSACTION_GET_COMMITTED}<br>
*      no additional processing
*   <li>{@link #TRANSACTION_REPEATABLE_GET}<br>
*      "get" operations move the retrieved values into the local map
*   <li>{@link #TRANSACTION_SERIALIZABLE}<br>
*      same as TRANSACTION_REPEATABLE_GET plus "keySet" operations move all
*      the values to the local map and "disconnect" the base to prevent the
*      "phantom" gets
*   </ul><br>
*   During the "prepare" operation an attempt is made to lock all the
*   resources contained in the local map and, if successful, the affected
*   key sets are validated using the current Validator (chain).
*   Upon successful validation the "commit" phase copies the local data
*   into the base map and unlocks everything.
*   Any exception during "prepare" or "commit" causes the rollback which
*   clears the local map and unlocks all relevant resources.
*   <p>
* <li> {@link #CONCUR_EXTERNAL}
*   <p>
*   This concurrency strategy is very similar to the optimistic one, except that
*   no locking is performed during the "prepare" operation and all
*   synchronization and validation are assumed to be done by a supplied Validator.
* </ul>
*
* @see <a href="http://www.cs.wisc.edu/~nil/764/Trans/14_p213-kung.pdf">
* H.T. KUNG and JOHN T. ROBINSON, On Optimistic Methods for Concurrency
* Control, ACM Transactions on Database Systems, Vol. 6, No. 2, June 1981.</a>
*
* @author gg  2002.03.21
*/
public interface TransactionMap
        extends ConcurrentMap
    {
    /**
    * Return the base map, which contains this TransactionMap's committed data.
    *
    * @return the ConcurrentMap serving as a base for this TransactionMap.
    */
    public ConcurrentMap getBaseMap();

    /**
    * Retrieve this TransactionMap's current transaction isolation level.
    * <p>
    * The TRANSACTION_* constants defined in this interface are the
    * possible transaction isolation levels.
    *
    * @return the current TRANSACTION_* isolation value
    */
    public int getTransactionIsolation();

    /**
    * Attempt to change the transaction isolation level to the specified value.
    * <p>
    * The TRANSACTION_* constants defined in this interface are the
    * possible transaction isolation levels.
    * <p>
    * <b>Note:</b> This method cannot be called while in the middle
    * of a transaction.
    *
    * @param nLevel one of the TRANSACTION_* isolation values
    *
    * @exception IllegalStateException if the transaction isolation
    *            level cannot be changed
    */
    public void setTransactionIsolation(int nLevel);

    /**
    * Retrieve this TransactionMap's current concurrency mode.
    * <p>
    * The CONCUR_* constants defined in this interface are the possible
    * concurrency mode values.
    *
    * @return the current CONCUR_* mode
    */
    public int getConcurrency();

    /**
    * Attempt to change the concurrency mode to the given value.
    * <p>
    * The CONCUR_* constants defined in this interface are the possible
    * concurrency mode values.
    * <p>
    * <b>Note:</b> This method cannot be called while in the middle
    * of a transaction.
    *
    * @param nConcurrency one of the CONCUR_* mode values
    *
    * @exception IllegalStateException if the concurrency mode
    *            cannot be changed
    */
    public void setConcurrency(int nConcurrency);

    /**
    * Check whether or not the values stored in this TransactionMap are
    * known to be immutable.
    *
    * @return true iff the values are known to be immutable
    *
    * @since Coherence 2.3
    */
    public boolean isValuesImmutable();

    /**
    * Specify whether or not the values stored in this TransactionMap are
    * known to be immutable.
    * <p>
    * If the values are not known to be immutable, TransactionMap must
    * assume that they are mutable, and will clone the objects returned by
    * the "get" method to ensure that any changes made to those values are
    * rolled back if the transaction is rolled back.
    * <p>
    * By explicitly specifying that the values are known to be immutable,
    * the TransactionMap is permitted to skip the cloning step, which may
    * result in better performance.
    * <p>
    * <b>Note:</b> This method cannot be called while in the middle
    * of a transaction.
    *
    * @param fImmutable  true iff the values are known to be immutable
    *
    * @exception IllegalStateException if the setting cannot be changed
    *
    * @since Coherence 2.3
    */
    public void setValuesImmutable(boolean fImmutable);

    /**
    * Retrieve transaction timeout value for this TransactionMap.
    *
    * @return transaction timeout value in seconds.
    */
    public int getTransactionTimeout();

    /**
    * Set transaction timeout value for this TransactionMap.
    * <p>
    * <b>Note:</b> This method cannot be called while in the middle
    * of a transaction.
    *
    * @param cSeconds  transaction timeout value in seconds.
    *        Value of zero means "no timeout".
    *
    * @exception IllegalStateException if the concurrency mode
    *            cannot be changed
    */
    public void setTransactionTimeout(int cSeconds);

    /**
    * Retrieve the topmost Validator in TransactionMap's validation chain.
    *
    * @return the current Validator
    */
    public Validator getValidator();

    /**
    * Add the specified Validator to the top of validation chain
    * for this TransactionMap. The Validator is only used if the
    * concurrency setting is CONCUR_OPTIMISTIC or CONCUR_EXTERNAL.
    * <p>
    * <b>Note:</b> This method cannot be called while in the middle
    * of a transaction.
    *
    * @param validator the Validator to be added
    *
    * @exception IllegalStateException if the validator cannot be added
    */
    public void setValidator(Validator validator);

    /**
    * Start a transaction for this TransactionMap. After this method is
    * called and before commit or rollback are called, attempt to change
    * the concurrency, isolation or timeout value will throw an exception.
    * <p>
    * <b>Note:</b> specifc implementations may choose to support the
    * concept of an implicit begin of a transaction making this
    * operation a no-op.
    *
    * @exception IllegalStateException if this TransactionMap has been
    *            already started
    */
    public void begin();

    /**
    * Prepare to commit changes made to the TransactionMap. This phase
    * usually ensures that all resources involved in this transaction are
    * locked at the base map and validated by the current Validator.
    * If this map serves as a base to another (nested) TransactionMap,
    * specific implementations may choose to not allow its data to be
    * prepared until the nested map is either committed or rolled back.
    *
    * @exception ConcurrentModificationException if the changes cannot be
    *            prepared due to the concurrency limitations
    * @exception IllegalStateException if this TransactionMap serves as a
    *            base to a nested TransactionMap that has not yet been
    *            committed or rolled back
    */
    public void prepare();

    /**
    * Commit the changes made to the TransactionMap. This effectively
    * means copying the content of this map into the base map, clearing
    * the content of this map and releasing all the locks.
    * If this map serves as a base to another (nested) TransactionMap,
    * specific implementations may choose to not allow its data to be
    * committed until the nested map is either committed or rolled back.
    *
    * @exception ConcurrentModificationException if the changes cannot be
    *            committed due to the concurrency limitations
    * @exception IllegalStateException if this TransactionMap serves as a
    *            base to a nested TransactionMap that has not yet been
    *            committed or rolled back
    */
    public void commit();

    /**
    * Rollback the changes made to the TransactionMap. This effectively
    * means clearing the content of this map and releasing all the locks
    * without changing the content of the base map.
    */
    public void rollback();

    /**
    * Dirty gets are prevented; non-repeatable gets and phantom
    * gets can occur.  This level only prohibits a transaction
    * from getting an uncommitted values from a map.
    */
    public static final int TRANSACTION_GET_COMMITTED  = 1;

    /**
    * Dirty gets and non-repeatable gets are prevented; phantom
    * gets can occur.  This level prohibits a transaction from
    * getting an uncommitted values in a map, and it also
    * prohibits the situation where one transaction gets a value,
    * a second transaction alters the value, and the first transaction
    * retrieves the value again, getting a different values the second time
    * (a "non-repeatable get").
    */
    public static final int TRANSACTION_REPEATABLE_GET = 2;

    /**
    * Dirty gets, non-repeatable gets and phantom gets are prevented.
    * This level includes the prohibitions in TRANSACTION_REPEATABLE_GET
    * and further prohibits the situation where one transaction gets
    * an iterator for keys or values, a second transaction inserts a value,
    * and the first transaction requests an iterator again, retrieving
    * the additional "phantom" values the second time.
    */
    public static final int TRANSACTION_SERIALIZABLE   = 3;

    /**
    * Pessimistic concurrency. Every time an item is added to the
    * TransactionMap (as a result of either "put", "remove" or "get" operation)
    * the corresponding item is "locked" at the base.
    */
    public static final int CONCUR_PESSIMISTIC         = 1;

    /**
    * Optimistic concurrency. No locking occurs during "put", "remove" or "get"
    * operations. All synchronization and validation is instead deferred until
    * the "prepare" time.
    */
    public static final int CONCUR_OPTIMISTIC          = 2;

    /**
    * External concurrency. No locking is performed automatically. All
    * synchronization and validation are assumed to be done by a custom
    * transaction Validator during the {@link Validator#validate validate}
    * call.
    *
    * @since Coherence 3.3
    */
    public static final int CONCUR_EXTERNAL            = 3;

    /**
    * A callback interface used by TransactionMap implementations.
    * <p>
    * By providing an implementation of this interface, it is possible to
    * provide alternative strategies for verifying the correctness of
    * concurrent execution of transactions.
    */
    public interface Validator
        {
        /**
        * Enlist the resource with the specified transaction.
        * <p>
        * This method is invoked for all resources that are "used" by the
        * transaction immediately before the resource value is copied into
        * the local map.
        * <p>
        * It is this method's responsibility to call a next Validator in the
        * validation chain
        * (i.e. <code>getNextValidator().enlist(map, okey);</code>)
        *
        * @param mapTx  the TransactionMap to enlist the resource with
        * @param oKey   the resource key to be enlisted with the transaction
        */
        public void enlist(TransactionMap mapTx, Object oKey);

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
                throws ConcurrentModificationException;

        /**
        * Retrive the next Validator.
        *
        * @return the next Validator
        */
        public Validator getNextValidator();

        /**
        * Set the next Validator.
        *
        * <b>Note:</b> This method cannot be called while in the middle
        * of a validation (commit) phase.
        *
        * @param v  the Validator to be added
        *
        * @exception IllegalStateException if the next validator cannot be changed
        */
        public void setNextValidator(Validator v);
        }
    }
