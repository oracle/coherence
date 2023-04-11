
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.message.Response

package com.tangosol.coherence.component.net.extend.message;

import com.tangosol.util.ImmutableArrayList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Abstract com.tangosol.net.messaging.Response implementation.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class Response
        extends    com.tangosol.coherence.component.net.extend.Message
        implements com.tangosol.net.messaging.Response
    {
    // ---- Fields declarations ----
    
    /**
     * Property Failure
     *
     * The status of the Response. If false, the Request was processed
     * successfully. If true, an error or exception occurred while processing
     * the Request.
     * 
     * @see com.tangosol.net.message.Response#isFailure
     */
    private boolean __m_Failure;
    
    /**
     * Property FMT_COLLECTION
     *
     * Result POF format: Collection
     */
    public static final int FMT_COLLECTION = 1;
    
    /**
     * Property FMT_GENERIC
     *
     * Result POF format: Generic
     */
    public static final int FMT_GENERIC = 0;
    
    /**
     * Property FMT_MAP
     *
     * Result POF format: Map
     */
    public static final int FMT_MAP = 2;
    
    /**
     * Property RequestId
     *
     * The unique identifier of the Request associated with this Response.
     * 
     * @see com.tangosol.net.messaging.Response#getRequestId
     */
    private long __m_RequestId;
    
    /**
     * Property Result
     *
     * The result of processing the Request associated with this Response.
     * 
     * @see com.tangsol.net.messaging.Response#getResult
     */
    private Object __m_Result;
    
    /**
     * Property ResultFormat
     *
     * The POF format of the result. The value of this property may be one of:
     *     FMT_GENERIC
     *     FMT_COLLECTION
     *     FMT_MAP
     */
    private int __m_ResultFormat;
    
    // Initializing constructor
    public Response(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        }
    
    //++ getter for static property _CLASS
    /**
     * Getter for property _CLASS.<p>
    * Property with auto-generated accessor that returns the Class object for a
    * given component.
     */
    public static Class get_CLASS()
        {
        Class clz;
        try
            {
            clz = Class.forName("com.tangosol.coherence/component/net/extend/message/Response".replace('/', '.'));
            }
        catch (ClassNotFoundException e)
            {
            throw new NoClassDefFoundError(e.getMessage());
            }
        return clz;
        }
    
    //++ getter for autogen property _Module
    /**
     * This is an auto-generated method that returns the global [design time]
    * parent component.
    * 
    * Note: the class generator will ignore any custom implementation for this
    * behavior.
     */
    private com.tangosol.coherence.Component get_Module()
        {
        return this;
        }
    
    // Declared at the super level
    /**
     * Return a human-readable description of this component.
    * 
    * @return a String representation of this component
     */
    protected String getDescription()
        {
        StringBuffer sb = new StringBuffer(super.getDescription());
        
        sb.append(", RequestId=").append(getRequestId());
        if (isFailure())
            {
            sb.append(", Failure=");
            sb.append(getResult());
            }
        else
            {
            sb.append(", Result=");
            Object oResult = getResult();
            sb.append(oResult == null ? "null" : oResult.getClass().getSimpleName() + "(HashCode=" + oResult.hashCode() + ')');
            }
        
        return sb.toString();
        }
    
    // From interface: com.tangosol.net.messaging.Response
    // Accessor for the property "RequestId"
    /**
     * Getter for property RequestId.<p>
    * The unique identifier of the Request associated with this Response.
    * 
    * @see com.tangosol.net.messaging.Response#getRequestId
     */
    public long getRequestId()
        {
        return __m_RequestId;
        }
    
    // From interface: com.tangosol.net.messaging.Response
    // Accessor for the property "Result"
    /**
     * Getter for property Result.<p>
    * The result of processing the Request associated with this Response.
    * 
    * @see com.tangsol.net.messaging.Response#getResult
     */
    public Object getResult()
        {
        return __m_Result;
        }
    
    // Accessor for the property "ResultFormat"
    /**
     * Getter for property ResultFormat.<p>
    * The POF format of the result. The value of this property may be one of:
    *     FMT_GENERIC
    *     FMT_COLLECTION
    *     FMT_MAP
     */
    protected int getResultFormat()
        {
        return __m_ResultFormat;
        }
    
    // From interface: com.tangosol.net.messaging.Response
    // Accessor for the property "Failure"
    /**
     * Getter for property Failure.<p>
    * The status of the Response. If false, the Request was processed
    * successfully. If true, an error or exception occurred while processing
    * the Request.
    * 
    * @see com.tangosol.net.message.Response#isFailure
     */
    public boolean isFailure()
        {
        return __m_Failure;
        }
    
    // Declared at the super level
    public void readExternal(com.tangosol.io.pof.PofReader in)
            throws java.io.IOException
        {
        // import com.tangosol.io.pof.PofHelper$ReadableEntrySetMap as com.tangosol.io.pof.PofHelper.ReadableEntrySetMap;
        // import com.tangosol.util.ImmutableArrayList;
        // import java.util.ArrayList;
        // import java.util.Collection;
        // import java.util.Map;
        
        super.readExternal(in);
        
        setRequestId(in.readLong(0));
        setFailure(in.readBoolean(1));
        
        // determine which result format is being used
        int nFormat = in.readInt(2);
        setResultFormat(nFormat);
        
        switch (nFormat)
            {
            default:
            case FMT_GENERIC:
                setResult(in.readObject(3));
                break;
        
            case FMT_COLLECTION:
                {
                Collection collection = in.readCollection(4, new ArrayList());
                setResult(new ImmutableArrayList(collection));
                }
                break;
        
            case FMT_MAP:
                {
                Map map = in.readMap(5, new com.tangosol.io.pof.PofHelper.ReadableEntrySetMap());
                setResult(map.entrySet());
                }
                break;
            }
        }
    
    // From interface: com.tangosol.net.messaging.Response
    // Accessor for the property "Failure"
    /**
     * Setter for property Failure.<p>
    * The status of the Response. If false, the Request was processed
    * successfully. If true, an error or exception occurred while processing
    * the Request.
    * 
    * @see com.tangosol.net.message.Response#isFailure
     */
    public void setFailure(boolean fFailure)
        {
        __m_Failure = fFailure;
        }
    
    // From interface: com.tangosol.net.messaging.Response
    // Accessor for the property "RequestId"
    /**
     * Setter for property RequestId.<p>
    * The unique identifier of the Request associated with this Response.
    * 
    * @see com.tangosol.net.messaging.Response#getRequestId
     */
    public void setRequestId(long lId)
        {
        __m_RequestId = lId;
        }
    
    // From interface: com.tangosol.net.messaging.Response
    // Accessor for the property "Result"
    /**
     * Setter for property Result.<p>
    * The result of processing the Request associated with this Response.
    * 
    * @see com.tangsol.net.messaging.Response#getResult
     */
    public void setResult(Object oResult)
        {
        __m_Result = (oResult);
        setResultFormat(FMT_GENERIC);
        }
    
    // Accessor for the property "ResultAsCollection"
    /**
     * Setter for property ResultAsCollection.<p>
    * The result of processing the Request as a Collection.
     */
    public void setResultAsCollection(java.util.Collection colResult)
        {
        setResult(colResult);
        setResultFormat(FMT_COLLECTION);
        }
    
    // Accessor for the property "ResultAsEntrySet"
    /**
     * Setter for property ResultAsEntrySet.<p>
    * The result of processing the Request as a Set of Map.Entry objects.
     */
    public void setResultAsEntrySet(java.util.Set setResult)
        {
        // import com.tangosol.io.pof.PofHelper$WriteableEntrySetMap as com.tangosol.io.pof.PofHelper.WriteableEntrySetMap;
        
        setResult(setResult == null ? null : new com.tangosol.io.pof.PofHelper.WriteableEntrySetMap(setResult));
        setResultFormat(FMT_MAP);
        }
    
    // Accessor for the property "ResultFormat"
    /**
     * Setter for property ResultFormat.<p>
    * The POF format of the result. The value of this property may be one of:
    *     FMT_GENERIC
    *     FMT_COLLECTION
    *     FMT_MAP
     */
    protected void setResultFormat(int nFormat)
        {
        __m_ResultFormat = nFormat;
        }
    
    // Declared at the super level
    public void writeExternal(com.tangosol.io.pof.PofWriter out)
            throws java.io.IOException
        {
        // import java.util.Collection;
        // import java.util.Map;
        
        super.writeExternal(out);
        
        out.writeLong(0, getRequestId());
        out.writeBoolean(1, isFailure());
        
        int nFormat = getResultFormat();
        out.writeInt(2, nFormat);
        switch (nFormat)
            {
            default:
            case FMT_GENERIC:
                out.writeObject(3, getResult());
                break;
        
            case FMT_COLLECTION:
                out.writeCollection(4, (Collection) getResult());
                break;
        
            case FMT_MAP:
                out.writeMap(5, (Map) getResult());
                break;
            }
        }
    }
