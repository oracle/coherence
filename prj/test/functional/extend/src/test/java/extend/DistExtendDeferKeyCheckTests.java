/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package extend;


import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.coherence.common.base.Blocking;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.KeyAssociation;
import com.tangosol.util.ClassHelper;

import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MultiplexingMapListener;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.InKeySetFilter;
import common.AbstractFunctionalTest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Set;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;


/**
* A collection of functional tests for Coherence*Extend that verify support
* for the defer-key-association-check configuration element
*
* @author phf  2011.09.07
*/
public class DistExtendDeferKeyCheckTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public DistExtendDeferKeyCheckTests()
        {
        super(FILE_CLIENT_CFG_CACHE);
        }


    // ----- DistExtendDeferKeyCheckTests methods ---------------------------

    /**
    * Return the cache used in all test methods.
    *
    * @return the test cache
    */
    protected NamedCache getNamedCache()
        {
        return getNamedCache(CACHE_DIST_EXTEND_DIRECT);
        }


    // ----- AbstractNamedCacheTest methods ---------------------------------

    /**
    * {@inheritDoc}
    */
    protected NamedCache getNamedCache(String sCacheName, ClassLoader loader)
        {
        ConfigurableCacheFactory factory = getFactory();
        NamedCache               cache   = factory.ensureCache(sCacheName, loader);

        // release any previous state
        if (cache.getCacheService().getInfo().getServiceType().equals(
                CacheService.TYPE_LOCAL))
            {
            try
                {
                Object o = cache;
                o = ClassHelper.invoke(o, "getNamedCache",  ClassHelper.VOID);
                o = ClassHelper.invoke(o, "getActualMap",   ClassHelper.VOID);
                o = ClassHelper.invoke(o, "getCacheLoader", ClassHelper.VOID);
                    ClassHelper.invoke(o, "destroy",        ClassHelper.VOID);
                }
            catch (Exception e)
                {
                // ignore
                }
            }
        cache.destroy();

        return factory.ensureCache(sCacheName, loader);
        }

    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        CoherenceClusterMember memberProxy = startCacheServer("DistExtendDeferKeyCheckTests", "extend", FILE_SERVER_CFG_CACHE);
        Eventually.assertThat(invoking(memberProxy).isServiceRunning("ExtendTcpProxyService"), is(true));
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("DistExtendDeferKeyCheckTests");
        }


    // ----- DistExtendDeferKeyCheckTests test methods ----------------------

    /**
    * Verify that put with a simple key class succeeds but with a custom key
    * class throws an exception.
    */
    @Test
    public void put()
        {
        NamedCache cache = getNamedCache();

        cache.clear();
        assertTrue(cache.isEmpty());

        cache.put("key", "value");

        try
            {
            cache.put(new CustomKeyClass("key"), "value");
            }
        catch (Exception e)
            {
            // expected
            return;
            }
        fail("expected exception");
        }

    /**
     * Test that we received insert, update etc. events.
     */
    @Test
    public void testUpdateAssociatedKeyEvents() throws InterruptedException
        {
        NamedCache cacheOrder = getNamedCache("dist-order-id");
        NamedCache cacheItem  = getNamedCache("dist-order-item");

        try
            {
            AssociatedKeyListener itemListener  = new AssociatedKeyListener();

            OrderId orderId  = new OrderId(1234L);
            OrderId orderId2 = new OrderId(2345L);
            ItemId  itemId   = new ItemId(orderId, 1L);
            ItemId  itemId11 = new ItemId(orderId, 11L);
            ItemId  itemId2  = new ItemId(orderId2, 2L);

            cacheOrder.put(orderId, "OrderValue");
            cacheItem.addMapListener(itemListener, itemId, false);
            putAndCheckEvent(cacheItem, itemListener, itemId, "ItemValueKey");
            putAndCheckEvent(cacheItem, itemListener, itemId, "ItemValueKey2");
            cacheItem.removeMapListener(itemListener, itemId);

            cacheItem.addMapListener(itemListener, AlwaysFilter.INSTANCE, false);
            putAndCheckEvent(cacheItem, itemListener, itemId11, "ItemValueFilter11");
            cacheItem.removeMapListener(itemListener, AlwaysFilter.INSTANCE);

            Set            itemSet       = new ImmutableArrayList(new ItemId[]{itemId, itemId2, itemId11});
            InKeySetFilter itemSetFilter = new InKeySetFilter(AlwaysFilter.INSTANCE, itemSet);

            cacheItem.addMapListener(itemListener, itemSetFilter, false);
            putAndCheckEvent(cacheItem, itemListener, itemId, "ItemValue3");
            putAndCheckEvent(cacheItem, itemListener, itemId2, "ItemValue2");
            cacheItem.removeMapListener(itemListener, itemSetFilter);
            }
        finally
            {
            cacheOrder.clear();
            cacheItem.clear();
            }
        }

    /**
     * Helper.
     */
    protected void putAndCheckEvent(NamedCache cache, AssociatedKeyListener listener, Object oKey, Object oValue)
        {
        listener.reset();

        Object oExpect;
        if (oValue == null)
            {
            cache.remove(oKey);
            oExpect = "Removed";
            }
        else
            {
            cache.put(oKey, oValue);
            oExpect = oValue;
            }

        assertEquals(oExpect, listener.waitForResult());
        }

    // ----- inner classes --------------------------------------------------

    /**
     * An event listener used for testUpdateAssociatedKeyEvents.
     */
    public class AssociatedKeyListener
            extends MultiplexingMapListener
        {
        @Override
        protected void onMapEvent(MapEvent evt)
            {
            synchronized (this)
                {
                m_oValue = evt.getNewValue();
                m_fResult = true;
                notify();
                }
            }

        public synchronized Object waitForResult()
            {
            int c = 0;
            while (!m_fResult && c < WAIT_TIMEOUT)
                {
                c++;
                try
                    {
                    Blocking.wait(this, 10000);
                    }
                catch (InterruptedException e)
                    {
                    fail();
                    }
                }

            if (c == WAIT_TIMEOUT)
                {
                fail("Expecting event but received none!");
                }
            return m_oValue;
            }

        public synchronized void reset()
            {
            m_fResult = false;
            m_oValue = null;
            }

        // ----- constants ------------------------------------------------------

        protected final static int WAIT_TIMEOUT = 6;

        // ----- data members -----------------------------------------------

        /**
         * The value to be notified of.
         */
        protected Object m_oValue;

        /**
         * Used as a signal to know when the event has been received.
         */
        protected boolean m_fResult;
        }

    /**
     * An object used for testUpdateAssociatedKeyEvents.
     */
    public static class OrderId
            implements PortableObject
        {
        /**
         * Default constructor. Needed for PortableObject.
         */
        public OrderId()
            {
            }

        public OrderId(long orderId)
            {
            m_orderId = orderId;
            }

        public long getOrderId()
            {
            return m_orderId;
            }

        // ----- PortableObject interface ---------------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in) throws IOException
            {
            m_orderId = in.readLong(0);
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeLong(0, m_orderId);
            }

        @Override
        public boolean equals(Object obj)
            {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            OrderId other = (OrderId) obj;
            if (m_orderId != other.m_orderId)
                return false;
            return true;
            }

        @Override
        public int hashCode()
            {
            return 31 * (int) m_orderId;
            }

        private long m_orderId;
        }

    /**
     * An object used for testUpdateAssociatedKeyEvents.
     */
    public static class ItemId
            implements KeyAssociation, PortableObject
        {
        /**
         * Default constructor. Needed for PortableObject.
         */
        public ItemId()
            {}

        public ItemId(OrderId orderId, long itemId)
            {
            m_orderId = orderId;
            m_itemId  = itemId;
            }

        public Object getAssociatedKey()
            {
            return getOrderId();
            }

        public OrderId getOrderId()
            {
            return m_orderId;
            }

        long getItemId()
            {
            return m_itemId;
            }

        // ----- PortableObject interface ---------------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in) throws IOException
            {
            m_orderId = in.readObject(0);
            m_itemId  = in.readLong(1);
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0, m_orderId);
            out.writeLong(1, m_itemId);
            }

        @Override
        public boolean equals(Object obj)
            {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ItemId other = (ItemId) obj;
            if (m_itemId != other.m_itemId)
                return false;
            if (m_orderId.getOrderId() != other.m_orderId.getOrderId())
                return false;
            return true;
            }

        @Override
        public int hashCode()
            {
            return 31 * (int) m_orderId.getOrderId() + 31 * (int) m_itemId;
            }

        private OrderId m_orderId;
        private long    m_itemId;
        }

    // ----- constants ------------------------------------------------------

    /**
    * The file name of the default cache configuration file used by this test.
    */
    public static String FILE_CLIENT_CFG_CACHE
            = "client-cache-config-defer-key-check.xml";

    /**
    * The file name of the default cache configuration file used by cache
    * servers launched by this test.
    */
    public static String FILE_SERVER_CFG_CACHE    = "server-cache-config.xml";

    /**
    * Cache name: "dist-extend-direct"
    */
    public static String CACHE_DIST_EXTEND_DIRECT = "dist-extend-direct";
    }
