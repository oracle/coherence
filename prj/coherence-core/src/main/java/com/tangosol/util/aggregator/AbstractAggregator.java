/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.aggregator;


import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.Streamer;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;

/**
 * Abstract base class implementation of {@link InvocableMap.EntryAggregator}
 * that supports streaming aggregation.
 *
 * @param <K> the type of the Map entry key
 * @param <V> the type of the Map entry value
 * @param <T> the type of the value to extract from
 * @param <E> the type of the extracted value to aggregate
 * @param <R> the type of the aggregation result
 *
 * @author cp/gg/jh  2005.07.19
 * @since Coherence 3.1
 */
public abstract class AbstractAggregator<K, V, T, E, R>
        extends    ExternalizableHelper
        implements InvocableMap.StreamingAggregator<K, V, Object, R>,
                   ExternalizableLite, PortableObject, Cloneable
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public AbstractAggregator()
        {
        }

    /**
     * Construct an AbstractAggregator that will aggregate values extracted from
     * a set of {@link com.tangosol.util.InvocableMap.Entry} objects.
     *
     * @param extractor the extractor that provides values to aggregate
     */
    public AbstractAggregator(ValueExtractor<? super T, ? extends E> extractor)
        {
        azzert(extractor != null);
        m_extractor = Lambdas.ensureRemotable(extractor);
        }

    /**
     * Construct an AbstractAggregator that will aggregate values extracted from
     * a set of {@link com.tangosol.util.InvocableMap.Entry} objects.
     *
     * @param sMethod the name of the method that could be invoked via
     *                reflection and that returns values to aggregate; this
     *                parameter can also be a dot-delimited sequence of method
     *                names which would result in an aggregator based on the
     *                {@link ChainedExtractor} that is based on an array of
     *                corresponding {@link ReflectionExtractor} objects
     */
    public AbstractAggregator(String sMethod)
        {
        m_extractor = sMethod.indexOf('.') < 0 ?
            new ReflectionExtractor(sMethod) :
            (ValueExtractor) new ChainedExtractor(sMethod);
        }

    // ----- InvocableMap.StreamingAggregator interface ---------------------

    @Override
    public InvocableMap.StreamingAggregator<K, V, Object, R> supply()
        {
        try
            {
            AbstractAggregator aggregator = (AbstractAggregator) super.clone();
            aggregator.m_fInit = false;
            return aggregator;
            }
        catch (CloneNotSupportedException e)
            {
            // this should never happen
            throw new IllegalStateException(getClass().getName() + " is not Cloneable", e);
            }
        }

    @Override
    public boolean accumulate(Streamer<? extends InvocableMap.Entry<? extends K, ? extends V>> streamer)
        {
        if (streamer.hasNext())
            {
            return InvocableMap.StreamingAggregator.super.accumulate(streamer);
            }

        ensureInitialized(false);
        return true;
        }

    @Override
    public boolean accumulate(InvocableMap.Entry<? extends K, ? extends V> entry)
        {
        ensureInitialized(false);

        processEntry(entry);
        return true;
        }

    @Override
    public boolean combine(Object partialResult)
        {
        ensureInitialized(true);

        process(partialResult, true);
        return true;
        }

    @Override
    public Object getPartialResult()
        {
        return finalizeResult(false);
        }

    @Override
    public R finalizeResult()
        {
        return (R) finalizeResult(true);
        }

    // ----- AbstractAggregator methods -------------------------------------

    /**
     * Ensure that this aggregator is initialized.
     *
     * @param fFinal  true is passed if the aggregation process that is being
     *                initialized must produce a final aggregation result;
     *                this will only be false if a parallel approach is being
     *                used and the initial (partial) aggregation process is
     *                being initialized
     */
    protected void ensureInitialized(boolean fFinal)
        {
        if (!m_fInit)
            {
            m_fParallel = !fFinal;
            init(fFinal);
            m_fInit = true;
            }
        }

    /**
     * Incorporate one aggregatable entry into the result.
     *
     * @param entry the entry to incorporate into the aggregation result
     */
    protected void processEntry(InvocableMap.Entry<? extends K, ? extends V> entry)
        {
        process(entry.extract(getValueExtractor()), false);
        }

    /**
     * Determine the ValueExtractor whose values this aggregator is
     * aggregating.
     *
     * @return the ValueExtractor used by this aggregator
     */
    public ValueExtractor<? super T, ? extends E> getValueExtractor()
        {
        return m_extractor;
        }

    /**
     * Initialize the aggregation result.
     *
     * @param fFinal true is passed if the aggregation process that is being
     *               initialized must produce a final aggregation result; this
     *               will only be false if a parallel approach is being used and
     *               the initial (partial) aggregation process is being
     *               initialized
     */
    protected abstract void init(boolean fFinal);

    /**
     * Incorporate one aggregatable value into the result.
     * <p>
     * If the <tt>fFinal</tt> parameter is true, the given object is a partial
     * result (returned by an individual parallel aggregator) that should be
     * incorporated into the final result; otherwise, the object is a value
     * extracted from an {@link com.tangosol.util.InvocableMap.Entry}.
     *
     * @param o      the value to incorporate into the aggregated result
     * @param fFinal true to indicate that the given object is a partial result
     *               returned by a parallel aggregator
     */
    protected abstract void process(Object o, boolean fFinal);

    /**
     * Obtain the result of the aggregation.
     * <p>
     * If the <tt>fFinal</tt> parameter is true, the returned object must be
     * the final result of the aggregation; otherwise, the returned object
     * will be treated as a partial result that should be incorporated into
     * the final result.
     *
     * @param  fFinal  true to indicate that the final result of the
     *                 aggregation process should be returned; this will only
     *                 be false if a parallel approach is being used
     *
     * @return the result of the aggregation process
     */
    protected abstract Object finalizeResult(boolean fFinal);

    // ----- Object methods -------------------------------------------------

    /**
    * Provide a human-readable representation of this object.
    *
    * @return a String whose contents represent the value of this object
    */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass()) + '('
                + getValueExtractor().toString() + ')';
        }

    /**
    * Returns a hash code value for this object.
    *
    * @return  a hash code value for this object
    */
    public int hashCode()
        {
        return getClass().hashCode() ^ getValueExtractor().hashCode();
        }

    /**
    * Compares this object with another object for equality.
    *
    * @param  o  an object reference or null
    *
    * @return  true iff the passed object reference is of the same class and
    *          has the same state as this object
    */
    public boolean equals(Object o)
        {
        if (o instanceof AbstractAggregator)
            {
            AbstractAggregator that = (AbstractAggregator) o;
            return this == that
                || this.getClass()  == that.getClass()
                && this.m_fParallel == that.m_fParallel
                && equals(this.m_extractor, that.m_extractor);
            }

        return false;
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_fParallel = in.readBoolean();
        m_extractor = readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        out.writeBoolean(m_fParallel);
        writeObject(out, m_extractor);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_fParallel = in.readBoolean(0);
        m_extractor = in.readObject(1);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeBoolean(0, m_fParallel);
        out.writeObject(1, m_extractor);
        }

    // ----- data members ---------------------------------------------------

    /**
    * Set to true if this aggregator realizes that it is going to be used in
    * parallel.
    */
    @JsonbProperty("parallel")
    protected boolean m_fParallel;

    /**
    * The ValueExtractor that obtains the value to aggregate from the value
    * that is stored in the Map.
    */
    @JsonbProperty("extractor")
    private ValueExtractor<? super T, ? extends E> m_extractor;

    /**
     * The flag specifying whether this aggregator has been initialized.
     */
    private transient boolean m_fInit;
    }
