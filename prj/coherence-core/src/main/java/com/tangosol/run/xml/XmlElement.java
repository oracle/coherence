/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.xml;


import java.io.PrintWriter;

import java.util.List;
import java.util.Map;
import java.util.Iterator;


/**
* An interface for XML element access.  The XmlElement interface represents both the
* element and its content (through the underlying XmlValue interface).
*
* @author cp  2000.10.12
*/
public interface XmlElement
        extends XmlValue
    {
    /**
    * Get the name of the element.
    *
    * @return the element name
    */
    public String getName();

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
    * @throws UnsupportedOperationException if the element can not be renamed
    */
    public void setName(String sName);

    /**
    * Obtains the {@link QualifiedName} of the {@link XmlElement}.
    *
    * @return the {@link QualifiedName}
    */
    public QualifiedName getQualifiedName();

    /**
    * Get the root element.
    *
    * This is a convenience method.  Parent element is retrived using
    * getParent().
    *
    * @return the root element for this element
    */
    public XmlElement getRoot();

    /**
    * Get the '/'-delimited path of the element starting from the
    * root element.
    *
    * This is a convenience method.  Elements are retrieved by simple name
    * using getName().
    *
    * @return the element path
    */
    public String getAbsolutePath();

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
    public List getElementList();

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
    * @param sName  the specified name
    *
    * @return the specified element as an object implementing XmlElement, or
    *         null if the specified child element does not exist
    */
    public XmlElement getElement(String sName);

    /**
    * Get an iterator of child elements that have a specific name.
    *
    * This is a convenience method.  Elements are accessed and manipulated
    * via the list returned from getElementList().
    *
    * @param sName  the specified name
    *
    * @return an iterator containing all child elements of the specified name
    */
    public Iterator getElements(String sName);

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
    *         otherwise can not add a child element
    */
    public XmlElement addElement(String sName);

    /**
    * Find a child element with the specified '/'-delimited path.  This is
    * based on a subset of the XPath specification, supporting:
    * <ul>
    * <li>  Leading '/' to specify root
    * <li>  Use of '/' as a path delimiter
    * <li>  Use of '..' to specify parent
    * </ul>
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
    * @return the specified element as an object implementing XmlElement, or
    *         null if the specified child element does not exist
    */
    public XmlElement findElement(String sPath);

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
    public XmlElement getSafeElement(String sPath);

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
    *         is immutable or otherwise can not add a child element
    * @see #findElement
    */
    public XmlElement ensureElement(String sPath);

    /**
    * Get the map of all attributes.  The map is keyed by attribute names.
    * The corresponding values are non-null objects that implement the
    * XmlValue interface.
    *
    * @return a Map containing all attributes of this XmlElement; the return
    *         value will never be null, although it may be an empty map
    */
    public Map getAttributeMap();

    /**
    * Get an attribute value.
    *
    * This is a convenience method.  Attributes are accessed and manipulated
    * via the map returned from getAttributeMap.
    *
    * @param sName  the name of the attribute
    *
    * @return the value of the specified attribute, or null if the attribute
    *         does not exist
    */
    public XmlValue getAttribute(String sName);

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
    public void setAttribute(String sName, XmlValue val);

    /**
    * Provides a means to add a new attribute value.  If the attribute
    * of the same name already exists, it is returned, otherwise a new
    * value is created and added as an attribute.
    *    
    * This is a convenience method.  Attributes are accessed and manipulated
    * via the map returned from getAttributeMap.
    *
    * @param sName  the name of the attribute
    *
    * @return a XmlValue of the same name if it already exists
    */
    public XmlValue addAttribute(String sName);

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
    public XmlValue getSafeAttribute(String sName);

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
    public String getComment();

    /**
    * Set the text of this element's comment.  This interface allows a single
    * comment to be associated with the element.  The XML specification does
    * not allow a comment to contain the String "--".
    *
    * @param sComment  the comment text
    *
    * @throws IllegalArgumentException if the comment contains "--"
    */
    public void setComment(String sComment);

    /**
    * Write the element as it will appear in XML.
    *
    * @param out      a PrintWriter object to use to write to
    * @param fPretty  true to specify that the output is intended to be as
    *                 human readable as possible
    */
    public void writeXml(PrintWriter out, boolean fPretty);


    // ----- Object methods -------------------------------------------------

    /**
    * Format the XML element and all its contained information into a String
    * in a display format.  Note that this overrides the contract of the
    * toString method in the super interface XmlValue.
    *
    * @return a String representation of the XML element
    */
    public String toString();

    /**
    * Provide a hash value for this XML element and all of its contained
    * information.  Note that this overrides the contract of the hashCode
    * method in the super interface XmlValue.  The hash value is defined
    * as a xor of the following:
    * <ul>
    * <li> the hashCode from the element's value (i.e. super.hashCode())
    * <li> the hashCode from each attribute name
    * <li> the hashCode from each attribute value
    * <li> the hashCode from each sub-element
    * </ul>
    * @return the hash value for this XML element
    */
    public int hashCode();

    /**
    * Compare this XML element and all of its contained information with
    * another XML element for equality.  Note that this overrides the
    * contract of the equals method in the super interface XmlValue.
    *
    * @return true if the elements are equal, false otherwise
    */
    public boolean equals(Object o);

    /**
    * Creates and returns a copy of this XmlElement.
    *
    * The returned copy is a deep clone of this XmlElement, and is
    * "unlinked" from the parent and mutable.
    *
    * @return  a clone of this instance.
    */
    public Object clone();
    }

