/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.packager;


import  com.tangosol.io.Base64OutputStream;

import  java.io.FileOutputStream;
import  java.io.IOException;

import  java.util.Collection;
import  java.util.Iterator;
import  java.util.Properties;

import  java.util.jar.Attributes;
import  java.util.jar.JarEntry;
import  java.util.jar.Manifest;
import  java.util.jar.JarOutputStream;

import  java.util.zip.CRC32;
import  java.util.zip.ZipEntry;

import  java.security.MessageDigest;
import  java.security.NoSuchAlgorithmException;


/**
*  This kind of PackagerSet materializes its collection of files in a
*  Jar file with a Manifest.
*/
public class JarFilePackagerSet
        extends PackagerSet
    {
    /**
    *  Construct a JarFilePackagerSet.
    */
    public JarFilePackagerSet()
        {
        super();
        }

    /**
    *  Construct a JarFilePackagerSet to be materialized to the file specified.
    */
    public JarFilePackagerSet(String fileName)
            throws IOException
        {
        super();
        setJarFilePathName(fileName);
        }

    /**
    *  Return whether file compression will be used for the JarFilePackagerSet.
    */
    public boolean isCompressed()
        {
        return(compressed);
        }

    /**
    *  Set whether file compression will be used for the JarFilePackagerSet.
    *  This method must be called before the JarFilePackagerSet is materialized.
    */
    public void setCompressed(boolean compressed)
        {
        this.compressed = compressed;
        }

    /**
    *  Return the manifest entry. This method could be overriden by subclasses
    *  to supply a custom manifest
    */
    public Manifest getManifest()
        {
        return(manifest);
        }

    /**
    *  Sets the manifest. This method could be used by a client to supply
    *  a custom Manifest.
    *
    *  This method must be called before the JarFilePackagerSet is materialized.
    */
    public void setManifest(Manifest manifest)
        {
        this.manifest = manifest;
        }

    /**
    *  Return the style of the manifest generation.
    *
    *  Note: this value has no relevance if the Manifest is set directly
    */
    public int getManifestStyle()
        {
        return(style);
        }

    /**
    *  Sets the style of the manifest generation. Valid values are:
    *    MANIFEST_NONE   -- no manifest generation is required
    *    MANIFEST_BLANK  -- manifest should be empty
    *    MANIFEST_HEADER -- manifest should be only contain the header
    *    MANIFEST_FULL   -- full manifest should be generated
    *
    *  This method must be called before the JarFilePackagerSet is materialized.
    */
    public void setManifestStyle(int style)
        {
        this.style = style;
        }

    /**
    *  Return the file name to be used for materialization.
    */
    public String getJarFilePathName()
        {
        return(jarFilePathName);
        }

    /**
    *  Set the file name to be used for materialization.
    */
    public void setJarFilePathName(String jarFilePathName)
            throws IOException
        {
        this.jarFilePathName = jarFilePathName;
        }

    /**
    *  Materialize the PackagerSet from its specified entries in the context
    *  of the specified ClassLoader.
    */
    public void materialize(ClassLoader classLoader)
            throws IOException, UnexpectedPackagerException
        {
        Collection entries  = getCollectedEntries();
        Manifest   manifest = getManifest();

        if (manifest == null)
            {
            manifest = createManifest(classLoader);
            }

        FileOutputStream fileOut = new FileOutputStream(jarFilePathName);
        JarOutputStream  jarOut  = manifest == null ?
            new JarOutputStream(fileOut) : new JarOutputStream(fileOut, manifest);

        try
            {
            // store all the entries with their data
            Iterator entriesIterator = entries.iterator();
            while (entriesIterator.hasNext())
                {
                PackagerEntry entry = (PackagerEntry)entriesIterator.next();

                try
                    {
                    storeEntryData(jarOut, entry, classLoader);
                    }
                catch (PackagerEntryNotFoundException e)
                    {
                    throw new IOException("Failed to store " + entry + ": " + e);
                    }
                }
            }
        finally
            {
            jarOut.close();
            }
        }

    /**
    *  Create the Manifest for the Jar file in the context of the specified
    *  ClassLoader.
    *
    *  This method assigns top-level attributes in the manifest, creates
    *  entries for each PackagerEntry, and computes and records SHA and
    *  MD5 message digests for each entry (excluding directory entries
    *  and package entries which have zero length).
    */
    protected Manifest createManifest(ClassLoader classLoader)
        {
        if (style == MANIFEST_NONE)
            {
            return null;
            }

        Manifest manifest = new Manifest();

        if (style > MANIFEST_BLANK)
            {
            Collection entries = getCollectedEntries();

            // initialize main attributes
            Attributes mainAttrs = manifest.getMainAttributes();
            mainAttrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");

            // copy attributes from the PackagerSet to the manifest
            Properties mainProps = getAttributes();
            for (Iterator propKeys = mainProps.keySet().iterator(); propKeys.hasNext(); )
                {
                String key = (String)propKeys.next();
                mainAttrs.put(new Attributes.Name(key), mainProps.getProperty(key));
                }

            if (style > MANIFEST_HEADER)
                {
                // set things up to compute message digest attributes
                String[] digestAlgorithms = {"SHA", "MD5"};
                Attributes.Name digestAlgorithmsAttributeName = new Attributes.Name("Digest-Algorithms");
                Attributes.Name[] digestAttributeNames =
                    {
                    new Attributes.Name("SHA-Digest"),
                    new Attributes.Name("MD5-Digest"),
                    };

                MessageDigest[] msgDigests = new MessageDigest[2];
                String digestNames = null;
                for (int i = 0, n = digestAlgorithms.length; i < n; i++)
                    {
                    try
                        {
                        msgDigests[i] = MessageDigest.getInstance(digestAlgorithms[i]);
                        if (digestNames == null)
                            {
                            digestNames = digestAlgorithms[i];
                            }
                        else
                            {
                            digestNames += " " + digestAlgorithms[i];
                            }
                        }
                    catch (NoSuchAlgorithmException noSuchAlgorithm) {}
                    }

                // compute entry attributes and build the manifest
                for (Iterator entriesIterator = entries.iterator(); entriesIterator.hasNext(); )
                    {
                    PackagerEntry entry = (PackagerEntry)entriesIterator.next();
                    Attributes attributes = new Attributes();

                    // compute message digests for entry unless it's a directory or package
                    if (digestNames != null && entry.isSecured())
                        {
                        try
                            {
                            attributes.put(digestAlgorithmsAttributeName, digestNames);
                            byte[] entryData = entry.getData(classLoader);
                            if (entryData == null)
                                {
                                throw new PackagerEntryNotFoundException("No data for: " + entry);
                                }

                            for (int i = 0, n = digestAlgorithms.length; i < n; i++)
                                {
                                MessageDigest msgDigest = msgDigests[i];
                                if (msgDigest != null)
                                    {
                                    msgDigest.reset();
                                    byte[] digestValue  = msgDigest.digest(entryData);
                                    String digestString = new String(Base64OutputStream.encode(digestValue, false));
                                    attributes.put(digestAttributeNames[i], digestString);
                                    }
                                }
                            }
                        catch (PackagerEntryNotFoundException noSuchEntry)
                            {
                            // out(noSuchEntry.getMessage());
                            continue;
                            }

                        // copy attributes from the PackagerEntry to the ZipEntry
                        Properties entryProps = entry.getAttributes();
                        if (entryProps != null)
                            {
                            for (Iterator propKeys = entryProps.keySet().iterator(); propKeys.hasNext(); )
                                {
                                String key = (String)propKeys.next();
                                attributes.put(new Attributes.Name(key), entryProps.getProperty(key));
                                }
                            }
                        }

                    // add the entry with its attributes to the manifest
                    manifest.getEntries().put(entry.getPath().getPathName(), attributes);
                    }
                }
            }

        return manifest;
        }

    /**
    *  Store the PackagerEntry in the PackagerSet, in the context of the
    *  specified ClassLoader.
    *  This method creates and initialized a ZipEntry for the PackagerEntry.
    */
    protected void storeEntryData(JarOutputStream jarOut, PackagerEntry entry, ClassLoader classLoader)
            throws IOException, PackagerEntryNotFoundException
        {
        PackagerPath path = entry.getPath();
        byte[] data = entry.getData(classLoader);
        if (data == null)
            {
            throw new PackagerEntryNotFoundException("No data for: " + entry);
            }

        ZipEntry zipEntry = new JarEntry(path.getPathName());
        int entrySize  = ((data == null) ? 0 : data.length);

        zipEntry.setTime(entry.getModificationTime());
        String comment = entry.getComment();
        if (comment != null)
            {
            zipEntry.setComment(comment);
            }

        if (entrySize == 0)
            {
            zipEntry.setMethod(JarOutputStream.STORED);
            zipEntry.setSize(0L);
            zipEntry.setCrc(0L);
            }
        else
            {
            zipEntry.setMethod(JarOutputStream.DEFLATED);

            if (!isCompressed())
                {
                crc32.reset();
                crc32.update(data);
                zipEntry.setSize(data.length);
                zipEntry.setCrc(crc32.getValue());
                }
            }

        jarOut.putNextEntry(zipEntry);
        jarOut.write(data, 0, entrySize);
        jarOut.closeEntry();
        }


    // ----- constants ------------------------------------------------------

    public final static int  MANIFEST_NONE    = 0;
    public final static int  MANIFEST_BLANK   = 1;
    public final static int  MANIFEST_HEADER  = 2;
    public final static int  MANIFEST_FULL    = 3;


    // ----- data members ---------------------------------------------------

    private       String     jarFilePathName;
    private       boolean    compressed       = true;
    private       Manifest   manifest         = null;
    private       int        style            = MANIFEST_NONE;
    private       CRC32      crc32            = new CRC32();
    }
