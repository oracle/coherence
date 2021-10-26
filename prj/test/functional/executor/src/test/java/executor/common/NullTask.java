/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package executor.common;

import com.oracle.coherence.concurrent.executor.PortableTask;

/**
 * A {@link PortableTask} that returns <code>null</code>.
 *
 * @author bo
 * @since 21.12
 */
public class NullTask<T>
        implements PortableTask<T>
    {
    // ----- PortableTask interface -----------------------------------------

    @Override
    public T execute(Context<T> context)
        {
        return null;
        }
    }
