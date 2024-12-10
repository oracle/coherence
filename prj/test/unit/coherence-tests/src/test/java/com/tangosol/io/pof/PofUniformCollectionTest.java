/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.io.Serializer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * A collection of POF unit tests that test the PofConstants.T_IDENTITY in
 * uniform collections.
 *
 * @author lh 2013.08.07
 */
public class PofUniformCollectionTest
    {
    @BeforeClass
    public static void init()
        {
        SimplePofContext context = new SimplePofContext();
        context.registerUserType(1000, MapTestClass.class, new PortableObjectSerializer(1000));
        context.registerUserType(1001, CollectionTestClass.class, new PortableObjectSerializer(1001));
        PofUniformCollectionTest.s_serializer = context;
        }

    @Test
    public void testIntegerStringMap()
        {
        verifyPofSerialization(new MapTestClass<Integer,String>(Collections.singletonMap(PofConstants.T_IDENTITY, "A string"), true));
        }

    @Test
    public void testIntegerStringKeyOnly() 
        {
        verifyPofSerialization(new MapTestClass<Integer,String>(Collections.singletonMap(PofConstants.T_IDENTITY, "A string"), false));
        }

    @Test
    public void testStringIntegerKeyOnly() 
        {
        verifyPofSerialization(new MapTestClass<String,Integer>(Collections.singletonMap("A string", PofConstants.T_IDENTITY), false));
        }

    @Test
    public void testStringIntegerMap() 
        {
        verifyPofSerialization(new MapTestClass<String,Integer>(Collections.singletonMap("A string", PofConstants.T_IDENTITY), true));
        }

    @Test
    public void testIntegerSingleton()
        {
        verifyPofSerialization(new CollectionTestClass(Collections.singletonList(PofConstants.T_IDENTITY)));
        }

    @Test
    public void testIntegerCollection()
        {
        verifyPofSerialization(new CollectionTestClass(Arrays.asList(PofConstants.T_IDENTITY, 1)));
        }

    private void verifyPofSerialization(Object testObject)
        {
        Binary binary = ExternalizableHelper.toBinary(testObject, s_serializer);
        Object obj    = ExternalizableHelper.fromBinary(binary, s_serializer);

        assertEquals(testObject, obj);
        }

    // ----- inner class: MapTestClass ----------------------------------

    public static class MapTestClass<K, V> implements PortableObject
        {
        public MapTestClass()
            {
            }

        private MapTestClass(Map<K, V> map, boolean uniformValue)
            {
            this.m_map = map;
            this.m_uniformValue = uniformValue;
            }

        @SuppressWarnings("unchecked")
        public void readExternal(PofReader in) throws IOException
            {
            this.m_map = in.readMap(1, (Map) null);
            }

        public void writeExternal(PofWriter out) throws IOException
            {
            assertTrue(m_map != null);

            Map.Entry<K, V> entry = m_map.entrySet().iterator().next();
            Class<K> clzKey   = (Class<K>) entry.getKey().getClass();
            Class<V> clzValue = (Class<V>) entry.getValue().getClass();
            if (m_uniformValue)
                {
                out.writeMap(1, m_map, clzKey, clzValue);
                }
            else
                {
                out.writeMap(1, m_map, clzKey);
                }
            }

        @Override
        public String toString()
            {
            return "MapTestClass[map=" + m_map + ", uniformValue=" + m_uniformValue + "]";
            }

        @Override
        public int hashCode()
            {
            final int prime  = 31;
            int       result = 1;
            result = prime * result + ((m_map == null) ? 0 : m_map.hashCode());
            return result;
            }

        @Override
        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }
            if (o == null)
                {
                return false;
                }
            if (getClass() != o.getClass())
                {
                return false;
                }
            @SuppressWarnings("rawtypes")
            MapTestClass that = (MapTestClass) o;
            return Base.equals(m_map, that.m_map);
            }

        // ----- data members ---------------------------------------------------

        private Map<K,V> m_map;
        private boolean  m_uniformValue;
        }

    // ----- inner class: CollectionTestClass -------------------------------

    public static class CollectionTestClass implements PortableObject
        {
        public CollectionTestClass()
            {
            }

        private CollectionTestClass(Collection<Integer> coll)
            {
            this.m_coll = coll;
            }

        @SuppressWarnings("unchecked")
        public void readExternal(PofReader in) throws IOException
            {
            this.m_coll = in.readCollection(1, (Collection) null);
            }

        public void writeExternal(PofWriter out) throws IOException
            {
            assertTrue(m_coll != null);
            out.writeCollection(1, m_coll, m_coll.iterator().next().getClass());
            }

        public String toString()
            {
            return "CollectionTestClass[coll=" + m_coll + "]";
            }

        public int hashCode()
            {
            final int prime  = 31;
            int       result = 1;
            result = prime * result + ((m_coll == null) ? 0 : m_coll.hashCode());
            return result;
            }

        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }
            if (o == null)
                {
                return false;
                }
            if (getClass() != o.getClass())
                {
                return false;
                }
            CollectionTestClass that = (CollectionTestClass) o;
            return Base.equals(m_coll, that.m_coll);
            }

        // ----- data members ---------------------------------------------------

        private Collection<Integer> m_coll;
        }

    // ----- data members ---------------------------------------------------

    private static Serializer s_serializer;
    }