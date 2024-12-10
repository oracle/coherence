/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.extractor;


import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.Extractors;
import com.tangosol.util.ValueExtractor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;


/**
* The KeyExtractor is a special purpose ValueExtractor implementation that
* serves as an indicator that a query should be run against the key objects
* rather than the values. The major difference between the KeyExtractor and a
* standard {@link ReflectionExtractor} is that when used in various
* {@link com.tangosol.util.filter.EntryFilter} implementations it forces
* the evaluation of entry keys rather than entry values.
* <p>
* For example, consider a key object that consists of two properties:
* "FirstName" and "LastName". To retrieve all keys that have a value of the
* "LastName" property equal to "Smith", the following query could be used:
* <pre>
* ValueExtractor extractor = new KeyExtractor("getLastName");
* Set setKeys = cache.keySet(new EqualsFilter(extractor, "Smith"));
* </pre>
*
* As of Coherence 3.5, the same effect can be achieved for subclasses of the
* AbstractExtractor, for example:
* <pre>
* ValueExtractor extractor = new ReflectionExtractor("getLastName", null,
*      AbstractExtractor.KEY);
* Set setKeys = cache.keySet(new EqualsFilter(extractor, "Smith"));
* </pre>
*
* <b>Note:</b> This class does not function with
* {@link com.tangosol.util.extractor.PofExtractor} and the recommendation is
* to create a {@link PofExtractor} with the target set to {@link #KEY} or
* create a {@link PofExtractor} and call the {@link #fromKey()} method on it.
*
* @author gg 2006.06.12
* @since Coherence 3.2
*/
@SuppressWarnings({"unchecked", "DataFlowIssue"})
public class KeyExtractor<T, E>
        extends AbstractExtractor<T, E>
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite
    * and PortableObject interfaces).
    */
    public KeyExtractor()
        {
        // this forces the m_nTarget to be initialized as KEY
        this((ValueExtractor<T, E>) null);
        }

    /**
    * Construct a KeyExtractor based on a specified ValueExtractor.
    *
    * @param extractor  the underlying ValueExtractor
    */
    public KeyExtractor(ValueExtractor<? super T, ? extends E> extractor)
        {
        m_nTarget   = KEY;
        m_extractor = extractor == null
                        ? IdentityExtractor.INSTANCE
                        : Lambdas.ensureRemotable(extractor);
        }

    /**
    * Construct a KeyExtractor for a specified method name.
    *
    * @param sMethod  a method name to construct an underlying
    *                 {@link ReflectionExtractor} for;
    *                 this parameter can also be a dot-delimited sequence
    *                 of method names which would result in a KeyExtractor
    *                 based on the {@link ChainedExtractor} that is based on
    *                 an array of corresponding ReflectionExtractor objects
    *
    * @deprecated use {@link Extractors#key(String...)} instead.
    */
    @SuppressWarnings("rawtypes")
    public KeyExtractor(String sMethod)
        {
        azzert(sMethod != null, "Method name is missing");

        m_nTarget   = KEY;
        m_extractor = sMethod.indexOf('.') < 0 ? (ValueExtractor)
            new ReflectionExtractor(sMethod) :
            new ChainedExtractor(sMethod);
        }


    // ----- ValueExtractor interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public E extract(T oTarget)
        {
        return m_extractor.extract(oTarget);
        }

    // ----- CanonicallyNamed interface -------------------------------------

    @Override
    public String getCanonicalName()
        {
        if (m_sNameCanon == null)
            {
            // optimization in AbstractExtractor#equals(Object) requires
            // AbstractExtractor.m_sNameCanon to be set.
            m_sNameCanon = m_extractor.getCanonicalName();
            }
        return m_sNameCanon;
        }

    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the underlying ValueExtractor.
    *
    * @return the ValueExtractor
    */
    public ValueExtractor<? super T, ? extends E> getExtractor()
        {
        return m_extractor;
        }


    // ----- helper methods -------------------------------------------------

    /**
    * Factory method for key extractor.
    *
    * @param <T>        the type of the value to extract from
    * @param <E>        the type of value that will be extracted
    * @param extractor  an extractor to convert to key extractor
    *
    * @return key extractor based on the specified extractor
    */
    public static <T, E> ValueExtractor<T, E> of(ValueExtractor<T, E> extractor)
        {
        return extractor instanceof KeyExtractor ? extractor : new KeyExtractor<>(extractor);
        }

    // ----- Object methods -------------------------------------------------

    @SuppressWarnings("rawtypes")
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

        if (o instanceof KeyExtractor &&
            m_extractor instanceof AbstractExtractor)
            {
            KeyExtractor that = (KeyExtractor) o;
            return Base.equals(this.m_extractor, that.m_extractor);
            }

        return false;
        }

    /**
    * Compute hashCode from underlying {@link ValueExtractor ValueExtractor}.
    *
    * @return an integer hash value for this KeyExtractor object
    */
    @Override
    public int hashCode()
        {
        return m_extractor.hashCode();
        }

    /**
    * Return a human-readable description for this KeyExtractor.
    *
    * @return a String description of the KeyExtractor
    */
    @Override
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass()) +
            "(extractor=" + m_extractor + ')';
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_extractor = readObject(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        writeObject(out, m_extractor);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_extractor = in.readObject(0);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_extractor);
        }


    // ----- data fields ----------------------------------------------------

    /**
    * The underlying ValueExtractor.
    */
    @JsonbProperty("extractor")
    protected ValueExtractor<? super T, ? extends E> m_extractor;
    }
