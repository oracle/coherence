/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.partitions.bank;

import com.oracle.bedrock.junit.CoherenceClusterExtension;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;

import com.oracle.bedrock.runtime.java.options.IPv4Preferred;

import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.testsupport.junit.TestLogsExtension;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;

import com.tangosol.util.Processors;
import com.tangosol.util.ValueExtractor;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * A simple integration test for the Bank example
 */
public class BankIT
    {
    @BeforeAll
    static void setup() throws Exception
        {
        System.setProperty(ClusterName.PROPERTY, "BankIT");
        System.setProperty(WellKnownAddress.PROPERTY, "127.0.0.1");
        System.setProperty(LocalHost.PROPERTY, "127.0.0.1");
        System.setProperty("coherence.client", "remote");

        s_coherence = Coherence.client().start().get(5, TimeUnit.MINUTES);
        }

    @AfterAll
    static void cleanup()
        {
        Coherence.closeAll();
        }

    @Test
    public void shouldTransferFunds()
        {
        Session                        session   = s_coherence.getSession();
        NamedMap<CustomerId, Customer> customers = session.getMap(BankCacheNames.CUSTOMERS);
        NamedMap<AccountId, Account>   accounts  = session.getMap(BankCacheNames.ACCOUNTS);

        CustomerId customerId   = new CustomerId("Foo");
        Customer   customer     = new Customer(customerId, "John", "Doe");
        AccountId  accountIdOne = new AccountId(customerId, "One");
        Account    accountOne   = new Account(accountIdOne, new BigDecimal(1000));
        AccountId  accountIdTwo = new AccountId(customerId, "Two");
        Account    accountTwo   = new Account(accountIdOne, new BigDecimal(500));

        customers.put(customerId, customer);
        accounts.put(accountIdOne, accountOne);
        accounts.put(accountIdTwo, accountTwo);

        TransferProcessor processor = new TransferProcessor(accountIdOne, accountIdTwo, new BigDecimal(100));
        customers.invoke(customerId, processor);

        assertThat(accounts.invoke(accountIdOne, Processors.extract(ValueExtractor.of(Account::getBalance))), is(new BigDecimal(900)));
        assertThat(accounts.invoke(accountIdTwo, Processors.extract(ValueExtractor.of(Account::getBalance))), is(new BigDecimal(600)));
        }

//    @RegisterExtension
//    @Order(1)
//    static TestLogsExtension s_testLogsExtension = new TestLogsExtension(BankIT.class);

    @RegisterExtension
    @Order(2)
    static CoherenceClusterExtension s_clusterExtension = new CoherenceClusterExtension()
            .with(ClusterName.of("BankIT"),
                  DisplayName.of("Storage"),
                  IPv4Preferred.yes(),
                  WellKnownAddress.loopback(),
                  LocalHost.only()
                  // s_testLogsExtension
                  )
            .include(3, CoherenceClusterMember.class);

    private static Coherence s_coherence;
    }
