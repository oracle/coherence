/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler;


import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.StringTable;

import java.util.Enumeration;
import java.util.Locale;


/**
* This class provides standardized compiler locator and instantiator
* services.
*
* @version 1.00, 11/16/98
* @author  Cameron Purdy
*/
public abstract class Locator
        extends Base
    {
    /**
    * Determine the available scripting languages.
    *
    * @return an enumerator of language names
    */
    public static Enumeration languages()
        {
        return m_tblLanguages.keys();
        }

    /**
    * Specify a default scripting language.
    *
    * @param sLanguage  the name of the default scripting language
    */
    public static void setDefaultLanguage(String sLanguage)
        {
        if (m_sDefault == null)
            {
            throw new IllegalArgumentException();
            }

        if (!sLanguage.equals(m_sDefault) && m_tblLanguages.contains(sLanguage))
            {
            m_sDefault = sLanguage;
            }
        }

    /**
    * Get the default scripting language.
    *
    * @return  the name of the default scripting language
    */
    public static String getDefaultLanguage()
        {
        return m_sDefault;
        }

    /**
    * Register a scripting language, overwriting any registered language of
    * the same name.
    *
    * @param sLanguage     the name of the language
    * @param clzScript     the script class for the language
    * @param clzTokenizer  the tokenizer class for the language
    * @param clzColorizer  the colorizer class for the language
    * @param clzCompiler   the compiler class for the language
    */
    public static void registerLanguage(String sLanguage, Class clzScript, Class clzTokenizer, Class clzColorizer, Class clzCompiler)
        {
        // check language
        if (sLanguage == null || sLanguage.length() < 1)
            {
            throw new IllegalArgumentException();
            }

        // check classes
        if (clzScript == null || clzTokenizer == null || clzColorizer == null || clzCompiler == null)
            {
            throw new IllegalArgumentException();
            }

        // register the language
        m_tblLanguages.put(sLanguage, new LanguageInfo(sLanguage,
                clzScript, clzTokenizer, clzColorizer, clzCompiler));

        // if no language is the default language, use this one
        if (m_sDefault == null)
            {
            m_sDefault = sLanguage;
            }
        }

    /**
    * Register a scripting language.  This method is intended to be used by
    * a process which loads language information from a persistent stored,
    * for example an initialization file.
    *
    * @param sLanguage  the language name
    * @param sPackage   a package (or relative package) name
    *
    * @return false if registration fails to locate the specified language
    *         implementation
    *
    * @see com.tangosol.util.ClassHelper#getCompositePackage
    */
    public static boolean registerLanguage(String sLanguage, String sPackage)
        {
        // check language
        if (sLanguage == null || sLanguage.length() < 1)
            {
            return false;
            }

        // check package
        if (sPackage == null || sPackage.length() < 1)
            {
            return false;
            }

        try
            {
            // load classes using default names constructed from the
            // interface names and the specified package
            Class clzScript    = Class.forName(ClassHelper.getCompositeName(SCRIPT   , sPackage));
            Class clzTokenizer = Class.forName(ClassHelper.getCompositeName(TOKENIZER, sPackage));
            Class clzColorizer = Class.forName(ClassHelper.getCompositeName(COLORIZER, sPackage));
            Class clzCompiler  = Class.forName(ClassHelper.getCompositeName(COMPILER , sPackage));

            registerLanguage(sLanguage, clzScript, clzTokenizer, clzColorizer, clzCompiler);
            return true;
            }
        catch (Throwable t)
            {
            out(t);
            return false;
            }
        }

    /**
    * Attempt to register a language which is "included with the system".
    * For example, the "Java" language would map to the package ".java.",
    * which is resolved to "com.tangosol.dev.compiler.java.", which
    * contains an implementation of Script, Tokenizer, Colorizer, and
    * Compiler.
    *
    * @param sLanguage the language name
    *
    * @return true if the language was registered
    */
    private static boolean registerSystemLanguage(String sLanguage)
        {
        // package is based on the language name
        // verify that the package is a simple lower-case name
        String sPackage = sLanguage.toLowerCase(Locale.ENGLISH);
        char[] achTest = sPackage.toCharArray();
        int    cchTest = achTest.length;
        for (int of = 0; of < cchTest; ++of)
            {
            if (achTest[of] < 'a' || achTest[of] > 'z')
                {
                return false;
                }
            }

        // package is relative to the base compiler package
        return registerLanguage(sLanguage, "." + sPackage + ".");
        }

    /**
    * Unregister a scripting language.
    *
    * @param sLanguage  the language name
    */
    public static void unregisterLanguage(String sLanguage)
        {
        // check language
        if (sLanguage == null || sLanguage.length() < 1)
            {
            throw new IllegalArgumentException();
            }

        // check if the default language is being unregistered
        if (sLanguage.equals(m_sDefault))
            {
            m_sDefault = null;
            }

        // discard the language info
        m_tblLanguages.remove(sLanguage);
        }

    /**
    * For a registered language, return the language information.
    *
    * @param sLanguage  the scripting language name
    *
    * @return the associated LanguageInfo object
    *
    * @exception IllegalArgumentException  if the language is not registered
    */
    private static LanguageInfo getLanguageInfo(String sLanguage)
        {
        if (!m_tblLanguages.contains(sLanguage))
            {
            registerSystemLanguage(sLanguage);
            }

        LanguageInfo info = (LanguageInfo) m_tblLanguages.get(sLanguage);
        if (info == null)
            {
            throw new IllegalArgumentException("Unsupported language: \"" + sLanguage + '"');
            }

        return info;
        }

    /**
    * For a specified language, instantiate a script.
    *
    * @param sLanguage  the scripting language name
    *
    * @return an uninitialized script object for the specified language
    *
    * @exception IllegalArgumentException       if the language is not registered
    * @exception UnsupportedOperationException  if the object cannot be instantiated
    */
    public static Script getScript(String sLanguage)
        {
        try
            {
            return (Script) getLanguageInfo(sLanguage).clzScript.newInstance();
            }
        catch (InstantiationException e)
            {
            throw new UnsupportedOperationException();
            }
        catch (IllegalAccessException e)
            {
            throw new UnsupportedOperationException();
            }
        }

    /**
    * For a specified language, instantiate a tokenizer.
    *
    * @param sLanguage  the scripting language name
    *
    * @return an uninitialized tokenizer for the specified language
    *
    * @exception IllegalArgumentException       if the language is not registered
    * @exception UnsupportedOperationException  if the object cannot be instantiated
    */
    public static Tokenizer getTokenizer(String sLanguage)
        {
        try
            {
            return (Tokenizer) getLanguageInfo(sLanguage).clzTokenizer.newInstance();
            }
        catch (InstantiationException e)
            {
            throw new UnsupportedOperationException();
            }
        catch (IllegalAccessException e)
            {
            throw new UnsupportedOperationException();
            }
        }

    /**
    * For a specified language, instantiate a colorizer.
    *
    * @param sLanguage  the scripting language name
    *
    * @return an uninitialized colorizer for the specified language
    *
    * @exception IllegalArgumentException       if the language is not registered
    * @exception UnsupportedOperationException  if the object cannot be instantiated
    */
    public static Colorizer getColorizer(String sLanguage)
        {
        try
            {
            return (Colorizer) getLanguageInfo(sLanguage).clzColorizer.newInstance();
            }
        catch (InstantiationException e)
            {
            throw new UnsupportedOperationException();
            }
        catch (IllegalAccessException e)
            {
            throw new UnsupportedOperationException();
            }
        }

    /**
    * For a specified language, instantiate a compiler.
    *
    * @param sLanguage  the scripting language name
    *
    * @return an uninitialized compiler for the specified language
    *
    * @exception IllegalArgumentException       if the language is not registered
    * @exception UnsupportedOperationException  if the object cannot be instantiated
    */
    public static Compiler getCompiler(String sLanguage)
        {
        try
            {
            return (Compiler) getLanguageInfo(sLanguage).clzCompiler.newInstance();
            }
        catch (InstantiationException e)
            {
            throw new UnsupportedOperationException();
            }
        catch (IllegalAccessException e)
            {
            throw new UnsupportedOperationException();
            }
        }


    // ----- inner classes --------------------------------------------------

    /**
    * Stores information associated with a language.
    */
    static class LanguageInfo
        {
        /**
        * Constructor.
        */
        LanguageInfo(String sLanguage, Class clzScript, Class clzTokenizer, Class clzColorizer, Class clzCompiler)
            {
            this.sLanguage    = sLanguage;
            this.clzScript    = clzScript;
            this.clzTokenizer = clzTokenizer;
            this.clzColorizer = clzColorizer;
            this.clzCompiler  = clzCompiler;
            }

        String sLanguage;
        Class  clzScript;
        Class  clzTokenizer;
        Class  clzColorizer;
        Class  clzCompiler;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The script interface.
    */
    private static final Class SCRIPT = Script.class;

    /**
    * The tokenizer interface.
    */
    private static final Class TOKENIZER = Tokenizer.class;

    /**
    * The colorizer interface.
    */
    private static final Class COLORIZER = Colorizer.class;

    /**
    * The compiler interface.
    */
    private static final Class COMPILER = Compiler.class;

    /**
    * Table of LanguageInfo keyed by language name.
    */
    private static StringTable m_tblLanguages = new StringTable();

    /**
    * Default language name.
    */
    private static String m_sDefault;
    }
