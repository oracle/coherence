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
 * An {@link Interceptor} that will carry out validation on Customers type against
 * their credit limit and reject the update if validation rules fail.
 *
 * @author Tim Middleton 2022.05.05
 */
// #tag::class[]
@Interceptor(identifier = "ValidationInterceptor") // <1>
@EntryEvents({EntryEvent.Type.INSERTING, EntryEvent.Type.UPDATING}) // <2>
public class ValidationInterceptor
        implements EventInterceptor<EntryEvent<Integer, Customer>>, Serializable {  // <3>

    @Override
    public void onEvent(EntryEvent<Integer, Customer> event) {  // <4>
        BinaryEntry<Integer, Customer> entry       = event.getEntry();
        Customer                       customerOld = entry.getOriginalValue();
        Customer                       customerNew = entry.getValue();
        EntryEvent.Type                eventType   = event.getType();

        if (eventType == EntryEvent.Type.INSERTING) {  // <5>
            // Rule 1 - New customers cannot have credit limit above 1,000,000 unless they are GOLD
            if (customerNew.getCreditLimit() >= 1_000_000L && !customerNew.getCustomerType().equals(Customer.GOLD)) {
                // reject the update
                throw new RuntimeException("Only gold customers may have credit limits above 1,000,000");
            }
        }
        else if (eventType == EntryEvent.Type.UPDATING) {  // <6>
            // Rule 2 - Cannot change customer type from BRONZE directly to GOLD, must go BRONZE -> SILVER -> GOLD
            if (customerNew.getCustomerType().equals(Customer.GOLD) && customerOld.getCustomerType().equals(Customer.BRONZE)) {
                // reject the update
                throw new RuntimeException("Cannot update customer directly to GOLD from BRONZE");
            }
        }

        // otherwise, continue with update
        entry.setValue(customerNew);  // <7>
    }
}
// #end::class[]
