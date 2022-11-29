/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.agent;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import org.junit.Test;

import java.util.LinkedList;
import java.util.Queue;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author jk 2015.08.24
 */
public class PollResultTest
    {
    @Test
    public void shouldSerializeUsingPof()
        {
        ConfigurablePofContext serializer  = new ConfigurablePofContext("coherence-pof-config.xml");
        LinkedList<Binary>     list        = new LinkedList<>();
        PollProcessor.Result   toSerialize = new PollProcessor.Result(0, 0, list,0L);

        list.add(ExternalizableHelper.toBinary("Foo", serializer));
        list.add(ExternalizableHelper.toBinary("Bar", serializer));

        Binary               binary = ExternalizableHelper.toBinary(toSerialize, serializer);
        PollProcessor.Result result = ExternalizableHelper.fromBinary(binary, serializer);

        assertThat(result.getRemainingElementCount(), is(toSerialize.getRemainingElementCount()));
        assertThat(result.getNextIndex(), is(toSerialize.getNextIndex()));
        assertThat(result.getElements(), is(toSerialize.getElements()));

        // validate Evolvable version across pof serialization
        assertThat(result.getDataVersion(), is(toSerialize.getImplVersion()));
        assertThat(result.toString(), is(notNullValue()));
        }

    @Test
    public void shouldReturnEmptyListIfConstructedWithNullList()
        {
        PollProcessor.Result pollResult = new PollProcessor.Result(0, 0, null,0L);
        Queue<Binary>        elements   = pollResult.getElements();

        assertThat(elements, is(notNullValue()));
        assertThat(elements.isEmpty(), is(true));
        }
    }
