/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config;

import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.json.bind.annotation.JsonbProperty;

/**
 * A {@link ResolvableParameterList} is a {@link ParameterList} implementation that additionally supports
 * name-based {@link Parameter} resolution as defined by the {@link ParameterResolver} interface.
 *
 * @author bo  2011.06.22
 * @since Coherence 12.1.2
 */
public class ResolvableParameterList
        implements ParameterList, ParameterResolver, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs an empty {@link ResolvableParameterList}.
     */
    public ResolvableParameterList()
        {
        m_mapParameters = new LinkedHashMap<String, Parameter>();
        }

    /**
     * Constructs a {@link ResolvableParameterList} based on a {@link ParameterList}.
     *
     * @param listParameters  the {@link ParameterList} from which {@link Parameter}s should be drawn
     */
    public ResolvableParameterList(ParameterList listParameters)
        {
        this();

        for (Parameter parameter : listParameters)
            {
            add(parameter);
            }
        }

    /**
     * Construct a {@link ResolvableParameterList} from provided map.
     * The key of the map is used as the parameter name and the
     * corresponding value as the parameter value.
     *
     * @param map  the Map of Named Parameter of type String to an Object value
     *
     * @since Coherence 12.2.1
     */
    public ResolvableParameterList(Map map)
        {
        this();

        if (map != null && ! map.isEmpty())
            {
            Map<?,?> map1 = map;
            for (Map.Entry entry : map1.entrySet())
                {
                add(new Parameter(String.valueOf(entry.getKey()), entry.getValue()));
                }
            }
        }


    // ----- ParameterResolver interface ------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Parameter resolve(String sName)
        {
        return m_mapParameters.get(sName);
        }

    // ----- ParameterList interface ----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(Parameter parameter)
        {
        m_mapParameters.put(parameter.getName(), parameter);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty()
        {
        return m_mapParameters.isEmpty();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size()
        {
        return m_mapParameters.size();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Parameter> iterator()
        {
        return m_mapParameters.values().iterator();
        }

    // ----- ExternalizableLite interface -----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
        {
        ExternalizableHelper.readMap(in, m_mapParameters, this.getClass().getClassLoader());
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeMap(out, m_mapParameters);
        }

    // ----- PortableObject interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
        {
        reader.readMap(0, m_mapParameters);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
        {
        writer.writeMap(0, m_mapParameters);
        }

    // ----- logging support ------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return "{" + m_mapParameters.toString() + "}";
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Parameter} map.
     * <p>
     * NOTE: It's important that this is a {@link LinkedHashMap} as we expect to be able to iterate over
     * the {@link Parameter}s in the order in which they were added to the {@link ResolvableParameterList}.
     * This is to support implementation of the {@link ParameterList} interface.
     */
    @JsonbProperty("parameters")
    private LinkedHashMap<String, Parameter> m_mapParameters;
    }
