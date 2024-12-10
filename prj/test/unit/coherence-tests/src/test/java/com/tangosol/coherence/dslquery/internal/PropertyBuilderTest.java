/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.internal;

import com.tangosol.util.Extractors;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;
import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.CompositeUpdater;
import com.tangosol.util.extractor.UniversalExtractor;
import com.tangosol.util.extractor.UniversalUpdater;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author jk  2013.12.09
 */
public class PropertyBuilderTest
    {
    @Test
    public void shouldCreateExtractorForSimpleString()
            throws Exception
        {
        String          name      = "foo";
        ValueExtractor  expected  = Extractors.extract("foo");
        PropertyBuilder builder   = new PropertyBuilder();
        ValueExtractor  extractor = builder.extractorFor(name);

        assertThat(extractor, is(expected));
        }

    @Test
    public void shouldCreateExtractorForSimpleStringMatchingIgnorePrefix()
            throws Exception
        {
        String          name      = "foo";
        ValueExtractor  expected  = new ChainedExtractor(new ValueExtractor[0]);
        PropertyBuilder builder   = new PropertyBuilder("foo");
        ValueExtractor  extractor = builder.extractorFor(name);

        assertThat(extractor, is(expected));
        }

    @Test
    public void shouldCreateExtractorForDottedString()
            throws Exception
        {
        String          name      = "foo1.foo2.bar";
        ValueExtractor  expected  = Extractors.chained("foo1.foo2.bar");
        PropertyBuilder builder   = new PropertyBuilder();
        ValueExtractor  extractor = builder.extractorFor(name);

        assertThat(extractor, is(expected));
        }

    @Test
    public void shouldCreateExtractorForDottedStringIgnoringPrefix()
            throws Exception
        {
        String          name      = "foo1.foo2.bar";
        ValueExtractor  expected  = Extractors.chained("foo2.bar");
        PropertyBuilder builder   = new PropertyBuilder("foo1");
        ValueExtractor  extractor = builder.extractorFor(name);

        assertThat(extractor, is(expected));
        }

    @Test
    public void shouldCreateExtractorForStringArray()
            throws Exception
        {
        String[]        name      = new String[] {"foo1", "foo2", "bar"};
        ValueExtractor  expected  = Extractors.chained("foo1.foo2.bar");
        PropertyBuilder builder   = new PropertyBuilder();
        ValueExtractor  extractor = builder.extractorFor(name);

        assertThat(extractor, is(expected));
        }

    @Test
    public void shouldCreateExtractorForStringArrayIgnoringPrefix()
            throws Exception
        {
        String[]        name      = new String[] {"foo1", "foo2", "bar"};
        ValueExtractor  expected  = Extractors.chained("foo2.bar");
        PropertyBuilder builder   = new PropertyBuilder("foo1");
        ValueExtractor  extractor = builder.extractorFor(name);

        assertThat(extractor, is(expected));
        }

    @Test
    public void shouldGetExtractorStringForSimpleString()
            throws Exception
        {
        String          name    = "foo";
        PropertyBuilder builder = new PropertyBuilder();

        assertThat(builder.extractorStringFor(name), is("getFoo()"));
        }

    @Test
    public void shouldGetExtractorStringForSimpleStringMatchingIgnorePrefix()
            throws Exception
        {
        String          name    = "foo";
        PropertyBuilder builder = new PropertyBuilder("foo");

        assertThat(builder.extractorStringFor(name), is(""));
        }

    @Test
    public void shouldGetExtractorStringForDottedString()
            throws Exception
        {
        String          name    = "foo1.foo2.bar";
        PropertyBuilder builder = new PropertyBuilder();

        assertThat(builder.extractorStringFor(name), is("getFoo1().getFoo2().getBar()"));
        }

    @Test
    public void shouldMakeSimpleNameFromStringNotStartingWithPrefix()
            throws Exception
        {
        String          prefix  = "foo";
        String          name    = "bar";
        PropertyBuilder builder = new PropertyBuilder();

        assertThat(builder.makeSimpleName(prefix, name), is("fooBar()"));
        }

    @Test
    public void shouldMakeSimpleNameFromLowerCaseStringStartingWithPrefix()
            throws Exception
        {
        String          prefix  = "foo";
        String          name    = "foobar";
        PropertyBuilder builder = new PropertyBuilder();

        assertThat(builder.makeSimpleName(prefix, name), is("fooBar()"));
        }

    @Test
    public void shouldMakeSimpleNameFromCamelCaseStringStartingWithPrefix()
            throws Exception
        {
        String          prefix  = "foo";
        String          name    = "fooBar";
        PropertyBuilder builder = new PropertyBuilder();

        assertThat(builder.makeSimpleName(prefix, name), is("fooBar()"));
        }

    @Test
    public void shouldMakeSimpleNameFromStartingSameAsPrefix()
            throws Exception
        {
        String          prefix  = "foo";
        String          name    = "foo";
        PropertyBuilder builder = new PropertyBuilder();

        assertThat(builder.makeSimpleName(prefix, name), is("foo"));
        }

    @Test
    public void shouldGetPlainNameForStringNotPrefixedWithGetOrSetWithLowerCaseLeadingChar()
            throws Exception
        {
        String          name    = "foo";
        PropertyBuilder builder = new PropertyBuilder();

        assertThat(builder.plainName(name), is("foo"));
        }

    @Test
    public void shouldGetPlainNameForStringNotPrefixedWithGetOrSetWithUpperCaseLeadingChar()
            throws Exception
        {
        String          name    = "Foo";
        PropertyBuilder builder = new PropertyBuilder();

        assertThat(builder.plainName(name), is("foo"));
        }

    @Test
    public void shouldGetPlainNameForStringPrefixedWithGet()
            throws Exception
        {
        String          name    = "foo";
        PropertyBuilder builder = new PropertyBuilder();

        assertThat(builder.plainName(name), is("foo"));
        }

    @Test
    public void shouldGetPlainNameForStringPrefixedWithSet()
            throws Exception
        {
        String          name    = "setFoo";
        PropertyBuilder builder = new PropertyBuilder();

        assertThat(builder.plainName(name), is("foo"));
        }

    @Test
    public void shouldGetPropertyStringForSimpleString()
            throws Exception
        {
        String          name    = "foo";
        PropertyBuilder builder = new PropertyBuilder();

        assertThat(builder.propertyStringFor(name), is("foo"));
        }

    @Test
    public void shouldGetPropertyStringForSimpleStringMatchingIgnorePrefix()
            throws Exception
        {
        String          name    = "foo";
        PropertyBuilder builder = new PropertyBuilder("foo");

        assertThat(builder.propertyStringFor(name), is(""));
        }

    @Test
    public void shouldGetPropertyStringForDottedString()
            throws Exception
        {
        String          name    = "foo.bar";
        PropertyBuilder builder = new PropertyBuilder();

        assertThat(builder.propertyStringFor(name), is("foo.bar"));
        }

    @Test
    public void shouldGetPropertyStringForDottedStringWithIgnorePrefix()
            throws Exception
        {
        String          name    = "foo.foo2.bar";
        PropertyBuilder builder = new PropertyBuilder("foo");

        assertThat(builder.propertyStringFor(name), is("foo2.bar"));
        }

    @Test
    public void shouldSplitNullString()
            throws Exception
        {
        PropertyBuilder builder = new PropertyBuilder();

        assertThat(builder.splitString(null, '.'), is(nullValue()));
        }

    @Test
    public void shouldSplitEmptyString()
            throws Exception
        {
        PropertyBuilder builder = new PropertyBuilder();

        assertThat(builder.splitString("", '.'), is(new String[] {""}));
        }

    @Test
    public void shouldSplitStringWithoutDelimiter()
            throws Exception
        {
        PropertyBuilder builder = new PropertyBuilder();

        assertThat(builder.splitString("foo", '.'), is(new String[] {"foo"}));
        }

    @Test
    public void shouldSplitStringWithDelimiter()
            throws Exception
        {
        PropertyBuilder builder = new PropertyBuilder();

        assertThat(builder.splitString("one.two.three", '.'), is(new String[] {"one", "two", "three"}));
        }

    @Test
    public void shouldCreateUniformArrayForEmptyInput()
            throws Exception
        {
        String[]        input    = new String[0];
        String          prefix   = "foo";
        String[]        expected = new String[0];

        PropertyBuilder builder  = new PropertyBuilder();

        assertThat(builder.uniformArrayFor(input, prefix), is(expected));
        }

    @Test
    public void shouldCreateUniformArray()
            throws Exception
        {
        String[]        input    = new String[] {"one", "two", "three"};
        String          prefix   = "foo";
        String[]        expected = new String[] {"fooOne()", "fooTwo()", "fooThree()"};

        PropertyBuilder builder  = new PropertyBuilder();

        assertThat(builder.uniformArrayFor(input, prefix), is(expected));
        }

    @Test
    public void shouldCreateUniformArrayIgnoringPrefix()
            throws Exception
        {
        String[]        input    = new String[] {"bar", "one", "two", "three"};
        String          prefix   = "foo";
        String[]        expected = new String[] {"fooOne()", "fooTwo()", "fooThree()"};

        PropertyBuilder builder  = new PropertyBuilder("bar");

        assertThat(builder.uniformArrayFor(input, prefix), is(expected));
        }

    @Test
    public void shouldCreateUniformStringForEmptyArray()
            throws Exception
        {
        String[]        input   = new String[0];

        PropertyBuilder builder = new PropertyBuilder("bar");

        assertThat(builder.uniformStringFor(input), is(""));
        }

    @Test
    public void shouldCreateUniformStringForArray()
            throws Exception
        {
        String[]        input   = new String[] {"one", "two", "three"};

        PropertyBuilder builder = new PropertyBuilder("bar");

        assertThat(builder.uniformStringFor(input), is("one.two.three"));
        }

    @Test
    public void shouldCreateUpdaterForSimpleString()
            throws Exception
        {
        String          name     = "foo";
        ValueUpdater    expected = new UniversalUpdater("foo");
        PropertyBuilder builder  = new PropertyBuilder();

        assertThat(builder.updaterFor(name), is(expected));
        }

    @Test
    public void shouldCreateUpdaterForDottedString()
            throws Exception
        {
        String       name     = "foo.bar1.bar2";
        ValueUpdater expected = new CompositeUpdater(Extractors.chained("foo.bar1"),
                                    new UniversalUpdater("bar2"));
        PropertyBuilder builder = new PropertyBuilder();

        assertThat(builder.updaterFor(name), is(expected));
        }

    @Test
    public void shouldCreateUpdaterForDottedStringWithIgnoredPrefix()
            throws Exception
        {
        String          name     = "foo.bar1.bar2";
        ValueUpdater    expected = new CompositeUpdater(Extractors.chained("bar1"),
                                       new UniversalUpdater("bar2"));
        PropertyBuilder builder  = new PropertyBuilder("foo");

        assertThat(builder.updaterFor(name), is(expected));
        }

    @Test
    public void shouldCreateUpdaterForStringArray()
            throws Exception
        {
        String[]        names    = new String[] {"foo", "bar1", "bar2"};
        PropertyBuilder builder  = new PropertyBuilder();
        ValueUpdater    expected = new CompositeUpdater(Extractors.chained("foo.bar1"),
                                       new UniversalUpdater("bar2"));

        assertThat(builder.updaterFor(names), is(expected));
        }

    @Test
    public void shouldCreateUpdaterForStringArrayWithIgnorePrefix()
            throws Exception
        {
        String[]        names    = new String[] {"foo", "bar1", "bar2"};
        PropertyBuilder builder  = new PropertyBuilder("foo");
        ValueUpdater    expected = new CompositeUpdater(Extractors.chained("bar1"),
                                       new UniversalUpdater("bar2"));

        assertThat(builder.updaterFor(names), is(expected));
        }

    @Test
    public void shouldGetUpdaterStringForSimpleString()
            throws Exception
        {
        String          name    = "foo";
        PropertyBuilder builder = new PropertyBuilder();

        assertThat(builder.updaterStringFor(name), is("setFoo()"));
        }

    @Test
    public void shouldUpdaterStringForSimpleStringMatchingIgnorePrefix()
            throws Exception
        {
        String          name    = "foo";
        PropertyBuilder builder = new PropertyBuilder("foo");

        assertThat(builder.updaterStringFor(name), is(""));
        }

    @Test
    public void shouldGetUpdaterStringForDottedString()
            throws Exception
        {
        String          name    = "foo1.foo2.bar";
        PropertyBuilder builder = new PropertyBuilder();

        assertThat(builder.updaterStringFor(name), is("setFoo1().setFoo2().setBar()"));
        }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();
    }
