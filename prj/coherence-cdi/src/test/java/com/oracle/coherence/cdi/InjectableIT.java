/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.cdi;

import com.oracle.coherence.cdi.data.Account;
import com.oracle.coherence.cdi.data.Person;
import com.oracle.coherence.cdi.data.PhoneNumber;
import com.oracle.coherence.cdi.events.CacheName;
import com.oracle.coherence.cdi.events.Created;
import com.oracle.coherence.cdi.events.Destroyed;
import com.oracle.coherence.cdi.events.Executed;
import com.oracle.coherence.cdi.events.Executing;
import com.oracle.coherence.cdi.events.Inserted;
import com.oracle.coherence.cdi.events.Processor;
import com.oracle.coherence.cdi.events.Removed;
import com.oracle.coherence.cdi.events.Updated;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.events.Event;
import com.tangosol.net.events.application.LifecycleEvent;
import com.tangosol.net.events.partition.TransactionEvent;
import com.tangosol.net.events.partition.TransferEvent;
import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.net.events.partition.cache.EntryProcessorEvent;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration test for the {@link EventDispatcher} using the Weld JUnit
 * extension.
 *
 * @author as  2020.04.03
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InjectableIT
    {

    @WeldSetup
    private WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addBeanClass(CacheFactoryUriResolver.Default.class)
                                                          .addBeanClass(ConfigurableCacheFactoryProducer.class)
                                                          .addBeanClass(CurrencyConverter.class)
                                                          .addBeanClass(TestObservers.class)
                                                          .addBeanClass(EventDispatcher.class));

    @Inject
    @CacheFactory("injectable-cache-config.xml")
    private ConfigurableCacheFactory ccf;

    @Test
    void testNsfWithdrawal()
        {
        ccf.activate();

        NamedCache<String, Account> accounts = ccf.ensureCache("accounts", null);
        accounts.put("X", new Account("X", 5000L));

        accounts.invoke("X", new CreditAccount(100L));
        assertThat(accounts.get("X").getBalance(), is(-5000L));
        }

    // ---- helper classes --------------------------------------------------

    @Dependent
    public static class CreditAccount
            implements InvocableMap.EntryProcessor<String, Account, Long>, Injectable
        {
        private long amount;

        @Inject
        private CurrencyConverter currencyConverter;

        public CreditAccount()
            {
            }

        public CreditAccount(long amount)
            {
            this.amount = amount;
            }

        @Override
        public Long process(InvocableMap.Entry<String, Account> entry)
            {
            Account account = entry.getValue();
            account.withdraw(currencyConverter.convert(amount));
            entry.setValue(account);
            return account.getBalance();
            }
        }

    @ApplicationScoped
    public static class CurrencyConverter
        {
        public long convert(long amount)
            {
            return amount * 100;
            }
        }

    @ApplicationScoped
    public static class TestObservers
        {
        private void onAccountOverdrawn(@Observes Account.Overdrawn event)
            {
            System.out.println(event);
            assertThat(event.getAmount(), is(5000L));
            }
        }
    }
