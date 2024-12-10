/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.xml;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;


/**
 * SimpleParser unit test.
 */
public class SimpleParserTest
    {
    // ----- unit tests -----------------------------------------------------
    
    @Test
    public void testNoEncodingParse() throws IOException
        {
        byte[] utfChars =
                { // lowercase alphabet A, U+0061
                  0x61,
                  // latin captal ligature, U+00C6
                  (byte) 0xc3, (byte) 0x86,
                  // hiragana letter A, U+3042
                  (byte) 0xe3, (byte) 0x81, (byte) 0x82,
                };
        char[] ucsChars =
                { '\u0061', // lowercase alphabet A
                  '\u00c6', // latin captal ligature
                  '\u3042', // hiragana letter A
                };
        // if no encoding declaration is present, the xml document shoule be
        // parsed with UTF-8
        XmlDocument doc = new SimpleParser(true).parseXml(
                new ByteArrayInputStream(getXmlBytes(null, utfChars)));
        
        assertArrayEquals(ucsChars, ((String) doc.getValue()).toCharArray());
        }

    @Test
    public void testUtf8Parse() throws IOException
        {
        byte[] utfChars =
                { // lowercase alphabet A, U+0061
                  0x61,
                  // latin captal ligature, U+00C6
                  (byte) 0xc3, (byte) 0x86,
                  // hiragana letter A, U+3042
                  (byte) 0xe3, (byte) 0x81, (byte) 0x82,
                };
        char[] ucsChars =
                { '\u0061', // lowercase alphabet A
                  '\u00c6', // latin captal ligature
                  '\u3042', // hiragana letter A
                };
        XmlDocument doc = new SimpleParser(true).parseXml(
                new ByteArrayInputStream(getXmlBytes("UTF-8", utfChars)));
        
        assertArrayEquals(ucsChars, ((String) doc.getValue()).toCharArray());
        }


    // ----- helper methods -------------------------------------------------

    private byte[] getXmlBytes(String xmlEncode, byte[] value)
            throws IOException
        {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try
            {
            DataOutputStream out = new DataOutputStream(bout);
            if (xmlEncode == null)
                {
                out.writeBytes("<?xml version=\"1.0\" ?>");
                }
            else
                {
                out.writeBytes("<?xml version=\"1.0\" encoding=\"" + xmlEncode + "\" ?>");
                }
            out.writeBytes("<foo>");
            out.write(value, 0, value.length);
            out.writeBytes("</foo>");
            out.flush();
            return bout.toByteArray();
            }
        finally
            {
            bout.close();
            }
        }

    }
