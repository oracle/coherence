
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse

package com.tangosol.coherence.component.net.message.responseMessage;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import java.io.IOException;

/**
 * SimpleResponse is an abstract Message component used to respond to generic
 * request messages, carrying a value and return code.
 * 
 * SimpleResponse has a final onReceived implementation, and any response
 * related processing should be goverened by the associated poll.
 * 
 * Attributes:
 *     Result
 *     Value
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class SimpleResponse
        extends    com.tangosol.coherence.component.net.message.ResponseMessage
    {
    // ---- Fields declarations ----
    
    /**
     * Property Result
     *
     * One of the RESULT_ values.
     */
    private int __m_Result;
    
    /**
     * Property RESULT_FAILURE
     *
     */
    public static final int RESULT_FAILURE = 2;
    
    /**
     * Property RESULT_RETRY
     *
     */
    public static final int RESULT_RETRY = 1;
    
    /**
     * Property RESULT_SUCCESS
     *
     */
    public static final int RESULT_SUCCESS = 0;
    
    /**
     * Property Value
     *
     * The response value. The value could be:
     *     - a Binary, Boolean, Integer instance in case of SUCCESS
     *     - an Exception or String message in case of FAILURE
     *     - undetermined in case of RETRY
     */
    private Object __m_Value;
    
    // Default constructor
    public SimpleResponse()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public SimpleResponse(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        
        if (fInit)
            {
            __init();
            }
        }
    
    // Main initializer
    public void __init()
        {
        // private initialization
        __initPrivate();
        
        
        // signal the end of the initialization
        set_Constructed(true);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        }
    
    //++ getter for static property _Instance
    /**
     * Getter for property _Instance.<p>
    * Auto generated
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        return new com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse();
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
            clz = Class.forName("com.tangosol.coherence/component/net/message/responseMessage/SimpleResponse".replace('/', '.'));
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
     * Getter for property Description.<p>
    * Used for debugging purposes (from toString). Create a human-readable
    * description of the specific Message data.
     */
    public String getDescription()
        {
        StringBuffer sb = new StringBuffer();
        
        switch (getResult())
            {
            case RESULT_SUCCESS:
                sb.append(": Value=")
                  .append(getValue());
                 break;
            case RESULT_RETRY:
                sb.append(": Retry");
                 break;
            case RESULT_FAILURE:
                sb.append(": Failure=")
                  .append(getValue());
                 break;
            default:
                throw new IllegalStateException();
            }
        
        return sb.toString();
        }
    
    // Declared at the super level
    /**
     * Getter for property EstimatedByteSize.<p>
    * The estimated serialized size of this message.  A negative value
    * indicates that the size is unknown and that it is safe to estimate the
    * size via a double serialization.
     */
    public int getEstimatedByteSize()
        {
        // import com.tangosol.util.Binary;
        
        Object oResult = getValue();
        
        return super.getEstimatedByteSize() +
            2 + // short - Result
            4 + // int   - type
            (oResult instanceof Binary ? 4 /*int - length*/ + ((Binary) oResult).length() : 0);
        }
    
    // Accessor for the property "Failure"
    /**
     * Getter for property Failure.<p>
    * (Calculated) Helper property that converts the Value into a
    * RuntimeException.
     */
    public RuntimeException getFailure()
        {
        // import com.tangosol.util.Base;
        
        Object oResponse = getValue();
        return oResponse instanceof Throwable ?
            Base.ensureRuntimeException((Throwable) oResponse) :
            new RuntimeException((String) oResponse);
        }
    
    // Accessor for the property "Result"
    /**
     * Getter for property Result.<p>
    * One of the RESULT_ values.
     */
    public int getResult()
        {
        return __m_Result;
        }
    
    // Accessor for the property "Value"
    /**
     * Getter for property Value.<p>
    * The response value. The value could be:
    *     - a Binary, Boolean, Integer instance in case of SUCCESS
    *     - an Exception or String message in case of FAILURE
    *     - undetermined in case of RETRY
     */
    public Object getValue()
        {
        return __m_Value;
        }
    
    // Declared at the super level
    public void read(com.tangosol.io.ReadBuffer.BufferInput input)
            throws java.io.IOException
        {
        // import java.io.IOException;
        
        try
            {
            setResult(input.readShort());
            setValue(readObject(input));
            }
        catch (IOException e)
            {
            getService().onConfigIOException(e, getFromMember());
            }
        }
    
    // Accessor for the property "Result"
    /**
     * Setter for property Result.<p>
    * One of the RESULT_ values.
     */
    public void setResult(int nResult)
        {
        __m_Result = nResult;
        }
    
    // Accessor for the property "Value"
    /**
     * Setter for property Value.<p>
    * The response value. The value could be:
    *     - a Binary, Boolean, Integer instance in case of SUCCESS
    *     - an Exception or String message in case of FAILURE
    *     - undetermined in case of RETRY
     */
    public void setValue(Object oValue)
        {
        __m_Value = oValue;
        }
    
    // Declared at the super level
    public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
            throws java.io.IOException
        {
        output.writeShort(getResult());
        writeObject(output, getValue());
        }
    }
