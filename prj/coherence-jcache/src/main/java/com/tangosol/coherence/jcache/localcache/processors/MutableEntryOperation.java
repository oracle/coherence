/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.jcache.localcache.processors;

import com.tangosol.coherence.jcache.localcache.LocalCacheValue;

/**
 * The operation to perform on a {@link LocalCacheValue} as a result of
 * actions performed on a {@link javax.cache.processor.MutableEntry}.
 *
 * @author jf  2014.01.17
 * @since Coherence 12.1.3
 */
public enum MutableEntryOperation {
  /**
   * Don't perform any operations on the {@link LocalCacheValue}.
   */
  NONE,

  /**
   * Access an existing {@link LocalCacheValue}.
   */
  ACCESS,

  /**
   * Create a new {@link LocalCacheValue}.
   */
  CREATE,

  /**
   * Loaded a new {@link LocalCacheValue}.
   */
  LOAD,

  /**
   * Remove the {@link LocalCacheValue} (and thus the Cache Entry).
   */
  REMOVE,

  /**
   * Update the {@link LocalCacheValue}.
   */
  UPDATE;
}
