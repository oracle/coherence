/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.tools;


import java.io.File;
import java.io.FileInputStream;

import java.util.Enumeration;


/**
* A global search and replace utility.
*
* @author cp 07/22/98
*/
public class LineCount
        extends CommandLineTool
    {
    public static void main(String asArgs[])
        {
        try
            {
            // first 1 argument is required
            int cArgs = asArgs.length;
            if (cArgs < 1)
                {
                showInstructions();
                return;
                }

            // parse file spec
            String sFileSpec = asArgs[0];
            if (sFileSpec.length() < 1)
                {
                showInstructions();
                return;
                }

            // parse for options
            boolean fPrompt  = false;
            boolean fRecurse = false;
            boolean fVerbose = false;
            for (int i = 1; i < cArgs; ++i)
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
                out("File Spec   :  \"" + sFileSpec + "\"");
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
            int    cTotal  = 0;
            int    cFiles  = 0;
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
                if (!fReadable || cbFile > MAXSIZE)
                    {
                    String sText = "";
                    if (!fReadable)
                        {
                        sText = "(not readable)";
                        }
                    else if (cbFile > MAXSIZE)
                        {
                        sText = "(too large)";
                        }

                    out("Skipping " + file.getPath() + " " + sText);
                    continue;
                    }

                // verify with user (if prompt option used)
                if (fPrompt)
                    {
                    char chAns = in("Evaluate? (Y/N): ");
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

                // read entire file into buffer
                {
                FileInputStream in = new FileInputStream(file);
                int cbTotal = 0;
                while (cbTotal < cbFile)
                    {
                    cbTotal += in.read(abBuf, cbTotal, cbFile - cbTotal);
                    }
                in.close();
                }

                // count lines
                int cLines = 0;
                for (int of = 0; of < cbFile; ++of)
                    {
                    if (abBuf[of] == '\n')
                        {
                        ++cLines;
                        }
                    }

                // display number of lines
                if (fVerbose)
                    {
                    out(cLines + " line(s)");
                    }

                ++cFiles;
                cTotal += cLines;
                }

            out();
            out("Total files:  " + cFiles);
            out("Total lines:  " + cTotal);
            out();
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

    static void showInstructions()
        {
        out();
        out("Line-counting utility");
        out();
        out("Usage:");
        out("  LineCount <filespec> [-p] [-d] [-v]");
        out();
        out("Options:");
        out("  -d  Recurse sub-directories");
        out("  -p  Prompt before making each change");
        out("  -v  Verbose mode");
        out();
        out("Example:");
        out("  java com.tangosol.dev.tools.LineCount \"*.java\" -d");
        out();
        }
    }
