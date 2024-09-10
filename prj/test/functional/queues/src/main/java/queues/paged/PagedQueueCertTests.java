/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package queues.paged;


import com.google.common.collect.testing.QueueTestSuiteBuilder;
import com.google.common.collect.testing.TestStringQueueGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;

import com.tangosol.coherence.config.scheme.PagedQueueScheme;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedQueue;
import com.tangosol.net.Session;

import junit.framework.TestFailure;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Queue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.fail;

public class PagedQueueCertTests
    {
    @BeforeAll
    static void setup() throws Exception
        {
        System.setProperty("coherence.ttl",         "0");
        System.setProperty("coherence.wka",         "127.0.0.1");
        System.setProperty("coherence.localhost",   "127.0.0.1");
        System.setProperty("coherence.cacheconfig", "queue-cache-config.xml");

        Coherence coherence = Coherence.clusterMember().start().get(5, TimeUnit.MINUTES);
        m_session = coherence.getSession();
        }

    public Session getSession()
        {
        return m_session;
        }

    @TestFactory
    Iterable<DynamicTest> shouldDoSomething()
        {
        TestSuite suite = QueueTestSuiteBuilder.using(new TestStringQueueGenerator()
                    {
                    @Override
                    @SuppressWarnings("unchecked")
                    public Queue<String> create(String... asValue)
                        {
                        String             sName = "test-" + m_queueId.getAndIncrement();
                        NamedQueue<String> queue = PagedQueueScheme.INSTANCE.realize(sName, m_session);

                        for (String sValue : asValue)
                            {
                            queue.add(sValue);
                            }
                        return queue;
                        }
                    })
                .named("NamedQueue Tests")
                .withFeatures(CollectionFeature.KNOWN_ORDER,
                        CollectionFeature.NON_STANDARD_TOSTRING,
                        CollectionFeature.SUPPORTS_ADD,
                        CollectionSize.ANY
                        )
                .createTestSuite();

        List<junit.framework.Test> list = getTests(suite);

        List<DynamicTest> tests = new ArrayList<>();
        for (junit.framework.Test test : list)
            {
            DynamicTest dt = DynamicTest.dynamicTest(test.toString(), new Executable()
                {
                @Override
                public void execute() throws Throwable
                    {
                    TestResult result = new TestResult();
                    test.run(result);
                    if (!result.wasSuccessful())
                        {
                        Enumeration<TestFailure> enError = result.errors();
                        while (enError.hasMoreElements())
                            {
                            TestFailure failure = enError.nextElement();
                            System.err.println(failure);
                            System.err.println(failure.exceptionMessage());
                            Throwable error = failure.thrownException();
                            if (error != null)
                                {
                                error.printStackTrace();
                                }
                            fail();
                            }
                        }
                    }
                });
            tests.add(dt);
            }
        return tests;
        }

    private List<junit.framework.Test> getTests(TestSuite suite)
        {
        List<junit.framework.Test> list = new ArrayList<>();
        Enumeration<junit.framework.Test> en = suite.tests();
        while (en.hasMoreElements())
            {
            junit.framework.Test test = en.nextElement();
            if (test instanceof TestSuite)
                {
                list.addAll(getTests((TestSuite) test));
                }
            else
                {
                list.add(test);
                }
            }
        return list;
        }

    // ----- data members ---------------------------------------------------

    private static final AtomicInteger m_queueId = new AtomicInteger();
    private static Session m_session;
    }
