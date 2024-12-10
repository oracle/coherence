/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.extractor;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


/**
* Trivial ValueExtractor implementation that does not actually extract
* anything from the passed value, but returns the value itself.
*
* @author jh/gg 2006.03.26
* @since Coherence 3.2
*/
public class IdentityExtractor<T>
        extends    AbstractExtractor<T, T>
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    * <p>
    * To obtain an instance of an IdentityExtractor use the
    * {@link IdentityExtractor#INSTANCE IdentityExtractor.INSTANCE} constant.
    */
    public IdentityExtractor()
        {
        }

    // ----- ValueExtractor interface ---------------------------------------

    /**
    * Simply return the passed object.
    */
    public T extract(T target)
        {
        return target;
        }

    // ----- CanonicallyNamed interface -------------------------------------

    /**
    * @return {@link IdentityExtractor#CANONICAL_NAME canonical name}.
    */
    @Override
    public String getCanonicalName()
        {
        return CANONICAL_NAME;
        }

    // ----- Object methods -------------------------------------------------

    /**
    * Compare the IdentityExtractor with another object to determine
    * equality.
    *
    * @return true iff the passed object is an IdentityExtractor
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

        return o instanceof IdentityExtractor;
        }

    /**
    * Return the hashCode of {@link #getCanonicalName() canonical name}.
    *
    * @return an integer hash value for this {@code IdentityExtractor} object
    */
    @Override
    public int hashCode()
        {
        return getCanonicalName().hashCode();
        }

    /**
    * Provide a human-readable description of this IdentityExtractor object.
    *
    * @return a human-readable description of this IdentityExtractor object
    */
    @Override
    public String toString()
        {
        return "IdentityExtractor";
        }

    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        }

    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        }

    // ---- constants -------------------------------------------------------

    /**
    * Canonical name for identity extractor.
    *
    * @since 12.2.1.4
    */
    public static final String CANONICAL_NAME = "{id}";

    /**
    * An instance of the IdentityExtractor.
    */
    public static final IdentityExtractor INSTANCE = new IdentityExtractor();

    /**
    * Return an instance of the IdentityExtractor.
    *
    * @param <T>  the type of the value to extract
    *
    * @return a IdentityExtractor
    */
    public static <T> IdentityExtractor<T> INSTANCE()
        {
        return INSTANCE;
        }
    }
