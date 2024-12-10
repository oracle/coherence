
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.FileHelper

package com.tangosol.coherence.component.util;

import java.io.File;
import java.io.IOException;

/**
 * This abstract component contains various helper methods related to files
 * manipulation.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class FileHelper
        extends    com.tangosol.coherence.component.Util
    {
    // ---- Fields declarations ----
    
    // Initializing constructor
    public FileHelper(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        }
    
    //++ getter for static property _CLASS
    /**
     * Getter for property _CLASS.<p>
    * Property with auto-generated accessor that returns the Class object for a
    * given component.
     */
    public static Class get_CLASS()
        {
        Class clz;
        try
            {
            clz = Class.forName("com.tangosol.coherence/component/util/FileHelper".replace('/', '.'));
            }
        catch (ClassNotFoundException e)
            {
            throw new NoClassDefFoundError(e.getMessage());
            }
        return clz;
        }
    
    //++ getter for autogen property _Module
    /**
     * This is an auto-generated method that returns the global [design time]
    * parent component.
    * 
    * Note: the class generator will ignore any custom implementation for this
    * behavior.
     */
    private com.tangosol.coherence.Component get_Module()
        {
        return this;
        }
    
    /**
     * Tries to represent the specified path as a relative path to the root
    * directory.
    * 
    * @return string representing the relative path such that the following
    * holds true:
    *               new File(new File(sRootDir), sRelPath).equals(new
    * File(sPath))
    * 
    * Note: the returned relative path uses '/' as a separator, since it'll
    * work on both Windows and Unix OS
     */
    public static String getRelativePath(String sPath, String sRoot)
        {
        // import java.io.File;
        // import java.io.IOException;
        
        final char SEP = File.separatorChar;
        try
            {
            String sRootAbs = new File(sRoot).getCanonicalPath();
            String sPathAbs = new File(sPath).getCanonicalPath();
             
            // try trivial cases first
            if (sPathAbs.equals(sRootAbs))
                {
                return "./";
                }
            if (sPathAbs.startsWith(sRootAbs + SEP))
                {
                return "./" + sPathAbs.substring(sRootAbs.length() + 1).replace(SEP, '/');
                }
        
            // general case
            char[] achRoot = sRootAbs.toCharArray();
            char[] achPath = sPathAbs.toCharArray();
        
            int cMin   = Math.min(achRoot.length, achPath.length);
            int ofRoot = -1;   // offset of the largest common root
            int ofDiff = cMin; // offset of the first difference
            for (int of = 0; of < cMin; of++)
                {
                char ch = achPath[of];
                if (ch == achRoot[of])
                    {
                    if (ch == SEP)
                        {
                        ofRoot = of;
                        }
                    }
                else
                    {
                    ofDiff = of;
                    break;
                    }
                }
            
            if (ofRoot > 0)
                {
                // there is a common root
                // walk the "root" up and then the "path" down
                StringBuilder sbPath = new StringBuilder();
        
                for (int of = achRoot.length - 1; of >= ofRoot; --of)
                    {
                    if (achRoot[of] == SEP)
                        {
                        sbPath.append("../");
                        }
                    }
        
                sbPath.append(sPathAbs.substring(ofRoot + 1).replace(SEP, '/'));
        
                _assert(new File(new File(sRoot), sbPath.toString()).getCanonicalPath().equals(sPathAbs));
                return sbPath.toString();
                }
            }
        catch (IOException ignored)
            {
            }
        return sPath.replace('\\', '/');
        }
    
    /**
     * Checks whether or not the specified file could be read from, written
    * into, deleted or renamed.
    * 
    * @param file  the file to check
    * 
    * @return true if the specified file is fully accessible; false otherwise
     */
    public static boolean isFullyAccessible(java.io.File file)
        {
        // import java.io.File;
        // import java.io.IOException;
        
        // TODO: when JDK 1.4 is shipped this should be replaced
        // using FileChannel.tryLock() call
        
        try
            {
            file = file.getCanonicalFile();
        
            if (file.exists())
                {
                if (!file.canRead() || !file.canWrite())
                    {
                    return false;
                    }
        
                File fileTmp = File.createTempFile("tmp", ".tmp", file.getParentFile());
                fileTmp.delete();
                if (!file.renameTo(fileTmp))
                    {
                    return false;
                    }
                if (!fileTmp.renameTo(file))
                    {
                    throw new IllegalStateException(
                        "File: " + file + " was temporarily renamed to: " + fileTmp +
                        " but could not be renamed back. The operation should be performed manually.");
                    }
                return true;
                }
            else
                {
                if (file.createNewFile())
                    {
                    file.delete();
                    return true;
                    }
                else
                    {
                    return false;
                    }
                }
            }
        catch (IOException e)
            {
            return false;
            }
        }
    }
