/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.agent;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.LongArray;
import com.tangosol.util.SparseArray;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author jk 2015.05.18
 */
public class OfferResultTest
    {
    @Test
    public void shouldSerializeUsingPof()
        {
        SparseArray<Throwable> aErrors     = new SparseArray<>();
        ConfigurablePofContext serializer  = new ConfigurablePofContext("coherence-pof-config.xml");
        OfferProcessor.Result  toSerialize = new OfferProcessor.Result(OfferProcessor.Result.Status.Success, 100, 99, aErrors, 19);
        Binary                 binary      = ExternalizableHelper.toBinary(toSerialize, serializer);
        OfferProcessor.Result  result      = ExternalizableHelper.fromBinary(binary, serializer);

        assertThat(result.getStatus(), is(OfferProcessor.Result.Status.Success));
        assertThat(result.getAcceptedCount(), is(100));
        assertThat(result.getPageCapacity(), is(99));
        assertThat(result.getErrors(), is(aErrors));
        assertThat(result.getOffset(), is(19));

        // validate Evolvable version across pof serialization
        assertThat(result.getDataVersion(), is(toSerialize.getImplVersion()));
        }

    @Test
    public void shouldSerializeUsingPofWithErrors()
        {
        SparseArray<Throwable> aErrors = new SparseArray<>();
        RuntimeException       error   = new RuntimeException("No!");

        aErrors.set(19L, error);

        ConfigurablePofContext serializer    = new ConfigurablePofContext("coherence-pof-config.xml");
        OfferProcessor.Result toSerialize    = new OfferProcessor.Result(OfferProcessor.Result.Status.Success, 100, 99, aErrors, 19);
        Binary                 binary        = ExternalizableHelper.toBinary(toSerialize, serializer);
        OfferProcessor.Result result         = ExternalizableHelper.fromBinary(binary, serializer);
        LongArray<Throwable>   aResultErrors = result.getErrors();
        Throwable              throwable     = aResultErrors.get(19L);

        assertThat(throwable.getMessage(), is(error.getMessage()));
        }
    }
