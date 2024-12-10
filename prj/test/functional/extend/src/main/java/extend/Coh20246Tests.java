/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;


import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;

import com.tangosol.util.filter.EqualsFilter;

import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.net.partition.KeyAssociator;

import com.tangosol.util.Binary;
import com.tangosol.util.Filter;

import java.io.IOException;

import static org.junit.Assert.*;


/**
* Coherence*Extend test for the COH-20246.
*
* @author bbc  2019.11.15
*/
public class Coh20246Tests
        extends AbstractExtendTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public Coh20246Tests()
        {
        super(sCache, FILE_CLIENT_CFG_CACHE);
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        startCacheServerWithProxy("Proxy_Coh20246", FILE_SERVER_CFG_CACHE);
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer(sCache);
        }


    /**
     * COH-20246
     *
     * Test the NearCache getAll.
     */
    @Test
    public void testCoh20246()
        {
        try
            {
            int cSize = 10;
            NamedCache<Key, Value> cache = getNamedCache();
            cache.clear();

            assertEquals(cache.size(), 0);

            if (cache.size() == 0)
                {
                for (int i = 0; i < cSize; i++)
                    {
                    if (i % 2 == 0)
                        {
                        put(cache, "TestKey" + i, "TestValue" + 2);
                        }
                    else
                        {
                        put(cache, "TestKey" + i, "TestValue" + i);
                        }
                    }
                }

            Filter filter  = new EqualsFilter("getTextValue", "TestValue2");
            Set<Key> setKeys = cache.keySet(filter);

            assertEquals(cSize / 2, setKeys.size());

            Map<Key, Value> mapResult = cache.getAll(setKeys);

            // before the fix, mapResult.size is 0
            assertEquals(setKeys.size(), mapResult.size());
            }
        catch (Exception e)
            {
            fail("extend.testCoh20246 failed with " + e.getMessage());
            }
        }


    // ------- helper classes --------

    public void put(NamedCache cache, String sKey, String sValue)
        {
        Key key = new Key();
        key.setTextKey(sKey);
        Value value = new Value();
        value.setTextValue(sValue);

        cache.put(key, value);
        }

    /**
     * Test Key class
     */
    public static class Key
        {
        private String textKey;

        public Key()
            {
            }

        public String toString()
            {
            return "Key [textKey=" + this.textKey + "]";
            }

        public int hashCode()
            {
            boolean prime = true;
            int result = 1;
            result = 31 * result + (this.textKey == null ? 0 : this.textKey.hashCode());
            return result;
            }

        public boolean equals(Object obj)
            {
            if (this == obj)
                {
                return true;
                }
            else if (obj == null)
                {
                return false;
                }
            else if (this.getClass() != obj.getClass())
                {
                return false;
                }
            else
                {
                Key other = (Key) obj;
                if (this.textKey == null)
                    {
                    if (other.textKey != null)
                        {
                        return false;
                        }
                    }
                else if (!this.textKey.equals(other.textKey))
                    {
                    return false;
                    }
                }

            return true;
            }

        public String getTextKey()
            {
            return this.textKey;
            }

        public void setTextKey(String textKey)
            {
            this.textKey = textKey;
            }
        }

    /**
     * Test Value class
     */
    public static class Value
        {
        private String textValue;

        public Value()
            {
            }

        public String getTextValue()
            {
            return this.textValue;
            }

        public void setTextValue(String textValue)
            {
            this.textValue = textValue;
            }

        public int hashCode()
            {
            boolean prime = true;
            int result = 1;
            result = 31 * result + (this.textValue == null ? 0 : this.textValue.hashCode());
            return result;
            }

        public boolean equals(Object obj)
            {
            if (this == obj)
                {
                return true;
                }
            else if (obj == null)
                {
                return false;
                }
            else if (this.getClass() != obj.getClass())
                {
                return false;
                }
            else
                {
                Value other = (Value)obj;
                if (this.textValue == null)
                    {
                    if (other.textValue != null)
                        {
                        return false;
                        }
                    }
                else if (!this.textValue.equals(other.textValue))
                    {
                    return false;
                    }

                return true;
                }
            }

        public String toString()
            {
            return "Value [textValue=" + this.textValue + "]";
            }
        }

    /**
     * Serializer for Key
     */
    public static class KeySerializer implements PofSerializer<Key>
        {
        public KeySerializer()
            {
            }

        public void serialize(PofWriter pofWriter, Key key) throws IOException
            {
            pofWriter.writeString(0, key.getTextKey());
            pofWriter.writeRemainder((Binary)null);
            }

        public Key deserialize(PofReader pofReader) throws IOException
            {
            Key key = new Key();
            key.setTextKey(pofReader.readString(0));
            pofReader.readRemainder();
            return key;
            }
        }

    /**
     * Serializer for Value
     */
    public static class ValueSerializer implements PofSerializer<Value>
        {
        public ValueSerializer()
            {
            }

        public void serialize(PofWriter pofWriter, Value value) throws IOException
            {
            pofWriter.writeString(0, value.getTextValue());
            pofWriter.writeRemainder((Binary)null);
            }

        public Value deserialize(PofReader pofReader) throws IOException
            {
            Value value = new Value();
            value.setTextValue(pofReader.readString(0));
            pofReader.readRemainder();
            return value;
            }
        }

    /**
     * KeyAssociator used in the test.
     */
    public static class KeyCacheAssociator implements KeyAssociator
        {
        public KeyCacheAssociator()
            {};

        public Object getAssociatedKey(Object oKey)
            {
            if (oKey instanceof Key)
                {
                return ((Key)oKey).getTextKey().substring(0, 3);
                }
            return oKey;
            }

        public void init(PartitionedService service)
            {
            // Nothing to do here
            }
        }

    /**
     * Test cache name
     */
    public static String sCache = "dist-extend-near-present";

    /**
     * Client cache config
     */
    public static String FILE_CLIENT_CFG_CACHE = "client-cache-config.xml";

    /**
     * Server cache config
     */
    public static String FILE_SERVER_CFG_CACHE = "server-cache-config.xml";
    }
