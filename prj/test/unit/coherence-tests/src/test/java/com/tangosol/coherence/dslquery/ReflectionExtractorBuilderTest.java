/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery;

import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.AbstractExtractor;
import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author jk 2014.07.15
 */
public class ReflectionExtractorBuilderTest
    {
    @Test
    public void shouldBuildSimpleReflectionExtractor()
            throws Exception
        {
        ReflectionExtractorBuilder builder   = new ReflectionExtractorBuilder();
        ValueExtractor             extractor = builder.realize("foo", AbstractExtractor.VALUE, "bar");

        assertThat(extractor, is((ValueExtractor) new ReflectionExtractor("getBar")));
        }

    @Test
    public void shouldBuildSimpleReflectionExtractorTargetingKey()
            throws Exception
        {
        ReflectionExtractorBuilder builder   = new ReflectionExtractorBuilder();
        ValueExtractor             extractor = builder.realize("foo", AbstractExtractor.KEY, "bar");

        assertThat(extractor, is((ValueExtractor) new ReflectionExtractor("getBar", null, AbstractExtractor.KEY)));
        }

    @Test
    public void shouldBuildChainedExtractor()
            throws Exception
        {
        ReflectionExtractorBuilder builder   = new ReflectionExtractorBuilder();
        ValueExtractor             extractor = builder.realize("test", AbstractExtractor.VALUE, "foo.bar");

        assertThat(extractor,
                   is((ValueExtractor) new ChainedExtractor(new ReflectionExtractor("getFoo", null,
                       AbstractExtractor.VALUE), new ReflectionExtractor("getBar", null, AbstractExtractor.VALUE))));
        }

    @Test
    public void shouldBuildChainedExtractorTargetingKey()
            throws Exception
        {
        ReflectionExtractorBuilder builder   = new ReflectionExtractorBuilder();
        ValueExtractor             extractor = builder.realize("test", AbstractExtractor.KEY, "foo.bar");

        assertThat(extractor,
                   is((ValueExtractor) new ChainedExtractor(new ReflectionExtractor("getFoo", null,
                       AbstractExtractor.KEY), new ReflectionExtractor("getBar", null, AbstractExtractor.VALUE))));
        }
    }
