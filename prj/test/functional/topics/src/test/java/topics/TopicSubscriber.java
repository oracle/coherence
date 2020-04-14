/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package topics;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.Subscriber.Element;

import com.tangosol.util.Base;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author jk 2015.07.01
 */
public class TopicSubscriber
        implements Runnable
    {
    public TopicSubscriber(Subscriber<String> subscriber, String sPrefix, int nExpected, long timeout,
                           TimeUnit units, boolean fFailIfNullPolled)
        {
        this(subscriber, sPrefix, nExpected, timeout, units, fFailIfNullPolled, true);
        }

    public TopicSubscriber(Subscriber<String> subscriber, String sPrefix, int nExpected, long timeout,
            TimeUnit units, boolean fFailIfNullPolled, boolean fVerifyOrder)
        {
        m_sPrefix           = sPrefix;
        m_nExpected         = nExpected;
        m_nTimeout          = units.toMillis(timeout);
        m_fFailIfNullPolled = fFailIfNullPolled;
        m_subscriber        = subscriber;
        m_fVerifyOrder      = fVerifyOrder;
        }

    public int getConsumedCount()
        {
        return m_nConsumed;
        }

    @Override
    public void run()
        {
        long   duration = 0;

        try
            {
            long   start = System.currentTimeMillis();

            while(duration < m_nTimeout && m_nConsumed < m_nExpected)
                {
                try
                    {
                    CompletableFuture<Element<String>> future  = m_subscriber.receive();
                    Element<String>                    element = future.get(m_nTimeout, TimeUnit.MILLISECONDS);

                    if (element != null && element.getValue() != null)
                        {
                        if (m_fVerifyOrder)
                            {
                            assertThat("subscriber verifying published order", element.getValue(), is(m_sPrefix + m_nConsumed));
                            CacheFactory.log("subscriber id: " + m_subscriber.hashCode() + " consumed message order for " + element.getValue(), Base.LOG_MAX);
                            }
                        m_nConsumed++;
                        }
                    else
                        {
                        if (m_fFailIfNullPolled)
                            {
                            break;
                            }
                        }

                    duration = System.currentTimeMillis() - start;
                    }
                catch (Exception e)
                    {
                    e.printStackTrace();
                    throw Base.ensureRuntimeException(e);
                    }
                }
            }
        finally
            {
            CacheFactory.log("Exiting TopicSubscriber: duration: " + duration + " " + this, Base.LOG_DEBUG);
            m_subscriber.close();
            }
        }

    // ----- Object methods -------------------------------------------------

    public String toString()
        {
        return "TopicSubscriber [subscriber: " + m_subscriber + " received messages: " + m_nConsumed + " expected messages: " + m_nExpected + "]";
        }


    private final Subscriber<String> m_subscriber;
    private final String             m_sPrefix;
    private final long               m_nTimeout;
    private final int                m_nExpected;
    private final boolean            m_fFailIfNullPolled;
    private int                      m_nConsumed = 0;
    private final boolean            m_fVerifyOrder;
    }
