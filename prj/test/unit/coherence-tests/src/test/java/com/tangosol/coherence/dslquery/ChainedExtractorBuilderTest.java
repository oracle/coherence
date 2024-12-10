/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery;

import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.AbstractExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;
import org.junit.Test;
import org.mockito.InOrder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author jk 2014.07.15
 */
public class ChainedExtractorBuilderTest
    {
    @Test
    public void shouldReturnNullFromEmptyChain()
            throws Exception
        {
        ChainedExtractorBuilder builder = new ChainedExtractorBuilder();

        assertThat(builder.realize("test", AbstractExtractor.VALUE, "foo"), is(nullValue()));
        }

    @Test
    public void shouldDelegateToChain()
            throws Exception
        {
        ExtractorBuilder        builder1       = mock(ExtractorBuilder.class);
        ExtractorBuilder        builder2       = mock(ExtractorBuilder.class);
        ChainedExtractorBuilder chainedBuilder = new ChainedExtractorBuilder(builder1, builder2);

        when(builder1.realize(anyString(), anyInt(), anyString())).thenReturn(null);
        when(builder2.realize(anyString(), anyInt(), anyString())).thenReturn(null);

        assertThat(chainedBuilder.realize("test", AbstractExtractor.VALUE, "foo"), is(nullValue()));

        InOrder inOrder = inOrder(builder1, builder2);

        inOrder.verify(builder1).realize("test", AbstractExtractor.VALUE, "foo");
        inOrder.verify(builder2).realize("test", AbstractExtractor.VALUE, "foo");
        }

    @Test
    public void shouldReturnFirstNonNullResult()
            throws Exception
        {
        ExtractorBuilder        builder1       = mock(ExtractorBuilder.class);
        ExtractorBuilder        builder2       = mock(ExtractorBuilder.class);
        ExtractorBuilder        builder3       = mock(ExtractorBuilder.class);
        ChainedExtractorBuilder chainedBuilder = new ChainedExtractorBuilder(builder1, builder2);

        when(builder1.realize(anyString(), anyInt(), anyString())).thenReturn(null);
        when(builder2.realize(anyString(), anyInt(), anyString())).thenReturn(new ReflectionExtractor("from2"));
        when(builder3.realize(anyString(), anyInt(), anyString())).thenReturn(new ReflectionExtractor("from3"));

        assertThat(chainedBuilder.realize("test", AbstractExtractor.VALUE, "foo"),
                   is((ValueExtractor) new ReflectionExtractor("from2")));

        InOrder inOrder = inOrder(builder1, builder2);

        inOrder.verify(builder1).realize("test", AbstractExtractor.VALUE, "foo");
        inOrder.verify(builder2).realize("test", AbstractExtractor.VALUE, "foo");
        verify(builder3, never()).realize(anyString(), anyInt(), anyString());
        }
    }
