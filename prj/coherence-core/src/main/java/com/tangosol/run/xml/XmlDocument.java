/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.xml;


import java.io.PrintWriter;


/**
* An interface for XML document access.  The XmlDocumnet interface represents
* the document as both the root element (through the underlying XmlElement
* interface) and the properties specific to a document, such as DOCTYPE.
*
* @author cp  2001.07.11
*/
public interface XmlDocument
        extends XmlElement
    {
    /**
    * Get the URI of the DTD (DOCTYPE) for the document. This is referred to
    * as the System Identifier by the XML specification.
    *
    * For example:
    *   http://java.sun.com/j2ee/dtds/web-app_2_2.dtd
    *
    * @return the document type URI
    */
    public String getDtdUri();

    /**
    * Set the URI of the DTD (DOCTYPE) for the document. This is referred to
    * as the System Identifier by the XML specification.
    *
    * @param sUri  the document type URI
    */
    public void setDtdUri(String sUri);

    /**
    * Get the public identifier of the DTD (DOCTYPE) for the document.
    *
    * For example:
    *   -//Sun Microsystems, Inc.//DTD Web Application 1.2//EN
    *
    * @return the DTD public identifier
    */
    public String getDtdName();

    /**
    * Set the public identifier of the DTD (DOCTYPE) for the document.
    *
    * @param sName  the DTD public identifier
    */
    public void setDtdName(String sName);

    /**
    * Get the encoding string for the XML document. Documents that are parsed
    * may or may not have the encoding string from the persistent form of the
    * document.
    *
    * @return the encoding set for the document
    */
    public String getEncoding();

    /**
    * Set the encoding string for the XML document.
    *
    * @param sEncoding  the encoding that the document will use
    */
    public void setEncoding(String sEncoding);

    /**
    * Get the XML comment that appears outside of the root element. This
    * differs from the Comment property of this object, which refers to
    * the comment within the root element.
    *
    * @return the document comment
    */
    public String getDocumentComment();

    /**
    * Set the XML comment that appears outside of the root element. This
    * differs from the Comment property of this object, which refers to
    * the comment within the root element.
    *
    * @param sComment  the document comment
    */
    public void setDocumentComment(String sComment);

    /**
    * Write the XML document, including an XML header and DOCTYPE if one
    * exists. This overrides the contract of the XmlElement super interface.
    *
    * @param out      a PrintWriter object to use to write to
    * @param fPretty  true to specify that the output is intended to be as
    *                 human readable as possible
    */
    public void writeXml(PrintWriter out, boolean fPretty);


    // ----- Object methods -------------------------------------------------

    /**
    * Format the XML document and all its contained information into a String
    * in a display format.  Note that this overrides the contract of the
    * toString method in the super interface XmlElement.
    *
    * @return a String representation of the XML document
    */
    public String toString();

    /**
    * Provide a hash value for this XML document and all of its contained
    * information.  Note that this overrides the contract of the hashCode
    * method in the super interface XmlElement.  The hash value is defined
    * as a xor of the following:
    * <ul>
    * <li> the hashCode from the root element
    * <li> the hashCode from the document type
    * </ul>
    * @return the hash value for this XML document
    */
    public int hashCode();

    /**
    * Compare this XML document and all of its contained information with
    * another XML document for equality.  Note that this overrides the
    * contract of the equals method in the super interface XmlElement.
    *
    * @return true if the documents are equal, false otherwise
    */
    public boolean equals(Object o);

    /**
    * Creates and returns a copy of this XmlDocument.
    *
    * @return  a clone of this instance.
    */
    public Object clone();
    }

