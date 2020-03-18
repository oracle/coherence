/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.io.Serializable;


/**
* MapTrigger represents a functional agent that allows to validate, reject or
* modify mutating operations against an underlying map. The trigger operates on
* an {@link Entry} object that represents a pending mutation that is about to be
* committed to the underlying map. A MapTrigger could be registered with any
* ObservableMap using the {@link MapTriggerListener} class:
* <pre>
*   NamedCache cache = CacheFactory.getCache(sCacheName);
*   MapTrigger trigger = new MyCustomTrigger();
*   cache.addMapListener(new MapTriggerListener(trigger));
* </pre>
* <b>Note:</b> In a clustered environment, MapTrigger registration process
* requires triggers to be serializable and providing a non-default
* implementation of the hashCode() and equals() methods.
* Failure to do so may result in duplicate registration and a redundant entry
* processing by equivalent, but "not equal" MapTrigger objects.
*
* @see com.tangosol.util.filter.FilterTrigger
*
* @author cp/gg  2008.03.11
* @since Coherence 3.4
*/
public interface MapTrigger<K, V>
        extends Serializable
    {
    /**
    * This method is called before the result of a mutating operation
    * represented by the specified Entry object is committed into the underlying
    * map.
    * <p>
    * An implementation of this method can evaluate the change by analyzing the
    * original and the new value, and can perform any of the following:
    * <ul>
    *   <li> override the requested change by calling {@link Entry#setValue}
    *        with a different value;
    *   <li> undo the pending change by resetting the entry value to the
    *        original value obtained from {@link Entry#getOriginalValue};
    *   <li> remove the entry from the underlying map by calling
    *        {@link Entry#remove};
    *   <li> reject the pending change by throwing a RuntimeException, which
    *        will prevent any changes from being committed, and will result in
    *        the exception being thrown from the operation that attempted to
    *        modify the map; or
    *   <li> do nothing, thus allowing the pending change to be committed to the
    *        underlying map.
    * </ul>
    *
    * @param entry  a {@link Entry} object that represents the pending change to
    *               be committed to the map, as well as the original state of
    *               the Entry
    */
    public void process(Entry<K, V> entry);


    // ----- MapTrigger.Entry interface -------------------------------------

    /**
    * A MapTrigger Entry represents a pending change to an Entry that is about
    * to committed to the underlying Map. The methods inherited from
    * {@link InvocableMap.Entry} provide both the pending state and the ability
    * to alter that state. The original state of the Entry can be obtained using
    * the {@link #getOriginalValue()} and {@link #isOriginalPresent} methods.
    */
    public interface Entry<K, V>
            extends InvocableMap.Entry<K, V>
        {
        /**
        * Determine the value that existed before the start of the mutating
        * operation that is being evaluated by the trigger.
        *
        * @return the original value corresponding to this Entry; may be null if
        *         the value is null or if the Entry did not exist in the Map
        */
        public V getOriginalValue();

        /**
        * Determine whether or not the Entry existed before the start of the
        * mutating operation that is being evaluated by the trigger.
        *
        * @return true iff this Entry was existent in the containing Map
        */
        public boolean isOriginalPresent();
        }
    }
