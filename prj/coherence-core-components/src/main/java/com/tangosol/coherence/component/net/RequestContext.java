
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.RequestContext

package com.tangosol.coherence.component.net;

import com.tangosol.io.pof.PofPrincipal;
import java.security.Principal;
import java.util.Iterator;
import java.util.Set;
import javax.security.auth.Subject;

/**
 * This component is used to carry state necessary to implement idempotent
 * request executions. For example, DistributedCache invocation requests should
 * behave correctly in the following scenario:
 * 
 * 1) Client A calls cache server B with cache.process(oKey, agent);
 * 2) Server B invokes a process
 * 3) Server B sends an update to the backup server C
 * 4) Server B dies
 * 5) Client A re-submits the process
 * 
 * If the message (3) was succesfully delivered to server C, the server C must
 * not repeat the agent execution and respond back with the result that have
 * been calculated by (2).
 * 
 * @see Message.RequestMessage#RequestContext property
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class RequestContext
        extends    com.tangosol.coherence.component.Net
        implements com.tangosol.io.ExternalizableLite
    {
    // ---- Fields declarations ----
    
    /**
     * Property OldestPendingSUID
     *
     * A smallest (oldest) pending SUID issued by the local service member.
     */
    private long __m_OldestPendingSUID;
    
    /**
     * Property RequestSUID
     *
     * A SUID that is associated with a current request.
     */
    private long __m_RequestSUID;
    
    /**
     * Property Subject
     *
     * An optional Subject identifying the caller.
     */
    private javax.security.auth.Subject __m_Subject;
    
    // Default constructor
    public RequestContext()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public RequestContext(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.RequestContext();
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
            clz = Class.forName("com.tangosol.coherence/component/net/RequestContext".replace('/', '.'));
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
    
    // Accessor for the property "OldestPendingSUID"
    /**
     * Getter for property OldestPendingSUID.<p>
    * A smallest (oldest) pending SUID issued by the local service member.
     */
    public long getOldestPendingSUID()
        {
        return __m_OldestPendingSUID;
        }
    
    // Accessor for the property "RequestSUID"
    /**
     * Getter for property RequestSUID.<p>
    * A SUID that is associated with a current request.
     */
    public long getRequestSUID()
        {
        return __m_RequestSUID;
        }
    
    // Accessor for the property "Subject"
    /**
     * Getter for property Subject.<p>
    * An optional Subject identifying the caller.
     */
    public javax.security.auth.Subject getSubject()
        {
        return __m_Subject;
        }
    
    // From interface: com.tangosol.io.ExternalizableLite
    public void readExternal(java.io.DataInput in)
            throws java.io.IOException
        {
        // import com.tangosol.io.pof.PofPrincipal;
        // import java.util.Set;
        // import javax.security.auth.Subject;
        
        long lSUIDPending = in.readLong();
        int  cDelta       = in.readInt();
        long lSUIDCurrent = cDelta == -1 ? in.readLong() : lSUIDPending + cDelta;
        
        setRequestSUID(lSUIDCurrent);
        setOldestPendingSUID(lSUIDPending);
        
        int cPals = in.readByte();
        if (cPals > 0)
            {
            Subject subject = new Subject();
            Set     setPals = subject.getPrincipals();
        
            for (int i = 0; i < cPals; i++)
                {
                String sName = in.readUTF();
        
                setPals.add(new PofPrincipal(sName));
                }
            subject.setReadOnly();
            setSubject(subject);
            }
        }
    
    // Accessor for the property "OldestPendingSUID"
    /**
     * Setter for property OldestPendingSUID.<p>
    * A smallest (oldest) pending SUID issued by the local service member.
     */
    public void setOldestPendingSUID(long lSUID)
        {
        __m_OldestPendingSUID = lSUID;
        }
    
    // Accessor for the property "RequestSUID"
    /**
     * Setter for property RequestSUID.<p>
    * A SUID that is associated with a current request.
     */
    public void setRequestSUID(long lSUID)
        {
        __m_RequestSUID = lSUID;
        }
    
    // Accessor for the property "Subject"
    /**
     * Setter for property Subject.<p>
    * An optional Subject identifying the caller.
     */
    public void setSubject(javax.security.auth.Subject subject)
        {
        __m_Subject = subject;
        }
    
    // From interface: com.tangosol.io.ExternalizableLite
    public void writeExternal(java.io.DataOutput out)
            throws java.io.IOException
        {
        // import java.security.Principal;
        // import java.util.Iterator;
        // import java.util.Set;
        // import javax.security.auth.Subject;
        
        // compact the wire format of the SUID
        long lSUIDCurrent = getRequestSUID();
        long lSUIDPending = getOldestPendingSUID();
        long lDelta       = lSUIDCurrent - lSUIDPending;
        
        out.writeLong(lSUIDPending);
        
        _assert(lDelta >= 0);
        if (lDelta < Integer.MAX_VALUE)
            {
            out.writeInt((int) lDelta);
            }
        else
            {
            out.writeInt(-1);
            out.writeLong(lSUIDCurrent);
            }
        
        Subject subject = getSubject();
        if (subject == null)
            {
            out.writeByte(0);
            }
        else
            {
            // at the moment we only serialize the Principals
            Set setPals = subject.getPrincipals();
        
            out.writeByte(setPals.size());
            for (Iterator iter = setPals.iterator(); iter.hasNext();)
                {
                Principal p = (Principal) iter.next();
        
                out.writeUTF(p.getName());
                }
            }
        }
    }
