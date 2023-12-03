/*
 * Copyright (c) 2016, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.options;

import com.oracle.coherence.concurrent.executor.ClusteredRegistration;
import com.oracle.coherence.concurrent.executor.TaskExecutorService;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * An {@link TaskExecutorService.Registration.Option option} to signal
 * the registered {@link java.util.concurrent.ExecutorService} should be
 * {@link java.util.concurrent.ExecutorService#shutdown() terminated} when
 * the clustered registration is {@link ClusteredRegistration#close() closed}.
 *
 * @author rl 11.17.2023
 * @since 22.06.7
 */
public class CloseExecutor
        implements TaskExecutorService.Registration.Option, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link CloseExecutor} option.
     */
    @SuppressWarnings("unused")
    public CloseExecutor()
        {
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
    public void readExternal(PofReader in) throws IOException
        {
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        }
    }
