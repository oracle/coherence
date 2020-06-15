/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.util.Base;
import com.tangosol.util.Binary;

import java.io.File;

import java.util.Iterator;


/**
* Abstract implementation of the BinaryStore interface.
*
* @author cp 2003.05.30
*/
public abstract class AbstractBinaryStore
        extends Base
        implements BinaryStore
    {
    // ----- BinaryStore interface ------------------------------------------
    
    /**
    * Return the value associated with the specified key, or null if the
    * key does not have an associated value in the underlying store.
    *
    * @param binKey  key whose associated value is to be returned
    *
    * @return the value associated with the specified key, or
    *         <tt>null</tt> if no value is available for that key
    */
    public abstract Binary load(Binary binKey);

    /**
    * Store the specified value under the specific key in the underlying
    * store. This method is intended to support both key/value creation
    * and value update for a specific key.
    *
    * @param binKey    key to store the value under
    * @param binValue  value to be stored
    *
    * @throws UnsupportedOperationException  if this implementation or the
    *         underlying store is read-only
    */
    public void store(Binary binKey, Binary binValue)
        {
        throw new UnsupportedOperationException();
        }

    /**
    * Remove the specified key from the underlying store if present.
    *
    * @param binKey key whose mapping is to be removed from the map
    *
    * @throws UnsupportedOperationException  if this implementation or the
    *         underlying store is read-only
    */
    public void erase(Binary binKey)
        {
        throw new UnsupportedOperationException();
        }

    /**
    * Remove all data from the underlying store.
    *
    * @throws UnsupportedOperationException  if this implementation or the
    *         underlying store is read-only
    */
    public void eraseAll()
        {
        for (Iterator iter = keys(); iter.hasNext(); )
            {
            erase((Binary) iter.next());
            }
        }

    /**
    * Iterate all keys in the underlying store.
    *
    * @return a read-only iterator of the keys in the underlying store
    *
    * @throws UnsupportedOperationException  if the underlying store is not
    *         iterable
    */
    public Iterator keys()
        {
        throw new UnsupportedOperationException();
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Obtain a File object for the default directory to use for file storage.
    * The default directory is defined by the "java.io.tmpdir" system
    * property.
    *
    * @return a File object representing the location of the default
    *         directory
    */
    public static File getDefaultDirectory()
        {
        File dir = s_dirDefault;
        if (dir != null)
            {
            return dir;
            }

        String sDir = null;
        try
            {
            sDir = System.getProperty("java.io.tmpdir");
            }
        catch (Exception e) {}

        if (sDir == null)
            {
            try
                {
                File fileTemp = File.createTempFile("temp", null, null);
                fileTemp.delete();
                dir = fileTemp.getParentFile();
                }
            catch (Exception e)
                {
                throw new IllegalStateException(
                        "Temporary directory (java.io.tmpdir property) undefined or inaccessible.");
                }
            }
        else
            {
            dir = new File(sDir);
            if (!(dir.exists() && dir.isDirectory()))
                {
                throw new IllegalStateException(
                        "Temporary directory (" + sDir + ") does not exist.");
                }
            }

        s_dirDefault = dir;
        return dir;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The default directory to create files in. Lazily initialized.
    */
    private static File s_dirDefault;
    }