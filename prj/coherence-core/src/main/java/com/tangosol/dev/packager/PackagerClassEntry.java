/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.packager;


import  java.io.ByteArrayOutputStream;
import  java.io.File;
import  java.io.InputStream;
import  java.io.IOException;


/**
* This kind of PackagerEntry is specified as a Class entry.
*/
public class PackagerClassEntry
        extends PackagerBaseEntry
    {
    /**
    * Create a new PackagerClassEntry as an uninitialized Java Bean.
    */
    public PackagerClassEntry()
        {
        this(null);
        }

    /**
    * Create a new PackagerClassEntry for the specified Java class
    */
    public PackagerClassEntry(String entryClassName)
        {
        super();
        setEntryClassName(entryClassName);
        }

    /**
    * Get the Java class modelled by the PackagerClassEntry.
    */
    public String getEntryClassName()
        {
        return entryClassName;
        }

    /**
    * Set the Java class modelled by the PackagerClassEntry.
    */
    public void setEntryClassName(String entryClassName)
        {
        this.entryClassName = entryClassName;
        modificationTime = -1;
        }

    /**
    * Return the path associated with the PackagerClassEntry.
    */
    public String getPathName()
        {
        return getPath().getPathName();
        }

    /**
    * Returns a PackagerPath for the PackagerEntry.
    */
    public PackagerPath getPath()
        {
        if (entryPath == null)
            {
            entryPath = new ClassPackagerPath(getEntryClassName());
            }
        return entryPath;
        }

    /**
    * Sets a PackagerPath for the PackagerEntry.
    */
    public void setPath(PackagerPath path)
        {
        entryPath = path;
        }

    /**
    * Returns the PackagerEntry as binary data suitable for packager.
    * The specified ClassLoader is used to find the class file as a
    * ClassLoader resource using ClassLoader.getResourceAsStream().
    */
    public byte[] getData(ClassLoader classLoader)
            throws PackagerEntryNotFoundException
        {
        try
            {
            InputStream entryStream =
                    classLoader.getResourceAsStream(getEntryClassName().replace('.', '/') + ".class");
            if (entryStream == null)
                {
                throw new PackagerEntryNotFoundException(getPathName());
                }

            ByteArrayOutputStream collector = new ByteArrayOutputStream();
            try
                {
                byte[] buffer = new byte[4096];

                while (true)
                    {
                    int bytesRead = entryStream.read(buffer);
                    if (bytesRead == -1)
                        {
                        break;
                        }
                    else
                        {
                        collector.write(buffer, 0, bytesRead);
                        }
                    }
                }
            finally
                {
                entryStream.close();
                }

            return collector.toByteArray();
            }
        catch (IOException ioe)
            {
            throw new UnexpectedPackagerException(ioe);
            }
        }

    /**
    * Return the modification time of the class file, if the class is loaded
    * from a normal file in the classpath.  Otherwise, returns the current
    * time.
    */
    public long getModificationTime()
        {
        if (modificationTime == -1)
            {
            File classFile = findClassFile();
            if (classFile != null)
                modificationTime = classFile.lastModified();
            }

        return modificationTime == -1 ?
            super.getModificationTime() : modificationTime;
        }

    /**
    * Returns a comment string for the PackagerClasEntry.
    */
    public String getComment()
        {
        return null;
        }

    /**
    * Returns true if this entry has to be "secured" in a package
    */
    public boolean isSecured()
        {
        return true;
        }

    /**
    * Return a File representing the class file from which the class
    * was loaded in the current classpath.  If the class was not loaded
    * directly from a file, this method returns null.
    */
    private File findClassFile()
        {
        return findEntryFile();
        }


    // ----- data members ---------------------------------------------------

    private String        entryClassName;
    private PackagerPath  entryPath;
    private long          modificationTime = -1;
    }
