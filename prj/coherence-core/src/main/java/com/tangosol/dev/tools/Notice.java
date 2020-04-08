/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.tools;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.channels.FileChannel;

import java.util.Enumeration;


/**
* A utility which stamps a notice onto source files.
*
* @author cp 07/29/98
*/
public class Notice
        extends CommandLineTool
    {
    public static final byte[] FILE = "%file%".getBytes();
    public static final byte[] DIR  = "%dir%" .getBytes();
    public static final byte[] PATH = "%path%".getBytes();

    public static void main(String asArgs[])
        {
        try
            {
            // first 2 arguments are required
            int cArgs = asArgs.length;
            if (cArgs < 2)
                {
                showInstructions();
                return;
                }

            // parse file spec, text file
            String sFileSpec = asArgs[0];
            String sTextFile = asArgs[1];
            if (sFileSpec.length() < 1 || sTextFile.length() < 1)
                {
                showInstructions();
                return;
                }

            // parse for options
            boolean fPrompt  = false;
            boolean fRecurse = false;
            boolean fVerbose = false;
            for (int i = 2; i < cArgs; ++i)
                {
                String sOpt = asArgs[i];
                if (sOpt.length() < 2 || sOpt.charAt(0) != '-')
                    {
                    showInstructions();
                    return;
                    }

                for (int of = 1; of < sOpt.length(); ++of)
                    {
                    switch (sOpt.charAt(of))
                        {
                        case 'P':
                        case 'p':
                            fPrompt  = true;
                            break;

                        case 'D':
                        case 'd':
                            fRecurse = true;
                            break;

                        case 'V':
                        case 'v':
                            fVerbose = true;
                            break;

                        default:
                            showInstructions();
                            return;
                        }
                    }
                }

            if (fVerbose)
                {
                out();
                out("File Spec:  \"" + sFileSpec + "\"");
                out("Text File:  \"" + sTextFile + "\"");
                out("Selected Options:");
                if (fRecurse)
                    {
                    out("  -d  Recurse sub-directories");
                    }
                if (fPrompt)
                    {
                    out("  -p  Prompt before making each change");
                    }
                out("  -v  Verbose mode");
                }


            // ----- load the text file (the notice) -----

            if (fVerbose)
                {
                out();
                out("Loading notice ...");
                }

            byte[] abNotice = null;
            {
            Enumeration enmrFiles = applyFilter(sTextFile, false);
            if (enmrFiles == null)
                {
                out();
                out("Invalid directory or file specification:  " + sTextFile);
                showInstructions();
                return;
                }

            if (!enmrFiles.hasMoreElements())
                {
                out();
                out("File does not exist:  " + sTextFile);
                showInstructions();
                return;
                }

            File file = (File) enmrFiles.nextElement();
            if (enmrFiles.hasMoreElements())
                {
                out();
                out("File specification is ambiguous:  " + sTextFile);
                showInstructions();
                return;
                }

            boolean fReadable  = file.canRead();
            int     cbFile     = (int) file.length();   // assume <4gb

            // verify that the text can be operated on
            if (!fReadable || cbFile == 0 || cbFile > 0x10000)
                {
                String sText = "";
                if (!fReadable)
                    {
                    sText = "(not readable)";
                    }
                else if (cbFile == 0)
                    {
                    sText = "(zero length)";
                    }
                else
                    {
                    sText = "(too large)";
                    }

                out();
                out("Notice file " + file.getPath() + " is invalid " + sText);
                showInstructions();
                return;
                }

            // read notice file
            abNotice = new byte[cbFile];
            try (FileInputStream in = new FileInputStream(file))
                {
                int cbTotal = 0;
                while (cbTotal < cbFile)
                    {
                    cbTotal += in.read(abNotice, cbTotal, cbFile - cbTotal);
                    }
                }
            }

            if (fVerbose)
                {
                out();
                out("Notice file:");
                out(indentString(new String(abNotice), "     "));
                }


            // ----- stamp the source files with the notice -----

            if (fVerbose)
                {
                out();
                out("Selecting files ...");
                }

            Enumeration enmrFiles = applyFilter(sFileSpec, fRecurse);
            if (enmrFiles == null)
                {
                out();
                out("Invalid directory or file specification:  " + sFileSpec);
                showInstructions();
                return;
                }

            if (fVerbose)
                {
                out();
                out("Processing files ...");
                }

            int    MAXSIZE = 0x1000000;         // 16mb
            int    MINSIZE = 0x10000;           // 64k
            int    cbBuf   = MINSIZE;
            byte[] abBuf   = new byte[cbBuf];
            while (enmrFiles.hasMoreElements())
                {
                File    file       = (File) enmrFiles.nextElement();
                boolean fReadable  = file.canRead();
                boolean fWriteable = file.canWrite();
                boolean fHidden    = file.isHidden();
                int     cbFile     = (int) file.length();   // assume <4gb

                // verbose mode:  show file details
                if (fPrompt || fVerbose)
                    {
                    StringBuffer sb = new StringBuffer();
                    if (fReadable)
                        {
                        sb.append("r");
                        }
                    if (fWriteable)
                        {
                        sb.append("w");
                        }
                    if (fHidden)
                        {
                        sb.append("h");
                        }
                    out(file.getPath() + ", (" + sb.toString() + "), " + cbFile + " bytes");
                    }

                // verify that the file can be operated on
                if (!fReadable || !fWriteable || fHidden || cbFile > MAXSIZE)
                    {
                    String sText = "";
                    if (!fReadable)
                        {
                        sText = "(not readable)";
                        }
                    else if (!fWriteable)
                        {
                        sText = "(not writeable)";
                        }
                    else if (fHidden)
                        {
                        sText = "(hidden)";
                        }
                    else if (cbFile > MAXSIZE)
                        {
                        sText = "(too large)";
                        }

                    out("Skipping " + file.getPath() + " " + sText);
                    continue;
                    }

                // verify with user (if prompt option used)
                char chAns = 'Y';
                if (fPrompt)
                    {
                    chAns = in("Stamp? (Y/N): ");
                    if (chAns != 'Y' && chAns != 'y')
                        {
                        // process next file
                        continue;
                        }
                    }

                // make sure buffer is big enough
                if (cbFile > cbBuf)
                    {
                    cbBuf = cbFile + MINSIZE;
                    abBuf = new byte[cbBuf];
                    }

                // create the notice for this file
                byte[] abStamp = createNotice(abNotice, file);

                // read entire file into buffer
                try (FileInputStream in = new FileInputStream(file))
                    {
                    int cbTotal = 0;
                    while (cbTotal < cbFile)
                        {
                        cbTotal += in.read(abBuf, cbTotal, cbFile - cbTotal);
                        }
                    }

                // write the notice plus the contents of the file
                try (FileOutputStream out = new FileOutputStream(file))
                    {
                    // verify exclusive access
                    FileChannel channel = out.getChannel();
                    channel.lock();
                    channel.truncate(0);

                    out.write(abStamp);
                    out.write(abBuf, 0, cbFile);
                    }
                }
            }
        catch (Throwable t)
            {
            throw ensureRuntimeException(t);
            }
        }

    static void showInstructions()
        {
        out();
        out("Source code stamping utility");
        out();
        out("Usage:");
        out("  Notice <filespec> <text file> [-p] [-d] [-v]");
        out();
        out("Options:");
        out("  -d  Recurse sub-directories");
        out("  -p  Prompt for each files");
        out("  -v  Verbose mode");
        out();
        out("The text file can contain the following tags:");
        out("  %file%   Substituted with the name of the file");
        out("  %dir%    Substituted with the directory name containing the file");
        out("  %path%   Substituted with the fully qualified path of the file");
        out();
        out("Example:");
        out("  java com.tangosol.dev.tools.Notice \"*.java\" copyright.txt -d");
        out();
        }

    static byte[] createNotice(byte[] abNotice, File file)
            throws IOException
        {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        int cbNotice = abNotice.length;
        for (int of = 0; of < cbNotice; )
            {
            byte b = abNotice[of];

            if (b == '%')
                {
                String s  = null;
                int    cb = 0;

                if (byteCompare(abNotice, of, FILE))
                    {
                    cb = FILE.length;
                    s  = file.getName();
                    }
                else if (byteCompare(abNotice, of, DIR))
                    {
                    cb = DIR.length;
                    s  = file.getParent();
                    }
                else if (byteCompare(abNotice, of, PATH))
                    {
                    cb = PATH.length;
                    s  = file.getPath();
                    }

                if (s != null)
                    {
                    byte[] ab = s.getBytes();
                    stream.write(ab);
                    of += cb;
                    continue;
                    }
                }

            stream.write(b);
            ++of;
            }

        return stream.toByteArray();
        }

    static boolean byteCompare(byte[] ab, int of, byte[] abSearch)
            throws IOException
        {
        // check for over-run (search string would not fit)
        int cb = abSearch.length;
        if (of + cb > ab.length)
            {
            return false;
            }

        // compare bytes
        for (int i = 0; i < cb; ++i)
            {
            if (ab[of++] != abSearch[i])
                {
                return false;
                }
            }

        return true;
        }
    }
