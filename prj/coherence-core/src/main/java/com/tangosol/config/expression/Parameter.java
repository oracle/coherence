/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.expression;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;

/**
 * A {@link Parameter} represents an optionally named and optionally explicitly typed {@link Expression}.
 * <p>
 * <strong>Terminology:</strong>
 * <p>
 * A {@link Parameter} that doesn't have a name, or when the name is irrelevant, is referred to as an
 * <strong>Actual {@link Parameter}</strong>.  That is, only the value of the {@link Parameter} is of any importance.
 * <p>
 * A {@link Parameter} with a name and/or when the name is of importance, is referred to as a <strong>Formal
 * {@link Parameter}</strong> or <strong>Argument</strong>.
 * <p>
 * NOTE: This class is used to represent Actual and Formal {@link Parameter}s.
 *
 * @author bo 2011.06.22
 * @since Coherence 12.1.2
 */
public class Parameter
        implements Expression<Value>, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     *  Default constructor needed for serialization.
     */
    public Parameter()
        {
        }

    /**
     * Construct an implicitly typed {@link Parameter} based on an {@link Expression}.
     *
     * @param sName       the name of the {@link Parameter}
     * @param expression  the {@link Expression} for the {@link Parameter}
     */
    public Parameter(String sName, Expression<?> expression)
        {
        m_sName      = sName;
        m_clzType    = null;
        m_expression = expression;
        }

    /**
     * Construct an implicitly typed {@link Object}-based {@link Parameter}.
     *
     * @param sName   the name of the {@link Parameter}
     * @param oValue  the value for the {@link Parameter}
     */
    public Parameter(String sName, Object oValue)
        {
        m_sName      = sName;
        m_clzType    = null;
        m_expression = new LiteralExpression<Object>(oValue);
        }

    /**
     * Construct an {@link Parameter}.
     *
     * @param sName       the name of the {@link Parameter}
     * @param clzType     the expected type of the {@link Parameter}
     * @param expression  the {@link Expression} for the {@link Parameter}
     */
    public Parameter(String sName, Class<?> clzType, Expression<?> expression)
        {
        m_sName      = sName;
        m_clzType    = clzType;
        m_expression = expression;
        }

    /**
     * Construct an explicitly typed {@link Object}-based {@link Parameter}.
     *
     * @param sName    the name of the {@link Parameter}
     * @param clzType  the expected type of the {@link Parameter}
     * @param oValue  the value for the {@link Parameter}
     */
    public Parameter(String sName, Class<?> clzType, Object oValue)
        {
        m_sName      = sName;
        m_clzType    = clzType;
        m_expression = new LiteralExpression<Object>(oValue);
        }

    // ----- Expression interface -------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Value evaluate(ParameterResolver resolver)
        {
        // evaluate whatever the expression is
        Object oValue = m_expression.evaluate(resolver);

        // make sure the result is a value
        Value value = oValue instanceof Value ? (Value) oValue : new Value(oValue);

        // when the parameter is explicitly typed, attempt to coerce the type into that which is specified
        if (isExplicitlyTyped())
            {
            return new Value(value.as(m_clzType));
            }
        else
            {
            return value;
            }
        }

    // ----- Object interface -----------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        String sResult = "";

        if (m_sName != null)
            {
            sResult += "name=" + m_sName;
            }

        if (m_clzType != null)
            {
            sResult += (sResult.isEmpty() ? "" : ", ") + "type=" + m_clzType;
            }

        sResult += (sResult.isEmpty() ? "" : ", ") + "expression=" + m_expression;

        return "Parameter{" + sResult + "}";
        }

    // ----- Parameter methods ----------------------------------------------

    /**
     * Obtains the name of the {@link Parameter}.
     *
     * @return a {@link String} representing the name of the {@link Parameter}
     */
    public String getName()
        {
        return m_sName;
        }

    /**
     * Obtains the explicitly specified type of the {@link Parameter}.
     *
     * @return a {@link Class} representing the type of the {@link Parameter}
     */
    public Class<?> getExplicitType()
        {
        return m_clzType;
        }

    /**
     * Obtains if an expected/actual type of the {@link Parameter} has been specified/is known.
     *
     * @return <code>true</code> if the type of the {@link Parameter} is known/specified.
     *         ie: {@link #getExplicitType()} is not <code>null</code>, otherwise returns <code>false</code>.
     */
    public boolean isExplicitlyTyped()
        {
        return m_clzType != null;
        }

    /**
     * Obtains the {@link Expression} for the {@link Parameter}.
     *
     * @return the {@link Expression} for the {@link Parameter}
     */
    public Expression<?> getExpression()
        {
        return m_expression;
        }

    // ----- ExternalizableLite interface -----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_sName      = ExternalizableHelper.readSafeUTF(in);
        m_expression = (Expression) ExternalizableHelper.readObject(in);

        String sClzName = ExternalizableHelper.readSafeUTF(in);
        if (sClzName.length() > 0)
            {
            try
                {
                m_clzType = Class.forName(sClzName);
                }
            catch (ClassNotFoundException e)
                {
                throw Base.ensureRuntimeException(e);
                }
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeSafeUTF(out, m_sName);
        ExternalizableHelper.writeObject(out, m_expression);
        ExternalizableHelper.writeSafeUTF(out, m_clzType == null ? "" : m_clzType.getName());
        }

    // ----- PortableObject interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
        {
        m_sName      = reader.readString(0);
        m_expression = (Expression) reader.readObject(1);

        String sClzName = reader.readString(2);
        if (sClzName.length() > 0)
            {
            try
                {
                m_clzType = Class.forName(sClzName);
                }
            catch (ClassNotFoundException e)
                {
                throw Base.ensureRuntimeException(e);
                }
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
        {
        writer.writeString(0, m_sName);
        writer.writeObject(1, m_expression);
        writer.writeString(2, m_clzType == null ? "" : m_clzType.getName());
        }

    // ----- data members ---------------------------------------------------

    /**
     * The name of the {@link Parameter}.
     */
    @JsonbProperty("name")
    private String m_sName;

    /**
     * (optional) The expected type of value of the {@link Parameter}.
     * <p>
     * NOTE: when <code>null</code> a type has not been specified.
     */
    @JsonbProperty("classType")
    private Class<?> m_clzType;

    /**
     * The {@link Expression} representing the value of the {@link Parameter}.
     */
    @JsonbProperty("expression")
    private Expression<?> m_expression;
    }
