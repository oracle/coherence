/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.serverevents;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.guides.serverevents.interceptors.AuditingInterceptor;
import com.oracle.coherence.guides.serverevents.interceptors.EntryProcessorAuditingInterceptor;
import com.oracle.coherence.guides.serverevents.interceptors.UppercaseInterceptor;
import com.oracle.coherence.guides.serverevents.interceptors.ValidationInterceptor;

import com.oracle.coherence.guides.serverevents.model.Customer;

import com.tangosol.net.NamedCache;

import com.tangosol.util.Processors;

import org.hamcrest.Matchers;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests to demonstrate various uses of cache events.
 *
 * @author Tim Middleton 2022.05.04
 */
public class ServerCacheEventsTest
        extends AbstractEventsTest {

    @BeforeAll
    public static void startup() {
        _startup("cache-events");
    }

    // #tag::test1[]
    /**
     *  Test the {@link UppercaseInterceptor} which is defined on the 'customers' cache only,
     *  to update name and address fields to uppercase.
     */
    @Test
    public void testCustomerUppercaseInterceptor() {
        System.out.println("testCustomerUppercaseInterceptor");

        NamedCache<Integer, Customer> customers = getMember1().getCache("customers");
        customers.truncate();

        // put a new Customer with lowercase names and addresses
        customers.put(1, new Customer(1, "tim", "123 james street, perth", Customer.GOLD, 10000L));

        // validate that the name and address are uppercase
        Customer customer = customers.get(1);
        assertEquals(customer.getName(), "TIM");
        assertEquals(customer.getAddress(), "123 JAMES STREET, PERTH");

        // update a customers name and ensure that it is updated to uppercase
        customers.invoke(1, Processors.update(Customer::setName, "timothy"));
        assertEquals(customers.get(1).getName(), "TIMOTHY");
    }
    // #end::test1[]

    // #tag::test2[]
    /**
     * Test the {@link AuditingInterceptor} which will audit any changes to caches
     * that fall thought and match the '*' cache-mapping.
     */
    @Test
    public void testAuditingInterceptor() {
        System.out.println("testAuditingInterceptor");
        CoherenceClusterMember member = getMember1();

        // create two different caches to be audited which will match to the auditing-scheme
        NamedCache<Integer, String>   cache1 = member.getCache("test-cache");
        NamedCache<Integer, Customer> cache2 = member.getCache("test-customer");

        cache1.truncate();
        cache2.truncate();
        Eventually.assertDeferred(() -> auditEvents.size(), Matchers.is(4));

        // clear the audit-events cache, so we miss the created and truncated events
        auditEvents.clear();

        // generate some mutations that will be audited
        cache1.put(1, "one");
        cache1.put(2, "two");
        cache1.put(1, "ONE");
        cache1.remove(1);

        dumpAuditEvents("testAuditingInterceptor-1");

        // ensure 3 inserts and 1 remove events are received
        Eventually.assertDeferred(() -> auditEvents.size(), Matchers.is(4));

        auditEvents.clear();

        // generate new set of mutations for customers
        cache2.put(1, new Customer(1, "Tim", "Address 1", Customer.GOLD, 10000));
        cache2.put(2, new Customer(2, "John", "Address 2", Customer.SILVER, 4000));
        cache2.clear();

        dumpAuditEvents("testAuditingInterceptor-2");

        // ensure 2 insert and 2 remove events are received
        Eventually.assertDeferred(() -> auditEvents.values().size(), Matchers.is(4));
    }
    // #end::test2[]

    // #tag::truncate[]
    @Test
    public void testTruncate() {
        System.out.println("testTruncate");
        auditEvents.clear();

        NamedCache<Integer, String> cache1 = getMember1().getCache("test-cache");
        cache1.truncate();

        // ensure we get two events, one from each storage node
        Eventually.assertDeferred(() -> auditEvents.values().stream().filter(p -> p.getEventType().equals("TRUNCATED")).count(), Matchers.is(2L));

        dumpAuditEvents("truncate");
    }
    // #end::truncate[]

    // #tag::test3[]
    /**
     * Test the {@link EntryProcessorAuditingInterceptor} which will audit any entry processor
     * executions on caches that match the '*' cache-mapping.
     */
    @Test
    public void testEntryProcessorInterceptor() {
        System.out.println("testEntryProcessorInterceptor");
        CoherenceClusterMember member = getMember1();

        // create a cache to audit entry processor events on
        NamedCache<Integer, Customer> cache = member.getCache("test-customer");
        cache.truncate();
        Eventually.assertDeferred(() -> auditEvents.size(), Matchers.is(4));

        // clear the audit-events cache, so we miss the created and truncated events
        auditEvents.clear();

        // add some entries
        cache.put(1, new Customer(1, "Tim", "Address 1", Customer.GOLD, 10_000));
        cache.put(2, new Customer(2, "Tom", "Address 2", Customer.SILVER, 10_000));
        cache.put(3, new Customer(3, "Helen", "Address 3", Customer.BRONZE, 10_000));
        Eventually.assertDeferred(() -> auditEvents.size(), Matchers.is(3));

        auditEvents.clear();

        cache.invokeAll(Processors.update(Customer::setCreditLimit, 100_000L));

        dumpAuditEvents("testEntryProcessorInterceptor-1");
        // up to 3 entry processor events and 3 updates
        Eventually.assertDeferred(() -> auditEvents.values().stream().filter(p -> p.getEventType().equals("EXECUTED")).count(), Matchers.lessThanOrEqualTo(3L));
        Eventually.assertDeferred(() -> auditEvents.values().stream().filter(p -> p.getEventType().equals("UPDATED")).count(), Matchers.is(3L));

        auditEvents.clear();

        // invoke an entry processor across all customers to update credit limit to 100,000
        cache.invokeAll(Processors.update(Customer::setCreditLimit, 100_000L));
        cache.invoke(1, Processors.update(Customer::setCreditLimit, 100_000L));

        dumpAuditEvents("testEntryProcessorInterceptor-2");

        // ensure up to 4 EXECUTED events are received
        Eventually.assertDeferred(() -> auditEvents.values().stream().filter(p -> p.getEventType().equals("EXECUTED")).count(), Matchers.lessThanOrEqualTo(4L));
    }
    // #end::test3[]

    // #tag::test4[]
    /**
     * Test the {@link ValidationInterceptor} which will reject updates if business rules fail.
     */
    @Test
    public void testValidatingInterceptor() {
        System.out.println("testValidatingInterceptor");
        NamedCache<Integer, Customer> customers = getMember1().getCache("customers");
        customers.truncate();

        // try adding a BRONZE customer with credit limit > 1,000,000
        try {
            customers.put(1, new Customer(1, "tim", "123 james street, perth", Customer.BRONZE, 2_000_000L));
            fail("Put succeeded but should have failed");
        }
        catch (Exception e) {
            System.out.printf("Put was correctly rejected: %s\n", e.getMessage());
        }

        // should be rejected
        assertEquals(customers.size(), 0);

        // add a normal BRONZE customer, should succeed with credit limit 10,000
        customers.put(1, new Customer(1, "tim", "123 james street, perth", Customer.BRONZE, 10_000L));
        assertEquals(customers.size(), 1);

        // try and update credit limit to GOLD from BRONZE, should fail
        try {
            customers.invoke(1, Processors.update(Customer::setCustomerType, Customer.GOLD));
            fail("Put succeeded but should have failed");
        }
        catch (Exception e) {
                System.out.printf("Update was correctly rejected: %s\n", e.getMessage());
        }

        assertEquals(customers.get(1).getCustomerType(), Customer.BRONZE);
    }
    // #end::test4[]
}
