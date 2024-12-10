/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import com.tangosol.util.Base;
import com.tangosol.util.LiteMap;

import java.security.Permission;

import java.io.Serializable;

import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;


/**
* This class represents access to a clustered resource such as a Service
* or NamedCache.  A ClusterPermission consists of a target name and a set
* of actions valid for that target.
* <p>
* Target name is a string containing a list of one or more comma-separated
* target attributes. The valid target attributes are:
* <ul>
*   <li> "service=&lt;service-name&gt;"
*   <li> "service=&lt;service-prefix*&gt;"
*   <li> "cache=&lt;cache-name&gt;"
*   <li> "cache=&lt;cache-prefix*&gt;"
*   <li> "*"
* </ul>
*
* If a target string does not contain the "service" attribute, it is
* equivalent to the "service=*" attribute value.
* If a target string does not contain the "cache" attribute, it is
* equivalent to the "cache=*" attribute value or is meant to indicate a service
* that is not a {@link CacheService} (e.g. {@link InvocationService}).
* A target name string consisting of a single "*" indicates all clustered
* resources.
* <p>
* The actions to be granted are passed to the constructor in a string
* containing a list of one or more comma-separated keywords. The possible
* keywords are: "create", "destroy", "join", "all".
* An action string "all" indicates a combination of all valid actions.
* <br>
* Note: the actions string is converted to lowercase before processing.
*
* @author gg  2004.05.28
* @since Coherence 2.5
*/
public final class ClusterPermission
        extends    Permission
        implements Serializable
    {
    /**
    * Construct a ClusterPermission object.
    *
    * @param sTarget  the clustered resource name; must be specified
    * @param sAction  the action(s) name; must be specified
    */
    public ClusterPermission(String sTarget, String sAction)
        {
        this(null, sTarget, sAction);
        }

    /**
    * Construct a ClusterPermission object.
    *
    * @param sClusterName  the cluster name
    * @param sTarget       the clustered resource name; must be specified
    * @param sAction       the action(s) name; must be specified
    */
    public ClusterPermission(String sClusterName, String sTarget, String sAction)
        {
        super(sTarget);

        m_sClusterName = sClusterName;

        parseTarget(sTarget);
        parseAction(sAction);
        }


    // ----- Permission methods ---------------------------------------------

    /**
    * Check if the specified permission's actions are "implied by"
    * this object's actions.
    * <p>
    * There is a slight difference in semantics of the wild card ("*") in "this"
    * and passed-in Permission's target. The specified permission for cache-less
    * services will not contain any "cache=" attribute in the target string,
    * while cache services will always specify a "cache=" attribute.
    *
    * @param permission the permission to check against
    *
    * @return true if the specified permission is implied by this object,
    *         false if not
    */
    public boolean implies(Permission permission)
        {
        if (!(permission instanceof ClusterPermission))
            {
            return false;
            }

        ClusterPermission that = (ClusterPermission) permission;

        int nActionThis = this.m_nActionMask;
        int nActionThat = that.m_nActionMask;

        if ((nActionThis & nActionThat) != nActionThat)
            {
            // action doesn't match
            return false;
            }

        if (this.m_fAllTargets)
            {
            return true;
            }

        if (that.m_fAllTargets)
            {
            return false;
            }

        Map mapServiceThis = this.m_mapService;
        Map mapCacheThis   = this.m_mapCache;
        Map mapServiceThat = that.m_mapService;
        Map mapCacheThat   = that.m_mapCache;

        if (that.m_fAllTargets
         || mapServiceThat.size() > 1
         || mapCacheThat.size()   > 1)
            {
            throw new IllegalArgumentException(
                "Composite permission cannot be implied");
            }

    checkService:
        if (!mapServiceThis.isEmpty())
            {
            for (Iterator iterThis = mapServiceThis.entrySet().iterator();
                    iterThis.hasNext();)
                {
                Map.Entry entryThis = (Map.Entry) iterThis.next();

                for (Iterator iterThat = mapServiceThat.keySet().iterator();
                        iterThat.hasNext();)
                    {
                    String sNameThat = (String) iterThat.next();

                    if (implies(entryThis, sNameThat))
                        {
                        break checkService;
                        }
                    }
                }
            return false;
            }

        if (mapCacheThat.isEmpty())
            {
            // cache-less (e.g. invocation) service join check
            // cannot be allowed by the "cache=*" permission
            return mapCacheThis.isEmpty();
            }
        if (mapCacheThat.size() == 1 && mapCacheThat.containsKey("*"))
            {
            // cache service join check; let it proceed now, since it will be
            // followed by another more precise cache specific check
            return true;
            }

        if (!mapCacheThis.isEmpty())
            {
            for (Iterator iterThis = mapCacheThis.entrySet().iterator();
                    iterThis.hasNext();)
                {
                Map.Entry entryThis = (Map.Entry) iterThis.next();

                for (Iterator iterThat = mapCacheThat.keySet().iterator();
                        iterThat.hasNext();)
                    {
                    String sNameThat = (String) iterThat.next();

                    if (implies(entryThis, sNameThat))
                        {
                        return true;
                        }
                    }
                }
            }
        return false;
        }

    /**
    * Check if the specified name is "implied by" this object's entry.
    *
    * @param entryThis the entry to check
    * @param sNameThat the name to check against
    *
    * @return true iff the specified name is implied by this entry,
    */
    private static boolean implies(Map.Entry entryThis, String sNameThat)
        {
        String sNameThis = (String)  entryThis.getKey();
        if (sNameThis.equals("*"))
            {
            return true;
            }
        Boolean FExact = (Boolean) entryThis.getValue();
        return FExact.booleanValue()
            ? sNameThat.equals(sNameThis)
            : sNameThat.startsWith(sNameThis);
        }

    /**
    * Checks two Permission objects for equality.
    *
    * @param obj the object we are testing for equality with this object
    *
    * @return true if both Permission objects are equivalent
    */
    public boolean equals(Object obj)
        {
        if (obj instanceof ClusterPermission)
            {
            ClusterPermission that = (ClusterPermission) obj;
            return Base.equals(this.getName(), that.getName())
                   && this.m_nActionMask == that.m_nActionMask;
            }
        return false;
        }

    /**
    * Return the hash code value for this ClusterPermission object.
    *
    * @return a hash code value for this object
    */
    public int hashCode()
        {
        return getName().hashCode() + m_nActionMask;
        }


    // ----- Accessors ------------------------------------------------------

    /**
    * Return the actions as a String in a canonical form.
    *
    * @return the actions of this Permission
    */
    public String getActions()
        {
        return formatAction(m_nActionMask);
        }

    /**
    * Return the cluster name.
    *
    * @return the cluster name
    */
    public String getClusterName()
        {
        return m_sClusterName;
        }

    /**
    * Return the service name for this permission object or null
    * if the permission applies to any service.
    *
    * @return the service name for this permission object
    */
    public String getServiceName()
        {
        Map mapService = m_mapService;
        if (mapService.size() == 1)
            {
            String sName = (String) mapService.keySet().iterator().next();
            if (!sName.equals("*"))
                {
                return sName;
                }
            }
        return null;
        }

    /**
    * Return the cache name for this permission object or null
    * if the permission applies to any cache.
    *
    * @return the cache name for this permission object
    */
    public String getCacheName()
        {
        Map mapCache = m_mapCache;
        if (mapCache.size() == 1)
            {
            String sName = (String) mapCache.keySet().iterator().next();
            if (!sName.equals("*"))
                {
                return sName;
                }
            }
        return null;
        }


    // ---- helpers ---------------------------------------------------------

    /**
    * Parse the target string.
    *
    * @param sTarget  the target string
    */
    protected void parseTarget(String sTarget)
        {
        if (sTarget == null || sTarget.length() == 0)
            {
            throw new IllegalArgumentException("Target is not specified");
            }

        if (sTarget.equals("*"))
            {
            m_fAllTargets = true;
            return;
            }

        Map mapService = m_mapService = new LiteMap();
        Map mapCache   = m_mapCache   = new LiteMap();

        StringTokenizer tokens =
            new StringTokenizer(sTarget, ",");

        while (tokens.hasMoreTokens())
            {
            String sToken = tokens.nextToken().trim();

            String sName;
            Map    map;
            if (sToken.startsWith("service="))
                {
                map   = mapService;
                sName = sToken.substring("service=".length());
                }
            else if (sToken.startsWith("cache="))
                {
                map   = mapCache;
                sName = sToken.substring("cache=".length());
                }
            else
                {
                throw new IllegalArgumentException(
                    "Unknown target attribute: " + sToken);
                }

            if (sName.endsWith("*"))
                {
                if (sName.length() > 1)
                    {
                    sName = sName.substring(0, sName.length() - 1);
                    }
                map.put(sName, Boolean.FALSE);
                }
            else
                {
                map.put(sName, Boolean.TRUE);
                }
            }
        }

    /**
    * Parse the action string and set the action flag.
    *
    * @param sAction  the action string
    */
    protected void parseAction(String sAction)
        {
        if (sAction == null || sAction.length() == 0)
            {
            throw new IllegalArgumentException("Action is not specified");
            }

        int nAction = NONE;
        if (sAction.equals("all"))
            {
            nAction = ALL;
            }
        else
            {
            StringTokenizer tokens =
                new StringTokenizer(sAction.toLowerCase(), ", ");

            while (tokens.hasMoreTokens())
                {
                String sToken = tokens.nextToken();
                if (sToken.equals("all"))
                    {
                    nAction |= ALL;
                    }
                else if (sToken.equals("create"))
                    {
                    nAction |= CREATE;
                    }
                else if (sToken.equals("destroy"))
                    {
                    nAction |= DESTROY;
                    }
                else if (sToken.equals("join"))
                    {
                    nAction |= JOIN;
                    }
                else
                    {
                    throw new IllegalArgumentException(
                        "Unknown action name: " + sToken);
                    }
                }
            }
        m_nActionMask = nAction;
        }

    /**
    * Format the action string.
    *
    * @param nAction  the action mask
    *
    * @return the action string
    */
    public static String formatAction(int nAction)
        {
        nAction &= ALL;

        if (nAction == ALL)
            {
            return "all";
            }
        if (nAction == NONE)
            {
            return "none";
            }

        StringBuffer sb = new StringBuffer();
        if ((nAction & CREATE) != 0)
            {
            sb.append(",create");
            }
        if ((nAction & DESTROY) != 0)
            {
            sb.append(",destroy");
            }
        if ((nAction & JOIN) != 0)
            {
            sb.append(",join");
            }
        return sb.substring(1);
        }

    /**
    * Unit test allows to compare the specified permissions.
    * <pre>
    *   java com.tangosol.net.ClusterPermission &lt;target1&gt; &lt;action1&gt; &lt;target2&gt; &lt;action2&gt;
    * </pre>
    */
    public static void main(String[] asArg)
        {
        if (asArg.length != 4)
            {
            Base.out("java com.tangosol.net.ClusterPermission " +
                     "<target1> <action1> <target2> <action2>");
            }
        else
            {
            ClusterPermission perm1 = new ClusterPermission(asArg[0], asArg[1]);
            ClusterPermission perm2 = new ClusterPermission(asArg[2], asArg[3]);

            Base.out("<P1>=" + perm1);
            Base.out("<P2>=" + perm2);
            if (perm1.implies(perm2))
                {
                Base.out("<P1> implies <P2>");
                }
            else
                {
                Base.out("<P1> does not imply <P2>");
                }
            }
        }


    // ----- constants and fields -------------------------------------------

    /**
    * Create action.
    */
    public final static int CREATE  = 0x1;

    /**
    * Create action.
    */
    public final static int DESTROY = 0x2;

    /**
    * Join action.
    */
    public final static int JOIN    = 0x4;

    /**
    * All actions.
    */
    public final static int ALL     = CREATE | DESTROY | JOIN;

    /**
    * No actions.
    */
    public final static int NONE    = 0x0;

    /**
    * The actions mask.
    */
    private int m_nActionMask;

    /**
    * All targets flag.
    */
    private boolean m_fAllTargets;

    /**
    * The set of service entries for the target. The entry key represents
    * a service name or prefix. The entry value is the "exact" match flag.
    */
    private Map m_mapService;

    /**
    * The set of cache entries for the target. The entry key represents
    * a cache name or prefix. The entry value is the "exact" match flag.
    */
    private Map m_mapCache;

    /**
    * The cluster name (optional).
    */
    private String m_sClusterName;
    }
