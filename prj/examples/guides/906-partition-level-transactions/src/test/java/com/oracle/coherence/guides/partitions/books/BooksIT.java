/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.partitions.books;

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

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class BooksIT
    {
    @BeforeAll
    static void setup() throws Exception
        {
        System.setProperty(ClusterName.PROPERTY, "BooksIT");
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
    public void shouldUpdateSales() throws Exception
        {
        String  bookId    = "ABC-123";
        SalesId globalId  = new SalesId(bookId, "Global", null);
        SalesId emeaId    = new SalesId(bookId, "EMEA", "Global");
        SalesId ukId      = new SalesId(bookId, "UK", "EMEA");
        SalesId franceId  = new SalesId(bookId, "FR", "EMEA");
        SalesId americaId = new SalesId(bookId, "NAMER", "Global");
        SalesId usId      = new SalesId(bookId, "US", "NAMER");

        Session session   = s_coherence.getSession();

        NamedMap<SalesId, BookSales> sales = session.getMap(BookCacheNames.BOOK_SALES);
        sales.addIndex(ValueExtractor.of(SalesId::getBookId).fromKey());
        sales.addIndex(ValueExtractor.of(SalesId::getRegionCode).fromKey());

        sales.put(globalId, new BookSales());
        sales.put(emeaId, new BookSales());
        sales.put(ukId, new BookSales());
        sales.put(franceId, new BookSales());
        sales.put(americaId, new BookSales());
        sales.put(usId, new BookSales());

        sales.invoke(ukId, new IncrementSalesProcessor(10L, 20L, 30L));

        assertThat(sales.invoke(ukId, Processors.extract(ValueExtractor.of(BookSales::getEBookSales))), is(10L));
        assertThat(sales.invoke(ukId, Processors.extract(ValueExtractor.of(BookSales::getAudioSales))), is(20L));
        assertThat(sales.invoke(ukId, Processors.extract(ValueExtractor.of(BookSales::getPaperSales))), is(30L));

        assertThat(sales.invoke(emeaId, Processors.extract(ValueExtractor.of(BookSales::getEBookSales))), is(10L));
        assertThat(sales.invoke(emeaId, Processors.extract(ValueExtractor.of(BookSales::getAudioSales))), is(20L));
        assertThat(sales.invoke(emeaId, Processors.extract(ValueExtractor.of(BookSales::getPaperSales))), is(30L));

        assertThat(sales.invoke(globalId, Processors.extract(ValueExtractor.of(BookSales::getEBookSales))), is(10L));
        assertThat(sales.invoke(globalId, Processors.extract(ValueExtractor.of(BookSales::getAudioSales))), is(20L));
        assertThat(sales.invoke(globalId, Processors.extract(ValueExtractor.of(BookSales::getPaperSales))), is(30L));

        sales.invoke(franceId, new IncrementSalesProcessor(5L, 15L, 25L));

        assertThat(sales.invoke(franceId, Processors.extract(ValueExtractor.of(BookSales::getEBookSales))), is(5L));
        assertThat(sales.invoke(franceId, Processors.extract(ValueExtractor.of(BookSales::getAudioSales))), is(15L));
        assertThat(sales.invoke(franceId, Processors.extract(ValueExtractor.of(BookSales::getPaperSales))), is(25L));

        assertThat(sales.invoke(ukId, Processors.extract(ValueExtractor.of(BookSales::getEBookSales))), is(10L));
        assertThat(sales.invoke(ukId, Processors.extract(ValueExtractor.of(BookSales::getAudioSales))), is(20L));
        assertThat(sales.invoke(ukId, Processors.extract(ValueExtractor.of(BookSales::getPaperSales))), is(30L));

        assertThat(sales.invoke(emeaId, Processors.extract(ValueExtractor.of(BookSales::getEBookSales))), is(15L));
        assertThat(sales.invoke(emeaId, Processors.extract(ValueExtractor.of(BookSales::getAudioSales))), is(35L));
        assertThat(sales.invoke(emeaId, Processors.extract(ValueExtractor.of(BookSales::getPaperSales))), is(55L));

        assertThat(sales.invoke(globalId, Processors.extract(ValueExtractor.of(BookSales::getEBookSales))), is(15L));
        assertThat(sales.invoke(globalId, Processors.extract(ValueExtractor.of(BookSales::getAudioSales))), is(35L));
        assertThat(sales.invoke(globalId, Processors.extract(ValueExtractor.of(BookSales::getPaperSales))), is(55L));
        }


//    @RegisterExtension
//    @Order(1)
//    static TestLogsExtension s_testLogsExtension = new TestLogsExtension(BooksIT.class);

    @RegisterExtension
    @Order(2)
    static CoherenceClusterExtension s_clusterExtension = new CoherenceClusterExtension()
            .with(ClusterName.of("BooksIT"),
                  DisplayName.of("Storage"),
                  IPv4Preferred.yes(),
                  WellKnownAddress.loopback(),
                  LocalHost.only())
                  // s_testLogsExtension)
            .include(3, CoherenceClusterMember.class);

    private static Coherence s_coherence;
    }
