/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.examples.rest.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Sample Product class used for REST examples.
 *
 * @author  tam 2015.07.06
 * @since 12.2.1
 */
@XmlRootElement(name="product")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Product
        implements Serializable
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public Product()
        {
        }

    /**
     * Create a new Product.
     *
     * @param nProductId  id of the prodcut
     * @param sName       product name
     * @param nPrice      product price
     * @param sDeptCode   department code
     * @param nQtyOnHand  quantity on hand
     */
    public Product(int nProductId, String sName, double nPrice, String sDeptCode, int nQtyOnHand)
        {
        m_nProductId = nProductId;
        m_sName      = sName;
        m_nPrice     = nPrice;
        m_sDeptCode  = sDeptCode;
        m_nQtyOnHand = nQtyOnHand;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the product id.
     *
     * @return  the product id.
     */
    public int getProductId()
        {
        return m_nProductId;
        }

    /**
     * Set the product id.
     *
     * @param nProductId  the product id
     */
    public void setProductId(int nProductId)
        {
        m_nProductId = nProductId;
        }

    /**
     * Return the product name.
     *
     * @return  the product name.
     */
    public String getName()
        {
        return m_sName;
        }

    /**
     * Set the prodcut name.
     *
     * @param sName  the product name
     */
    public void setName(String sName)
        {
        m_sName = sName;
        }

    /**
     * Return the product price.
     *
     * @return  the product price
     */
    public double getPrice()
        {
        return m_nPrice;
        }

    /**
     * Set the product price.
     *
     * @param nPrice  the product price
     */
    public void setPrice(double nPrice)
        {
        m_nPrice = nPrice;
        }

    /**
     * Return the department code.
     *
     * @return  the department code
     */
    public String getDeptCode()
        {
        return m_sDeptCode;
        }

    /**
     * Set the department code.
     *
     * @param sDeptCode  the department code
     */
    public void setDeptCode(String sDeptCode)
        {
        m_sDeptCode = sDeptCode;
        }

    /**
     * Return the quantity on hand.
     *
     * @return  the quantity on hand
     */
    public int getQtyOnHand()
        {
        return m_nQtyOnHand;
        }

    /**
     * Set the quantity on hand.
     *
     * @param nQtyOnHand  the quantity on hand
     */
    public void setQtyOnHand(int nQtyOnHand)
        {
        m_nQtyOnHand = nQtyOnHand;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Product id.
     */
    private int m_nProductId;

    /**
     * Product name.
     */
    private String m_sName;

    /**
     * Price.
     */
    private double m_nPrice;

    /**
     * Department.
     */
    private String m_sDeptCode;

    /**
     * Quantity on hand.
     */
    private int m_nQtyOnHand;
    }
