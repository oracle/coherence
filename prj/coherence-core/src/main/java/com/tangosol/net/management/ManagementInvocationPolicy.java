/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.management;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.net.management.DiagnosticCommandPolicy;
import com.tangosol.internal.net.management.MBeanCollectorFunction;
import com.tangosol.internal.util.CoherenceMode;

import com.tangosol.io.SerializationRole;

import com.tangosol.net.Invocable;
import com.tangosol.net.events.EventInterceptor;

import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapTrigger;
import com.tangosol.util.OperationReason;
import com.tangosol.util.RemoteExecutablePolicy;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.NeverFilter;

import com.tangosol.util.function.Remote;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import javax.security.auth.Subject;

/**
 * Central policy for management TCMP invocation requests that can reach a
 * local {@link MBeanServer}.
 *
 * @author as 2026.05.14
 * @since 26.07
 */
public final class ManagementInvocationPolicy
    {
    /**
     * Validate an {@link MBeanServerProxy#execute(Remote.Function)} function.
     *
     * @param function  the function to validate
     * @param subject   the current subject, or {@code null}
     * @param sScope    the caller scope
     */
    public static void validateRemoteFunction(Remote.Function function, Subject subject, String sScope)
        {
        if (function == null)
            {
            reject(sScope, "remote-function", "null-function", null, subject);
            }

        Class<?> clz = function.getClass();
        if (!ALLOWED_FUNCTIONS.contains(clz))
            {
            reject(sScope, "remote-function", "function-not-allowed", clz.getName(), subject);
            }

        RemoteExecutablePolicy.current().enforce(clz, OperationReason.INVOKE, SerializationRole.JMX, subject);
        }

    /**
     * Validate a concrete management ObjectName string.
     *
     * @param sName    the management name
     * @param sDomain  the local management domain
     * @param sScope   the caller scope
     *
     * @return the normalized object name
     */
    public static ObjectName validateObjectName(String sName, String sDomain, String sScope)
        {
        return validateObjectName(toObjectName(sName, sDomain, sScope), sScope);
        }

    /**
     * Validate a management query pattern.
     *
     * @param sPattern  the management query pattern
     * @param sDomain   the local management domain
     * @param sScope    the caller scope
     *
     * @return the normalized query pattern
     */
    public static ObjectName validateQueryPattern(String sPattern, String sDomain, String sScope)
        {
        if (sPattern == null || sPattern.trim().isEmpty())
            {
            return null;
            }
        return validateQueryObjectName(toObjectName(sPattern, sDomain, sScope), sScope);
        }

    /**
     * Validate a management query pattern.
     *
     * @param name    the management query pattern
     * @param sScope  the caller scope
     *
     * @return the normalized query pattern
     */
    public static ObjectName validateQueryPattern(ObjectName name, String sScope)
        {
        return validateQueryObjectName(name, sScope);
        }

    /**
     * Validate a matched ObjectName before using it as an MBeanServer target.
     *
     * @param name    the matched ObjectName
     * @param sScope  the caller scope
     *
     * @return the validated ObjectName
     */
    public static ObjectName validateObjectName(ObjectName name, String sScope)
        {
        if (name == null)
            {
            reject(sScope, "object-name", "null-name", null, null);
            }

        String sDomain = name.getDomain();
        if (name.isDomainPattern())
            {
            reject(sScope, "object-name", "invalid-name", "domain-pattern", null);
            }

        if (isPlatformDomain(sDomain))
            {
            reject(sScope, "object-name", "foreign-domain", sDomain, null);
            }

        return name;
        }

    /**
     * Validate a set of ObjectNames returned by a query.
     *
     * @param setNames  the returned names
     * @param sScope    the caller scope
     */
    public static void validateQueryResult(Set<?> setNames, String sScope)
        {
        validateQueryResult(setNames, sScope, false);
        }

    /**
     * Validate a set of ObjectNames returned by a read-only query.
     *
     * @param setNames  the returned names
     * @param sScope    the caller scope
     */
    public static void validateReadQueryResult(Set<?> setNames, String sScope)
        {
        validateQueryResult(setNames, sScope, true);
        }

    /**
     * Validate a set of ObjectNames returned by a query.
     *
     * @param setNames  the returned names
     * @param sScope    the caller scope
     * @param fRead     {@code true} for read-only platform-wrapper paths
     */
    private static void validateQueryResult(Set<?> setNames, String sScope, boolean fRead)
        {
        if (setNames == null)
            {
            return;
            }

        for (Object oName : setNames)
            {
            if (oName instanceof ObjectName)
                {
                if (fRead)
                    {
                    validateReadObjectName((ObjectName) oName, sScope);
                    }
                else
                    {
                    validateObjectName((ObjectName) oName, sScope);
                    }
                }
            else if (oName != null)
                {
                validateObjectName(String.valueOf(oName), DEFAULT_DOMAIN, sScope);
                }
            }
        }

    /**
     * Validate an attribute read.
     *
     * @param server  the MBeanServer
     * @param name    the ObjectName
     * @param sAttr   the attribute name
     * @param sScope  the caller scope
     */
    public static void validateGetAttribute(MBeanServer server, ObjectName name, String sAttr, String sScope)
        {
        if (allowsPlatformReadScope(sScope))
            {
            validateReadObjectName(name, sScope);
            }
        else
            {
            validateObjectName(name, sScope);
            }
        validateIdentifier(sAttr, sScope, "attribute", "invalid-attribute");
        validateAttributeDescriptor(server, name, sAttr, false, sScope);
        }

    /**
     * Validate an attribute write.
     *
     * @param server  the MBeanServer
     * @param name    the ObjectName
     * @param attr    the attribute
     * @param sScope  the caller scope
     */
    public static void validateSetAttribute(MBeanServer server, ObjectName name, Attribute attr, String sScope)
        {
        if (attr == null)
            {
            reject(sScope, "attribute", "null-attribute", null, null);
            }

        validateObjectName(name, sScope);
        validateIdentifier(attr.getName(), sScope, "attribute", "invalid-attribute");
        validateSafeValue(attr.getValue(), sScope, "attribute-value");
        validateAttributeDescriptor(server, name, attr.getName(), true, sScope);
        }

    /**
     * Validate an MBean operation invocation.
     *
     * @param server       the MBeanServer
     * @param name         the ObjectName
     * @param sOperation   the operation name
     * @param aoParam      the parameters
     * @param asSignature  the signature
     * @param sScope       the caller scope
     */
    public static void validateInvoke(MBeanServer server, ObjectName name, String sOperation,
            Object[] aoParam, String[] asSignature, String sScope)
        {
        validateObjectName(name, sScope);
        validateIdentifier(sOperation, sScope, "operation", "invalid-operation");
        validateArguments(aoParam, asSignature, sScope);
        validateOperationDescriptor(server, name, sOperation, asSignature, sScope);
        }

    /**
     * Validate a WrapperJmxModel operation invocation.
     *
     * @param server       the MBeanServer
     * @param name         the ObjectName
     * @param sOperation   the operation name
     * @param aoParam      the parameters
     * @param asSignature  the signature
     */
    public static void validateWrapperInvoke(MBeanServer server, ObjectName name, String sOperation,
            Object[] aoParam, String[] asSignature)
        {
        validateIdentifier(sOperation, "wrapper-jmx", "operation", "invalid-operation");
        validateArguments(aoParam, asSignature, "wrapper-jmx");

        if (isDiagnosticCommand(name) && DiagnosticCommandPolicy.isWrappedJmxOperationAllowed(sOperation))
            {
            validateOperationDescriptor(server, name, sOperation, asSignature, "wrapper-jmx");
            return;
            }

        validateInvoke(server, name, sOperation, aoParam, asSignature, "wrapper-jmx");
        }

    /**
     * Validate parameters and signature values.
     *
     * @param aoParam      the parameters
     * @param asSignature  the signature
     * @param sScope       the caller scope
     */
    public static void validateArguments(Object[] aoParam, String[] asSignature, String sScope)
        {
        if (aoParam != null)
            {
            for (Object oParam : aoParam)
                {
                validateSafeValue(oParam, sScope, "argument");
                }
            }

        if (asSignature != null)
            {
            for (String sType : asSignature)
                {
                validateIdentifier(sType, sScope, "signature", "invalid-signature");
                }
            }
        }

    /**
     * Validate a single management value.
     *
     * @param oValue  the value
     * @param sScope  the caller scope
     * @param sGate   the gate name
     */
    public static void validateSafeManagementValue(Object oValue, String sScope, String sGate)
        {
        validateSafeValue(oValue, sScope, sGate);
        }

    /**
     * Validate a RemoteModel operation against its exposed MBean descriptor.
     *
     * @param info         the exposed MBean descriptor
     * @param nOp          the RemoteModel operation id
     * @param sMethod      the reflected method name
     * @param aoParam      the invocation parameters
     * @param asSignature  the invocation signature
     * @param sScope       the caller scope
     */
    public static void validateRemoteModelInvocation(MBeanInfo info, int nOp, String sMethod,
            Object[] aoParam, String[] asSignature, String sScope)
        {
        validateRemoteModelInvocation(info, null, nOp, sMethod, aoParam, asSignature, sScope);
        }

    /**
     * Validate a RemoteModel operation against its exposed MBean descriptor.
     *
     * @param info         the exposed MBean descriptor
     * @param clzModel     the reflected local model class, or {@code null}
     * @param nOp          the RemoteModel operation id
     * @param sMethod      the reflected method name
     * @param aoParam      the invocation parameters
     * @param asSignature  the invocation signature
     * @param sScope       the caller scope
     */
    public static void validateRemoteModelInvocation(MBeanInfo info, Class<?> clzModel, int nOp, String sMethod,
            Object[] aoParam, String[] asSignature, String sScope)
        {
        if (info == null)
            {
            reject(sScope, "remote-model-descriptor", "descriptor-unavailable", null, null);
            }

        validateIdentifier(sMethod, sScope, "remote-model-method", "invalid-method");

        switch (nOp)
            {
            case REMOTE_MODEL_OP_GET:
                if (!isZeroArg(aoParam) || !isZeroSignature(asSignature) || !isReadableAttributeMethod(info, sMethod))
                    {
                    reject(sScope, "remote-model-method", "attribute-not-readable", sMethod, null);
                    }
                return;

            case REMOTE_MODEL_OP_SET:
                if (!isOneArg(aoParam) || !isWritableAttributeMethod(info, sMethod)
                        || !isAttributeSignature(info, clzModel, sMethod, asSignature, aoParam[0]))
                    {
                    reject(sScope, "remote-model-method", "attribute-not-writable", sMethod, null);
                    }
                return;

            case REMOTE_MODEL_OP_INVOKE:
                if (!isExposedOperation(info, clzModel, sMethod, aoParam, asSignature))
                    {
                    reject(sScope, "remote-model-method", "operation-not-found", sMethod, null);
                    }
                return;

            default:
                reject(sScope, "remote-model-op", "invalid-operation", String.valueOf(nOp), null);
            }
        }

    /**
     * Validate an MBeanAccessor query before it reaches a server-side query.
     *
     * @param query   the query
     * @param sScope  the caller scope
     */
    public static void validateParsedQuery(MBeanAccessor.QueryBuilder.ParsedQuery query, String sScope)
        {
        if (query == null)
            {
            reject(sScope, "query", "null-query", null, null);
            }

        validateQueryPattern(query.getQuery(), DEFAULT_DOMAIN, sScope);
        validateParsedQueryFilters(query.getMapFilters(), sScope);
        }

    /**
     * Validate an attribute filter before it can evaluate MBean metadata.
     *
     * @param filter  the attribute filter
     * @param sScope  the caller scope
     */
    public static void validateAttributeFilter(Filter<?> filter, String sScope)
        {
        if (filter != null && !isAllowedAttributeFilter(filter))
            {
            reject(sScope, "attribute-filter", "filter-not-allowed", filter.getClass().getName(), null);
            }
        }

    /**
     * Validate a direct management query filter.
     *
     * @param filter  the filter
     * @param sScope  the caller scope
     */
    public static void validateQueryFilter(Filter filter, String sScope)
        {
        if (filter != null && !isAllowedQueryFilter(filter))
            {
            reject(sScope, "query-filter", "filter-not-allowed", filter.getClass().getName(), null);
            }
        }

    /**
     * Validate an MBeanCollectorFunction before it reaches a server-side query.
     *
     * @param function  the function
     * @param sScope    the caller scope
     */
    public static void validateCollectorFunction(MBeanCollectorFunction function, String sScope)
        {
        if (function == null)
            {
            reject(sScope, "collector", "null-collector", null, null);
            }
        }

    /**
     * Reject a management TCMP request.
     *
     * @param sScope   the caller scope
     * @param sGate    the gate name
     * @param sReason  the rejection reason
     * @param sValue   a bounded value
     * @param subject  the current subject, or {@code null}
     */
    public static void reject(String sScope, String sGate, String sReason, String sValue, Subject subject)
        {
        if (!CoherenceMode.isSecurityHardeningEnabled() && isCompatibilityShadowReason(sReason))
            {
            shadow(sScope, sGate, sReason, sValue, subject);
            return;
            }

        Logger.warn("Rejected management TCMP request: route=management-tcmp"
                + ", scope=" + sanitize(sScope)
                + ", gate=" + sanitize(sGate)
                + ", reason=" + sanitize(sReason)
                + ", value=" + sanitize(sValue)
                + ", principal=" + sanitize(subject == null || subject.getPrincipals().isEmpty()
                    ? null : subject.getPrincipals().iterator().next().getName()));

        throw new SecurityException("Unsupported management invocation");
        }

    // ----- helper methods -------------------------------------------------

    private static void shadow(String sScope, String sGate, String sReason, String sValue, Subject subject)
        {
        Logger.warn("Allowed compatibility management TCMP request that security hardening would reject:"
                + " route=management-tcmp"
                + ", scope=" + sanitize(sScope)
                + ", gate=" + sanitize(sGate)
                + ", reason=" + sanitize(sReason)
                + ", security-mode=compatibility"
                + ", result=would_reject"
                + ", value=" + sanitize(sValue)
                + ", principal=" + sanitize(subject == null || subject.getPrincipals().isEmpty()
                    ? null : subject.getPrincipals().iterator().next().getName()));
        }

    private static boolean isCompatibilityShadowReason(String sReason)
        {
        return "executable-value".equals(sReason)
                || "attribute-not-writable".equals(sReason)
                || "filter-not-allowed".equals(sReason)
                || "foreign-domain".equals(sReason)
                || "function-not-allowed".equals(sReason)
                || "unsupported-value".equals(sReason);
        }

    private static ObjectName toObjectName(String sName, String sDomain, String sScope)
        {
        if (sName == null || sName.trim().isEmpty() || containsControl(sName))
            {
            reject(sScope, "object-name", "invalid-name", sName, null);
            }

        try
            {
            String sUseDomain = sDomain == null || sDomain.trim().isEmpty() ? DEFAULT_DOMAIN : sDomain;
            int    ofDomain   = sName.indexOf(':');
            int    ofEquals   = sName.indexOf('=');

            if (0 <= ofEquals && ofEquals < ofDomain)
                {
                ofDomain = -1;
                }

            String sQualified = ofDomain == -1
                    ? sUseDomain + ':' + sName
                    : ofDomain == 0
                        ? sUseDomain + sName
                        : sName;

            return new ObjectName(MBeanHelper.quoteCanonical(sQualified));
            }
        catch (MalformedObjectNameException | RuntimeException e)
            {
            reject(sScope, "object-name", "malformed-name", sName, null);
            return null;
            }
        }

    private static boolean isCoherenceDomain(String sDomain)
        {
        return DEFAULT_DOMAIN.equals(sDomain) || sDomain != null && sDomain.startsWith(DEFAULT_DOMAIN + '@');
        }

    private static boolean isPlatformDomain(String sDomain)
        {
        return "java.lang".equals(sDomain)
                || "java.nio".equals(sDomain)
                || "java.util.logging".equals(sDomain)
                || "com.sun.management".equals(sDomain)
                || "JMImplementation".equals(sDomain)
                || "jdk.management.jfr".equals(sDomain)
                || "javax.management.loading".equals(sDomain);
        }

    private static boolean allowsPlatformReadScope(String sScope)
        {
        return "mbean-accessor".equals(sScope) || "wrapper-jmx".equals(sScope);
        }

    private static ObjectName validateReadObjectName(ObjectName name, String sScope)
        {
        if (name == null)
            {
            reject(sScope, "object-name", "null-name", null, null);
            }

        String sDomain = name.getDomain();
        if (name.isDomainPattern())
            {
            reject(sScope, "object-name", "invalid-name", "domain-pattern", null);
            }

        return name;
        }

    private static ObjectName validateQueryObjectName(ObjectName name, String sScope)
        {
        // Query patterns remain broad for inventory compatibility. Every
        // matched name must be passed through validateQueryResult(),
        // validateReadQueryResult(), or equivalent concrete-name validation
        // before it is used as an MBeanServer target.
        if (name == null)
            {
            return null;
            }

        return name;
        }

    private static boolean isDiagnosticCommand(ObjectName name)
        {
        return name != null
                && "com.sun.management".equals(name.getDomain())
                && "DiagnosticCommand".equals(name.getKeyProperty("type"));
        }

    private static void validateAttributeDescriptor(MBeanServer server, ObjectName name, String sAttr,
            boolean fWrite, String sScope)
        {
        MBeanInfo info = getMBeanInfoIfRegistered(server, name, sScope);
        if (info == null)
            {
            return;
            }

        for (MBeanAttributeInfo attr : info.getAttributes())
            {
            if (attr.getName().equals(sAttr) && (fWrite ? attr.isWritable() : attr.isReadable()))
                {
                return;
                }
            }

        reject(sScope, "attribute", fWrite ? "attribute-not-writable" : "attribute-not-readable", sAttr, null);
        }

    private static void validateOperationDescriptor(MBeanServer server, ObjectName name, String sOperation,
            String[] asSignature, String sScope)
        {
        MBeanInfo info = getMBeanInfoIfRegistered(server, name, sScope);
        if (info == null)
            {
            return;
            }

        for (MBeanOperationInfo operation : info.getOperations())
            {
            if (operation.getName().equals(sOperation) && signaturesMatch(operation, asSignature))
                {
                return;
                }
            }

        reject(sScope, "operation", "operation-not-found", sOperation, null);
        }

    private static MBeanInfo getMBeanInfoIfRegistered(MBeanServer server, ObjectName name, String sScope)
        {
        try
            {
            return server != null && server.isRegistered(name)
                    ? server.getMBeanInfo(name)
                    : null;
            }
        catch (SecurityException e)
            {
            throw e;
            }
        catch (Exception e)
            {
            reject(sScope, "descriptor", "descriptor-unavailable", name == null ? null : name.getDomain(), null);
            return null;
            }
        }

    private static boolean signaturesMatch(MBeanOperationInfo operation, String[] asSignature)
        {
        if (asSignature == null)
            {
            return true;
            }

        MBeanParameterInfo[] aParamInfo = operation.getSignature();
        if (aParamInfo.length != asSignature.length)
            {
            return false;
            }

        for (int i = 0; i < asSignature.length; i++)
            {
            if (!signatureTypeMatches(aParamInfo[i].getType(), asSignature[i]))
                {
                return false;
                }
            }
        return true;
        }

    private static boolean isReadableAttributeMethod(MBeanInfo info, String sMethod)
        {
        String sName = getAttributeName(sMethod, true);
        for (MBeanAttributeInfo attr : info.getAttributes())
            {
            if (attr.getName().equals(sName) && attr.isReadable())
                {
                return true;
                }
            }
        return false;
        }

    private static boolean isWritableAttributeMethod(MBeanInfo info, String sMethod)
        {
        String sName = getAttributeName(sMethod, false);
        for (MBeanAttributeInfo attr : info.getAttributes())
            {
            if (attr.getName().equals(sName) && attr.isWritable())
                {
                return true;
                }
            }
        return false;
        }

    private static boolean isAttributeSignature(MBeanInfo info, Class<?> clzModel, String sMethod,
            String[] asSignature, Object oValue)
        {
        if (asSignature != null && asSignature.length > 1)
            {
            return false;
            }

        String sName = getAttributeName(sMethod, false);
        for (MBeanAttributeInfo attr : info.getAttributes())
            {
            if (attr.getName().equals(sName) && attr.isWritable())
                {
                return (asSignature == null || asSignature.length == 0
                        || signatureTypeMatches(attr.getType(), asSignature[0]))
                        && valueTypeMatches(attr.getType(), oValue)
                        && runtimeSelectionMatches(clzModel, sMethod, new String[] {attr.getType()},
                                new Object[] {oValue});
                }
            }
        return false;
        }

    private static boolean isExposedOperation(MBeanInfo info, Class<?> clzModel, String sMethod, Object[] aoParam,
            String[] asSignature)
        {
        int cMatch = 0;
        for (MBeanOperationInfo operation : info.getOperations())
            {
            if (operation.getName().equals(sMethod)
                    && signaturesMatch(operation, asSignature)
                    && runtimeParametersMatch(operation, aoParam)
                    && runtimeSelectionMatches(clzModel, sMethod, descriptorTypes(operation), aoParam))
                {
                cMatch++;
                }
            }
        return cMatch == 1;
        }

    private static boolean runtimeParametersMatch(MBeanOperationInfo operation, Object[] aoParam)
        {
        MBeanParameterInfo[] aParamInfo = operation.getSignature();
        int                  cParam     = aoParam == null ? 0 : aoParam.length;
        if (aParamInfo.length != cParam)
            {
            return false;
            }

        for (int i = 0; i < cParam; i++)
            {
            if (!valueTypeMatches(aParamInfo[i].getType(), aoParam[i]))
                {
                return false;
                }
            }
        return true;
        }

    private static String[] descriptorTypes(MBeanOperationInfo operation)
        {
        MBeanParameterInfo[] aParamInfo = operation.getSignature();
        String[]             asType     = new String[aParamInfo.length];
        for (int i = 0; i < aParamInfo.length; i++)
            {
            asType[i] = aParamInfo[i].getType();
            }
        return asType;
        }

    private static boolean runtimeSelectionMatches(Class<?> clzModel, String sMethod, String[] asDescriptorType,
            Object[] aoParam)
        {
        if (clzModel == null)
            {
            return true;
            }

        if (!hasNullArgument(aoParam))
            {
            Method method = selectRuntimeMethod(clzModel, sMethod, aoParam);
            return method != null && parametersMatchDescriptorStrict(method.getParameterTypes(), asDescriptorType);
            }

        int cDescriptorMatch = 0;
        for (Method method : clzModel.getMethods())
            {
            if (method.getName().equals(sMethod)
                    && !Modifier.isStatic(method.getModifiers())
                    && parametersMatchRuntime(method.getParameterTypes(), aoParam))
                {
                if (parametersMatchDescriptorStrict(method.getParameterTypes(), asDescriptorType))
                    {
                    cDescriptorMatch++;
                    }
                else
                    {
                    return false;
                    }
                }
            }
        return cDescriptorMatch == 1;
        }

    private static Method selectRuntimeMethod(Class<?> clzModel, String sMethod, Object[] aoParam)
        {
        Class<?>[] aclzParam = runtimeTypes(aoParam);
        try
            {
            return clzModel.getMethod(sMethod, aclzParam);
            }
        catch (NoSuchMethodException e)
            {
            // fall through to the same primitive/assignable pass ClassHelper uses
            }

        for (Method method : clzModel.getMethods())
            {
            if (method.getName().equals(sMethod)
                    && !Modifier.isStatic(method.getModifiers())
                    && parametersMatchRuntime(method.getParameterTypes(), aoParam))
                {
                return method;
                }
            }
        return null;
        }

    private static Class<?>[] runtimeTypes(Object[] aoParam)
        {
        int        cParam    = aoParam == null ? 0 : aoParam.length;
        Class<?>[] aclzParam = new Class<?>[cParam];
        for (int i = 0; i < cParam; i++)
            {
            aclzParam[i] = aoParam[i].getClass();
            }
        return aclzParam;
        }

    private static boolean hasNullArgument(Object[] aoParam)
        {
        if (aoParam == null)
            {
            return false;
            }

        for (Object oParam : aoParam)
            {
            if (oParam == null)
                {
                return true;
                }
            }
        return false;
        }

    private static boolean parametersMatchRuntime(Class<?>[] aclzActual, Object[] aoParam)
        {
        int cParam = aoParam == null ? 0 : aoParam.length;
        if (aclzActual.length != cParam)
            {
            return false;
            }

        for (int i = 0; i < cParam; i++)
            {
            Object oParam = aoParam[i];
            if (oParam == null)
                {
                if (aclzActual[i].isPrimitive())
                    {
                    return false;
                    }
                }
            else if (!classTypeMatches(aclzActual[i], oParam.getClass()))
                {
                return false;
                }
            }
        return true;
        }

    private static boolean parametersMatchDescriptorStrict(Class<?>[] aclzActual, String[] asDescriptorType)
        {
        if (aclzActual.length != asDescriptorType.length)
            {
            return false;
            }

        for (int i = 0; i < aclzActual.length; i++)
            {
            if (!asDescriptorType[i].equals(aclzActual[i].getName())
                    && !primitiveMethodMatchesWrapperDescriptor(aclzActual[i], asDescriptorType[i]))
                {
                return false;
                }
            }
        return true;
        }

    private static boolean primitiveMethodMatchesWrapperDescriptor(Class<?> clzActual, String sDescriptorType)
        {
        return clzActual.isPrimitive()
                && primitiveWrapperType(clzActual.getName()).equals(sDescriptorType);
        }

    private static boolean classTypeMatches(Class<?> clzActual, Class<?> clzParam)
        {
        if (clzActual.isPrimitive())
            {
            return primitiveWrapperType(clzActual.getName()).equals(clzParam.getName());
            }
        return clzActual.isAssignableFrom(clzParam);
        }

    private static boolean valueTypeMatches(String sDescriptorType, Object oValue)
        {
        if (oValue == null)
            {
            return !isPrimitiveType(sDescriptorType);
            }

        return signatureTypeMatches(sDescriptorType, oValue.getClass().getName());
        }

    private static String getAttributeName(String sMethod, boolean fRead)
        {
        if (fRead)
            {
            if (sMethod.startsWith("get") && sMethod.length() > 3)
                {
                return sMethod.substring(3);
                }
            if (sMethod.startsWith("is") && sMethod.length() > 2)
                {
                return sMethod.substring(2);
                }
            }
        else if (sMethod.startsWith("set") && sMethod.length() > 3)
            {
            return sMethod.substring(3);
            }
        return sMethod;
        }

    private static boolean isZeroArg(Object[] aoParam)
        {
        return aoParam == null || aoParam.length == 0;
        }

    private static boolean isOneArg(Object[] aoParam)
        {
        return aoParam != null && aoParam.length == 1;
        }

    private static boolean isZeroSignature(String[] asSignature)
        {
        return asSignature == null || asSignature.length == 0;
        }

    private static boolean signatureTypeMatches(String sDescriptorType, String sRequestType)
        {
        return sDescriptorType.equals(sRequestType)
                || primitiveWrapperType(sDescriptorType).equals(sRequestType)
                || sDescriptorType.equals(primitiveWrapperType(sRequestType));
        }

    private static String primitiveWrapperType(String sType)
        {
        switch (sType)
            {
            case "boolean":
                return Boolean.class.getName();
            case "byte":
                return Byte.class.getName();
            case "char":
                return Character.class.getName();
            case "short":
                return Short.class.getName();
            case "int":
                return Integer.class.getName();
            case "long":
                return Long.class.getName();
            case "float":
                return Float.class.getName();
            case "double":
                return Double.class.getName();
            default:
                return sType;
            }
        }

    private static boolean isPrimitiveType(String sType)
        {
        return "boolean".equals(sType)
                || "byte".equals(sType)
                || "char".equals(sType)
                || "short".equals(sType)
                || "int".equals(sType)
                || "long".equals(sType)
                || "float".equals(sType)
                || "double".equals(sType);
        }

    private static void validateIdentifier(String sValue, String sScope, String sGate, String sReason)
        {
        if (sValue == null || sValue.trim().isEmpty() || containsControl(sValue))
            {
            reject(sScope, sGate, sReason, sValue, null);
            }
        }

    private static void validateSafeValue(Object oValue, String sScope, String sGate)
        {
        if (oValue == null)
            {
            return;
            }

        Class<?> clz = oValue.getClass();
        if (clz.isArray())
            {
            int c = java.lang.reflect.Array.getLength(oValue);
            for (int i = 0; i < c; i++)
                {
                validateSafeValue(java.lang.reflect.Array.get(oValue, i), sScope, sGate);
                }
            return;
            }

        if (oValue instanceof Remote || oValue instanceof Invocable || oValue instanceof Filter
                || oValue instanceof ValueExtractor || oValue instanceof ValueUpdater
                || oValue instanceof InvocableMap.EntryProcessor || oValue instanceof InvocableMap.EntryAggregator
                || oValue instanceof MapTrigger || oValue instanceof EventInterceptor
                || oValue instanceof Class || oValue instanceof ClassLoader || oValue instanceof Throwable)
            {
            reject(sScope, sGate, "executable-value", clz.getName(), null);
            }

        if (isAllowedScalarClass(clz) || oValue instanceof Enum)
            {
            return;
            }

        reject(sScope, sGate, "unsupported-value", clz.getName(), null);
        }

    private static void validateParsedQueryFilters(Map<String, Filter<String>> mapFilters, String sScope)
        {
        if (mapFilters == null)
            {
            return;
            }

        for (Map.Entry<String, Filter<String>> entry : mapFilters.entrySet())
            {
            validateIdentifier(entry.getKey(), sScope, "query-filter-key", "invalid-filter-key");
            Filter<String> filter = entry.getValue();
            if (filter == null || !isAllowedParsedQueryFilter(filter))
                {
                reject(sScope, "query-filter", filter == null ? "null-filter" : "filter-not-allowed",
                        filter == null ? null : filter.getClass().getName(), null);
                }
            }
        }

    private static boolean isAllowedScalarClass(Class<?> clz)
        {
        return clz == String.class
                || clz == Boolean.class
                || clz == Character.class
                || clz == Byte.class
                || clz == Short.class
                || clz == Integer.class
                || clz == Long.class
                || clz == Float.class
                || clz == Double.class
                || clz == BigInteger.class
                || clz == BigDecimal.class
                || clz == ObjectName.class;
        }

    private static boolean isAllowedParsedQueryFilter(Filter<?> filter)
        {
        return isAllowedQueryFilter(filter)
                || filter.getClass() == MBeanAccessor.QueryBuilder.NullValueFilter.class
                || filter.getClass() == MBeanAccessor.QueryBuilder.EqualsValueFilter.class;
        }

    private static boolean isAllowedAttributeFilter(Filter<?> filter)
        {
        return isAllowedQueryFilter(filter);
        }

    private static boolean isAllowedQueryFilter(Filter filter)
        {
        return filter.getClass() == AlwaysFilter.class || filter.getClass() == NeverFilter.class;
        }

    private static boolean containsControl(String sValue)
        {
        if (sValue == null)
            {
            return false;
            }

        for (int i = 0, c = sValue.length(); i < c; i++)
            {
            if (Character.isISOControl(sValue.charAt(i)))
                {
                return true;
                }
            }
        return false;
        }

    private static String sanitize(String sValue)
        {
        if (sValue == null)
            {
            return "unknown";
            }

        String        sLower = sValue.toLowerCase(Locale.ROOT);
        StringBuilder sb     = new StringBuilder(Math.min(32, sLower.length()));
        for (int i = 0, c = Math.min(32, sLower.length()); i < c; i++)
            {
            char ch = sLower.charAt(i);
            if (ch >= 'a' && ch <= 'z' || ch >= '0' && ch <= '9' || ch == '-' || ch == '_')
                {
                sb.append(ch);
                }
            }
        return sb.length() == 0 ? "redacted" : sb.toString();
        }

    // ----- constants ------------------------------------------------------

    private static final String DEFAULT_DOMAIN = "Coherence";

    private static final Set<Class<?>> ALLOWED_FUNCTIONS = new HashSet<>(Arrays.asList(
            MBeanAccessor.GetAttributes.class,
            MBeanAccessor.SetAttributes.class,
            MBeanAccessor.Invoke.class,
            MBeanCollectorFunction.class));

    private static final int REMOTE_MODEL_OP_GET = 1;

    private static final int REMOTE_MODEL_OP_INVOKE = 2;

    private static final int REMOTE_MODEL_OP_SET = 3;

    private ManagementInvocationPolicy()
        {
        }
    }
