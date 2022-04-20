/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config;

import com.tangosol.coherence.config.scheme.PagedTopicScheme;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertThat;

/**
 * @author jk 2015.06.01
 */
public class CacheMappingRegistryTest
    {
    @Test
    public void shouldBeEmptyIterableWhenNew() throws Exception
        {
        CacheMappingRegistry registry = new CacheMappingRegistry(new SchemeMappingRegistry());

        assertThat(registry, is(emptyIterable()));
        }

    @Test
    public void shouldHaveZeroSizeWhenEmpty() throws Exception
        {
        CacheMappingRegistry registry = new CacheMappingRegistry(new SchemeMappingRegistry());

        assertThat(registry, is(emptyIterable()));
        }

    @Test
    public void shouldRegisterMapping() throws Exception
        {
        SchemeMappingRegistry schemeMappingRegistryRegistry = new SchemeMappingRegistry();
        CacheMapping          mapping  = new CacheMapping("foo", "bar");

        schemeMappingRegistryRegistry.register(mapping);

        CacheMappingRegistry registry = new CacheMappingRegistry(schemeMappingRegistryRegistry);


        assertThat(registry.findCacheMapping(mapping.getNamePattern()), is(sameInstance(mapping)));
        assertThat(registry.size(), is(1));
        assertThat(registry, containsInAnyOrder(mapping));
        }

    @Test
    public void shouldRegisterSubMappings() throws Exception
        {
        SchemeMappingRegistry registry      = new SchemeMappingRegistry();
        CacheMapping          mappingParent = new CacheMapping("Parent", "bar");
        CacheMapping          mappingChild1 = new CacheMapping("Child-1", "bar");
        CacheMapping          mappingChild2 = new CacheMapping("Child-2", "bar");

        List<ResourceMapping> subMappings = mappingParent.getSubMappings();

        subMappings.add(mappingChild1);
        subMappings.add(mappingChild2);

        registry.register(mappingParent);

        assertThat(registry.findMapping(mappingParent.getNamePattern(), CacheMapping.class), is(sameInstance(mappingParent)));
        assertThat(registry.findMapping(mappingChild1.getNamePattern(), CacheMapping.class), is(sameInstance(mappingChild1)));
        assertThat(registry.findMapping(mappingChild2.getNamePattern(), CacheMapping.class), is(sameInstance(mappingChild2)));
        assertThat(registry.size(), is(3));
        assertThat(registry, IsIterableContainingInAnyOrder.containsInAnyOrder(mappingParent, mappingChild1, mappingChild2));
        }

    @Test
    public void shouldNotAllowDuplicateCacheMappingRegistration() throws Exception
        {
        m_expectedException.expect(IllegalArgumentException.class);
        m_expectedException.expectMessage("Attempted to redefine an existing mapping for <cache-name>foo</cache-name>");

        SchemeMappingRegistry registry = new SchemeMappingRegistry();
        CacheMapping          mapping1  = new CacheMapping("foo", "bar");
        CacheMapping          mapping2  = new CacheMapping("foo", "bar");

        registry.register(mapping1);
        registry.register(mapping2);
        }

    @Test
    public void shouldAllowSchemaMappingOrCacheMappingRegistration() throws Exception
        {
        SchemeMappingRegistry resourceMappingRegistry = new SchemeMappingRegistry();
        CacheMappingRegistry  cacheMappingRegistry    = new CacheMappingRegistry(resourceMappingRegistry);

        CacheMapping          mapping1 = new CacheMapping("foo", "bar");
        TopicMapping          mapping2 = new TopicMapping("foo", "DistributedScheme", PagedTopicScheme.class);
        TopicMapping          mapping3 = new TopicMapping("bar", "DistributedScheme", PagedTopicScheme.class);
        CacheMapping          mapping4 = new CacheMapping("bar", "barbar");
        CacheMapping          mapping5 = new CacheMapping("foobar", "barbar");

        assertThat(resourceMappingRegistry.findMapping("foo", CacheMapping.class), nullValue());

        resourceMappingRegistry.register(mapping1);
        assertThat(resourceMappingRegistry.findMapping("foo", CacheMapping.class), is(mapping1));
        assertThat(resourceMappingRegistry.findMapping("foo", TopicMapping.class), nullValue());
        assertThat(cacheMappingRegistry.findCacheMapping("foo"), is(mapping1));

        resourceMappingRegistry.register(mapping2);
        assertThat(resourceMappingRegistry.findMapping("foo", CacheMapping.class), is(mapping1));
        assertThat(resourceMappingRegistry.findMapping("foo", TopicMapping.class), is(mapping2));
        assertThat(resourceMappingRegistry.findMapping("foo", ResourceMapping.class), is(mapping1));

        resourceMappingRegistry.register(mapping4);
        cacheMappingRegistry.register(mapping5);

        resourceMappingRegistry.register(mapping3);

        assertThat(resourceMappingRegistry.size(), is(5));

        Iterator<ResourceMapping> iter = resourceMappingRegistry.iterator();
        int cTopicMapping = 0;
        int cCacheMapping = 0;
        while(iter.hasNext())
            {
            ResourceMapping resource = iter.next();
            if (resource instanceof TopicMapping)
                {
                cTopicMapping++;
                }
            else if (resource instanceof CacheMapping)
                {
                cCacheMapping++;
                }
            }
        assertThat(cTopicMapping, is(2));
        assertThat(cCacheMapping, is(3));

        assertThat(cacheMappingRegistry.findCacheMapping("foo"), is(mapping1));
        assertThat(cacheMappingRegistry.findCacheMapping("foobar"), is(mapping5));
        assertThat(cacheMappingRegistry.findCacheMapping("bar"), is(mapping4));
        assertThat(cacheMappingRegistry.size(), is(3));
        }

    @Test
    public void shouldNotAllowDuplicateSubMappingRegistration() throws Exception
        {
        m_expectedException.expect(IllegalArgumentException.class);
        m_expectedException.expectMessage("Attempted to redefine an existing mapping for <cache-name>Child-1</cache-name>");

        SchemeMappingRegistry registry      = new SchemeMappingRegistry();
        CacheMapping          mappingParent = new CacheMapping("Parent", "bar");
        CacheMapping          mappingChild1 = new CacheMapping("Child-1", "bar");
        CacheMapping          mappingChild2 = new CacheMapping("Child-1", "bar");

        List<ResourceMapping> subMappings = mappingParent.getSubMappings();

        subMappings.add(mappingChild1);
        subMappings.add(mappingChild2);

        registry.register(mappingParent);
        }

    @Rule
    public ExpectedException m_expectedException = ExpectedException.none();
    }
