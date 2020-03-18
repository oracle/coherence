/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.processor;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;


/**
* ExtractorProcessor is an EntryProcessor implementations that extracts a
* value from an object cached in an InvocableMap. A common usage pattern is:
* <pre>
*   cache.invoke(oKey, new ExtractorProcessor(extractor));
* </pre>
* which is functionally equivalent to the following operation:
* <pre>
*   extractor.extract(cache.get(oKey));
* </pre>
* The major difference is that for clustered caches using the
* ExtractorProcessor could significantly reduce the amount of network
* traffic.
* <p>
* An alternative (and superior) approach would be to use the
* {@link com.tangosol.util.aggregator.ReducerAggregator ReducerAggregator}
*
* @author gg 2005.11.30
*/
public class ExtractorProcessor<K, V, T, E>
        extends    AbstractProcessor<K, V, E>
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public ExtractorProcessor()
        {
        }

    /**
    * Construct an ExtractorProcessor based on the specified ValueExtractor.
    *
    * @param extractor  a ValueExtractor object; passing null is equivalent
    *                    to using the {@link IdentityExtractor}
    */
    public ExtractorProcessor(ValueExtractor<? super T, ? extends E> extractor)
        {
        m_extractor = extractor == null ? IdentityExtractor.INSTANCE : extractor;
        }

    /**
    * Construct an ExtractorProcessor for a given method name.
    *
    * @param sMethod  a method name to make a {@link ReflectionExtractor}
    *                 for; this parameter can also be a dot-delimited
    *                 sequence of method names which would result in an
    *                 ExtractorProcessor based on the {@link
    *                 ChainedExtractor} that is based on an array of
    *                 corresponding ReflectionExtractor objects
    */
    public ExtractorProcessor(String sMethod)
        {
        m_extractor =
            sMethod == null || sMethod.length() == 0 ?
                IdentityExtractor.INSTANCE :
            sMethod.indexOf('.') < 0 ?
                new ReflectionExtractor(sMethod) :
                new ChainedExtractor(sMethod);
        }


    // ----- EntryProcessor interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public E process(InvocableMap.Entry<K, V> entry)
        {
        return entry.extract(m_extractor);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the ExtractorProcessor with another object to determine
    * equality.
    *
    * @return true iff this ExtractorProcessor and the passed object are
    *         equivalent ExtractorProcessor objects
    */
    public boolean equals(Object o)
        {
        if (o instanceof ExtractorProcessor)
            {
            ExtractorProcessor that = (ExtractorProcessor) o;
            return equals(this.m_extractor, that.m_extractor);
            }

        return false;
        }

    /**
    * Determine a hash value for the ExtractorProcessor object according to
    * the general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this ExtractorProcessor object
    */
    public int hashCode()
        {
        return m_extractor.hashCode();
        }

    /**
    * Return a human-readable description for this ExtractorProcessor.
    *
    * @return a String description of the ExtractorProcessor
    */
    public String toString()
        {
        return "ExtractorProcessor(" + m_extractor + ')';
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_extractor = (ValueExtractor) ExternalizableHelper.readObject(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeObject(out, m_extractor);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_extractor = (ValueExtractor) in.readObject(0);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_extractor);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The underlying value extractor.
    */
    @JsonbProperty("extractor")
    protected ValueExtractor<? super T, ? extends E> m_extractor;
    }