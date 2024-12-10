/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.io.Serializable;

import java.text.Collator;
import java.text.CollationKey;

import java.util.Map;


/**
* Implements a collator which caches its keys.
*
* @version 1.00, 10/01/98
* @author 	Cameron Purdy
*/
public class CacheCollator
        extends Collator
        implements Serializable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct this collator to cache the results of another collator.
    *
    * @param collator  the collator to delegate to
    */
    public CacheCollator(Collator collator)
        {
        super();

        // a copy of the passed collator is made so that the caller cannot
        // modify the collator used by the cache collator
        this.collator = (Collator) collator.clone();
        }


    // ----- Collator methods -----------------------------------------------

    /**
    * Returns this Collator's strength property.  The strength property
    * determines the minimum level of difference considered significant
    * during comparison.
    *
    * @return this Collator's current strength property.
    */
    public int getStrength()
        {
        return collator.getStrength();
        }

    /**
    * Sets this Collator's strength property.  The strength property
    * determines the minimum level of difference considered significant
    * during comparison.
    *
    * @param newStrength the new strength value.
    *
    * @exception UnsupportedOperationException  always thrown
    */
    public void setStrength(int newStrength)
        {
        throw new UnsupportedOperationException("cache collator is immutable");
        }

    /**
    * Get the decomposition mode of this Collator. Decomposition mode
    * determines how Unicode composed characters are handled. Adjusting
    * decomposition mode allows the user to select between faster and more
    * complete collation behavior.
    *
    * @return the decomposition mode
    */
    public int getDecomposition()
        {
        return collator.getDecomposition();
        }

    /**
    * Set the decomposition mode of this Collator. See getDecomposition
    * for a description of decomposition mode.
    *
    * @param decomposition the new decomposition mode
    *
    * @exception UnsupportedOperationException  always thrown
    */
    public void setDecomposition(int decomposition)
        {
        throw new UnsupportedOperationException("cache collator is immutable");
        }

    /**
    * Compares the source string to the target string according to the
    * collation rules for this Collator.  Returns an integer less than,
    * equal to or greater than zero depending on whether the source String
    * is less than, equal to or greater than the target string.  See the
    * Collator class description for an example of use.
    * <p>
    * For a one time comparison, this method has the best performance. If a
    * given String will be involved in multiple comparisons,
    * CollationKey.compareTo has the best performance. See the Collator class
    * description for an example using CollationKeys.
    *
    * @param source the source string.
    * @param target the target string.
    *
    * @return Returns an integer value. Value is less than zero if source is
    *         less than target, value is zero if source and target are equal,
    *         value is greater than zero if source is greater than target.
    */
    public int compare(String source, String target)
        {
        return getCollationKey(source).compareTo(getCollationKey(target));
        }

    /**
    * Transforms the String into a series of bits that can be compared bitwise
    * to other CollationKeys. CollationKeys provide better performance than
    * Collator.compare when Strings are involved in multiple comparisons.
    * See the Collator class description for an example using CollationKeys.
    *
    * @param source the string to be transformed into a collation key.
    *
    * @return the CollationKey for the given String based on this Collator's
    *         collation rules. If the source String is null, a null
    *         CollationKey is returned.
    */
    public CollationKey getCollationKey(String source)
        {
        if (source == null)
            {
            return null;
            }

        Map cache = getCache();
        CollationKey key = (CollationKey) cache.get(source);
        if (key == null)
            {
            key = collator.getCollationKey(source);
            cache.put(source, key);
            }
        return key;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Clone the caching collator.
    *
    * @return a clone of this object
    */
    public Object clone()
        {
        return this;
        }

    /**
    * Compares the equality of two Collators.
    *
    * @param that the Collator to be compared with this.
    *
    * @return true if this Collator is the same as that Collator;
    *         false otherwise.
    */
    public boolean equals(Object that)
        {
        return collator.equals(that);
        }

    /**
    * Generates the hash code for this Collator.
    *
    * @return the hashcode of the delegatee collator
    */
    public int hashCode()
        {
        return collator.hashCode();
        }


    // ----- internal -------------------------------------------------------

    /**
    * @return the internal cache map
    */
    protected Map getCache()
        {
        Map cache = m_cache;

        if (cache == null)
            {
            m_cache = cache = new SafeHashMap();
            }

        return cache;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The collator to delegate to.
    */
    private Collator collator;

    /**
    * The cache of collation keys.
    */
    private transient Map m_cache;
    }
