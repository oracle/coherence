/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.cachestores;

import com.oracle.coherence.common.base.Logger;
import com.tangosol.net.cache.CacheStore;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

/**
 * A trivial implementation of a {@link CacheStore} which stores values in files with the name of the key
 * under a specific directory. Note: This is an example only to demonstrate how {@link CacheStore}s work and
 * will not work with multiple cache servers.
 *
 * @author Tim Middleton 2020.02.18
 */
// #tag::class[]
public class FileCacheStore
        implements CacheStore<Integer, String> {  // <1>

    /**
     * Base directory off which to store data.
     */
    private final File directory;

    public FileCacheStore(String directoryName) {  // <2>
        if (directoryName == null || directoryName.equals("")) {
            throw new IllegalArgumentException("A directory must be specified");
        }

        directory = new File(directoryName);
        if (!directory.isDirectory() || !directory.canWrite()) {
            throw new IllegalArgumentException("Unable to open directory " + directory);
        }
        Logger.info("FileCacheStore constructed with directory " + directory);
    }

    @Override
    public void store(Integer key, String value) {  // <3>
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(getFile(directory, key), false));
            writer.write(value);
            writer.close();
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to delete key " + key, e);
        }
    }

    @Override
    public void erase(Integer key) {  // <4>
        // we ignore result of delete as the key may not exist
        getFile(directory, key).delete();
    }

    @Override
    public String load(Integer key) {  // <5>
        File file = getFile(directory, key);
        try {
            // use Java 1.8 method
            return Files.readAllLines(file.toPath()).get(0);
        }
        catch (IOException e) {
            return null;  // does not exist in cache store
        }
    }

    protected static File getFile(File directory, Integer key) {
        return new File(directory, key + ".txt");
    }
}
// #end::class[]
