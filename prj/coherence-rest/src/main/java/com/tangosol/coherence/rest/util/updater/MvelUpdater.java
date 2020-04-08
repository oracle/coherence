/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util.updater;

import com.tangosol.coherence.rest.util.RestHelper;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.ValueUpdater;

import com.tangosol.util.extractor.AbstractUpdater;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import org.mvel2.MVEL;
import org.mvel2.ParserContext;

/**
 * MVEL-based ValueUpdater implementation.
 *
 * @author ic  2011.07.14
 */
public class MvelUpdater
        extends AbstractUpdater
        implements ValueUpdater, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor (necessary for the ExternalizableLite and
     * PortableObject interfaces).
     */
    public MvelUpdater()
        {
        }

    /**
     * Construct a MvelUpdater based on an MVEL expression.
     *
     * @param sExpr  the MVEL expression to evaluate
     */
    public MvelUpdater(String sExpr)
        {
        if (sExpr == null)
            {
            throw new IllegalArgumentException("null MVEL expression");
            }
        m_sExpr = sExpr;
        }

    // ----- ValueUpdater interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    public void update(Object oTarget, Object oValue)
        {
        if (oTarget == null)
            {
            throw new IllegalArgumentException(
                "target object is missing for the Updater: " + this);
            }

        MVEL.executeSetExpression(getCompiledExpression(), oTarget, oValue);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return a compiled MVEL expression.
     *
     * @return compiled MVEL expression
     */
    protected Serializable getCompiledExpression()
        {
        Serializable oExpr = m_oExpr;
        if (oExpr == null)
            {
            ParserContext ctx = RestHelper.getMvelParserContext();
            m_oExpr = oExpr = MVEL.compileSetExpression(m_sExpr, ctx);
            }
        return oExpr;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * Compare the MvelUpdater with another object to determine equality.
     *
     * @return true iff this MvelUpdater and the passed object are
     *         equivalent MvelUpdater instances
     */
    public boolean equals(Object o)
        {
        if (o == this)
            {
            return true;
            }

        if (o instanceof MvelUpdater)
            {
            MvelUpdater that = (MvelUpdater) o;
            return Base.equals(this.m_sExpr, that.m_sExpr);
            }

        return false;
        }

    /**
     * Determine a hash value for the MvelUpdater object according to the
     * general {@link Object#hashCode()} contract.
     *
     * @return an integer hash value for this MvelUpdater object
     */
    public int hashCode()
        {
        String sExpr = m_sExpr;
        return sExpr == null ? 0 : sExpr.hashCode();
        }

    /**
     * Provide a human-readable description of this MvelUpdater object.
     *
     * @return a human-readable description of this MvelUpdater object
     */
    public String toString()
        {
        return String.valueOf(m_sExpr);
        }

    // ----- ExternalizableLite implementation ------------------------------

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

    // ----- PortableObject implementation ----------------------------------

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

    // ----- data members ---------------------------------------------------

    /**
     * The MVEL expression to evaluate.
     */
    protected String m_sExpr;

    /**
     * Compiled expression.
     */
    protected transient Serializable m_oExpr;
    }
