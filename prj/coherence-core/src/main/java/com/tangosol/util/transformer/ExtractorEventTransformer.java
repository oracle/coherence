/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.transformer;


import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapEventTransformer;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.MultiExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;


/**
* ExtractorEventTransformer is a special purpose {@link MapEventTransformer}
* implementation that transforms emitted events, extracting one or more
* properties from either the OldValue or the NewValue. This transformation
* will generally result in the change of the values' data type.
* <p>
* Example: the following code will register a listener to receive events only
* if the value of the AccountBalance property changes. The transformed event's
* NewValue will be a List containing the LastTransactionTime and AccountBalance
* properties. The OldValue will always be null.
* <pre>
*    Filter filter = new ValueChangeEventFilter("getAccountBalance");
*    ValueExtractor extractor = new MultiExtractor("getLastTransactionTime,getAccountBalance");
*    MapEventTransformer transformer = new ExtractorEventTransformer(null, extractor);
*
*    cache.addMapListener(listener, new MapEventTransformerFilter(filter, transformer), false);
* </pre>
*
* @author gg  2008.06.01
* @since Coherence 3.4
*/
public class ExtractorEventTransformer<K, V, E>
        extends ExternalizableHelper
        implements MapEventTransformer<K, V, E>, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * No-argument constructor for lite deserialization.
    */
    public ExtractorEventTransformer()
        {
        }

    /**
    * Construct a ExtractorEventTransformer that transforms MapEvent values
    * based on the specified extractor.
    * <p>
    * Note: The specified extractor will be applied to both old and new values.
    *
    * @param extractor  ValueExtractor to extract MapEvent values
    */
    public ExtractorEventTransformer(ValueExtractor<? super V, ? extends E> extractor)
        {
        this(extractor, extractor);
        }

    /**
    * Construct a ExtractorEventTransformer that transforms MapEvent's values
    * based on the specified method name. The name could be a comma-delimited
    * sequence of method names which will result in a MultiExtractor that is
    * based on a corresponding array of {@link ValueExtractor} objects;
    * individual array elements will be either {@link ReflectionExtractor}
    * or {@link ChainedExtractor} objects.
    * <p>
    * Note: The specified extractor will be applied to both old and new values.
    *
    * @param sMethod  the name of the method to invoke via reflection
    */
    public ExtractorEventTransformer(String sMethod)
        {
        this(sMethod.indexOf(',') < 0
            ? (sMethod.indexOf('.') < 0
                ? new ReflectionExtractor(sMethod)
                : (ValueExtractor) new ChainedExtractor(sMethod))
            : new MultiExtractor(sMethod));
        }

    /**
    * Construct a ExtractorEventTransformer that transforms MapEvent values
    * based on the specified extractors. Passing null indicates that the
    * corresponding values should be skipped completely.
    *
    * @param extractorOld  extractor to extract the OldValue property(s)
    * @param extractorNew  extractor to extract the NewValue property(s)
    */
    public ExtractorEventTransformer(ValueExtractor<? super V, ? extends E> extractorOld,
                                     ValueExtractor<? super V, ? extends E> extractorNew)
        {
        m_extractorOld = extractorOld;
        m_extractorNew = extractorNew;
        }


    // ----- Accessors ------------------------------------------------------

    /**
    * Return a ValueExtractor used to transfrom the event's OldValue.
    *
    * @return an extractor from the OldValue
    */
    public ValueExtractor getOldValueExtractor()
        {
        return m_extractorOld;
        }

    /**
    * Return a ValueExtractor used to transfrom the event's NewValue.
    *
    * @return an extractor from the NewValue
    */
    public ValueExtractor getNewValueExtractor()
        {
        return m_extractorNew;
        }


    // ----- MapEventTransformer methods ------------------------------------

    /**
    * Transform the specified MapEvent using the corresponding extractors.
    *
    * @param event MapEvent object to transform
    *
    * @return a modified MapEvent object that contains extracted values
    */
    public MapEvent<K, E> transform(MapEvent<K, V> event)
        {
        ValueExtractor<? super V, ? extends E> extractorOld = getOldValueExtractor();
        ValueExtractor<? super V, ? extends E> extractorNew = getNewValueExtractor();

        return new MapEvent(event.getMap(), event.getId(), event.getKey(),
            extractorOld == null ? null : extractorOld.extract(event.getOldValue()),
            extractorNew == null ? null : extractorNew.extract(event.getNewValue()));
        }


    // ----- ExternalizableLite methods -------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        boolean fSame = in.readBoolean();

        m_extractorOld = ExternalizableHelper.readObject(in);
        m_extractorNew = fSame ? m_extractorOld :
            (ValueExtractor) ExternalizableHelper.readObject(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ValueExtractor extractorOld = m_extractorOld;
        ValueExtractor extractorNew = m_extractorNew;

        if (equals(extractorOld, extractorNew))
            {
            out.writeBoolean(true);
            ExternalizableHelper.writeObject(out, extractorOld);
            }
        else
            {
            out.writeBoolean(false);
            ExternalizableHelper.writeObject(out, extractorOld);
            ExternalizableHelper.writeObject(out, extractorNew);
            }
        }


    // ----- PortableObject methods -----------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        boolean fSame = in.readBoolean(0);

        m_extractorOld = in.readObject(1);
        m_extractorNew = fSame ? m_extractorOld :
            (ValueExtractor) in.readObject(2);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        ValueExtractor extractorOld = m_extractorOld;
        ValueExtractor extractorNew = m_extractorNew;

        if (equals(extractorOld, extractorNew))
            {
            out.writeBoolean(0, true);
            out.writeObject(1, extractorNew);
            }
        else
            {
            out.writeBoolean(0, false);
            out.writeObject(1, extractorOld);
            out.writeObject(2, extractorNew);
            }
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the ExtractorEventTransformer with another object to determine
    * equality.
    *
    * @return true iff this ExtractorEventTransformer and the passed object are
    *         equivalent
    */
    public boolean equals(Object o)
        {
        if (o instanceof ExtractorEventTransformer)
            {
            ExtractorEventTransformer that = (ExtractorEventTransformer) o;
            return equals(this.m_extractorOld, that.m_extractorOld)
                && equals(this.m_extractorNew, that.m_extractorNew);
            }
        return false;
        }

    /**
    * Determine a hash value for the ExtractorEventTransformer object according
    * to the general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this object
    */
    public int hashCode()
        {
        ValueExtractor extractorOld = m_extractorOld;
        ValueExtractor extractorNew = m_extractorNew;
        return (extractorOld == null ? 0 : extractorOld.hashCode()) +
               (extractorNew == null ? 0 : extractorNew.hashCode());
        }

    /**
    * Provide a human-readable representation of this object.
    *
    * @return a String whose contents represent the value of this object
    */
    public String toString()
        {
        ValueExtractor extractorOld = getOldValueExtractor();
        ValueExtractor extractorNew = getNewValueExtractor();

        StringBuilder sb = new StringBuilder(ClassHelper.getSimpleName(getClass()));
        sb.append('{');
        if (equals(extractorOld, extractorNew))
            {
            sb.append("extractors=")
              .append(extractorOld);
            }
        else
            {
            sb.append("extractor old=")
              .append(extractorOld)
              .append(", extractor new=")
              .append(extractorNew);
            }
        sb.append('}');

        return  sb.toString();
        }


    // ----- data fields ----------------------------------------------------

    /**
    * The OldValue extractor.
    */
    @JsonbProperty("extractorOld")
    private ValueExtractor<? super V, ? extends E> m_extractorOld;

    /**
    * The NewValue extractor.
    */
    @JsonbProperty("extractorNew")
    private ValueExtractor<? super V, ? extends E> m_extractorNew;
    }
