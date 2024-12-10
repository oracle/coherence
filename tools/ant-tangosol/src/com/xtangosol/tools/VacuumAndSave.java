/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.xtangosol.tools;


import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;


/**
* A utility which downloads and saves everything from a web site.
*/
public class VacuumAndSave
    {
    // ----- helper methods -------------------------------------------------

    /**
    * Print a blank line.
    */
    public static void out()
        {
        System.out.println();
        }

    /**
    * Print the passed String value.
    *
    * @param s  the String to print.
    */
    public static void out(String s)
        {
        System.out.println(s);
        }

    /**
    * Print the passed exception information.
    *
    * @param e  the Throwable object to print.
    */
    public static void out(Throwable e)
        {
        e.printStackTrace(System.out);
        }

    /**
    * Replace all occurences of the specified substring in the specified string
    *
    * @param sText  string to change
    * @param sFrom  pattern to change from
    * @param sTo    pattern to change to
    *
    * @return   modified string
    */
    public static String replace(String sText, String sFrom, String sTo)
        {
        StringBuffer sbTextNew = new StringBuffer();
        int iTextLen = sText.length();
        int iStart   = 0;

        while (iStart < iTextLen)
            {
            int iPos = sText.indexOf(sFrom, iStart);
            if (iPos != -1)
                {
                sbTextNew.append(sText.substring(iStart, iPos));
                sbTextNew.append(sTo);
                iStart = iPos + sFrom.length();
                }
            else
                {
                sbTextNew.append(sText.substring(iStart));
                break;
                }
            }

        return sbTextNew.toString();
        }

    /**
    * Return true if the given integers are equal. If both integers are
    * letters (between 'a' and 'z' or 'A' and 'Z', inclusive), the result of
    * a case-insensitive comparision is returned.
    *
    * @param n   the first integer to compare
    * @param n2  the second integer to compare
    *
    * @return true if the given integers match
    */
    public static boolean match(int n, int n2)
        {
        if (n == n2)
            {
            return true;
            }

        if (n2 >= 'A' && n2 <= 'Z')
            {
            return n == (n2 + 'a' - 'A');
            }

        if (n2 >= 'a' && n2 <= 'z')
            {
            return n == (n2 + 'A' - 'a');
            }

        return false;
        }

    /**
    * Read and return all data from the given stream.
    *
    * @param stream  the InputStream to read from
    *
    * @return the content read from the stream
    *
    * @throws Throwable on error
    */
    public static byte[] readAll(InputStream stream)
            throws Throwable
        {
        ThreadLocal tloBuf = s_tloBuf;
        try
            {
            boolean fMod = false;
            byte[] abBuf = (byte[]) tloBuf.get();
            int    cbBuf;
            if (abBuf == null)
                {
                cbBuf = BUFFER_SIZE;
                abBuf = new byte[cbBuf];
                fMod  = true;
                }
            else
                {
                cbBuf = abBuf.length;
                }

            int cbTotal = 0;
            while (true)
                {
                int cbBlock = stream.read(abBuf, cbTotal, cbBuf - cbTotal);
                if (cbBlock < 0)
                    {
                    break;
                    }

                cbTotal += cbBlock;
                if (cbTotal == cbBuf)
                    {
                    int    cbNew = cbBuf * 2;
                    byte[] abNew = new byte[cbNew];
                    System.arraycopy(abBuf, 0, abNew, 0, cbBuf);
                    abBuf = abNew;
                    cbBuf = cbNew;
                    fMod  = true;
                    }
                }

            byte[] abResult = new byte[cbTotal];
            System.arraycopy(abBuf, 0, abResult, 0, cbTotal);

            if (fMod)
                {
                tloBuf.set(abBuf);
                }

            return abResult;
            }
        finally
            {
            try
                {
                stream.close();
                }
            catch (Throwable t)
                {
                out();
                out("Caught \"" + t + "\" while closing InputStream.");
                }
            }
        }

    /**
    * Write the given data to the file with the specified name.
    *
    * @param sFile  the name of the file to write to
    * @param ab     the data to write
    *
    * @throws Throwable on error
    */
    public static void write(String sFile, byte[] ab)
            throws Throwable
        {
        // ensure unique name
        Set    setNames = s_setNames;
        String sOrig    = sFile;
        synchronized (setNames)
            {
            if (setNames.contains(sFile))
                {
                int i = 1;
                do
                    {
                    int of = sOrig.lastIndexOf('.');
                    sFile  = sOrig.substring(0, of) + "_" + ++i + sOrig.substring(of);
                    }
                while (setNames.contains(sFile));
                }
            setNames.add(sFile);
            }

        // write bytes
        File file = new File(sFile);
        FileOutputStream stream = new FileOutputStream(file);
        try
            {
            stream.write(ab);
            }
        finally
            {
            try
                {
                stream.close();
                }
            catch (Throwable t)
                {
                out();
                out("Caught \"" + t + "\" while closing OutputStream.");
                }
            }
        }

    /**
    * Add the given URL to the queue of URLs to process.
    *
    * @param url  the URL of the site to visit
    *
    * @throws Throwable on error
    */
    public static void queue(URL url)
            throws Throwable
        {
        // no repeat visits
        Set setVisited = s_setVisited;
        synchronized (setVisited)
            {
            if (setVisited.contains(url))
                {
                return;
                }
            setVisited.add(url);
            out("Queued: " + url);
            }

        // queue the URL
        List listURLs = s_listURLs;
        synchronized (listURLs)
            {
            listURLs.add(url);
            }

        // see if a thread is waiting
        List listThreads = s_listThreads;
        synchronized (listThreads)
            {
            if (!listThreads.isEmpty())
                {
                VacuumThread thread = (VacuumThread) listThreads.remove(0);
                synchronized (thread)
                    {
                    thread.notify();
                    }
                }
            }
        }

    /**
    * Download and save all the content from the given URL.
    *
    * @param url  the target URL
    *
    * @throws Throwable on error
    */
    public static void vacuum(URL url)
            throws Throwable
        {
        URLConnection con = url.openConnection();

        String sCookie = s_sCookie;
        if (sCookie != null)
            {
            con.setRequestProperty("Cookie", sCookie);
            }

        String      sMime   = con.getContentType();
        InputStream stream  = con.getInputStream();
        byte[]      ab      = readAll(stream);
        int         cb      = ab.length;
        String      sFilter = s_sFilter;

        if (sMime.startsWith("text/htm") || sMime.startsWith("text/xml"))
            {
            // parse out all "<A ", "<IMG ", and "<LINK " tags
            for (int i = 0; i < cb; ++i)
                {
                if (ab[i] == '<')
                    {
                    try
                        {
                        switch ((char) ab[++i])
                            {
                            case 'L':
                            case 'l':
                                {
                                if (!match(ab[++i], 'i') ||
                                    !match(ab[++i], 'n') ||
                                    !match(ab[++i], 'k'))
                                    {
                                    break;
                                    }
                                }

                            case 'A':
                            case 'a':
                                {
                                // verify space
                                if (ab[++i] != ' ')
                                    {
                                    break;
                                    }

                                // search for " HREF=", break on '>'
                                byte b = ab[i];
                                boolean fSpace = false;
                                while (b != '>')
                                    {
                                    if (b == ' ')
                                        {
                                        fSpace = true;
                                        }
                                    else
                                        {
                                        if (fSpace && match(b      , 'h')
                                                   && match(ab[++i], 'r')
                                                   && match(ab[++i], 'e')
                                                   && match(ab[++i], 'f')
                                                   && match(ab[++i], '=')
                                                   && match(ab[++i], '"'))
                                            {
                                            StringBuffer sb = new StringBuffer();

                                            // extract the new URL
                                            final int iStart = i;
                                            while (ab[++i] != '"')
                                                {
                                                sb.append((char) ab[i]);
                                                }
                                            final int iEnd = i;

                                            String s = sb.toString();

                                            // <begin> strip off session tags from .jsp generation
                                            int ofSession = s.indexOf(";j");
                                            if (ofSession >= 0)
                                                {
                                                int iEndNew = i - s.length() + ofSession;
                                                int iEndOld = i;

                                                int ofPound = s.indexOf('#');
                                                if (ofPound >= 0)
                                                    {
                                                    iEndOld -= (s.length() - ofPound);
                                                    }

                                                System.arraycopy(ab, iEndOld, ab, iEndNew, cb - iEndOld);
                                                int cbDif = (iEndOld - iEndNew);
                                                i  = i - cbDif + 1;
                                                cb = cb - cbDif;
                                                }
                                            // <end>

                                            try
                                                {
                                                int ofPound = s.indexOf('#');
                                                if (ofPound >= 0)
                                                    {
                                                    s = s.substring(0, ofPound);
                                                    }
                                                if (s.length() > 0 && (sFilter == null || s.matches(sFilter)))
                                                    {
                                                    URL urlNext = new URL(url, s);
                                                    if (url.getProtocol().startsWith("http")
                                                            && url.getHost().equalsIgnoreCase(urlNext.getHost())
                                                            && url.toString().indexOf('#') < 0)
                                                        {
                                                        queue(urlNext);

                                                        // replace '?' with '+'
                                                        for (int j = iStart + 1; j < iEnd; ++j)
                                                            {
                                                            if (ab[j] == '?')
                                                                {
                                                                ab[j] = '+';
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            catch (Exception e)
                                                {
                                                }
                                            }

                                        fSpace = false;
                                        }

                                    b = ab[++i];
                                    }
                                }
                                break;

                            case 'I':
                            case 'i':
                                {
                                // verify "img "
                                if (!match(ab[++i], 'm') ||
                                    !match(ab[++i], 'g') ||
                                    !match(ab[++i], ' '))
                                    {
                                    break;
                                    }

                                // search for " SRC=", break on '>'
                                byte b = ab[i];
                                boolean fSpace = false;
                                while (b != '>')
                                    {
                                    if (b == ' ')
                                        {
                                        fSpace = true;
                                        }
                                    else
                                        {
                                        if (fSpace && match(b      , 's')
                                                   && match(ab[++i], 'r')
                                                   && match(ab[++i], 'c')
                                                   && match(ab[++i], '=')
                                                   && match(ab[++i], '"'))
                                            {
                                            StringBuffer sb = new StringBuffer();

                                            // extract the new URL
                                            final int iStart = i;
                                            while (ab[++i] != '"')
                                                {
                                                sb.append((char) ab[i]);
                                                }
                                            final int iEnd = i;

                                            String s = sb.toString();

                                            if (s.length() > 0 && (sFilter == null || s.matches(sFilter)))
                                                {
                                                URL urlNext = new URL(url, s);
                                                if (url.getProtocol().startsWith("http")
                                                        && url.getHost().equalsIgnoreCase(urlNext.getHost()))
                                                    {
                                                    queue(urlNext);

                                                    // replace '?' with '+'
                                                    for (int j = iStart + 1; j < iEnd; ++j)
                                                        {
                                                        if (ab[j] == '?')
                                                            {
                                                            ab[j] = '+';
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                        fSpace = false;
                                        }

                                    b = ab[++i];
                                    }
                                }
                                break;
                            }
                        }
                    catch (ArrayIndexOutOfBoundsException e)
                        {
                        out();
                        out("Caught \"" + e + "\" while parsing HTML.");
                        return;
                        }
                    }
                }
            }

        // <begin> save urls to files
        // convert "http://www.blah.com/./file.html" to
        //         "www.blah.com/file.html"
        String newURL = url.toString();
        if (newURL.indexOf(";") >= 0)
            {
            newURL = newURL.substring(0, newURL.indexOf(";"));
            }
        if (newURL.indexOf("#") >= 0)
            {
            newURL = newURL.substring(0, newURL.indexOf("#"));
            }
        String      sep   = "://";
        int         index = newURL.indexOf(sep);
        if (index >= 0)
            {
            newURL = newURL.substring(index + sep.length());
            }
        newURL = replace(newURL, "/./", "/");

        index = newURL.indexOf("/");
        if (index >= 0 && index + 1 < newURL.length())
            {
            String newFolder;
            String sDir = s_sDir;
            if (sDir == null)
                {
                newFolder = newURL.substring(0, index);

                // <begin> strip off port numbers
                if (newFolder.indexOf(":") >= 0)
                    {
                    newFolder = newFolder.substring(0, newFolder.indexOf(":"));
                    }
                // <end>
                }
            else
                {
                newFolder = sDir;
                }

            String newFile = newURL.substring(index + 1).replace('?', '+');
            newFile = replace(newFile, "/", File.separator);

            String fullpath = newFolder + File.separator + newFile;
            String fulldir = fullpath.substring(0, fullpath.lastIndexOf(File.separator));
            out("URL '" + url + "' is '" + newFile + "' in '" + fulldir + "'");
            File dir = new File(fulldir);
            dir.mkdirs();
            File file = new File(fullpath);
            file.createNewFile();

            FileOutputStream fstream = new FileOutputStream(file);
            fstream.write(ab, 0, cb);
            fstream.close();
            }
        }

    /**
    * Authenticate the calling thread using form authentication over HTTP.
    *
    * @throws Throwable on error
    */
    protected static void authenticate()
            throws Throwable
        {
        String sUrlForm         = s_sUrlForm;
        String sUsername        = s_sUsername;
        String sPassword        = s_sPassword;
        String sParamUsername   = s_sParamUsername;
        String sParamPassword   = s_sParamPassword;
        String sParamAdditional = s_sParamAdditional;

        if (sUrlForm == null)
            {
            return;
            }

        URL url = new URL(sUrlForm);
        if (!url.getProtocol().equalsIgnoreCase("http"))
            {
            return;
            }

        HttpURLConnection http = (HttpURLConnection) url.openConnection();

        out("Authenticating using form authentication:");
        out("\tUsername             : " + sUsername);
        out("\tPassword             : " + sPassword);
        out("\tForm URL             : " + sUrlForm);
        out("\tUsername parameter   : " + sParamUsername);
        out("\tPassword parameter   : " + sParamPassword);
        out("\tAdditional parameters: " + s_sParamAdditional);

        // get the session cookie
        http.setRequestMethod("GET");
        http.connect();

        String sCookie = http.getHeaderField("Set-Cookie");
        if (sCookie == null)
            {
            throw new RuntimeException("No session cookie returned.");
            }

        int i = sCookie.indexOf(';');
        if (i != -1)
            {
            sCookie = sCookie.substring(0, i).trim();
            }

        StringBuffer sb = new StringBuffer(sParamUsername)
                .append('=')
                .append(sUsername)
                .append('&')
                .append(sParamPassword)
                .append('=')
                .append(sPassword);

        if (sParamAdditional != null)
            {
            sb.append('&').append(sParamAdditional);
            }

        http = (HttpURLConnection) url.openConnection();
        http.setDoOutput(true);
        http.setRequestMethod("POST");
        http.setRequestProperty("Cookie", sCookie);
        http.setRequestProperty("Content-Length", String.valueOf(sb.length()));
        http.connect();
        http.getOutputStream().write(sb.toString().getBytes());

        int nCode = http.getResponseCode();
        if (nCode == 200)
            {
            s_sCookie = sCookie;
            }
        else
            {
            throw new RuntimeException("Authentication failed: ("
                    + nCode
                    + ") "
                    + http.getResponseMessage());
            }
        }

    /**
    * Output application usage.
    */
    protected static void usage()
        {
        out("Usage:\n"
                + "VacuumAndSave -url (target URL)\n\t"
                + "[-auth (username) (password) (form URL) (username parameter) (password parameter) [additional parameters]]\n\t"
                + "[-dir (directory)]\n\t"
                + "[-filter (filter)]");
        }

    /**
    * Parse the given command line arguments.
    *
    * @param asArgs  the command line arguments
    *
    * @return true if the arguments were parsed successfully; false otherwise
    */
    protected static boolean parseArguments(String[] asArgs)
        {
        String sDir             = null;
        String sFilter          = null;
        String sUrl             = null;
        String sUrlForm         = null;
        String sUsername        = null;
        String sPassword        = null;
        String sParamUsername   = null;
        String sParamPassword   = null;
        String sParamAdditional = null;

        // parse arguments
        for (int i = 0, c = asArgs.length; i < c; ++i)
            {
            int    cLeft = c - i;
            String sArg  = asArgs[i];

            // -auth
            if (sArg.equalsIgnoreCase("-auth"))
                {
                if (sUrlForm == null && cLeft > 4)
                    {
                    sUsername      = asArgs[++i];
                    sPassword      = asArgs[++i];
                    sUrlForm       = asArgs[++i];
                    sParamUsername = asArgs[++i];
                    sParamPassword = asArgs[++i];

                    if (i < c && !asArgs[i + 1].startsWith("-"))
                        {
                        sParamAdditional = asArgs[++i];
                        }
                    }
                else
                    {
                    usage();
                    return false;
                    }
                }

            // -dir
            else if (sArg.equals("-dir"))
                {
                if (sDir == null && cLeft > 0)
                    {
                    sDir = asArgs[++i];
                    }
                else
                    {
                    usage();
                    return false;
                    }
                }

            // -dir
            else if (sArg.equals("-filter"))
                {
                if (sFilter == null && cLeft > 0)
                    {
                    sFilter = asArgs[++i];
                    }
                else
                    {
                    usage();
                    return false;
                    }
                }

            // -url
            else if (sArg.equalsIgnoreCase("-url"))
                {
                if (sUrl == null && cLeft > 0)
                    {
                    sUrl = asArgs[++i];
                    }
                else
                    {
                    usage();
                    return false;
                    }
                }
            }

        // check required arguments
        if (sUrl == null ||
            ((sUsername == null) != (sPassword == null)) ||
            (sUsername == null) != (sUrlForm == null))
            {
            usage();
            return false;
            }

        s_sDir             = sDir;
        s_sFilter          = sFilter;
        s_sUrl             = sUrl;
        s_sUrlForm         = sUrlForm;
        s_sUsername        = sUsername;
        s_sPassword        = sPassword;
        s_sParamUsername   = sParamUsername;
        s_sParamPassword   = sParamPassword;
        s_sParamAdditional = sParamAdditional;

        return true;
        }

    // ------ application entry ---------------------------------------------

    /**
    * Main application entry point.
    *
    * @param asArgs  command line arguments
    */
    public static void main(String asArgs[])
        {
        try
            {
            if (!parseArguments(asArgs))
                {
                return;
                }

            // authenticate, if necessary
            authenticate();

            String sUrl       = s_sUrl;
            Thread threadMain = s_threadMain = Thread.currentThread();

            // create some worker threads
            for (int i = 0; i < THREADS_MAX; ++i)
                {
                String s = "Vacuum worker " + i;
                (new VacuumThread(s)).start();
                }

            // wait for all threads to register
            out("Waiting for worker threads to start...");
            List listThreads = s_listThreads;
            while (listThreads.size() < THREADS_MAX)
                {
                Thread.yield();
                }
            out("Worker threads started.");

            // queue up the starting URL (which will assign a thread to process it)
            URL url = new URL(sUrl);
            queue(url);

            // wait for all threads to complete
            out("Waiting for job completion...");
            synchronized (threadMain)
                {
                threadMain.wait();
                }
            out("Exiting...");
            }
        catch (Throwable t)
            {
            out();
            out("Caught \"" + t + "\"");
            out("(begin stack trace)");
            out(t);
            out("(end stack trace)");
            out();
            }
        }


    // ----- VaccumThread inner class ---------------------------------------

    /**
    * Thread that downloads and saves content from a URL.
    */
    static class VacuumThread extends Thread
        {
        // ----- constructors ---------------------------------------------

        /**
        * Create a new VacuumThread with the given name.
        *
        * @param sName  the name of the new VacuumThread
        */
        public VacuumThread(String sName)
            {
            super(sName);
            }


        // ----- Thread methods ---------------------------------------------

        /**
        * Main run method.
        */
        public void run()
            {
            while (true)
                {
                try
                    {
                    // nothing to work on ... stand in line for more work
                    List listThreads = s_listThreads;
                    synchronized (listThreads)
                        {
                        listThreads.add(this);
                        if (listThreads.size() == THREADS_MAX)
                            {
                            // all work is done
                            Thread threadMain = s_threadMain;
                            synchronized (threadMain)
                                {
                                threadMain.notify();
                                }
                            }
                        }

                    // waiting for work to do
                    synchronized (this)
                        {
                        wait();
                        }

                    // while there are URLs to work on, process the next one
                    while (true)
                        {
                        URL  url;
                        List listURLs = s_listURLs;
                        synchronized (listURLs)
                            {
                            if (listURLs.isEmpty())
                                {
                                break;
                                }

                            url = (URL) listURLs.remove(0);
                            }

                        try
                            {
                            vacuum(url);
                            }
                        catch (Throwable t)
                            {
                            out();
                            out("Caught \"" + t + "\" while vacuuming.");
                            }
                        }
                    }
                catch (Throwable t)
                    {
                    out();
                    out("Caught \"" + t + "\" while running worker thread.");
                    }
                }
            }
        }


    // ----- constants ------------------------------------------------------

    /**
    * The number of worker threads to run in parallel.
    */
    private static final int THREADS_MAX = 16;

    /**
    * The size of the read buffer used by each worker thread.
    */
    private static final int BUFFER_SIZE    = 0x10000; // 64k


    // ----- data members ---------------------------------------------------

    /**
    * The main application thread.
    */
    private static Thread s_threadMain;

    /**
    * The name of the directory in which to save content.
    */
    private static String s_sDir;

    /**
    * An optional regular expression used to filter URLs.
    */
    private static String s_sFilter;

    /**
    * The target URL.
    */
    private static String s_sUrl;

    /**
    * The authentication form URL.
    */
    private static String s_sUrlForm;

    /**
    * The username to use during authentication.
    */
    private static String s_sUsername;

    /**
    * The password to use during authentication.
    */
    private static String s_sPassword;

    /**
    * The username parameter used for form authentication.
    */
    private static String s_sParamUsername;

    /**
    * The password parameter used for form authentication.
    */
    private static String s_sParamPassword;

    /**
    * Additional parameters to pass during form authentication.
    */
    private static String s_sParamAdditional;

    /**
    * The session ID cookie.
    */
    private static String s_sCookie;

    /**
    * The list of worker threds.
    */
    private static final List s_listThreads = new LinkedList();

    /**
    * The list of URLs to vaccum.
    */
    private static final List s_listURLs = new LinkedList();

    /**
    * The set of file names that have been written.
    */
    private static final Set s_setNames = new HashSet();

    /**
    * The set of URLs visted by worker threads.
    */
    private static final Set s_setVisited = new HashSet();

    /**
    * ThreadLocal object used to store a read buffer.
    */
    private static final ThreadLocal s_tloBuf = new ThreadLocal();
    }