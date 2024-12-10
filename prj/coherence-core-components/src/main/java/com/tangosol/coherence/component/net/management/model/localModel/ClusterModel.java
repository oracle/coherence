
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.management.model.localModel.ClusterModel

package com.tangosol.coherence.component.net.management.model.localModel;

import com.tangosol.coherence.component.net.management.Gateway;
import com.tangosol.coherence.component.util.SafeCluster;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.Member;
import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.management.Registry;
import com.tangosol.util.Base;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Model components implement the JMX-managed functionality of the
 * corresponding MBeans without being dependent on any JMX classes and could be
 * used both in-process and out-of-process (relative to an MBeanServer).
 * 
 * The LocalModel components operate in two distinct modes: live and snapshot.
 * In the live mode all model methods call corresponding methods on managed
 * objects. The snapshot mode uses the _SnapshotMap to keep the attribute
 * values.
 * 
 * Every time a remote invocation is used by the RemoteModel to do a
 * setAttribute or invoke call, the snapshot model is refreshed.
 * 
 * The CluserModel represents a Cluster object.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ClusterModel
        extends    com.tangosol.coherence.component.net.management.model.LocalModel
    {
    // ---- Fields declarations ----
    
    /**
     * Property _Cluster
     *
     * Cluster object associated with this model.
     */
    private transient com.tangosol.coherence.component.util.SafeCluster __m__Cluster;
    
    /**
     * Property _ClusterRef
     *
     * Cluster object associated with this model, wrapped into WeakReference to
     * avoid resource leakage.
     */
    private transient java.lang.ref.WeakReference __m__ClusterRef;
    
    // Default constructor
    public ClusterModel()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ClusterModel(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        
        // state initialization: public and protected properties
        try
            {
            set_SnapshotMap(new java.util.HashMap());
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
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
        return new com.tangosol.coherence.component.net.management.model.localModel.ClusterModel();
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
            clz = Class.forName("com.tangosol.coherence/component/net/management/model/localModel/ClusterModel".replace('/', '.'));
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
    
    /**
     * Configure the tracing sampling ratio for those members matching the
    * specified role, or if role is null or empty,
    * configure the tracing ratio for all cluster members.
     */
    public void configureTracing(String sRole, Float fRatio)
        {
        // import Component.Net.Management.Gateway;
        // import com.tangosol.net.Cluster;
        // import com.tangosol.net.Member;
        // import com.tangosol.net.management.MBeanHelper;
        // import com.tangosol.net.management.Registry;
        // import java.util.Objects;
        // import java.util.Iterator;
        // import javax.management.Attribute;
        // import javax.management.MBeanServer;
        // import javax.management.ObjectName;
        
        checkReadOnly("configureTracing");
        
        // validate fRatio
        if (fRatio == null)
            {
            throw new IllegalArgumentException("fRatio cannot be null");
            }
        
        // enforce sane configuration values
        float fRatioPrim = fRatio.floatValue();
        fRatio = Float.valueOf(fRatioPrim < 0 ? -1 : Math.min(fRatioPrim, 1.0f));
        
        Cluster cluster = get_Cluster();
        if (cluster == null)
            {
            return;
            }
        
        MBeanServer server      = MBeanHelper.findMBeanServer();
        Gateway     gateway     = (Gateway) cluster.getManagement();
        boolean     fAllMembers = sRole == null || sRole.isEmpty();
        String      sNodePrefix = gateway.getDomainName() + ':' + Registry.NODE_TYPE;
        
        
        for (Iterator iter = cluster.getMemberSet().iterator(); iter.hasNext(); )
            {
            Member member = (Member) iter.next();
            if (fAllMembers || Objects.equals(member.getRoleName(), sRole))
                {
                try
                    {
                    String sNode = gateway.ensureGlobalName(sNodePrefix, member);
                    server.setAttribute(new ObjectName(sNode), new Attribute("TracingSamplingRatio", fRatio));
                    }
                catch (Exception ignore)
                    {
                    }
                }
            }
        }
    
    /**
     * Dump heap on all cluster members.
     */
    public void dumpClusterHeap(String sRole)
        {
        // import Component.Net.Management.Gateway;
        // import com.tangosol.net.Cluster;
        // import com.tangosol.net.Member;
        // import com.tangosol.net.management.MBeanHelper;
        // import com.tangosol.net.management.Registry;
        // import java.util.Objects;
        // import java.util.Iterator;
        // import javax.management.MBeanServer;
        // import javax.management.ObjectName;
        
        checkReadOnly("dumpClusterHeap");
        
        Cluster cluster = get_Cluster();
        if (cluster == null)
            {
            return;
            }
        
        MBeanServer server      = MBeanHelper.findMBeanServer();
        Gateway     gateway     = (Gateway) cluster.getManagement();
        boolean     fAllMembers = sRole == null || sRole.isEmpty();
        String      sNodePrefix = gateway.getDomainName() + ':' + Registry.NODE_TYPE;
        
        for (Iterator iter = cluster.getMemberSet().iterator(); iter.hasNext(); )
            {
            Member member = (Member) iter.next();
            if (fAllMembers || Objects.equals(member.getRoleName(), sRole))
                {
                try
                    {
                    String sNode     = gateway.ensureGlobalName(sNodePrefix, member);
                    String sFileName = (String) server.invoke(new ObjectName(sNode), "dumpHeap", new Object[]{null}, null);
                    _trace("Heapdump is created for Node" + member.getId() + " in " + sFileName, 2);
                    }
                catch (Exception ignore)
                    {
                    }
                }
            }
        }
    
    /**
     * Return the provided options ammending the ‘filename’ option to include
    * node information.
    * The name of the file is generated if a directory reference is provided
    * using the following format: <node-id>-<jfr-name>.jfr.
    * 
    * For example:
    * 
    *    “name=foo,filename=/mydir/mydump.jfr” ->
    * “name=foo,filename=/mydir/1-mydump.jfr”
    *    “name=foo,filename=/mydir”                        ->
    * “name=foo,filename=/mydir/1-foo.jfr”
    * 
    * sOptions  a comma separated JFR options
    * nMemberId the node ID
     */
    public String ensureGlobalFileName(String sOptions, int nMemberId)
        {
        // import java.io.File;
        
        final String FILE_OPTION = "filename";
        final String NAME_OPTION = "name";
        
        for (int iFilename = sOptions.indexOf(FILE_OPTION); iFilename >= 0;
             iFilename = sOptions.indexOf(FILE_OPTION, iFilename + FILE_OPTION.length()))
            {
            char chFilePre  = sOptions.charAt(Math.max(iFilename - 1, 0));
            char chFilePost = sOptions.charAt(iFilename + FILE_OPTION.length());
        
            if ((chFilePre  != ' ' && chFilePre  != ',') &&
                    (chFilePost != ' ' && chFilePost != '='))
                {
                continue; // not exclusively the filename option
                }
        
            // find the start of the value
            int iValueStart = iFilename + FILE_OPTION.length();
            while (sOptions.charAt(iValueStart) == ' ' || sOptions.charAt(iValueStart) == '=')
                {
                iValueStart++;
                }
        
            // find the end of the value
            int iValueEnd = iValueStart + 1;
            while (iValueEnd < sOptions.length() && sOptions.charAt(iValueEnd) != ',')
                {
                iValueEnd++;
                }
        
            // determine whether filename is a file or directory
            String sFileName = sOptions.substring(iValueStart, iValueEnd);
            int    iFileName = sFileName.lastIndexOf(File.separator);
            if (iFileName > 0)
                {
                sFileName = sFileName.substring(iFileName + 1);
                }
        
            int    iFileExt = sFileName.length() > 0 ? sFileName.lastIndexOf(".") : -1;
            String sName    = "";
            if (iFileExt < 0 || sFileName.length() - iFileExt > 4)
                {
                // filename is a directory
                for (int iBegin = sOptions.indexOf(NAME_OPTION); iBegin >= 0;
                     iBegin = sOptions.indexOf(NAME_OPTION, iBegin + NAME_OPTION.length()))
                    {
                    char chNamePre  = sOptions.charAt(Math.max(iBegin - 1, 0));
                    char chNamePost = sOptions.charAt(iBegin + NAME_OPTION.length());
        
                    if (!(iBegin == 0 || chNamePre == ' ' || chNamePre == ',') ||
                            !(chNamePost == ' ' || chNamePost == '='))
                        {
                        continue;
                        }
                    iBegin = iBegin + NAME_OPTION.length();
        
                    // find the end of the value
                    int iEnd = iBegin + 1;
                    while (iEnd < sOptions.length() && sOptions.charAt(iEnd) != ',')
                        {
                        iEnd++;
                        }
        
                    sName = sOptions.substring(iBegin + 1, iEnd);
                    break;
                    }
        
                return sFileName.length() == 0
                    ? sOptions.substring(0, iValueEnd) + nMemberId + "-" + sName + ".jfr" + sOptions.substring(iValueEnd)
                      : sOptions.substring(0, iValueEnd) + File.separator + nMemberId + "-" + sName + ".jfr" + sOptions.substring(iValueEnd);
                }
            else
                {
                // filename is a file
                iFileName = sOptions.lastIndexOf(File.separator) + 1;
                if (iFileName > 0)
                    {
                    iValueStart = iFileName;
                    }
        
                return sOptions.substring(0, iValueStart) +
                        nMemberId + '-' + sOptions.substring(iValueStart);
                }
            }
        return sOptions;
        }
    
    /**
     * Ensures that the cluster service is running on this node.
     */
    public void ensureRunning()
        {
        // import com.tangosol.net.Cluster;
        
        checkReadOnly("ensureRunning");
        
        Cluster cluster = get_Cluster();
        if (cluster != null && !cluster.isRunning())
            {
            cluster.start();
            }
        }
    
    /**
     * Perform a Java flight recorder operation on all eligible cluster members.
    *  If a role is specified, the JFR will be performed on all members in the
    * given role; otherwise, on all members of the cluster.
    * 
    * The valid commands are: jfrStart, jfrStop, jfrDump, jfrCheck.
    * The options are a comma delimited (array of) arguments passed to the JFR
    * given diagnostic command.
     */
    public String[] flightRecording(String sRole, String sCmd, String sOptions)
        {
        // import Component.Net.Management.Gateway;
        // import com.tangosol.net.Cluster;
        // import com.tangosol.net.Member;
        // import com.tangosol.net.management.MBeanHelper;
        // import com.tangosol.net.management.Registry;
        // import com.tangosol.util.Base;
        // import java.util.ArrayList;
        // import java.util.Iterator;
        // import java.util.List;
        // import java.util.Objects;
        // import javax.management.MBeanServer;
        // import javax.management.ObjectName;
        
        checkReadOnly("clusterJfr");
        
        Cluster cluster = get_Cluster();
        if (cluster == null)
            {
            return null;
            }
        
        MBeanServer server         = MBeanHelper.findMBeanServer();
        Gateway     gateway        = (Gateway) cluster.getManagement();
        boolean     fAllMembers    = sRole == null || sRole.isEmpty();
        String[]    sResults       = new String[cluster.getMemberSet().size()];
        String      sDiagPrefix    = gateway.getDomainName() + ":type=DiagnosticCommand,Domain=com.sun.management,subType=DiagnosticCommand";
        int         i              = 0;
        List        listMBeanNames = new ArrayList(cluster.getMemberSet().size());
        
        for (Iterator iter = cluster.getMemberSet().iterator(); iter.hasNext(); )
            {
            Member member = (Member) iter.next();
            if (fAllMembers || Objects.equals(member.getRoleName(), sRole))
                {
                try
                    {
                    ObjectName oName = new ObjectName(gateway.ensureGlobalName(sDiagPrefix, member));
                    listMBeanNames.add(oName);
                    }
                catch (Exception e)
                    {
                    throw Base.ensureRuntimeException(e, "Unable to unlock commercial features for member " +
                            member.getId() + " due to: " + e.getMessage());
                    }
                }
            }
        
        // now that commercial features are enabled
        
        for (Iterator iter = listMBeanNames.iterator(); iter.hasNext(); )
            {
            ObjectName oName     = (ObjectName) iter.next();
            int        nMemberId = Integer.parseInt(oName.getKeyProperty("nodeId"));
            String     sMember   = (String) oName.getKeyProperty("member");
        
            // member key property may not always be present
            if (sMember == null)
                {
                sMember = "Member " + nMemberId;
                }
        
            try
                {
                String sNodeOptions = sOptions;
        
                if (sOptions.indexOf("filename=") >= 0)
                    {
                    sNodeOptions = ensureGlobalFileName(sOptions, nMemberId);
                    }
        
                Object[] aoArguments = sNodeOptions.split(",");
                String   sMessage    = (String) server.invoke(oName, sCmd, new Object[]{aoArguments},
                                               new String[]{String[].class.getName()});
                sResults[i++] = sMember + "->" + sMessage;
                }
            catch (Exception ignore)
                {
                String sMessage = "Flight Recorder operation for node " + nMemberId + ", got exception: " + ignore.getMessage();
                sResults[i++] = sMember + "->" + sMessage + "\n";
                _trace(sMessage, 2);
                }
            }
        
        return sResults;
        }
    
    // Accessor for the property "_Cluster"
    /**
     * Getter for property _Cluster.<p>
    * Cluster object associated with this model.
     */
    public com.tangosol.coherence.component.util.SafeCluster get_Cluster()
        {
        // import Component.Util.SafeCluster;
        // import java.lang.ref.WeakReference;
        
        WeakReference wr = get_ClusterRef();
        return wr == null ? null : (SafeCluster) wr.get();
        }
    
    // Accessor for the property "_ClusterRef"
    /**
     * Getter for property _ClusterRef.<p>
    * Cluster object associated with this model, wrapped into WeakReference to
    * avoid resource leakage.
     */
    protected java.lang.ref.WeakReference get_ClusterRef()
        {
        return __m__ClusterRef;
        }

    /**
     * Get the Coherence cluster configuration.
     */
    public String getClusterConfig()
        {
        return CacheFactory.getClusterConfig().toString() + "\n"
               + CacheFactory.getLoggingConfig().toString() + "\n"
               + CacheFactory.getManagementConfig().toString() + "\n"
               + CacheFactory.getFederationConfig().toString();
        }

    /**
     * Get cluster description.
     */
    public String getClusterDescription()
        {
        Cluster cluster = get_Cluster();
        return cluster == null ? canonicalString(null) : cluster.toString();
        }

    // Accessor for the property "ClusterName"
    /**
     * Getter for property ClusterName.<p>
    * The name of the cluster.
     */
    public String getClusterName()
        {
        // import com.tangosol.net.Cluster;
        
        Cluster cluster = get_Cluster();
        return cluster == null ? canonicalString(null) : canonicalString(cluster.getClusterName());
        }
    
    // Accessor for the property "ClusterSize"
    /**
     * Getter for property ClusterSize.<p>
    * The total number of cluster nodes.
     */
    public int getClusterSize()
        {
        // import com.tangosol.net.Cluster;
        
        Cluster cluster = get_Cluster();
        return cluster != null && cluster.isRunning() ? cluster.getMemberSet().size() : 0;
        }
    
    // Declared at the super level
    /**
     * Getter for property Description.<p>
    * Human readable description.
    * 
    * @see Manageable.ModelAdapter#toString()
     */
    public String getDescription()
        {
        // import com.tangosol.net.Cluster;
        
        Cluster cluster = get_Cluster();
        return cluster != null && cluster.isRunning() ?
            "MemberId=" + cluster.getLocalMember().getId() :
            "Not running";
        }
    
    // Accessor for the property "LicenseMode"
    /**
     * Getter for property LicenseMode.<p>
    * The license mode that this cluster is using. Possible values are
    * Evaluation, Development or Production.
     */
    public String getLicenseMode()
        {
        // import Component.Net.Member as com.tangosol.coherence.component.net.Member;
        // import com.tangosol.net.Cluster;
        
        Cluster cluster = get_Cluster();
        if (cluster != null && cluster.isRunning())
            {
            com.tangosol.coherence.component.net.Member member = (com.tangosol.coherence.component.net.Member) cluster.getLocalMember();
            return com.tangosol.coherence.component.net.Member.MODE_NAME[member.getMode()];
            }
        else
            {
            return canonicalString(null);
            }
        }
    
    // Accessor for the property "LocalMemberId"
    /**
     * Getter for property LocalMemberId.<p>
     */
    public int getLocalMemberId()
        {
        // import com.tangosol.net.Cluster;
        
        Cluster cluster = get_Cluster();
        if (cluster != null && cluster.isRunning())
            {
            return cluster.getLocalMember().getId();
            }
        else
            {
            return -1;
            }
        }
    
    // Accessor for the property "MemberIds"
    /**
     * Getter for property MemberIds.<p>
    * An array of all existing cluster members.
     */
    public int[] getMemberIds()
        {
        // import com.tangosol.net.Cluster;
        // import com.tangosol.net.Member;
        // import java.util.Iterator;
        // import java.util.Set;
        
        Cluster cluster = get_Cluster();
        if (cluster != null && cluster.isRunning())
            {
            Set   setMember = cluster.getMemberSet();
            int[] anMember  = new int[setMember.size()];
            Iterator iter = setMember.iterator();
            for (int i = 0; iter.hasNext() && i < anMember.length; i++)
                {
                anMember[i] = ((Member) iter.next()).getId();
                }
            return anMember;
            }
        else
            {
            return new int[0];
            }
        }
    
    // Accessor for the property "Members"
    /**
     * Getter for property Members.<p>
    * An array of all existing cluster members.
     */
    public String[] getMembers()
        {
        // import com.tangosol.net.Cluster;
        // import java.util.Iterator;
        // import java.util.Set;
        
        Cluster cluster = get_Cluster();
        if (cluster != null && cluster.isRunning())
            {
            Set      setMember = cluster.getMemberSet();
            String[] asMember  = new String[setMember.size()];
            Iterator iter = setMember.iterator();
            for (int i = 0; iter.hasNext() && i < asMember.length; i++)
                {
                asMember[i] = iter.next().toString();
                }
            return asMember;
            }
        else
            {
            return new String[0];
            }
        }
    
    // Accessor for the property "MembersDeparted"
    /**
     * Getter for property MembersDeparted.<p>
     */
    public String[] getMembersDeparted()
        {
        // import Component.Net.Cluster as com.tangosol.coherence.component.net.Cluster;
        // import java.util.HashSet;
        // import java.util.Iterator;
        // import java.util.Set;
        
        try 
            { 
            com.tangosol.coherence.component.net.Cluster clusterReal = get_Cluster().getCluster();
        
            // take a RecycleSet snapshot to avoid concurrent modifications
            Set setMembers = new HashSet(
                clusterReal.getClusterService().getClusterMemberSet().getRecycleSet());
        
            String[] asMembers = new String[setMembers.size()];
            Iterator iter      = setMembers.iterator();
            for (int i = 0; iter.hasNext(); i++)
                {
                asMembers[i] = String.valueOf(iter.next());
                }
            return asMembers;
            } 
        catch (Exception e) // ClassCast or NullPointer 
            { 
            // must be a local cache service 
            return new String[0];
            }
        }
    
    // Accessor for the property "MembersDepartureCount"
    /**
     * Getter for property MembersDepartureCount.<p>
    * The number of times this node has observed another node`s departure from
    * the cluster since this management node has joined the cluster or
    * statistics have been reset.
     */
    public long getMembersDepartureCount()
        {
        // import Component.Net.Cluster as com.tangosol.coherence.component.net.Cluster;
         
        try 
            { 
            com.tangosol.coherence.component.net.Cluster clusterReal = get_Cluster().getCluster(); 
            return clusterReal.getClusterService().getStatsMembersDepartureCount(); 
            }
        catch (Exception e) // ClassCast or NullPointer 
            { 
            // must be a local cache service 
            return 0L; 
            }
        }
    
    // Accessor for the property "OldestMemberId"
    /**
     * Getter for property OldestMemberId.<p>
     */
    public int getOldestMemberId()
        {
        // import com.tangosol.net.Cluster;
        
        Cluster cluster = get_Cluster();
        if (cluster != null && cluster.isRunning())
            {
            return cluster.getOldestMember().getId();
            }
        else
            {
            return -1;
            }
        }
    
    // Accessor for the property "Version"
    /**
     * Getter for property Version.<p>
     */
    public String getVersion()
        {
        // import com.tangosol.net.CacheFactory;
        return CacheFactory.VERSION;
        }
    
    // Accessor for the property "Running"
    /**
     * Getter for property Running.<p>
    * Determines whether or not the cluster is running.
     */
    public boolean isRunning()
        {
        // import com.tangosol.net.Cluster;
        
        Cluster cluster = get_Cluster();
        return cluster != null && cluster.isRunning();
        }
    
    /**
     * Log state on cluster members running with specified role. The state
    * includes full thread dump and outstanding polls. If sRole is not
    * specified or set to "all", state will be logged on all cluster members.
     */
    public void logClusterState(String sRole)
        {
        // import Component.Net.Management.Gateway;
        // import com.tangosol.net.Cluster;
        // import com.tangosol.net.Member;
        // import com.tangosol.net.management.MBeanHelper;
        // import com.tangosol.net.management.Registry;
        // import java.util.Iterator;
        // import javax.management.MBeanServer;
        // import javax.management.ObjectName;
        
        checkReadOnly("logClusterState");
        
        Cluster cluster = get_Cluster();
        if (cluster == null)
            {
            return;
            }
        
        MBeanServer server      = MBeanHelper.findMBeanServer();
        Gateway     gateway     = (Gateway) cluster.getManagement();
        boolean     fAllMembers = sRole == null || sRole.isEmpty();
        String      sNodePrefix = gateway.getDomainName() + ':' + Registry.NODE_TYPE;
        
        for (Iterator iter = cluster.getMemberSet().iterator(); iter.hasNext(); )
            {
            Member member = (Member) iter.next();
            if (fAllMembers || member.getRoleName().equals(sRole))
                {
                try
                    {
                    String sNode = gateway.ensureGlobalName(sNodePrefix, member);
        
                    server.invoke(new ObjectName(sNode), "logNodeState", null, null);
                    }
                catch (Exception ignore)
                    {
                    }
                }
            }
        }
    
    // Declared at the super level
    /**
     * Must be supplemented at each specific Model implementation.
     */
    public void readExternal(java.io.DataInput in)
            throws java.io.IOException
        {
        throw new IllegalStateException("ClusterModel is not global");
        }
    
    /**
     * Resume all suspended members of the service identified by the specified
    * name.
    * 
    * If "Cluster" is passed for the service name, all services (including the
    * ClusterService) will be resumed.
    * 
    * @see com.tangosol.net.Cluster#resumeService
     */
    public void resumeService(String sService)
        {
        // import com.tangosol.net.Cluster;
        
        checkReadOnly("resumeService");
        
        Cluster cluster = get_Cluster();
        if (cluster != null)
            {
            cluster.resumeService(sService);
            }
        }
    
    // Accessor for the property "_Cluster"
    /**
     * Setter for property _Cluster.<p>
    * Cluster object associated with this model.
     */
    public void set_Cluster(com.tangosol.coherence.component.util.SafeCluster cluster)
        {
        // import java.lang.ref.WeakReference;
        
        set_ClusterRef(new WeakReference(cluster));
        }
    
    // Accessor for the property "_ClusterRef"
    /**
     * Setter for property _ClusterRef.<p>
    * Cluster object associated with this model, wrapped into WeakReference to
    * avoid resource leakage.
     */
    protected void set_ClusterRef(java.lang.ref.WeakReference refCluster)
        {
        __m__ClusterRef = refCluster;
        }
    
    /**
     * Shuts down the cluster service on this node.
     */
    public void shutdown()
        {
        // import com.tangosol.net.Cluster;
        
        checkReadOnly("shutdown");
        
        Cluster cluster = get_Cluster();
        if (cluster != null)
            {
            cluster.shutdown();
            }
        }
    
    /**
     * Suspend all members of the service identified by the specified name.  A
    * suspended Service has been placed in a "quiesced" or "deactivated" state
    * in preparation to be shutdown.  Once suspended, a service may be
    * "resumed" or "reactivated" with the resumeService method.
    * 
    * If "Cluster" is passed for the service name, all services (including the
    * ClusterService) will be suspended.
    * 
    * @see com.tangosol.net.Cluster#suspendService
     */
    public void suspendService(String sService)
        {
        // import com.tangosol.net.Cluster;
        
        checkReadOnly("suspendService");
        
        Cluster cluster = get_Cluster();
        if (cluster != null)
            {
            cluster.suspendService(sService);
            }
        }
    
    // Declared at the super level
    /**
     * Must be supplemented at each specific Model implementation.
     */
    public void writeExternal(java.io.DataOutput out)
            throws java.io.IOException
        {
        throw new IllegalStateException("ClusterModel is not global");
        }
    }
