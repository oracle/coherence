/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.xml;


import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.IndentingWriter;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.SimpleMapEntry;

import java.io.CharArrayWriter;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;


/**
* A simple implementation of the XmlElement interface.  Protected methods are
* provided to support inheriting classes.
*
* @author cp  2000.10.20
*/
public class SimpleElement
        extends SimpleValue
        implements XmlElement, XmlSerializable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct an empty SimpleElement.
    *
    * Note: this constructor is needed <b>only</b> to comply with the
    * requirements for the Externalizable and ExternalizableLite interfaces.
    */
    public SimpleElement()
        {
        super();
        }

    /**
    * Construct a SimpleElement.
    *
    * @param sName  the name of the element
    */
    public SimpleElement(String sName)
        {
        this(sName, null);
        }

    /**
    * Construct a SimpleElement.
    *
    * @param sName   the name of the element
    * @param oValue  an initial value for this element
    */
    public SimpleElement(String sName, Object oValue)
        {
        super(oValue);
        setName(sName);
        }


    // ----- XmlElement interface -------------------------------------------

    /**
    * Get the name of the element.
    *
    * @return the element name
    */
    public String getName()
        {
        return m_qName == null ? null : m_qName.getName();
        }

    /**
    * Set the Name of the element.  This method is intended primarily to be
    * utilized to configure a newly instantiated element before adding it as
    * a child element to another element.
    *
    * Implementations of this interface that support read-only documents are
    * expected to throw UnsupportedOperationException from this method if the
    * document (or this element) is in a read-only state.
    *
    * If this XmlElement has a parent XmlElement, then the implementation of
    * this interface is permitted to throw UnsupportedOperationException from
    * this method.  This results from typical document implementations in
    * which the name of an element that is a child of another element is
    * immutable; the W3C DOM interfaces are one example.
    *
    * @param sName  the new element name
    *
    * @throws IllegalArgumentException  if the name is null or if the name is
    *         not a legal XML tag name
    * @throws UnsupportedOperationException if the element cannot be renamed
    */
    public void setName(String sName)
        {
        if (!isNameMutable())
            {
            throw new UnsupportedOperationException("the element cannot be renamed");
            }

        if (!XmlHelper.isNameValid(sName))
            {
            throw new IllegalArgumentException(
                    "illegal name \"" + sName + "\"; see XML 1.0 2ed section 2.3 [5]");
            }

        m_qName = new QualifiedName(sName);
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public QualifiedName getQualifiedName()
        {
        return m_qName;
        }

    /**
    * Get the root element.
    *
    * This is a convenience method.  Parent element is retrived using
    * getParent().
    *
    * @return the root element for this element
    */
    public XmlElement getRoot()
        {
        XmlElement parent = getParent();
        return parent == null ? this : parent.getRoot();
        }

    /**
    * Get the '/'-delimited path of the element starting from the
    * root element.
    *
    * This is a convenience method.  Elements are retrieved by simple name
    * using getName().
    *
    * @return the element path
    */
    public String getAbsolutePath()
        {
        return XmlHelper.getAbsolutePath(this);
        }

    /**
    * Get the list of all child elements.  The contents of the list implement
    * the XmlValue interface.  If this XmlElement is mutable, then the list
    * returned from this method is expected to be mutable as well.
    *
    * An element should be fully configured before it is added to the list:
    * <ul>
    * <li>  The List implementation is permitted (and most implementations
    *       are expected) to instantiate its own copy of any XmlElement
    *       objects added to it.
    * <li>  Certain properties of an element (such as Name) may not be
    *       settable once the element has been added.  (See the comments for
    *       the setName method.)
    * </ul>
    * @return a List containing all elements of this XmlElement
    */
    public List getElementList()
        {
        List list = m_listChildren;
        if (list == null)
            {
            m_listChildren = list = instantiateElementList();
            }
        return list;
        }

    /**
    * Get a child element.
    *
    * This is a convenience method.  Elements are accessed and manipulated
    * via the list returned from getElementList().
    *
    * If multiple child elements exist that have the specified name, then
    * the behavior of this method is undefined, and it is permitted to return
    * any one of the matching elements, to return null, or to throw an
    * arbitrary runtime exception.
    *
    * @return the specified element as an object implementing XmlElement, or
    *         null if the specified child element does not exist
    */
    public XmlElement getElement(String sName)
        {
        return XmlHelper.getElement(this, sName);
        }

    /**
    * Get an iterator of child elements that have a specific name.
    *
    * This is a convenience method.  Elements are accessed and manipulated
    * via the list returned from getElementList().
    *
    * @return an iterator containing all child elements of the specified name
    */
    public Iterator getElements(String sName)
        {
        return new ElementIterator(sName);
        }

    /**
    * Create a new element and add it as a child element to this element.
    *
    * This is a convenience method.  Elements are accessed and manipulated
    * via the list returned from getElementList().
    *
    * @param sName  the name for the new element
    *
    * @return the new XmlElement object
    *
    * @throws IllegalArgumentException  if the name is null or if the name is
    *         not a legal XML tag name
    * @throws UnsupportedOperationException if this element is immutable or
    *         otherwise cannot add a child element
    */
    public XmlElement addElement(String sName)
        {
        XmlElement element = instantiateElement(sName, null);
        getElementList().add(element);
        return element;
        }

    /**
    * Find a child element with the specified '/'-delimited path.  This is
    * based on a subset of the XPath specification, supporting:
    * <ol>
    * <li> Leading '/' to specify root
    * <li> Use of '/' as a path delimiter
    * <li> Use of '..' to specify parent
    * </ol>
    * This is a convenience method.  Elements are accessed and manipulated
    * via the list returned from getElementList().
    *
    * If multiple child elements exist that have the specified name, then
    * the behavior of this method is undefined, and it is permitted to return
    * any one of the matching elements, to return null, or to throw an
    * arbitrary runtime exception.
    *
    * @return the specified element as an object implementing XmlElement, or
    *         null if the specified child element does not exist
    */
    public XmlElement findElement(String sPath)
        {
        return XmlHelper.findElement(this, sPath);
        }

    /**
    * Return the specified child element using the same path notation as
    * supported by findElement, but return a read-only element if the
    * specified element does not exist.
    *
    * <b>This method never returns null.</b>
    *
    * This is a convenience method.  Elements are accessed and manipulated
    * via the list returned from getElementList().
    *
    * If multiple child elements exist that have the specified name, then
    * the behavior of this method is undefined, and it is permitted to return
    * any one of the matching elements, to return null, or to throw an
    * arbitrary runtime exception.
    *
    * @param  sPath element path
    *
    * @return the specified element (never null) as an object implementing
    *         XmlElement for read-only use
    */
    public XmlElement getSafeElement(String sPath)
        {
        azzert(sPath != null);

        if (sPath.length() == 0)
            {
            return this;
            }

        if (sPath.charAt(0) == '/')
            {
            return getRoot().getSafeElement(sPath.substring(1));
            }

        // get the first name from the path
        int of = sPath.indexOf('/');
        String sName;
        String sRemain;
        if (of < 0)
            {
            sName   = sPath;
            sRemain = null;
            }
        else
            {
            sName   = sPath.substring(0, of);
            sRemain = sPath.substring(of+1);
            }

        // check if going "up" (..) or "down" (child name)
        XmlElement element;
        if (sName.equals(".."))
            {
            element = getParent();
            if (element == null)
                {
                throw new IllegalArgumentException("Invalid path " + sPath);
                }
            }
        else
            {
            element = getElement(sName);
            if (element == null)
                {
                // create a temporary "safe" element (read-only)
                element = instantiateElement(sName, null);

                // parent (this) does not know its new safe child (element)
                // because this is a "read-only" operation; however, the child
                // does know its parent so it can answer pathed questions etc.
                element.setParent(this);

                // child is marked read-only if it supports it
                if (element instanceof SimpleElement)
                    {
                    ((SimpleElement) element).setMutable(false);
                    }
                }
            }

        return sRemain == null ? element : element.getSafeElement(sRemain);
        }

    /**
    * Ensure that a child element exists.
    *
    * This is a convenience method.  It combines the functionality of
    * findElement() and addElement(). If any part of the path does not exist
    * create new child elements to match the path.
    *
    * @param  sPath element path
    *
    * @return the existing or new XmlElement object
    *
    * @throws IllegalArgumentException  if the name is null or if any part
    *         of the path is not a legal XML tag name
    * @throws UnsupportedOperationException if any element in the path
    *         is immutable or otherwise cannot add a child element
    * @see #findElement
    */
    public XmlElement ensureElement(String sPath)
        {
        return XmlHelper.ensureElement(this, sPath);
        }

    /**
    * Get the map of all attributes.  The map is keyed by attribute names.
    * The corresponding values are non-null objects that implement the
    * XmlValue interface.
    *
    * @return a Map containing all attributes of this XmlElement; the return
    *         value will never be null, although it may be an empty map
    */
    public Map getAttributeMap()
        {
        Map map = m_mapAttributes;
        if (map == null)
            {
            m_mapAttributes = map = instantiateAttributeMap();
            }
        return map;
        }

    /**
    * Get an attribute value.
    *
    * This is a convenience method.  Attributes are accessed and manipulated
    * via the map returned from getAttributeMap.
    *
    * @param sName  the name of the attribute
    *
    * @return the value of the specified attribute, or null if the attribute
    *         does not exist or does not have a value
    */
    public XmlValue getAttribute(String sName)
        {
        if (sName == null || !XmlHelper.isNameValid(sName))
            {
            throw new IllegalArgumentException("Invalid attribute name [" + sName + "]");
            }

        return (XmlValue) getAttributeMap().get(sName);
        }

    /**
    * Set an attribute value.  If the attribute does not already exist, and
    * the new value is non-null, then the attribute is added and its value
    * is set to the passed value.  If the attribute does exist, and the new
    * value is non-null, then the attribute's value is updated to the passed
    * value.  If the attribute does exist, but the new value is null, then
    * the attribute and its corresponding value are removed.
    *
    * This is a convenience method.  Attributes are accessed and manipulated
    * via the map returned from getAttributeMap.
    *
    * @param sName  the name of the attribute
    * @param val    the new value for the attribute; null indicates that the
    *               attribute should be removed
    */
    public void setAttribute(String sName, XmlValue val)
        {
        Map map = getAttributeMap();
        if (val == null)
            {
            map.remove(sName);
            }
        else
            {
            map.put(sName, val);
            }
        }

    /**
    * Provides a means to add a new attribute value.  If the attribute
    * of the same name already exists, it is returned, otherwise a new
    * value is created and added as an attribute.
    *
    * This is a convenience method.  Attributes are accessed and manipulated
    * via the map returned from getAttributeMap.
    *
    * @param sName  the name of the attribute
    */
    public XmlValue addAttribute(String sName)
        {
        XmlValue attr = getAttribute(sName);
        if (attr == null)
            {
            attr = instantiateAttribute();
            setAttribute(sName, attr);
            }
        return attr;
        }

    /**
    * Get an attribute value, and return a temporary value if the attribute
    * does not exist.
    *
    * This is a convenience method.  Attributes are accessed and manipulated
    * via the map returned from getAttributeMap.
    *
    * @param sName  the name of the attribute
    *
    * @return the value of the specified attribute, or a temporary value if
    *         the attribute does not exist
    */
    public XmlValue getSafeAttribute(String sName)
        {
        XmlValue value = getAttribute(sName);
        if (value == null)
            {
            value = instantiateAttribute();
            value.setParent(this);
            if (value instanceof SimpleValue)
                {
                ((SimpleValue) value).setMutable(false);
                }
            }

        return value;
        }

    /**
    * Get the text of any comments that are in the XML element.  An element
    * can contain many comments interspersed randomly with textual values and
    * child elements.  In reality, comments are rarely used.  The purpose of
    * this method and the corresponding mutator are to ensure that if comments
    * do exist, that their text will be accessible through this interface and
    * not lost through a transfer from one instance of this interface to
    * another.
    *
    * @return the comment text from this element (not including the "<!--" and
    * "-->") or null if there was no comment
    */
    public String getComment()
        {
        return m_sComment;
        }

    /**
    * Set the text of this element's comment.  This interface allows a single
    * comment to be associated with the element.  The XML specification does
    * not allow a comment to contain the String "--".
    *
    * @param sComment  the comment text
    *
    * @throws IllegalArgumentException if the comment contains "--"
    */
    public void setComment(String sComment)
        {
        checkMutable();

        if (sComment.indexOf("--") >= 0)
            {
            throw new IllegalArgumentException(
                    "comment contains \"--\"; see XML 1.0 2ed section 2.5 [15]");
            }

        m_sComment = sComment;
        }

    /**
    * Write the element as it will appear in XML.
    *
    * @param out      a PrintWriter object to use to write to
    * @param fPretty  true to specify that the output is intended to be as
    *                 human readable as possible
    */
    public void writeXml(PrintWriter out, boolean fPretty)
        {
        String  sComment  = getComment();
        boolean fComment  = sComment != null && sComment.length() > 0;
        boolean fValue    = !isEmpty();
        boolean fChildren = !getElementList().isEmpty();

        if (!fComment && !fValue && !fChildren)
            {
            writeEmptyTag(out, fPretty);
            }
        else if (!fChildren)
            {
            writeStartTag(out, fPretty);
            writeComment(out, fPretty);
            writeValue(out, fPretty);
            writeEndTag(out, fPretty);
            }
        else
            {
            PrintWriter out2 = fPretty ?
                               new IndentingWriter(out, 2) : out;

            writeStartTag(out, fPretty);
            if (fPretty)
                {
                out.println();
                }

            if (fComment)
                {
                writeComment(out2, fPretty);
                if (fPretty)
                    {
                    out2.println();
                    }
                }

            if (fValue)
                {
                writeValue(out2, fPretty);
                if (fPretty)
                    {
                    out2.println();
                    }
                }

            writeChildren(out2, fPretty);
            if (fPretty)
                {
                out2.println();
                }

            out2.flush();

            writeEndTag(out, fPretty);
            }

        if (getParent() == null)
            {
            out.flush();
            }
        }

    /**
    * Write the value as it will appear in XML.
    *
    * @param out      a PrintWriter object to use to write to
    * @param fPretty  true to specify that the output is intended to be as
    *                 human readable as possible
    */
    public void writeValue(PrintWriter out, boolean fPretty)
        {
        if (fPretty && out instanceof IndentingWriter)
            {
            ((IndentingWriter) out).suspend();
            super.writeValue(out, fPretty);
            ((IndentingWriter) out).resume();
            }
        else
            {
            super.writeValue(out, fPretty);
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
        return isMutable() ? (XmlElement) clone() : this;
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
        checkMutable();

        xml = (XmlElement) xml.clone();

        setInternalValue(xml.getValue());

        m_qName         = xml.getQualifiedName();
        m_listChildren  = xml.getElementList();
        m_mapAttributes = xml.getAttributeMap();
        m_sComment      = xml.getComment();
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        if (m_qName != null)
            {
            throw new NotActiveException();
            }

        m_fDeserializing = true;
        try
            {
            super.readExternal(in);

            // element name
            if (in.readBoolean())
                {
                m_qName = new QualifiedName(readUTF(in));
                }

            // child elements
            if (in.readBoolean())
                {
                ((ExternalizableLite) getElementList()).readExternal(in);
                }

            // attributes
            if (in.readBoolean())
                {
                ((ExternalizableLite) getAttributeMap()).readExternal(in);
                }

            // element comment
            if (in.readBoolean())
                {
                m_sComment = readUTF(in);
                }
            }
        finally
            {
            m_fDeserializing = false;
            }
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        super.writeExternal(out);

        // element name
        String  sName = m_qName.getName();
        boolean fName = sName != null;
        out.writeBoolean(fName);
        if (fName)
            {
            writeUTF(out, sName);
            }

        // child elements
        List listKids = m_listChildren;
        boolean fKids = listKids != null && !listKids.isEmpty();
        out.writeBoolean(fKids);
        if (fKids)
            {
            ((ExternalizableLite) listKids).writeExternal(out);
            }

        // attributes
        Map mapAttr = m_mapAttributes;
        boolean fAttr = mapAttr != null && !mapAttr.isEmpty();
        out.writeBoolean(fAttr);
        if (fAttr)
            {
            ((ExternalizableLite) mapAttr).writeExternal(out);
            }

        // element comment
        String  sComment = m_sComment;
        boolean fComment = sComment != null;
        out.writeBoolean(fComment);
        if (fComment)
            {
            writeUTF(out, sComment);
            }
        }


    // ----- PortableObject interface --------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        if (m_qName != null)
            {
            throw new NotActiveException();
            }

        m_fDeserializing = true;
        try
            {
            super.readExternal(in);

            // element name
            if (in.readBoolean(4))
                {
                m_qName = new QualifiedName(in.readString(5));
                }

            // child elements
            if (in.readBoolean(6))
                {
                in.readCollection(7,
                    m_listChildren = instantiateElementList());
                }

            // attributes
            if (in.readBoolean(8))
                {
                in.readMap(9,
                    m_mapAttributes = instantiateAttributeMap());
                }

            // element comment
            if (in.readBoolean(10))
                {
                m_sComment = in.readString(11);
                }
            }
        finally
            {
            m_fDeserializing = false;
            }
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        super.writeExternal(out);

        // element name
        String  sName = m_qName.getName();
        boolean fName = sName != null;
        out.writeBoolean(4, fName);
        if (fName)
            {
            out.writeString(5, sName);
            }

        // child elements
        List listKids = m_listChildren;
        boolean fKids = listKids != null && !listKids.isEmpty();
        out.writeBoolean(6, fKids);
        if (fKids)
            {
            out.writeCollection(7, listKids);
            }

        // attributes
        Map mapAttr = m_mapAttributes;
        boolean fAttr = mapAttr != null && !mapAttr.isEmpty();
        out.writeBoolean(8, fAttr);
        if (fAttr)
            {
            out.writeMap(9, mapAttr);
            }

        // element comment
        String  sComment = m_sComment;
        boolean fComment = sComment != null;
        out.writeBoolean(10, fComment);
        if (fComment)
            {
            out.writeString(11, sComment);
            }
        }


    // ----- support for inheriting implementations -------------------------

    /**
    * Validates that the element is mutable, otherwise throws an
    * UnsupportedOperationException.
    *
    * @exception UnsupportedOperationException  if the document is immutable
    */
    protected void checkMutable()
        {
        if (!isMutable())
            {
            throw new UnsupportedOperationException(
                "element \"" + getAbsolutePath() + "\" is not mutable");
            }
        }

    /**
    * Determine if the name can be changed.  The default implementation
    * allows a name to be changed.  This can be overridden by inheriting
    * implementations.
    *
    * @return true if the name can be changed
    */
    protected boolean isNameMutable()
        {
        return m_fDeserializing || isMutable();
        }

    /**
    * Instantiate a List implementation that will hold child elements.
    *
    * @return a List that supports XmlElements
    */
    protected List instantiateElementList()
        {
        return new ElementList();
        }

    /**
    * Instantiate an XmlElement implementation for an element.
    *
    * @param sName   element name
    * @param oValue  element value
    * @return a new XmlElement to be used as an element
    */
    protected XmlElement instantiateElement(String sName, Object oValue)
        {
        return new SimpleElement(sName, oValue);
        }

    /**
    * Instantiate a Map implementation that will support the name to value
    * map used to hold attributes.
    *
    * @return a Map that supports String keys and XmlValue values
    */
    protected Map instantiateAttributeMap()
        {
        return new AttributeMap();
        }

    /**
    * Instantiate an XmlValue implementation for an attribute value.
    *
    * @return a new XmlValue to be used as an attribute value
    */
    protected XmlValue instantiateAttribute()
        {
        return new SimpleValue(null, true);
        }


    // ----- XML generation -------------------------------------------------

    /**
    * Write the element as a combined start/end tag.
    *
    * @param out      a PrintWriter object to use to write to
    * @param fPretty  true to specify that the output is intended to be as
    *                 human readable as possible
    */
    protected void writeEmptyTag(PrintWriter out, boolean fPretty)
        {
        out.print('<');
        out.print(getName());
        writeAttributes(out, fPretty);
        out.print("/>");
        }

    /**
    * Write the element's start tag.
    *
    * @param out      a PrintWriter object to use to write to
    * @param fPretty  true to specify that the output is intended to be as
    *                 human readable as possible
    */
    protected void writeStartTag(PrintWriter out, boolean fPretty)
        {
        out.print('<');
        out.print(getName());
        writeAttributes(out, fPretty);
        out.print('>');
        }

    /**
    * Write the element's end tag.
    *
    * @param out      a PrintWriter object to use to write to
    * @param fPretty  true to specify that the output is intended to be as
    *                 human readable as possible
    */
    protected void writeEndTag(PrintWriter out, boolean fPretty)
        {
        out.print("</");
        out.print(getName());
        out.print(">");
        }

    /**
    * Write the attributes as part of a start tag.
    *
    * @param out      a PrintWriter object to use to write to
    * @param fPretty  true to specify that the output is intended to be as
    *                 human readable as possible
    */
    protected void writeAttributes(PrintWriter out, boolean fPretty)
        {
        for (Iterator iter = getAttributeMap().entrySet().iterator(); iter.hasNext(); )
            {
            Map.Entry entry = (Map.Entry) iter.next();
            String   sName = (String)   entry.getKey();
            XmlValue value = (XmlValue) entry.getValue();
            out.print(' ');
            out.print(sName);
            out.print('=');
            value.writeValue(out, fPretty);
            }
        }

    /**
    * Write the element as it will appear in XML.
    *
    * @param out      a PrintWriter object to use to write to
    * @param fPretty  true to specify that the output is intended to be as
    *                 human readable as possible
    */
    protected void writeComment(PrintWriter out, boolean fPretty)
        {
        String sComment = getComment();
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
            }
        }

    /**
    * Write the element as it will appear in XML.
    *
    * @param out      a PrintWriter object to use to write to
    * @param fPretty  true to specify that the output is intended to be as
    *                 human readable as possible
    */
    protected void writeChildren(PrintWriter out, boolean fPretty)
        {
        List    list   = getElementList();
        boolean fFirst = true;
        for (Iterator iter = list.iterator(); iter.hasNext(); )
            {
            if (fFirst)
                {
                fFirst = false;
                }
            else if (fPretty)
                {
                out.println();
                }

            ((XmlElement) iter.next()).writeXml(out, fPretty);
            }
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Format the XML element and all its contained information into a String
    * in a display format.  Note that this overrides the contract of the
    * toString method in the super interface XmlValue.
    *
    * @return a String representation of the XML element
    */
    public String toString()
        {
        return toString(true);
        }

    /**
    * Format the XML element and all its contained information into a String
    * in a display format.
    *
    * @param fPretty  true to specify that the output is intended to be as
    *                 human readable as possible
    *
    * @return a String representation of the XML element
    */
    public String toString(boolean fPretty)
        {
        CharArrayWriter writer0 = new CharArrayWriter();
        PrintWriter     writer1 = new PrintWriter(writer0);

        writeXml(writer1, fPretty);
        writer1.flush();

        return writer0.toString();
        }

    /**
    * Provide a hash value for this XML element and all of its contained
    * information.  Note that this overrides the contract of the hashCode
    * method in the super interface XmlValue.  The hash value is defined
    * as a xor of the following:
    * <ol>
    * <li> the hashCode from the element's value (i.e. super.hashCode())
    * <li> the hashCode from each attribute name
    * <li> the hashCode from each attribute value
    * <li> the hashCode from each sub-element
    * </ol>
    * @return the hash value for this XML element
    */
    public int hashCode()
        {
        return XmlHelper.hashElement(this);
        }

    /**
    * Compare this XML element and all of its contained information with
    * another XML element for equality.  Note that this overrides the
    * contract of the equals method in the super interface XmlValue.
    *
    * @return true if the elements are equal, false otherwise
    */
    public boolean equals(Object o)
        {
        if (!(o instanceof XmlElement))
            {
            return false;
            }

        return XmlHelper.equalsElement(this, (XmlElement) o);
        }

    /**
    * Creates and returns a copy of this SimpleElement.
    *
    * The returned copy is a deep clone of this SimpleElement
    * "unlinked" from the parent and mutable
    *
    * @return  a clone of this instance.
    */
    public Object clone()
        {
        SimpleElement that = (SimpleElement) super.clone();

        Map mapThat = that.instantiateAttributeMap();
        for (Iterator iter = getAttributeMap().entrySet().iterator(); iter.hasNext(); )
            {
            Map.Entry entry = (Map.Entry) iter.next();
            String    sName = (String)   entry.getKey();
            XmlValue  value = (XmlValue) entry.getValue();

            mapThat.put(sName, value.clone());
            }
        that.m_mapAttributes = mapThat;

        List listThat = that.instantiateElementList();
        for (Iterator iter = getElementList().iterator(); iter.hasNext(); )
            {
            XmlElement el = (XmlElement) iter.next();

            listThat.add(el.clone());
            }
        that.m_listChildren = listThat;

        return that;
        }


    // ----- ElementList inner class ----------------------------------------

    /**
    * An implementation of List that only supports XmlElements as the content
    * of the List.
    */
    protected class ElementList
            extends ArrayList
            implements ExternalizableLite
        {
        /**
        * Replaces the element at the specified position in this list with
        * the specified element.
        *
        * @param index index of element to replace.
        * @param element element to be stored at the specified position.
        * @return the element previously at the specified position.
        * @throws    IndexOutOfBoundsException if index out of range
        *		  <tt>(index &lt; 0 || index &gt;= size())</tt>.
        */
        public Object set(int index, Object element)
            {
            checkMutable();
            return super.set(index, checkElement((XmlElement) element));
            }

        /**
        * Appends the specified element to the end of this list.
        *
        * @param o element to be appended to this list.
        * @return <tt>true</tt> (as per the general contract of Collection.add).
        */
        public boolean add(Object o)
            {
            checkMutable();
            return super.add(checkElement((XmlElement) o));
            }

        /**
        * Inserts the specified element at the specified position in this
        * list. Shifts the element currently at that position (if any) and
        * any subsequent elements to the right (adds one to their indices).
        *
        * @param index index at which the specified element is to be inserted.
        * @param element element to be inserted.
        * @throws    IndexOutOfBoundsException if index is out of range
        *		  <tt>(index &lt; 0 || index &gt; size())</tt>.
        */
        public void add(int index, Object element)
            {
            checkMutable();
            super.add(index, checkElement((XmlElement) element));
            }

        /**
        * Removes the element at the specified position in this list.
        * Shifts any subsequent elements to the left (subtracts one from their
        * indices).
        *
        * @param index the index of the element to removed.
        * @return the element that was removed from the list.
        * @throws    IndexOutOfBoundsException if index out of range <tt>(index
        * 		  &lt; 0 || index &gt;= size())</tt>.
        */
        public Object remove(int index)
            {
            checkMutable();
            return super.remove(index);
            }

        /**
        * Removes all of the elements from this list.  The list will
        * be empty after this call returns.
        */
        public void clear()
            {
            checkMutable();
            super.clear();
            }

        /**
        * Overrides the ArrayList implementation to force this throw the single
        * object add() method.
        */
        public boolean addAll(Collection c)
            {
            return addAll(size(), c);
            }

        /**
        * Overrides the ArrayList implementation to force this throw the single
        * object add() method.
        */
        public boolean addAll(int index, Collection c)
            {
            boolean fModified = false;
            Iterator iterator = c.iterator();
            while (iterator.hasNext())
                {
                add(index++, iterator.next());
                fModified = true;
                }
            return fModified;
            }

        /**
        * Validates the passed XmlElement, copying the element into a new valid
        * element if necessary.
        *
        * @param element  the XML element
        *
        * @return a XML element, copying if necessary
        */
        protected XmlElement checkElement(XmlElement element)
            {
            // element must not have a parent
            if (element.getParent() != null)
                {
                // copy name and value
                XmlElement elementNew = instantiateElement(
                        element.getName(), element.getValue());

                // copy comment
                String sComment = element.getComment();
                if (sComment != null)
                    {
                    elementNew.setComment(sComment);
                    }

                // copy attributes
                Map map  = element.getAttributeMap();
                if (!map.isEmpty())
                    {
                    elementNew.getAttributeMap().putAll(map);
                    }

                // copy child elements
                List list = element.getElementList();
                if (!list.isEmpty())
                    {
                    elementNew.getElementList().addAll(list);
                    }

                element = elementNew;
                }

            element.setParent(SimpleElement.this);
            return element;
            }

        public void readExternal(DataInput in)
                throws IOException
            {
            SimpleElement xmlThis = SimpleElement.this;

            int c = readInt(in);
            for (int i = 0; i < c; ++i)
                {
                SimpleElement xmlKid = new SimpleElement();
                xmlKid.readExternal(in);
                xmlKid.setParent(xmlThis);
                super.add(xmlKid);
                }
            }

        public void writeExternal(DataOutput out)
                throws IOException
            {
            int c = size();
            writeInt(out, c);

            for (int i = 0; i < c; ++i)
                {
                ((ExternalizableLite) get(i)).writeExternal(out);
                }
            }
        }


    // ----- ElementIterator inner class ------------------------------------

    /**
    * Provides an Iterator implementation that exposes only those Elements
    * from the Element list that match a certain name.
    */
    protected class ElementIterator
            implements Iterator
        {
        public ElementIterator(String sName)
            {
            m_sName = sName;
            }

        public boolean hasNext()
            {
            if (m_nState == FOUND)
                {
                return true;
                }

            Iterator iterator = m_iterator;
            while (iterator.hasNext())
                {
                XmlElement element = (XmlElement) iterator.next();
                if (element.getName().equals(m_sName))
                    {
                    m_element = element;
                    m_nState  = FOUND;
                    return true;
                    }
                }

            return false;
            }

        public Object next()
            {
            switch (m_nState)
                {
                case RETURNED:
                case REMOVED:
                    if (!hasNext())
                        {
                        throw new NoSuchElementException();
                        }
                case FOUND:
                    m_nState = RETURNED;
                    return m_element;
                default:
                    throw new IllegalStateException();
                }
            }

        public void remove()
            {
            if (m_nState != RETURNED)
                {
                throw new IllegalStateException();
                }

            m_nState = REMOVED;
            m_iterator.remove();
            }

        protected String     m_sName;
        protected Iterator   m_iterator = getElementList().iterator();
        protected XmlElement m_element;
        protected int        m_nState   = REMOVED;

        protected static final int FOUND    = 0;
        protected static final int RETURNED = 1;
        protected static final int REMOVED  = 2;
        }


    // ----- AttributeMap inner class ---------------------------------------

    /**
    * A Map implementation using a list that can be a read-only map
    * that supports only Strings for keys and XmlValue for values.
    * <p>
    * As of Coherence 3.2, this is an inlined version of the old ListMap.
    */
    public class AttributeMap
            extends AbstractMap
            implements Cloneable, Serializable, ExternalizableLite
        {
        public AttributeMap()
            {
            }

        // ----- Map interface -----------------------------------------

        public int size()
            {
            return m_cEntries;
            }

        public boolean containsKey(Object key)
            {
            return findEntry(key) != null;
            }

        public Object get(Object key)
            {
            Entry entry = findEntry(key);
            return entry == null ? null : entry.getValue();
            }

        public Object put(Object key, Object value)
            {
            Entry  entry = findEntry(key);
            if (entry != null)
                {
                return entry.setValue(value);
                }

            entry = instantiateEntry(key, value);
            if (m_entryTail == null)
                {
                m_entryHead = entry;
                }
            else
                {
                m_entryTail.setNextEntry(entry);
                }

            m_entryTail = entry;
            ++m_cEntries;
            return null;
            }

        public Object remove(Object key)
            {
            checkMutable();

            Entry entry     = m_entryHead;
            Entry entryPrev = null;

            // find the desired entry as well as the entry from which it is linked
            while (entry != null)
                {
                Object oEntryKey = entry.getKey();
                if (oEntryKey == null ? key == null : oEntryKey.equals(key))
                    {
                    break;
                    }

                entryPrev = entry;
                entry = entry.getNextEntry();
                }

            // check if no such entry found
            if (entry == null)
                {
                return null;
                }

            // check if it was the head
            if (entry == m_entryHead)
                {
                m_entryHead = entry.getNextEntry();
                }

            // check if it was the tail
            if (entry == m_entryTail)
                {
                m_entryTail = entryPrev;
                }

            // link around it
            if (entryPrev != null)
                {
                entryPrev.setNextEntry(entry.getNextEntry());
                }

            --m_cEntries;
            return entry.getValue();
            }

        public void clear()
            {
            checkMutable();

            m_entryHead  = null;
            m_entryTail  = null;
            m_cEntries   = 0;
            }

        public Set entrySet()
            {
            Set set = m_setEntries;
            if (set == null)
                {
                m_setEntries = set = instantiateEntrySet();
                }
            return set;
            }

        // ----- Object methods -----------------------------------------

        public boolean equals(Object o)
            {
            if (o instanceof Map)
                {
                Map that = (Map) o;
                if (this.size() != that.size())
                    {
                    return false;
                    }

                Iterator iterThis = this.entrySet().iterator();
                Iterator iterThat = that.entrySet().iterator();
                try
                    {
                    while (iterThis.hasNext())
                        {
                        if (!iterThis.next().equals(iterThat.next()))
                            {
                            return false;
                            }
                        }

                    return true;
                    }
                catch (ConcurrentModificationException e) {}
                }

            return false;
            }

        public Object clone()
            {
            AttributeMap that = new AttributeMap();
            for (Iterator iter = this.entrySet().iterator(); iter.hasNext(); )
                {
                Entry entry = (Entry) iter.next();
                that.put(entry.getKey(), entry.getValue());
                }
            return that;
            }

        // ----- Serializable interface ---------------------------------

        /**
        * Write this object to an ObjectOutputStream.
        *
        * @param out  the ObjectOutputStream to write this object to
        *
        * @throws IOException  thrown if an exception occurs writing this
        *                      object
        */
        private synchronized void writeObject(ObjectOutputStream out)
                throws IOException
            {
            Entry entry   = m_entryHead;
            int  cEntries = m_cEntries;
            int  cCheck = 0;

            out.writeInt(cEntries);
            while (entry != null)
                {
                out.writeObject(entry.getKey());
                out.writeObject(entry.getValue());
                entry = entry.getNextEntry();
                ++cCheck;
                }

            if (cCheck != cEntries)
                {
                throw new IOException("expected to write " + cEntries
                    + " entries but actually wrote " + cCheck);
                }
            }

        /**
        * Read this object from an ObjectInputStream.
        *
        * @param in  the ObjectInputStream to read this object from
        *
        * @throws IOException  if an exception occurs reading this object
        * @throws ClassNotFoundException  if the class for an object being
        *         read cannot be found
        */
        private void readObject(ObjectInputStream in)
                throws IOException, ClassNotFoundException
            {
            int   cEntries  = in.readInt();
            Entry entryPrev = null;
            for (int i = 0; i < cEntries; ++i)
                {
                Entry entryNext = instantiateEntry(in.readObject(), in.readObject());

                if (entryPrev == null)
                    {
                    m_entryHead = entryNext;
                    }
                else
                    {
                    entryPrev.setNextEntry(entryNext);
                    }

                entryPrev = entryNext;
                }

            m_cEntries  = cEntries;
            m_entryTail = entryPrev;
            }

        // ----- ExternalizableLite interface ---------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(DataInput in)
                throws IOException
            {
            int c = readInt(in);
            for (int i = 0; i < c; ++i)
                {
                String      sName = readUTF(in);
                SimpleValue value = new SimpleValue();
                value.readExternal(in);
                put(sName, value);
                }
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(DataOutput out)
                throws IOException
            {
            int c = size();
            writeInt(out, c);

            if (c > 0)
                {
                int cActual = 0;
                for (Iterator iter = entrySet().iterator(); iter.hasNext(); )
                    {
                    Map.Entry entry = (Map.Entry) iter.next();
                    writeUTF(out, (String) entry.getKey());
                    ((SimpleValue) entry.getValue()).writeExternal(out);
                    ++cActual;
                    }
                if (c != cActual)
                    {
                    throw new IOException("expected to write " + c
                            + " attributes; actually wrote " + cActual);
                    }
                }
            }

        // ----- helpers ------------------------------------------------

        /**
        * Instantiate an Entry instance.  This method permits inheriting
        * classes to easily override the implementation of Entity.
        *
        * @param oKey  the key
        * @param oVal  the value
        *
        * @return the instantiated Entry
        */
        protected Entry instantiateEntry(Object oKey, Object oVal)
            {
            return new Entry(oKey, oVal);
            }

        /**
        * Find an entry with the specified key.
        *
        * @param oKey  the key to search for
        *
        * @return the entry with the specified key or null if the key could
        *         not be found
        */
        protected Entry findEntry(Object oKey)
            {
            Entry entry = m_entryHead;
            while (entry != null)
                {
                Object oEntryKey = entry.getKey();
                if (oEntryKey == null ? oKey == null : oEntryKey.equals(oKey))
                    {
                    break;
                    }
                entry = entry.getNextEntry();
                }

            return entry;
            }

        /**
        * Return the first entry in the list..
        *
        * @return the first entry in the list
        */
        protected Entry getFirstEntry()
            {
            return m_entryHead;
            }

        /**
        * Instantiate an Entry Set.  This method permits inheriting classes
        * to easily override the implementation of the Entity Set.
        *
        * @return the instantiated set of Entry's
        */
        protected Set instantiateEntrySet()
            {
            return new EntrySet();
            }

        // ----- inner class:  Entry ------------------------------------

        /**
        * An implementation of Entry that supports keeping them in a list.
        */
        public class Entry
                extends SimpleMapEntry
            {
            // ----- constructors -----------------------------------

            /**
            * Construct an Entry.
            *
            * @param oKey  the key
            * @param oVal  the value
            */
            protected Entry(Object oKey, Object oVal)
                {
                super(oKey);

                if (!(oKey instanceof String))
                    {
                    throw new IllegalArgumentException(
                            "attribute name must be a String");
                    }

                if (!XmlHelper.isNameValid((String) oKey))
                    {
                    throw new IllegalArgumentException("illegal name \""
                            + oKey + "\"; see XML 1.0 2ed section 2.3 [5]");
                    }

                setValue(oVal);
                }

            // ----- Map.Entry interface ----------------------------

            public Object setValue(Object value)
                {
                checkMutable();

                if (!(value instanceof XmlValue))
                    {
                    throw new IllegalArgumentException(
                            "attribute value must be an XmlValue");
                    }

                XmlValue xmlvalue = (XmlValue) value;
                if (xmlvalue.getParent() != null || !xmlvalue.isAttribute())
                    {
                    // clone the value as an attribute
                    xmlvalue = new SimpleValue(xmlvalue.getValue(), true);
                    }

                // set the parent of the value to this element
                xmlvalue.setParent(SimpleElement.this);

                // store the value
                return super.setValue(xmlvalue);
                }

            // ----- helpers --------------------------------------------

            /**
            * Get the next entry in the linked list
            *
            * @return the next entry in the linked list
            */
            protected Entry getNextEntry()
                {
                return m_entryNext;
                }

            /**
            * Set the next entry in the linked list.
            *
            * @param entry  the next entry in the linked list
            */
            protected void setNextEntry(Entry entry)
                {
                m_entryNext = entry;
                }

            // ----- data members -----------------------------------

            /**
            * Next entry in the linked list of entries.
            */
            private Entry  m_entryNext;
            }

        // ----- inner class:  EntrySet ---------------------------------

        /**
        * A Set implementation to hold Entry objects.
        */
        protected class EntrySet
                extends AbstractSet
                implements Serializable
            {
            public Iterator iterator()
                {
                return new EntrySetIterator();
                }

            public int size()
                {
                return AttributeMap.this.size();
                }

            /**
            * An Iterator over the Entry objects in the EntrySet.
            */
            protected class EntrySetIterator
                    implements Iterator
                {
                public boolean hasNext()
                    {
                    return m_entryNext != null;
                    }

                public Object next()
                    {
                    Entry entry = m_entryNext;
                    if (entry == null)
                        {
                        throw new NoSuchElementException();
                        }

                    m_entryPrev = entry;
                    m_entryNext = entry.getNextEntry();

                    return entry;
                    }

                public void remove()
                    {
                    Entry entry = m_entryPrev;
                    if (entry == null)
                        {
                        throw new IllegalStateException();
                        }

                    AttributeMap.this.remove(entry.getKey());
                    m_entryPrev = null;
                    }

                private Entry m_entryPrev;
                private Entry m_entryNext = AttributeMap.this.getFirstEntry();
                }
            }

        // ----- data members -------------------------------------------

        /**
        * Head of entry linked list.
        */
        private Entry m_entryHead;

        /**
        * Tail of linked list.
        */
        private Entry m_entryTail;

        /**
        * Number of entries in the linked list.
        */
        private int m_cEntries;

        /**
        * The entry set (lazy instantiated).
        */
        private transient Set m_setEntries;
        }


    // ----- data members ---------------------------------------------------

    private QualifiedName m_qName;
    private List          m_listChildren;
    private Map           m_mapAttributes;
    private String        m_sComment;

    private transient boolean m_fDeserializing;
    }
