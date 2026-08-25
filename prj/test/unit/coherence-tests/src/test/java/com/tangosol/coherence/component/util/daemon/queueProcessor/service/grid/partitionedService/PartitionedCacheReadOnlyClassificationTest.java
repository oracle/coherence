/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService;

import com.tangosol.coherence.component.net.message.RequestMessage;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.BinaryMap;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.Storage;

import com.tangosol.net.Action;
import com.tangosol.net.ActionPolicy;
import com.tangosol.net.BackingMapManager;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.DefaultConfigurableCacheFactory;
import com.tangosol.net.RequestPolicyException;
import com.tangosol.net.Service;
import com.tangosol.net.security.StorageAccessAuthorizer;

import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.SafeHashMap;

import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit tests for PartitionedCache get/getAll read-only classification.
 *
 * @author Aleks Seovic  2026.04.28
 * @since 26.07
 */
public class PartitionedCacheReadOnlyClassificationTest
    {
    @Test
    public void shouldClassifyPlainGetAndGetAllAsReadOnly()
        {
        TestService service = serviceWithStorage(false);

        assertThat(getRequest(service).isReadOnly(), is(true));
        assertThat(getAllRequest(service).isReadOnly(), is(true));
        }

    @Test
    public void shouldClassifyWriteCapableGetAndGetAllAsWriteShaped()
        {
        TestService service = serviceWithStorage(true);

        assertThat(getRequest(service).isReadOnly(), is(false));
        assertThat(getAllRequest(service).isReadOnly(), is(false));
        }

    @Test
    public void shouldClassifyUnknownStorageAsWriteShaped()
        {
        TestService service = serviceWithMissingStorage();

        assertThat(getRequest(service).isReadOnly(), is(false));
        assertThat(getAllRequest(service).isReadOnly(), is(false));
        assertThat(new PartitionedCache.GetRequest().isReadOnly(), is(false));
        assertThat(new PartitionedCache.GetAllRequest().isReadOnly(), is(false));
        }

    @Test
    public void shouldClassifyKnownCacheWithoutLocalStorageAsReadOnly()
        {
        TestService service = serviceWithBinaryMapOnly();

        assertThat(getRequest(service).isReadOnly(), is(true));
        assertThat(getAllRequest(service).isReadOnly(), is(true));
        }

    @Test
    public void shouldCacheKnownCacheWithoutLocalStorageClassification()
        {
        TestBackingMapManager manager = new TestBackingMapManager(false);
        TestService           service = serviceWithBinaryMapOnly(manager);

        assertThat(getRequest(service).isReadOnly(), is(true));
        assertThat(getAllRequest(service).isReadOnly(), is(true));
        assertThat(getRequest(service).isReadOnly(), is(true));
        assertThat(manager.getMayWriteOnReadCount(), is(1));
        }

    @Test
    public void shouldClassifyDefaultCustomManagerWithoutLocalStorageAsWriteShaped()
        {
        TestBackingMapManager manager = new TestBackingMapManager(null);
        TestService           service = serviceWithBinaryMapOnly(manager);

        assertThat(getRequest(service).isReadOnly(), is(false));
        assertThat(getAllRequest(service).isReadOnly(), is(false));
        assertThat(manager.getMayWriteOnReadCount(), is(1));
        }

    @Test
    public void shouldClassifyLegacyXmlPlainCacheWithoutLocalStorageAsReadOnly()
        {
        TestService service = serviceWithBinaryMapOnly(legacyFactory("<local-scheme/>"));

        assertThat(getRequest(service).isReadOnly(), is(true));
        assertThat(getAllRequest(service).isReadOnly(), is(true));
        }

    @Test
    public void shouldClassifyLegacyXmlReadWriteBackingMapCacheWithoutLocalStorageAsWriteShaped()
        {
        TestService service = serviceWithBinaryMapOnly(legacyFactory(
                "<read-write-backing-map-scheme>"
              + "  <internal-cache-scheme><local-scheme/></internal-cache-scheme>"
              + "</read-write-backing-map-scheme>"));

        assertThat(getRequest(service).isReadOnly(), is(false));
        assertThat(getAllRequest(service).isReadOnly(), is(false));
        }

    @Test
    public void shouldClassifyLegacyXmlSlidingExpiryCacheWithoutLocalStorageAsWriteShaped()
        {
        TestService service = serviceWithBinaryMapOnly(legacyFactory(
                "<sliding-expiry>true</sliding-expiry>"
              + "<local-scheme/>"));

        assertThat(getRequest(service).isReadOnly(), is(false));
        assertThat(getAllRequest(service).isReadOnly(), is(false));
        }

    @Test
    public void shouldAllowPlainGetsAndRejectWritesWhenOnlyReadQuorumAllows()
        {
        TestService service = serviceWithStorage(false);
        service.setTestActionPolicy(new QuorumPolicy(true, false));

        assertAllowed(service, getRequest(service), CacheService.CacheAction.READ);
        assertAllowed(service, getAllRequest(service), CacheService.CacheAction.READ);
        assertRejected(service, putRequest(service), CacheService.CacheAction.WRITE);
        assertRejected(service, removeRequest(service), CacheService.CacheAction.WRITE);
        }

    @Test
    public void shouldRejectPlainGetsAndAllowWritesWhenOnlyWriteQuorumAllows()
        {
        TestService service = serviceWithStorage(false);
        service.setTestActionPolicy(new QuorumPolicy(false, true));

        assertRejected(service, getRequest(service), CacheService.CacheAction.READ);
        assertRejected(service, getAllRequest(service), CacheService.CacheAction.READ);
        assertAllowed(service, putRequest(service), CacheService.CacheAction.WRITE);
        assertAllowed(service, removeRequest(service), CacheService.CacheAction.WRITE);
        }

    @Test
    public void shouldRejectReadWriteBackingMapGetsWhenOnlyReadQuorumAllows()
        {
        assertWriteShapedGetsRejectedByReadOnlyQuorum(serviceWithStorage(true));
        }

    @Test
    public void shouldRejectSlidingExpiryGetsWhenOnlyReadQuorumAllows()
        {
        assertWriteShapedGetsRejectedByReadOnlyQuorum(serviceWithStorage(true));
        }

    @Test
    public void shouldRejectReadWriteBackingMapWithSlidingExpiryGetsWhenOnlyReadQuorumAllows()
        {
        assertWriteShapedGetsRejectedByReadOnlyQuorum(serviceWithStorage(true));
        }

    @Test
    public void shouldRejectMissingStorageGetsWhenOnlyReadQuorumAllows()
        {
        assertWriteShapedGetsRejectedByReadOnlyQuorum(serviceWithMissingStorage());
        }

    private static void assertWriteShapedGetsRejectedByReadOnlyQuorum(TestService service)
        {
        service.setTestActionPolicy(new QuorumPolicy(true, false));

        assertRejected(service, getRequest(service), CacheService.CacheAction.WRITE);
        assertRejected(service, getAllRequest(service), CacheService.CacheAction.WRITE);
        }

    private static void assertAllowed(TestService service, RequestMessage request, Action action)
        {
        service.checkQuorum(request, request.isReadOnly());
        assertThat(service.getTestActionPolicy().getLastAction(), is(action));
        }

    private static void assertRejected(TestService service, RequestMessage request, Action action)
        {
        try
            {
            service.checkQuorum(request, request.isReadOnly());
            fail("Expected RequestPolicyException");
            }
        catch (RequestPolicyException expected)
            {
            assertThat(service.getTestActionPolicy().getLastAction(), is(action));
            }
        }

    private static TestService serviceWithStorage(boolean fMayWriteOnRead)
        {
        return new TestService(new TestStorage(fMayWriteOnRead));
        }

    private static TestService serviceWithMissingStorage()
        {
        return new TestService(null);
        }

    private static TestService serviceWithBinaryMapOnly()
        {
        return serviceWithBinaryMapOnly(new TestBackingMapManager(false));
        }

    private static TestService serviceWithBinaryMapOnly(DefaultConfigurableCacheFactory factory)
        {
        return serviceWithBinaryMapOnly(factory.new Manager());
        }

    private static TestService serviceWithBinaryMapOnly(BackingMapManager manager)
        {
        TestService service = new TestService(null, manager);
        BinaryMap   map     = new BinaryMap("BinaryMap", service, true);

        map.setCacheId(CACHE_ID);
        map.setCacheName(CACHE_NAME);
        service.getBinaryMapArray().set(CACHE_ID, map);

        return service;
        }

    private static DefaultConfigurableCacheFactory legacyFactory(String sBackingMapScheme)
        {
        return new DefaultConfigurableCacheFactory(XmlHelper.loadXml(
                "<cache-config>"
              + "  <caching-scheme-mapping>"
              + "    <cache-mapping>"
              + "      <cache-name>" + CACHE_NAME + "</cache-name>"
              + "      <scheme-name>test-scheme</scheme-name>"
              + "    </cache-mapping>"
              + "  </caching-scheme-mapping>"
              + "  <caching-schemes>"
              + "    <distributed-scheme>"
              + "      <scheme-name>test-scheme</scheme-name>"
              + "      <backing-map-scheme>"
              +          sBackingMapScheme
              + "      </backing-map-scheme>"
              + "    </distributed-scheme>"
              + "  </caching-schemes>"
              + "</cache-config>"));
        }

    private static PartitionedCache.GetRequest getRequest(TestService service)
        {
        PartitionedCache.GetRequest request = new PartitionedCache.GetRequest();
        request.setService(service);
        request.setCacheId(CACHE_ID);
        request.setKey(binary("key"));
        return request;
        }

    private static PartitionedCache.GetAllRequest getAllRequest(TestService service)
        {
        PartitionedCache.GetAllRequest request = new PartitionedCache.GetAllRequest();
        request.setService(service);
        request.setCacheId(CACHE_ID);
        request.setKeySet(Set.of(binary("key")));
        return request;
        }

    private static PartitionedCache.PutRequest putRequest(TestService service)
        {
        PartitionedCache.PutRequest request = new PartitionedCache.PutRequest();
        request.setService(service);
        request.setCacheId(CACHE_ID);
        request.setKey(binary("key"));
        return request;
        }

    private static PartitionedCache.RemoveRequest removeRequest(TestService service)
        {
        PartitionedCache.RemoveRequest request = new PartitionedCache.RemoveRequest();
        request.setService(service);
        request.setCacheId(CACHE_ID);
        request.setKey(binary("key"));
        return request;
        }

    private static Binary binary(String s)
        {
        return ExternalizableHelper.toBinary(s);
        }

    private static class TestService
            extends PartitionedCache
        {
        TestService(Storage storage)
            {
            this(storage, new TestBackingMapManager(false));
            }

        TestService(Storage storage, BackingMapManager manager)
            {
            super("PartitionedCacheReadOnlyClassificationTest", null, true);
            f_storage = storage;
            f_manager = manager;
            }

        @Override
        public Storage getStorage(long lCacheId)
            {
            return lCacheId == CACHE_ID ? f_storage : null;
            }

        public void setTestActionPolicy(QuorumPolicy policy)
            {
            m_policy = policy;
            setActionPolicy(policy);
            }

        public QuorumPolicy getTestActionPolicy()
            {
            return m_policy;
            }

        @Override
        public BackingMapManager getBackingMapManager()
            {
            return f_manager;
            }

        private final Storage f_storage;
        private final BackingMapManager f_manager;
        private QuorumPolicy m_policy;
        }

    private static class TestStorage
            extends Storage
        {
        TestStorage(boolean fMayWriteOnRead)
            {
            super(null, null, true);
            f_fMayWriteOnRead = fMayWriteOnRead;
            }

        @Override
        public void onInit()
            {
            }

        @Override
        public boolean mayWriteOnRead()
            {
            return f_fMayWriteOnRead;
            }

        private final boolean f_fMayWriteOnRead;
        }

    private static class QuorumPolicy
            implements ActionPolicy
        {
        QuorumPolicy(boolean fAllowRead, boolean fAllowWrite)
            {
            f_fAllowRead  = fAllowRead;
            f_fAllowWrite = fAllowWrite;
            }

        @Override
        public void init(Service service)
            {
            }

        @Override
        public boolean isAllowed(Service service, Action action)
            {
            m_actionLast = action;
            if (action == CacheService.CacheAction.READ)
                {
                return f_fAllowRead;
                }
            if (action == CacheService.CacheAction.WRITE)
                {
                return f_fAllowWrite;
                }
            return true;
            }

        public Action getLastAction()
            {
            return m_actionLast;
            }

        private final boolean f_fAllowRead;
        private final boolean f_fAllowWrite;
        private Action m_actionLast;
        }

    private static class TestBackingMapManager
            implements BackingMapManager
        {
        TestBackingMapManager()
            {
            this(false);
            }

        TestBackingMapManager(Boolean fMayWriteOnRead)
            {
            f_fMayWriteOnRead = fMayWriteOnRead;
            }

        @Override
        public void init(BackingMapManagerContext context)
            {
            }

        @Override
        public com.tangosol.net.ConfigurableCacheFactory getCacheFactory()
            {
            return null;
            }

        @Override
        public BackingMapManagerContext getContext()
            {
            return null;
            }

        @Override
        public Map instantiateBackingMap(String sName)
            {
            return new SafeHashMap();
            }

        @Override
        public boolean isBackingMapPersistent(String sName)
            {
            return false;
            }

        @Override
        public boolean isBackingMapSlidingExpiry(String sName)
            {
            return false;
            }

        @Override
        public boolean mayWriteOnRead(String sName)
            {
            ++m_cMayWriteOnRead;
            return f_fMayWriteOnRead == null
                    ? BackingMapManager.super.mayWriteOnRead(sName)
                    : f_fMayWriteOnRead;
            }

        public int getMayWriteOnReadCount()
            {
            return m_cMayWriteOnRead;
            }

        @Override
        public StorageAccessAuthorizer getStorageAccessAuthorizer(String sName)
            {
            return null;
            }

        @Override
        public void releaseBackingMap(String sName, Map map)
            {
            }

        private final Boolean f_fMayWriteOnRead;
        private int m_cMayWriteOnRead;
        }

    private static final long CACHE_ID = 1L;
    private static final String CACHE_NAME = "test";
    }
