/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.cluster;

import com.tangosol.net.MemberIdentity;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;

import java.lang.management.ManagementFactory;

import java.lang.reflect.InvocationTargetException;

import javax.management.ObjectName;

/**
 * DefaultMemberIdentity provides a basic MemberIdentity implementation as well as
 * default values where applicable.
 *
 * @author pfm  2011.05.01
 * @since Coherence 3.7.1
 */
public class DefaultMemberIdentity
        implements MemberIdentity
    {
    // ----- constructors ---------------------------------------------------

    /**
     * The default Constructor.
     */
    public DefaultMemberIdentity()
        {
        this(null);
        }

    /**
     * A copy constructor that will initialize this object using an existing MemberIdentity.
     *
     * @param identity  the MemberIdentity use to initialize this object
     */
    public DefaultMemberIdentity(MemberIdentity identity)
        {
        if (identity != null)
            {
            m_sClusterName   = identity.getClusterName();
            m_nMachineId     = identity.getMachineId();
            m_sMachineName   = identity.getMachineName();
            m_sMemberName    = identity.getMemberName();
            m_memberPriority = identity.getPriority();
            m_sProcessName   = identity.getProcessName();
            m_sRackName      = identity.getRackName();
            m_sRoleName      = identity.getRoleName();
            m_sSiteName      = identity.getSiteName();
            }
        }

    // ----- accessors  -----------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String getClusterName()
        {
        return m_sClusterName;
        }

    /**
     * Set the cluster name.
     *
     * @param sClusterName  the cluster name
     *
     * @return this object
     */
    public DefaultMemberIdentity setClusterName(String sClusterName)
        {
        m_sClusterName = sClusterName;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMachineId()
        {
        return m_nMachineId;
        }

    /**
     * Set the machine id.
     *
     * @param nMachineId  the machine id
     *
     * @return this object
     */
    public DefaultMemberIdentity setMachineId(int nMachineId)
        {
        m_nMachineId = nMachineId & 0x0000FFFF;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMachineName()
        {
        return m_sMachineName;
        }

    /**
     * Set the name of the machine that the local Member is running on.
     *
     * @param sMachineName  the machine name
     *
     * @return this object
     */
    public DefaultMemberIdentity setMachineName(String sMachineName)
        {
        m_sMachineName = sMachineName;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMemberName()
        {
        return m_sMemberName;
        }

    /**
     * Set the name of the local Member.
     *
     * @param sName  the name of the local member
     *
     * @return this object
     */
    public DefaultMemberIdentity setMemberName(String sName)
        {
        m_sMemberName = sName;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPriority()
        {
        return m_memberPriority;
        }

    /**
     * Specifies the priority (or "weight") of the local Member.
     *
     * @param nPriority  the priority of the local member
     *
     * @return this object
     */
    public DefaultMemberIdentity setPriority(int nPriority)
        {
        m_memberPriority = nPriority;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProcessName()
        {
        String sProcessName = m_sProcessName;
        if (sProcessName == null)
            {
            m_sProcessName = sProcessName = makeProcessName();
            }
        return sProcessName;
        }

    /**
     * Set the name of the process (JVM) that the local Member is running in.
     *
     * @param sProcessName  the name of the process
     *
     * @return this object
     */
    public DefaultMemberIdentity setProcessName(String sProcessName)
        {
        m_sProcessName = sProcessName;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRackName()
        {
        return m_sRackName;
        }

    /**
     * Set the name of the rack where the local Member is running.
     *
     * @param sRackName  the name of the rack
     *
     * @return this object
     */
    public DefaultMemberIdentity setRackName(String sRackName)
        {
        m_sRackName = sRackName;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRoleName()
        {
        String sRoleName = m_sRoleName;
        if (sRoleName == null)
            {
            m_sRoleName = sRoleName = makeRoleName();
            }
        return sRoleName;
        }

    /**
     * Set the application-defined role name of the local Member.
     *
     * @param sRoleName  the role name of the local member
     *
     * @return this object
     */
    public DefaultMemberIdentity setRoleName(String sRoleName)
        {
        m_sRoleName = sRoleName;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSiteName()
        {
        return m_sSiteName;
        }

    /**
     * Set the site name.
     *
     * @param sSiteName  the site name
     *
     * @return this object
     */
    public DefaultMemberIdentity setSiteName(String sSiteName)
        {
        m_sSiteName = sSiteName;
        return this;
        }

    // ----- DefaultMemberIdentity methods ----------------------------------

    /**
     * Validate the supplied dependencies.
     *
     * @throws IllegalArgumentException if the dependencies are not valid
     *
     * @return this object
     */
    public DefaultMemberIdentity validate()
        {
        Base.checkRange(getClusterName() == null ? 0 : getClusterName().length(), 0, MEMBER_IDENTITY_LIMIT, "ClusterName");
        Base.checkRange(getMachineName() == null ? 0 : getMachineName().length(), 0, MEMBER_IDENTITY_LIMIT, "MachineName");
        Base.checkRange(getMemberName()  == null ? 0 : getMemberName().length(), 0, MEMBER_IDENTITY_LIMIT, "MemberName");
        Base.checkRange(getProcessName() == null ? 0 : getProcessName().length(), 0, MEMBER_IDENTITY_LIMIT, "ProcessName");
        Base.checkRange(getRackName()    == null ? 0 : getRackName().length(), 0, MEMBER_IDENTITY_LIMIT, "RackName");
        Base.checkRange(getRoleName()    == null ? 0 : getRoleName().length(), 0, MEMBER_IDENTITY_LIMIT, "RoleName");
        Base.checkRange(getSiteName()    == null ? 0 : getSiteName().length(), 0, MEMBER_IDENTITY_LIMIT, "SiteName");

        Base.checkRange(getPriority(), 0, Thread.MAX_PRIORITY, "Priority");

        return this;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return "DefaultMemberIdentity"
                + "{ClusterName="  + getClusterName()
                + ", MachineId="   + getMachineId()
                + ", MachineName=" + getMachineName()
                + ", MemberName="  + getMemberName()
                + ", Priority="    + getPriority()
                + ", ProcessName=" + getProcessName()
                + ", RackName="    + getRackName()
                + ", RoleName="    + getRoleName()
                + ", SiteName="    + getSiteName()
                + "}";
        }

    // ----- internal methods -----------------------------------------------

    /**
     * Make a process name.
     *
     * @return a process name
     */
    protected String makeProcessName()
        {
        String sName = null;
        try
            {
            Class<?> clz = Class.forName("java.lang.management.ManagementFactory");
            Object oRT = ClassHelper.invokeStatic(clz, "getRuntimeMXBean", ClassHelper.VOID);
            sName = (String) ClassHelper.invoke(oRT, "getName", ClassHelper.VOID);
            }
        catch (ClassNotFoundException e)
            {
            // ignore
            }
        catch (InvocationTargetException e)
            {
            // ignore
            }
        catch (NoSuchMethodException e)
            {
            // ignore
            }
        catch (IllegalAccessException e)
            {
            // ignore
            }

        // result looks like pid@machine; strip off @machine
        int ofAt = sName.indexOf('@');
        if (ofAt != -1)
            {
            String sPid = sName.substring(0, ofAt);
            // only strip if in expected format
            try
                {
                Long.parseLong(sPid);
                sName = sPid;
                }
            catch (NumberFormatException e)
                {
                // don't change sName
                }
            }
        if (sName.length() > MEMBER_IDENTITY_LIMIT)
            {
            sName = sName.substring(0, MEMBER_IDENTITY_LIMIT);
            }

        return sName;
        }

    /**
     * Make a role name.
     *
     * @return the role name
     */
    protected String makeRoleName()
        {
        // compute a default role name based on the application which was executed.
        // This is determined by finding the "main" thread and examining its root class name.
        String sName = null;
        try
            {
            Thread thread = Thread.currentThread();
            StackTraceElement[] aStack = null;
            if (Base.equals(thread.getName(), "main")) // 1.4 and higher
                {
                // we're on main thread
                aStack = new Throwable().getStackTrace();
                }
            else // 1.5 and higher
                {
                // walk up ThreadGroup to root, and look for "main" thread
                ThreadGroup group = thread.getThreadGroup();
                ThreadGroup parent = group.getParent();
                while (parent != null)
                    {
                    group = parent;
                    parent = group.getParent();
                    }
                Thread[] aThread = new Thread[group.activeCount()];
                for (int i = 0, c = group.enumerate(aThread); i < c; ++i)
                    {
                    thread = aThread[i];
                    String sThreadName = thread.getName();
                    if (Base.equals(sThreadName, "main")         // Sun JVM
                     || Base.equals(sThreadName, "Main Thread")) // JRockit JVM
                        {
                        aStack = (StackTraceElement[]) ClassHelper.invoke(thread, "getStackTrace", ClassHelper.VOID);
                        break;
                        }
                    }
                }

            if (aStack != null)
                {
                String sClass = aStack[aStack.length - 1].getClassName();

                // translate well known classes
                if (sClass.equals("com.tangosol.coherence.component.Application")
                 || sClass.equals("com.tangosol.net.CacheFactory"))
                    {
                    sName = "CoherenceConsole";
                    }
                else if (sClass.equals("com.tangosol.net.DefaultCacheServer"))
                    {
                    sName = "CoherenceServer";
                    }
                else
                    {
                    // build a role from the class name
                    String[] sParts = Base.parseDelimitedString(sClass, '.');

                    // strip off well known prefixes
                    int ofPartStart = sParts[0].equals("com") || sParts[0].equals("org") ? 1 : 0;

                    sName = Base.capitalize(sParts[ofPartStart]);
                    String sEnd = Base.capitalize(sParts[sParts.length - 1]);
                    if (sName.length() + sEnd.length() < MEMBER_IDENTITY_LIMIT)
                        {
                        // remove parts from the end until we fit within the character limit

                        for (int i = ofPartStart + 1, c = sParts.length - 1; i < c; ++i)
                            {
                            String sPart = sParts[i];
                            if (sName.length() + sPart.length() + sEnd.length() > MEMBER_IDENTITY_LIMIT)
                                {
                                break;
                                }
                            sName = sName + Base.capitalize(sPart);
                            }
                        sName += sEnd;
                        }
                    else
                        {
                        // just use truncated class name (no package)
                        sName = sEnd.substring(0, MEMBER_IDENTITY_LIMIT);
                        }
                    }
                }
            }
        catch (Throwable e)
            {}

        return sName;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The cluster name.
     */
    private String m_sClusterName;

    /**
     * An identifier that should uniquely identify each server machine.
     */
    private int m_nMachineId;

    /**
     * The machine name.
     */
    private String m_sMachineName;

    /**
     * The member name.
     */
    private String m_sMemberName;

    /**
     * The member priority.
     */
    private int m_memberPriority;

    /**
     * The process name.
     */
    private String m_sProcessName;

    /**
     * The rack name.
     */
    private String m_sRackName;

    /**
     * The role name.
     */
    private String m_sRoleName;

    /**
     * The site name.
     */
    private String m_sSiteName;
    }
