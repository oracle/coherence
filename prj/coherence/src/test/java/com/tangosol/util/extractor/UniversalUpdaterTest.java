/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.extractor;

import com.tangosol.util.Base;

import java.lang.reflect.InvocationTargetException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.tangosol.util.WrapperException;

import org.junit.Test;

import static junit.framework.TestCase.assertFalse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
* Unit test of the {@link UniversalUpdater}.
*
* @author jf 08/11/2016
*/
public class UniversalUpdaterTest
        extends Base
    {

    @Test
    public void testEqualsHashCode()
        {
        UniversalUpdater ru1 = new UniversalUpdater("foo()");
        UniversalUpdater ru2 = new UniversalUpdater("foo");
        UniversalUpdater ru3 = new UniversalUpdater("setFoo()");

        assertFalse(ru1.equals(ru2));
        assertFalse(ru2.equals(ru1));
        assertNotEquals(ru1.hashCode(), ru2.hashCode());

        assertEquals(ru2.getCanonicalName(), ru3.getCanonicalName());
        assertTrue(ru2.equals(ru3));
        assertTrue(ru3.equals(ru2));
        assertEquals(ru2.hashCode(), ru3.hashCode());
        }

    /**
     * Test support for UniversalUpdater with a Map.
     */
    @Test
    public void testUpdateMap()
        {
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        map.put("key2", "value2");

        UniversalUpdater updater1 = new UniversalUpdater("key");
        assertNotNull(updater1.toString());
        updater1.update(map, "updatedValue");
        assertEquals("updatedValue", map.get("key"));

        // validate cached map target source code path works also.
        updater1.update(map, "updatedValue2");
        assertEquals("updatedValue2", map.get("key"));
        }

    public static class TestJavaBean
        {
        // ----- TestJavaBean methods ---------------------------------------

        public void setFoo(String s)
            {
            m_sFoo = s;
            }

        public String getFoo()
            {
            return m_sFoo;
            }

        public void setFlag(boolean f)
            {
            m_fFlag = f;
            }

        public boolean isFlag()
            {
            return m_fFlag;
            }

        // ----- data members -----------------------------------------------
        private String  m_sFoo;

        private boolean m_fFlag;
        }

    @Test
    public void testJavaBean()
        {
        TestJavaBean bean = new TestJavaBean();
        bean.setFoo("value");
        bean.setFlag(true);

        // test scenarios where java bean attribute is updated.
        UniversalUpdater updater = new UniversalUpdater("foo");
        updater.update(bean, "updatePropertyValue");
        assertEquals("updatePropertyValue", bean.getFoo());

        // ensure cached method approach works also
        updater.update(bean, "updatePropertyValue2");
        assertEquals("updatePropertyValue2", bean.getFoo());

        UniversalUpdater updater1 = new UniversalUpdater("flag");
        updater1.update(bean, false);
        assertFalse(bean.isFlag());

        // test scenarios where full javabean accessor method is provided.
        UniversalUpdater updater2 = new UniversalUpdater("setFoo()");
        updater2.update(bean, "updatePropertyValue3");
        assertEquals("updatePropertyValue3", bean.getFoo());

        // ensure cached method approach works also
        updater.update(bean, "updatePropertyValue4");
        assertEquals("updatePropertyValue4", bean.getFoo());

        UniversalUpdater updater3 = new UniversalUpdater("setFlag()");
        updater3.update(bean, true);
        assertTrue(bean.isFlag());
        }

    class UniversalUpdaterWithStatistics extends UniversalUpdater
        {

        public UniversalUpdaterWithStatistics(String name)
            {
            super(name);
            }

        protected void updateComplex(Object oTarget, Object oValue)
                throws InvocationTargetException, IllegalAccessException
            {
            nUpdateComplexCalls++;
            super.updateComplex(oTarget, oValue);
            }

        public int nUpdateComplexCalls;
        }

    @Test
    public void validateReflectionUpdaterMethodCaching()
        {
        ArrayList<TestJavaBean> beans = new ArrayList<>(10);

        for (int i = 0; i < 10; i++)
            {
            TestJavaBean bean = new TestJavaBean();
            bean.setFoo(new Integer(i).toString());
            bean.setFlag(false);
            beans.add(bean);
            }

        // test scenarios where method setter is updated.
        UniversalUpdaterWithStatistics updater = new UniversalUpdaterWithStatistics("foo");
        for (int i = 0; i < beans.size(); i++)
            {
            updater.update(beans.get(i), "newValue");
            assertEquals(beans.get(i).getFoo(), "newValue");
            }

        assertTrue("expected 1, observed " + updater.nUpdateComplexCalls, updater.nUpdateComplexCalls == 1);
        }

    @Test
    public void validateReflectionExtractorCaching()
        {
        ArrayList<TestJavaBean> beans = new ArrayList<>(10);

        for (int i = 0; i < 10; i++)
            {
            TestJavaBean bean = new TestJavaBean();
            bean.setFoo(new Integer(i).toString());
            bean.setFlag(false);
            beans.add(bean);
            }

        // test scenarios where java bean attribute is modified.
        UniversalUpdaterWithStatistics updater =
                new UniversalUpdaterWithStatistics("foo");
        for (int i = 0; i < 10; i++)
            {
            updater.update(beans.get(i), "newValue");
            assertEquals(beans.get(i).getFoo(), "newValue");
            }

        assertTrue("expected 1, observed " + updater.nUpdateComplexCalls, updater.nUpdateComplexCalls == 1);
        }

    @Test
    public void validateReflectionUpdaterMapCaching()
        {
        ArrayList<Map> maps = new ArrayList<>(10);

        for (int i = 0; i < 10; i++)
            {
            Map map = new HashMap();
            map.put("key", "value");
            maps.add(map);
            }

        // test scenarios where map access across different maps.
        UniversalUpdaterWithStatistics updater = new UniversalUpdaterWithStatistics("key");
        for (int i = 0; i < maps.size(); i++)
            {
            updater.update(maps.get(i), "newValue");
            assertEquals(maps.get(i).get("key"), "newValue");
            }

        assertTrue("expected 1, observed " + updater.nUpdateComplexCalls, updater.nUpdateComplexCalls == 1 && maps.size() == 10);
        }


    @SuppressWarnings("unchecked")
    @Test(expected = IllegalArgumentException.class)
    public void testDefaultReflectionBlacklistAgainstClass()
        throws Throwable
        {
        UniversalUpdater extractor = new UniversalUpdater("isInstance()");
        try
            {
            extractor.update(String.class, "stringInstance");
            }
        catch (WrapperException e)
            {
            throw e.getOriginalException();
            }
        }

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalArgumentException.class)
    public void testDefaultReflectionBlacklistAgainstRuntime()
        throws Throwable
        {
        UniversalUpdater extractor = new UniversalUpdater("exec()");
        try
            {
            extractor.update(Runtime.getRuntime(), "command");
            }
        catch (WrapperException e)
            {
            throw e.getOriginalException();
            }
        }
    }
