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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author jk 2015.06.19
 */
public class TopicInitialiseProcessorTest
    {
    @Test
    public void shouldSerializeWithPof()
            throws Exception
        {
        ConfigurablePofContext   serializer = new ConfigurablePofContext("coherence-pof-config.xml");
        TopicInitialiseProcessor processor  = new TopicInitialiseProcessor();
        Binary                   binary     = ExternalizableHelper.toBinary(processor, serializer);
        TopicInitialiseProcessor result     = ExternalizableHelper.fromBinary(binary, serializer);

        assertThat(result, is(instanceOf(TopicInitialiseProcessor.class)));

        // validate Evolvable version across pof serialization
        assertThat(result.getDataVersion(), is(processor.getImplVersion()));
        }
    }
