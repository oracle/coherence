/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package topics;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Publisher.Option;

import com.tangosol.util.Base;

import java.util.concurrent.CompletableFuture;

/**
 * @author jk 2015.07.01
 */
public class TopicPublisher
        implements Runnable
    {
    // ----- constructor ----------------------------------------------------

    public TopicPublisher(NamedTopic<String> topic, String sPrefix, int nCount, boolean fSync)
        {
        this(topic, sPrefix, nCount, fSync, null);
        }

    public TopicPublisher(NamedTopic<String> topic, String sPrefix, int nCount, boolean fSync, Publisher.Option ...arOptions)
        {
        m_topic     = topic;
        m_sPrefix   = sPrefix;
        m_nCount    = nCount;
        m_fSync     = fSync;
        m_arOptions = arOptions;
        }

    // ----- accessor methods -----------------------------------------------

    public int getPublished()
        {
        return m_nPublished;
        }

    public int getCount()
        {
        return m_nCount;
        }

    public String getPrefix()
        {
        return m_sPrefix;
        }

    public NamedTopic<String> getTopic()
        {
        return m_topic;
        }

    // ----- Runnable methods -----------------------------------------------

    @Override
    public void run()
        {
        try (Publisher<String>   publisher = m_topic.createPublisher(m_arOptions))
            {
            CompletableFuture[] aFutures  = new CompletableFuture[m_nCount];

            while (m_nPublished < m_nCount)
                {
                String sMessage = m_sPrefix + m_nPublished;
                CompletableFuture<Void> future = publisher.send(sMessage);

                if (m_fSync)
                    {
                    future.get();
                    CacheFactory.log("publisher id: " + publisher.hashCode() + " send message:" + sMessage, Base.LOG_MAX);
                    }
                else
                    {
                    aFutures[m_nPublished] = future;
                    }

                m_nPublished++;
                }

            if (!m_fSync)
                {
                CompletableFuture.allOf(aFutures).get();
                }

            }
        catch (Exception e)
            {
            e.printStackTrace();
            }
        finally
            {
            CacheFactory.log("Exiting " + this, Base.LOG_DEBUG);
            }
        }

    // ----- Object methods -------------------------------------------------

    public String toString()
        {
        StringBuilder sb = new StringBuilder();
        sb.append("TopicPublisher [topic: ").append(m_topic.getName());
        if (m_arOptions != null)
            {
            sb.append(" PublisherOptions: [");

            for (Option option : m_arOptions)
                {
                sb.append(option.getClass().getSimpleName()).append(" ");
                }
            sb.append("]");
            }

        sb.append(" published messages: ").append(m_nPublished).append(" synch: ").append( m_fSync).append("]");
        return sb.toString();
        }

    // ----- data members ---------------------------------------------------

    private final NamedTopic<String> m_topic;
    private final String             m_sPrefix;
    private final int                m_nCount;
    private final boolean            m_fSync;
    private int                      m_nPublished = 0;
    private Publisher.Option[]       m_arOptions;
    }
