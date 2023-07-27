/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.notifications.model;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class CustomerTest {
    @Test
    public void shouldSerializeUsingPOF() {
        Customer customer = new Customer("A100", "Primož", "Roglič");

        ConfigurablePofContext pofContext = new ConfigurablePofContext();
        Binary                 binary     = ExternalizableHelper.toBinary(customer, pofContext);
        Customer               result     = ExternalizableHelper.fromBinary(binary, pofContext);

        assertThat(result, is(notNullValue()));
        assertThat(result.getId(), is(customer.getId()));
        assertThat(result.getFirstName(), is(customer.getFirstName()));
        assertThat(result.getLastName(), is(customer.getLastName()));
    }
}
