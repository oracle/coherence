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

import com.tangosol.net.CacheFactory;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link ValueMacroExpression} is a string value potentially containing expandable macros.
 * <p>
 * Resolving the expression performs macro expansion. The macro syntax is <tt><i>${system-property default-value}</i></tt>.
 * Thus, a value of <tt>near-<i>${coherence.client direct}</i></tt> is macro expanded by default to <tt>near-direct</tt>.
 * If system property <tt><i>coherence.client</i></tt> is set to <tt>remote</tt>, then the value would be expanded to <tt>near-remote</tt>.
 *
 * @author jf 2015.05.18
 * @since Coherence 12.2.1
 */
public class ValueMacroExpression
        implements Expression<String>, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     *  Default constructor needed for serialization.
     *
     */
    public ValueMacroExpression()
        {
        }

    /**
     * Construct a {@link ValueMacroExpression}.
     *
     * @param value  the value that potentially contains a macro expression.
     */
    public ValueMacroExpression(String value)
        {
        m_sValue    = value;
        }

    // ----- Expression interface -------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String evaluate(ParameterResolver resolver)
        {
        String sValue = m_sValue;

        if (sValue != null)
            {
            // replace in-lined properties i.e. ${prop-name default-value} using resolver
            int nCount = 0;
            for (int ofStart = sValue.indexOf("${"); ofStart >= 0; ofStart = sValue.indexOf("${"), nCount++)
                {
                int ofEnd = sValue.indexOf('}', ofStart);

                if (ofEnd == -1)
                    {
                    // missing closing } so no macro to process here
                    break;
                    }

                String sMacro = sValue.substring(ofStart, ofEnd + 1);
                String sDefault;
                String sProp;

                ofStart = sMacro.indexOf(' ');

                if (ofStart >= 0)
                    {
                    sDefault = sMacro.substring(ofStart, sMacro.length() - 1).trim();
                    sProp    = sMacro.substring(2, ofStart);
                    }
                else
                    {
                    sDefault = "";
                    sProp    = sMacro.substring(2, sMacro.length() - 1);
                    }

                try
                    {
                    Parameter p          = resolver.resolve(sProp);
                    String    sPropValue = p == null ? sDefault : p.evaluate(resolver).as(String.class);

                    if (sPropValue.contains("${" + sProp) && sPropValue.contains("}") || nCount > MAX_MACRO_EXPANSIONS)
                        {
                        CacheFactory.log("SystemPropertyPreprocessor: using default value of \"" + sDefault + "\", detected recursive macro definition in system property " +
                                         sProp + " with the value of \"" + sPropValue + "\" ", Base.LOG_ERR);
                        sPropValue = sDefault;
                        }
                    sValue = sValue.replace(sMacro, sPropValue);
                    }
                catch (Exception e)
                    {
                    sValue = sValue.replace(sMacro, sDefault);
                    }
                }
            }

        return sValue;
        }

    // ----- ValueMacroExpression methods ------------------------------------

    /**
     * Check if string contains a macro.
     *
     * @param sValue string potentially containing a macro
     * @return true iff the string value contains a macro
     */
    static public boolean containsMacro(String sValue)
        {
        if (sValue == null)
            {
            return false;
            }

        int ofStart = sValue.indexOf("${");

        return ofStart >= 0 && sValue.indexOf('}', ofStart) > 0;
        }

    /**
     * Check if this contains a macro.
     *
     * @return true iff this contains a macro
     */
    public boolean containsMacro()
        {
        return containsMacro(m_sValue);
        }

    // ----- ExternalizableLite interface -----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_sValue = ExternalizableHelper.readObject(in);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeObject(out, m_sValue);
        }

    // ----- PortableObject interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader)
            throws IOException
        {
        m_sValue = reader.readObject(0);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer)
            throws IOException
        {
        writer.writeObject(0, m_sValue);
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return String.format("ValueMacroExpression[value=%s]", m_sValue);
        }

    // ----- constants ------------------------------------------------------

    /**
     * Avoid recursive macro expansions that never return.  No need for more than 20 macro expansions on
     * one value.
     */
    public static int MAX_MACRO_EXPANSIONS = 20;


    // ----- data members ---------------------------------------------------

    /**
     * The String value.
     */
    private String m_sValue;
    }
