/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


/**
 * Execute a java application is a sandboxed ClassLoader.
 *
 * @author mf  2011.07.05
 */
public class ApplicationLoader
    extends URLClassLoader
    implements Runnable
    {
    /**
     * Creates a new ApplicationLoader for the specified url.
     *
     * @param aUrl    the url of the jar file
     * @param asArgs  the program arguments
     */
    public ApplicationLoader(URL[] aUrl, String[] asArgs)
        {
        super(aUrl, SHARED_ROOT ? ClassLoader.getSystemClassLoader() : ClassLoader.getSystemClassLoader().getParent());
        f_asArgs = asArgs;
        }

    /**
     * Invokes the application in this jar file given the name of the
     * main class and an array of arguments. The class must define a
     * static method "main" which takes an array of String arguements
     * and is of return type "void".
     *
     * @param name the name of the main class
     * @param args the arguments for the application
     * @exception ClassNotFoundException if the specified class could not
     *            be found
     * @exception NoSuchMethodException if the specified class does not
     *            contain a "main" method
     * @exception InvocationTargetException if the application raised an
     *            exception
     */
    public void invokeClass(String name, String[] args)
        throws ClassNotFoundException,
               NoSuchMethodException,
            InvocationTargetException
        {
        Class c = loadClass(name);
        Method m = c.getMethod("main", new Class[] { args.getClass() });
        m.setAccessible(true);
        int mods = m.getModifiers();
        if (m.getReturnType() != void.class || !Modifier.isStatic(mods) ||
            !Modifier.isPublic(mods)) {
            throw new NoSuchMethodException("main");
        }
        try {
            m.invoke(null, new Object[] { args });
        } catch (IllegalAccessException e) {
            // This should not happen, as we have disabled access checks
        }
    }

    public void run()
        {
        Thread.currentThread().setContextClassLoader(this);

        // Get the application's main class name
        String name = f_asArgs[1];

        // Get arguments for the application
        String[] newArgs = new String[f_asArgs.length - 2];
        System.arraycopy(f_asArgs, 2, newArgs, 0, newArgs.length);
        // Invoke application's main class
        try
            {
            invokeClass(name, newArgs);
            } catch (ClassNotFoundException e) {
                fatal("Class not found: " + name);
            } catch (NoSuchMethodException e) {
                fatal("Class does not define a 'main' method: " + name);
            } catch (InvocationTargetException e) {
                e.getTargetException().printStackTrace();
            }
        }


    /**
     * Split into multiple command lines based on &amp; and ; delimiters
     *
     * @param args the line to split
     *
     * @return the split lines
     */
    public static String[][] splitArgs(String[] args)
        {
        ArrayList<String[]> listArgs = new ArrayList<String[]>();

        int ofStart = 0;
        for (int i = 0, c = args.length; i < c; ++i)
            {
            if (args[i].contains("&") || args[i].contains(";"))
                {
                int    ofAmp   = args[i].indexOf('&');
                int    ofSemi  = args[i].indexOf(';');
                int    ofTerm  = ofAmp == -1 ? ofSemi : ofAmp;
                String sSuffix = args[i].substring(ofTerm + 1);

                args[i] = args[i].substring(0, ofTerm + (ofAmp == -1 ? 0 : 1)); // leave &, but strip ;

                int cTok = args[i].isEmpty() ? 0 : 1;

                String[] newArgs = new String[i + cTok - ofStart];
                System.arraycopy(args, ofStart, newArgs, 0, i + cTok);
                listArgs.add(newArgs);

                ofStart = i + 1;

                if (!sSuffix.isEmpty())
                    {
                    int cReps = Integer.parseInt(sSuffix);
                    for (int j = 1; j < cReps; ++j)
                        {
                        listArgs.add(newArgs.clone());
                        }
                    }
                }
            }

        if (ofStart < args.length)
            {
            String[] newArgs = new String[args.length - ofStart];
            System.arraycopy(args, ofStart, newArgs, 0, newArgs.length);
            listArgs.add(newArgs);
            }

        return listArgs.toArray(new String[listArgs.size()][]);
        }

    public static void main(String[] args)
        {
        if (args.length < 2)
            {
            usage();
            }

        String[][] aaArgs = splitArgs(args);

        int cMain = 0;
        for (final String[] asArgs : aaArgs)
            {
            List<URL> listJars = new ArrayList<URL>();
            try
                {
                StringTokenizer tok = new StringTokenizer(asArgs[0], ":");
                while (tok.hasMoreElements())
                    {
                    listJars.add(new URL("file:" + tok.nextToken()));
                    }
                }
            catch (MalformedURLException e)
                {
                fatal("Invalid URL: " + asArgs[0]);
                }

            String sLast = asArgs[asArgs.length - 1];
            if (sLast.endsWith("&"))
                {
                asArgs[asArgs.length - 1] = sLast.substring(0, sLast.length() -1);
                new Thread(new ApplicationLoader(listJars.toArray(new URL[0]), asArgs), "main-" + ++cMain).start();
                }
            else
                {
                new ApplicationLoader(listJars.toArray(new URL[0]), asArgs).run();
                }
            }
        }

    private static void fatal(String s)
        {
        System.err.println(s);
        System.exit(1);
        }

    private static void usage()
        {
        fatal("Usage: java " + ApplicationLoader.class.getName() + " [classpath class [args..] [&[N]]]+ ");
        }

    protected final String[] f_asArgs;

    /**
     * Indicates if the launched applications should share the system classpath.
     */
    private static final boolean SHARED_ROOT = Boolean.getBoolean(ApplicationLoader.class.getName() + ".sharedRoot");
    }
