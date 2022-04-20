/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.common.util;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.Attributes;


/**
 * Execute a java application is a sandboxed ClassLoader.
 */
public class ClassLoaderTest
    {
    /**
     * A class loader for loading jar files, both local and remote.
     */
    public static class JarClassLoader extends URLClassLoader
        {
        /**
         * Creates a new JarClassLoader for the specified url.
         *
         * @param aUrl the url of the jar file
         */
        public JarClassLoader(URL[] aUrl) {
            super(aUrl);
        }



        /**
         * Invokes the application in this jar file given the name of the
         * main class and an array of arguments. The class must define a
         * static method "main" which takes an array of String arguemtns
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

    }

    /**
     * Split into multiple command lines based on & delimiter
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
            if (args[i].endsWith("&"))
                {
                args[i] = args[i].substring(0, args[i].length() - 1);

                int cTok = args[i].isEmpty() ? 0 : 1;

                String[] newArgs = new String[i + cTok - ofStart];
                System.arraycopy(args, ofStart, newArgs, 0, i + cTok);
                listArgs.add(newArgs);

                ofStart = i + 1;
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
        if (args.length < 2) {
            usage();
        }

        String[][] aaArgs = splitArgs(args);

        for (final String[] asArgs : aaArgs)
            {
            new Thread()
            {
            public void run()
                {
                List<URL> listJars = new ArrayList<URL>();
                try {
                    StringTokenizer tok = new StringTokenizer(asArgs[0], ":");
                    while (tok.hasMoreElements())
                        {
                        listJars.add(new URL("file:" + tok.nextToken()));
                        }
                } catch (MalformedURLException e) {
                    fatal("Invalid URL: " + asArgs[0]);
                }


                // Create the class loader for the application jar file
                JarClassLoader cl = new JarClassLoader(listJars.toArray(new URL[0]));

                Thread.currentThread().setContextClassLoader(cl);

                // Get the application's main class name
                String name = asArgs[1];

                // Get arguments for the application
                String[] newArgs = new String[asArgs.length - 2];
                System.arraycopy(asArgs, 2, newArgs, 0, newArgs.length);
                // Invoke application's main class
                try
                    {
                    cl.invokeClass(name, newArgs);
                    } catch (ClassNotFoundException e) {
                        fatal("Class not found: " + name);
                    } catch (NoSuchMethodException e) {
                        fatal("Class does not define a 'main' method: " + name);
                    } catch (InvocationTargetException e) {
                        e.getTargetException().printStackTrace();
                    }
                }
            }.start();
         }
    }

    private static void fatal(String s) {
        System.err.println(s);
        System.exit(1);
    }

    private static void usage() {
        fatal("Usage: java ClassLoaderTest classpath class [args..] [& classpath class [args..]]+ ");
    }
    }
