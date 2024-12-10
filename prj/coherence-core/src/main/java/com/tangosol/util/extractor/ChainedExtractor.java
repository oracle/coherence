/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.extractor;


import com.tangosol.io.pof.PofReader;

import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.MapTrigger;
import com.tangosol.util.ValueExtractor;

import java.io.DataInput;
import java.io.IOException;

import java.util.Map;
import java.util.Objects;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;


/**
* Composite ValueExtractor implementation based on an array of extractors.
* The extractors in the array are applied sequentially left-to-right, so a
* result of a previous extractor serves as a target object for a next one.
*
* @author gg 2003.09.22
*/
public class ChainedExtractor<T, E>
        extends AbstractCompositeExtractor<T, E>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public ChainedExtractor()
        {
        }

    /**
    * Construct a ChainedExtractor based on a specified ValueExtractor array.
    *
    * @param aExtractor  the ValueExtractor array
    */
    @JsonbCreator
    public ChainedExtractor(@JsonbProperty("extractors") ValueExtractor[] aExtractor)
        {
        super(aExtractor);
        m_nTarget = computeTarget();
        }

    /**
    * Construct a ChainedExtractor based on two extractors.
    *
    * @param <U>         the type of value that will be extracted in the first ValueExtractor
    * @param extractor1  the first ValueExtractor
    * @param extractor2  the second ValueExtractor
    */
    public <U> ChainedExtractor(ValueExtractor<? super T, ? extends U> extractor1,
                                ValueExtractor<? super U, ? extends E> extractor2)
        {
        super(new ValueExtractor[] {ValueExtractor.of(extractor1) , ValueExtractor.of(extractor2)});
        m_nTarget = computeTarget();
        }

    /**
    * Construct a ChainedExtractor for a specified method name sequence.
    *
    * @param sName  a dot-delimited sequence of method names which results
    *                 in a ChainedExtractor that is based on an array of
    *                 corresponding {@link ReflectionExtractor} objects
    *
    * @deprecated use {@link com.tangosol.util.Extractors#chained(String...)}
    */
    public ChainedExtractor(String sName)
        {
        super(createExtractors(sName));
        m_nTarget = computeTarget();
        }

    // ----- ChainedExtractor methods ---------------------------------------

    /**
     * Ensure that this target is correct after first extractor manually updated.
     */
    public void ensureTarget()
        {
        m_nTarget = computeTarget();
        }

    // ----- AbstractExtractor methods --------------------------------------

    /**
    * Extract the value from the passed object. The underlying extractors are
    * applied sequentially, so a result of a previous extractor serves as a
    * target object for a next one. A value of null prevents any further
    * extractions and is returned immediately. For intrinsic types, the
    * returned value is expected to be a standard wrapper type in the same
    * manner that reflection works; for example, int would be returned as a
    * java.lang.Integer.
    */
    public E extract(Object oTarget)
        {
        ValueExtractor[] aExtractor = getExtractors();
        for (int i = 0, c = aExtractor.length; i < c && oTarget != null; i++)
            {
            oTarget = aExtractor[i].extract(oTarget);
            }

        return (E) oTarget;
        }

    /**
    * Extract the value from the passed entry. The underlying extractors are
    * applied sequentially, so a result of a previous extractor serves as a
    * target object for a next one. A value of null prevents any further
    * extractions and is returned immediately. For intrinsic types, the
    * returned value is expected to be a standard wrapper type in the same
    * manner that reflection works; for example, int would be returned as a
    * java.lang.Integer.
    */
    public E extractFromEntry(Map.Entry entry)
        {
        ValueExtractor[] aExtractor = getExtractors();
        Object           oTarget    =
            InvocableMapHelper.extractFromEntry(aExtractor[0], entry);

        for (int i = 1, c = aExtractor.length; i < c && oTarget != null; i++)
            {
            oTarget = aExtractor[i].extract(oTarget);
            }

        return (E) oTarget;
        }

    /*
    * Analogous to the {@link #extractFromEntry} method, extract the value from
    * the "original value" of the passed Entry object.
    */
    public E extractOriginalFromEntry(MapTrigger.Entry entry)
        {
        ValueExtractor[] aExtractor = getExtractors();
        Object           oTarget    =
            InvocableMapHelper.extractOriginalFromEntry(aExtractor[0], entry);

        for (int i = 1, c = aExtractor.length; i < c && oTarget != null; i++)
            {
            oTarget = aExtractor[i].extract(oTarget);
            }

        return (E) oTarget;
        }

    @Override
    @SuppressWarnings("unchecked")
    public <V> ValueExtractor<V, E> compose(ValueExtractor<? super V, ? extends T> before)
        {
        Objects.requireNonNull(before);

        ValueExtractor[] aBefore = before instanceof ChainedExtractor
            ? ((ChainedExtractor) before).getExtractors()
            : new ValueExtractor[] {before};

        // invalidate cached canonical name since this extractor composition is changing.
        // recompute target since first extractor in chain changed.
        m_sNameCanon = null;
        m_aExtractor = merge(aBefore, m_aExtractor);
        m_nTarget    = computeTarget();

        return (ValueExtractor<V, E>) this;
        }

    @Override
    @SuppressWarnings("unchecked")
    public <V> ValueExtractor<T, V> andThen(ValueExtractor<? super E, ? extends V> after)
        {
        Objects.requireNonNull(after);

        ValueExtractor[] aAfter = after instanceof ChainedExtractor
            ? ((ChainedExtractor) after).getExtractors()
            : new ValueExtractor[] {after};

        // invalidate cached canonical name since this extractor composition is changing.
        m_sNameCanon = null;
        m_aExtractor = merge(m_aExtractor, aAfter);

        return (ValueExtractor<T, V>) this;
        }

    /**
    * Compute a canonical name as a dot-separated concatenation of
    * the canonical name of each {@link ValueExtractor ValueExtractor} array element, starting
    * from lowest index array element.
    *
    * @return canonical name reflecting this instance's array of {@link ValueExtractor ValueExtractor}s.
    */
    @Override
    public String getCanonicalName()
        {
        String sCName = super.getCanonicalName();

        if (sCName == null)
            {
            StringBuilder    sb         = null;
            ValueExtractor[] aExtractor = m_aExtractor;

            for (ValueExtractor extractor : aExtractor)
                {
                String sCNCur = extractor.getCanonicalName();

                if (sCNCur == null)
                    {
                    // if any of the extractor's canonical names are null,
                    // the composite extractor's canonical name is null.
                    return null;
                    }
                sb = sb == null ? new StringBuilder(sCNCur) : sb.append('.').append(sCNCur);
                }
            sCName = m_sNameCanon = sb == null ? null : sb.toString();
            }
        return sCName;
        }

    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        super.readExternal(in);
        m_nTarget = computeTarget();
        }

    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        super.readExternal(in);
        m_nTarget = computeTarget();
        }

    // ----- helpers --------------------------------------------------------

    /**
    * Parse a dot-delimited sequence of method names and instantiate
    * a corresponding array of {@link ReflectionExtractor} objects.
    *
    * @param sName  a dot-delimited sequence of method names
    *
    * @return an array of {@link ReflectionExtractor} objects
    *
    * @deprecated {@link com.tangosol.util.Extractors#chained(String...)} which uses {@link UniversalExtractor}
    */
    public static ValueExtractor[] createExtractors(String sName)
        {
        String[]              asMethod   = parseDelimitedString(sName, '.');
        int                   cMethods   = asMethod.length;
        ReflectionExtractor[] aExtractor = new ReflectionExtractor[cMethods];
        for (int i = 0; i < cMethods; i++)
            {
            aExtractor[i] = new ReflectionExtractor(asMethod[i]);
            }
        return aExtractor;
        }

    /**
    * Return a {@link ValueExtractor} array with the provided arrays merged
    * into a single array.
    *
    * @param aHead  the first group of elements in the returned array
    * @param aTail  the second group of elements in the returned array
    *
    * @return a ValueExtractor array with the provided arrays merged
    *         into a single array
    */
    protected static ValueExtractor[] merge(ValueExtractor[] aHead, ValueExtractor[] aTail)
        {
        int cHead = aHead.length;
        int cTail = aTail.length;

        ValueExtractor[] aExtractorsNew = new ValueExtractor[cHead + cTail];

        System.arraycopy(aHead, 0, aExtractorsNew, 0, cHead);
        System.arraycopy(aTail, 0, aExtractorsNew, cHead, cTail);

        return aExtractorsNew;
        }

    /**
    * Return the target of the first extractor in composite extractor.
    * <p>
    * Enables equivalence between KeyExtractor("foo.bar") and
    * ChainExtractor(ReflectionExtractor("foo", null, KEY), ReflectionExtractor("bar")).
    *
    * @return the target of the first extractor in CompositeExtractor.
    *
    * @since 12.2.1.4
    */
    protected int computeTarget()
        {
        ValueExtractor[] aExtractor = m_aExtractor;

        return (aExtractor != null && aExtractor.length > 0 && aExtractor[0] instanceof AbstractExtractor) ?
               aExtractor[0].getTarget() : AbstractExtractor.VALUE;
        }
    }
