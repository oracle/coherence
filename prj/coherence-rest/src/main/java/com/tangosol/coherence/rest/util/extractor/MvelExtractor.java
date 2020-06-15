/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util.extractor;


import com.tangosol.coherence.rest.util.RestHelper;
import com.tangosol.coherence.rest.util.JsonMap;

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

import org.mvel2.MVEL;
import org.mvel2.ParserContext;

import org.mvel2.asm.MethodVisitor;
import org.mvel2.asm.Opcodes;

import org.mvel2.integration.PropertyHandler;
import org.mvel2.integration.PropertyHandlerFactory;
import org.mvel2.integration.VariableResolverFactory;

import org.mvel2.optimizers.impl.asm.ProducesBytecode;


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

        return MVEL.executeExpression(getCompiledExpression(), oTarget);
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
            m_oExpr = oExpr = MVEL.compileExpression(m_sExpr, ctx);
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

    // ----- inner class: JsonMapPropertyHandler ----------------------------

    private static class JsonMapPropertyHandler
            implements PropertyHandler, ProducesBytecode
        {
        @Override
        public Object getProperty(String name, Object contextObj, VariableResolverFactory variableFactory)
            {
            return ((JsonMap) contextObj).get(name);
            }

        @Override
        public Object setProperty(String name, Object contextObj, VariableResolverFactory variableFactory, Object value)
            {
            ((JsonMap) contextObj).put(name, value);
            return value;
            }

        @Override
        public void produceBytecodeGet(MethodVisitor mv, String propertyName, VariableResolverFactory factory)
            {
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitTypeInsn(Opcodes.CHECKCAST, "com/tangosol/coherence/rest/util/JsonMap");
            mv.visitLdcInsn(propertyName);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/tangosol/coherence/rest/util/JsonMap", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
            }

        @Override
        public void produceBytecodePut(MethodVisitor mv, String propertyName, VariableResolverFactory factory)
            {
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitTypeInsn(Opcodes.CHECKCAST, "com/tangosol/coherence/rest/util/JsonMap");
            mv.visitLdcInsn(propertyName);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/tangosol/coherence/rest/util/JsonMap", "put", "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;", false);
            }
        }


    // ----- static initialization ------------------------------------------

    static
        {
        MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;
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
