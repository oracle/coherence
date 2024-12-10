/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.cachestores;

import com.oracle.coherence.common.base.Logger;
import com.tangosol.io.FileHelper;
import com.tangosol.net.NamedMap;
import com.tangosol.net.cache.CacheStore;
import com.tangosol.net.cache.TypeAssertion;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * A simple test class showing a simple {@link CacheStore}.
 *
 * @author Tim Middleton  2021.02.17
 */
// #tag::class[]
public class FileCacheStoreTest
        extends AbstractCacheStoreTest {

    private static File baseDirectory;


    @BeforeAll
    public static void startup() throws IOException {
        // #tag::beforeAll[]
        baseDirectory = FileHelper.createTempDir();
        // baseDirectory = new File("/tmp/tim");
        // #end::beforeAll[]
        System.setProperty("test.base.dir", baseDirectory.getAbsolutePath());

        startupCoherence("file-cache-store-cache-config.xml"); // <1>
    }
    
    @Test
    public void testFileCacheStore() throws IOException {
        try {
            Logger.info("Base directory " + baseDirectory.getAbsolutePath());
            NamedMap<Integer, String> namedMap = getSession()
                    .getMap("simple-test", TypeAssertion.withTypes(Integer.class, String.class)); // <2>

            namedMap.clear();

            // #tag::assert[]
            assertNull(namedMap.get(1));
            // #end::assert[]

            namedMap.put(1, "New Value"); // <3>
            assertEquals("New Value", namedMap.get(1));

            // ensure that the value from the file was written
            assertValueFromFile(1, "New Value");

            namedMap.put(1, "Updated Value"); // <3>
            assertEquals("Updated Value", namedMap.get(1));
            assertValueFromFile(1, "Updated Value");

            namedMap.remove(1);
            assertFalse(namedMap.containsKey(1));
            assertValueFromFile(1, null);

            Map<Integer, String> map = new HashMap<Integer, String>();
            for (int i = 2; i < 10; i++) {
                map.put(i, "Number " + i);
            }
            namedMap.putAll(map);
            
            for (int i = 2; i < 10; i++) {
                assertEquals("Number " + i, namedMap.get(i));
                assertValueFromFile(i, "Number " + i);
            }
        }
        finally {
            // #tag::delete[]
            FileHelper.deleteDir(baseDirectory);
            // #end::delete[]
        }
    }

    /**
     * Assert that the file related to the key is expected value or null
     *
     * @param key           the key to check
     * @param expectedValue the expected value or if null then the file shoudl not exist
     */
    private void assertValueFromFile(int key, String expectedValue) throws IOException {
        File keyFile = FileCacheStore.getFile(baseDirectory, key);
        if (expectedValue == null) {
            assertFalse(keyFile.exists(), "Key file " + keyFile + " should not exist");
        }
        else {
            assertEquals(expectedValue, Files.readAllLines(keyFile.toPath()).get(0));
        }
    }

}
// #end::class[]
