/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package memcached;

import com.tangosol.coherence.memcached.server.BinaryConnection;
import com.tangosol.coherence.memcached.server.BinaryConnection.BinaryRequest;
import com.tangosol.coherence.memcached.server.BinaryConnection.BinaryResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


import com.tangosol.coherence.memcached.server.ResponseQueue;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link ResponseQueue}.
 *
 * @author bb  2013.08.15
 */
public class ResponseQueueTest
    {
    @Before
    public void setUp()
        {
        m_queue = new ResponseQueue();
        m_conn  = new BinaryConnection(null, null, 1);
        }

    @Test
    public void testAdd()
        {
        for (int i = 0; i < 10; i++)
            {
            BinaryResponse response = createBinaryResponse();
            m_queue.add(response);
            }   	
        assertEquals(10, m_queue.size());
        }

    @Test
    public void testRemove()
        {
        int nSize = 10;
        BinaryResponse[] aResponses = new BinaryResponse[nSize];
        for (int i = 0; i < nSize; i++)
            {
            aResponses[i] = createBinaryResponse();
            m_queue.add(aResponses[i]);
            }   	
        for (int i = 0; i < nSize - 1; i++)
            {
            BinaryResponse nextResponse = m_queue.removeAndGetNext(aResponses[i]);
            assertNotNull(nextResponse);
            assertEquals(aResponses[i+1], nextResponse);
            }
         BinaryResponse nextResponse = m_queue.removeAndGetNext(aResponses[nSize - 1]);
         assertNull(nextResponse);
        }

    @Test
    public void testConcurrentUpdate()
        {
        int             nLoop                = 5000;
        List            listResponsesFlushed = new ArrayList(nLoop);
        ResponseQueue   fQueue               = m_queue;
        ExecutorService executorSvc          = Executors.newFixedThreadPool(50);
        for (int i = 0; i < nLoop; i++)
            {
            BinaryResponse response = createBinaryResponse();
            fQueue.add(response);
            executorSvc.submit(createConsumer(fQueue, response, listResponsesFlushed));
            int nSleep = (int)(Math.random() * (100));
            if (nSleep < 50)
               {
               try
                 {
                 Thread.sleep(nSleep);
                 }
              catch (Exception ex)
                 {
                 ex.printStackTrace();
                 }
               }
           }
        try
          {
          executorSvc.shutdown();
          assertTrue(executorSvc.awaitTermination(1,TimeUnit.MINUTES));
          assertEquals(nLoop, m_responseId.get());
          }
        catch (Exception ex)
          {
          ex.printStackTrace();
          }
        }

    // ---- helper methods --------------------------------------------------

    protected BinaryResponse createBinaryResponse()
        {
        BinaryRequest request = new BinaryRequest(null, null, null, m_conn);
        return new BinaryResponse(null, m_conn, request);
        }

    protected Runnable createConsumer(final ResponseQueue queue, final BinaryResponse response, final List listResponsesFlushed)
        {    
        return new Runnable()
           {
           public void run()
              {
              try
                {
                int nSleep = (int)(Math.random() * 100);
                Thread.sleep(nSleep);
                boolean        fFlush      = queue.isFlushable(response, true);
                BinaryResponse nxtResponse = response;
                do
                    {
                    if (fFlush)
                        {
                        int id = (int) nxtResponse.getRequest().getId();
                        if (m_responseId.get() == id)
                           {
                           m_responseId.incrementAndGet(); // ensures responses are flushed in order thus checking the ResponseQueue FIFO order
                           }
                        nxtResponse = queue.removeAndGetNext(nxtResponse);
                        }
                    else
                        {
                        queue.markDeferred(nxtResponse);
                        }
                    }
                while (fFlush = queue.isFlushable(nxtResponse, false));
                }
              catch (Exception ex) 
                { 
                ex.printStackTrace();
                }
              }
           };  
        }


    // ---- data members ----------------------------------------------------

    protected ResponseQueue m_queue;
    protected BinaryConnection m_conn;
    protected AtomicInteger m_responseId = new AtomicInteger();
    }
