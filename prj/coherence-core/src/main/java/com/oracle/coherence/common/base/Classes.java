/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.base;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Hashtable;
import java.util.Vector;

import static com.oracle.coherence.common.base.Formatting.isDecimal;


/**
 * This abstract class contains dynamic (reflect-based) class, method, and
 * field manipulation methods.
 * <p>
 * Note:  This class is primarily for supporting generated code.
 *
 * @author Cameron Purdy
 * @version 1.00, 11/22/96
 */
public abstract class Classes
    {
    /**
     * Determine the simple (unqualified) name of a class.
     *
     * @param clz the class to determine the simple name of
     * @return the simple name of the class
     */
    static public String getSimpleName(Class clz)
        {
        String sFullClass = clz.getName();
        int ofDotClass = sFullClass.lastIndexOf('.');
        if (ofDotClass < 0)
            {
            return sFullClass;
            }
        else
            {
            String sName = sFullClass.substring(ofDotClass + 1);
            int ofInnerClass = sName.lastIndexOf('$');
            return ofInnerClass < 0 ? sName : sName.substring(ofInnerClass + 1);
            }
        }

    /**
     * Determine the simple (unqualified) name of a class.
     * <pre>
     *   in      out
     *   ------- -------
     *   [blank] [blank]
     *   a       a
     *   .a      [blank]
     *   a.      [blank]
     *   .a.     [blank]
     *   a.b     b
     *   .a.b    b
     *   a.b.    [blank]
     *   .a.b.   [blank]
     * </pre>
     *
     * @param sName the simple or qualified name of the class (or package)
     * @return the simple name of the class
     */
    static public String getSimpleName(String sName)
        {
        int ofLastDot = sName.lastIndexOf('.');

        // no dot means no package name
        if (ofLastDot < 0)
            {
            return sName;
            }

        // trailing dot means entire string is package name
        if (ofLastDot == sName.length() - 1)
            {
            return "";
            }

        // dot in middle means qualified name
        if (ofLastDot > 0)
            {
            return sName.substring(ofLastDot + 1);
            }

        // otherwise, whole name is the package name
        return "";
        }

    /**
     * Instantiate the specified class using the specified parameters.
     *
     * @param clz the class to instantiate
     * @param aoParam the constructor parameters
     *
     * @return a new instance of the specified class
     *
     * @throws InstantiationException if an exception is raised trying
     *         to instantiate the object, whether the exception is a
     *         security, method access, no such method, or instantiation
     *         exception
     * @throws InvocationTargetException if the constructor of the new
     *         object instance raises an exception
     */
    static public Object newInstance(Class clz, Object[] aoParam)
            throws InstantiationException, InvocationTargetException
        {
        if (clz == null)
            {
            throw new InstantiationException("Required class object is null");
            }

        if (aoParam == null)
            {
            aoParam = VOID;
            }

        int cParams = aoParam.length;

        // simple instantiation:  no parameters
        if (cParams == 0)
            {
            try
                {
                return clz.newInstance();
                }
            catch (InstantiationException e)
                {
                // InstantiationException could be thrown with an empty message.
                if (e.getMessage() == null)
                    {
                    throw new InstantiationException(clz.getName());
                    }
                else
                    {
                    throw e;
                    }
                }
            catch (IllegalAccessException e)
                {
                throw new InstantiationException(e.toString());
                }
            }

        // determine required constructor parameter types
        Class[] aclzParam   = new Class[cParams];
        boolean fExactMatch = true;
        for (int i = 0; i < cParams; ++i)
            {
            if (aoParam[i] == null)
                {
                fExactMatch = false;
                }
            else
                {
                aclzParam[i] = aoParam[i].getClass();
                }
            }

        // try to find a matching constructor
        Constructor constrMatch = null;

        // check for exact constructor match if all parameters were non-null
        if (fExactMatch)
            {
            try
                {
                constrMatch = clz.getConstructor(aclzParam);
                }
            catch (Exception e)
                {
                }
            }

        if (constrMatch == null)
            {
            // search for first constructor that matches the parameters
            Constructor[] aconstr = clz.getConstructors();
            int cconstr = aconstr.length;
            for (int iconstr = 0; iconstr < cconstr; ++iconstr)
                {
                Constructor constr = aconstr[iconstr];
                Class[] aclzActual = constr.getParameterTypes();
                if (aclzActual.length == cParams)
                    {
                    boolean fMatch = true;
                    for (int i = 0; i < cParams; ++i)
                        {
                        // matchable possibilities:
                        //  1)  null parameter with any reference type
                        //  2)  non-null parameter matching primitive type
                        //  3)  non-null parameter assignable to reference type
                        if (aoParam[i] == null)
                            {
                            // null cannot be assigned to a primitive param
                            fMatch = !aclzActual[i].isPrimitive();
                            }
                        else if (aclzActual[i].isPrimitive())
                            {
                            fMatch = (aclzActual[i] == (Class) tblPrimitives.get(aclzParam[i]));
                            }
                        else
                            {
                            fMatch = aclzActual[i].isAssignableFrom(aclzParam[i]);
                            }

                        // no match try the next constructor
                        if (!fMatch)
                            {
                            break;
                            }
                        }

                    // found valid constructor.
                    if (fMatch)
                        {
                        constrMatch = constr;
                        break;
                        }
                    }
                }
            }

        if (constrMatch != null)
            {
            try
                {
                return constrMatch.newInstance(aoParam);
                }
            catch (InstantiationException e)
                {
                // InstantiationException could be thrown with a null message.
                if (e.getMessage() == null)
                    {
                    throw new InstantiationException(clz.getName());
                    }
                else
                    {
                    throw e;
                    }
                }
            catch (IllegalAccessException e)
                {
                throw new InstantiationException(e.toString());
                }
            catch (SecurityException e)
                {
                throw new InstantiationException(e.toString());
                }
            }

        // did not find a matching constructor
        StringBuilder sb = new StringBuilder();
        sb.append("Could not find a constructor for ");
        sb.append(clz.getName());
        sb.append("(");

        for (int i = 0; i < cParams; ++i)
            {
            if (i > 0)
                {
                sb.append(", ");
                }

            sb.append(aoParam[i] == null ? "null" : aoParam[i].getClass().getName());
            }
        sb.append(")");

        throw new InstantiationException(sb.toString());
        }

    /**
     * Invoke the specified static method using the passed arguments.
     *
     * @param clz the class to invoke the static method of
     * @param sName the method name
     * @param aoParam the method arguments
     *
     * @return the return value (if any) from the method
     */
    static public Object invokeStatic(Class clz, String sName, Object[] aoParam)
            throws NoSuchMethodException,
                           IllegalAccessException,
                           InvocationTargetException
        {
        return invoke(clz, null, sName, aoParam);
        }

    /**
     * Invoke the specified instance method using the passed arguments.
     *
     * @param obj the object to invoke the instance method of
     * @param sName the method name
     * @param aoParam the method arguments
     *
     * @return the return value (if any) from the method
     */
    static public Object invoke(Object obj, String sName, Object[] aoParam)
            throws NoSuchMethodException,
                           IllegalAccessException,
                           InvocationTargetException
        {
        return invoke(obj.getClass(), obj, sName, aoParam);
        }

    /**
     * Invoke the specified method using the passed arguments.
     *
     * @param clz     the class to invoke the method on
     * @param obj     the object to invoke the method on
     * @param sName   the method name
     * @param aoParam the method arguments
     *
     * @return the return value (if any) from the method invocation
     */
    static public Object invoke(Class clz, Object obj, String sName, Object[] aoParam)
            throws NoSuchMethodException,
                           IllegalAccessException,
                           InvocationTargetException
        {
        if (aoParam == null)
            {
            aoParam = VOID;
            }

        // determine method parameter types
        int     cParams   = aoParam.length;
        Class[] aclzParam = cParams == 0 ? VOID_PARAMS : new Class[cParams];
        for (int i = 0; i < cParams; ++i)
            {
            Object oParam = aoParam[i];
            if (oParam != null)
                {
                aclzParam[i] = oParam.getClass();
                }
            }

        // the outermost IllegalAccessException to rethrow if nothing else works
        IllegalAccessException iae = null;

        // search for the first matching method
        boolean fStatic = (obj == null);
        Method  method  = findMethod(clz, sName, aclzParam, fStatic);
        while (method != null)
            {
            try
                {
                return method.invoke(obj, aoParam);
                }
            catch (IllegalAccessException e)
                {
                if (iae == null)
                    {
                    iae = e;
                    }
                }

            // there is a matching method, but it cannot be called as a class
            // method;  this could happen for an interface method implemented
            // by a non-public class; let's look for a matching interface
            Class[] aclzIface = clz.getInterfaces();
            for (int i = 0, c = aclzIface.length; i < c; i++)
                {
                method = com.oracle.coherence.common.base.Classes.findMethod(aclzIface[i], sName,
                        aclzParam, fStatic);
                if (method != null)
                    {
                    try
                        {
                        return method.invoke(obj, aoParam);
                        }
                    catch (IllegalAccessException e) {}
                    }
                }

            clz = clz.getSuperclass();
            if (clz == null)
                {
                throw iae;
                }

            // repeat the entire sequence for a super class
            method = findMethod(clz, sName, aclzParam, fStatic);
            }

        if (iae == null)
            {
            // no matching method was found
            throw new NoSuchMethodException(clz.getName() + '.' + sName);
            }
        else
            {
            // the method is there, but is not callable
            throw iae;
            }
        }

    /**
     * Find a Method that matches the specified name and parameter types.
     * If there are more than one matching methods, the first one will be
     * returned.
     *
     * @param clz        the class reference
     * @param sName      the method name
     * @param aclzParam  the parameter types (some array elements could be null)
     * @param fStatic    the method scope flag
     *
     * @return the matching Method object or null if no match could be found
     */
    public static Method findMethod(Class clz, String sName, Class[] aclzParam, boolean fStatic)
        {
        if (aclzParam == null)
            {
            aclzParam = VOID_PARAMS;
            }

        int     cParams     = aclzParam.length;
        boolean fExactMatch = true;
        for (int i = 0; i < cParams; ++i)
            {
            if (aclzParam[i] == null)
                {
                fExactMatch = false;
                break;
                }
            }

        if (fExactMatch && !fStatic)
            {
            // check for an exact instance method match
            try
                {
                return clz.getMethod(sName, aclzParam);
                }
            catch (NoSuchMethodException e) {}
            }

        Method[] aMethod  = clz.getMethods();
        int      cMethods = aMethod.length;

        NextMethod:
        for (int iMethod = 0; iMethod < cMethods; ++iMethod)
            {
            Method method = aMethod[iMethod];
            if (method.getName().equals(sName) &&
                        Modifier.isStatic(method.getModifiers()) == fStatic)
                {
                Class[] aclzActual = method.getParameterTypes();
                if (aclzActual.length == cParams)
                    {
                    for (int i = 0; i < cParams; ++i)
                        {
                        Class clzParam  = aclzParam[i];
                        Class clzActual = aclzActual[i];

                        // matchable possibilities:
                        //  1)  null parameter with any reference type
                        //  2)  non-null parameter matching primitive type
                        //  3)  non-null parameter assignable to reference type
                        boolean fMatch;
                        if (clzParam == null)
                            {
                            // null cannot be assigned to a primitive param
                            fMatch = !clzActual.isPrimitive();
                            }
                        else if (clzActual.isPrimitive())
                            {
                            fMatch = (clzActual == tblPrimitives.get(clzParam));
                            }
                        else
                            {
                            fMatch = clzActual.isAssignableFrom(clzParam);
                            }

                        if (!fMatch)
                            {
                            continue NextMethod;
                            }
                        }
                    return method;
                    }
                }
            }
        return null;
        }

    /**
     * Calculate the class array based on the parameter array.
     *
     * @return the class array based on the parameter array
     */
    public static Class[] getClassArray(Object[] aoParam)
        {
        if (aoParam != null)
            {
            int cParams = aoParam.length;
            if (cParams > 0)
                {
                Class[] aClass = new Class[cParams];
                for (int i = 0; i < cParams; i++)
                    {
                    Object oParam = aoParam[i];
                    if (oParam != null)
                        {
                        aClass[i] = oParam.getClass();
                        }
                    }
                return aClass;
                }
            }

        return VOID_PARAMS;
        }

    /**
     * Replace wrapper types with appropriate primitive types.
     *
     * @return the class array with primitive instead of wrapper types
     */
    public static Class[] unwrap(Class[] aClasses)
        {
        for (int i = 0; i < aClasses.length; i++)
            {
            Class<?> clz = aClasses[i];
            if (clz == null)            aClasses[i] = Object.class;
            if (clz == Boolean.class)   aClasses[i] = Integer.TYPE;
            if (clz == Byte.class)      aClasses[i] = Byte.TYPE;
            if (clz == Character.class) aClasses[i] = Character.TYPE;
            if (clz == Short.class)     aClasses[i] = Short.TYPE;
            if (clz == Integer.class)   aClasses[i] = Integer.TYPE;
            if (clz == Long.class)      aClasses[i] = Long.TYPE;
            if (clz == Float.class)     aClasses[i] = Float.TYPE;
            if (clz == Double.class)    aClasses[i] = Double.TYPE;
            }

        return aClasses;
        }

    /**
     * Parse the method signature into discrete return type and parameter
     * signatures as they appear in Java .class structures.
     *
     * @param sSig the JVM method signature
     *
     * @return an array of JVM type signatures, where [0] is the return
     *         type and [1]..[c] are the parameter types.
     */
    public static String[] toTypes(String sSig)
        {
        // check for start of signature
        char[] ach = sSig.toCharArray();
        if (ach[0] != '(')
            {
            throw new IllegalArgumentException("JVM Method Signature must start with '('");
            }

        // reserve the first element for the return value
        Vector vect = new Vector();
        vect.addElement(null);

        // parse parameter signatures
        int of = 1;
        while (ach[of] != ')')
            {
            int cch = getTypeLength(ach, of);
            vect.addElement(new String(ach, of, cch));
            of += cch;
            }

        // return value starts after the parameter-stop character
        // and runs to the end of the method signature
        ++of;
        vect.setElementAt(new String(ach, of, ach.length - of), 0);

        String[] asSig = new String[vect.size()];
        vect.copyInto(asSig);

        return asSig;
        }

    private static int getTypeLength(char[] ach, int of)
        {
        switch (ach[of])
            {
            case 'V':
            case 'Z':
            case 'B':
            case 'C':
            case 'S':
            case 'I':
            case 'J':
            case 'F':
            case 'D':
                return 1;

            case '[':
            {
            int cch = 1;
            while (isDecimal(ach[++of]))
                {
                ++cch;
                }
            return cch + getTypeLength(ach, of);
            }

            case 'L':
            {
            int cch = 2;
            while (ach[++of] != ';')
                {
                ++cch;
                }
            return cch;
            }

            default:
                throw new IllegalArgumentException("JVM Type Signature cannot start with '" + ach[of] + "'");
            }
        }

    // ----- Field operations -----------------------------------------------

    /**
     * Provide a Java source representation of a JVM type signature.
     *
     * @param sSig the JVM type signature
     *
     * @return the Java type name as found in Java source code
     */
    public static String toTypeStringField(String sSig)
        {
        switch (sSig.charAt(0))
            {
            case 'V':
                return "void";
            case 'Z':
                return "boolean";
            case 'B':
                return "byte";
            case 'C':
                return "char";
            case 'S':
                return "short";
            case 'I':
                return "int";
            case 'J':
                return "long";
            case 'F':
                return "float";
            case 'D':
                return "double";

            case 'L':
                return sSig.substring(1, sSig.indexOf(';')).replace('/', '.');

            case '[':
                int of = 0;
                while (isDecimal(sSig.charAt(++of)))
                    {}
                return toTypeStringField(sSig.substring(of)) + '[' + sSig.substring(1, of) + ']';

            default:
                throw new IllegalArgumentException("JVM Type Signature cannot start with '" + sSig.charAt(0) + "'");
            }
        }

    /**
     * Provide a boxed version of the given primitive in binary format.
     *
     * @param sSig  the JVM type signature
     *
     * @return the boxed version of the given primitive in binary format
     */
    public static String toBoxedTypeField(String sSig)
        {
        String sBoxedType = "java/lang/";
        switch (sSig.charAt(0))
            {
            case 'V':
                sBoxedType += "Void";
                break;
            case 'Z':
                sBoxedType += "Boolean";
                break;
            case 'B':
                sBoxedType += "Byte";
                break;
            case 'C':
                sBoxedType += "Character";
                break;
            case 'S':
                sBoxedType += "Short";
                break;
            case 'I':
                sBoxedType += "Integer";
                break;
            case 'J':
                sBoxedType += "Long";
                break;
            case 'F':
                sBoxedType += "Float";
                break;
            case 'D':
                sBoxedType += "Double";
                break;
            case 'L':
                if (sSig.startsWith("Ljava/lang/"))
                    {
                    return sSig.substring(1, sSig.length() - 1);
                    }
            case '[':
                // reference and array types are quietly unsupported
                sBoxedType = null;
                break;
            default:
                throw new IllegalArgumentException("JVM Type Signature cannot start with '" + sSig.charAt(0) + "'");
            }
        return sBoxedType;
        }

    /**
     * Provide a boxed version of the given primitive in binary format.
     *
     * @param sSig  the JVM type signature
     *
     * @return the boxed version of the given primitive in binary format
     */
    public static char fromBoxedTypeField(String sSig)
        {
        if (sSig.startsWith("java/lang/"))
            {
            switch (sSig.substring(10))
                {
                case "Void":
                    return 'V';
                case "Boolean":
                    return 'Z';
                case "Byte":
                    return 'B';
                case "Character":
                    return 'C';
                case "Short":
                    return 'S';
                case "Integer":
                    return 'I';
                case "Long":
                    return 'J';
                case "Float":
                    return 'F';
                case "Double":
                    return 'D';
                }
            }
        return 0; // ascii null
        }


    // ----- class loader support ----------------------------------------------

    /**
     * Obtain a non-null ClassLoader.
     *
     * @param loader a ClassLoader (may be null)
     * @return the passed ClassLoader (if not null), or the ContextClassLoader
     */
    public static ClassLoader ensureClassLoader(ClassLoader loader)
        {
        return loader == null ? getContextClassLoader() : loader;
        }

    /**
     * Try to determine the ClassLoader that supports the current context.
     *
     * @return a ClassLoader to use for the current context
     */
    public static ClassLoader getContextClassLoader()
        {
        return getContextClassLoader(null);
        }

    /**
     * Try to determine the ClassLoader that supports the current context.
     *
     * @param o the calling object, or any object out of the application
     *          that is requesting the class loader
     * @return a ClassLoader to use for the current context
     */
    public static ClassLoader getContextClassLoader(Object o)
        {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null)
            {
            if (o != null)
                {
                loader = o.getClass().getClassLoader();
                }
            if (loader == null)
                {
                loader = ClassLoader.class.getClassLoader();
                if (loader == null)
                    {
                    loader = ClassLoader.getSystemClassLoader();
                    }
                }
            }
        return loader;
        }

    /**
     * Attempt to load the specified class using sequentionally all of the
     * specified loaders, the ContextClassLoader and the current class loader.
     *
     * @param sClass   the class name
     * @param loader1  first ClassLoader to try
     * @param loader2  second ClassLoader to try
     *
     * @return the Class for the specified name
     *
     * @throws ClassNotFoundException if all the attempts fail
     */
    public static Class loadClass(String sClass, ClassLoader loader1, ClassLoader loader2)
            throws ClassNotFoundException
        {
        for (int i = 1; i <= 3; i++)
            {
            ClassLoader loader;
            switch (i)
                {
                case 1:
                    loader = loader1;
                    break;

                case 2:
                    loader = loader2;
                    break;

                case 3:
                    loader = getContextClassLoader();
                    if (loader == loader1 || loader == loader2)
                        {
                        loader = null;
                        }
                    break;

                default:
                    throw new IllegalStateException();
                }

            try
                {
                if (loader != null)
                    {
                    return Class.forName(sClass, false, loader);
                    }
                }
            catch (ClassNotFoundException e) {}
            }

        // nothing worked; try the current class loader as a last chance
        return Class.forName(sClass);
        }


    // ----- class formatting support ----------------------------------------------

    /**
     * Formats Class information for debug output purposes.
     *
     * @param clz the Class to print information for
     * @return a String describing the Class in detail
     */
    public static String toString(Class clz)
        {
        if (clz.isPrimitive())
            {
            return clz.toString();
            }
        else if (clz.isArray())
            {
            return "Array of " + toString(clz.getComponentType());
            }
        else if (clz.isInterface())
            {
            return toInterfaceString(clz, "");
            }
        else
            {
            return toClassString(clz, "");
            }
        }

    /**
     * Formats Class information for debug output purposes.
     *
     * @param clz     the Class to print information for
     * @param sIndent the indentation to precede each line of output
     * @return a String describing the Class in detail
     */
    private static String toClassString(Class clz, String sIndent)
        {
        StringBuilder sb = new StringBuilder();
        sb.append(sIndent)
                .append("Class ")
                .append(clz.getName())
                .append("  (")
                .append(toString(clz.getClassLoader()))
                .append(')');

        sIndent += "  ";

        Class[] aclz = clz.getInterfaces();
        for (int i = 0, c = aclz.length; i < c; ++i)
            {
            sb.append('\n')
                    .append(toInterfaceString(aclz[i], sIndent));
            }

        clz = clz.getSuperclass();
        if (clz != null)
            {
            sb.append('\n')
                    .append(toClassString(clz, sIndent));
            }

        return sb.toString();
        }

    /**
     * Formats interface information for debug output purposes.
     *
     * @param clz     the interface Class to print information for
     * @param sIndent the indentation to precede each line of output
     * @return a String describing the interface Class in detail
     */
    private static String toInterfaceString(Class clz, String sIndent)
        {
        StringBuilder sb = new StringBuilder();
        sb.append(sIndent)
                .append("Interface ")
                .append(clz.getName())
                .append("  (")
                .append(toString(clz.getClassLoader()))
                .append(')');

        Class[] aclz = clz.getInterfaces();
        for (int i = 0, c = aclz.length; i < c; ++i)
            {
            clz = aclz[i];

            sb.append('\n')
                    .append(toInterfaceString(clz, sIndent + "  "));
            }

        return sb.toString();
        }

    /**
     * Format a description for the specified ClassLoader object.
     *
     * @param loader the ClassLoader instance (or null)
     * @return a String description of the ClassLoader
     */
    private static String toString(ClassLoader loader)
        {
        if (loader == null)
            {
            return "System ClassLoader";
            }

        return "ClassLoader class=" + loader.getClass().getName()
                       + ", hashCode=" + loader.hashCode();
        }

    /**
     * Maps specific classes to their primitive "alter egos".  These classes
     * exist independently as instantiable classes but are also used by the
     * JVM reflection to pass primitive values as objects.  The JVM creates
     * java.lang.Class instances for each of the primitive types in order to
     * describe parameters and return values of constructors and methods.
     * For example, a parameter described as class java.lang.Boolean.TYPE
     * requires an instance of the class java.lang.Boolean as an argument.
     * (Note that a parameter described as class java.lang.Boolean also
     * requires an instance of the class java.lang.Boolean as an argument.
     * This parameter/argument asymmetry only occurs with primitive types.)
     */
    static private Hashtable tblPrimitives;
    static
        {
        tblPrimitives = new Hashtable(10, 1.0F);
        tblPrimitives.put(Boolean  .class, Boolean.TYPE  );
        tblPrimitives.put(Character.class, Character.TYPE);
        tblPrimitives.put(Byte     .class, Byte.TYPE     );
        tblPrimitives.put(Short    .class, Short.TYPE    );
        tblPrimitives.put(Integer  .class, Integer.TYPE  );
        tblPrimitives.put(Long     .class, Long.TYPE     );
        tblPrimitives.put(Float    .class, Float.TYPE    );
        tblPrimitives.put(Double   .class, Double.TYPE   );
        tblPrimitives.put(Void     .class, Void.TYPE     );
        }

    // ---- constants -------------------------------------------------------

    /**
     * Useful constant for methods with no parameters.
     */
    public final static Class[] VOID_PARAMS = new Class[0];

    /**
     * Useful constant for methods with no arguments.
     */
    public final static Object[] VOID = new Object[0];
    }
