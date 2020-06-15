/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config;

import com.tangosol.config.expression.Parameter;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;

import javax.json.bind.annotation.JsonbProperty;

/**
 * A {@link SimpleParameterList} is a simple implementation of {@link ParameterList}.
 *
 * @author bo  2012.02.02
 * @since Coherence 12.1.2
 */
public class SimpleParameterList
        implements ParameterList, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs an empty {@link SimpleParameterList}.
     */
    public SimpleParameterList()
        {
        m_listParameters = new ArrayList<Parameter>(5);
        }

    /**
     * Constructs a {@link SimpleParameterList} based on the specified array of objects,
     * each object becoming it's own {@link Parameter} in the resulting list.
     *
     * @param aObjects  the objects to be considered as parameters
     */
    public SimpleParameterList(Object... aObjects)
        {
        this();

        if (aObjects != null)
            {
            for (Object o : aObjects)
                {
                add(o);
                }
            }
        }

    /**
     * Constructs a {@link SimpleParameterList} based on a {@link ParameterList}.
     *
     * @param listParameters  the {@link ParameterList} from which {@link Parameter}s should be drawn
     */
    public SimpleParameterList(ParameterList listParameters)
        {
        this();

        for (Parameter parameter : listParameters)
            {
            add(parameter);
            }
        }

    // ----- ParameterList interface ----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(Parameter parameter)
        {
        m_listParameters.add(parameter);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty()
        {
        return m_listParameters.isEmpty();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size()
        {
        return m_listParameters.size();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Parameter> iterator()
        {
        return m_listParameters.iterator();
        }

    // ----- SimpleParameterList methods ------------------------------------

    /**
     * Adds the specified object to the end of the {@link ParameterList} as an anonymous {@link Parameter}.
     *
     * @param o  the object to add as a {@link Parameter}
     */
    public void add(Object o)
        {
        if (o instanceof Parameter)
            {
            add((Parameter) o);
            }
        else
            {
            add(new Parameter("param-" + Integer.toString(size() + 1), o));
            }
        }

    // ----- ExternalizableLite interface -----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
        {
        ExternalizableHelper.readCollection(in, m_listParameters, null);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeCollection(out, m_listParameters);
        }

    // ----- PortableObject interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
        {
        reader.readCollection(0, m_listParameters);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
        {
        writer.writeCollection(0, m_listParameters);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Parameter} list.
     */
    @JsonbProperty("parameters")
    private ArrayList<Parameter> m_listParameters;
    }
