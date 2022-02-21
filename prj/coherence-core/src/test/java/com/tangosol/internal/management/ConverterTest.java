/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.management;

import java.text.SimpleDateFormat;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;

import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import com.tangosol.internal.management.Converter;
import org.junit.Test;

import static org.junit.Assert.*;

import static org.hamcrest.CoreMatchers.*;

/**
 * @author shyaradh 8/23/17
 */
@SuppressWarnings("unchecked")
public class ConverterTest
    {
    @Test
    public void convertDate() throws Exception
        {
        Date date = Calendar.getInstance().getTime();
        Object converted = Converter.convert(date);
        assertThat(converted, notNullValue());
        assertThat(converted, instanceOf(String.class));
        SimpleDateFormat formatter = new SimpleDateFormat(Converter.ISO_8601_DATE_TIME_FORMAT);
        assertThat(formatter.parse((String) converted), is(date));
        }

    @Test
    public void convertIntSummaryStatistics() throws Exception
        {
        IntSummaryStatistics stats = new IntSummaryStatistics();
        stats.accept(1);
        stats.accept(2);
        Object converted = Converter.convert(stats);
        assertThat(converted, notNullValue());
        assertThat(converted, instanceOf(Map.class));

        Map<String, Object> map = (Map<String, Object>) converted;
        assertThat(map.get("sum"), is(3L));
        assertThat(map.get("average"), is(1.5));
        assertThat(map.get("min"), is(1));
        assertThat(map.get("max"), is(2));
        assertThat(map.get("count"), is(2L));
        }

    @Test
    public void convertArrayStrings() throws Exception
        {
        String[] array = {"a", "b", "c"};
        Object converted = Converter.convert(array);
        assertThat(converted, instanceOf(List.class));

        List<Object> list = (List<Object>) converted;
        assertThat(list.size(), is(array.length));
        assertThat(list, is(Arrays.asList(array)));
        }

    @Test
    public void convertArrayIntSummaryStatistics() throws Exception
        {
        IntSummaryStatistics stats1 = new IntSummaryStatistics();
        stats1.accept(1);
        stats1.accept(2);

        IntSummaryStatistics stats2 = new IntSummaryStatistics();
        stats2.accept(3);
        stats2.accept(4);

        IntSummaryStatistics[] array = {stats1, stats2};
        Object converted = Converter.convert(array);
        assertThat(converted, instanceOf(List.class));
        List<Map<String, Object>> list = (List<Map<String, Object>>) converted;
        assertThat(list.size(), is(2));

        Map<String, Object> map = list.get(0);
        assertThat(map.get("sum"), is(3L));
        assertThat(map.get("average"), is(1.5));
        assertThat(map.get("min"), is(1));
        assertThat(map.get("max"), is(2));
        assertThat(map.get("count"), is(2L));

        map = list.get(1);
        assertThat(map.get("sum"), is(7L));
        assertThat(map.get("average"), is(3.5));
        assertThat(map.get("min"), is(3));
        assertThat(map.get("max"), is(4));
        assertThat(map.get("count"), is(2L));
        }

    @Test
    public void convertCompositeData() throws Exception
        {
        String[] itemNames = {"a", "b"};
        CompositeType compositeType = new CompositeType("name", "desc", itemNames, new String[]{"desca", "descb"},
                new OpenType[]{SimpleType.STRING, SimpleType.STRING});

        String[] itemValues = {"vala", "valb"};
        CompositeDataSupport support = new CompositeDataSupport(compositeType, itemNames, itemValues);
        Object converted = Converter.convert(support);
        assertThat(converted, instanceOf(Map.class));

        Map<String, Object> map = (Map<String, Object>) converted;
        assertThat(map.get("a"), is("vala"));
        assertThat(map.get("b"), is("valb"));
        }
    }