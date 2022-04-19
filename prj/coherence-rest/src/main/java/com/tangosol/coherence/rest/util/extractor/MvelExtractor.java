/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util.extractor;


import com.tangosol.coherence.rest.util.MvelHelper;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.AbstractExtractor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;


/**
 * MVEL-based ValueExtractor implementation.
 *
 * @author as  2011.06.21
 */
public class MvelExtractor
        extends AbstractExtractor
        implements ValueExtractor, ExternalizableLite, PortableObject
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor (necessary for the ExternalizableLite and
     * PortableObject interfaces).
     */
    public MvelExtractor()
        {
        }

    /**
     * Construct a MvelExtractor based on an MVEL expression.
     *
     * @param sExpr  the MVEL expression to evaluate
     */
    public MvelExtractor(String sExpr)
        {
        this(sExpr, VALUE);
        }

    /**
     * Construct a MvelExtractor based on an MVEL expression and an entry
     * extraction target.
     *
     * @param sExpr    the MVEL expression to evaluate
     * @param nTarget  one of the {@link #VALUE} or {@link #KEY} values
     */
    public MvelExtractor(String sExpr, int nTarget)
        {
        if (sExpr == null)
            {
            throw new IllegalArgumentException("null MVEL expression");
            }
        if (nTarget != VALUE && nTarget != KEY)
            {
            throw new IllegalArgumentException("invalid target: " + nTarget);
            }
        m_sExpr   = sExpr;
        m_nTarget = nTarget;
        }

    // ----- ValueExtractor interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    public Object extract(Object oTarget)
        {
        if (oTarget == null)
            {
            return null;
            }

        return MvelHelper.executeExpression(getCompiledExpression(), oTarget);
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
            Object ctx = MvelHelper.getMvelParserContext();

            m_oExpr = oExpr = MvelHelper.compileExpression(m_sExpr, ctx);
            }
        return oExpr;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * Compare the MvelExtractor with another object to determine equality.
     * Two MvelExtractor objects, <i>e1</i> and <i>e2</i> are considered
     * equal iff <tt>e1.extract(o)</tt> equals <tt>e2.extract(o)</tt> for
     * all values of <tt>o</tt>.
     *
     * @return true iff this MvelExtractor and the passed object are
     *         equivalent MvelExtractor instances
     */
    public boolean equals(Object o)
        {
        if (o == this)
            {
            return true;
            }

        if (o instanceof MvelExtractor)
            {
            MvelExtractor that = (MvelExtractor) o;
            return Base.equals(this.m_sExpr, that.m_sExpr);
            }

        return false;
        }

    /**
     * Determine a hash value for the MvelExtractor object according to the
     * general {@link Object#hashCode()} contract.
     *
     * @return an integer hash value for this MvelExtractor object
     */
    public int hashCode()
        {
        String sExpr = m_sExpr;
        return sExpr == null ? 0 : sExpr.hashCode();
        }

    /**
     * Provide a human-readable description of this MvelExtractor object.
     *
     * @return a human-readable description of this MvelExtractor object
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
        m_nTarget = in.readInt();
        m_sExpr   = ExternalizableHelper.readSafeUTF(in);
        }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        out.writeInt(m_nTarget);
        ExternalizableHelper.writeSafeUTF(out, m_sExpr);
        }

    // ----- PortableObject implementation ----------------------------------

    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_nTarget = in.readInt(0);
        m_sExpr   = in.readString(1);
        }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeInt   (0, m_nTarget);
        out.writeString(1, m_sExpr);
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
