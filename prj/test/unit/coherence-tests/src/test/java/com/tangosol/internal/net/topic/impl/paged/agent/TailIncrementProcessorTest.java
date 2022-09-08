/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.agent;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicDependencies;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicPartition;
import com.tangosol.internal.net.topic.impl.paged.model.Usage;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.ArgumentMatchers.same;
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
        {
        ConfigurablePofContext serializer = new ConfigurablePofContext("coherence-pof-config.xml");
        TailAdvancer           processor  = new TailAdvancer(123);
        Binary                 binary     = ExternalizableHelper.toBinary(processor, serializer);
        TailAdvancer           result     = ExternalizableHelper.fromBinary(binary, serializer);
        assertThat(result.getNewTail(), is(processor.getNewTail()));

        // validate Evolvable version across pof serialization
        assertThat(result.getDataVersion(), is(processor.getImplVersion()));
        }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldReturnUnmodifiedInfoIfTailMismatch()
        {
        PagedTopicDependencies        dependencies        = mock(PagedTopicDependencies.class);
        long                          nInfoTail           = 5L;
        long                          nProcTail           = 4L;
        Usage                         info                = new Usage();
        BinaryEntry<Usage.Key, Usage> entry               = mock(BinaryEntry.class);
        TailAdvancer                  processor           = new TailAdvancer(nProcTail);
        TailAdvancer                  processorSpy        = spy(processor);
        PagedTopicPartition           pagedTopicPartition = mock(PagedTopicPartition.class);

        doReturn(pagedTopicPartition).when(processorSpy).ensureTopic(same(entry));

        when(pagedTopicPartition.getDependencies()).thenReturn(dependencies);
        when(entry.getValue()).thenReturn(info);

        info.setPublicationTail(nInfoTail);

        Long result = processorSpy.process(entry);

        assertThat(result, is(notNullValue()));
        assertThat(result, is(nInfoTail));
        }
    }
