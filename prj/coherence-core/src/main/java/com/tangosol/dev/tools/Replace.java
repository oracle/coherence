/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.tools;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.util.Enumeration;


/**
* A global search and replace utility.
*
* @author cp 07/22/98
*/
public class Replace
        extends CommandLineTool
    {
    public static void main(String asArgs[])
        {
        try
            {
            // first 3 arguments are required
            int cArgs = asArgs.length;
            if (cArgs < 3)
                {
                showInstructions();
                return;
                }

            // parse file spec, old text, and new text (can be blank)
            String sFileSpec = asArgs[0];
            String sOldText  = asArgs[1];
            String sNewText  = asArgs[2];
            if (sFileSpec.length() < 1 || sOldText.length() < 1)
                {
                showInstructions();
                return;
                }

            // parse for options
            boolean fPrompt  = false;
            boolean fRecurse = false;
            boolean fVerbose = false;
            for (int i = 3; i < cArgs; ++i)
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
                out("Search For  :  \"" + sOldText  + "\"");
                out("Replace With:  \"" + sNewText  + "\"");
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
            byte[] abOld   = getBytes(sOldText);
            int    cbOld   = abOld.length;
            byte   bTag    = abOld[0];
            byte[] abNew   = getBytes(sNewText);
            int    cbNew   = abNew.length;
            while (enmrFiles.hasMoreElements())
                {
                File    file       = (File) enmrFiles.nextElement();
                boolean fReadable  = file.canRead();
                boolean fWriteable = file.canWrite();
                boolean fHidden    = file.isHidden();
                boolean fNameShown = false;
                int     cbFile     = (int) file.length();   // assume <4gb

                // verbose mode:  show file details
                if (fVerbose)
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
                    fNameShown = true;
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

                // make sure buffer is big enough
                if (cbFile > cbBuf)
                    {
                    cbBuf = cbFile + MINSIZE;
                    abBuf = new byte[cbBuf];
                    }

                // read entire file into buffer
                try
                    {
                    FileInputStream in = new FileInputStream(file);
                    int cbTotal = 0;
                    while (cbTotal < cbFile)
                        {
                        cbTotal += in.read(abBuf, cbTotal, cbFile - cbTotal);
                        }
                    in.close();
                    }
                catch (IOException e)
                    {
                    out("Skipping " + file.getPath() + " due to IOException:");
                    out(e);
                    continue;
                    }

                FileOutputStream out = null;
                int  ofCur    = 0;
                int  ofMax    = cbFile - abOld.length;
                int  ofLine   = 0;
                int  cbTotal  = 0;
                int  cLines   = 1;
                int  cChanges = 0;
                while (ofCur <= ofMax)
                    {
                    byte bCur = abBuf[ofCur];
                    if (bCur == bTag)
                        {
                        boolean fMatch = true;
                        for (int i = 1; i < cbOld; ++i)
                            {
                            if (abBuf[ofCur+i] != abOld[i])
                                {
                                fMatch = false;
                                break;
                                }
                            }

                        if (fMatch)
                            {
                            if (!fNameShown)
                                {
                                out(file.getPath());
                                fNameShown= true;
                                }

                            // verify with user (if prompt option used)
                            char chAns = 'Y';
                            if (fPrompt)
                                {
                                // keep no more than 40 chars before
                                boolean fSkip = (ofLine < ofCur - 40);
                                if (fSkip)
                                    {
                                    ofLine = ofCur - 40;
                                    }

                                // find end of line (keep no more than 40 chars)
                                int ofEOL   = ofCur + cbOld;
                                int ofBreak = ofEOL + 40;
                                boolean fTrunc = false;
                                while (ofEOL < cbFile)
                                    {
                                    byte b = abBuf[ofEOL];
                                    if (b == '\r' || b == '\n')
                                        {
                                        break;
                                        }
                                    if (ofEOL >= ofBreak)
                                        {
                                        fTrunc = true;
                                        break;
                                        }
                                    ++ofEOL;
                                    }

                                out("(Line " + cLines + ")  "
                                        + (fSkip ? "... " : "")
                                        + new String(abBuf, ofLine, ofEOL - ofLine)
                                        + (fTrunc ? " ..." : ""));
                                chAns = in("Replace (Y/N): ");
                                }

                            if (chAns == 'Y' || chAns == 'y')
                                {
                                // make sure the file is ready to be written to
                                if (out == null)
                                    {
                                    if (!file.delete())
                                        {
                                        out("Error deleting " + file.getPath());
                                        return;
                                        }

                                    if (!file.createNewFile())
                                        {
                                        out("Error creating " + file.getPath());
                                        return;
                                        }

                                    out = new FileOutputStream(file);
                                    }

                                // write up to the point of the found string
                                if (cbTotal < ofCur)
                                    {
                                    out.write(abBuf, cbTotal, ofCur - cbTotal);
                                    }

                                // write the new string
                                out.write(abNew);

                                ofCur  += cbOld;
                                cbTotal = ofCur;
                                ++cChanges;
                                continue;
                                }
                            }
                        }
                    else if (fPrompt && bCur == '\n')
                        {
                        ofLine = ofCur + 1;
                        ++cLines;
                        }

                    // next character
                    ++ofCur;
                    }

                if (out != null)
                    {
                    if (cbTotal < cbFile)
                        {
                        // write out remainder of the file
                        out.write(abBuf, cbTotal, cbFile - cbTotal);
                        }

                    // close the file
                    out.close();

                    // display number of modifications
                    out("(" + cChanges + " occurrences replaced)");
                    }
                }
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
        out("Global search & replace utility");
        out();
        out("Usage:");
        out("  Replace <filespec> <old string> <new string> [-p] [-d] [-v]");
        out();
        out("Options:");
        out("  -d  Recurse sub-directories");
        out("  -p  Prompt before making each change");
        out("  -v  Verbose mode");
        out();
        out("Example:");
        out("  java com.tangosol.dev.tools.Replace \"*.java\" \"import \" \"// import \" -p");
        out();
        }

    static byte[] getBytes(String s)
        {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        char[]  ach     = s.toCharArray();
        int     cch     = ach.length;
        boolean fEscape = false;
        for (int i = 0; i < cch; ++i)
            {
            char ch = ach[i];
            if (fEscape)
                {
                switch (ch)
                    {
                    case 'b':
                        stream.write((byte) '\b');
                        break;

                    case 't':
                        stream.write((byte) '\t');
                        break;

                    case 'n':
                        stream.write((byte) '\n');
                        break;

                    case 'f':
                        stream.write((byte) '\f');
                        break;

                    case 'r':
                        stream.write((byte) '\r');
                        break;

                    case '\"':
                    case '\'':
                    case '\\':
                        stream.write((byte) ch);
                        break;

                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                        {
                        int cMaxDigits = (ch > '3' ? 2 : 3);
                        int nChar = 0;
                        do
                            {
                            nChar = (nChar * 8) + (ch - '0');
                            if (++i >= cch)
                                {
                                break;
                                }
                            ch = ach[i];
                            }
                        while (ch >= '0' && ch <= '7' && --cMaxDigits > 0);
                        --i;
                        ch = (char) nChar;
                        }
                        stream.write((byte) ch);
                        break;

                    case '\r':
                    case '\n':
                        throw new IllegalArgumentException("New line in escaped literal!");

                    default:
                        throw new IllegalArgumentException("New line in escaped literal!");
                    }

                fEscape = false;
                }
            else if (ch == '\\')
                {
                fEscape = true;
                }
            else
                {
                stream.write((byte) (ch & 0xFF));
                }
            }

        if (fEscape)
            {
            throw new IllegalArgumentException("Escaped literal not completed!");
            }

        return stream.toByteArray();
        }
    }
