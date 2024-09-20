
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.Config

package com.tangosol.coherence.component.util;

import com.tangosol.coherence.component.Application;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

/**
 * This component is used to carry configuration data for "Configurable"
 * components.
 * 
 * @see Component#applyConfig
 * @see Component#saveConfig
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class Config
        extends    com.tangosol.coherence.component.Util
    {
    // ---- Fields declarations ----
    
    /**
     * Property _Properties
     *
     */
    private java.util.Properties __m__Properties;
    
    /**
     * Property ArrayDelimiter
     *
     * Delimiter used to serialize arrays of values into a String.
     */
    private char __m_ArrayDelimiter;
    
    /**
     * Property CONFIG_DIRECTORY
     *
     */
    public static final String CONFIG_DIRECTORY = "/META-INF";
    
    /**
     * Property CONFIG_EXTENSION
     *
     */
    public static final String CONFIG_EXTENSION = ".properties";
    
    // Default constructor
    public Config()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public Config(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        
        if (fInit)
            {
            __init();
            }
        }
    
    // Main initializer
    public void __init()
        {
        // private initialization
        __initPrivate();
        
        // state initialization: public and protected properties
        try
            {
            setArrayDelimiter(':');
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
        // signal the end of the initialization
        set_Constructed(true);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        
        // state initialization: private properties
        try
            {
            __m__Properties = new java.util.Properties();
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        }
    
    //++ getter for static property _Instance
    /**
     * Getter for property _Instance.<p>
    * Auto generated
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        return new com.tangosol.coherence.component.util.Config();
        }
    
    //++ getter for static property _CLASS
    /**
     * Getter for property _CLASS.<p>
    * Property with auto-generated accessor that returns the Class object for a
    * given component.
     */
    public static Class get_CLASS()
        {
        return Config.class;
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
    
    public void clear()
        {
        get_Properties().clear();
        }
    
    public boolean containsKey(String sKey)
        {
        return get_Properties().containsKey(sKey);
        }
    
    /**
     * Decodes the specified path by replacing environment variables (i.e.
    * "{java.home}") with their values.
     */
    public static String decodePath(String sPath)
        {
        while (true)
            {
            int ofStart = sPath.indexOf('{');
            if (ofStart >= 0)
                {
                int ofEnd = sPath.indexOf('}', ofStart);
                if (ofEnd > ofStart)
                    {
                    String sProp = sPath.substring(ofStart + 1, ofEnd);
                    String sVal  = System.getProperty(sProp, "");
        
                    sPath = sPath.substring(0, ofStart) + sVal + sPath.substring(ofEnd + 1);
                    continue;
                    }
                }
        
            return sPath;
            }
        }
    
    /**
     * Encodes the specified path by replacing parts of the specified path with
    * the names of the specified environment variables.
    * 
    * @see #encodePath
     */
    public static String encodePath(String sPath, String[] asEnv)
        {
        for (int i = 0, c = asEnv.length; i < c; i++)
            {
            String sEnv = asEnv[i];
            String sVal = System.getProperty(sEnv);
            int    of   = sVal == null || sVal.length() == 0 ? -1 : sPath.indexOf(sVal);
        
            if (of != -1)
                {
                sPath = sPath.substring(0, of) + '{' + sEnv + '}' +
                        sPath.substring(of + sVal.length() + 1);
                }
            }
        return sPath;
        }
    
    // Accessor for the property "_Properties"
    /**
     * Getter for property _Properties.<p>
     */
    private java.util.Properties get_Properties()
        {
        return __m__Properties;
        }
    
    // Accessor for the property "ArrayDelimiter"
    /**
     * Getter for property ArrayDelimiter.<p>
    * Delimiter used to serialize arrays of values into a String.
     */
    public char getArrayDelimiter()
        {
        return __m_ArrayDelimiter;
        }
    
    public boolean getBoolean(String sKey)
        {
        return getBoolean(sKey, false);
        }
    
    public boolean getBoolean(String sKey, boolean fDefaultValue)
        {
        String sValue = get_Properties().getProperty(sKey);
        return sValue != null ?
            Boolean.valueOf(sValue).booleanValue() : fDefaultValue;
        }
    
    public Config getConfig(String sKey)
        {
        // import java.util.Enumeration;
        // import java.util.Properties;
        
        Config     config   = new Config();
        Properties propThis = this  .get_Properties();
        Properties propThat = config.get_Properties();
        
        for (Enumeration e = propThis.keys(); e.hasMoreElements();)
            {
            String sKeyThis = (String) e.nextElement();
            
            if (sKeyThis.startsWith(sKey))
                {
                String sKeyThat = sKeyThis.substring(sKey.length());
                propThat.setProperty(sKeyThat, propThis.getProperty(sKeyThis));
                }
            }
        return config;
        }
    
    public int getInt(String sKey)
        {
        return getInt(sKey, 0);
        }
    
    public int getInt(String sKey, int iDefaultValue)
        {
        String sValue = get_Properties().getProperty(sKey);
        if (sValue != null)
            {
            try
                {
                return Integer.parseInt(sValue);
                }
            catch (NumberFormatException ignored) {}
            }
        
        return iDefaultValue;
        }
    
    public int[] getIntArray(String sKey)
        {
        String sValue = get_Properties().getProperty(sKey);
        if (sValue != null && sValue.length() > 0)
            {
            char   cDelim = getArrayDelimiter();
            char[] ach    = sValue.toCharArray();
            int    cnt    = 1;
            
            for (int of = 0; of < ach.length; of++)
                {
                if (ach[of] == cDelim)
                    {
                    cnt++;
                    }
                }
            
            int[] aInt = new int[cnt];
            
            try
                {
                int ix      = 0;
                int ofStart = 0;
                
                for (int of = 0; of < ach.length; of++)
                    {
                    if (ach[of] == cDelim)
                        {
                        aInt[ix++] = Integer.parseInt(new String(ach, ofStart, of - ofStart));
                        ofStart = of + 1;
                        }
                    }
                aInt[ix] = Integer.parseInt(new String(ach, ofStart, ach.length - ofStart));
                }
            catch (NumberFormatException ignored) {}
        
            return aInt;
            }
        else
            {
            return new int[0];
            }
        }
    
    public String getString(String sKey)
        {
        return get_Properties().getProperty(sKey);
        }
    
    public String getString(String sKey, String sDefaultValue)
        {
        return get_Properties().getProperty(sKey, sDefaultValue);
        }
    
    public String[] getStringArray(String sKey)
        {
        String sValue = get_Properties().getProperty(sKey);
        if (sValue != null && sValue.length() > 0)
            {
            char   cDelim = getArrayDelimiter();
            char[] ach    = sValue.toCharArray();
            int    cnt    = 1;
            
            for (int of = 0; of < ach.length; of++)
                {
                if (ach[of] == cDelim)
                    {
                    cnt++;
                    }
                }
            
            String[] aString = new String[cnt];
            
            int ix      = 0;
            int ofStart = 0;
                
            for (int of = 0; of < ach.length; of++)
                {
                if (ach[of] == cDelim)
                    {
                    aString[ix++] = new String(ach, ofStart, of - ofStart);
                    ofStart = of + 1;
                    }
                }
            aString[ix] = new String(ach, ofStart, ach.length - ofStart);
            
            return aString;
            }
        else
            {
            return new String[0];
            }
        }
    
    public String[] getStringArray(String sKey, char cDelim)
        {
        char cDelimOrig = getArrayDelimiter();
        
        setArrayDelimiter(cDelim);
        String[] asResult = getStringArray(sKey);
        setArrayDelimiter(cDelimOrig);
        
        return asResult;
        }
    
    // Accessor for the property "Empty"
    /**
     * Getter for property Empty.<p>
     */
    public boolean isEmpty()
        {
        return get_Properties().isEmpty();
        }
    
    public java.util.Enumeration keys()
        {
        return get_Properties().keys();
        }
    
    /**
     * Lists properties to standard output stream.
     */
    public void list()
        {
        // import java.io.PrintWriter;
        
        list(new PrintWriter(System.out));
        }
    
    public void list(java.io.PrintWriter writer)
        {
        get_Properties().list(writer);
        writer.flush();
        }
    
    public void load(java.io.InputStream inStream)
            throws java.io.IOException
        {
        get_Properties().load(inStream);
        }
    
    public void load(String sName)
        {
        // import Component.Application;
        // import java.io.InputStream;
        // import java.io.File;
        // import java.io.FileInputStream;
        // import java.io.IOException;
        
        // find configuration file
        // it may be in the current .JAR or in a file-system file
        InputStream in;
        do
            {
            // get the current application (it is responsible for loading
            // resources and providing system properties)
            Application app = (Application) Application.get_Instance();
        
            // try in the "configuration directory" location
            if (!sName.startsWith("/"))
                {
                String sPath = resolvePath(sName);
                in = app.getResourceAsStream(sPath);
                if (in != null)
                    {
                    break;
                    }
                }
        
            // try again in the JAR's (or "resource file system's") root
            String sFile = resolveName(sName);
            in = app.getResourceAsStream(sFile);
            if (in != null)
                {
                break;
                }
        
            // try in the "user home"
            try
                {
                String sDir = app.getProperty("user.home");
                if (sDir != null && sDir.length() > 0)
                    {
                    File file = new File(sDir, sFile);
                    if (file.isFile() && file.exists() && file.canRead())
                        {
                        in = new FileInputStream(file);
                        break;
                        }
                    }
                }
            catch (SecurityException ignored)
                {
                }
            catch (IOException ignored)
                {
                }
        
            // try in the "user dir"
            try
                {
                String sDir = app.getProperty("user.dir");
                if (sDir != null && sDir.length() > 0)
                    {
                    File file = new File(sDir, sFile);
                    if (file.isFile() && file.exists() && file.canRead())
                        {
                        in = new FileInputStream(file);
                        break;
                        }
                    }
                }
            catch (SecurityException ignored)
                {
                }
            catch (IOException ignored)
                {
                }
        
            // try in the "default dir"
            try
                {
                File file = new File("", sFile);
                if (file.isFile() && file.exists() && file.canRead())
                    {
                    in = new FileInputStream(file);
                    break;
                    }
                }
            catch (SecurityException ignored)
                {
                }
            catch (IOException ignored)
                {
                }
            }
        while (false);
        
        // load configuration
        if (in != null)
            {
            try
                {
                load(in);
                }
            catch (IOException e)
                {
                _trace("Exception loading configuration: " + sName);
                _trace(e);
                }
            }
        }
    
    public void putBoolean(String sKey, boolean fValue)
        {
        get_Properties().setProperty(sKey, String.valueOf(fValue));
        }
    
    public void putConfig(String sKey, Config config)
        {
        // import java.util.Enumeration;
        // import java.util.Iterator;
        // import java.util.Properties;
        
        // remove any exact match
        remove(sKey);
        
        // remove any nested matches
        final String sPrefix = sKey + '.';
        for (Iterator iter = get_Properties().keySet().iterator(); iter.hasNext();)
            {
            String sEachKey = (String) iter.next();
            if (sEachKey.startsWith(sPrefix))
                {
                iter.remove();
                }
            }
        
        if (config != null && !config.isEmpty())
            {
            Properties propThis = this  .get_Properties();
            Properties propThat = config.get_Properties();
        
            for (Enumeration e = propThat.keys(); e.hasMoreElements();)
                {
                String sKeyThat = (String) e.nextElement();
            
                propThis.setProperty(sKey + sKeyThat, propThat.getProperty(sKeyThat));
                }
            }
        }
    
    public void putInt(String sKey, int iValue)
        {
        get_Properties().setProperty(sKey, String.valueOf(iValue));
        }
    
    public void putIntArray(String sKey, int[] aiValue)
        {
        if (aiValue == null)
            {
            remove(sKey);
            }
        else
            {
            char cDelim = getArrayDelimiter();
        
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < aiValue.length; i++)
                {
                if (i > 0)
                    {
                    sb.append(cDelim);
                    }
                sb.append(aiValue[i]);
                }
        
            get_Properties().setProperty(sKey, sb.toString());
            }
        }
    
    public void putString(String sKey, String sValue)
        {
        if (sValue == null)
            {
            remove(sKey);
            }
        else
            {
            get_Properties().setProperty(sKey, sValue);
            }
        }
    
    public void putStringArray(String sKey, String[] asValue)
        {
        if (asValue == null)
            {
            remove(sKey);
            }
        else
            {
            char cDelim = getArrayDelimiter();
        
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < asValue.length; i++)
                {
                if (i > 0)
                    {
                    sb.append(cDelim);
                    }
                sb.append(asValue[i]);
                }
        
            get_Properties().setProperty(sKey, sb.toString());
            }
        }
    
    public void putStringArray(String sKey, String[] asValue, char cDelim)
        {
        char cDelimOrig = getArrayDelimiter();
        
        setArrayDelimiter(cDelim);
        putStringArray(sKey, asValue);
        setArrayDelimiter(cDelimOrig);
        }
    
    public void remove(String sName)
        {
        get_Properties().remove(sName);
        }
    
    public static String resolveName(String sName)
        {
        return sName.startsWith("/") ? sName : "/" + sName + CONFIG_EXTENSION;
        }
    
    public static String resolvePath(String sName)
        {
        return sName.startsWith("/") ? sName : CONFIG_DIRECTORY + resolveName(sName);
        }
    
    // Accessor for the property "_Properties"
    /**
     * Setter for property _Properties.<p>
     */
    private void set_Properties(java.util.Properties p_Properties)
        {
        __m__Properties = p_Properties;
        }
    
    // Accessor for the property "ArrayDelimiter"
    /**
     * Setter for property ArrayDelimiter.<p>
    * Delimiter used to serialize arrays of values into a String.
     */
    public void setArrayDelimiter(char pArrayDelimiter)
        {
        __m_ArrayDelimiter = pArrayDelimiter;
        }
    
    public void store(java.io.OutputStream outStream, String sHeader)
            throws java.io.IOException
        {
        get_Properties().store(outStream, sHeader);
        }
    
    // Declared at the super level
    public String toString()
        {
        return get_Properties().toString();
        }
    }
