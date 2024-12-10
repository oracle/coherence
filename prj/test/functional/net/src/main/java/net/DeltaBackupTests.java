/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package net;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.coherence.component.util.safeService.safeCacheService.SafeDistributedCacheService;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.util.Base;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


public class DeltaBackupTests
        extends AbstractFunctionalTest
    {

    public DeltaBackupTests()
        {
        super();
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.distributed.compressor", "standard");

        AbstractFunctionalTest._startup();
        }


    // ----- test methods ---------------------------------------------------

    @Test
    public void testSession()
        {
        Properties properties = new Properties();
        properties.setProperty("coherence.distributed.compressor", "standard");
        properties.setProperty("coherence.distributed.localstorage", "true");
        NamedCache namedCache = getNamedCache(getCacheName());
        startCacheServer("initial", "net", null, properties);

        verifyCompression(namedCache);

        m_map = new TestMap(namedCache);
        final int initialSize = getInitialSize();
        final int minOperations = getMinOperations();
        final int maxOperations = getMaxOperations();
        final int keys = getKeyRange();
        final int sessions = getSessions();



        for (int i = 0; i < sessions; i++)
            {
            System.out.println("Running session " + (i + 1) + "/" + sessions);
            startCacheServer("secondary", "net", null, properties);
            prepare();
            Map<Integer, RandomType> map = initMap(initialSize);
            generateOperations(minOperations, maxOperations, keys, map);
            processOperations();
            assertEquals(m_map.size(), namedCache.size());

            stopCacheServer("secondary");
            validateTestMap();
            }
        stopCacheServer("intial");
        }

    /**
     * Generate a sequence of random operations
     *
     * @param minOps the minimum number of operations to generate
     * @param maxOps the maximum number of operations to generate
     * @param maxKey the maximum key to generate an operation for
     * @param map the initial map to use
     */
    private void generateOperations(int minOps, int maxOps, int maxKey, Map<Integer, RandomType> map)
        {
        Random random = Base.getRandom();
        int ops = random.nextInt(maxOps) + minOps;

        for (int i = 0; i < ops; i++)
            {
            Operation op = randomOperation();
            int key = random.nextInt(maxKey);

            switch (op)
                {
                case PUT:
                    session.add(new MapOperation<Integer, RandomType>(op, key, mutate(key, map)));
                    break;

                case GET:
                    session.add(new MapOperation<Integer, RandomType>(op, key));
                    break;

                case REMOVE:
                    session.add(new MapOperation<Integer, RandomType>(op, key));
                    break;
                }
            }
        }

    private String getCacheName()
        {
        return System.getProperty(CACHE_NAME_PROPERTY, CACHE_NAME);
        }

    private int getInitialSize()
        {
        return Integer.parseInt(System.getProperty(INITIAL_SIZE_PROPERTY, "1024"));
        }

    private int getKeyRange()
        {
        return Integer.parseInt(System.getProperty(KEY_RANGE_PROPERTY, "32767"));
        }

    private int getMaxOperations()
        {
        return Integer.parseInt(System.getProperty(MAX_OPERATIONS_PROPERTY, "65535"));
        }

    private int getMinOperations()
        {
        return Integer.parseInt(System.getProperty(MIN_OPERATIONS_PROPERTY, "2048"));
        }

    private int getSessions()
        {
        return Integer.parseInt(System.getProperty(SESSIONS_PROPERTY, "10"));
        }

    private Map<Integer, RandomType> getTestMap()
        {
        return m_map;
        }

    /**
     * Create the initial map, filled with RandomType objects.
     *
     * @param entries the number of entries that the initial map should contain
     *
     * @return return the created map
     */
    private Map<Integer, RandomType> initMap(int entries)
        {
        Map<Integer, RandomType> map = new HashMap<Integer, RandomType>(entries);

        for (int i = 0; i < entries; i++)
            {
            map.put(i, new RandomType(i));
            }

        session.add(new MapOperation<Integer, RandomType>(Operation.PUTALL, map));
        return map;
        }

    /**
     * Mutate or create an entry that is stored in the <code>map</code>.
     *
     * @param key the entry to mutate or create.
     * @param map the map containing all the entries
     *
     * @return the new value
     */
    private RandomType mutate(int key, Map<Integer, RandomType> map)
        {
        RandomType randomType = map.get(key);
        return randomType == null ? new RandomType(key) : randomType.mutate();
        }

    /**
     * prepare the session
     */
    private void prepare()
        {
        m_map.clear();
        session.clear();
        }

    /**
     * Process each operation in the session against the <code>Test Map</code>.
     */
    private void processOperations()
        {
        System.out.println("Processing session with " + session.size() + " item(s).");
        for (MapOperation<Integer, RandomType> op : session)
            {
            op.run(getTestMap());
            }
        }

    /**
     * Generate a random mutating operation i.e. PUT and REMOVE.
     *
     * @return return a fairly random  mutating operation
     */
    private Operation randomOperation()
        {
        Random random = Base.getRandom();
        Operation op = Operation.values()[random.nextInt(2)];
        return op;
        }

    /**
     * Validate the inner map.
     */
    private void validateTestMap()
        {
        TestMap testMap = (TestMap) getTestMap();
        testMap.validate();
        }

    private PartitionedCache verifyCompression(NamedCache cache)
        {
        assertTrue(cache.getCacheService() instanceof SafeDistributedCacheService);

        SafeDistributedCacheService safeService = (SafeDistributedCacheService) cache.getCacheService();
        assertTrue(safeService.getService() instanceof DistributedCacheService);

        DistributedCacheService service = (DistributedCacheService) safeService.getService();
        assertTrue(service instanceof PartitionedCache);

        assertEquals(com.tangosol.io.DecoratedBinaryDeltaCompressor.class,
                ((PartitionedCache) service).getBackupDeltaCompressor().getClass());

        return (PartitionedCache) service;
        }

    public static void main(String[] args)
        {
        new DeltaBackupTests();
        }

    /**
     * A MapOperation
     */
    public static class MapOperation<K, V>
        {
        public MapOperation(Operation op, K key)
            {
            this.operation = op;
            assert (operation.equals(Operation.GET) || operation.equals(Operation.REMOVE));
            this.key = key;
            }

        public MapOperation(Operation op, K key, V value)
            {
            this.operation = op;
            assert (operation.equals(Operation.PUT));
            this.key = key;
            this.value = value;
            }

        public MapOperation(Operation op, Map<K, V> map)
            {
            this.operation = op;
            assert (op.equals(Operation.PUTALL));
            this.map = map;
            }

        public void run(Map<K, V> map)
            {
            switch (operation)
                {
                case PUT:
                    map.put(key, value);
                    break;

                case REMOVE:
                    map.remove(key);
                    break;

                case GET:
                    map.get(key);
                    break;

                case PUTALL:
                    map.putAll(this.map);
                    break;
                }
            }

        private Operation operation;

        private K key;

        private V value;

        /**
         * If the operation is a PUTALL keep the data in this field.
         */
        private Map<K, V> map;
        }

    public enum Operation
        {
        PUT, REMOVE, PUTALL, GET, GETALL
        }

    public static class RandomType
            implements Externalizable
        {
        public RandomType()
            {}

        public RandomType(int id)
            {
            this.id = id;
            randomizeFields();
            }

        /**
         * @{inheritDoc
         */
        @Override
        public boolean equals(Object obj)
            {
            if (this == obj)
                {
                return true;
                }
            if (obj == null)
                {
                return false;
                }
            if (!(obj instanceof RandomType))
                {
                return false;
                }
            RandomType other = (RandomType) obj;
            if (a == null)
                {
                if (other.a != null)
                    {
                    return false;
                    }
                }
            else if (!a.equals(other.a))
                {
                return false;
                }
            if (b == null)
                {
                if (other.b != null)
                    {
                    return false;
                    }
                }
            else if (!b.equals(other.b))
                {
                return false;
                }
            if (c != other.c)
                {
                return false;
                }
            if (d != other.d)
                {
                return false;
                }
            if (Double.doubleToLongBits(e) != Double.doubleToLongBits(other.e))
                {
                return false;
                }
            if (Float.floatToIntBits(f) != Float.floatToIntBits(other.f))
                {
                return false;
                }

            if (!Arrays.equals(g, other.g) || !Arrays.equals(h, other.h))
                {
                return false;
                }

            return true;
            }

        /**
         * @{inheritDoc
         */
        @Override
        public int hashCode()
            {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((a == null) ? 0 : a.hashCode());
            result = prime * result + ((b == null) ? 0 : b.hashCode());
            result = prime * result + (int) (c ^ (c >>> 32));
            result = prime * result + d;
            long temp;
            temp = Double.doubleToLongBits(e);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            result = prime * result + Float.floatToIntBits(f);
            /*
             * result = prime * result + Arrays.hashCode(g); result = prime *
             * result + Arrays.hashCode(h);
             */
            return result;
            }

        /**
         * Potentially mutate one ore more fields. If no fields are mutate
         * return the same instance.
         */
        public RandomType mutate()
            {
            return willMutate() ? mutateFields() : this;
            }

        @Override
        public void readExternal(ObjectInput in)
                throws IOException, ClassNotFoundException
            {
            id = in.readInt();
            a = in.readUTF();
            b = in.readUTF();
            c = in.readLong();
            d = in.readByte();
            e = in.readDouble();
            f = in.readFloat();

                {
                int len = in.readInt();
                g = (byte[]) in.readObject();
                assertEquals(len, g.length);
                }

                {
                int len = in.readInt();
                h = (byte[]) in.readObject();
                assertEquals(len, h.length);
                }
            }

        @Override
        public void writeExternal(ObjectOutput out)
                throws IOException
            {
            out.writeInt(id);
            out.writeUTF(a);
            out.writeUTF(b);
            out.writeLong(c);
            out.writeByte(d);
            out.writeDouble(e);
            out.writeFloat(f);

            assert (g.length > 0);
            out.writeInt(g.length);
            out.writeObject(g);

            assert (h.length > 0);
            out.writeInt(h.length);
            out.writeObject(h);
            }

        /**
         * Randomly assigning the fields to random values.
         *
         * @return the
         */
        private RandomType mutateFields()
            {
            RandomType other = new RandomType();
            Random random = Base.getRandom();

            other.id = id;

            if (willMutate())
                {
                other.a = randomString();
                }
            else
                {
                other.a = a;
                }

            if (willMutate())
                {
                other.b = randomString();
                }
            else
                {
                other.b = b;
                }

            if (willMutate())
                {
                other.c = random.nextLong();
                }
            else
                {
                other.c = c;
                }

            if (willMutate())
                {
                other.d = (byte) random.nextInt(0xFF);
                }
            else
                {
                other.d = d;
                }

            if (willMutate())
                {
                other.e = random.nextDouble();
                }
            else
                {
                other.e = e;
                }

            if (willMutate())
                {
                other.f = random.nextFloat();
                }
            else
                {
                other.f = f;
                }

            if (willMutate())
                {
                other.g = new byte[random.nextInt(BYTE_ARRAY_LEN) + 1];
                random.nextBytes(g);
                }
            else
                {
                other.g = g;
                }

            if (willMutate())
                {
                other.h = new byte[random.nextInt(BYTE_ARRAY_LEN) + 1];
                random.nextBytes(h);
                }
            else
                {
                other.h = h;
                }

            return other;
            }

        private void randomizeFields()
            {
            Random random = Base.getRandom();
            int min = random.nextInt(128);
            int max = random.nextInt(256) + min;

            a = Base.getRandomString(min, max, true);

            min = random.nextInt(128);
            max = random.nextInt(256) + min;
            b = Base.getRandomString(min, max, true);

            c = random.nextLong();

            d = (byte) random.nextInt(0xFF);

            e = random.nextDouble();

            f = random.nextFloat();

            g = new byte[random.nextInt(BYTE_ARRAY_LEN) + 1];
            random.nextBytes(g);

            h = new byte[random.nextInt(BYTE_ARRAY_LEN) + 1];
            random.nextBytes(h);
            }

        private String randomString()
            {
            Random random = Base.getRandom();

            int min = random.nextInt(128);
            int max = random.nextInt(256) + min;

            return Base.getRandomString(min, max, true);
            }

        private boolean willMutate()
            {
            Random random = Base.getRandom();

            return random.nextInt(4) > 2;
            }

        private static final int BYTE_ARRAY_LEN = 700;

        private int id;
        private String a;
        private String b;
        private long c;
        private byte d;
        private double e;
        private float f;
        private byte[] g;
        private byte[] h;
        }

    public static class TestMap
            extends HashMap<Integer, RandomType>
        {
        public TestMap(NamedCache namedCache)
            {
            m_cache = namedCache;
            }

        /**
         * @{inheritDoc
         */
        @Override
        public void clear()
            {
            m_cache.clear();
            super.clear();
            assertEquals(0, size());
            assertEquals(0, m_cache.size());
            }

        /**
         * @{inheritDoc
         */
        @Override
        public RandomType get(Object key)
            {
            RandomType getBack = (RandomType) m_cache.get(key);
            RandomType getFront = super.get(key);
            assertTrue((getFront == null && getBack == null) || getBack.equals(getFront));
            return getFront;
            }

        /**
         * @return the cache
         */
        public NamedCache getCache()
            {
            return m_cache;
            }

        /**
         * @{inheritDoc
         */
        @Override
        public RandomType put(Integer key, RandomType value)
            {
            RandomType putBack = (RandomType) m_cache.put(key, value);
            RandomType putFront = super.put(key, value);
            if (putFront == null)
                {
                Assert.assertNull(putBack);
                }
            else
                {
                Assert.assertEquals(putFront, putBack);
                }
            return putFront;
            }

        /**
         * @{inheritDoc
         */
        @Override
        public void putAll(Map map)
            {
            m_cache.putAll(map);
            super.putAll(map);
            Assert.assertEquals(m_cache.size(), size());
            }

        /**
         * @{inheritDoc
         */
        @Override
        public RandomType remove(Object key)
            {
            RandomType remBack = (RandomType) m_cache.remove(key);
            RandomType remFront = super.remove(key);
            if (remFront == null)
                {
                Assert.assertNull(remBack);
                }
            else
                {
                Assert.assertEquals(remFront, remBack);
                }
            return remFront;
            }

        public void validate()
            {
            System.out.println("Comparing expected map of size: " + size() + " with cache of size: " + m_cache.size());
            assertEquals(size(), m_cache.size());
            for (Object o : m_cache.keySet())
                {
                Integer key = (Integer) o;
                get(key); // the get will check the consistency

                if ((key.intValue() % 10) == 0)
                    {
                    System.out.print('.');
                    }
                }
            System.out.println("\nValidation OK!");
            }


        private final NamedCache m_cache;

        /**
         *
         */
        private static final long serialVersionUID = 1L;
        }

    private static final String CACHE_NAME = "dist-d";

    private static final String CACHE_NAME_PROPERTY = "cache.name";

    private static final String SESSIONS_PROPERTY = "sessions";

    private static final String MIN_OPERATIONS_PROPERTY = "minOperations";

    private static final String MAX_OPERATIONS_PROPERTY = "maxOperations";

    private static final String KEY_RANGE_PROPERTY = "keys";

    private static final String INITIAL_SIZE_PROPERTY = "initialSize";

    private Map<Integer, RandomType> m_map;

    /**
     * The sequence of operation to carry out
     */
    public List<MapOperation<Integer, RandomType>> session = new ArrayList<MapOperation<Integer, RandomType>>();

    }