/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;


import java.io.OutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * A {@link SchemaExporter} implementation that writes type and property metadata
 * to an XML file.
 *
 * @author as  2013.11.21
 */
@SuppressWarnings("unchecked")
public class XmlSchemaExporter
        extends AbstractSchemaExporter<Element, Element>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code XmlSchemaExporter} instance that will write schema
     * metadata to the console ({@code System.out}).
     */
    public XmlSchemaExporter()
        {
        this(System.out);
        }

    /**
     * Construct {@code XmlSchemaExporter} instance.
     *
     * @param outputStream  the output XML stream to write schema metadata to
     */
    public XmlSchemaExporter(OutputStream outputStream)
        {
        m_outputStream = outputStream;
        }

    // ---- SchemaExporter implementation -----------------------------------

    @Override
    public void export(Schema schema)
        {
        try
            {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();

            Document doc = db.newDocument();
            Element root = doc.createElementNS(NS, "schema");
            doc.appendChild(root);

            for (ExtensibleType t : schema)
                {
                Element type = doc.createElementNS(NS, "type");
                exportType(t, type, schema);

                for (TypeHandler handler : schema.getTypeHandlers(getExternalTypeClass()))
                    {
                    Type ext = t.getExtension(handler.getInternalTypeClass());
                    handler.exportType(ext, type, schema);
                    }

                for (ExtensibleProperty p : t.getProperties())
                    {
                    Element property = doc.createElementNS(NS, "property");
                    exportProperty(p, property, schema);

                    for (PropertyHandler handler : schema.getPropertyHandlers(getExternalPropertyClass()))
                        {
                        Property ext = p.getExtension(handler.getInternalPropertyClass());
                        handler.exportProperty(ext, property, schema);
                        }

                    type.appendChild(property);
                    }

                root.appendChild(type);
                }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount","2");

            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(m_outputStream);
            transformer.transform(source, result);
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        }

    // ---- TypeHandler implementation --------------------------------------

    @Override
    public Class<Element> getExternalTypeClass()
        {
        return Element.class;
        }

    @Override
    public void exportType(ExtensibleType type, Element target, Schema schema)
        {
        target.setAttribute("name", type.getFullName());
        }

    // ---- PropertyHandler implementation ----------------------------------

    @Override
    public Class<Element> getExternalPropertyClass()
        {
        return Element.class;
        }

    @Override
    public void exportProperty(ExtensibleProperty property, Element target, Schema schema)
        {
        target.setAttribute("name", property.getName());
        target.setAttribute("type", property.getType().toString());
        }

    // ---- static members --------------------------------------------------

    private static final String NS = "http://xmlns.oracle.com/coherence/schema";

    // ---- data members ----------------------------------------------------

    private OutputStream m_outputStream;
    }
