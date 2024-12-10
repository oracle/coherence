/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.oracle.coherence.common.util.MemorySize;

import com.tangosol.coherence.config.builder.NamedEventInterceptorBuilder;
import com.tangosol.coherence.config.builder.InstanceBuilder;
import com.tangosol.coherence.config.scheme.DistributedScheme.BackupConfig;

import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.CacheService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit Tests for a {@link DistributedScheme}.
 *
 * @author pfm  2012.06.27
 */
public class DistributedSchemeTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the default settings.
     */
    @Test
    public void testDefaults()
        {
        DistributedScheme scheme = new DistributedScheme();

        assertEquals(CacheService.TYPE_DISTRIBUTED, scheme.getServiceType());
        assertTrue(scheme.isRunningClusterNeeded());
        assertNotNull(scheme.getBackingMapScheme());
        assertNotNull(scheme.getBackupMapConfig());
        assertNull(scheme.getBundleManager());
        assertNull(scheme.getEventInterceptorBuilders());
        }

    /**
     * Test the setters.
     */
    @Test
    public void testSetters()
        {
        DistributedScheme scheme = new DistributedScheme();

        BackingMapScheme schemeBM = new BackingMapScheme();
        scheme.setBackingMapScheme(schemeBM);
        assertEquals(schemeBM, scheme.getBackingMapScheme());

        BackupMapConfig config = new DistributedScheme.BackupConfig();
        scheme.setBackupMapConfig(config);
        assertEquals(config, scheme.getBackupMapConfig());

        BundleManager mgr = new BundleManager();
        scheme.setBundleManager(mgr);
        assertEquals(mgr, scheme.getBundleManager());

        List<NamedEventInterceptorBuilder> bldrs  = new ArrayList<NamedEventInterceptorBuilder>();
        scheme.setEventInterceptorBuilders(bldrs);
        assertEquals(bldrs, scheme.getEventInterceptorBuilders());
        }

    /**
     * Test the default settings.
     */
    @Test
    public void testBackupConfigDefaults()
        {
        BackupConfig      config   = new BackupConfig();
        ParameterResolver resolver = new NullParameterResolver();

        assertNull(config.getCustomBuilder());
        assertEquals("",config.getBackupSchemeName(resolver));
        assertEquals("",config.getDirectory(resolver));
        assertEquals(new MemorySize("1M").getByteCount(), config.getInitialSize(resolver));
        assertEquals(new MemorySize("1024M").getByteCount(), config.getMaximumSize(resolver));
        assertEquals(BackingMapScheme.ON_HEAP,config.resolveType(resolver, new LocalScheme()));
        }

    /**
     * Test the default settings.
     */
    @Test
    public void testBackupConfigSetters()
        {
        BackupConfig      config   = new BackupConfig();
        ParameterResolver resolver = new NullParameterResolver();

        InstanceBuilder<Map> bldr = new InstanceBuilder<Map>();
        config.setCustomBuilder(bldr);
        assertEquals(bldr, config.getCustomBuilder());

        config.setBackupSchemeName(new LiteralExpression<String>("foo"));
        assertEquals("foo",config.getBackupSchemeName(resolver));

        config.setDirectory(new LiteralExpression<String>("dir"));
        assertEquals("dir",config.getDirectory(resolver));

        MemorySize initialSize = new MemorySize("20M");
        config.setInitialSize(new LiteralExpression<MemorySize>(initialSize));
        assertEquals(initialSize.getByteCount(), config.getInitialSize(resolver));

        MemorySize maxSize = new MemorySize("30M");
        config.setMaximumSize(new LiteralExpression<MemorySize>(maxSize));
        assertEquals(maxSize.getByteCount(), config.getMaximumSize(resolver));

        config.setType(new LiteralExpression<String>("custom"));
        assertEquals(BackingMapScheme.CUSTOM,config.resolveType(resolver, new LocalScheme()));

        config.setType(new LiteralExpression<String>("file-mapped"));
        assertEquals(BackingMapScheme.FILE_MAPPED,config.resolveType(resolver, new LocalScheme()));

        config.setType(new LiteralExpression<String>("on-heap"));
        assertEquals(BackingMapScheme.ON_HEAP,config.resolveType(resolver, new LocalScheme()));

        config.setType(new LiteralExpression<String>("off-heap"));
        assertEquals(BackingMapScheme.OFF_HEAP,config.resolveType(resolver, new LocalScheme()));

        config.setType(new LiteralExpression<String>("scheme"));
        assertEquals(BackingMapScheme.SCHEME,config.resolveType(resolver, new LocalScheme()));
        }
    }
