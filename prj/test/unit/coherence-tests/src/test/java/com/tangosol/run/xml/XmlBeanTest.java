/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.xml;


import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.SafeHashMap;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;


/**
* A collection of unit tests for {@link XmlBean}.
*
* @author jh  2006.01.16
*/
public class XmlBeanTest
        extends Base
    {
    // ----- test methods ---------------------------------------------------

    /**
    * Test an XmlBean with a property that has been declared as one of its
    * superclasses.
    */
    @Test
    public void narrowProperty()
        {
        NarrowBean   bean  = new NarrowBean();
        Integer[]    aI    = new Integer[2];
        ArrayList    list  = new ArrayList();
        SafeHashMap  map   = new SafeHashMap();

        aI[0] = Integer.MIN_VALUE;
        aI[1] = Integer.MAX_VALUE;

        list.add(Boolean.FALSE);
        list.add(Boolean.TRUE);

        map.put("0", "0");
        map.put("1", "1");

        bean.setIntegerArray(aI);
        bean.setList(list);
        bean.setSafeHashMap(map);

        Binary     binBean = ExternalizableHelper.toBinary(bean);
        NarrowBean bean2   = (NarrowBean) ExternalizableHelper.fromBinary(binBean);

        assertEquals(bean, bean2);
        }


    // ----- NarrowBean inner class -----------------------------------------

    /**
    * XmlBean class used by the {@link XmlBeanTest#narrowProperty()} test.
    */
    public static class NarrowBean
            extends XmlBean
        {
        // ----- constructors ---------------------------------------------

        /**
        * Default constructor.
        */
        public NarrowBean()
            {
            }

        // ----- accessors ------------------------------------------------

        /**
        * Return the Integer array for this NarrowBean
        * <p/>
        * The type of this property has been declared as java.lang.Object[]
        * in the XmlBean descriptor.
        *
        * @return the Integer array for this NarrowBean
        */
        public Integer[] getIntegerArray()
            {
            return m_aI;
            }

        /**
        * @see #getIntegerArray()
        */
        public void setIntegerArray(Integer[] aI)
            {
            m_aI = aI;
            }

        /**
        * Return the List for this NarrowBean
        * <p/>
        * The type of this property has been declared as java.util.Collection
        * in the XmlBean descriptor.
        *
        * @return the List for this NarrowBean
        */
        public List getList()
            {
            return m_list;
            }

        /**
        * @see #getList()
        */
        public void setList(List list)
            {
            m_list = list;
            }

        /**
        * Return the SafeHashMap for this NarrowBean.
        * <p/>
        * The type of this property has been declared as java.util.Map in the
        * XmlBean descriptor.
        *
        * @return the SafeHashMap for this NarrowBean
        */
        public SafeHashMap getSafeHashMap()
            {
            return m_map;
            }

        /**
        * @see #getSafeHashMap()
        */
        public void setSafeHashMap(SafeHashMap map)
            {
            m_map = map;
            }

        // ----- data members ---------------------------------------------

        /**
        * The Integer array for this NarrowBean.
        */
        private Integer[] m_aI;

        /**
        * The List for this NarrowBean.
        */
        private List m_list;

        /**
        * The SafeHashMap for this NarrowBean.
        */
        private SafeHashMap m_map;
        }
    }
