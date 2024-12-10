/*
 * Copyright (c) 2016, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.options;

import com.oracle.coherence.concurrent.executor.TaskExecutorService;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.concurrent.Executor;

/**
 * An {@link TaskExecutorService.Registration.Option} specifying whether an {@link Executor} is running on a Coherence
 * cluster member.
 * <p>
 * The presence of this Option indicates that it is a cluster member.
 *
 * @author phf
 * @since 21.12
 */
public class ClusterMember
        implements TaskExecutorService.Registration.Option, PortableObject
    {
    // ----- Object methods -------------------------------------------------

    @Override
    public int hashCode()
        {
        return 1;
        }

    @Override
    public boolean equals(Object obj)
        {
        return obj instanceof ClusterMember;
        }

    @Override
    public String toString()
        {
        return "ClusterMember{}";
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader pofReader) throws IOException
        {
        }

    @Override
    public void writeExternal(PofWriter pofWriter) throws IOException
        {
        }

    // ----- constants ------------------------------------------------------

    /**
     * Static {@code ClusterMember} instance.
     */
    public static ClusterMember INSTANCE = new ClusterMember();
    }
