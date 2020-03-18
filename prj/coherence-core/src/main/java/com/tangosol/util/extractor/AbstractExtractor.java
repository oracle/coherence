/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.extractor;


import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.MapTrigger;
import com.tangosol.util.QueryMap;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.comparator.QueryMapComparator;
import com.tangosol.util.comparator.SafeComparator;

import java.io.Serializable;

import java.util.Map;

import javax.json.bind.annotation.JsonbProperty;


/**
* Abstract base for ValueExtractor implementations.  It provides common
* functionality that allows any extending extractor to be used as a value
* Comparator.
* <p>
* Starting with Coherence 3.5, when used to extract information that is coming
* from a Map, subclasses have the additional ability to operate against the
* Map.Entry instead of just the value. In other words, like the
* {@link EntryExtractor} class, this allows an extractor implementation to
* extract a desired value using all available information on the corresponding
* Map.Entry object and is intended to be used in advanced custom scenarios, when
* application code needs to look at both key and value at the same time or can
* make some very specific assumptions regarding to the implementation details of
* the underlying Entry object. To maintain full backwards compatibility, the
* default behavior remains to extract from the Value property of the Map.Entry.
* <p>
* <b>Note:</b> subclasses are responsible for initialization and POF and/or
* Lite serialization of the {@link #m_nTarget} field.
*
* @author gg 2003.09.22
*/
public abstract class AbstractExtractor<T, E>
        extends    ExternalizableHelper
        implements ValueExtractor<T, E>, QueryMapComparator, Serializable
    {
    // ----- ValueExtractor interface ---------------------------------------

    @Override
    public E extract(T oTarget)
        {
        if (oTarget == null)
            {
            return null;
            }
        else
            {
            throw new UnsupportedOperationException();
            }
        }

    @Override
    public int getTarget()
        {
        return m_nTarget;
        }

    // ----- CanonicallyNamed interface -------------------------------------

    @Override
    public String getCanonicalName()
        {
        return m_sNameCanon;
        }

    // ----- Object methods -------------------------------------------------

    /**
    * Equivalence by canonical name and target.
    * <p>
    * When precondition {@link #isCanonicallyEquatable(Object)} is false,
    * fall back to implementation specific equals implementation.
    *
    * @param o  the reference object with which to compare
    *
    * @return {@code true} if canonical name match and no target mismatch
    */
    @Override
    public boolean equals(Object o)
        {
        if (o == this)
            {
            return true;
            }

        String sCNameThis = getCanonicalName();

        if (sCNameThis != null && o instanceof ValueExtractor)
            {
            ValueExtractor extractorThat = (ValueExtractor) o;
            String            sCNameThat = extractorThat.getCanonicalName();

            return extractorThat.getTarget() == getTarget() && Base.equals(sCNameThis, sCNameThat);
            }

        return false;
        }

    /**
    * HashCode value is hashCode of non-null
    * {@link ValueExtractor#getCanonicalName() canonical name};
    * otherwise, it is the identity hashCode value.
    * <p>
    * Subclass computes hashCode when canonical name is {@code null}.
    *
    * @return hashCode when canonical name set.
    */
    @Override
    public int hashCode()
        {
        String sCName = getCanonicalName();

        return sCName == null ? super.hashCode() : sCName.hashCode();
        }

    // ----- Comparator interface -------------------------------------------

    /**
    * Compares its two arguments for order.  Returns a negative integer,
    * zero, or a positive integer as the first argument is less than, equal
    * to, or greater than the second.
    *
    * @param o1  the first object to be compared
    * @param o2  the second object to be compared
    *
    * @return a negative integer, zero, or a positive integer as the first
    *         argument is less than, equal to, or greater than the second
    *
    * @throws ClassCastException if the arguments' types prevent them from
    *         being compared by this Comparator.
    */
    public int compare(Object o1, Object o2)
        {
        return SafeComparator.compareSafe(null, extract((T) o1), extract((T) o2));
        }


    // ----- QueryMapComparator interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public int compareEntries(QueryMap.Entry entry1, QueryMap.Entry entry2)
        {
        return SafeComparator.compareSafe(null,
                entry1.extract(this), entry2.extract(this));
        }

    // ----- subclassing support --------------------------------------------

    /**
    * Extract the value from the passed Entry object. The returned value should
    * follow the conventions outlined in the {@link #extract} method. By
    * overriding this method, an extractor implementation is able to extract a
    * desired value using all available information on the corresponding
    * Map.Entry object and is intended to be used in advanced custom scenarios,
    * when application code needs to look at both key and value at the same time
    * or can make some very specific assumptions regarding to the implementation
    * details of the underlying Entry object.
    *
    * @param entry  an Entry object to extract a desired value from
    *
    * @return the extracted value
    *
    * @since Coherence 3.5
    */
    public E extractFromEntry(Map.Entry entry)
        {
        return extract((T) (m_nTarget == VALUE ? entry.getValue() : entry.getKey()));
        }

    /**
    * Extract the value from the "original value" of the passed Entry object
    * or the key (if targeted). This method's conventions are exactly the same
    * as for the {@link #extractFromEntry} method.
    *
    * @param entry  an Entry object whose original value should be used to
    *               extract the desired value from
    *
    * @return the extracted value or null if the original value is not present
    *
    * @since Coherence 3.6
    */
    public E extractOriginalFromEntry(MapTrigger.Entry entry)
        {
        return m_nTarget == KEY
            ? extract((T) entry.getKey()) :
          entry.isOriginalPresent()
            ? extract((T) entry.getOriginalValue()) : null;
        }

    // ----- helper methods -------------------------------------------------

    /**
    * Return true if either this or {@code oValue} have a non-null
    * {@link #getCanonicalName() canonical name}.
    * <p>
    * This is a precondition for solely relying on {@link #equals(Object)}
    * to compute equivalence in subclass implementations. Since {@link #hashCode()}
    * is computed differently when canonical name exists,
    * implementation specific equivalence can only be used when both
    * instances have a null canonical name; otherwise,
    * the object {@link #equals(Object)}/{@link #hashCode()}
    * contract would be violated.
    *
    * @param oValue an object
    *
    * @return Return {@code true} if either this or {@code oValue} have a non-null
    *         {@link #getCanonicalName() canonical name}.
    *
    * @since 12.2.1.4
    */
    protected boolean isCanonicallyEquatable(Object oValue)
        {
        return getCanonicalName() != null ||
               (oValue instanceof ValueExtractor &&
                ((ValueExtractor) oValue).getCanonicalName() != null);
        }

    // ----- fields and constants --------------------------------------------

    /**
    * Indicates that the {@link #extractFromEntry} operation should use the
    * Entry's value.
    *
    * @since Coherence 3.5
    */
    public static final int VALUE = 0;

    /**
    * Indicates that the {@link #extractFromEntry} operation should use the
    * Entry's key.
    *
    * @since Coherence 3.5
    */
    public static final int KEY   = 1;

    /**
    * Specifies which part of the entry should be used by the
    * {@link #extractFromEntry} operation. Legal values are {@link #VALUE}
    * (default) or {@link #KEY}.
    * <p>
    * <b>Note:</b> subclasses are responsible for initialization and POF and/or
    * Lite serialization of this field.
    *
    * @since Coherence 3.5
    */
    @JsonbProperty("target")
    protected int m_nTarget;

    /**
    * Canonical name for this extractor.
    * <p>
    * <b>Note:</b> subclasses are responsible for initialization and POF and/or
    * Lite serialization of this field.
    *
    * @since 12.2.1.4
    */
    protected transient String m_sNameCanon = null;
    }
