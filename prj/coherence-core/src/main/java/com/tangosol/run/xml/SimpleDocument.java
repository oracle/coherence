/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.xml;


import com.tangosol.io.Utf8Writer;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import java.io.CharArrayWriter;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.PrintWriter;


/**
* A simple implementation of the XmlElement interface.  Protected methods are
* provided to support inheriting classes.
*
* @author cp  2000.10.20
*/
public class SimpleDocument
        extends SimpleElement
        implements XmlDocument, Externalizable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct an empty SimpleDocument.
    *
    * Note: this constructor is needed <b>only</b> to comply with the
    * requirements for the Externalizable and ExternalizableLite interfaces.
    */
    public SimpleDocument()
        {
        super();
        }

    /**
    * Construct a SimpleDocument.
    *
    * @param sName  the name of the root element
    */
    public SimpleDocument(String sName)
        {
        super(sName);
        }

    /**
    * Construct a SimpleDocument.
    *
    * @param sName     the name of the root element
    * @param sDtdUri   the URI of the DTD (system identifier)
    * @param sDtdName  the name of the DTD (public identifier); may be null
    */
    public SimpleDocument(String sName, String sDtdUri, String sDtdName)
        {
        super(sName);
        setDtdUri(sDtdUri);
        setDtdName(sDtdName);
        }


    // ----- XmlDocument interface -------------------------------------------

    /**
    * Get the URI of the DTD (DOCTYPE) for the document. This is referred to
    * as the System Identifier by the XML specification.
    *
    * For example:
    *   http://java.sun.com/j2ee/dtds/web-app_2_2.dtd
    *
    * @return the document type URI
    */
    public String getDtdUri()
        {
        return m_sDtdUri;
        }

    /**
    * Set the URI of the DTD (DOCTYPE) for the document. This is referred to
    * as the System Identifier by the XML specification.
    *
    * @param sUri  the document type URI
    */
    public void setDtdUri(String sUri)
        {
        checkMutable();
        m_sDtdUri = sUri;
        }

    /**
    * Get the public identifier of the DTD (DOCTYPE) for the document.
    *
    * For example:
    *   -//Sun Microsystems, Inc.//DTD Web Application 1.2//EN
    *
    * @return the DTD public identifier
    */
    public String getDtdName()
        {
        return m_sDtdName;
        }

    /**
    * Set the public identifier of the DTD (DOCTYPE) for the document.
    *
    * @param sName  the DTD public identifier
    */
    public void setDtdName(String sName)
        {
        checkMutable();

        if (sName != null && sName.length() == 0)
            {
            sName = null;
            }

        if (sName != null && !XmlHelper.isPublicIdentifierValid(sName))
            {
            throw new IllegalArgumentException(
                    "illegal xml dtd public id: " + sName);
            }

        m_sDtdName = sName;
        }

    /**
    * Get the encoding string for the XML document. Documents that are parsed
    * may or may not have the encoding string from the persistent form of the
    * document.
    *
    * @return the encoding set for the document
    */
    public String getEncoding()
        {
        return m_sEncoding;
        }

    /**
    * Set the encoding string for the XML document.
    *
    * @param sEncoding  the encoding that the document will use
    */
    public void setEncoding(String sEncoding)
        {
        checkMutable();

        if (sEncoding != null && sEncoding.length() == 0)
            {
            sEncoding = null;
            }

        if (sEncoding != null && !XmlHelper.isEncodingValid(sEncoding))
            {
            throw new IllegalArgumentException(
                    "illegal xml document encoding: " + sEncoding);
            }

        m_sEncoding = sEncoding;
        }

    /**
    * Get the XML comment that appears outside of the root element. This
    * differs from the Comment property of this object, which refers to
    * the comment within the root element.
    *
    * @return the document comment
    */
    public String getDocumentComment()
        {
        return m_sComment;
        }

    /**
    * Set the XML comment that appears outside of the root element. This
    * differs from the Comment property of this object, which refers to
    * the comment within the root element.
    *
    * @param sComment  the document comment
    */
    public void setDocumentComment(String sComment)
        {
        checkMutable();

        if (sComment != null && sComment.length() == 0)
            {
            sComment = null;
            }

        if (sComment != null && !XmlHelper.isCommentValid(sComment))
            {
            throw new IllegalArgumentException(
                    "illegal xml comment: " + sComment);
            }

        m_sComment = sComment;
        }

    /**
    * Write the XML document, including an XML header and DOCTYPE if one
    * exists. This overrides the contract of the XmlElement super interface.
    *
    * @param out      a PrintWriter object to use to write to
    * @param fPretty  true to specify that the output is intended to be as
    *                 human readable as possible
    */
    public void writeXml(PrintWriter out, boolean fPretty)
        {
        String sDtdUri   = getDtdUri();
        String sDtdName  = getDtdName();
        String sEncoding = getEncoding();
        String sComment  = getDocumentComment();

        out.print("<?xml version='1.0'");

        if (sEncoding != null && sEncoding.length() > 0)
            {
            out.print(" encoding=" + XmlHelper.quote(sEncoding));
            }

        out.print("?>");

        if (fPretty)
            {
            out.println();
            }

        if (sDtdUri != null && sDtdUri.length() > 0)
            {
            out.print("<!DOCTYPE " + getName() + ' ');

            if (sDtdName != null && sDtdName.length() > 0)
                {
                out.print("PUBLIC");

                if (fPretty)
                    {
                    out.println();
                    }

                out.print(' ');

                out.print(XmlHelper.quote(sDtdName));
                }
            else
                {
                out.print("SYSTEM");
                }

            if (fPretty)
                {
                out.println();
                }

            out.print(' ');

            out.print(XmlHelper.quote(XmlHelper.encodeUri(sDtdUri)));

            out.print('>');

            if (fPretty)
                {
                out.println();
                }
            else
                {
                out.print(' ');
                }
            }

        if (sComment != null && sComment.length() > 0)
            {
            out.print("<!--");

            if (fPretty)
                {
                out.println();
                out.println(breakLines(sComment, 78, ""));
                }
            else
                {
                out.print(sComment);
                }

            out.print("-->");

            if (fPretty)
                {
                out.println();
                }
            else
                {
                out.print(' ');
                }
            }

        super.writeXml(out, fPretty);

        if (fPretty)
            {
            out.println();
            }
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Provide a hash value for this XML document and all of its contained
    * information.  Note that this overrides the contract of the hashCode
    * method in the super interface XmlElement.  The hash value is defined
    * as a xor of the following:
    * <ol>
    * <li> the hashCode from the root element
    * <li> the hashCode from the document type (uri and optional name)
    * </ol>
    * @return the hash value for this XML document
    */
    public int hashCode()
        {
        int n = super.hashCode();

        String sUri = getDtdUri();
        if (sUri != null && sUri.length() > 0)
            {
            n ^= sUri.hashCode();

            String sName = getDtdName();
            if (sName != null && sName.length() > 0)
                {
                n ^= sName.hashCode();
                }
            }

        return n;
        }

    /**
    * Compare this XML document and all of its contained information with
    * another XML document for equality.  Note that this overrides the
    * contract of the equals method in the super interface XmlElement.
    *
    * @return true if the documents are equal, false otherwise
    */
    public boolean equals(Object o)
        {
        if (o instanceof XmlDocument)
            {
            XmlDocument that = (XmlDocument) o;
            if (!super.equals(that))
                {
                return false;
                }

            return equals(this.getDtdUri()         , that.getDtdUri()         )
                && equals(this.getDtdName()        , that.getDtdName()        )
                && equals(this.getEncoding()       , that.getEncoding()       )
                && equals(this.getDocumentComment(), that.getDocumentComment());
            }

        return false;
        }

    /**
    * Creates and returns a copy of this XmlDocument.
    *
    * @return  a clone of this instance.
    */
    public Object clone()
        {
        // there is no deep cloning that must be performed at this level
        return super.clone();
        }


    // ----- Externalizable interface ---------------------------------------

    /**
    * The object implements the writeExternal method to save its contents
    * by calling the methods of DataOutput for its primitive values or
    * calling the writeObject method of ObjectOutput for objects, strings,
    * and arrays.
    *
    * @param out the stream to write the object to
    *
    * @exception IOException Includes any I/O exceptions that may occur
    */
    public void writeExternal(ObjectOutput out)
            throws IOException
        {
        CharArrayWriter cw = new CharArrayWriter(1024);
        PrintWriter     pw = new PrintWriter(cw);

        writeXml(pw, false);
        pw.flush();

        out.writeInt(cw.size());
        cw.writeTo(new Utf8Writer((OutputStream) out));
        }

    /**
    * The object implements the readExternal method to restore its
    * contents by calling the methods of DataInput for primitive
    * types and readObject for objects, strings and arrays.  The
    * readExternal method must read the values in the same sequence
    * and with the same types as were written by writeExternal.
    *
    * @param in the stream to read data from in order to restore the object
    *
    * @exception IOException if I/O errors occur
    * @exception ClassNotFoundException If the class for an object being
    *              restored cannot be found.
    */
    public void readExternal(ObjectInput in)
            throws IOException, ClassNotFoundException
        {
        char[] ach = readCharArray(in);

        // Bug 31045382 - Do not validate the XML to prevent XXE (XML eXternal Entity) injection
        XmlHelper.loadXml(new String(ach), this, /* fValidate */ false);
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        if (m_sDtdUri != null || m_sDtdName != null || m_sEncoding != null || m_sComment != null)
            {
            throw new NotActiveException();
            }

        super.readExternal(in);

        if (in.readBoolean())
            {
            m_sDtdUri = readUTF(in);
            }

        if (in.readBoolean())
            {
            m_sDtdName = readUTF(in);
            }

        if (in.readBoolean())
            {
            m_sEncoding = readUTF(in);
            }

        if (in.readBoolean())
            {
            m_sComment = readUTF(in);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        super.writeExternal(out);

        String sDtdUri   = m_sDtdUri;
        String sDtdName  = m_sDtdName;
        String sEncoding = m_sEncoding;
        String sComment  = m_sComment;

        boolean fDtdUri   = sDtdUri   != null;
        boolean fDtdName  = sDtdName  != null;
        boolean fEncoding = sEncoding != null;
        boolean fComment  = sComment  != null;

        out.writeBoolean(fDtdUri);
        if (fDtdUri)
            {
            writeUTF(out, m_sDtdUri);
            }

        out.writeBoolean(fDtdName);
        if (fDtdName)
            {
            writeUTF(out, m_sDtdName);
            }

        out.writeBoolean(fEncoding);
        if (fEncoding)
            {
            writeUTF(out, m_sEncoding);
            }

        out.writeBoolean(fComment);
        if (fComment)
            {
            writeUTF(out, m_sComment);
            }
        }


    // ----- PortableObject interface --------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        if (m_sDtdUri != null || m_sDtdName != null || m_sEncoding != null || m_sComment != null)
            {
            throw new NotActiveException();
            }

        super.readExternal(in);

        if (in.readBoolean(12))
            {
            m_sDtdUri = in.readString(13);
            }

        if (in.readBoolean(14))
            {
            m_sDtdName = in.readString(15);
            }

        if (in.readBoolean(16))
            {
            m_sEncoding = in.readString(17);
            }

        if (in.readBoolean(18))
            {
            m_sComment = in.readString(19);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        super.writeExternal(out);

        String sDtdUri   = m_sDtdUri;
        String sDtdName  = m_sDtdName;
        String sEncoding = m_sEncoding;
        String sComment  = m_sComment;

        boolean fDtdUri   = sDtdUri   != null;
        boolean fDtdName  = sDtdName  != null;
        boolean fEncoding = sEncoding != null;
        boolean fComment  = sComment  != null;

        out.writeBoolean(12, fDtdUri);
        if (fDtdUri)
            {
            out.writeString(13, m_sDtdUri);
            }

        out.writeBoolean(14, fDtdName);
        if (fDtdName)
            {
            out.writeString(15, m_sDtdName);
            }

        out.writeBoolean(16, fEncoding);
        if (fEncoding)
            {
            out.writeString(17, m_sEncoding);
            }

        out.writeBoolean(18, fComment);
        if (fComment)
            {
            out.writeString(19, m_sComment);
            }
        }


    // ----- XmlSerializable interface --------------------------------------

    /**
    * Serialize the object into an XmlElement.
    *
    * @return an XmlElement that contains the serialized form of the object
    */
    public XmlElement toXml()
        {
        return super.toXml();
        }

    /**
    * Deserialize the object from an XmlElement.
    *
    * This method can throw one of several RuntimeExceptions.
    *
    * @param xml  an XmlElement that contains the serialized form of the
    *             object
    *
    * @throws UnsupportedOperationException if this element is immutable
    */
    public void fromXml(XmlElement xml)
        {
        super.fromXml(xml);

        if (xml instanceof XmlDocument)
            {
            XmlDocument xmlDoc = (XmlDocument) xml;

            m_sDtdUri   = xmlDoc.getDtdUri();
            m_sDtdName  = xmlDoc.getDtdName();
            m_sEncoding = xmlDoc.getEncoding();
            m_sComment  = xmlDoc.getComment();
            }
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Validates that the document is mutable, otherwise throws an
    * UnsupportedOperationException.
    *
    * @exception UnsupportedOperationException  if the document is immutable
    */
    protected void checkMutable()
        {
        if (!isMutable())
            {
            throw new UnsupportedOperationException(
                "document \"" + getName() + "\" is not mutable");
            }
        }


    // ----- data members ---------------------------------------------------

    private String      m_sDtdUri;
    private String      m_sDtdName;
    private String      m_sEncoding;
    private String      m_sComment;
    }
