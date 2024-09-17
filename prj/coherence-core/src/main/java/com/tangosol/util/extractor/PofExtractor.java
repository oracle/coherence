/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.extractor;


import com.tangosol.io.pof.PofContext;
import com.tangosol.io.pof.PofHelper;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.io.pof.PofConstants;

import com.tangosol.io.pof.reflect.PofNavigator;
import com.tangosol.io.pof.reflect.PofValue;
import com.tangosol.io.pof.reflect.PofValueParser;
import com.tangosol.io.pof.reflect.SimplePofPath;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.MapTrigger;
import com.tangosol.util.ValueExtractor;

import java.io.IOException;
import java.io.NotActiveException;

import java.util.Map;


/**
* POF-based ValueExtractor implementation.
* PofExtractor takes advantage of POF's indexed state to extract part of an
* object without needing to deserialize the entire object.
* <p>
* POF uses a compact form in the serialized value when possible. For example,
* some numeric values are represented as special POF intrinsic types in which
* the type implies the value. As a result, POF requires the receiver of a
* value to have implicit knowledge of the type. PofExtractor uses the class
* supplied in the constructor as the source of the type information. If the
* class is null, PofExtractor will infer the type from the serialized state.
* <p>
* Example where extracted value is double:
* <pre>
*     PofExtractor extractor = new PofExtractor(double.class, 3);
* </pre>
*
* Example where extracted value should be inferred:
* <pre>
*     PofExtractor extractor = new PofExtractor(null, 4);
* </pre>
*
* @author as  2009.02.14
* @since Coherence 3.5
*/
public class PofExtractor<T, E>
        extends    AbstractExtractor<T, E>
        implements PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the PortableObject interface).
    */
    public PofExtractor()
        {
        m_nType = PofConstants.T_UNKNOWN;
        }

    /**
    * Constructs a PofExtractor based on a property index.
    * <p>
    * This constructor is equivalent to:
    * <pre>
    *   PofExtractor extractor =
    *       new PofExtractor(clz, new SimplePofPath(iProp), VALUE);
    * </pre>
    *
    * @param clz    the required class of the extracted value or null if
    *               the class is to be inferred from the serialized state
    * @param iProp  property index
    */
    public PofExtractor(Class<E> clz, int iProp)
        {
        this(clz, new SimplePofPath(iProp), VALUE);
        }

    /**
    * Constructs a PofExtractor based on a property index while providing the
    * property's canonical name.
    *
    * Providing an appropriate canonical name enables equivalence with other
    * {@link com.tangosol.util.ValueExtractor} instances with same canonical name.
    * See {@link com.tangosol.util.ValueExtractor#equals(Object)} and
    * {@link com.tangosol.util.ValueExtractor#getCanonicalName()}.
    *
    * @param clz        the required class of the extracted value or null if
    *                   the class is to be inferred from the serialized state
    * @param iProp      property index
    * @param sNameCanon the canonical name for this extractor
    *
    * @since 12.2.1.4
    */
    public PofExtractor(Class<E> clz, int iProp, String sNameCanon)
        {
        this(clz, new SimplePofPath(iProp), VALUE);

        m_sNameCanon = sNameCanon;
        }

    /**
    * Constructs a PofExtractor based on a POF navigator.
    * <p>
    * This constructor is equivalent to:
    * <pre>
    *   PofExtractor extractor =
    *       new PofExtractor(clz, navigator, VALUE);
    * </pre>
    *
    * @param clz        the required class of the extracted value or null if
    *                   the class is to be inferred from the serialized state
    * @param navigator  POF navigator
    */
    public PofExtractor(Class<E> clz, PofNavigator navigator)
        {
        this(clz, navigator, VALUE);
        }

    /**
    * Constructs a PofExtractor based on a POF navigator and the entry
    * extraction target.
    *
    * @param clz        the required class of the extracted value or null if
    *                   the class is to be inferred from the serialized state
    * @param navigator  POF navigator
    * @param nTarget    one of the {@link #VALUE} or {@link #KEY} values
    */
    public PofExtractor(Class<E> clz, PofNavigator navigator, int nTarget)
        {
        azzert(navigator != null, "Navigator must not be null.");

        m_clz       = clz;
        m_navigator = navigator;
        m_nTarget   = nTarget;
        if (clz == null)
            {
            m_nType = PofConstants.T_UNKNOWN;
            }
        }

    /**
    * Constructs a PofExtractor based on a POF navigator while providing
    * its canonical name.
    *
    * @param clz         the required class of the extracted value or {@code null} if
    *                    the class is to be inferred from the serialized state
    * @param navigator   POF navigator
    * @param nTarget     one of the {@link #VALUE} or {@link #KEY} values
    * @param sNameCanon  {@link com.tangosol.util.ValueExtractor#getCanonicalName() canonical name} for this extractor
    *
    * @since 12.2.1.4
    */
    public PofExtractor(Class<E> clz, PofNavigator navigator, int nTarget, String sNameCanon)
        {
        this(clz, navigator, nTarget);
        m_sNameCanon = sNameCanon;
        }

    /**
    * Constructs a VALUE PofExtractor based on a POF navigator while providing
    * its canonical name.
    *
    * @param clz        the required class of the extracted value or null if
    *                   the class is to be inferred from the serialized state
    * @param navigator  POF navigator
    * @param sNameCanon {@link com.tangosol.util.ValueExtractor#getCanonicalName() canonical name} for this extractor
    *
    * @since 12.2.1.4
    */
    public PofExtractor(Class<E> clz, PofNavigator navigator, String sNameCanon)
        {
        this(clz, navigator, VALUE, sNameCanon);
        }

    // ----- AbstractExtractor methods --------------------------------------

    /**
    * Extracts the value from the passed Entry object.
    * <p>
    * It is expected that this extractor will only be used against POF-encoded
    * entries implementing {@link BinaryEntry} interface.
    *
    * @param entry  an Entry object to extract a value from
    *
    * @return the extracted value
    *
    * @throws UnsupportedOperationException if the specified Entry is not
    *         a POF-encoded {@link BinaryEntry} or the serializer is not
    *         a PofContext
    * @throws ClassCastException if the extracted value is incompatible with
    *         the specified class
    */
    public E extractFromEntry(Map.Entry entry)
        {
        return extractInternal(entry, m_nTarget);
        }

    /*
    * Analogous to the {@link #extractFromEntry} method, extract the value from
    * the "original value" of the passed Entry object.
    *
    * @param entry  an Entry object to extract the original value from
    *
    * @return the extracted original value
    */
    public E extractOriginalFromEntry(MapTrigger.Entry entry)
        {
        return extractInternal(entry, m_nTarget == KEY ? KEY : -1);
        }

    /**
    * Implementation of the extract* methods.
    */
    private E extractInternal(Map.Entry entry, int nTarget)
        {
        BinaryEntry binEntry;
        PofContext  ctx;
        try
            {
            binEntry = (BinaryEntry) entry;
            ctx      = (PofContext) binEntry.getSerializer();
            }
        catch (ClassCastException cce)
            {
            String sReason = entry instanceof BinaryEntry
                    ? "the configured Serializer is not a PofContext"
                    : "the Map Entry is not a BinaryEntry";
            throw new UnsupportedOperationException(
                    "PofExtractor must be used with POF-encoded Binary entries; "
                    + sReason);
            }

        Binary binTarget;
        switch (nTarget)
            {
            default:
            case VALUE:
                binTarget = binEntry.getBinaryValue();
                break;
            case KEY:
                binTarget = binEntry.getBinaryKey();
                break;
            case -1: // internal target type; see extractOriginalFromEntry()
                binTarget = binEntry.getOriginalBinaryValue();
                break;
            }

        if (binTarget == null)
            {
            return null;
            }

        PofValue valueRoot   = PofValueParser.parse(binTarget, ctx);
        PofValue valueTarget = m_navigator.navigate(valueRoot);

        // be tolerant to a missing target (similar to ReflectionExtractor)
        return (E) (valueTarget == null ? null : valueTarget.getValue(getPofTypeId(ctx)));
        }

    @Override
    public ValueExtractor<T, E> fromKey()
        {
        return new PofExtractor<>(m_clz, m_navigator, KEY, m_sNameCanon);
        }

    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the POF navigator for this extractor.
    *
    * @return the POF navigator
    */
    public PofNavigator getNavigator()
        {
        return m_navigator;
        }

    /**
    * Obtain the Class of the extracted value.
    *
    * @return the expected Class
    */
    public Class<E> getClassExtracted()
        {
        return m_clz;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the PofExtractor with another object to determine equality.
    * <p>
    * {@link AbstractExtractor#equals(Object)} contract takes precedence when applicable,
    * falling back to implementation specific equals when this and {@code o}
    * have non-null canonical name.
    * <p>
    * Two PofExtractor objects are considered equal iff their navigators are
    * equal and they have the same target (key or value).
    *
    * @return true iff this PofExtractor and the passed object are equivalent
    */
    @Override
    public boolean equals(Object o)
        {
        // the super.equals() uses the canonical name comparison (if applies);
        // if that succeeds, no other checks are to be made.
        if (super.equals(o))
            {
            return true;
            }
        else if (isCanonicallyEquatable(o))
            {
            return false;
            }

        if (o instanceof PofExtractor)
            {
            PofExtractor that = (PofExtractor) o;
            return this.m_nTarget == that.m_nTarget
                   && Base.equals(this.m_navigator, that.m_navigator);
            }
        return false;
        }

    /**
    * Return the hashCode of a non-null {@link #getCanonicalName() canonical name};
    * otherwise, the hash code is {@link SimplePofPath#hashCode() PofNavigator.hashCode() implementation}.
    *
    * @return an integer hash value for this PofExtractor object
    *
    * @see com.tangosol.util.ValueExtractor#hashCode()
    */
    @Override
    public int hashCode()
        {
        String sCName = getCanonicalName();

        return sCName == null ? m_navigator.hashCode() + m_nTarget : sCName.hashCode();
        }

    /**
    * Return a human-readable description for this PofExtractor.
    *
    * @return a String description of the PofExtractor
    */
    @Override
    public String toString()
        {
        StringBuilder sb = new StringBuilder();

        sb.append(ClassHelper.getSimpleName(getClass()));
        sb.append("(target=").append(m_nTarget == VALUE ? "VALUE" : "KEY");
        sb.append(", navigator=").append(m_navigator);

        String sCName = getCanonicalName();
        if (sCName != null)
            {
            sb.append(", ValueExtractor(").append(sCName).append(')');
            }
        sb.append(')');
        return sb.toString();
        }

    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_nTarget   = in.readInt(0);
        m_navigator = (PofNavigator) in.readObject(1);

        // Note: we write out the TypeId offset by T_UNKNOWN to allow for pre
        // 3.5.2 backwards compatibility in the reader, i.e. the lack of this
        // property in the stream will result in T_UNKNOWN, and the old behavior
        // Note 2: this offset fix unfortunately could cause us to push the
        // written TypeId out of the legal int range.  To fix this we write
        // it as a long on the wire.
        m_nType = (int) (in.readLong(2) + PofConstants.T_UNKNOWN);
        m_clz   = null;
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        PofNavigator navigator = m_navigator;
        if (navigator == null)
            {
            throw new NotActiveException(
                    "PofExtractor was constructed without a navigator");
            }
        out.writeInt(0, m_nTarget);
        out.writeObject(1, navigator);
        // see note in readExternal regarding T_UNKNOWN offset
        out.writeLong(2, (long) getPofTypeId(out.getPofContext()) -
                (long) PofConstants.T_UNKNOWN);
        }


    // ----- helper methods -------------------------------------------------

    /**
    *  compute the expected pof type id based on the class.
    *
    * @param ctx  pof context
    *
    * @return pof type id or T_UNKNOWN if the class is null.
    */
    protected int getPofTypeId(PofContext ctx)
        {
        Class clz = m_clz;

        return clz == null ? m_nType : PofHelper.getPofTypeId(clz, ctx);
        }


    // ----- data members ---------------------------------------------------

    /**
    * POF navigator.
    */
    private PofNavigator m_navigator;

    /**
    * Class for what is being extracted; or null if this information is
    * specified in m_nType.
    */
    private transient Class<E> m_clz;

    /**
    * POF type for expected value.
    * This value is only meaningful when m_clz == null.
    */
    private int m_nType;
    }
