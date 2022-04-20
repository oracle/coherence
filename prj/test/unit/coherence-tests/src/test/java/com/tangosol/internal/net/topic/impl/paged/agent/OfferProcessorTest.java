/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.agent;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicPartition;
import com.tangosol.internal.net.topic.impl.paged.model.Page;
import com.tangosol.internal.net.topic.impl.paged.model.Page.Key;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author jk 2015.05.19
 */
@SuppressWarnings("unchecked")
public class OfferProcessorTest
    {
    @Test
    public void shouldSerializeWithPof()
            throws Exception
        {
        Binary                  binary1   = new Binary("1".getBytes());
        Binary                  binary2   = new Binary("2".getBytes());
        LinkedList<Binary>      elements  = new LinkedList<>(Arrays.asList(binary1, binary2));
        OfferProcessor processor = new OfferProcessor(elements, 0, true);
        ConfigurablePofContext  ctx       = new ConfigurablePofContext("coherence-pof-config.xml");
        Binary                  binary    = ExternalizableHelper.toBinary(processor, ctx);
        OfferProcessor result    = (OfferProcessor) ExternalizableHelper.fromBinary(binary, ctx);

        assertThat(result.m_listValues, is(elements));
        assertThat(result.m_fSealPage, is(true));

        // validate Evolvable version across pof serialization
        assertThat(result.getDataVersion(), is(processor.getImplVersion()));
        assertNotNull(processor.getElements());
        }

    @Test
    public void shouldOfferToTailOfQueue()
            throws Exception
        {
        Binary                         binary1          = new Binary("1".getBytes());
        Binary                         binary2          = new Binary("2".getBytes());
        LinkedList<Binary>             elements         = new LinkedList<>(Arrays.asList(binary1, binary2));
        PagedTopicPartition pagedTopicPartition = mock(PagedTopicPartition.class);
        BinaryEntry<Key, Page> entry            = mock(BinaryEntry.class);
        OfferProcessor.Result offerResult      = mock(OfferProcessor.Result.class);

        when(entry.getKey()).thenReturn(new Page.Key(0, 1234L));
        when(pagedTopicPartition.offerToPageTail(entry, elements, 0, true)).thenReturn(offerResult);

        OfferProcessor processor = new OfferProcessor(elements, 0, true, (e) -> pagedTopicPartition);

        OfferProcessor.Result result = processor.process(entry);

        assertThat(result, is(sameInstance(offerResult)));
        verify(pagedTopicPartition).offerToPageTail(entry, elements, 0, true);
        }
    }
