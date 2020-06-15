/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.tools;


import java.io.File;

import java.util.Enumeration;


/**
* A touch utility.
*
* @author gg 07/02/00
*/
public class Touch
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
                    out("  -p  Prompt before touching each file");
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

            int    cTotal  = 0;
            int    cFiles  = 0;
            long   lTime   = System.currentTimeMillis();
            while (enmrFiles.hasMoreElements())
                {
                File    file       = (File) enmrFiles.nextElement();
                boolean fReadable  = file.canRead();
                boolean fWriteable = file.canWrite();
                boolean fHidden    = file.isHidden();
                boolean fNameShown = false;

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
                    out(file.getPath() + " (" + sb.toString() + ")");
                    fNameShown = true;
                    }

                // verify that the file can be operated on
                if (!fReadable || !fWriteable || fHidden)
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

                    out("Skipping " + file.getPath() + " " + sText);
                    continue;
                    }

                // verify with user (if prompt option used)
                char chAns = 'Y';
                if (fPrompt)
                    {
                    chAns = in("Touch? (Y/N): ");
                    if (chAns != 'Y' && chAns != 'y')
                        {
                        // process next file
                        continue;
                        }
                    }

                // touch
                if (file.setLastModified(lTime))
                    {
                    ++cFiles;
                    }
                else
                    {
                    out("Failed to touch " + file.getPath());
                    }
                }

            out();
            out("Total files:  " + cFiles);
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
        out("Usage:");
        out("  Touch <filespec>");
        out();
        out("Options:");
        out("  -d  Recurse sub-directories");
        out("  -p  Prompt before touching each file");
        out("  -v  Verbose mode");
        out();
        out("Example:");
        out("  java com.tangosol.dev.tools.Touch \"*.java\" -d");
        out();
        }
    }
