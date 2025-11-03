/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.http;

import com.oracle.coherence.common.net.HttpProxyHelper;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * An annotation that should be applied to tests that need to configure
 * HTTP/HTTPS proxy based on the value of the standard proxy environment
 * variables:
 * <ul>
 *   <li>{@code HTTP_PROXY} or {@code http_proxy} - HTTP proxy URL</li>
 *   <li>{@code HTTPS_PROXY} or {@code https_proxy} - HTTPS proxy URL</li>
 *   <li>{@code NO_PROXY} or {@code no_proxy} - Comma-separated list of hosts to bypass proxy</li>
 * </ul>
 *
 * @author Aleks Seovic  2025.07.16
 * @since 25.09
 *
 * @see HttpProxyHelper
 * @see HttpProxyConfigurer
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(HttpProxyConfigurer.class)
public @interface UseProxy
    {
    }
