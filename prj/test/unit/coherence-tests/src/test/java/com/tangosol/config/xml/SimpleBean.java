/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.xml;

import com.oracle.coherence.common.util.MemorySize;

import com.tangosol.coherence.config.unit.Millis;
import com.tangosol.coherence.config.unit.Seconds;

import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

/**
 * A {@link SimpleBean}.
 *
 * @author bo
 */
public class SimpleBean
    {
    /**
     * Obtains the boolean property.
     *
     * @return the boolean property
     */
    public boolean isBooleanProperty()
        {
        return m_fBooleanProperty;
        }

    /**
     * Sets the boolean property.
     *
     * @param booleanProperty  the new boolean property
     */
    @Injectable
    public void setBooleanProperty(boolean booleanProperty)
        {
        m_fBooleanProperty = booleanProperty;
        }

    /**
     * Returns the string property.
     *
     * @return the string property
     */
    public String getStringProperty()
        {
        return m_sStringProperty;
        }

    /**
     * Sets the string property.
     *
     * @param stringProperty  the string property
     */
    @Injectable("string-property")
    public void replaceStringProperty(String stringProperty)
        {
        m_sStringProperty = stringProperty;
        }

    /**
     * Obtains the {@link Switch} enum property.
     *
     * @return the {@link Switch} property
     */
    public Switch getEnumProperty()
        {
        return m_enumProperty;
        }

    /**
     * Sets the {@link Switch} enum property.
     *
     * @param enumProperty  the {@link Switch} property to set
     */
    @Injectable("enum-property")
    public void setEnumProperty(Switch enumProperty)
        {
        m_enumProperty = enumProperty;
        }

    /**
     * Obtains the {@link StringBuffer} property.
     *
     * @return {@link StringBuffer}
     */
    public StringBuffer getStringBufferProperty()
        {
        return m_bufStringBufferProperty;
        }

    /**
     * Sets the {@link StringBuffer} property.
     *
     * @param buffer  the {@link StringBuffer}
     */
    @Injectable("string-buffer")
    public void setStringBufferProperty(StringBuffer buffer)
        {
        m_bufStringBufferProperty = buffer;
        }

    /**
     * Obtains the property that will not be injected.
     *
     * @return a {@link String}
     */
    public String getNotInjectedProperty()
        {
        return m_sNotInjectedProperty;
        }

    /**
     * Sets the property that will not be injected.
     *
     * @param value  a value.
     */
    public void setNotInjectedProperty(String value)
        {
        m_sNotInjectedProperty = value;
        }

    /**
     * Sets the {@link List} of {@link Switch}es.
     *
     * @param listSwitches  the {@link List} of {@link Switch}es
     */
    @Injectable("switch-list")
    public void setSwitchList(List<Switch> listSwitches)
        {
        m_listSwitches = listSwitches;
        }

    /**
     * Sets the array of {@link Switch}es
     *
     * @param aSwitches  the array of {@link Switch}es
     */
    @Injectable("switch-array")
    public void setSwitchArray(Switch[] aSwitches)
        {
        m_listSwitches = Arrays.asList(aSwitches);
        }

    /**
     * Sets the {@link TreeSet} of {@link Switch}es
     *
     * @param treeSwitches  the {@link List} of {@link Switch}es
     */
    @Injectable("switch-treeset")
    public void setSwitchTreeSet(TreeSet<Switch> treeSwitches)
        {
        m_listSwitches = new ArrayList<Switch>(treeSwitches);
        }

    /**
     * Obtains an {@link Iterator} over the {@link Switch}es.
     *
     * @return an {@link Iterator} over {@link Switch}es
     */
    public Iterator<Switch> getSwitches()
        {
        return m_listSwitches.iterator();
        }

    /**
     * Sets the {@link MemorySize} as an {@link Expression}.
     *
     * @param exprMemorySize  the {@link MemorySize} {@link Expression}
     */
    @Injectable
    public void setMemorySize(Expression<MemorySize> exprMemorySize)
        {
        m_exprMemorySize = exprMemorySize;
        }

    /**
     * Obtains the {@link MemorySize} {@link Expression}.
     *
     * @return an {@link Expression} for a {@link MemorySize}
     */
    public Expression<MemorySize> getMemorySize()
        {
        return m_exprMemorySize;
        }

    /**
     * Sets the {@link Value} as an {@link Expression}.
     *
     * @param exprValue  the {@link Value} {@link Expression}
     */
    @Injectable
    public void setValue(Expression<Value> exprValue)
        {
        m_exprValue = exprValue;
        }

    /**
     * Obtains the {@link Value} {@link Expression}.
     *
     * @return an {@link Expression} for a {@link Value}
     */
    public Expression<Value> getValue()
        {
        return m_exprValue;
        }

    /**
     * Sets the {@link Seconds} for expiry time.
     *
     * @param seconds  the {@link Seconds}
     */
    @Injectable
    public void setExpiryTime(Seconds seconds)
        {
        m_expiryTime = seconds;
        }

    /**
     * Obtains the expiry time in {@link Seconds}.
     *
     * @return  the expiry time in {@link Seconds}
     */
    public Seconds getExpiryTime()
        {
        return m_expiryTime;
        }

    /**
     * Sets the session timeout as an {@link Expression} in {@link Millis}.
     *
     * @param exprSessionTimeout  the session time out
     */
    @Injectable
    public void setSessionTimeout(Expression<Millis> exprSessionTimeout)
        {
        m_exprSessionTimeout = exprSessionTimeout;
        }

    /**
     * Obtains the session timeout
     *
     * @return an {@link Expression} prepresenting the session timeout in {@link Millis}.
     */
    public Expression<Millis> getSessionTimeout()
        {
        return m_exprSessionTimeout;
        }

    /**
     * Obtains the OtherBean that of which is injectable.
     *
     * @return the OtherBean
     */
    @Injectable(".")
    public OtherBean getOtherBean()
        {
        return m_otherBean;
        }

    /**
     * Obtains the OtherBean that of which is injectable.
     *
     * @return the OtherBean
     */
    @Injectable("nested")
    public OtherBean getNestedOtherBean()
        {
        return m_nestedOtherBean;
        }

    // ----- Switch enum ----------------------------------------------------

    /**
     * A {@link Switch}.
     */
    public enum Switch
        {
        /**
         * The {@link Switch} is On.
         */
        On,

        /**
         * The {@link Switch} is Off.
         */
        Off
        }

    // ----- data fields ----------------------------------------------------

    /**
     * A string property that won't be injected.
     */
    private String m_sNotInjectedProperty = "notinjected";

    /**
     * A {@link StringBuffer} property.
     */
    private StringBuffer m_bufStringBufferProperty;

    /**
     * A {@link Switch} property.
     */
    private Switch m_enumProperty;

    /**
     * A boolean property.
     */
    private boolean m_fBooleanProperty;

    /**
     * A {@link String} property.
     */
    private String m_sStringProperty;

    /**
     * A {@link List} property.
     */
    @SuppressWarnings("unchecked")
    private List<Switch> m_listSwitches = (List<Switch>) Collections.EMPTY_LIST;

    /**
     * An {@link Expression} using a {@link MemorySize}.
     */
    private Expression<MemorySize> m_exprMemorySize;

    /**
     * A {@link Value}-based {@link Expression}
     */
    private Expression<Value> m_exprValue;

    /**
     * A {@link Seconds} value for expiry time.
     */
    private Seconds m_expiryTime;

    /**
     * An expression for {@link Millis}.
     */
    private Expression<Millis> m_exprSessionTimeout;

    /**
     * The OtherBean to inject (using getter injection).
     */
    private OtherBean m_otherBean = new OtherBean();

    /**
     * The nested OtherBean to inject (using getter injection).
     */
    private OtherBean m_nestedOtherBean = new OtherBean();
    }
