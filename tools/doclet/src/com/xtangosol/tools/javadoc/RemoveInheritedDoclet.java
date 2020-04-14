/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.xtangosol.tools.javadoc;


import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.Doclet;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.MethodDoc;

import com.sun.tools.doclets.formats.html.HtmlDoclet;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;

import com.sun.tools.javadoc.ClassDocImpl;
import com.sun.tools.javadoc.FieldDocImpl;
import com.sun.tools.javadoc.ProgramElementDocImpl;

import java.lang.reflect.Field;


/**
* JavaDoc Doclet that removes inherited JavaDoc from a specified class.
*
* @author dr  2006.01.25
* @author jh  2006.01.25
*/
public class RemoveInheritedDoclet extends Doclet
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public RemoveInheritedDoclet()
        {
        }


    // ----- Doclet methods -------------------------------------------------

    /**
    * Check for Doclet-added options.
    *
    * @param sOption  the option passed to the Doclet
    *
    * @return the number of option arguments
    */
    public static int optionLength(String sOption)
        {
        int cOption = HtmlDoclet.optionLength(sOption);
        if (cOption == 0 && sOption != null)
            {
            cOption = sOption.equalsIgnoreCase(OPTION_IGNORE_INHERITED) ? 2 : 0;
            }

        return cOption;
        }

    /**
    * Validate supplied options.
    *
    * @param asOptions  the array of options passed to the Doclet
    * @param reporter  a DocErrorReported that can be used to report errors
    *
    * @return true if the supplied options are valid
    */
    public static boolean validOptions(String[][] asOptions, DocErrorReporter reporter)
        {
        s_reporter = reporter;

        boolean fValid = HtmlDoclet.validOptions(asOptions, reporter);
        for (int i = 0, c = asOptions == null ? 0 : asOptions.length; i < c; ++i)
            {
            String[] asOption = asOptions[i];
            int      cArgs    = asOption == null ? 0 : asOption.length;

            if (cArgs > 0)
                {
                if (asOption[0].equalsIgnoreCase(OPTION_IGNORE_INHERITED))
                    {
                    if (cArgs == 2)
                        {
                        s_asClzIgnored = asOption[1].split(",");
                        fValid &= true;
                        }
                    else
                        {
                        err("Incorrect number of arguments for the "
                                + OPTION_IGNORE_INHERITED
                                + " JavaDoc option; expected 2 arguments, received "
                                + cArgs
                                + " arguments.");
                        fValid = false;
                        }

                    break;
                    }
                }
            }

        return fValid;
        }

    /**
    * Generate JavaDoc.
    *
    * @param rootDoc  the Doc for the current run of the JavaDoc tool
    *
    * @return true on success; false otherwise
    */
    public static boolean start(RootDoc rootDoc)
        {
        String[] asClzIgnored = s_asClzIgnored;
        for (int i = 0, ci = asClzIgnored == null ? 0 : asClzIgnored.length; i < ci; ++ i)
            {
            ClassDoc doc = rootDoc.classNamed(asClzIgnored[i]);
            if (doc != null)
                {
                Field fieldSym  = s_fieldSym;
                Field fieldTsym = s_fieldTsym;

                try
                    {
                    // ignore inherited fields
                    FieldDocImpl[] aFields = (FieldDocImpl[]) doc.fields();
                    for (int j = 0, cj = aFields == null ? 0 : aFields.length; j < cj; ++j)
                        {
                        FieldDocImpl field = aFields[j];
                        if (field != null)
                            {
                            Symbol.VarSymbol sym = (Symbol.VarSymbol) fieldSym.get(field);
                            if (sym != null)
                                {
                                sym.flags_field = Flags.SYNTHETIC;
                                }
                            }
                        }

                    // ignore inherited methods
                    MethodDoc[] aMethod = doc.methods();
                    for (int j = 0, cj = aMethod == null ? 0: aMethod.length; j < cj; ++j)
                        {
                        MethodDoc method = aMethod[j];
                        if (method != null)
                            {
                            Symbol.MethodSymbol sym = (Symbol.MethodSymbol) fieldSym.get(method);
                            if (sym != null)
                                {
                                sym.flags_field = Flags.SYNTHETIC;
                                }
                            }
                        }

                    // ignore inherited inner classes
                    ClassDoc[] asDoc = doc.innerClasses();
                    for (int j = 0, cj = asDoc == null ? 0 : asDoc.length; j < cj; ++j)
                        {
                        ClassDoc docInner = asDoc[j];
                        if (docInner != null)
                            {
                            Symbol.ClassSymbol sym = (Symbol.ClassSymbol) fieldTsym.get(docInner);
                            if (sym != null)
                                {
                                sym.flags_field = Flags.SYNTHETIC;
                                }
                            }
                        }
                    }
                catch (Throwable t)
                    {
                    err("Error running RemoveInheritedDoclet: " + t);
                    return false;
                    }
                }
            }

        return HtmlDoclet.start(rootDoc);
        }

    /**
    * Indicate that this doclet supports the 1.5 language features.
    *
    * @return JAVA_1_5, indicating that the new features are supported
    */
    public static LanguageVersion languageVersion()
        {
        return HtmlDoclet.languageVersion();
        }


    // ----- internal helpers -----------------------------------------------

    /**
    * Report an error.
    *
    * @param sMsg  the error message
    */
    protected static void err(String sMsg)
        {
        DocErrorReporter reporter = s_reporter;
        if (reporter == null)
            {
            System.err.println("RemoveInheritedDoclet [ERROR]: " + sMsg);
            }
        else
            {
            reporter.printError(sMsg);
            }
        }


    // ----- constants ------------------------------------------------------

    /**
    * The name of the JavaDoc option used to specify the name of the class
    * for which inherited JavaDoc shouldn't be generated.
    */
    public static final String OPTION_IGNORE_INHERITED = "-ignoreinherited";


    // ----- data members ---------------------------------------------------

    /**
    * The name of classes for which inherited JavaDoc shouldn't be generated.
    */
    protected static String[] s_asClzIgnored;

    /**
    * The DocErrorReporter used to report error and warning messages.
    */
    protected static DocErrorReporter s_reporter;

    /**
    * The "sym" field of the <tt>com.sun.tools.javadoc.ProgramElementDocImpl</tt>
    * class.
    */
    protected static Field s_fieldSym;

    /**
    * The "tsym" field of the <tt>com.sun.tools.javadoc.ClassDocImpl</tt>
    * class.
    */
    protected static Field s_fieldTsym;

    static
        {
        try
            {
            Field field;

            field = s_fieldSym = ProgramElementDocImpl.class.getDeclaredField("sym");
            field.setAccessible(true);

            field = s_fieldTsym = ClassDocImpl.class.getDeclaredField("tsym");
            field.setAccessible(true);
            }
        catch (Throwable t)
            {
            throw new RuntimeException("Error initializing RemoveInheritedDoclet: ", t);
            }
        }
    }
