/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.expression.Value;

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
 * A {@link ParameterMacroExpression} is an {@link Expression} representing the use of a Coherence Parameter Macro,
 * typically occurring with in a Coherence Cache Configuration file.
 * <p>
 * Coherence Macro Parameters are syntactically represented as follows:
 * <p>
 * <code>{parameter-name [default-value]}</code>
 * <p>
 * When a {@link ParameterMacroExpression} is evaluated the parameter-name and it's associated value is resolved by
 * consulting the provided {@link ParameterResolver}.  If the parameter is resolvable,
 * the value of the resolved parameter is returned.  If it's not resolvable the default value is returned.
 * <p>
 * Note: Returned values are always coerced into the type defined by the {@link Expression}.
 *
 * @author bo  2011.06.22
 * @since Coherence 12.1.2
 */
public class ParameterMacroExpression<T>
        implements Expression<T>, ExternalizableLite, PortableObject
    {
    // ----- constructor ----------------------------------------------------

    /**
     *  Default constructor needed for serialization.
     */
    public ParameterMacroExpression()
        {
        }

    /**
     * Construct a {@link ParameterMacroExpression}.
     *
     * @param sExpression    a string representation of the {@link Expression}
     * @param clzResultType  the type of value the {@link Expression} will return when evaluated
     */
    public ParameterMacroExpression(String sExpression, Class<T> clzResultType)
        {
        Base.azzert(clzResultType != null);

        m_clzResultType = clzResultType;
        m_sExpression   = sExpression.trim();
        }

    // ----- Expression interface -------------------------------------------

    /**
     * {@inheritDoc}
     */
    public T evaluate(ParameterResolver resolver)
        {
        // there are two styles of usage for parameter macros with coherence.
        // i).  the expression contains a single parameter macro usage (and nothing else) in which the parameter
        // is evaluated and returned.
        //
        // ii). the expression contains zero or more parameter macros in a string in which case we have to
        // resolve each parameter and replace them in the string, after which we have to evaluate the string.

        // assume we don't have a result
        Value result = null;

        // assume we won't require the string based result
        boolean fUseStringBasedResult = false;

        // we may need to build a string containing the result, before we can produce a result
        StringBuilder bldr = new StringBuilder();

        for (int idx = 0; idx < m_sExpression.length(); idx++)
            {
            if (m_sExpression.startsWith("\\{", idx))
                {
                bldr.append("{");
                idx++;
                fUseStringBasedResult = true;
                }
            else if (m_sExpression.startsWith("\\}", idx))
                {
                bldr.append("}");
                idx++;
                fUseStringBasedResult = true;
                }
            else if (m_sExpression.charAt(idx) == '{')
                {
                String sParameterName;
                String sDefaultValue;
                int    idxDefaultValue = m_sExpression.indexOf(" ", idx + 1);

                if (idxDefaultValue >= 0)
                    {
                    sParameterName = m_sExpression.substring(idx + 1, idxDefaultValue).trim();

                    int idxEndMacro = m_sExpression.indexOf("}", idxDefaultValue);

                    if (idxEndMacro > idxDefaultValue)
                        {
                        sDefaultValue = m_sExpression.substring(idxDefaultValue, idxEndMacro).trim();
                        idx           = idxEndMacro;
                        }
                    else
                        {
                        throw new IllegalArgumentException(String.format(
                                "Invalid parameter macro definition in [%s].  "
                                        + "Missing closing brace '}'.", m_sExpression));
                        }
                    }
                else
                    {
                    int idxEndMacro = m_sExpression.indexOf("}", idx + 1);

                    if (idxEndMacro > idx + 1)
                        {
                        sParameterName = m_sExpression.substring(idx + 1, idxEndMacro).trim();
                        sDefaultValue  = null;
                        idx            = idxEndMacro;
                        }
                    else
                        {
                        throw new IllegalArgumentException(String.format(
                                "Invalid parameter macro definition in [%s].  "
                                    + "Missing closing brace '}'.", m_sExpression));
                        }
                    }

                Parameter parameter = resolver.resolve(sParameterName);
                Value     value;

                if (parameter == null)
                    {
                    if (sDefaultValue == null)
                        {
                        throw new IllegalArgumentException(String.format(
                                "The specified parameter name '%s' in the macro "
                                    + "parameter '%s' is unknown and not resolvable",
                                sParameterName, m_sExpression));
                        }
                    else
                        {
                        value = new Value(sDefaultValue);
                        }
                    }
                else
                    {
                    value = parameter.evaluate(resolver);
                    }

                result = value == null ? new Value() : value;

                if (fUseStringBasedResult || (value.get() instanceof String))
                    {
                    try
                        {
                        bldr.append(value.get().toString());
                        }
                    catch (Exception e)
                        {
                        Logger.warn("ParameterMacroExpression evaluation "
                                   + "resulted in a toString Exception for class "
                                   + value.get().getClass().getName());
                        throw Base.ensureRuntimeException(e);
                        }
                    }
                }
            else
                {
                bldr.append(m_sExpression.charAt(idx));
                fUseStringBasedResult = true;
                }
            }

        Object o  = fUseStringBasedResult
                ? new Value(bldr.toString()).as(m_clzResultType) : result == null ? null : result.as(m_clzResultType);

        return fUseStringBasedResult ? new Value(bldr.toString()).as(m_clzResultType)
                : result == null ? null :result.as(m_clzResultType);
        }

    // ----- ExternalizableLite interface -----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_sExpression   = ExternalizableHelper.readUTF(in);

        String sClzName = ExternalizableHelper.readSafeUTF(in);
        if (sClzName.length() > 0)
            {
            try
                {
                m_clzResultType = (Class<T>) Class.forName(sClzName);
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
        ExternalizableHelper.writeUTF(out, m_sExpression);
        ExternalizableHelper.writeUTF(out, m_clzResultType.getName());
        }

    // ----- PortableObject interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_sExpression   = in.readString(0);

        String sClzName = in.readString(1);
        if (sClzName.length() > 0)
            {
            try
                {
                m_clzResultType = (Class<T>) Class.forName(sClzName);
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
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, m_sExpression);
        out.writeString(1, m_clzResultType.getName());
        }

    // ----- Object interface -----------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String toString()
        {
        return String.format("ParameterMacroExpression{type=%s, expression=%s}", m_clzResultType, m_sExpression);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The type of value to be returned by the {@link Expression}.
     */
    @JsonbProperty("resultType")
    private Class<T> m_clzResultType;

    /**
     * The expression to be evaluated.
     */
    @JsonbProperty("expression")
    private String m_sExpression;
    }
