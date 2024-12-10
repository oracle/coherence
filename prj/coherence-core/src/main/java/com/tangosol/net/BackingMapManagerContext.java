/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import com.tangosol.run.xml.XmlConfigurable;
import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Converter;
import com.tangosol.util.ExternalizableHelper;

import java.util.Map;
import java.util.Set;


/**
* The BackingMapManager context is used by the CacheService to pass information
* to the BackingMapManager during the service initialization. This object also
* allows BackingMapManager instances running on different cluster participate in
* a common service distribution strategy.
*
* @author gg 2002.09.21, 2006.06.06
*
* @since Coherence 2.0
*/
public interface BackingMapManagerContext
        extends XmlConfigurable
    {
    /**
    * Return the BackingMapManager this object is a context for.
    *
    * @return the BackingMapManager this object is a context for
    */
    public BackingMapManager getManager();

    /**
    * Return the CacheService associated with this context.
    *
    * @return the CacheService associated with this context
    */
    public CacheService getCacheService();

    /**
    * Return the ClassLoader associated with this context.
    *
    * @return the ClassLoader associated with this context
    */
    public ClassLoader getClassLoader();

    /**
    * Assign the ClassLoader this context is associated with.
    *
    * @param loader  the ClassLoader associated with this context
    */
    public void setClassLoader(ClassLoader loader);

    /**
    * Return a converter that allows the manager (or a backing map
    * managed thereby) to convert a key object into its internal form
    * as managed by the CacheService.
    *
    * @return the object-to-internal converter
    */
    public Converter getKeyToInternalConverter();

    /**
    * Return a converter that allows the manager (or a backing map
    * managed thereby) to convert a key object from its internal form
    * (as managed by the CacheService) into its "normal" (Object) form.
    *
    * If a ClassLoader is available, it will be used if deserialization
    * is involved in the conversion.
    *
    * @return the internal-to-object converter
    */
    public Converter getKeyFromInternalConverter();

    /**
    * Return a converter that allows the manager (or a backing map
    * managed thereby) to convert a value object into its internal form
    * as managed by the CacheService.
    *
    * @return the object-to-internal converter
    */
    public Converter getValueToInternalConverter();

    /**
    * Return a converter that allows the manager (or a backing map
    * managed thereby) to convert a value object from its internal form
    * (as managed by the CacheService) into its "normal" (Object) form.
    *
    * If a ClassLoader is available, it will be used if deserialization
    * is involved in the conversion.
    *
    * @return the internal-to-object converter
    */
    public Converter getValueFromInternalConverter();

    /**
    * Determines whether or not the specified key (in the internal format)
    * is managed (i.e. controlled) by this service member. In other words,
    * is the specified key under the management of the backing map whose
    * manager this context represents. The key does not have to actually
    * exist for this method to evaluate it; the answer is not backing map-
    * specific.
    *
    * @param oKey  the resource key in the internal format
    *
    * @return true iff the key is managed by this service member
    *
    * @throws ClassCastException if the passed key is not in the internal format
    */
    public boolean isKeyOwned(Object oKey);

    /**
    * Determine the partition to which the specified key belongs.
    *
    * @param oKey  a key in its internal format
    *
    * @return the partition ID that the specified key is assigned to
    *
    * @since Coherence 3.5
    */
    public int getKeyPartition(Object oKey);

    /**
    * Obtain a collection of keys in the internal format that belong to the
    * specified partition for the specified backing map. The returned Set must
    * be used in a read-only manner.
    *
    * @param sCacheName  the cache name for the backing map to retrieve the
    *                    set of keys for
    * @param nPartition  the partition ID
    *
    * @return the Set of keys in the internal format; could be null if the
    *         backing map does not exists or the specified partition is not
    *         owned by this node
    *
    * @since Coherence 3.5
    */
    public Set getPartitionKeys(String sCacheName, int nPartition);

    /**
    * Obtain a reference to the backing map that corresponds to the specified
    * cache name. The returned Map must be used in a read-only manner.
    *
    * @param sCacheName  the cache name
    *
    * @return the backing map reference; null if the backing map does not exist
    *
    * @since Coherence 3.5
    *
    * @deprecated As of Coherence 3.7, use of this method is discouraged.
    *             Instead, use {@link BackingMapContext#getBackingMapEntry}.
    */
    public Map getBackingMap(String sCacheName);

    /**
    * Obtain a reference to the {@link BackingMapContext} that corresponds to
    * the specified cache name.
    * <p>
    * <b>Note:</b> calling this method will not create a backing map for the
    *   specified cache name; it will return null if the cache has yet to be used
    *   (e.g. via {@link ConfigurableCacheFactory#ensureCache(String, ClassLoader)
    *   ConfigurableCacheFactory.ensureCache} call) or has been destroyed
    *
    * @param sCacheName  the cache name
    *
    * @return the corresponding context; null if the cache does not exist
    *
    * @since Coherence 3.7
    */
    public BackingMapContext getBackingMapContext(String sCacheName);


    // ----- Internal value decoration support -------------------------------

    /**
    * Decorate a specified value in the internal form with a specified
    * decoration in the "normal" Object form.
    * It's important to understand that applying the
    * {@link #getValueFromInternalConverter() internal converter}
    * to either passed-in or returned internal values will produce identical
    * values in Object form.
    *
    * @param oValue    a value in the internal form
    * @param nDecorId  a decoration identifier; valid decoration identifiers
    *                   are any of the DECO_* constant values
    * @param oDecor    a decoration value in Object form
    *
    * @return a decorated value in the internal form
    */
    public Object addInternalValueDecoration(Object oValue, int nDecorId, Object oDecor);

    /**
    * Remove a decoration from the specified value in the internal form. If the
    * specified value is not decorated, the call will have no effect.
    * It's important to understand that applying the
    * {@link #getValueFromInternalConverter() internal converter}
    * to either passed-in or returned internal values will produce identical
    * values in Object form.
    *
    * @param oValue    a decorated value in the internal form
    * @param nDecorId  a decoration identifier; valid decoration identifiers
    *                   are any of the DECO_* constant values
    *
    * @return an un-decorated value in the internal form
    */
    public Object removeInternalValueDecoration(Object oValue, int nDecorId);

    /**
    * Check whether or not the specified value in the internal form is decorated.
    *
    * @param oValue    a decorated value in the internal form
    * @param nDecorId  a decoration identifier; valid decoration identifiers
    *                   are any of the DECO_* constant values
    *
    * @return true if the value is decorated using the specified decoration id;
    *          false otherwise
    */
    public boolean isInternalValueDecorated(Object oValue, int nDecorId);

    /**
    * Obtain a decoration from the specified value in the internal form.
    * If the specified value is decorated with the specified decoration id,
    * a value in a "normal" Object form is returned; otherwise null.
    *
    * @param oValue    a decorated value in the internal form
    * @param nDecorId  a decoration identifier; valid decoration identifiers
    *                   are any of the DECO_* constant values
    *
    * @return an un-decorated value in the internal form
    */
    public Object getInternalValueDecoration(Object oValue, int nDecorId);


    // ----- XmlConfigurable ------------------------------------------------

    /**
    * Determine the current configuration of the associated BackingMapManager.
    *
    * @return the XML configuration or null
    */
    public XmlElement getConfig();

    /**
    * Specify the configuration for the associated BackingMapManager.
    * The configuration content is shared between all instances of the
    * corresponding CacheService running on different cluster nodes.
    *
    * @param xml  the XML configuration
    *
    * @exception IllegalStateException  if the object is not in a state that
    *            allows the configuration to be set; for example, if the
    *            manager has already been configured and cannot be reconfigured
    */
    public void setConfig(XmlElement xml);


    // ----- decoration identifiers -----------------------------------------

    /**
    * The decoration id for the value expiry information.
    */
    public static final int DECO_EXPIRY = ExternalizableHelper.DECO_EXPIRY;

    /**
    * The decoration id for the persistent state of the decorated value.
    */
    public static final int DECO_STORE  = ExternalizableHelper.DECO_STORE;

    /**
    * The decoration id for a client specific (opaque) value information.
    */
    public static final int DECO_CUSTOM = ExternalizableHelper.DECO_CUSTOM;
    }