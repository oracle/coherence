/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.lang;


import com.oracle.coherence.common.schema.AbstractTypeHandler;
import com.oracle.coherence.common.schema.Property;
import com.oracle.coherence.common.schema.Schema;
import com.oracle.coherence.common.schema.TypeDescriptor;
import com.oracle.coherence.common.schema.TypeHandler;
import com.oracle.coherence.common.schema.util.StringUtils;
import com.oracle.coherence.common.schema.util.XmlUtils;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * A base class for {@link TypeHandler} implementations that read and write
 * type metadata from/to XML file.
 *
 * @param <T>   the type of the {@link com.oracle.coherence.common.schema.Type} class
 *              that is handled by this {@link TypeHandler}
 * @param <TD>  the type of the type descriptor used by the type {@code <T>}
 *
 * @author as  2013.11.20
 */
@SuppressWarnings("unchecked")
public class XmlTypeHandler
        <
            T extends AbstractLangType<? extends Property, TD>,
            TD extends TypeDescriptor
        >
        extends AbstractTypeHandler<T, Element>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct a {@code XmlTypeHandler} instance.
     */
    public XmlTypeHandler(String ns)
        {
        m_ns = ns;

        java.lang.reflect.Type superclass = getClass().getGenericSuperclass();
        this.m_typeDescriptorClass = (Class<TD>)
                ((ParameterizedType) superclass).getActualTypeArguments()[1];
        }

    // ---- TypeHandler implementation --------------------------------------

    @Override
    public void importType(T type, Element source, Schema schema)
        {
        Element xmlType = XmlUtils.getChildElement(source, getNS(), "type");
        if (xmlType != null)
            {
            String name = xmlType.getAttribute("name");
            type.setDescriptor(parseTypeName(name));

            List<Element> aliases = XmlUtils.toElementList(
                    xmlType.getElementsByTagNameNS(getNS(), "alias"));
            for (Element i : aliases)
                {
                name = i.getAttribute("name");
                type.addAlias(parseTypeName(name));
                }

            importTypeInternal(type, source, xmlType, schema);
            }
        }

    @Override
    public void exportType(T type, Element target, Schema schema)
        {
        if (type.getDescriptor() != null)
            {
            Document doc = target.getOwnerDocument();
            doc.getDocumentElement().setAttribute("xmlns:" + getPrefix(), getNS());

            Element xmlType = doc.createElementNS(getNS(), getPrefix() + ":type");
            xmlType.setAttribute("name", type.getDescriptor().getFullName());

            for (TD i : type.getAliases())
                {
                Element xmlInterface = doc.createElementNS(getNS(), getPrefix() + ":alias");
                xmlInterface.setAttribute("name", i.getFullName());
                xmlType.appendChild(xmlInterface);
                }

            exportTypeInternal(type, doc, target, xmlType, schema);
            target.appendChild(xmlType);
            }
        }

    // ---- helper methods --------------------------------------------------

    /**
     * Enables subclasses to provide additional processing during import from
     * XML (typically used to read additional, non-standard metadata from the
     * source element being imported).
     *
     * @param type     the type to import into
     * @param source   the root, canonical XML element for the type
     * @param xmlType  the extension-specific sub-element for the type
     * @param schema   the schema imported type belongs to
     */
    protected void importTypeInternal(T type, Element source, Element xmlType, Schema schema)
        {
        }

    /**
     * Enables subclasses to provide additional processing during export into
     * XML (typically used to write additional, non-standard metadata into the
     * target element being exported).
     *
     * @param type     the type to export
     * @param target   the root, canonical XML element for the type
     * @param xmlType  the extension-specific sub-element for the type
     * @param schema   the schema exported type belongs to
     */
    protected void exportTypeInternal(T type, Document doc, Element target, Element xmlType, Schema schema)
        {
        }

    /**
     * Parse specified platform-specific type name and return the
     * {@link TypeDescriptor} that corresponds to it.
     *
     * @param name  the type name to parse
     *
     * @return the {@link TypeDescriptor} for the specified type name
     */
    protected TD parseTypeName(String name)
        {
        try
            {
            if (m_parseMethod == null)
                {
                m_parseMethod = m_typeDescriptorClass.getMethod("parse", String.class);
                }
            return (TD) m_parseMethod.invoke(m_typeDescriptorClass, name);
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        }

    /**
     * Return the namespace prefix used within element names.
     *
     * @return the namespace prefix used within element names
     */
    protected String getPrefix()
        {
        if (m_nsPrefix == null)
            {
            String[] ns = StringUtils.split(m_ns, "/");
            m_nsPrefix = ns[ns.length-1];
            }
        return m_nsPrefix;
        }

    /**
     * Return the XML namespace this handler is responsible for.
     *
     * @return the XML namespace this handler is responsible for
     */
    protected String getNS()
        {
        return m_ns;
        }

    // ---- data members ----------------------------------------------------

    private String m_nsPrefix;
    private final String m_ns;
    private Class<TD> m_typeDescriptorClass;
    private Method m_parseMethod;
    }
