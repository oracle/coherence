/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.lang;


import com.oracle.coherence.common.schema.AbstractPropertyHandler;
import com.oracle.coherence.common.schema.PropertyHandler;
import com.oracle.coherence.common.schema.Schema;
import com.oracle.coherence.common.schema.TypeDescriptor;
import com.oracle.coherence.common.schema.util.StringUtils;
import com.oracle.coherence.common.schema.util.XmlUtils;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * A base class for {@link PropertyHandler} implementations that read and write
 * property metadata from/to Java class file.
 *
 * @param <T>   the type of property class that is handled by this {@link
 *              PropertyHandler}
 * @param <TD>  the type of type descriptor used to represent property type
 *
 * @author as  2013.11.20
 */
@SuppressWarnings("unchecked")
public class XmlPropertyHandler<T extends AbstractLangProperty<TD>, TD extends TypeDescriptor>
        extends AbstractPropertyHandler<T, Element>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct an {@code XmlPropertyHandler} instance.
     */
    public XmlPropertyHandler(String ns)
        {
        m_ns = ns;

        java.lang.reflect.Type superclass = getClass().getGenericSuperclass();
        this.m_typeDescriptorClass = (Class<TD>)
                ((ParameterizedType) superclass).getActualTypeArguments()[1];
        }

    // ---- PropertyHandler implementation ----------------------------------

    @Override
    public void importProperty(T property, Element source, Schema schema)
        {
        Element xmlProperty = XmlUtils.getChildElement(source, getNS(), "property");
        if (xmlProperty != null)
            {
            String type = xmlProperty.getAttributeNS(getNS(), "type");
            property.setType(parseTypeName(type));

            importPropertyInternal(property, source, xmlProperty, schema);
            }
        }

    @Override
    public void exportProperty(T property, Element target, Schema schema)
        {
        if (property.getType() != null)
            {
            Document doc = target.getOwnerDocument();
            doc.getDocumentElement().setAttribute("xmlns:" + getPrefix(), getNS());

            Element xmlProperty = doc.createElementNS(getNS(), getPrefix() + ":property");
            xmlProperty.setAttribute("type", property.getType().toString());
            target.appendChild(xmlProperty);

            exportPropertyInternal(property, target, xmlProperty, schema);
            }
        }

    // ---- helper methods --------------------------------------------------

    /**
     * Enables subclasses to provide additional processing during import from
     * XML (typically used to read additional, non-standard metadata from the
     * source element being imported).
     *
     * @param property     the property to import into
     * @param source       the root, canonical XML element for the property
     * @param xmlProperty  the extension-specific sub-element for the property
     * @param schema       the schema imported property belongs to
     */
    protected void importPropertyInternal(T property, Element source, Element xmlProperty, Schema schema)
        {
        }

    /**
     * Enables subclasses to provide additional processing during export into
     * XML (typically used to write additional, non-standard metadata into the
     * target element being exported).
     *
     * @param property     the property to export
     * @param target       the root, canonical XML element for the property
     * @param xmlProperty  the extension-specific sub-element for the property
     * @param schema       the schema exported property belongs to
     */
    protected void exportPropertyInternal(T property, Element target, Element xmlProperty, Schema schema)
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
