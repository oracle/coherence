/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;


import com.oracle.coherence.common.base.Classes;
import com.oracle.coherence.common.schema.util.StringUtils;
import com.oracle.coherence.common.schema.util.XmlUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * A {@link SchemaSource} implementation that reads type and property metadata
 * from an XML file.
 *
 * @author as  2013.07.11
 */
public class XmlSchemaSource
        extends AbstractSchemaSource<Element, Element>
    {
    // ---- Constructors ----------------------------------------------------

    /**
     * Construct {@code XmlSchemaSource} instance.
     *
     * @param fileName  the name of the file or classpath resource to populate
     *                  the schema from
     *
     * @throws IllegalArgumentException  if the specified XML file cannot be found
     */
    public XmlSchemaSource(String fileName)
        {
        File file = new File(fileName);
        try
            {
            m_inputXml = file.exists()
                         ? new FileInputStream(file)
                         : Classes.getContextClassLoader().getResourceAsStream(fileName);
            }
        catch (FileNotFoundException ignore)
            {
            // should never happen, as we check for the file existence first,
            // but even if it does it will be handled below
            }

        if (m_inputXml == null)
            {
            throw new IllegalArgumentException(
                    String.format("The specified XML file %s cannot be found", fileName));
            }
        }

    /**
     * Construct {@code XmlSchemaSource} instance.
     *
     * @param file  the file to populate the schema from
     *
     * @throws IllegalArgumentException  if the specified XML file cannot be found
     */
    public XmlSchemaSource(File file)
        {
        try
            {
            m_inputXml = new FileInputStream(file);
            }
        catch (FileNotFoundException e)
            {
            throw new IllegalArgumentException(
                    String.format("The specified XML file %s cannot be found", file.getAbsolutePath()), e);
            }
        }

    /**
     * Construct {@code XmlSchemaSource} instance.
     *
     * @param inputXml  the input XML stream to populate the schema from
     */
    public XmlSchemaSource(InputStream inputXml)
        {
        m_inputXml = inputXml;
        }

    // ---- SchemaSource implementation ---------------------------------------

    @Override
    public void populateSchema(Schema schema)
        {
        try
            {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);

            DocumentBuilder db        = dbf.newDocumentBuilder();
            Document        doc       = db.parse(m_inputXml);
            Element         root      = doc.getDocumentElement();
            boolean         fExternal = XmlUtils.getBooleanAttribute(root, "external");
            List<Element>   types     = XmlUtils.toElementList(root.getElementsByTagName("type"));

            for (Element t : types)
                {
                String         fullName = CanonicalTypeDescriptor.parse(t.getAttribute("name")).getFullName();
                ExtensibleType type     = populateTypeInternal(schema, schema.getType(fullName), t);
                type.setExternal(fExternal);
                schema.addType(type);
                }
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        }

    // ---- AbstractSchemaSource implementation -------------------------------

    @Override
    protected String getPropertyName(Element source)
        {
        return source.getAttribute("name");
        }

    @Override
    protected Collection<Element> getProperties(Element source)
        {
        return XmlUtils.toElementList(source.getElementsByTagName("property"));
        }

    // ---- TypeHandler implementation --------------------------------------

    @Override
    public Class<Element> getExternalTypeClass()
        {
        return Element.class;
        }

    @Override
    public void importType(ExtensibleType type, Element source, Schema schema)
        {
        String name = source.getAttribute("name");
        type.setDescriptor(CanonicalTypeDescriptor.parse(name));

        // optional attributes
        String base = source.getAttribute("base");
        if (!StringUtils.isEmpty(base))
            {
            type.setBase(CanonicalTypeDescriptor.parse(base));
            }

        if (source.hasAttribute("external"))
            {
            boolean fExternal = Boolean.parseBoolean(source.getAttribute("external"));
            if (!StringUtils.isEmpty(base))
                {
                type.setExternal(fExternal);
                }
            }

        List<Element> interfaces = XmlUtils.toElementList(source.getElementsByTagName("interface"));
        for (Element e : interfaces)
            {
            name = e.getAttribute("name");
            type.addInterface(CanonicalTypeDescriptor.parse(name));
            }
        }

    @Override
    public void exportType(ExtensibleType type, Element target, Schema schema)
        {
        }

    // ---- PropertyHandler implementation ----------------------------------

    @Override
    public Class<Element> getExternalPropertyClass()
        {
        return Element.class;
        }

    @Override
    public void importProperty(ExtensibleProperty property, Element source, Schema schema)
        {
        property.setName(source.getAttribute("name"));
        property.setType(CanonicalTypeDescriptor.parse(source.getAttribute("type")));
        }

    @Override
    public void exportProperty(ExtensibleProperty property, Element target, Schema schema)
        {
        }

    // ---- data members ----------------------------------------------------

    protected InputStream m_inputXml;
    }
