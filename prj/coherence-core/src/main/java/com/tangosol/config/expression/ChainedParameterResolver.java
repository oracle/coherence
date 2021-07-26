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

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Arrays;

import javax.json.bind.annotation.JsonbProperty;


/**
 * A {@link ChainedParameterResolver} is a {@link ParameterResolver} that 
 * consults zero or more provided {@link ParameterResolver}s in the order in 
 * which they were defined or added to resolve a {@link Parameter}.
 *
 * @author bo 2012.12.4
 * @since Coherence 12.1.2
 */
public class ChainedParameterResolver
        implements ParameterResolver, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor needed for serialization.
     */
    public ChainedParameterResolver()
        {
        m_aResolvers = new ParameterResolver[1];
        }

    /**
     * Construct a {@link ChainedParameterResolver} based on the specified
     * {@link ParameterResolver}s.
     *
     * @param resolvers  the {@link ParameterResolver}s to be chained
     */
    public ChainedParameterResolver(ParameterResolver... resolvers)
        {
        for (ParameterResolver resolver : resolvers)
            {
            if (resolver == null)
                {
                throw new NullPointerException("A null ParameterResolver found." +
                        " Only non-null ParameterResolvers are permitted");
                }
            }
        m_aResolvers = resolvers;
        }

    // ----- ParameterResolver interface ------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Parameter resolve(String sName)
        {
        for (ParameterResolver resolver : m_aResolvers)
            {
            Parameter param = resolver.resolve(sName);
            if (param != null)
                {
                return param;
                }
            }
        return null;
        }

    // ----- logging support ------------------------------------------------

    /**
     * Return a human-readable String representation of this class.
     *
     * @return the description of this class
     */
    protected String getDescription()
        {
        return "Resolvers=" + Arrays.toString(m_aResolvers);
        }

    // ----- ExternalizableLite interface -----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
        {
        int cResolver = ExternalizableHelper.readInt(in);
        Base.azzert(cResolver < 16384, "Unexpected number of parameter resolvers.");

        ParameterResolver[] aResolver = new ParameterResolver[cResolver];

        for (int i = 0; i < cResolver; i++)
            {
            aResolver[i] = (ParameterResolver) ExternalizableHelper.readObject(in);
            }

        m_aResolvers = aResolver;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ParameterResolver[] aResolver = m_aResolvers;
        int                 cResolver = aResolver.length;

        ExternalizableHelper.writeInt(out, cResolver);
        for (int i = 0; i < cResolver; i++)
            {
            ExternalizableHelper.writeObject(out, aResolver[i]);
            }
        }

    // ----- PortableObject interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_aResolvers = (ParameterResolver[]) in.readObjectArray(0, EMPTY_RESOLVER_ARRAY);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObjectArray(0, m_aResolvers);
        }

    // ----- constants ------------------------------------------------------

    /**
     * An empty array of {@link ParameterResolver}s.
     */
    private static final ParameterResolver[] EMPTY_RESOLVER_ARRAY = new ParameterResolver[0];

    /**
     * The {@link ParameterResolver}s to consult when attempting to resolve
     * a {@link Parameter}.
     */
    @JsonbProperty("resolvers")
    private ParameterResolver[] m_aResolvers;
    }
