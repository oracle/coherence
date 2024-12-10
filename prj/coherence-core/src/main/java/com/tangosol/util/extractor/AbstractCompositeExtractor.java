/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.extractor;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ValueExtractor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;


/**
* Abstract super class for ValueExtractor implementations that are based on
* an underlying array of ValueExtractor objects.
*
* @author gg 2006.02.08
* @since Coherence 3.2
*/
public abstract class AbstractCompositeExtractor<T, E>
        extends    AbstractExtractor<T, E>
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor
    */
    public AbstractCompositeExtractor()
        {
        }

    /**
    * Construct a AbstractCompositeExtractor based on the specified
    * ValueExtractor array.
    *
    * @param aExtractor  the ValueExtractor array
    */
    public AbstractCompositeExtractor(ValueExtractor[] aExtractor)
        {
        azzert(aExtractor != null);
        m_aExtractor = aExtractor;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the ValueExtractor array.
    *
    * @return the ValueExtractor array
    */
    public ValueExtractor[] getExtractors()
        {
        return m_aExtractor;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the AbstractCompositeExtractor with another object to determine
    * equality. If either instances have a canonical name, equality is determined
    * by {@link AbstractExtractor#equals(Object)}. Otherwise, fallback to
    * legacy equality where two AbstractCompositeExtractor objects are considered equal
    * iff they belong to the same class and their underlying ValueExtractor
    * arrays are {@link Base#equalsDeep deep-equal}.
    *
    * @return true iff this AbstractCompositeExtractor and the passed object
    *         are equivalent
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

        if (o instanceof AbstractCompositeExtractor)
            {
            AbstractCompositeExtractor that = (AbstractCompositeExtractor) o;
            return Base.equals(this.getClass(), that.getClass()) &&
                   Base.equalsDeep(this.m_aExtractor, that.m_aExtractor);
            }

        return false;
        }

    /**
    * Compute a hash value for the AbstractCompositeExtractor object
    * If {@link #getCanonicalName() canonical name} is non-null, return hashcode
    * of canonical name string; otherwise, compute the hashcode as a sum of all
    * {@link ValueExtractor#hashCode() hashCodes}.
    *
    * @return an integer hash value for this ValueExtractor object
    */
    @Override
    public int hashCode()
        {
        String sCName = getCanonicalName();

        if (sCName == null)
            {
            int iHash = 0;
            ValueExtractor[] aExtractor = m_aExtractor;

            for (int i = 0, c = aExtractor.length; i < c; i++)
                {
                iHash += aExtractor[i].hashCode();
                }
            return iHash;
            }
        return sCName.hashCode();
        }

    /**
    * Return a human-readable description for this ValueExtractor.
    *
    * @return a String description of the ValueExtractor
    */
    @Override
    public String toString()
        {
        StringBuilder sb = new StringBuilder();
        sb.append(ClassHelper.getSimpleName(getClass()))
          .append('(');

        ValueExtractor[] aExtractor = m_aExtractor;
        for (int i = 0, c = aExtractor.length; i < c; i++)
            {
            if (i > 0)
                {
                sb.append(", ");
                }
            sb.append(aExtractor[i]);
            }
        sb.append(')');

        return sb.toString();
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        int cExtractors = readInt(in);
        azzert(cExtractors < 16384, "Unexpected number of composite extractors");

        ValueExtractor[] aExtractor  = new ValueExtractor[cExtractors];

        for (int i = 0; i < cExtractors; i++)
            {
            aExtractor[i] = readObject(in);
            }
        m_aExtractor = aExtractor;
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ValueExtractor[] aExtractor  = m_aExtractor;
        int              cExtractors = aExtractor.length;

        writeInt(out, cExtractors);
        for (int i = 0; i < cExtractors; i++)
            {
            writeObject(out, aExtractor[i]);
            }
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_aExtractor = (ValueExtractor[]) in.readObjectArray(0, EMPTY_EXTRACTOR_ARRAY);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObjectArray(0, m_aExtractor);
        }


    // ----- constants ------------------------------------------------------

    /**
    * Empty array of ValueExtractor objects.
    */
    private static final ValueExtractor[] EMPTY_EXTRACTOR_ARRAY = new ValueExtractor[0];


    // ----- data members ---------------------------------------------------

    /**
    * The ValueExtractor array.
    */
    @JsonbProperty("extractors")
    protected ValueExtractor[] m_aExtractor;
    }