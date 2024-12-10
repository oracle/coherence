/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package config;

import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.ServiceInfo;
import com.tangosol.net.cache.WrapperNamedCache;

import java.util.HashMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MyCache extends WrapperNamedCache {

private MyClass myClass;

private ClassLoader classLoader;

public MyCache() {
this("Test");
}

public MyCache(String cacheName) {
super(new HashMap(), cacheName, mockCacheService());
}

public MyCache(ClassLoader classLoader) {
this("Test");
this.classLoader = classLoader;
}

public MyCache(String cacheName, MyClass myClass) {
this(cacheName);
this.myClass = myClass;
}

public MyCache(NamedCache cache) {
this(cache.getCacheName(), cache);
}

public MyCache(String cacheName, NamedCache cache) {
super(cache, cacheName, cache.getCacheService());
}

public MyClass getMyClass() {
return myClass;
}

public ClassLoader getClassLoader() {
return classLoader;
}

private static CacheService mockCacheService() {
CacheService service = mock(CacheService.class);
ServiceInfo info = mock(ServiceInfo.class);

when(service.getInfo()).thenReturn(info);
when(info.getServiceType()).thenReturn(CacheService.TYPE_LOCAL);

return service;
}
}
