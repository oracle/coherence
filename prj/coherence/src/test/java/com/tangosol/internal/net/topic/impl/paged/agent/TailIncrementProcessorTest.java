/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.agent;

import com.tangosol.internal.net.topic.impl.paged.Configuration;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicPartition;
import com.tangosol.internal.net.topic.impl.paged.model.Usage;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author jk 2015.05.20
 */
public class TailIncrementProcessorTest
    {
    @Test
    public void shouldSerializeUsingPof()
            throws Exception
        {
        ConfigurablePofContext serializer = new ConfigurablePofContext("coherence-pof-config.xml");
        TailAdvancer           processor  = new TailAdvancer(123);
        Binary                 binary     = ExternalizableHelper.toBinary(processor, serializer);
        TailAdvancer           result     = (TailAdvancer) ExternalizableHelper.fromBinary(binary,
                                                     serializer);
        assertThat(result.getNewTail(), is(processor.getNewTail()));

        // validate Evolvable version across pof serialization
        assertThat(result.getDataVersion(), is(processor.getImplVersion()));
        }

    @Test
    public void shouldReturnUnmodifiedInfoIfTailMismatch()
            throws Exception
        {
        Configuration                 configuration       = new Configuration();
        long                          nInfoTail           = 5L;
        long                          nProcTail           = 4L;
        Usage                         info                = new Usage();
        BinaryEntry<Usage.Key, Usage> entry               = mock(BinaryEntry.class);
        TailAdvancer                  processor           = new TailAdvancer(nProcTail);
        TailAdvancer                  processorSpy        = spy(processor);
        PagedTopicPartition           pagedTopicPartition = mock(PagedTopicPartition.class);

        doReturn(pagedTopicPartition).when(processorSpy).ensureTopic(same(entry));

        when(pagedTopicPartition.getTopicConfiguration()).thenReturn(configuration);
        when(entry.getValue()).thenReturn(info);

        info.setPublicationTail(nInfoTail);

        Long result = processorSpy.process(entry);

        assertThat(result, is(notNullValue()));
        assertThat(result, is(nInfoTail));
        }
    }
