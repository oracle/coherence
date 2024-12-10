/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.xml;


import com.tangosol.util.Resources;


/**
* Resources for XML parsing.
*
* @version 1.00, 07/16/01
* @author Cameron Purdy
*/
public class PackageResources extends Resources
    {
    public Object[][] getContents()
        {
        return resources;
        }

    static final Object[][] resources =
        {
        // XML Tokenizer resources
        {XmlTokenizer.ERR_INTERNAL      , "An internal error has occurred in the XML tokenizer."},
        {XmlTokenizer.ERR_UNEXPECTED_EOF, "Unexpected End-Of-File."},
        {XmlTokenizer.ERR_UNEXPECTED_IO , "Unexpected I/O error:  \"{0}\"."},
        {XmlTokenizer.ERR_XML_FORMAT    , "XML format error."},
        {XmlTokenizer.ERR_CHAR_EXPECTED , "Expected \"{0}\", encountered \"{1}\"."},
        };
    }
