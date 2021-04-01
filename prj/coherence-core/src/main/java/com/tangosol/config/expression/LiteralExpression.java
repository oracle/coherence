/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.expression;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;

/**
 * A {@link LiteralExpression} is a literal (aka: constant) {@link Expression}.
 *
 * @param T  the type of the literal constant
 *
 * @author bo  2011.06.24
 * @since Coherence 12.1.2
 */
public class LiteralExpression<T>
        implements Expression<T>, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     *  Default constructor needed for serialization.
     *
     */
    public LiteralExpression()
        {
        }

    /**
     * Construct a {@link LiteralExpression}.
     *
     * @param value  the value of the constant
     */
    public LiteralExpression(T value)
        {
        m_value = value;
        }

    // ----- Expression interface -------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public T evaluate(ParameterResolver resolver)
        {
        return m_value;
        }

    // ----- ExternalizableLite interface -----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_value = (T) ExternalizableHelper.readObject(in);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_value);
        }

    // ----- PortableObject interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
        {
        m_value = (T) reader.readObject(0);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
        {
        writer.writeObject(0, m_value);
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return String.format("LiteralExpression{value=%s}", m_value);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The value of the constant.
     */
    @JsonbProperty("value")
    private T m_value;
    }
