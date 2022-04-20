/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.dev.tools;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;

import static org.junit.Assert.assertThat;

/**
 * @author jk 2014.05.19
 */
public class CommandLineToolTest
    {

    @Test
    public void shouldParseArgumentWithoutValue() throws Exception
        {
        String[]            asArgs = {"a", "b"};
        Map<Object,Object>  map    = CommandLineTool.parseArguments(asArgs, null, false);

        assertThat(map.size(), is(2));
        assertThat(map, hasEntry((Object) 0, (Object) "a"));
        assertThat(map, hasEntry((Object) 1, (Object) "b"));
        }

    @Test
    public void shouldParseFlagArgumentWithoutValue() throws Exception
        {
        String[]            asArgs = {"-a", "-b"};
        Map<Object,Object>  map    = CommandLineTool.parseArguments(asArgs, null, false);

        assertThat(map.size(), is(2));
        assertThat(map, hasEntry((Object) "a", null));
        assertThat(map, hasEntry((Object) "b", null));
        }

    @Test
    public void shouldParseArgumentsWithValues() throws Exception
        {
        String[]            asArgs = {"-a", "value-a", "-b=value-b", "-c:value-c"};
        Map<Object,Object>  map    = CommandLineTool.parseArguments(asArgs, null, false);

        assertThat(map.size(), is(3));
        assertThat(map, hasEntry((Object) "a", (Object) "value-a"));
        assertThat(map, hasEntry((Object) "b", (Object) "value-b"));
        assertThat(map, hasEntry((Object) "c", (Object) "value-c"));
        }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectInvalidCommand() throws Exception
        {
        String[]            asArgs     = {"-a", "-z"};
        String[]            asCommands = {"a", "b", "c"};

        CommandLineTool.parseArguments(asArgs, asCommands, false);
        }

    @Test
    public void shouldParseMultipleArgumentValues() throws Exception
        {
        String[]            asArgs       = {"-a", "1", "-a=2", "-a:3"};
        Map<Object,Object>  map          = CommandLineTool.parseArguments(asArgs, null, false);

        assertThat(map.size(), is(1));
        assertThat((List<String>) map.get("a"), contains("1", "2", "3"));
        }
    }
