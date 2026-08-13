/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.config.index;

import com.oracle.coherence.ai.Vector;
import com.oracle.coherence.ai.hnsw.HnswIndex;
import com.oracle.coherence.hnswlib.Index;

import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Extractors;
import com.tangosol.util.ValueExtractor;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link HnswIndexConfig} HNSW guardrails.
 *
 * @author Aleks Seovic  2026.05.21
 * @since 26.04
 */
class HnswIndexConfigTest
    {
    @Test
    void shouldRejectInvalidSetters()
        {
        HnswIndexConfig config = new HnswIndexConfig();

        assertThrows(IllegalArgumentException.class, () -> config.setSpaceName(null));
        assertThrows(IllegalArgumentException.class, () -> config.setSpaceName("bad"));
        assertThrows(IllegalArgumentException.class, () -> config.setMaxElements(0));
        assertThrows(IllegalArgumentException.class, () -> config.setMaxElements(Index.MAX_ELEMENTS + 1));
        assertThrows(IllegalArgumentException.class, () -> config.setM(1));
        assertThrows(IllegalArgumentException.class, () -> config.setM(Index.MAX_M + 1));
        assertThrows(IllegalArgumentException.class, () -> config.setEfConstruction(0));
        assertThrows(IllegalArgumentException.class, () -> config.setEfConstruction(Index.MAX_EF_CONSTRUCTION + 1));
        assertThrows(IllegalArgumentException.class, () -> config.setEfSearch(0));
        assertThrows(IllegalArgumentException.class, () -> config.setEfSearch(Index.MAX_EF_SEARCH + 1));
        }

    @Test
    void shouldRejectInvalidPofState()
        {
        HnswIndexConfig config = new HnswIndexConfig();
        setField(config, "m_nM", 1);

        ConfigurablePofContext pofContext = new ConfigurablePofContext("coherence-rag-pof-config.xml");
        Binary                 binary     = ExternalizableHelper.toBinary(config, pofContext);

        assertThrows(RuntimeException.class, () -> ExternalizableHelper.fromBinary(binary, pofContext));
        }

    @Test
    void shouldRejectInvalidApplyState()
        {
        HnswIndexConfig config = new HnswIndexConfig();
        setField(config, "m_nEfSearch", 0);

        ValueExtractor<?, Vector<float[]>> extractor = Extractors.extract("vector");
        HnswIndex<?, ?>                    index     = new HnswIndex<>(extractor, 3);

        assertThrows(IllegalArgumentException.class, () -> config.apply(index));
        }

    private static void setField(Object target, String name, Object value)
        {
        try
            {
            Field field = HnswIndexConfig.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
            }
        catch (ReflectiveOperationException e)
            {
            throw new RuntimeException(e);
            }
        }
    }
