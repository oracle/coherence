/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.coherence.config.CacheMapping;

import com.tangosol.net.cache.TypeAssertion;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author jk 2015.06.22
 */
@RunWith(Parameterized.class)
public class PagedTopicCacheNamesTest
    {
    // ----- constructors ---------------------------------------------------

    public PagedTopicCacheNamesTest(PagedTopicCaches.Names pagedTopicCacheNames)
        {
        m_f_pagedTopicCacheNames = pagedTopicCacheNames;
        }

    // ----- test lifecycle methods -----------------------------------------

    @Parameterized.Parameters(name = "name={0}")
    public static Collection<Object[]> parameters()
        {
        List<Object[]>    list   = new ArrayList<>();

        for (PagedTopicCaches.Names name : PagedTopicCaches.Names.values())
            {
            list.add(new Object[]{name});
            }

        return list;
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldHaveCorrectCacheName() throws Exception
        {
        String sQueueName = "Foo";
        String sPrefix    = m_f_pagedTopicCacheNames.getPrefix();

        assertThat(m_f_pagedTopicCacheNames.cacheNameForTopicName(sQueueName), is(sPrefix + sQueueName));
        }

    @Test
    public void shouldHaveCorrectQueueName() throws Exception
        {
        String sQueueName = "Foo";
        String sCacheName = m_f_pagedTopicCacheNames.getPrefix() + sQueueName;

        assertThat(PagedTopicCaches.Names.getTopicName(sCacheName), is(sQueueName));
        }

    @Test
    public void shouldReturnQueueNameFromCacheName() throws Exception
        {
        String          sQueueName = "Foo";
        String          sCacheName = m_f_pagedTopicCacheNames.getPrefix() + sQueueName;
        PagedTopicCaches.Names qcn        = PagedTopicCaches.Names.fromCacheName(sCacheName);

        assertThat(qcn, is(m_f_pagedTopicCacheNames));
        }

    @Test
    public void shouldHaveTypeAssertion() throws Exception
        {
        Class         clsKey    = m_f_pagedTopicCacheNames.getKeyClass();
        Class         clsValue  = m_f_pagedTopicCacheNames.getValueClass();
        TypeAssertion assertion = m_f_pagedTopicCacheNames.getTypeAssertion();

        assertThat(clsKey, is(notNullValue()));
        assertThat(clsValue, is(notNullValue()));
        assertThat(assertion, is(notNullValue()));

        CacheMapping mapping = new CacheMapping("foo", "bar");
        mapping.setKeyClassName(clsKey.getName());
        mapping.setValueClassName(clsValue.getName());

        assertion.assertTypeSafety("foo", mapping);
        }

    // ----- data members ---------------------------------------------------

    private PagedTopicCaches.Names m_f_pagedTopicCacheNames;
    }
