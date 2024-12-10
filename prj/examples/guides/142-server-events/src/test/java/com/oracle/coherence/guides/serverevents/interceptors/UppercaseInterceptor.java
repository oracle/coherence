/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */


package com.oracle.coherence.guides.serverevents.interceptors;

import java.io.Serializable;

import com.oracle.coherence.guides.serverevents.model.Customer;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.annotation.EntryEvents;
import com.tangosol.net.events.annotation.Interceptor;
import com.tangosol.net.events.partition.cache.EntryEvent;

import com.tangosol.util.BinaryEntry;

/**
 * An {@link Interceptor} that will change all string values in a {@link Customer} to uppercase.
 *
 * @author Tim Middleton 2022.05.04
 */
// #tag::class[]
@Interceptor(identifier = "UppercaseInterceptor")  // <1>
@EntryEvents({EntryEvent.Type.INSERTING, EntryEvent.Type.UPDATING})  // <2>
public class UppercaseInterceptor
        implements EventInterceptor<EntryEvent<Integer, Customer>>, Serializable {  // <3>

    @Override
    public void onEvent(EntryEvent<Integer, Customer> event) {  // <4>
        BinaryEntry<Integer, Customer> entry = event.getEntry();
        Customer customer = entry.getValue();
        customer.setName(customer.getName().toUpperCase());
        customer.setAddress(customer.getAddress().toUpperCase());
        entry.setValue(customer);  // <5>
    }
}
// #end::class[]
