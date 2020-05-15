/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;

import java.lang.reflect.Field;

import java.util.concurrent.atomic.AtomicBoolean;

import java.util.function.Supplier;

import org.junit.After;
import org.junit.Test;

import static org.hamcrest.Matchers.is;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Test methods within {@link CacheFactory}.
 *
 * @author coh 2010.01.18, hr  2011.01.26
 *
 * @since Coherence
 */
public class CacheFactoryTest
    {

    @After
    public void cleanup()
        {
        System.setProperty("tangosol.coherence.edition", "GE");
        clearCoherence();
        }

    @Test(expected = IllegalArgumentException.class)
    public void testLogWithNullSupplier()
        {
        try
            {
            CacheFactory.log((Supplier<String>) null, CacheFactory.LOG_ERR);
            }
        catch (Exception e)
            {
            e.printStackTrace();
            throw e;
            }
        }

    @Test
    public void testLogWithSupplier()
        {
        final AtomicBoolean fSupplerCalled = new AtomicBoolean();
        CacheFactory.log(() ->
                         {
                         fSupplerCalled.compareAndSet(false, true);
                         return "";
                         },
                         CacheFactory.LOG_ERR);

        assertThat(fSupplerCalled.get(), is(true));

        fSupplerCalled.compareAndSet(true, false);

        CacheFactory.log(() ->
                         {
                         fSupplerCalled.compareAndSet(false, true);
                         return "";
                         },
                         CacheFactory.LOG_MAX);

        assertThat(fSupplerCalled.get(), is(false));
        }

    @Test
    public void testLocalMember()
        {
        Member memberLocal = CacheFactory.getCluster().getLocalMember();
        assertNotNull(memberLocal);
        }

    /**
     * Test the call to {@link CacheFactory#getEdition()}.
     */
    @Test
    public void testGetEdition()
        {
        assertThat(CacheFactory.getEdition(), is("CE"));

        clearCoherence();

        String sOldEdition = System.setProperty("tangosol.coherence.edition", "EE");
        try
            {
            assertThat(CacheFactory.getEdition(), is("EE"));
            }
        finally
            {
            System.setProperty("tangosol.coherence.edition", sOldEdition == null ? "" : sOldEdition);
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Clear down the Coherence class so that the edition can be replaced.
     * <p>
     * We hack via reflection the value of the product to avoid an assertion
     * exception which is valid in the majority of use cases.     *
     */
    protected void clearCoherence()
        {
        try
            {
            Class<?> clz          = Base.getContextClassLoader().loadClass("com.tangosol.coherence.component.application.console.Coherence");
            Object   appCoh       = clz.getMethod("get_Instance").invoke(null, ClassHelper.VOID);

            // We need to get the SafeCluster here otherwise calling shutdown
            // will not clear out the configuration map
            clz.getMethod("getSafeCluster").invoke(null, ClassHelper.VOID);
            CacheFactory.shutdown();

            Field[]  aFields      = clz.getDeclaredFields();
            Field    fieldProduct = null;
            for (int i = 0; fieldProduct == null && i < aFields.length; ++i)
                {
                Field field = aFields[i];
                if (field.getName().endsWith("Product"))
                    {
                    fieldProduct = field;
                    }
                }
            if (fieldProduct == null)
                {
                return;
                }
            fieldProduct.setAccessible(true);
            fieldProduct.set(appCoh, null);
            }
        catch (Exception e)
            {
            }
        }
    }
