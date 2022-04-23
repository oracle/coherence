/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;

public class GetProcessor
        extends AbstractProcessor
    {
    /**
     * Constructs a {@link GetProcessor}.
     */
    public GetProcessor()
        {
        }


    @Override
    public Object process(InvocableMap.Entry entry)
        {
        return entry.isPresent() ? entry.getValue() : null;
        }
    }
