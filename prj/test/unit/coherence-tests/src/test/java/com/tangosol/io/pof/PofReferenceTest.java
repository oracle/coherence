/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.WriteBuffer;

import com.tangosol.util.Binary;
import com.tangosol.util.CompositeKey;
import com.tangosol.util.ExternalizableHelper;

import data.pof.Address;
import data.pof.EvolvablePortablePerson;
import data.pof.EvolvablePortablePerson2;
import data.pof.PortablePerson;
import data.pof.PortablePersonReference;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
* Unit tests of POF serialization/deserialization of Itentity/Reference types
* and object graph.
*
* @author lh/jh 2011.03.21
*/
public class PofReferenceTest
        extends AbstractPofTest
    {
    @Test
    public void testEnableReferenceConfig() throws Exception
        {
        String sPath = "com/tangosol/io/pof/reference-pof-config.xml";
        ConfigurablePofContext ctx = new ConfigurablePofContext(sPath);
        ctx.ensureInitialized();
        assertTrue(ctx.isReferenceEnabled());
        }

    @Test
    public void testDuplicateObjectReferences()
            throws IOException
        {
        // loop twice, one for SimplePofContext, one for ConfigurablePofContext.
        for (int loop = 0; loop < 2; loop++)
            {
            PofContext ctx;
            if (loop == 0)
                {
                ctx = new SimplePofContext();
                ((SimplePofContext)ctx).registerUserType(101, PortablePerson.class, new PortableObjectSerializer(101));
                ((SimplePofContext)ctx).registerUserType(201, CompositeKey.class, new PortableObjectSerializer(201));
                ((SimplePofContext)ctx).setReferenceEnabled(true);
                }
            else
                {
                String sPath = "com/tangosol/io/pof/reference-pof-config.xml";
                ctx = new ConfigurablePofContext(sPath);
                }

            PortablePerson joe          = new PortablePerson("Joe Smith", new Date(78, 4, 25));
            PortablePerson differentJoe = new PortablePerson("Joe Smith", new Date(78, 4, 25));
            CompositeKey   key          = new CompositeKey(joe, joe);

            azzert(key.getPrimaryKey() == key.getSecondaryKey());

            Binary       bin  = ExternalizableHelper.toBinary(key, ctx);
            CompositeKey keyR = (CompositeKey) ExternalizableHelper.fromBinary(bin, ctx);

            azzert(keyR.getPrimaryKey() == keyR.getSecondaryKey());

            // test a collection of duplicate object references
            List list = new ArrayList(4);
            list.add(joe);
            list.add(joe);
            list.add(differentJoe);
            list.add(differentJoe);

            bin = ExternalizableHelper.toBinary(list, ctx);
            Collection col = (Collection) ExternalizableHelper.fromBinary(bin, ctx);

            azzert(col.size() == list.size());

            PortablePerson person = null;
            int            i      = 0;
            for (Iterator iter = col.iterator(); iter.hasNext(); )
                {
                PortablePerson personNext = (PortablePerson) iter.next();
                if (person == null)
                    {
                    person = personNext;
                    i++;
                    }
                else
                    {
                    azzert(person.equals(personNext));
                    if (i == 1 || i == 3)
                        {
                        azzert(person == personNext);
                        }
                    else
                        {
                        person = personNext;
                        }
                    i++;
                    }
                }
            }
        }

    @Test
    public void testReferencesInUniformArray()
            throws IOException
        {
        SimplePofContext ctx = new SimplePofContext();
        ctx.registerUserType(101, PortablePerson.class, new PortableObjectSerializer(101));
        ctx.registerUserType(102, PortablePersonReference.class, new PortableObjectSerializer(102));
        ctx.registerUserType(201, com.tangosol.util.CompositeKey.class, new PortableObjectSerializer(201));
        ctx.setReferenceEnabled(true);

        PortablePersonReference ivan  = new PortablePersonReference("Ivan", new Date(78, 4, 25));
        PortablePersonReference goran = new PortablePersonReference("Goran", new Date(82, 3, 3));
        ivan.setChildren(null);
        goran.setChildren(new PortablePerson[2]);
        goran.getChildren()[0] = new PortablePerson("Tom", new Date(103, 7, 5));
        goran.getChildren()[1] = new PortablePerson("Ellen", new Date(105, 3, 15));
        ivan.setSiblings(new PortablePersonReference[1]);
        ivan.getSiblings()[0] = goran;
        goran.setSiblings(new PortablePersonReference[1]);
        goran.getSiblings()[0] = ivan;

        Map<CompositeKey, PortableObject> mapPerson = new HashMap<CompositeKey, PortableObject>();
        String                            lastName  = "Smith";
        CompositeKey                      key1      = new CompositeKey(lastName, "ivan"),
                                          key2      = new CompositeKey(lastName, "goran");
        mapPerson.put(key1, ivan);
        mapPerson.put(key2, goran);

        initPOFWriter();
        m_writer = new PofBufferWriter(m_bo, ctx);
        if (ctx.isReferenceEnabled())
            {
            m_writer.enableReference();
            }
        m_writer.writeMap(0, mapPerson);

        initPOFReader();
        m_reader = new PofBufferReader(m_bi, ctx);

        Map<CompositeKey, PortableObject> mapResult = (Map<CompositeKey, PortableObject>) m_reader.readMap(0, (Map) null);
        assertEquals(2, mapResult.size());

        PortablePersonReference ivanR  = (PortablePersonReference) mapResult.get(key1);
        PortablePersonReference goranR = (PortablePersonReference) mapResult.get(key2);

        Collection   keySet = mapResult.keySet();
        Iterator     iter   = keySet.iterator();
        CompositeKey key1R  = (CompositeKey)iter.next();
        CompositeKey key2R  = (CompositeKey)iter.next();

        assertFalse(key1R.getPrimaryKey() == key2R.getPrimaryKey());
        azzert(ivanR.getSiblings()[0] == goranR);
        assertEquals(goran.m_sName, goranR.m_sName);
        assertNull(ivanR.getChildren());
        }

    @Test
    public void testReferencesInUniformMap()
            throws IOException
        {
        SimplePofContext ctx = new SimplePofContext();
        ctx.registerUserType(101, PortablePerson.class, new PortableObjectSerializer(101));
        ctx.registerUserType(102, PortablePersonReference.class, new PortableObjectSerializer(102));
        ctx.registerUserType(201, com.tangosol.util.CompositeKey.class, new PortableObjectSerializer(201));

        Map<CompositeKey, PortableObject> mapPerson = new HashMap<CompositeKey, PortableObject>();
        String lastName                             = "Smith";
        CompositeKey key1                           = new CompositeKey(lastName, "ivan"),
                     key2                           = new CompositeKey(lastName, "goran");
        PortablePersonReference ivan                = new PortablePersonReference("Ivan", new Date(78, 4, 25));
        ivan.setChildren(null);
        mapPerson.put(key1, ivan);
        mapPerson.put(key2, ivan);

        initPOFWriter();
        m_writer = new PofBufferWriter(m_bo, ctx);
        m_writer.enableReference();
        m_writer.writeMap(0, mapPerson, CompositeKey.class, PortablePersonReference.class);

        initPOFReader();
        m_reader = new PofBufferReader(m_bi, ctx);
        Map        mapPersonR = m_reader.readMap(0, (Map) null);
        Collection colVal     = mapPerson.values();
        Collection colValR    = mapPersonR.values();
        Collection colKey     = mapPerson.keySet();
        Collection colKeyR    = mapPersonR.keySet();

        // compare mapPerson with result
        assertTrue(colVal.containsAll(colValR));
        assertTrue(colKey.containsAll(colKeyR));
        assertEquals(colVal.size(), colValR.size());
        assertEquals(colKey.size(), colKeyR.size());
        azzert(mapPersonR.get(key1) == mapPersonR.get(key2));
        }

    @Test
    public void testReferencesInArray()
            throws IOException

        {
        SimplePofContext ctx = new SimplePofContext();
        ctx.registerUserType(101, PortablePerson.class, new PortableObjectSerializer(101));
        ctx.registerUserType(102, PortablePersonReference.class, new PortableObjectSerializer(102));

        PortablePersonReference ivan       = new PortablePersonReference("Ivan", new Date(78, 4, 25));
        PortablePersonReference goran      = new PortablePersonReference("Goran", new Date(82, 3, 3));
        PortablePersonReference jack       = new PortablePersonReference("Jack", new Date(80, 5, 25));
        PortablePersonReference jim        = new PortablePersonReference("Jim", new Date(80, 5, 25));
        PortablePersonReference[] siblings = new PortablePersonReference[2];
        siblings[0] = jack;
        siblings[1] = jim;

        ivan.setChildren(null);
        jack.setChildren(null);
        jim.setChildren(null);
        goran.setChildren(new PortablePerson[2]);
        goran.getChildren()[0] = new PortablePerson("Tom", new Date(103, 7, 5));
        goran.getChildren()[1] = new PortablePerson("Ellen", new Date(105, 3, 15));
        ivan.setSiblings(siblings);
        goran.setSiblings(siblings);
        azzert(ivan.getSiblings() == goran.getSiblings());

        Collection<PortableObject> col1 = new ArrayList<PortableObject>();
        col1.add(ivan);
        col1.add(goran);

        initPOFWriter();
        m_writer = new PofBufferWriter(m_bo, ctx);
        m_writer.enableReference();
        m_writer.writeCollection(0, col1);
        m_writer.writeCollection(0, col1, PortablePersonReference.class);

        initPOFReader();
        m_reader = new PofBufferReader(m_bi, ctx);

        Collection<PortableObject> result  = (Collection<PortableObject>) m_reader.readCollection(0, (Collection) null);
        Collection<PortableObject> result2 = (Collection<PortableObject>) m_reader.readCollection(0, (Collection) null);
        assertEquals(2, result.size());

        PortablePersonReference ivanR  = (PortablePersonReference) result.toArray()[0];
        PortablePersonReference goranR = (PortablePersonReference) result.toArray()[1];

        assertFalse(ivanR.getSiblings() == goranR.getSiblings());
        azzert(ivanR.getSiblings()[0] == goranR.getSiblings()[0]);
        azzert(ivanR.getSiblings()[1] == goranR.getSiblings()[1]);
        assertNull(ivanR.getChildren());

        PortablePersonReference ivanR2  = (PortablePersonReference) result2.toArray()[0];
        PortablePersonReference goranR2 = (PortablePersonReference) result2.toArray()[1];

        assertFalse(ivanR2.getSiblings() == goranR2.getSiblings());
        azzert(ivanR2.getSiblings()[0] == goranR2.getSiblings()[0]);
        azzert(ivanR2.getSiblings()[1] == goranR2.getSiblings()[1]);
        assertNull(ivanR2.getChildren());
        }

    @Test
    public void testEvolvableObjectSerialization()
            throws IOException
        {
        SimplePofContext ctxV1 = new SimplePofContext();
        ctxV1.registerUserType(1, EvolvablePortablePerson.class,
                new PortableObjectSerializer(1));
        ctxV1.registerUserType(2, Address.class,
                new PortableObjectSerializer(2));
        ctxV1.setReferenceEnabled(true);

        SimplePofContext ctxV2 = new SimplePofContext();
        ctxV2.registerUserType(1, EvolvablePortablePerson2.class,
                new PortableObjectSerializer(1));
        ctxV2.registerUserType(2, Address.class,
                new PortableObjectSerializer(2));
        ctxV2.setReferenceEnabled(true);

        EvolvablePortablePerson2 person12 = new EvolvablePortablePerson2(
                "Aleksandar Seovic", new Date(74, 7, 24));
        EvolvablePortablePerson2 person22 = new EvolvablePortablePerson2(
                "Ana Maria Seovic", new Date(104, 7, 14, 7, 43, 0));
        EvolvablePortablePerson2 person32 = new EvolvablePortablePerson2(
                "Art Seovic", new Date(107, 8, 12, 5, 20, 0));

        Address addr    = new Address("208 Myrtle Ridge Rd", "Lutz", "FL", "33549");
        Address addrPOB = new Address("128 Asbury Ave, #401", "Evanston", "IL", "60202");
        person12.setAddress(addr);
        person22.setAddress(addr);

        person12.m_sNationality = person22.m_sNationality = "Serbian";
        person12.m_addrPOB      = new Address(null, "Belgrade", "Serbia", "11000");
        person22.m_addrPOB      = addrPOB;
        person32.m_addrPOB      = addrPOB;
        person12.setChildren(new EvolvablePortablePerson2[]{person22, person32});

        byte[] abV2 = new byte[2048];
        ctxV2.serialize(new ByteArrayWriteBuffer(abV2).getBufferOutput(), person12);

        EvolvablePortablePerson person11 = (EvolvablePortablePerson) ctxV1
                .deserialize(new ByteArrayReadBuffer(abV2).getBufferInput());
        EvolvablePortablePerson person21 = new EvolvablePortablePerson(
                "Marija Seovic", new Date(78, 1, 20));
        person21.setAddress(person11.getAddress());
        person21.setChildren(person11.getChildren());
        person11.setSpouse(person21);

        byte[] abV1 = new byte[2048];
        ctxV1.serialize(new ByteArrayWriteBuffer(abV1).getBufferOutput(), person11);

        EvolvablePortablePerson2 person = (EvolvablePortablePerson2) ctxV2
                .deserialize(new ByteArrayReadBuffer(abV1).getBufferInput());

        assertEquals(person12.m_sName, person.m_sName);
        assertEquals(person12.m_sNationality, person.m_sNationality);
        assertEquals(person12.m_dtDOB, person.m_dtDOB);
        assertEquals(person11.getSpouse().m_sName, person.getSpouse().m_sName);
        assertEquals(person12.getAddress(), person.getAddress());
        assertEquals(person12.m_addrPOB, person.m_addrPOB);

        //reference is not supported for EvolvablePortableObject
        azzert(person.getAddress() != (person.getChildren()[0]).getAddress());
        azzert(person.getAddress() != person.getSpouse().getAddress());
        azzert(person.getChildren()[0] != person.getSpouse().getChildren()[0]);
        azzert(person.getChildren()[1] != person.getSpouse().getChildren()[1]);

        // using PofBufferWriter and PofBufferReader directly
        initPOFWriter();
        m_writer = new PofBufferWriter(m_bo, ctxV2);
        m_writer.enableReference();
        m_writer.writeObject(0, person12);

        initPOFReader();
        m_reader = new PofBufferReader(m_bi, ctxV1);
        person11 = (EvolvablePortablePerson) m_reader.readObject(0);
        person11.setSpouse(person21);

        initPOFWriter();
        m_writer = new PofBufferWriter(m_bo, ctxV1);
        m_writer.enableReference();
        m_writer.writeObject(0, person11);

        initPOFReader();
        m_reader = new PofBufferReader(m_bi, ctxV2);
        person11 = (EvolvablePortablePerson2) m_reader.readObject(0);

        assertEquals(person12.m_sName, person.m_sName);
        assertEquals(person12.m_sNationality, person.m_sNationality);
        assertEquals(person12.m_dtDOB, person.m_dtDOB);
        assertEquals(person11.getSpouse().m_sName, person.getSpouse().m_sName);
        assertEquals(person12.getAddress(), person.getAddress());
        assertEquals(person12.m_addrPOB, person.m_addrPOB);
        azzert(person.getAddress() != (person.getChildren()[0]).getAddress());
        azzert(person.getAddress() != person.getSpouse().getAddress());
        azzert(person.getChildren()[0] != person.getSpouse().getChildren()[0]);
        azzert(person.getChildren()[1] != person.getSpouse().getChildren()[1]);
        }

    @Test
    public void testCircularReferences()
            throws IOException
        {
        SimplePofContext ctx = new SimplePofContext();
        ctx.registerUserType(101, PortablePerson.class, new PortableObjectSerializer(101));
        ctx.registerUserType(102, PortablePersonReference.class, new PortableObjectSerializer(102));

        PortablePersonReference ivan = new PortablePersonReference("Ivan", new java.util.Date(78, 4, 25));
        ivan.setChildren(new PortablePerson[1]);
        ivan.getChildren()[0] = new PortablePerson("Mary Jane", new java.util.Date(97, 8, 14));
        ivan.setSpouse(new PortablePerson("Eda", new java.util.Date(79, 6, 25)));

        PortablePersonReference goran = new PortablePersonReference("Goran", new java.util.Date(82, 3, 3));
        goran.setChildren(new PortablePerson[2]);
        goran.getChildren()[0] = new PortablePerson("Tom", new java.util.Date(103, 7, 5));
        goran.getChildren()[1] = new PortablePerson("Ellen", new java.util.Date(105, 3, 15));
        goran.setSpouse(new PortablePerson("Tiffany", new java.util.Date(82, 3, 25)));
        goran.setFriend(ivan);
        ivan.setFriend(goran);

        initPOFWriter();
        m_writer = new PofBufferWriter(m_bo, ctx);
        m_writer.enableReference();
        m_writer.writeObject(0, ivan);

        initPOFReader();
        m_reader = new PofBufferReader(m_bi, ctx);

        PortablePersonReference ivanR = (PortablePersonReference) m_reader.readObject(0);
        assertEquals(ivanR.m_sName, ivan.m_sName);
        assertEquals(ivanR.getChildren().length, 1);
        assertTrue(ivanR.getFriend().equals(goran));
        azzert(ivanR == ivanR.getFriend().getFriend());
        }

    @Test
    public void testNestedReferences()
            throws Exception
        {
        String                 sPath = "com/tangosol/io/pof/reference-pof-config.xml";
        ConfigurablePofContext ctx   = new ConfigurablePofContext(sPath);
        ctx.setContextClassLoader(ConfigurablePofContextTest.PofMaster.class.getClassLoader());

        ConfigurablePofContextTest.PofMaster pm = new ConfigurablePofContextTest.PofMaster();
        ConfigurablePofContextTest.PofChild pc1 = new ConfigurablePofContextTest.PofChild();
        ConfigurablePofContextTest.PofChild pc2 = new ConfigurablePofContextTest.PofChild();
        List list1 = null;
        List list2 = new ArrayList();
        List list3 = new ArrayList();
        Map  map1  = null;
        Map  map2  = new HashMap();
        Map  map3  = map2;

        list3.add(0);
        map3.put("key1", pc1);
        map3.put("key2", pc2);
        pc1.setId("child1");
        pc2.setId("child2");

        pm.setList1(list1);
        pm.setList2(list2);
        pm.setList3(list3);
        pm.setMap1(map1);
        pm.setMap2(map2);
        pm.setMap3(map3);
        pm.setNumber(9999);
        pm.setText("cross fingers");
        pm.setChildren(new ConfigurablePofContextTest.PofChild[] {pc1, pc2, pc2});

        WriteBuffer buf = new ByteArrayWriteBuffer(20);
        ctx.serialize(buf.getBufferOutput(), pm);

        ConfigurablePofContextTest.PofMaster pm2 = (ConfigurablePofContextTest.PofMaster) ctx.deserialize(buf.toBinary().getBufferInput());

        assertEquals(pm, pm2);
        Map map2R = pm2.getMap2();
        Map map3R = pm2.getMap3();
        azzert(map2R != map3R);
        azzert(map2R.get("key1") == map3R.get("key1"));
        azzert(map2R.get("key2") == map3R.get("key2"));

        ConfigurablePofContextTest.PofChild children[] = pm2.getChildren();
        azzert(children[0] == map2R.get("key1"));
        azzert(children[1] == children[2]);
        }

    /**
    * Test Nested Type.
    */
    @Test
    public void testNestedType()
            throws IOException
        {
        SimplePofContext ctx = new SimplePofContext();
        ctx.registerUserType(101, NestedTypeWithReference.class, new PortableObjectSerializer(101));
        ctx.registerUserType(102, PortablePerson.class, new PortableObjectSerializer(102));

        NestedTypeWithReference tv = new NestedTypeWithReference();

        initPOFWriter();
        m_writer = new PofBufferWriter(m_bo, ctx);
        m_writer.enableReference();
        m_writer.writeObject(0, tv);

        initPOFReader();
        m_reader = new PofBufferReader(m_bi, ctx);
        NestedTypeWithReference result = (NestedTypeWithReference) m_reader.readObject(0);
        }

    public static class NestedTypeWithReference implements PortableObject
        {
        private static final int         INTEGER      = 100;
        private static final String      STRING       = "Hello World";
        private static final String[]    STRING_ARRAY = new String[] { "one",
                "two", "three" };
        private static final float[]     FLOAT_ARRAY  = new float[] { 1.0f,
                2.0f, 3.3f, 4.4f };
        private static final PortablePerson PERSON    = new PortablePerson(
                "Joe Smith", new Date(78, 4, 25));
        private static final PortablePerson CHILD1    = new PortablePerson(
                "Tom", new Date(103, 7, 5));
        private static final PortablePerson CHILD2    = new PortablePerson(
                "Ellen", new Date(105, 3, 15));

        static
            {
            PERSON.setChildren(new PortablePerson[2]);
            PERSON.getChildren()[0] = CHILD1;
            PERSON.getChildren()[1] = CHILD2;
            }

        private static final List<String> list;

        static
            {
            list = new ArrayList<String>();
            list.add("four");
            list.add("five");
            list.add("six");
            list.add("seven");
            list.add("eight");
            }

        public void readExternal(PofReader reader) throws IOException
            {
            Assert.assertEquals(INTEGER, reader.readInt(0));
            PofReader nested1 = reader.createNestedPofReader(1);

            PofReader nested2 = nested1.createNestedPofReader(0);
            Assert.assertEquals(STRING, nested2.readString(0));
            PortablePerson person2 = (PortablePerson)nested2.readObject(1);
            float[] floatArray = nested2.readFloatArray(2);
            Assert.assertEquals(Arrays.equals(FLOAT_ARRAY, floatArray), true);

            PofReader nested3 = nested2.createNestedPofReader(3);
            String[] stringArray = (String[]) nested3.readObjectArray(0,
                    new String[0]);
            Assert.assertTrue(Arrays.equals(stringArray, STRING_ARRAY));
            nested3.readRemainder();

            // close nested3 and continue to nested2
            boolean bool = nested2.readBoolean(4);
            Assert.assertEquals(false, bool);

            // nested1
            Collection<String> col = nested1.readCollection(1, (Collection) null);
            for (String string : list)
                {
                Assert.assertTrue(col.contains(string));
                }

            Assert.assertEquals(2.0, nested1.readDouble(2));
            Assert.assertEquals(5, nested1.readInt(3));

            col = nested1.readCollection(4, new ArrayList<String>());
            for (String string : list)
                {
                Assert.assertTrue(col.contains(string));
                }

            PortablePerson person1 = (PortablePerson)nested1.readObject(5);
            Assert.assertEquals(2.222, nested1.readDouble(10));

            nested1.readRemainder();

            Assert.assertEquals(4.444, reader.readDouble(2));
            Assert.assertEquals(15, reader.readInt(3));
            PortablePerson person = (PortablePerson)reader.readObject(4);
            azzert(person == person1);
            azzert(person1 == person2);
            }

        public void writeExternal(PofWriter writer) throws IOException
            {
            writer.writeInt(0, INTEGER);

            PofWriter nested1 = writer.createNestedPofWriter(1);

            PofWriter nested2 = nested1.createNestedPofWriter(0);
            nested2.writeString(0, STRING);
            nested2.writeObject(1, PERSON);
            nested2.writeFloatArray(2, FLOAT_ARRAY);

            PofWriter nested3 = nested2.createNestedPofWriter(3);
            nested3.writeObjectArray(0, STRING_ARRAY, String.class);

            nested2.writeBoolean(4, false);
            nested2.writeRemainder(null);

            nested1.writeCollection(1, list);
            nested1.writeDouble(2, 2.0);
            nested1.writeInt(3, 5);
            nested1.writeCollection(4, list, String.class);
            nested1.writeObject(5,PERSON);
            nested1.writeDouble(10, 2.222);

            writer.writeDouble(2, 4.444);
            writer.writeInt(3, 15);
            writer.writeObject(4, PERSON);
            }
        }
    }
