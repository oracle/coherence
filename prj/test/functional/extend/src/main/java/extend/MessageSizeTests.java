/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.tangosol.net.NamedCache;

import com.tangosol.net.messaging.ConnectionException;

import com.tangosol.util.Service;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * A collection of functional tests for Coherence*Extend that specify
 * max-message-size in incoming and outgoing message handlers.
 *
 * @author lh  2013.3.20
 */
public class MessageSizeTests
        extends AbstractExtendTests
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public MessageSizeTests()
        {
        super(CACHE_DIST_EXTEND_DIRECT, "client-cache-config-msg-size.xml");
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        startCacheServerWithProxy("MessageSizeTests", "extend", "server-cache-config-msg-size.xml");
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("MessageSizeTests");
        }

    // ----- message size tests ---------------------------------------------

    /**
     * Put a large set of data into cache. The message exceeds the
     * max-message-size of the Proxy Service incoming message handler.
     */
    @Test
    public void entrySetWithLargeData()
        {
        NamedCache cache = getNamedCache();
        HashMap    map   = new HashMap();

        for (int i = 0; i < 1200; i++)
            {
            map.put(Integer.valueOf(i), Integer.valueOf(i));
            }

        Service service = cache.getCacheService();
        try
            {
            cache.putAll(map);
            }
        catch (ConnectionException e)
            {
            return;
            }
        finally
            {
            // shutdown the service to prevent reuse attempts while the *Extend connection is closing
            service.shutdown();
            }
        fail("expected exception");
        }

    /**
     * Get a large set of data into cache. The result message exceeds the
     * max-message-size of the client incoming message handler.
     */
    @Test
    public void getLargeData()
        {
        NamedCache cache = getNamedCache();
        HashMap    map   = new HashMap();
        List       keys  = new ArrayList();

        for (int i = 0; i < 1000; i++)
            {
            keys.add(Integer.valueOf(i));
            map.put(Integer.valueOf(i), Integer.valueOf(i));
            }

        Service service = cache.getCacheService();
        try
            {
            cache.putAll(map);
            cache.getAll(keys);
            }
        catch (ConnectionException e)
            {
            return;
            }
        finally
            {
            // shutdown the service to prevent reuse attempts while the *Extend connection is closing
            service.shutdown();
            }
        fail("expected exception");
        }

    /**
     * Put an extra large set of data into cache. The message exceeds the
     * max-message-size of the client's (initiator) outgoing message handler.
     */
    @Test
    public void entrySetWithExtraLargeData()
        {
        NamedCache cache = getNamedCache();
        HashMap    map   = new HashMap();

        for (int i = 0; i < 2000; ++i)
            {
            Integer oInt = Integer.valueOf(i);
            map.put(oInt, oInt);
            }

        Service service = cache.getCacheService();
        try
            {
            cache.putAll(map);
            }
        catch (Exception e)
            {
            if (e.getCause() instanceof IOException)
                {
                return;
                }
            fail("expected an IOException, not: " + e.getMessage() + ":\n" + printStackTrace(e));
            }
        finally
            {
            // shutdown the service to prevent reuse attempts while the *Extend connection is closing
            service.shutdown();
            }
        fail("expected exception");
        }

    /**
     * Get a large set of data into cache. The result message exceeds the
     * max-message-size of the Proxy Service outgoing message handler.
     */
    @Test
    public void getExtraLargeData()
        {
        NamedCache cache = getNamedCache();
        HashMap    map   = new HashMap();
        List       keys  = new ArrayList();

        for (int i = 0; i < 1200; i++)
            {
            keys.add(Integer.valueOf(i));
            map.put(Integer.valueOf(i), Integer.valueOf(i));
            }

        Service service = cache.getCacheService();
        try
            {
            cache.putAll(map);
            cache.getAll(keys);
            }
        catch (ConnectionException e)
            {
            return;
            }
        finally
            {
            // shutdown the service to prevent reuse attempts while the *Extend connection is closing
            service.shutdown();
            }
        fail("expected exception");
        }

    @Test
    public void topNAggregator()
        {
        // overridden because it creates larger message than allowed
        }

    @Test
    public void compositeParallel()
        {
        // overridden because it creates larger message than allowed
        }
    }