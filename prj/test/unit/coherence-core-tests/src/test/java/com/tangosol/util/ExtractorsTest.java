/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;

import com.tangosol.io.pof.reflect.SimplePofPath;
import com.tangosol.util.extractor.AbstractExtractor;
import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.PofExtractor;
import com.tangosol.util.extractor.UniversalExtractor;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ExtractorsTest
    {
    @Test
    public void shouldCreatePofExtractor()
        {
        PofExtractor<?, ?> extractor = Extractors.fromPof(1, 4, 19);
        assertThat(extractor, is(notNullValue()));
        assertThat(extractor.getNavigator(), is(new SimplePofPath(new int[]{1, 4, 19})));
        assertThat(extractor.getTarget(), is(AbstractExtractor.VALUE));
        }

    @Test
    public void shouldCreatePofExtractorFromKey()
        {
        PofExtractor<?, ?> pofExtractor = Extractors.fromPof(1, 4, 19);
        ValueExtractor<?, ?> extractor = Extractors.key(pofExtractor);
        assertThat(extractor, is(instanceOf(PofExtractor.class)));
        assertThat(((PofExtractor<?, ?>)extractor).getNavigator(), is(new SimplePofPath(new int[]{1, 4, 19})));
        assertThat(extractor.getTarget(), is(AbstractExtractor.KEY));
        }

    @Test
    public void shouldCreateUniversalExtractor()
        {
        ValueExtractor<?, ?> extractor = Extractors.extract("foo");
        assertThat(extractor, is(instanceOf(UniversalExtractor.class)));
        assertThat(((UniversalExtractor<?, ?>) extractor).getMethodName(), is("getFoo"));
        assertThat(extractor.getTarget(), is(AbstractExtractor.VALUE));
        }

    @Test
    public void shouldCreateUniversalExtractorFromKey()
        {
        ValueExtractor<?, ?> extractor = Extractors.key("foo");
        assertThat(extractor, is(instanceOf(UniversalExtractor.class)));
        assertThat(((UniversalExtractor<?, ?>) extractor).getMethodName(), is("getFoo"));
        assertThat(extractor.getTarget(), is(AbstractExtractor.KEY));
        }

    @Test
    public void shouldCreateChainedExtractorFromFields()
        {
        ValueExtractor   foo     = Extractors.extract("foo");
        ValueExtractor   bar     = Extractors.extract("bar");
        ChainedExtractor chained = new ChainedExtractor(foo, bar);

        ValueExtractor<?, ?> extractor = Extractors.chained("foo", "bar");
        assertThat(extractor, is(chained));
        }

    @Test
    public void shouldCreateChainedExtractorFromFieldsForKey()
        {
        ValueExtractor   foo     = Extractors.extract("foo");
        ValueExtractor   bar     = Extractors.extract("bar");
        ChainedExtractor chained = new ChainedExtractor(foo, bar);

        ValueExtractor<?, ?> extractor = Extractors.key("foo", "bar");
        assertThat(extractor, is(chained.fromKey()));
        }

    @Test
    public void shouldCreateChainedExtractorFromExtractors()
        {
        ValueExtractor   foo     = Extractors.extract("foo");
        ValueExtractor   bar     = Extractors.extract("bar");
        ChainedExtractor chained = new ChainedExtractor(foo, bar);

        ValueExtractor<?, ?> extractor = Extractors.chained(foo, bar);
        assertThat(extractor, is(chained));
        }

    @Test
    public void shouldCreateChainedExtractorFromExtractorsForKey()
        {
        ValueExtractor   foo     = Extractors.extract("foo");
        ValueExtractor   bar     = Extractors.extract("bar");
        ChainedExtractor chained = new ChainedExtractor(foo, bar);

        ValueExtractor<?, ?> extractor = Extractors.key(foo, bar);
        assertThat(extractor, is(chained.fromKey()));
        }
    }
