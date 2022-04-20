/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.lang.java;


import com.oracle.coherence.common.schema.CanonicalTypeDescriptor;
import com.oracle.coherence.common.schema.Schema;
import com.oracle.coherence.common.schema.SchemaBuilder;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;


/**
 * @author jk  2017.03.13
 */
public class JavaTypeDescriptorTest
    {
    @Test
    public void shouldReturnLocalDateType() throws Exception
        {
        Schema                  schema = new SchemaBuilder().build();
        CanonicalTypeDescriptor ctd    = CanonicalTypeDescriptor.parse("LocalDate");

        JavaTypeDescriptor jtd = JavaTypeDescriptor.from(ctd, schema);

        assertThat(jtd, is(notNullValue()));
        assertThat(jtd.getNamespace(), is("java.time"));
        assertThat(jtd.getName(), is("LocalDate"));
        assertThat(jtd.isArray(), is(false));
        assertThat(jtd.getGenericArguments(), is(nullValue()));
        }

    @Test
    public void shouldReturnLocalDateTimeType() throws Exception
        {
        Schema                  schema = new SchemaBuilder().build();
        CanonicalTypeDescriptor ctd    = CanonicalTypeDescriptor.parse("LocalDateTime");

        JavaTypeDescriptor jtd = JavaTypeDescriptor.from(ctd, schema);

        assertThat(jtd, is(notNullValue()));
        assertThat(jtd.getNamespace(), is("java.time"));
        assertThat(jtd.getName(), is("LocalDateTime"));
        assertThat(jtd.isArray(), is(false));
        assertThat(jtd.getGenericArguments(), is(nullValue()));
        }

    @Test
    public void shouldReturnOffsetDateTimeType() throws Exception
        {
        Schema                  schema = new SchemaBuilder().build();
        CanonicalTypeDescriptor ctd    = CanonicalTypeDescriptor.parse("OffsetDateTime");

        JavaTypeDescriptor jtd = JavaTypeDescriptor.from(ctd, schema);

        assertThat(jtd, is(notNullValue()));
        assertThat(jtd.getNamespace(), is("java.time"));
        assertThat(jtd.getName(), is("OffsetDateTime"));
        assertThat(jtd.isArray(), is(false));
        assertThat(jtd.getGenericArguments(), is(nullValue()));
        }

    @Test
    public void shouldReturnOffsetTimeType() throws Exception
        {
        Schema                  schema = new SchemaBuilder().build();
        CanonicalTypeDescriptor ctd    = CanonicalTypeDescriptor.parse("OffsetTime");

        JavaTypeDescriptor jtd = JavaTypeDescriptor.from(ctd, schema);

        assertThat(jtd, is(notNullValue()));
        assertThat(jtd.getNamespace(), is("java.time"));
        assertThat(jtd.getName(), is("OffsetTime"));
        assertThat(jtd.isArray(), is(false));
        assertThat(jtd.getGenericArguments(), is(nullValue()));
        }

    @Test
    public void shouldReturnZonedDateTimeType() throws Exception
        {
        Schema                  schema = new SchemaBuilder().build();
        CanonicalTypeDescriptor ctd    = CanonicalTypeDescriptor.parse("ZonedDateTime");

        JavaTypeDescriptor jtd = JavaTypeDescriptor.from(ctd, schema);

        assertThat(jtd, is(notNullValue()));
        assertThat(jtd.getNamespace(), is("java.time"));
        assertThat(jtd.getName(), is("ZonedDateTime"));
        assertThat(jtd.isArray(), is(false));
        assertThat(jtd.getGenericArguments(), is(nullValue()));
        }
    }
