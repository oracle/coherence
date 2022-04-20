/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.statement;

/**
 * @author jk  2013.12.19
 */
public class DummyValueWithKey
        extends DummyValue
    {
    public DummyValueWithKey(String key, String fieldOne)
        {
        super(key, fieldOne);
        }

    public String getKey()
        {
        return getId();
        }
    }
