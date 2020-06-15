/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.component;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;


/**
* WarStorage is a JarStorage that understands the structure
* of the web archive (.war) file
* The peculiar feature of the WarStorage is that it shows resources
* coming from two places: "WEB-INF/classes/" directory and root.
* This storage flattens those two name spaces, giving the WEB-INF/classes/
* a priority (in reality a name conflict is highly unlikely)
*
* @version 1.00, 06/19/01
* @author  gg
*/
public class WarStorage
        extends JarStorage
    {
    // ----- constructors ---------------------------------------------------
    /**
    * Create a storage object backed by the web archive.
    *
    * @param fileWar   the File object specifying the web archive to
    *                  read entries from
    */
    public WarStorage(File fileWar)
        {
        super(fileWar, ROOT);
        }

    /**
    * Create a storage object backed by the web archive that is an entry
    * in the specified enterprise archive.
    *
    * @param fileEar the File object specifying the enterprise archive that 
    *                the web archive is a part of
    * @param sName   the name of the web archive entry in the 
    *                enterprise archive
    *
    */
    public WarStorage(File fileEar, String sName)
        {
        super(fileEar, sName + "!/" + ROOT);
        }

    // ----- Resources --------------------------------------------------------

    /**
    * Load the original (before any customization takes place) resource.
    *
    * @param sName  fully qualified resource name
    *
    * @return the specified resource as a byte array
    *
    * @exception IOException  if an unrecoverable error occurs
    */
    public byte[] loadOriginalResource(String sName)
            throws IOException
        {
        // if the resource cannot be found,
        // try to load from the root
        byte[] ab = super.loadOriginalResource(sName);

        if (ab == null)
            {
            try
                {
                ab = loadFileBytes("/" + sName);
                }
            catch (FileNotFoundException e)
                {
                }
            }

        return ab;
        }

    // ---- helpers --------------------------------------------------------

    /** 
    * @return a type of the specified entry; -1 if the entry type is not known
    */
    protected int getEntryType(String sEntry)
        {
        int iType = super.getEntryType(sEntry);

        if (iType == -1)
            {
            // everything, but the manifest is a resource
            // even if not located under "WEB-INF/classes/"
            if (!sEntry.endsWith("/") &&
                !sEntry.equalsIgnoreCase("META-INF/Manifest.mf"))
                {
                iType = T_RES_BINARY;
                }
            }
        return iType;
        }
    
    // ----- Object methods -------------------------------------------------

    /**
    * Provide a short human-readable description of the trait.
    *
    * @return a human-readable description of this trait
    */
    public String toString()
        {
        return CLASS + '(' + getJar().getName() + ')';
        }    

    
    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "WarStorage";
    
    /**
    * The root entry for classes in a web archive
    */
    private static final String ROOT = "WEB-INF/classes/";
    }
    
