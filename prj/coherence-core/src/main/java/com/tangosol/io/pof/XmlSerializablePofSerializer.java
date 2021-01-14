/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlSerializable;
import com.tangosol.run.xml.SimpleParser;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;


/**
* {@link PofSerializer} implementation that supports the serialization and
* deserialization of any class that implements {@link XmlSerializable} to
* and from a POF stream. This implementation is provided to ease migration
* of XmlSerializable implementations to support the POF stream format.
*
* @author cp  2006.07.31
*
* @since Coherence 3.2
*/
public class XmlSerializablePofSerializer
        extends PofHelper
        implements PofSerializer
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new XmlSerializablePofSerializer for the user type with the
    * given type identifier.
    *
    * @param nTypeId  the user type identifier
    */
    public XmlSerializablePofSerializer(int nTypeId)
        {
        azzert(nTypeId >= 0, "user type identifier cannot be negative");
        m_nTypeId = nTypeId;
        }


    // ----- PofSerializer interface ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public void serialize(PofWriter out, Object o)
            throws IOException
        {
        XmlSerializable xmlser;
        try
            {
            xmlser = (XmlSerializable) o;
            }
        catch (ClassCastException e)
            {
            String sClass = null;
            try
                {
                sClass = out.getPofContext().getClassName(m_nTypeId);
                }
            catch (Exception eIgnore) {}

            String sActual = null;
            try
                {
                sActual = o.getClass().getName();
                }
            catch (Exception eIgnore) {}

            throw new IOException(
                    "An exception occurred writing an XmlSerializable"
                    + " user type to a POF stream: type-id=" + m_nTypeId
                    + (sClass == null ? "" : ", class-name=" + sClass)
                    + (sActual == null ? "" : ", actual class-name=" + sActual)
                    + ", exception=\n" + e);
            }

        // write out the object's properties
        XmlElement   xml     = xmlser.toXml();
        StringWriter writer  = new StringWriter();
        PrintWriter  printer = new PrintWriter(writer);
        xml.writeXml(printer, false);
        printer.close();
        out.writeString(0, writer.toString());
        out.writeRemainder(null);
        }

    /**
    * {@inheritDoc}
    */
    public Object deserialize(PofReader in)
            throws IOException
        {
        // create a new instance of the user type
        XmlSerializable xmlser;
        try
            {
            xmlser = (XmlSerializable) in.getPofContext()
                    .getClass(m_nTypeId).newInstance();
            in.registerIdentity(xmlser);
            }
        catch (Exception e)
            {
            String sClass = null;
            try
                {
                sClass = in.getPofContext().getClassName(m_nTypeId);
                }
            catch (Exception eIgnore) {}

            throw new IOException(
                    "An exception occurred instantiating an XmlSerializable"
                    + " user type from a POF stream: type-id=" + m_nTypeId
                    + (sClass == null ? "" : ", class-name=" + sClass)
                    + ", exception=\n" + e);
            }

        // read the object's properties
        String     sXml = in.readString(0);

        // Bug 32341371 - Do not validate the XML to prevent XXE (XML eXternal Entity) injection
        XmlElement xml  = new SimpleParser(/* fValidate */ false).parseXml(sXml);
        xmlser.fromXml(xml);
        in.readRemainder();

        return xmlser;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The type identifier of the user type to serialize and deserialize.
    */
    protected final int m_nTypeId;
    }
