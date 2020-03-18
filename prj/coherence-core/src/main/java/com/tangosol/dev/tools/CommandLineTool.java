/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.tools;


import com.tangosol.util.Base;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.SimpleEnumerator;
import com.tangosol.util.StringTable;
import com.tangosol.util.ListMap;

import java.io.File;
import java.io.FileFilter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;


/**
* A class on which to base command line tools.
*
* @author cp 07/22/98
*/
public abstract class CommandLineTool
        extends Base
    {
    // ---- property accessors ----------------------------------------------

    public String[] getArguments()
        {
        return m_asArg;
        }

    public void setArguments(String[] asArg)
        {
        m_asArg = asArg;
        }


    // ----- console support ------------------------------------------------

    /**
    * Get console input:  Character.
    *
    * @param s  the text to print
    *
    * @return the character pressed
    */
    public static char in(String s)
        {
        if (s != null && s.length() > 0)
            {
            System.out.print(s);
            System.out.flush();
            }

        InputStreamReader reader = new InputStreamReader(System.in);
        int ch;
        try
            {
            while ((ch = reader.read()) < 0)
                {
                }
            }
        catch (IOException e)
            {
            ch = 0x0;
            }

        return (char)ch;
        }

    /**
    * Get console input:  String.
    *
    * @param s  the text to print
    *
    * @return the string entered (null if an IOException occurs)
    */
    public static String inputString(String s)
        {
        if (s != null && s.length() > 0)
            {
            System.out.print(s);
            System.out.flush();
            }

        StringBuffer sb = new StringBuffer();
        try
            {
            InputStreamReader reader = new InputStreamReader(System.in);
            do
                {
                sb.append(new BufferedReader(reader).readLine());
                }
            while (System.in.available() > 0);

            return sb.toString();
            }
        catch (IOException e)
            {
            return null;
            }
        }

    // ----- agrument handling ----------------------------------------------

    /**
    * Parses the array of arguments into a map.
    *
    * Assume that a java tool starts by command line having
    * the following syntax:
    *   cmd-line  ::== (command space)* (argument space)*
    *   command   ::== "-" cmd-name ("=" | ":" | space) (cmd-value)?
    *   cmd-name  ::== word
    *   cmd-value ::== word ("," word)*
    *   argument  ::== word ("," word)*
    *   space     ::== (" ")+
    *
    * When java starts an application the arguments in the command line
    * are placed into a string array by breaking at spaces.
    * The purpose of this method is to place the command line
    * into a ListMap where each <command> would represent
    * an entry in this map with values equal to <cmd-value> (null if not
    * present) and each <argument> represented with an entry that has
    * the key equal to an Integer object holding on the 0-based argument number
    * and the value equal to the argument itself.
    *
    * @param asArg      an array of arguments from "public static main(String[])"
    * @param asCommand  an array of valid commands (if null, anything is allowed)
    * @param fCaseSens  if true, uses the commands the way they are typed in;
    *                   if false, converts all the commands to lowercase.
    *
    * @throws IllegalArgumentException if the syntax is unexpected or an invalid
    *         command has been encountered;  a caller is supposed to output the
    *         "Usage: ... " message if this exception is thrown.
    */
    public static ListMap parseArguments(String[] asArg, String[] asCommand, boolean fCaseSens)
        {
        ListMap map        = new ListMap();
        String  sCommand   = null;
        int     iArg       = -1;

        for (int i = 0; i < asArg.length; i++)
            {
            String sArg = asArg[i];

            if (sArg.charAt(0) == '-')
                {
                // encountered a new command
                if (sCommand != null)
                    {
                    // the previous command had no value
                    addArgToResultsMap(sCommand, null, map);
                    }

                sCommand = sArg.substring(1);
                if (sCommand.length() == 0)
                    {
                    throw new IllegalArgumentException("An empty command");
                    }

                int of = sCommand.indexOf('=');
                if (of < 0)
                    {
                    of = sCommand.indexOf(':');
                    }

                if (of > 0)
                    {
                    String sValue = sCommand.substring(of + 1);

                    sCommand = validateCommand(sCommand.substring(0, of),
                        asCommand, fCaseSens);
                    addArgToResultsMap(sCommand, sValue, map);
                    sCommand = null;
                    }
                else
                    {
                    sCommand = validateCommand(sCommand, asCommand, fCaseSens);
                    }
                }
            else
                {
                if (sCommand == null)
                    {
                    // encountered an argument
                    map.put(Integer.valueOf(++iArg), sArg);
                    }
                else
                    {
                    // encountered an cmd-value
                    addArgToResultsMap(sCommand, sArg, map);
                    sCommand = null;
                    }
                }
            }

        if (sCommand != null)
            {
            // the last arg was an command without a value
            map.put(sCommand, null);
            }
        return map;
        }

    /**
    * Add the provided argument and its value to the results map inflating the
    * value to be a List of values if the map already contains the argument.
    *
    * @param sArg    the parsed command line argument
    * @param oValue  the parsed value for the argument
    * @param map     the results map to add the argument and value to
    */
    protected static void addArgToResultsMap(String sArg, Object oValue, Map map)
        {
        Object oValuePrev = map.get(sArg);
        if (oValuePrev == null)
            {
            map.put(sArg, oValue);
            return;
            }

        List listVals;
        if (oValuePrev instanceof List)
            {
            listVals = (List) oValuePrev;
            }
        else
            {
            map.put(sArg, listVals = new LinkedList());
            listVals.add(oValuePrev);
            }
        listVals.add(oValue);
        }

    /**
    * @see #parseArguments(String[], String[], boolean)
    */
    public static ListMap parseArguments(String[] asArg)
        {
        return parseArguments(asArg, null, false);
        }

    /**
    * Search the supplied argument set for known switches, and extract them.
    * Each switch which is found will be placed in the returned List and
    * removed from the argument collection.
    *
    * @param colArg         argument collection to parse, and remove switch from
    * @param asValidSwitch  switches to look for
    *
    * @return list of found switches
    */
    public static List extractSwitches(Collection colArg,
                                          String[] asValidSwitch)
        {
        List lResult = new LinkedList();
        for (int i = 0, c = asValidSwitch.length; i < c; ++i)
            {
            String sSwitch = "-" + asValidSwitch[i];
            for (Iterator iter = colArg.iterator(); iter.hasNext(); )
                {
                String sArg = (String) iter.next();
                if (sArg.equals(sSwitch))
                    {
                    lResult.add(asValidSwitch[i]);
                    iter.remove();
                    }
                }
            }
        return lResult;
        }

    /**
    * Process a command from the command line arguments.
    * This method is used to process required commands, and will throw
    * an exception if the command was not specified.
    *
    * @param mapCommands the map of command line arguments
    * @param oArg        the argument to process
    *
    * @return the value
    *
    * @throws UnsupportedOperationException if a no value is available
    */
    public static Object processCommand(Map mapCommands, Object oArg)
            throws UnsupportedOperationException
        {
        Object value = mapCommands.get(oArg);
        if (value == null)
            {
            if (oArg instanceof String)
                {
                throw new UnsupportedOperationException("-" + oArg
                        + " must be specified.");
                }
            else
                {
                throw new UnsupportedOperationException("argument " + oArg
                        + " must be specified.");
                }
            }
        return value;
        }

   /**
   * Process a command from the command line arguments.
   * This method is used to process optional commands, and the
   * default will be returned if command was not explicitly specified.
   *
   * @param mapCommands the map of command line arguments
   * @param oArg        the argument to process
   * @param oDefault    Specifies a default value
   *
   * @return the value, or <tt>oDefault</tt> if unspecified
   */
    public static Object processCommand(Map mapCommands, Object oArg, Object oDefault)
            throws UnsupportedOperationException
        {
        Object value = mapCommands.get(oArg);
        return (value == null ? oDefault : value);
        }

    /**
    * Process an optional command from the command line arguments, where the
    * value is to be interpreted as an integer.
    *
    * @param mapCommands the map of command line arguments
    * @param oArg        the argument to process
    * @param iDefault    Specifies an default value
    *
    * @return the value, or <tt>iDefault</tt> if unspecified
    */
    public static int processIntCommand(Map mapCommands, Object oArg, int iDefault)
            throws UnsupportedOperationException
        {
        Object value = mapCommands.get(oArg);
        return (value == null ? iDefault : Integer.parseInt((String) value));
        }

    /**
    * Process a required command from the command line arguments, where the
    * value is to be interpreted as an integer.
    *
    * @param mapCommands the map of command line arguments
    * @param oArg        the argument to process
    *
    * @return the value
    * @throws UnsupportedOperationException if a no value is available
    */
    public static int processIntCommand(Map mapCommands, Object oArg)
            throws UnsupportedOperationException
        {
        Object value = processCommand(mapCommands, oArg);
        return Integer.parseInt((String) value);
        }

    /**
    * Process an optional command from the command line arguments, where the
    * value is to be interpreted as an long.
    *
    * @param mapCommands the map of command line arguments
    * @param oArg        the argument to process
    * @param lDefault    Specifies an default value
    *
    * @return the value, or <tt>iDefault</tt> if unspecified
    */
    public static long processLongCommand(Map mapCommands, Object oArg, long lDefault)
            throws UnsupportedOperationException
        {
        Object value = mapCommands.get(oArg);
        return (value == null ? lDefault : Long.parseLong((String) value));
        }

    /**
    * Validate the command
    */
    protected static String validateCommand(String sCommand, String[] asCommand, boolean fCaseSens)
        {
        if (!fCaseSens)
            {
            sCommand = sCommand.toLowerCase();
            }

        if (asCommand == null)
            {
            return sCommand;
            }

        for (int i = 0; i < asCommand.length; i++)
            {
            if (asCommand[i].equals(sCommand))
                {
                return sCommand;
                }
            }
        throw new IllegalArgumentException("Illegal command: -" + sCommand);
        }

    /**
    * Print to console:  The passed arguments.
    */
    public static void showArgs(String[] asArgs)
        {
        out();
        int cArgs = asArgs.length;
        out(cArgs + " command line arguments:");
        for (int i = 0; i < cArgs; ++i)
            {
            out("[" + i + "]=\"" + asArgs[i] + "\"");
            }
        out();
        }


    // ----- filter support -------------------------------------------------

    /**
    * Select files based on a filter specification.
    *
    * @param sFileSpec  a file specification using the wild-cards "?" and "*"
    * @param fRecurse   true if sub-directories should be searched
    *
    * @return an enumeration of the selected files
    */
    public static Enumeration applyFilter(String sFileSpec, boolean fRecurse)
        {
        // determine what the file filter is:
        //  (1) the path or name of a file (e.g. "Replace.java")
        //  (2) a path (implied *.*)
        //  (3) a path and a filter (e.g. "\*.java")
        //  (4) a filter (e.g. "*.java")
        String sFilter = "*";
        String sDir    = sFileSpec;
        File   dir;

        // file spec may be a complete dir specification
        dir = new File(sDir).getAbsoluteFile();
        if (!dir.isDirectory())
            {
            // parse the file specification into a dir and a filter
            int of = sFileSpec.lastIndexOf(File.separatorChar);
            if (of < 0)
                {
                sDir    = "";
                sFilter = sFileSpec;
                }
            else
                {
                sDir    = sFileSpec.substring(0, ++of);
                sFilter = sFileSpec.substring(of);
                }

            // test the parsed directory name by itself
            dir = new File(sDir).getAbsoluteFile();
            if (!dir.isDirectory())
                {
                return null;
                }
            }

        // check filter, and determine if it denotes
        //  (1) a specific file
        //  (2) all files
        //  (3) a subset (filtered set) of files
        Stack stackDirs = new Stack();
        FileFilter filter;

        if (sFilter.length() < 1)
            {
            sFilter = "*";
            }

        if (sFilter.indexOf('*') < 0 && sFilter.indexOf('?') < 0)
            {
            if (fRecurse)
                {
                // even though we are looking for a specific file, we still
                // have to recurse through sub-dirs
                filter = new ExactFilter(stackDirs, fRecurse, sFilter);
                }
            else
                {
                File file = new File(dir, sFilter);
                if (file.isFile() && file.exists())
                    {
                    return new SimpleEnumerator(new File[] {file});
                    }
                else
                    {
                    return NullImplementation.getEnumeration();
                    }
                }
            }
        else if (sFilter.equals("*"))
            {
            filter = new AllFilter(stackDirs, fRecurse);
            }
        else
            {
            filter = new PatternFilter(stackDirs, fRecurse, sFilter);
            }

        stackDirs.push(dir);
        return applyFilter(stackDirs, filter);
        }

    /**
    * Using the passed filter, enumerate all files that pass the filter in
    * the set of directories which are in the passed stack.
    */
    protected static Enumeration applyFilter(Stack stackDirs, FileFilter filter)
        {
        Vector vectFiles = new Vector();

        while (!stackDirs.isEmpty())
            {
            File dir = (File) stackDirs.pop();
            if (!dir.isDirectory())
                {
                throw new IllegalArgumentException("Illegal directory:  \"" + dir.getPath() + "\"");
                }

            File[] aFiles = dir.listFiles(filter);
            int    cFiles = aFiles.length;
            for (int i = 0; i < cFiles; ++i)
                {
                vectFiles.addElement(aFiles[i]);
                }
            }

        return vectFiles.elements();
        }

    /**
    * Select files from a ZIP/JAR based on a filter specification.
    *
    * A zip file contains directories and files.  (Directories are optional.)
    * The format for a directory name is "<dir>/<dir>/"
    * The format for a file name is "<dir>/<dir>/<file>"
    *
    * @param zip        an open ZIP/JAR file
    * @param sFileSpec  a file specification using the wild-cards "?" and "*"
    * @param fRecurse   true if sub-directories should be searched
    *
    * @return an enumeration of the selected files
    */
    public static Enumeration applyZipFilter(ZipFile zip, String sFileSpec, boolean fRecurse)
        {
        // determine what the file filter is:
        //  (1) the path or name of a file (e.g. "Replace.java")
        //  (2) a path (implied *.*)
        //  (3) a path and a filter (e.g. "/*.java")
        //  (4) a filter (e.g. "*.java")

        // file spec may be a complete dir specification
        String sFilter = "*";
        String sDir    = sFileSpec;
        if (sDir.length() > 0 && sDir.charAt(0) == '/')
            {
            sDir = sDir.substring(1);
            }
        // we know it is a directory if it ends with "/" ... but if it
        // does not, it could still be a directory
        if (sDir.length() > 0 && sDir.charAt(sDir.length()-1) != '/')
            {
            ZipEntry file = zip.getEntry(sDir);
            if (file == null)
                {
                ZipEntry dir = zip.getEntry(sDir + '/');
                if (dir == null)
                    {
                    int ofSlash = sDir.lastIndexOf('/') + 1;
                    if (ofSlash > 0)
                        {
                        sFilter = sDir.substring(ofSlash);
                        sDir    = sDir.substring(0, ofSlash);
                        }
                    else
                        {
                        // assume it is a file name
                        if (fRecurse)
                            {
                            // search for that file name in all dirs
                            sFilter = sDir;
                            sDir    = "";
                            }
                        else
                            {
                            // file not found
                            return NullImplementation.getEnumeration();
                            }
                        }
                    }
                else
                    {
                    sDir += '/';
                    }
                }
            else
                {
                // if a dir is specified as part of the name or if it is
                // the root directory of the zip and directory recursion
                // is not specified, then it is an exact file match (not
                // a directory)
                if (sDir.indexOf('/') >= 0 || !fRecurse)
                    {
                    return new SimpleEnumerator(new Object[] {file});
                    }

                // otherwise look for that file in all directories
                sFilter = sDir;
                sDir    = "";
                }
            }

        // check filter, and determine if it denotes
        //  (1) all files
        //  (2) a subset (filtered set) of files
        //  (3) a specific file
        ZipFilter filter;

        if (sFilter.length() < 1 || sFilter.equals("*"))
            {
            filter = new ZipFilter(sDir, fRecurse);
            }
        else if (sFilter.indexOf('*') >= 0 || sFilter.indexOf('?') >= 0)
            {
            filter = new ZipPatternFilter(sDir, fRecurse, sFilter);
            }
        else
            {
            filter = new ZipExactFilter(sDir, fRecurse, sFilter);
            }

        // build an ordered list of the entries
        StringTable tbl = new StringTable();
        for (Enumeration enmr = zip.entries(); enmr.hasMoreElements(); )
            {
            ZipEntry entry = (ZipEntry) enmr.nextElement();
            if (filter.accept(entry))
                {
                tbl.put(entry.getName(), entry);
                }
            }
        return tbl.elements();
        }

    /**
    * Tests whether or not the passed file name matches the
    * specified wildcard pattern.
    *
    * @param   sName       the file name
    * @param   achPattern  the file pattern with wildcards (?, *)
    *
    * @return  <code>true</code> if and only if <code>sName</code>
    *          matches the pattern in <code>achPattern</code>
    */
    public static boolean matches(String sName, char[] achPattern)
        {
        return matches(sName.toCharArray(), 0, achPattern, 0);
        }

    /**
    * Tests whether or not the passed file name matches the
    * specified wildcard pattern.
    *
    * @param   achName     the file name
    * @param   ofName      the offset within the name to match
    * @param   achPattern  the file pattern with wildcards (?, *)
    * @param   ofPattern   the offset within the pattern to match
    *
    * @return  <code>true</code> if and only if <code>achName</code>
    *          matches the pattern in <code>achPattern</code>
    */
    private static boolean matches(char[] achName, int ofName, char[] achPattern, int ofPattern)
        {
        int cchName    = achName.length;
        int cchPattern = achPattern.length;
        while (ofName < cchName && ofPattern < cchPattern)
            {
            char chName    = achName[ofName];
            char chPattern = achPattern[ofPattern];

            // latin conversion
            if (chName >= 'A' && chName <= 'Z')
                {
                chName = (char) ('a' + (chName - 'A'));
                }
            if (chPattern >= 'A' && chPattern <= 'Z')
                {
                chPattern = (char) ('a' + (chPattern - 'A'));
                }

            switch (chPattern)
                {
                case '?':
                    // any char is OK
                    break;

                case '*':
                    {
                    if (ofPattern == cchPattern-1)
                        {
                        // asterisk is at end of pattern ... any name
                        // goes at this point
                        return true;
                        }

                    char chTag = achPattern[ofPattern+1];
                    for (int ofTag = ofName; ofTag < cchName; ++ofTag)
                        {
                        if ((chTag == '?' || achName[ofTag] == chTag) &&
                            matches(achName, ofTag, achPattern, ofPattern+1))
                            {
                            return true;
                            }
                        }
                    }
                    return false;

                default:
                    if (chName != chPattern)
                        {
                        return false;
                        }
                    break;
                }

            ++ofName;
            ++ofPattern;
            }

        // equal iff both name and pattern were exhausted
        return (ofName == cchName && ofPattern == cchPattern);
        }


    // ----- File filters ---------------------------------------------------

    /**
    * File filter which implements "*" (all files are accepted).
    */
    public static class AllFilter implements FileFilter
        {
        protected Stack   m_stackDirs;
        protected boolean m_fPushDirs;

        /**
        * Construct filter.
        *
        * @param stackDirs  a stack of directories encountered
        * @param fPushDirs  if true, this filter must push all
        *                   encountered directories onto stackDirs
        */
        public AllFilter(Stack stackDirs, boolean fPushDirs)
            {
            m_stackDirs = stackDirs;
            m_fPushDirs = fPushDirs;
            }

        /**
        * Tests whether or not the specified abstract path should be
        * included in a path list.
        *
        * @param   path  the path to be tested
        * @return  <code>true</code> if and only if <code>path</code>
        *          should be included
        */
        public boolean accept(File path)
            {
            if (path.isDirectory())
                {
                if (m_fPushDirs)
                    {
                    m_stackDirs.push(path);
                    }
                return false;
                }
            else
                {
                return true;
                }
            }
        }

    /**
    * File filter which accepts files that match a pattern.
    */
    public static class PatternFilter implements FileFilter
        {
        protected Stack   m_stackDirs;
        protected boolean m_fPushDirs;
        protected char[]  m_achPattern;

        /**
        * Construct filter.
        *
        * @param stackDirs  a stack of directories encountered
        * @param fPushDirs  if true, this filter must push all
        *                   encountered directories onto stackDirs
        * @param sPattern   the pattern (wildcards:  *, ?)
        */
        public PatternFilter(Stack stackDirs, boolean fPushDirs, String sPattern)
            {
            m_stackDirs  = stackDirs;
            m_fPushDirs  = fPushDirs;
            m_achPattern = sPattern.toCharArray();
            }

        /**
        * Tests whether or not the specified abstract path should be
        * included in a path list.
        *
        * @param   path  the path to be tested
        * @return  <code>true</code> if and only if <code>path</code>
        *          should be included
        */
        public boolean accept(File path)
            {
            if (path.isDirectory())
                {
                if (m_fPushDirs)
                    {
                    m_stackDirs.push(path);
                    }
                return false;
                }
            else
                {
                return CommandLineTool.matches(path.getName(), m_achPattern);
                }
            }
        }

    /**
    * File filter which accepts a file of an exact name.
    */
    public static class ExactFilter implements FileFilter
        {
        protected Stack   m_stackDirs;
        protected boolean m_fPushDirs;
        protected String  m_sFile;

        /**
        * Construct filter.
        *
        * @param stackDirs  a stack of directories encountered
        * @param fPushDirs  if true, this filter must push all
        *                   encountered directories onto stackDirs
        * @param sFile      the name of the file being searched for
        */
        public ExactFilter(Stack stackDirs, boolean fPushDirs, String sFile)
            {
            m_stackDirs = stackDirs;
            m_fPushDirs = fPushDirs;
            m_sFile     = sFile;
            }

        /**
        * Tests whether or not the specified abstract path should be
        * included in a path list.
        *
        * @param   path  the path to be tested
        * @return  <code>true</code> if and only if <code>path</code>
        *          should be included
        */
        public boolean accept(File path)
            {
            if (path.isDirectory())
                {
                if (m_fPushDirs)
                    {
                    m_stackDirs.push(path);
                    }
                return false;
                }
            else
                {
                return m_sFile.equalsIgnoreCase(path.getName());
                }
            }
        }


    // ----- ZipEntry filters -----------------------------------------------

    /**
    * A "FileFilter"-like class but for ZipEntry objects.  All zip filters
    * must derive from this class.
    */
    public static class ZipFilter
        {
        /**
        * The directory name to filter from (or 0-length to filter from the
        * root).
        */
        protected String m_sDir;

        /**
        * True if searching is recursive down the directory tree.
        */
        protected boolean m_fRecurse;

        /**
        * Construct a ZipFilter.
        *
        * @param sDir      the directory name to filter from (or 0-length to
        *                  filter from the root)
        * @param fRecurse  true if searching is recursive down the directory
        *                  tree
        */
        public ZipFilter(String sDir, boolean fRecurse)
            {
            if (sDir.length() > 0 && sDir.charAt(sDir.length() - 1) != '/')
                {
                throw new IllegalStateException("ZipFilter:  \"" + sDir + "\" is not a directory!");
                }

            m_sDir     = sDir;
            m_fRecurse = fRecurse;
            }

        /**
        * Test a ZipEntry to see if it meets the filter criteria.
        *
        * @param entry  the zip entry to test
        *
        * @return true if the entry meets the filter criteria
        */
        public boolean accept(ZipEntry entry)
            {
            // the entry name must start with the directory name, and it must
            // not be a directory
            String sName = entry.getName();
            if (sName.length() > m_sDir.length() && sName.startsWith(m_sDir)
                    && sName.charAt(sName.length() - 1) != '/')
                {
                // if searching recursively, the entry can be several levels
                // under the directory, otherwise it must be in that directory
                return m_fRecurse || sName.indexOf('/', m_sDir.length()) < 0;
                }

            return false;
            }
        }

    /**
    * Zip filter which accepts files that match a pattern.
    */
    public static class ZipPatternFilter extends ZipFilter
        {
        protected char[] m_achPattern;

        /**
        * Construct a ZipPatternFilter.
        *
        * @param sDir      the directory name to filter from (or 0-length to
        *                  filter from the root)
        * @param fRecurse  true if searching is recursive down the directory
        *                  tree
        * @param sPattern  the pattern (wildcards:  *, ?)
        */
        public ZipPatternFilter(String sDir, boolean fRecurse, String sPattern)
            {
            super(sDir, fRecurse);
            m_achPattern = sPattern.toCharArray();
            }

        /**
        * Test a ZipEntry to see if it meets the filter criteria.
        *
        * @param entry  the zip entry to test
        *
        * @return true if the entry meets the filter criteria
        */
        public boolean accept(ZipEntry entry)
            {
            return super.accept(entry)
                && CommandLineTool.matches(entry.getName(), m_achPattern);
            }
        }

    /**
    * Zip filter which accepts an entry with an exact name.
    */
    public static class ZipExactFilter extends ZipFilter
        {
        /**
        * The file name to match.
        */
        protected String m_sFile;

        /**
        * Construct a ZipExactFilter.
        *
        * @param sDir      the directory name to filter from (or 0-length to
        *                  filter from the root)
        * @param fRecurse  true if searching is recursive down the directory
        *                  tree
        * @param sFile     the file pattern (wildcards:  *, ?)
        */
        public ZipExactFilter(String sDir, boolean fRecurse, String sFile)
            {
            super(sDir, fRecurse);
            m_sFile = sFile;
            }

        /**
        * Test a ZipEntry to see if it meets the filter criteria.
        *
        * @param entry  the zip entry to test
        *
        * @return true if the entry meets the filter criteria
        */
        public boolean accept(ZipEntry entry)
            {
            if (super.accept(entry))
                {
                String sName = entry.getName();
                return m_sFile.equals(sName.substring(sName.lastIndexOf('/') + 1));
                }

            return false;
            }
        }


    // ---- data members ----------------------------------------------------

    private String[] m_asArg;
    }
