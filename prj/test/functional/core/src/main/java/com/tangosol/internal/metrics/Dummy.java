/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.metrics;

/**
 * A dummy custom MBean.
 *
 * @author jk  2019.06.25
 */
public class Dummy
        implements DummyMBean
    {
    @Override
    public long getValueOne()
        {
        return 100;
        }

    @Override
    public long getValueTwo()
        {
        return 200;
        }

    @Override
    public long getValueThree()
        {
        return 300;
        }

    @Override
    public String getTagValueOne()
        {
        return "TagOne";
        }

    @Override
    public String getTagValueTwo()
        {
        return "TagTwo";
        }
    }
