/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.config;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;
import java.util.Objects;

/**
 * A composite configuration key used to address configuration entries in a
 * distributed Coherence {@code NamedMap}.
 * <p/>
 * The key consists of two parts:
 * <ul>
 *   <li><b>key</b> – a required opaque component that uniquely identifies a
 *   configuration subject. For model configuration, the recommended format is
 *   {@code "{type}:{provider}/{model}"} (for example,
 *   {@code "chat:OpenAI/gpt-4o-mini"}, {@code "embedding:OpenAI/text-embedding-3-small"}).
 *   </li>
 *   <li><b>storeName</b> – an optional context component that, when present,
 *   scopes the configuration to a specific store (for example, {@code "docs"}).
 *   A {@code null} store name indicates a global configuration that applies
 *   across all stores.</li>
 * </ul>
 * <p/>
 * Equality and hashing include <i>both</i> the opaque key and the optional
 * store name, allowing concurrent global and store-scoped entries for the same
 * subject to coexist without collision.
 * <p/>
 * This class implements Coherence POF {@link PortableObject} to enable efficient
 * serialization and cross-version evolution. The class is intentionally minimal
 * and immutable by convention once constructed.
 *
 * @author Aleks Seovic 2025.08.06
 * @since 25.09
 */
public class ConfigKey
        implements PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * No-arg constructor for POF deserialization.
     */
    public ConfigKey()
        {
        }

    /**
     * Creates a new {@link ConfigKey} for a global (store-agnostic) entry.
     *
     * @param oKey the required opaque key; must not be {@code null}
     */
    public ConfigKey(Object oKey)
        {
        this(oKey, null);
        }

    /**
     * Creates a new {@link ConfigKey} for a store-scoped entry.
     *
     * @param oKey       the required opaque key; must not be {@code null}
     * @param storeName  the optional store name; {@code null} for global scope
     */
    public ConfigKey(Object oKey, String storeName)
        {
        m_oKey = oKey;
        m_sStoreName = storeName;
        }

    // ---- accessors -------------------------------------------------------

    /**
     * Returns the opaque key component identifying a configuration subject.
     *
     * @return the opaque key component; never {@code null} for valid instances
     */
    public Object key()
        {
        return m_oKey;
        }

    /**
     * Returns the optional store name that scopes this configuration.
     *
     * @return the store name, or {@code null} if this key represents a global configuration
     */
    public String storeName()
        {
        return m_sStoreName;
        }

    // ---- Object methods --------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }
        ConfigKey configKey = (ConfigKey) o;
        return Objects.equals(m_oKey, configKey.m_oKey) && Objects.equals(m_sStoreName, configKey.m_sStoreName);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_oKey, m_sStoreName);
        }

    @Override
    public String toString()
        {
        return "ConfigKey[" +
               "key=" + m_oKey +
               ", storeName='" + m_sStoreName + '\'' +
               ']';
        }

    // ---- PortableObject methods ------------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_oKey = in.readObject(0);
        m_sStoreName = in.readString(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_oKey);
        out.writeString(1, m_sStoreName);
        }

    // ---- data members ----------------------------------------------------

    private Object m_oKey;
    private String m_sStoreName;
    }
