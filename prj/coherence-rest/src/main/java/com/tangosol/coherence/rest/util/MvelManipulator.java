/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util;

import com.tangosol.coherence.dslquery.UniversalExtractorBuilder;

import com.tangosol.coherence.rest.util.extractor.MvelExtractor;

import com.tangosol.coherence.rest.util.updater.MvelUpdater;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueManipulator;
import com.tangosol.util.ValueUpdater;

import com.tangosol.util.extractor.UniversalUpdater;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static com.tangosol.util.extractor.AbstractExtractor.VALUE;


/**
 *  MVEL-based ValueManipulator implementation.
 *
 * @author ic  2011.07.14
 */
public class MvelManipulator
        implements ValueManipulator, ExternalizableLite, PortableObject
    {
    // ----- constructors ----------------------------------------------------

    /**
     * Default constructor (necessary for the ExternalizableLite and
     * PortableObject interfaces).
     */
    public MvelManipulator()
        {
        }

    /**
     * Construct a MvelManipulator based on an MVEL expression.
     *
     * @param sExpr  the MVEL expression to evaluate
     */
    public MvelManipulator(String sExpr)
        {
        if (sExpr == null)
            {
            throw new IllegalArgumentException("null expression");
            }
        m_sExpr = sExpr;
        }

    // ----- ValueManipulator interface -------------------------------------

    /**
     * {@inheritDoc}
     */
    public ValueExtractor getExtractor()
        {
        ValueExtractor extractor = m_extractor;
        if (extractor == null)
            {
            m_extractor = extractor = MvelHelper.isEnabled()
                                      ? new MvelExtractor(m_sExpr)
                                      : new UniversalExtractorBuilder().realize("",
                                                                                VALUE,
                                                                                m_sExpr);
            }
        return extractor;
        }

    /**
     * {@inheritDoc}
     */
    public ValueUpdater getUpdater()
        {
        ValueUpdater updater = m_updater;
        if (updater == null)
            {
            m_updater = updater = MvelHelper.isEnabled()
                                  ? new MvelUpdater(m_sExpr)
                                  : new UniversalUpdater(m_sExpr);
            }
        return updater;
        }

    // ----- Object methods -------------------------------------------------

     /**
     * Compare the MvelManipulator with another object to determine equality.
     *
     * @return true iff this MvelManipulator and the passed object are
     *         equivalent MvelManipulators
     */
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o instanceof MvelManipulator)
            {
            MvelManipulator that = (MvelManipulator) o;
            return Base.equals(this.m_sExpr, that.m_sExpr);    
            }

        return false;
        }

    /**
     * Determine a hash value for the MvelManipulator object according to the
     * general {@link Object#hashCode()} contract.
     *
     * @return an integer hash value for this MvelManipulator object
     */
    public int hashCode()
        {
        return m_sExpr.hashCode();
        }

    /**
     * Provide a human-readable description of this MvelManipulator object.
     *
     * @return a human-readable description of this MvelManipulator object
     */
    public String toString()
        {
        return "MvelManipulator(" + m_sExpr + ')';
        }

    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_sExpr = ExternalizableHelper.readSafeUTF(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeSafeUTF(out, m_sExpr);
        }

    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_sExpr = in.readString(0);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeString(0, m_sExpr);
        }

    // ---- data members ----------------------------------------------------

    /**
     * The MVEL expression to evaluate.
     */
    protected String m_sExpr;

    /**
     * The underlying ValueExtractor.
     */
    protected transient ValueExtractor m_extractor;

    /**
     * The underlying ValueUpdater.
     */
    protected transient ValueUpdater m_updater;
    }
