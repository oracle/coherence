/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.agent;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author jf 2019.11.20
 */
public class TailAdvancerTest
    {
    @Test
    public void shouldSerializeUsingPof()
        throws Exception
        {
        ConfigurablePofContext serializer   = new ConfigurablePofContext("coherence-pof-config.xml");
        TailAdvancer tailAdvancer = new TailAdvancer(10);

        Binary       binary = ExternalizableHelper.toBinary(tailAdvancer, serializer);
        TailAdvancer result = ExternalizableHelper.fromBinary(binary, serializer);

        assertThat(result.getNewTail(), is(tailAdvancer.getNewTail()));
        assertThat(result.getDataVersion(), is(tailAdvancer.getImplVersion()));
        }
    }
