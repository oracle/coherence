/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.tangosol.util.Binary;

import data.pof.EvolvablePortablePerson;
import data.pof.PortablePerson;
import data.pof.PortablePersonLite;

import org.junit.Test;

import java.io.IOException;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;


/**
* Unit tests of POF serialization/deserialization of java.util.Collection
* classes.
*
* @author gm 2006.12.21
* @author jh 2006.12.27
*/
public class PofCollectionTest
        extends AbstractPofTest
    {
    @Test
    public void testPofWriterWriteObjectArray()
            throws IOException
        {
        Object[] ao1 = {"test", "test3", "testPOF1"};
        Object[] ao2 = {"test", "test1", "testPOF2"};
        Object[] ao3 = new Object[0];
        Object[] ao  = new Object[]{11, "test", Boolean.TRUE};

        initPOFWriter();
        m_writer.writeObjectArray(0, null);
        m_writer.writeObjectArray(0, new Object[0]);
        m_writer.writeObjectArray(0, ao1);
        m_writer.writeObjectArray(0, ao2);
        m_writer.writeObjectArray(0, ao3);
        m_writer.writeObjectArray(0, ao);

        initPOFReader();
        assertNull(m_reader.readObjectArray(0, (Object[]) null));
        assertArrayEquals(m_reader.readObjectArray(0, new Object[0]), new Object[0]);
        assertArrayEquals(m_reader.readObjectArray(0, new Object[3]), ao1);
        assertArrayEquals(m_reader.readObjectArray(0, new Object[3]), ao2);
        assertArrayEquals(m_reader.readObjectArray(0, new Object[0]), ao3);
        assertArrayEquals(m_reader.readObjectArray(0, new Object[3]), ao);
        }

    @Test
    public void testPofWriterWriteUniformObjectArray()
            throws IOException
        {
        Object[] ao1 = {"test", "test3", "testPOF1"};
        Object[] ao2 = {32, Integer.MAX_VALUE, -1};

        initPOFWriter();
        m_writer.writeObjectArray(0, ao1, String.class);
        m_writer.writeObjectArray(0, ao2, Integer.class);

        initPOFReader();
        assertArrayEquals(m_reader.readObjectArray(0, new Object[3]), ao1);
        assertArrayEquals(m_reader.readObjectArray(0, new Object[3]), ao2);
        }

    // test case for COH-3370
    @Test
    public void testPofWriterWriteUniformObjectArrayWithNull()
            throws IOException
        {
        Object[] ao1 = {"test", "test3", null, null, "test4"};
        Object[] ao2 = {32, Integer.MAX_VALUE, -1, null};

        initPOFWriter();
        m_writer.writeObjectArray(0, ao1, String.class);
        m_writer.writeObjectArray(0, ao2, Integer.class);

        initPOFReader();
        assertArrayEquals(ao1, m_reader.readObjectArray(0, new Object[5]));
        assertArrayEquals(ao2, m_reader.readObjectArray(0, new Object[4]));
        }

    @Test
    public void testPofWriterWriteCollection()
            throws IOException
        {
        Collection col1 = new ArrayList();
        Collection col2 = new ArrayList();
        col1.add("A");
        col1.add("Z");
        col1.add("7");
        col2.add(32);
        col2.add(Integer.MIN_VALUE);
        col2.add(Integer.MAX_VALUE);

        initPOFWriter();
        m_writer.writeCollection(0, null);
        m_writer.writeCollection(0, col1);
        m_writer.writeCollection(0, col2);
        m_writer.writeCollection(0, col1);
        m_writer.writeDate(0, new Date(2006, 8, 8));

        initPOFReader();
        assertNull(m_reader.readCollection(0, (Collection) null));
        assertArrayEquals(m_reader.readCollection(0, new ArrayList(3)).toArray(), col1.toArray());
        assertArrayEquals(m_reader.readCollection(0, new ArrayList(3)).toArray(), col2.toArray());
        assertArrayEquals(m_reader.readCollection(0, (Collection) null).toArray(), col1.toArray());
        try
            {
            assertArrayEquals(m_reader.readCollection(0, (Collection) null).toArray(), col1.toArray()); // exception
            fail("expected exception");
            }
        catch (IOException e)
            {
            // expected
            }
        }

    @Test
    public void testPofWriterWriteCollectionEx1()
            throws IOException
        {
        List list1 = new ArrayList();
        list1.add("A");
        list1.add("Z");
        list1.add("7");

        initPOFWriter();
        m_writer.writeCollection(0, list1);
        m_writer.writeCollection(0, new ArrayList(0));

        initPOFReader();
        Object o1 = m_reader.readObject(0);
        assertTrue(o1 instanceof Collection);
        List listTmp = new ArrayList((Collection) o1);
        assertTrue(listTmp.size() == 3);
        assertEquals(list1.get(0), listTmp.get(0));
        assertEquals(list1.get(1), listTmp.get(1));
        assertEquals(list1.get(2), listTmp.get(2));

        o1 = m_reader.readObject(0);
        assertTrue(o1 instanceof Collection);
        listTmp = new ArrayList((Collection) o1);
        assertTrue(listTmp.size() == 0);
        }

    @Test
    public void testPofWriterWriteUniformCollection()
            throws IOException
        {
        List list1 = new ArrayList();
        List list2 = new ArrayList();
        list1.add("A");
        list1.add("Z");
        list1.add("7");
        list2.add(32);
        list2.add(Integer.MIN_VALUE);
        list2.add(Integer.MAX_VALUE);

        initPOFWriter();
        m_writer.writeCollection(0, list1, String.class);
        m_writer.writeCollection(0, list2, Integer.class);
        m_writer.writeCollection(0, new ArrayList(0), String.class);
        m_writer.writeCollection(0, null, String.class);

        initPOFReader();
        Collection col1 = m_reader.readCollection(0, new ArrayList());
        assertTrue(col1 instanceof ArrayList);
        List listTmp = (ArrayList) col1;
        assertTrue(listTmp.size() == 3);
        assertEquals(list1.get(0), listTmp.get(0));
        assertEquals(list1.get(1), listTmp.get(1));
        assertEquals(list1.get(2), listTmp.get(2));

        Collection col2 = m_reader.readCollection(0, (Collection) null);
        assertArrayEquals(list2.toArray(), col2.toArray());
        assertEquals(new ArrayList(), m_reader.readCollection(0, new ArrayList()));
        }

    // test case for COH-3370
    @Test
    public void testPofWriterWriteUniformCollectionWithNulls()
            throws IOException
        {
        List list1 = new ArrayList();
        List list2 = new ArrayList();
        List list3 = new ArrayList();
        List list4 = new ArrayList();
        list1.add("A");
        list1.add("Z");
        list1.add("7");
        list1.add(null);
        list2.add(32);
        list2.add(PofConstants.V_REFERENCE_NULL);
        list2.add(Integer.MIN_VALUE);
        list2.add(Integer.MAX_VALUE);
        list2.add(-1);
        list2.add(null);
        list3.add(64L);
        list3.add(Long.MIN_VALUE);
        list3.add(Long.MAX_VALUE);
        list3.add(null);
        list3.add((long) -1);
        list4.add((short) 16);
        list4.add(Short.MIN_VALUE);
        list4.add(Short.MAX_VALUE);
        list4.add(null);
        list4.add((short) -1);

        initPOFWriter();
        m_writer.writeCollection(0, list1, String.class);
        m_writer.writeCollection(0, list2, Integer.class);
        m_writer.writeCollection(0, list3, Long.class);
        m_writer.writeCollection(0, list4, Short.class);
        m_writer.writeCollection(0, new ArrayList(0), String.class);
        m_writer.writeCollection(0, null, String.class);

        initPOFReader();
        Collection col1 = m_reader.readCollection(0, new ArrayList());
        assertTrue(col1 instanceof ArrayList);
        List listTmp = (ArrayList) col1;
        assertTrue(listTmp.size() == list1.size());
        assertEquals(list1.get(0), listTmp.get(0));
        assertEquals(list1.get(1), listTmp.get(1));
        assertEquals(list1.get(2), listTmp.get(2));
        assertEquals(list1.get(3), listTmp.get(3));

        Collection col2 = m_reader.readCollection(0, new ArrayList());
        assertTrue(col2 instanceof ArrayList);
        listTmp = (ArrayList) col2;
        assertTrue(listTmp.size() == list2.size());
        assertEquals(list2.get(0), listTmp.get(0));
        assertEquals(list2.get(1), listTmp.get(1));
        assertEquals(list2.get(2), listTmp.get(2));
        assertEquals(list2.get(3), listTmp.get(3));
        assertEquals(list2.get(4), listTmp.get(4));
        assertEquals(list2.get(5), listTmp.get(5));

        Collection col3 = m_reader.readCollection(0, new ArrayList());
        assertTrue(col3 instanceof ArrayList);
        listTmp = (ArrayList) col3;
        assertTrue(listTmp.size() == list3.size());
        assertEquals(list3.get(0), listTmp.get(0));
        assertEquals(list3.get(1), listTmp.get(1));
        assertEquals(list3.get(2), listTmp.get(2));
        assertEquals(list3.get(3), listTmp.get(3));
        assertEquals(list3.get(4), listTmp.get(4));

        Collection col4 = m_reader.readCollection(0, new ArrayList());
        assertTrue(col4 instanceof ArrayList);
        listTmp = (ArrayList) col4;
        assertTrue(listTmp.size() == list4.size());
        assertEquals(list4.get(0), listTmp.get(0));
        assertEquals(list4.get(1), listTmp.get(1));
        assertEquals(list4.get(2), listTmp.get(2));
        assertEquals(list4.get(3), listTmp.get(3));
        assertEquals(list4.get(4), listTmp.get(4));

        assertEquals(new ArrayList(), m_reader.readCollection(0, new ArrayList()));
        assertEquals(null, m_reader.readCollection(0, (Collection) null));
        }

    @Test
    public void testPofWriterWriteMap()
            throws IOException
        {
        Map map1 = new HashMap();
        Map map2 = new HashMap();
        map1.put(0, "A");
        map1.put(1, "Z");
        map1.put(2, "7");
        map2.put(5, 32);
        map2.put(10, Integer.MIN_VALUE);
        map2.put(15, Integer.MAX_VALUE);

        initPOFWriter();
        m_writer.writeMap(0, map1);
        m_writer.writeMap(0, map2);
        m_writer.writeMap(0, null);
        m_writer.writeMap(0, map1);

        initPOFReader();
        Map mapR1 = m_reader.readMap(0, new HashMap(3));
        Map mapR2 = m_reader.readMap(0, new HashMap(3));

        Collection colVal1  = map1.values();
        Collection colVal2  = map2.values();
        Collection colValR1 = mapR1.values();
        Collection colValR2 = mapR2.values();

        Collection colKey1  = map1.keySet();
        Collection colKey2  = map2.keySet();
        Collection colKeyR1 = mapR1.keySet();
        Collection colKeyR2 = mapR2.keySet();

        // compare col1 with result 1
        assertTrue(colVal1.containsAll(colValR1));
        assertTrue(colKey1.containsAll(colKeyR1));
        assertEquals(colVal1.size(), colValR1.size());
        assertEquals(colKey1.size(), colKeyR1.size());

        // compare col2 with result 2
        assertTrue(colVal2.containsAll(colValR2));
        assertTrue(colKey2.containsAll(colKeyR2));
        assertEquals(colVal2.size(), colValR2.size());
        assertEquals(colKey2.size(), colKeyR2.size());

        assertEquals(m_reader.readMap(0, (Map) null), null);
        Map rcol3 = m_reader.readMap(0, (Map) null);
        Collection valRCol3 = rcol3.values();
        Collection keyRCol3 = rcol3.keySet();

        // compare col1 with result 3
        assertTrue(colVal1.containsAll(valRCol3));
        assertTrue(colKey1.containsAll(keyRCol3));
        assertEquals(colVal1.size(), valRCol3.size());
        assertEquals(colKey1.size(), colKeyR2.size());
        }

    @Test
    public void testPofWriterWriteMapEx1()
            throws IOException
        {
        Map map1 = new HashMap();
        Map map2 = new HashMap();
        map1.put((short) 0, "A");
        map1.put((short) 1, "Z");
        map1.put((short) 2, "7");

        map2.put(5, 32);
        map2.put(10, Integer.MIN_VALUE);
        map2.put(15, Integer.MAX_VALUE);

        initPOFWriter();
        m_writer.writeMap(0, map1, Short.class);
        m_writer.writeMap(0, map2, Integer.class);
        m_writer.writeMap(0, null, Integer.class);

        initPOFReader();
        Map mapR1 = m_reader.readMap(0, new HashMap(3));
        Map mapR2 = m_reader.readMap(0, (Map) null);

        Collection colVal1  = map1.values();
        Collection colVal2  = map2.values();
        Collection colValR1 = mapR1.values();
        Collection colValR2 = mapR2.values();

        Collection colKey1  = map1.keySet();
        Collection colKey2  = map2.keySet();
        Collection colKeyR1 = mapR1.keySet();
        Collection colKeyR2 = mapR2.keySet();

        // compare col1 with result 1
        assertTrue(colVal1.containsAll(colValR1));
        assertTrue(colKey1.containsAll(colKeyR1));
        assertEquals(colVal1.size(), colValR1.size());
        assertEquals(colKey1.size(), colKeyR1.size());

        // compare col2 with result 2
        assertTrue(colVal2.containsAll(colValR2));
        assertTrue(colKey2.containsAll(colKeyR2));
        assertEquals(colVal2.size(), colValR2.size());
        assertEquals(colKey2.size(), colKeyR2.size());
        }

    @Test
    public void testPofWriterWriteUniformMap()
            throws IOException
        {
        Map map1 = new HashMap();
        Map map2 = new HashMap();
        map1.put((short) 0, "A");
        map1.put((short) 1, "Z");
        map1.put((short) 2, "7");

        map2.put(5, 32);
        map2.put(10, Integer.MIN_VALUE);
        map2.put(15, Integer.MAX_VALUE);

        initPOFWriter();
        m_writer.writeMap(0, map1, Short.class, String.class);
        m_writer.writeMap(0, map2, Integer.class, Integer.class);
        m_writer.writeDate(0, new Date(2006, 8, 8));

        initPOFReader();
        Map mapR1 = m_reader.readMap(0, new HashMap(3));
        Map mapR2 = m_reader.readMap(0, (Map) null);

        Collection colVal1  = map1.values();
        Collection colVal2  = map2.values();
        Collection colValR1 = mapR1.values();
        Collection colValR2 = mapR2.values();

        Collection colKey1  = map1.keySet();
        Collection colKey2  = map2.keySet();
        Collection colKeyR1 = mapR1.keySet();
        Collection colKeyR2 = mapR2.keySet();

        // compare col1 with result 1
        assertTrue(colVal1.containsAll(colValR1));
        assertTrue(colKey1.containsAll(colKeyR1));
        assertEquals(colVal1.size(), colValR1.size());
        assertEquals(colKey1.size(), colKeyR1.size());

        // compare col2 with result 2
        assertTrue(colVal2.containsAll(colValR2));
        assertTrue(colKey2.containsAll(colKeyR2));
        assertEquals(colVal2.size(), colValR2.size());
        assertEquals(colKey2.size(), colKeyR2.size());

        try
            {
            m_reader.readMap(0, (Map) null);
            fail("expected exception");
            }
        catch (IOException e)
            {
            // expected
            }
        }

    // test case for COH-3370
    @Test
    public void testPofWriterWriteUniformMapWithNulls()
            throws IOException
        {
        Map map1 = new HashMap();
        Map map2 = new HashMap();
        map1.put((short) 0, "A");
        map1.put((short) 1, "Z");
        map1.put((short) 2, "7");
        map1.put((short) 3, null);
        map1.put(null, null);

        map2.put(5, 32);
        map2.put(10, Integer.MIN_VALUE);
        map2.put(15, Integer.MAX_VALUE);
        map2.put(20, null);
        map2.put(null, 77);

        initPOFWriter();
        m_writer.writeMap(0, map1, Short.class, String.class);
        m_writer.writeMap(0, map2, Integer.class, Integer.class);
        m_writer.writeDate(0, new Date(2006, 8, 8));

        initPOFReader();
        Map mapR1 = m_reader.readMap(0, new HashMap(3));
        Map mapR2 = m_reader.readMap(0, new HashMap());

        Collection colVal1  = map1.values();
        Collection colValR1 = mapR1.values();

        Collection colKey1  = map1.keySet();
        Collection colKeyR1 = mapR1.keySet();

        // compare col1 with result 1
        assertTrue(colVal1.containsAll(colValR1));
        assertTrue(colKey1.containsAll(colKeyR1));
        assertEquals(colVal1.size(), colValR1.size());
        assertEquals(colKey1.size(), colKeyR1.size());

        // compare map2 with result 2
        assertEquals(mapR2.size(), map2.size());
        assertEquals(mapR2.get(5), map2.get(5));
        assertEquals(mapR2.get(10), map2.get(10));
        assertEquals(mapR2.get(15), map2.get(15));
        assertEquals(mapR2.get(20), map2.get(20));
        assertEquals(mapR2.get(null), map2.get(null));

        try
            {
            m_reader.readMap(0, (Map) null);
            fail("expected exception");
            }
        catch (IOException e)
            {
            // expected
            }
        }

    @Test
    public void testPofWriterWriteMapEx3()
            throws IOException
        {
        Map col1 = new HashMap();
        Map col2 = new HashMap();

        col1.put(0, "A");
        col1.put(1, "G");
        col1.put(2, "7");

        col2.put(0, new java.sql.Date(106, 7, 7));
        col2.put(1, new Time(7, 7, 7));
        col2.put(2, new Date(106, 7, 8));
        col2.put(3, new Timestamp(106, 7, 8, 0, 0, 0, 0));
        col2.put(10, col1);
        col2.put(15, Double.POSITIVE_INFINITY);
        col2.put(20, Double.NEGATIVE_INFINITY);
        col2.put(25, Double.NaN);
        col2.put(30, Float.POSITIVE_INFINITY);
        col2.put(35, Float.NEGATIVE_INFINITY);
        col2.put(40, Float.NaN);

        initPOFWriter();
        m_writer.writeMap(0, col2);

        initPOFReader();
        Map colR2 = m_reader.readMap(0, new HashMap(3));

        Collection colVal1  = col1.values();
        Collection colVal2  = col2.values();
        Collection colValR2 = colR2.values();
        Collection colKey1  = col1.keySet();
        Collection colKey2  = col2.keySet();
        Collection colKeyR2 = colR2.keySet();

        // compare col2 with result 2
        assertTrue(colVal2.containsAll(colValR2));
        assertTrue(colKey2.containsAll(colKeyR2));
        assertEquals(colVal2.size(), colValR2.size());
        assertEquals(colKey2.size(), colKeyR2.size());

        Object obj = colR2.get(10);

        assertTrue(obj instanceof HashMap);
        HashMap    hashCol    = (HashMap) obj;
        Collection valHashCol = hashCol.values();
        Collection keyHashCol = hashCol.keySet();

        // compare hashCol with result 1
        assertTrue(valHashCol.containsAll(colVal1));
        assertTrue(keyHashCol.containsAll(colKey1));

        assertEquals(valHashCol.size(), colVal1.size());
        assertEquals(keyHashCol.size(), colKey1.size());
        }

    @Test
    public void testPofWriterWriteMapEx4()
            throws IOException
        {
        Map map1 = new HashMap();
        map1.put(0, "A");
        map1.put(1, "G");
        map1.put(2, "7");

        initPOFWriter();
        m_writer.writeMap(0, map1, Integer.class);
        m_writer.writeMap(0, map1, Integer.class, String.class);
        m_writer.writeMap(0, new HashMap(0), String.class);
        m_writer.writeMap(0, new HashMap(0), String.class, String.class);
        m_writer.writeMap(0, null, String.class, String.class);

        initPOFReader();
        Object o = m_reader.readObject(0);
        assertTrue(o instanceof Map);
        Map mapR1 = (Map) o;
        assertEquals(mapR1.size(), map1.size());
        assertEquals(mapR1.get(0), map1.get(0));
        assertEquals(mapR1.get(1), map1.get(1));
        assertEquals(mapR1.get(2), map1.get(2));

        o = m_reader.readObject(0);
        assertTrue(o instanceof Map);
        mapR1 = (Map) o;
        assertEquals(mapR1.size(), map1.size());
        assertEquals(mapR1.get(0), map1.get(0));
        assertEquals(mapR1.get(1), map1.get(1));
        assertEquals(mapR1.get(2), map1.get(2));
        }

    @Test
    public void testWriteGenericList()
            throws IOException
        {
        SimplePofContext ctx = new SimplePofContext();
        ctx.registerUserType(101, PortablePersonLite.class, new PortableObjectSerializer(101));
        ctx.registerUserType(102, PortablePerson.class, new PortableObjectSerializer(102));
        ctx.registerUserType(103, EvolvablePortablePerson.class, new PortableObjectSerializer(103));

        initPOFWriter();
        m_writer = new PofBufferWriter(m_bo, ctx);

        Collection<String> col1 = new ArrayList<String>();
        col1.add("A");
        col1.add("Z");
        col1.add("7");

        Collection<PortableObject> colPerson  = new ArrayList<PortableObject>();
        Collection<PortablePerson> colPerson2 = new ArrayList<PortablePerson>();

        PortablePerson ivan = new PortablePerson("Ivan", new java.util.Date(78, 4, 25));
        ivan.setChildren(null);

        PortablePerson goran = new PortablePerson("Goran", new java.util.Date(82, 3, 3));
        goran.setChildren(null);

        EvolvablePortablePerson aleks = new EvolvablePortablePerson("Aleks", new java.util.Date(74, 8, 24));
        aleks.setChildren(new EvolvablePortablePerson[1]);
        aleks.getChildren()[0] = new EvolvablePortablePerson("Ana Maria", new java.util.Date(104, 8, 14));
        aleks.setDataVersion(2);

        colPerson.add(ivan);
        colPerson.add(aleks);
        colPerson.add(goran);
        colPerson.add(null);
        colPerson2.add(ivan);
        colPerson2.add(null);
        colPerson2.add(goran);

        m_writer.writeCollection(0, col1);
        m_writer.writeCollection(0, colPerson);
        m_writer.writeCollection(0, colPerson);
        m_writer.writeCollection(0, colPerson2, PortablePerson.class);

        initPOFReader();
        m_reader = new PofBufferReader(m_bi, ctx);

        Collection<PortableObject> colResult1 = new ArrayList<PortableObject>();
        m_reader.readCollection(0, colResult1);

        assertEquals(3, colResult1.size());

        for (int i = 0; i < colResult1.size(); i++)
            {
            assertEquals(col1.toArray()[i], colResult1.toArray()[i]);
            }

        Collection<PortableObject> colResult2 = new ArrayList<PortableObject>();
        m_reader.readCollection(0, colResult2);

        assertEquals(4, colResult2.size());

        ArrayList<PortableObject> listResult2 = (ArrayList<PortableObject>) colResult2;
        assertFalse(listResult2.get(0) instanceof EvolvablePortablePerson);
        assertTrue(listResult2.get(1) instanceof EvolvablePortablePerson);
        assertFalse(listResult2.get(2) instanceof EvolvablePortablePerson);
        assertEquals(listResult2.get(3), null);

        EvolvablePortablePerson epp = (EvolvablePortablePerson) listResult2.get(1);
        assertEquals(aleks.m_sName, epp.m_sName);
        assertEquals(aleks.getChildren()[0].m_sName, epp.getChildren()[0].m_sName);

        PortablePerson pp = (PortablePerson) listResult2.get(0);
        assertEquals(ivan.m_sName, pp.m_sName);
        assertNull(pp.getChildren());

        List<PortableObject> listResult3 = (List<PortableObject>) m_reader.readCollection(0, (Collection) null);
        assertEquals(4, listResult3.size());
        assertFalse(listResult3.get(0) instanceof EvolvablePortablePerson);
        assertTrue(listResult3.get(1) instanceof EvolvablePortablePerson);
        assertFalse(listResult3.get(2) instanceof EvolvablePortablePerson);
        assertEquals(listResult3.get(3), null);

        epp = (EvolvablePortablePerson) listResult3.get(1);
        assertEquals(aleks.m_sName, epp.m_sName);
        assertEquals(aleks.getChildren()[0].m_sName, epp.getChildren()[0].m_sName);

        pp = (PortablePerson) listResult3.get(0);
        assertEquals(ivan.m_sName, pp.m_sName);
        assertNull(pp.getChildren());

        List<PortablePerson> listResult4 = (List<PortablePerson>) m_reader.readCollection(0, (Collection) null);
        assertTrue(listResult4.containsAll(colPerson2));
        }

    @Test
    public void testWriteGenericMap()
            throws IOException
        {
        SimplePofContext ctx = new SimplePofContext();
        ctx.registerUserType(101, PortablePersonLite.class, new PortableObjectSerializer(101));
        ctx.registerUserType(102, PortablePerson.class, new PortableObjectSerializer(102));
        ctx.registerUserType(103, EvolvablePortablePerson.class, new PortableObjectSerializer(103));

        initPOFWriter();
        m_writer = new PofBufferWriter(m_bo, ctx);

        Map<String, Double> map = new HashMap<String, Double>();
        map.put("A", 11.11);
        map.put("Z", 88.88);
        map.put("7", 100.1);

        Map<String, PortableObject> mapPerson = new HashMap<String, PortableObject>();

        PortablePerson ivan = new PortablePerson("Ivan", new java.util.Date(78, 4, 25));
        ivan.setChildren(null);

        PortablePerson goran = new PortablePerson("Goran", new java.util.Date(82, 3, 3));
        goran.setChildren(null);

        EvolvablePortablePerson aleks = new EvolvablePortablePerson("Aleks", new java.util.Date(74, 8, 24));
        aleks.setChildren(new EvolvablePortablePerson[1]);
        aleks.getChildren()[0] = new EvolvablePortablePerson("Ana Maria", new java.util.Date(104, 8, 14));
        aleks.setDataVersion(2);

        mapPerson.put("key1", ivan);
        mapPerson.put("key2", aleks);
        mapPerson.put("key3", goran);

        m_writer.writeMap(0, map);
        m_writer.writeMap(0, mapPerson);
        m_writer.writeMap(0, mapPerson);

        initPOFReader();
        m_reader = new PofBufferReader(m_bi, ctx);

        Map<String, PortableObject> mapResult = new HashMap<String, PortableObject>();
        m_reader.readMap(0, mapResult);

        assertEquals(3, mapResult.size());
        assertEquals(map.get("key1"), mapResult.get("key1"));
        assertEquals(map.get("key2"), mapResult.get("key2"));
        assertEquals(map.get("key3"), mapResult.get("key3"));

        Map<String, PortableObject> mapResult2 = new HashMap<String, PortableObject>();
        m_reader.readMap(0, mapResult2);
        assertEquals(3, mapResult2.size());
        assertFalse(mapResult2.get("key1") instanceof EvolvablePortablePerson);
        assertTrue(mapResult2.get("key2") instanceof EvolvablePortablePerson);
        assertFalse(mapResult2.get("key3") instanceof EvolvablePortablePerson);

        EvolvablePortablePerson epp = (EvolvablePortablePerson) mapResult2.get("key2");
        assertEquals(aleks.m_sName, epp.m_sName);
        assertEquals(aleks.getChildren()[0].m_sName, epp.getChildren()[0].m_sName);

        PortablePerson pp = (PortablePerson) mapResult2.get("key3");
        assertEquals(goran.m_sName, pp.m_sName);
        assertNull(pp.getChildren());

        Map<String, PortableObject> mapResult3 = (Map<String, PortableObject>) m_reader.readMap(0, (Map) null);

        assertEquals(3, mapResult3.size());
        assertFalse(mapResult3.get("key1") instanceof EvolvablePortablePerson);
        assertTrue(mapResult3.get("key2") instanceof EvolvablePortablePerson);
        assertFalse(mapResult3.get("key3") instanceof EvolvablePortablePerson);

        epp = (EvolvablePortablePerson) mapResult3.get("key2");
        assertEquals(aleks.m_sName, epp.m_sName);
        assertEquals(aleks.getChildren()[0].m_sName, epp.getChildren()[0].m_sName);

        pp = (PortablePerson) mapResult3.get("key3");
        assertEquals(goran.m_sName, pp.m_sName);
        assertNull(pp.getChildren());
        }

    @Test
    public void testWriteReminder()
            throws IOException
        {
        initPOFWriter();
        initPOFReader();
        try
            {
            m_writer.writeRemainder(new Binary(new byte[]{1, 2, 3}));
            fail("expected exception");
            }
        catch (IllegalStateException e)
            {
            // expected
            }
        }
    }
