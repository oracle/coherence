/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package filter;

import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.junit.SessionBuilders;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Pof;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.filter.AllFilter;
import com.tangosol.util.filter.AnyFilter;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.NotFilter;

import com.tangosol.util.filter.XorFilter;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Set;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.tangosol.net.cache.TypeAssertion.withoutTypeChecking;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

import static org.junit.Assert.assertThat;

/**
 * @author jk 2015.08.18
 */
public class NotFilterTests
    {

    @BeforeClass
    public static void setup() throws Exception
        {
        ConfigurableCacheFactory ccf = s_clusterRunner.createSession(SessionBuilders.storageDisabledMember());

        s_filterOne   = new EqualsFilter<>(s_extractorOne, true);
        s_filterTwo   = new EqualsFilter<>(s_extractorTwo, true);
        s_filterThree = new EqualsFilter<>(s_extractorThree, true);
        s_aFilters    = new Filter[]{s_filterOne, s_filterTwo, s_filterThree};

        s_cache       = ccf.ensureTypedCache("dist-test", null, withoutTypeChecking());

        s_cache.put(0, new DomainObject(false, false, false));
        s_cache.put(1, new DomainObject(false, false, true));
        s_cache.put(2, new DomainObject(false, true,  false));
        s_cache.put(3, new DomainObject(false, true,  true));
        s_cache.put(4, new DomainObject(true, false, false));
        s_cache.put(5, new DomainObject(true, false, true));
        s_cache.put(6, new DomainObject(true, true, false));
        s_cache.put(7, new DomainObject(true, true, true));
        }

    @After
    public void cleanup()
        {
        s_cache.removeIndex(s_extractorOne);
        s_cache.removeIndex(s_extractorTwo);
        s_cache.removeIndex(s_extractorThree);
        }

    @Test
    public void shouldWrapAllFilterWithoutIndexes() throws Exception
        {
        Filter<DomainObject>   filter    = new NotFilter<>(new AllFilter(s_aFilters));
        Set<Integer>           setKeys   = s_cache.keySet(filter);

        assertThat(setKeys, containsInAnyOrder(0, 1, 2, 3, 4, 5, 6));
        }

    @Test
    public void shouldWrapAllFilterWithOneIndex() throws Exception
        {
        s_cache.addIndex(s_extractorOne, false, null);

        Filter<DomainObject>   filter    = new NotFilter<>(new AllFilter(s_aFilters));
        Set<Integer>           setKeys   = s_cache.keySet(filter);

        assertThat(setKeys, containsInAnyOrder(0, 1, 2, 3, 4, 5, 6));
        }

    @Test
    public void shouldWrapAllFilterWithTwoIndexes() throws Exception
        {
        s_cache.addIndex(s_extractorOne, false, null);
        s_cache.addIndex(s_extractorTwo, false, null);

        Filter<DomainObject> filter = new NotFilter<>(new AllFilter(s_aFilters));

        Eventually.assertThat(invoking(s_cache).keySet(filter), containsInAnyOrder(0, 1, 2, 3, 4, 5, 6));
        }

    @Test
    public void shouldWrapAllFilterWithThreeIndexes() throws Exception
        {
        s_cache.addIndex(s_extractorOne, false, null);
        s_cache.addIndex(s_extractorTwo, false, null);
        s_cache.addIndex(s_extractorThree, false, null);

        Filter<DomainObject> filter = new NotFilter<>(new AllFilter(s_aFilters));

        Eventually.assertThat(invoking(s_cache).keySet(filter), containsInAnyOrder(0, 1, 2, 3, 4, 5, 6));
        }

    @Test
    public void shouldWrapAnyFilterWithoutIndexes() throws Exception
        {
        Filter<DomainObject>   filter    = new NotFilter<>(new AnyFilter(s_aFilters));
        Set<Integer>           setKeys   = s_cache.keySet(filter);

        assertThat(setKeys, containsInAnyOrder(0));
        }

    @Test
    public void shouldWrapAnyFilterWithOneIndex() throws Exception
        {
        s_cache.addIndex(s_extractorOne, false, null);

        Filter<DomainObject> filter = new NotFilter<>(new AnyFilter(s_aFilters));
        Eventually.assertThat(invoking(s_cache).keySet(filter), containsInAnyOrder(0));
        }

    @Test
    public void shouldWrapAnyFilterWithTwoIndexes() throws Exception
        {
        s_cache.addIndex(s_extractorOne, false, null);
        s_cache.addIndex(s_extractorTwo, false, null);

        Filter<DomainObject> filter = new NotFilter<>(new AnyFilter(s_aFilters));
        Eventually.assertThat(invoking(s_cache).keySet(filter), containsInAnyOrder(0));
        }

    @Test
    public void shouldWrapAnyFilterWithThreeIndexes() throws Exception
        {
        s_cache.addIndex(s_extractorOne, false, null);
        s_cache.addIndex(s_extractorTwo, false, null);
        s_cache.addIndex(s_extractorThree, false, null);

        Filter<DomainObject> filter = new NotFilter<>(new AnyFilter(s_aFilters));
        Eventually.assertThat(invoking(s_cache).keySet(filter), containsInAnyOrder(0));
        }

    @Test
    public void shouldWrapXorFilterWithoutIndexes() throws Exception
        {
        Filter<DomainObject>   filter    = new NotFilter<>(new XorFilter(s_filterOne, s_filterTwo));
        Set<Integer>           setKeys   = s_cache.keySet(filter);

        assertThat(setKeys, containsInAnyOrder(0, 1, 6, 7));
        }

    @Test
    public void shouldWrapXorFilterWithOneIndex() throws Exception
        {
        s_cache.addIndex(s_extractorOne, false, null);

        Filter<DomainObject> filter = new NotFilter<>(new XorFilter(s_filterOne, s_filterTwo));
        Eventually.assertThat(invoking(s_cache).keySet(filter), containsInAnyOrder(0, 1, 6, 7));
        }

    @Test
    public void shouldWrapXorFilterWithTwoIndexes() throws Exception
        {
        s_cache.addIndex(s_extractorOne, false, null);
        s_cache.addIndex(s_extractorTwo, false, null);

        Filter<DomainObject> filter = new NotFilter<>(new XorFilter(s_filterOne, s_filterTwo));
        Eventually.assertThat(invoking(s_cache).keySet(filter), containsInAnyOrder(0, 1, 6, 7));
        }

    static
        {
        System.setProperty("coherence.pof.enabled", "true");
        }

    @ClassRule
    public static final CoherenceClusterResource s_clusterRunner = new CoherenceClusterResource()
            .include(2, LocalStorage.enabled())
            .with(ClusterName.of(NotFilterTests.class.getSimpleName() + "Cluster"),
                  Pof.enabled(),
                  Pof.config("filter-pof-config.xml"),
                  SystemProperty.of(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY,
                                    Config.getProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY)));

    public static final ValueExtractor<DomainObject,Boolean> s_extractorOne   = DomainObject::isOne;
    public static final ValueExtractor<DomainObject,Boolean> s_extractorTwo   = DomainObject::isTwo;
    public static final ValueExtractor<DomainObject,Boolean> s_extractorThree = DomainObject::isThree;

    public static Filter<DomainObject>                       s_filterOne;
    public static Filter<DomainObject>                       s_filterTwo;
    public static Filter<DomainObject>                       s_filterThree;

    public static Filter[]                                   s_aFilters;

    public static NamedCache<Integer, DomainObject>          s_cache;
    }
