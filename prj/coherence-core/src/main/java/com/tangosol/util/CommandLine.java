/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.run.xml.XmlHelper;
import com.tangosol.run.xml.XmlValue;

import java.io.PrintStream;
import java.io.LineNumberReader;
import java.io.InputStreamReader;

import java.lang.reflect.Method;

import java.text.Collator;

import java.util.Enumeration;
import java.util.Locale;


/**
* A command line base class.
*
* @author cp 2003.03.12
*/
public abstract class CommandLine
        extends Base
        implements Runnable
    {
    // ----- command line support -------------------------------------------

//    /**
//    * Command line entry point. Sub-class must over-ride.
//    *
//    * @param asArg  command line arguments
//    */
//    public static void main(String[] asArg)
//        {
//        new MyCommandLine(asArg).run();
//        }

    // ----- constructors ---------------------------------------------------

    protected CommandLine()
        {
        Collator collator = Collator.getInstance(Locale.ENGLISH);
        collator.setStrength(Collator.PRIMARY);
        m_collatorCmd = new CacheCollator(collator);
        m_tblCmd      = new StringTable(m_collatorCmd);
        m_tblCmdHelp  = new StringTable(m_collatorCmd);
        }

    protected CommandLine(String[] asArg)
        {
        this();
        m_asArg = asArg;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the number of command line arguments.
    */
    public int getArgCount()
        {
        String[] asArg = m_asArg;
        return asArg == null ? 0 : asArg.length;
        }

    /**
    * Get the specified command line argument.
    *
    * @param i  the argument index in the range (0 <= i < ArgCount)
    */
    public String getArg(int i)
        {
        String[] asArg = m_asArg;
        return asArg == null || i < 0 || i >= asArg.length ? null : asArg[i];
        }


    // ----- command line management ----------------------------------------

    /**
    * Main loop for command line input and processing.
    */
    public void run()
        {
        init();
        try
            {
            PrintStream      out = System.out;
            LineNumberReader in  = new LineNumberReader(new InputStreamReader(System.in));

            do
                {
                out();
                out.print("Command: ");
                out.flush();

                String sLine = in.readLine().trim();
                out();

                if (sLine != null && sLine.length() > 0)
                    {
                    String[] asParts = parseDelimitedString(sLine, ' ');
                    String   sCmd    = asParts[0];

                    Method method = (Method) m_tblCmd.get(sCmd);
                    if (method == null)
                        {
                        out("Unknown command \"" + sCmd + "\"; try \"help\".");
                        continue;
                        }

                    Class [] aclz = method.getParameterTypes();
                    Object[] ao   = NO_ARGS;
                    switch (aclz == null ? 0 : aclz.length)
                        {
                        case 1:
                            ao = new Object[] {asParts};
                        case 0:
                            try
                                {
                                method.invoke(this, ao);
                                }
                            catch (Throwable e)
                                {
                                err("An error occurred processing the command:");
                                err(e);
                                }
                            break;

                        default:
                            out("unsupported number of arguments (" + aclz.length + ") for invocation");
                            break;
                        }
                    }
                }
            while (!m_fDone);
            }
        catch (Throwable e)
            {
            err(e);
            }
        finally
            {
            exit();
            }
        }

    /**
    * Register the specified command to call the specified method.
    */
    public void register(String sCmd, String sMethod)
        {
        register(sCmd, sMethod, null);
        }

    /**
    * Register the specified command to call the specified method, and
    * display command and the specified text in the help text.
    */
    public void register(String sCmd, String sMethod, String sHelp)
        {
        Class  clz    = getClass();
        Method method = null;
        try
            {
            method = clz.getMethod(sMethod, VAR_PARAMS);
            }
        catch (Throwable e)
            {
            try
                {
                method = clz.getMethod(sMethod, NO_PARAMS);
                }
            catch (Throwable e2) {}
            }

        if (method == null)
            {
            err("unable to locate method \"" + sMethod + "\"; command \""
                    + sCmd + "\" will be unavailable.");
            }
        else
            {
            m_tblCmd.put(sCmd, method);
            }

        if (sHelp != null)
            {
            m_tblCmdHelp.put(sCmd, sHelp);
            }
        }

    /**
    * Called when starting. This method should register all possible
    * commands.
    */
    public void init()
        {
        register("bye" , "doQuit");
        register("exit", "doQuit");
        register("quit", "doQuit", "Exits the command-line tool");
        register("q"   , "doQuit");

        register("help", "doHelp", "Displays a list of possible commands");
        register("?"   , "doHelp");
        }

    /**
    * Called when exiting.
    */
    public void exit()
        {
        }


    // ---- command processing ----------------------------------------------

    /**
    * Process the "help" command.
    */
    public void doHelp()
        {
        out("Possible commands:");
        for (Enumeration enmr = m_tblCmdHelp.keys(); enmr.hasMoreElements(); )
            {
            String sCmd  = (String) enmr.nextElement();
            String sHelp = (String) m_tblCmdHelp.get(sCmd);

            if (sCmd.length() < 10)
                {
                sCmd = (sCmd + "          ").substring(0, 10);
                }

            out(sCmd + " - " + sHelp);
            }
        }

    /**
    * Process the "quit" command.
    */
    public void doQuit()
        {
        m_fDone = true;
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Convert the passed String to an boolean.
    *
    * @param s  the String to convert
    *
    * @return the boolean value from the String
    */
    public static boolean toBoolean(String s)
        {
        return toBoolean(s, false);
        }

    /**
    * Convert the passed String to an boolean.
    *
    * @param s  the String to convert
    * @param f  the boolean value to return if the String cannot be parsed
    *
    * @return the boolean value from the String, otherwise the default
    */
    public static boolean toBoolean(String s, boolean f)
        {
        if (s == null)
            {
            return f;
            }

        Boolean B = (Boolean) XmlHelper.convert(s, XmlValue.TYPE_BOOLEAN);
        return B == null ? f : B.booleanValue();
        }

    /**
    * Convert the passed String to an int.
    *
    * @param s  the String to convert
    *
    * @return the int value from the String
    */
    public static int toInt(String s)
        {
        return toInt(s, 0);
        }

    /**
    * Convert the passed String to an int.
    *
    * @param s  the String to convert
    * @param n  the int value to return if the String cannot be parsed
    *
    * @return the int value from the String, otherwise the default
    */
    public static int toInt(String s, int n)
        {
        if (s == null)
            {
            return n;
            }

        Integer I = (Integer) XmlHelper.convert(s, XmlValue.TYPE_INT);
        return I == null ? n : I.intValue();
        }

    /**
    * Convert the passed String to an long.
    *
    * @param s  the String to convert
    *
    * @return the long value from the String
    */
    public static long toLong(String s)
        {
        return toLong(s, 0);
        }

    /**
    * Convert the passed String to an long.
    *
    * @param s  the String to convert
    * @param n  the long value to return if the String cannot be parsed
    *
    * @return the long value from the String, otherwise the default
    */
    public static long toLong(String s, long n)
        {
        if (s == null)
            {
            return n;
            }

        Long L = (Long) XmlHelper.convert(s, XmlValue.TYPE_LONG);
        return L == null ? n : L.longValue();
        }

    /**
    * Convert the passed String to an double.
    *
    * @param s  the String to convert
    *
    * @return the double value from the String
    */
    public static double toDouble(String s)
        {
        return toDouble(s, 0);
        }

    /**
    * Convert the passed String to an double.
    *
    * @param s    the String to convert
    * @param dfl  the double value to return if the String cannot be parsed
    *
    * @return the double value from the String, otherwise the default
    */
    public static double toDouble(String s, double dfl)
        {
        if (s == null)
            {
            return dfl;
            }

        Double D = (Double) XmlHelper.convert(s, XmlValue.TYPE_DOUBLE);
        return D == null ? dfl : D.doubleValue();
        }


    // ----- data members ---------------------------------------------------

    protected static final Class[]  NO_PARAMS  = new Class[0];
    protected static final Class[]  VAR_PARAMS = new Class[] {String[].class};
    protected static final Object[] NO_ARGS    = new Object[0];


    // ----- data members ---------------------------------------------------

    /**
    * Case-insensitive collator based on the English character set.
    */
    protected Collator m_collatorCmd;

    /**
    * Command line arguments.
    */
    protected String[] m_asArg;

    /**
    * False while running; set to true to allow normal exit.
    */
    protected boolean  m_fDone = false;

    /**
    * Map of commands to their methods.
    */
    protected StringTable m_tblCmd;

    /**
    * Map of commands to display help for.
    */
    protected StringTable m_tblCmdHelp;
    }
