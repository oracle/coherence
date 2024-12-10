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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link NullParameterResolver} is a {@link ParameterResolver} that always
 * resolves named {@link Parameter}s to <code>null</code>.
 * <p>
 * <strong>IMPORTANT:</strong> Developers should only use this class when they
 * can't easily access the {@link ParameterResolver} provided by the CacheConfig
 * getDefaultParameterResolver() method.
 * <p>
 * In most circumstances this class is only ever used for:
 * a). testing and/or b) those very rare occasions that you need a
 * {@link ParameterResolver} and want all parameters resolved to <code>null</code>.
 * <p>
 * <strong>NOTE:</strong> This class does not provide a static INSTANCE
 * declaration by design. Developers are not meant to use this class very often
 * and hence we discourage this by not providing an INSTANCE declaration.
 * <p>
 *
 * @author bo  2011.09.27
 * @since Coherence 12.1.2
 */
public class NullParameterResolver
        implements ParameterResolver, ExternalizableLite, PortableObject
    {
    /**
     * Default constructor needed for serialization.
     */
    public NullParameterResolver()
        {
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Parameter resolve(String sName)
        {
        return null;
        }

    // ----- ExternalizableLite interface -----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
        {
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        }

    // ----- PortableObject interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
        {
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
        {
        }
    }
