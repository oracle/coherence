/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.expression;

import com.tangosol.coherence.config.ResolvableParameterList;

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
 * A {@link ScopedParameterResolver} is a {@link ParameterResolver} implementation 
 * that provides the ability to "scope" {@link Parameter} definitions to either an 
 * inner (wrapped) or outer {@link ParameterResolver}, such that those being 
 * defined in the outer {@link ParameterResolver} hide those (of the same name) 
 * in the inner (wrapped) {@link ParameterResolver}.
 * <p>
 * For example: Parameter "A" defined in the outer {@link ParameterResolver}
 * will override and thus "hide" Parameter "A" that is defined in the inner 
 * {@link ParameterResolver}.
 * <p>
 *
 * @author pfm 2011.12.2
 * @since Coherence 12.1.2
 */
public class ScopedParameterResolver
        implements ParameterResolver, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor needed for serialization.
     */
    public ScopedParameterResolver()
        {
        m_innerResolver = new ResolvableParameterList();;
        m_outerResolver = new ResolvableParameterList();
        }

    /**
     * Construct a {@link ScopedParameterResolver} given the specified inner {@link ParameterResolver}.
     *
     * @param resolver  the inner {@link ParameterResolver}
     */
    public ScopedParameterResolver(ParameterResolver resolver)
        {
        m_innerResolver = resolver == null ? new ResolvableParameterList() : resolver;
        m_outerResolver = new ResolvableParameterList();
        }

    // ----- ParameterResolver interface ------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Parameter resolve(String sName)
        {
        // see if m_object has the parameter, if not use the inner resolver.
        Parameter param = m_outerResolver.resolve(sName);

        return param == null ? m_innerResolver.resolve(sName) : param;
        }

    // ----- ScopedParameterResolver methods --------------------------------

    /**
     * Adds the specified {@link Parameter} to the outer {@link ParameterResolver}.
     *
     * @param parameter  the {@link Parameter} to add
     */
    public void add(Parameter parameter)
        {
        m_outerResolver.add(parameter);
        }

    // ----- logging support ------------------------------------------------

    /**
     * Return a human-readable String representation of this class.
     *
     * @return the description of this class
     */
    protected String getDescription()
        {
        return "Outer Resolver=" + m_outerResolver + ", Inner Resolver=" + m_innerResolver;
        }

    // ----- ExternalizableLite interface -----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_innerResolver = (ParameterResolver) ExternalizableHelper.readObject(
                in);
        m_outerResolver = (ResolvableParameterList) ExternalizableHelper.readObject(in);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_innerResolver);
        ExternalizableHelper.writeObject(out, m_outerResolver);
        }

    // ----- PortableObject interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
        {
        m_innerResolver = (ParameterResolver) reader.readObject(0);
        m_outerResolver = (ResolvableParameterList) reader.readObject(1);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
        {
        writer.writeObject(0, m_innerResolver);
        writer.writeObject(1, m_outerResolver);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The inner (wrapped) {@link ParameterResolver}.
     */
    @JsonbProperty("innerResolver")
    private ParameterResolver m_innerResolver;

    /**
     * The outer {@link ParameterResolver}.
     */
    @JsonbProperty("outerResolver")
    private ResolvableParameterList m_outerResolver;
    }
