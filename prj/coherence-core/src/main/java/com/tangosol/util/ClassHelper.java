/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.internal.util.extractor.ReflectionAllowedFilter;

import com.tangosol.run.xml.XmlElement;

import java.io.File;

import java.math.BigDecimal;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.MissingResourceException;

import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;


/**
* This abstract class contains dynamic (reflect-based) class, method, and
* field manipulation methods.
* <p>
* Note:  This class is primarily for supporting generated code.
*
* @author Cameron Purdy
* @version 1.00, 11/22/96
*/
public abstract class ClassHelper
    {
    /**
    * Determine if the passed object is an instance of the specified class.
    *
    * @param obj   the object
    * @param sClz  the class name
    *
    * @return true iff the passed object is an instance of the specified class
    */
    static public boolean isInstanceOf(Object obj, String sClz)
        {
        try
            {
            return isInstanceOf(obj, Class.forName(sClz));
            }
        catch (Exception e)
            {
            return false;
            }
        }

    /**
    * Determine if the passed object is an instance of the specified class.
    *
    * @param obj  the object
    * @param clz  the class
    *
    * @return true iff the passed object is an instance of the specified class
    */
    static public boolean isInstanceOf(Object obj, Class clz)
        {
        return clz.isAssignableFrom(obj.getClass());
        }

    /**
    * Determine the package name of a class.
    *
    * @param clz  the class to determine the package of
    *
    * @return the name of the package, including a trailing dot (which
    *         signifies that the string is a package name)
    */
    static public String getPackageName(Class clz)
        {
        String sFullClass = clz.getName();
        int    ofDotClass = sFullClass.lastIndexOf('.');
        if (ofDotClass < 0)
            {
            return "";
            }
        else
            {
            return sFullClass.substring(0, ofDotClass + 1);
            }
        }

    /**
    * Determine the package name from a class (or package) name.
    * <pre>
    *   in      out
    *   ------- -------
    *   [blank] [blank]
    *   a       [blank]
    *   .a      .a.
    *   a.      a.
    *   .a.     .a.
    *   a.b     a.
    *   .a.b    .a.
    *   a.b.    a.b.
    *   .a.b.   .a.b.
    * </pre>
    * @param sName  the class name to determine the package of
    *
    * @return the name of the package, including a trailing dot (which
    *         signifies that the string is a package name)
    */
    static public String getPackageName(String sName)
        {
        int ofLastDot = sName.lastIndexOf('.');

        // no dot means no package name
        if (ofLastDot < 0)
            {
            return "";
            }

        // trailing dot means entire string is package name
        if (ofLastDot == sName.length() - 1)
            {
            return sName;
            }

        // dot in middle means qualified name
        if (ofLastDot > 0)
            {
            return sName.substring(0, ofLastDot + 1);
            }

        // otherwise, return whole name with trailing dot as the package name
        return sName + '.';
        }

    /**
    * Determine the simple (unqualified) name of a class.
    *
    * @param clz  the class to determine the simple name of
    *
    * @return the simple name of the class
    */
    static public String getSimpleName(Class clz)
        {
        String sFullClass = clz.getName();
        int    ofDotClass = sFullClass.lastIndexOf('.');
        if (ofDotClass < 0)
            {
            return sFullClass;
            }
        else
            {
            String sName        = sFullClass.substring(ofDotClass + 1);
            int    ofInnerClass = sName.lastIndexOf('$');
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
    * @param sName  the simple or qualified name of the class (or package)
    *
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
    * Build the fully qualified name of a class based on a package name and a
    * simple class name.
    *
    * @param pkg    package name
    * @param sName  simple class name
    *
    * @return fully qualified class name
    */
    static public String getQualifiedName(String pkg, String sName)
        {
        if (pkg.length() == 0)
            {
            return sName;
            }

        if (pkg.charAt(0) == '.')
            {
            pkg = pkg.substring(1);
            }

        if (pkg.charAt(pkg.length()-1) == '.')
            {
            return pkg + sName;
            }
        else
            {
            return pkg + '.' + sName;
            }
        }

    /**
    * Build the composite name of a package based on two package names.  If
    * there is a conflict, the second package name over-rides the first.
    * <pre>
    *   pkg1        pkg2            composite
    *   ----------- --------------  ----------------------
    *   [blank]     [blank]         [blank]
    *   [blank]     yourpkg.        yourpkg.
    *   [blank]     .yourpkg.       .yourpkg.
    *   mypkg.      [blank]         mypkg.
    *   .mypkg.     [blank]         .mypkg.
    *   mypkg.      yourpkg.        yourpkg.
    *   mypkg.      .yourpkg.       mypkg.yourpkg.
    *   .mypkg.     .yourpkg.       .mypkg.yourpkg.
    * </pre>
    *
    * @param pkg1  the first (base) package
    * @param pkg2  the second (extending/overriding) package
    *
    * @return the composite package name, including a trailing dot
    */
    public static String getCompositePackage(String pkg1, String pkg2)
        {
        String sComposite;

        // no base package, use the extending/overriding package
        if (pkg1.length() == 0)
            {
            sComposite = pkg2;
            }
        // no extending/overriding package, use the base package
        else if (pkg2.length() == 0)
            {
            sComposite = pkg1;
            }
        // second package overrides first
        else if (pkg2.charAt(0) != '.')
            {
            sComposite = pkg2;
            }
        // second package extending first, combine the packages
        else if (pkg1.charAt(pkg1.length()-1) == '.')
            {
            // first ends with '.', second starts with '.'; avoid ".."
            sComposite = pkg1 + pkg2.substring(1);
            }
        else
            {
            sComposite = pkg1 + pkg2;
            }

        // make sure package ends with '.'
        if (sComposite.length() > 0 && sComposite.charAt(sComposite.length()-1) != '.')
            {
            sComposite += '.';
            }

        return sComposite;
        }

    /**
    * Build the composite name of a class based on an existing class and a
    * second class name.
    *
    * @param clz    the existing class
    * @param sName  the name to use to build the composite class name
    *
    * @return the composite name of the class
    */
    static public String getCompositeName(Class clz, String sName)
        {
        return getCompositeName(clz.getName(), sName);
        }

    /**
    * Build the composite name of a class based on an existing class name and
    * a second class name.  For example:
    * <pre>
    *   Class       Name           Derived
    *   ----------- -------------- ----------------------
    *   Test        Net            Net
    *   mypkg.Test  Net            mypkg.Net
    *   mypkg.Test  yourpkg.       yourpkg.Test
    *   mypkg.Test  .yourpkg       mypkg.yourpkg.Test
    *   mypkg.Test  .yourpkg.      mypkg.yourpkg.Test
    *   mypkg.Test  yourpkg.Net    yourpkg.Net
    *   mypkg.Test  .yourpkg.Net   mypkg.yourpkg.Net
    *   mypkg.Test  .yourpkg.Net.  mypkg.yourpkg.Net.Test
    * </pre>
    *
    * @param sName1  the existing class name
    * @param sName2  the name to use to build the composite class name
    *
    * @return the composite name of the class
    */
    static public String getCompositeName(String sName1, String sName2)
        {
        String sPkg   = getCompositePackage(getPackageName(sName1), getPackageName(sName2));
        String sClass = getSimpleName(sName2);

        // if the second name didn't include a class, use the class name
        // from the first name
        if (sClass.length() == 0)
            {
            sClass = getSimpleName(sName1);
            }

        return getQualifiedName(sPkg, sClass);
        }

    /**
    * Build the name of a "derived" class based on an existing class and a
    * "derived" class prefix.
    *
    * @param clz      the existing class
    * @param sPrefix  the prefix to apply to the existing class to determine
    *                 the name of the "derived" class
    *
    * @return the expected name of the "derived" class
    */
    static public String getDerivedName(Class clz, String sPrefix)
        {
        return getDerivedName(clz.getName(), sPrefix);
        }

    /**
    * Build the name of a "derived" class based on the name of an existing
    * class and a "derived" class prefix.  For example:
    * <pre>
    *   Class       Prefix        Derived
    *   ----------- ------------- ----------------------
    *   Test        Net           NetTest
    *   mypkg.Test  Net           mypkg.NetTest
    *   mypkg.Test  yourpkg.      yourpkg.Test
    *   mypkg.Test  .yourpkg      mypkg.yourpkg.Test
    *   mypkg.Test  .yourpkg.     mypkg.yourpkg.Test
    *   mypkg.Test  yourpkg.Net   yourpkg.NetTest
    *   mypkg.Test  .yourpkg.Net  mypkg.yourpkg.NetTest
    *   mypkg.Test  .yourpkg.Net. mypkg.yourpkg.Net.Test
    * </pre>
    *
    * @param sName    the existing class name
    * @param sPrefix  the prefix to apply to the existing class to determine
    *                 the name of the "derived" class
    *
    * @return the expected name of the "derived" class
    */
    static public String getDerivedName(String sName, String sPrefix)
        {
        String sPkg    = getCompositePackage(getPackageName(sName), getPackageName(sPrefix));
               sPrefix = getSimpleName(sPrefix);
        String sSuffix = getSimpleName(sName);

        return getQualifiedName(sPkg, sPrefix + sSuffix);
        }

    /**
    * Determine if a partial name (for example, a prefix, class
    * name, package name, etc. as accepted by the above methods)
    * is legal.
    *
    * @param sName  the partial name
    *
    * @return true if the name is a legal partial name
    */
    public static boolean isPartialNameLegal(String sName)
        {
        // name is required
        if (sName == null || sName.length() == 0)
            {
            return false;
            }

        int ofStart = 0;
        int ofEnd   = sName.length();

        // check for leading dot (which is allowed)
        if (sName.charAt(ofStart) == '.')
            {
            ++ofStart;
            }

        // check for trailing dot (which is allowed)
        if (ofEnd > 1 && sName.charAt(ofEnd - 1) == '.')
            {
            --ofEnd;
            }

        return isQualifiedNameLegal(sName.substring(ofStart, ofEnd));
        }

    /**
    * Determine if the passed string is a legal simple name.  This does not
    * check if the name is reserved by the Java language.
    *
    * @param sName  the string containing the name
    *
    * @return true if a legal name, false otherwise
    */
    public static boolean isSimpleNameLegal(String sName)
        {
        char[] ach = sName.toCharArray();
        int    cch = ach.length;

        // verify that the string is a lexically valid identifier
        if (cch < 1 || !Character.isJavaIdentifierStart(ach[0]))
            {
            return false;
            }

        for (int i = 1; i < cch; ++i)
            {
            if (!Character.isJavaIdentifierPart(ach[i]))
                {
                return false;
                }
            }

        return true;
        }

    /**
    * Determine if the passed string is a legal dot-delimited identifier.
    *
    * @param sName  the string containing the dot-delimited identifier
    *
    * @return true if a legal identifier, false otherwise
    */
    public static boolean isQualifiedNameLegal(String sName)
        {
        int cch = sName.length();
        if (cch < 1)
            {
            return false;
            }

        int ofStart = 0;
        while (ofStart < cch)
            {
            int ofEnd = sName.indexOf('.', ofStart);
            if (ofEnd == cch - 1)
                {
                // illegal:  ends with dot
                return false;
                }

            if (ofEnd < 0)
                {
                ofEnd = cch;
                }

            if (!isSimpleNameLegal(sName.substring(ofStart, ofEnd)))
                {
                return false;
                }

            ofStart = ofEnd + 1;
            }

        return true;
        }

    /**
    * Calculate the class array based on the parameter array.
    *
    * @param aoParam  the parameter array
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
    * @param aClasses  an array of classes
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
    * Load the default package resources for the specified class name.
    *
    * @param sClass  the class name (fully qualified) to get the resources for
    *
    * @return the resource bundle for the class's package
    *
    * @see com.tangosol.util.ClassHelper#getResources
    */
    static public Resources getPackageResources(String sClass)
            throws MissingResourceException
        {
        return (Resources) ResourceBundle.getBundle(getCompositeName(sClass, "PackageResources"));
        }

    /**
    * Load the default package resources for the specified class.
    *
    * @param clz  the class to get the resources for
    *
    * @return the resource bundle for the class's package
    *
    * @see com.tangosol.util.ClassHelper#getResources
    */
    static public Resources getPackageResources(Class clz)
            throws MissingResourceException
        {
        return getResources(clz, "PackageResources");
        }

    /**
    * Load the named resources for the specified class.
    *
    * @param clz    the class to get the resources for
    * @param sName  the name of the resource class
    *
    * @return the resource bundle for the class's package
    */
    static public Resources getResources(Class clz, String sName)
            throws MissingResourceException
        {
        return (Resources) ResourceBundle.getBundle(getCompositeName(clz, sName));
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
        tblPrimitives.put(java.lang.Boolean  .class, java.lang.Boolean.TYPE  );
        tblPrimitives.put(java.lang.Character.class, java.lang.Character.TYPE);
        tblPrimitives.put(java.lang.Byte     .class, java.lang.Byte.TYPE     );
        tblPrimitives.put(java.lang.Short    .class, java.lang.Short.TYPE    );
        tblPrimitives.put(java.lang.Integer  .class, java.lang.Integer.TYPE  );
        tblPrimitives.put(java.lang.Long     .class, java.lang.Long.TYPE     );
        tblPrimitives.put(java.lang.Float    .class, java.lang.Float.TYPE    );
        tblPrimitives.put(java.lang.Double   .class, java.lang.Double.TYPE   );
        tblPrimitives.put(java.lang.Void     .class, java.lang.Void.TYPE     );
        }

    /**
    * Instantiate the specified class using the specified parameters.
    *
    * @param clz      the class to instantiate
    * @param aoParam  the constructor parameters
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
        Class[] aclzParam = cParams == 0 ? VOID_PARAMS : new Class[cParams];
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
    * @param clz      the class to invoke the static method of
    * @param sName    the method name
    * @param aoParam  the method arguments
    *
    * @return the return value (if any) from the method
    *
    * @throws NoSuchMethodException     if method with given name cannot be found
    * @throws IllegalAccessException    if the current method does not have access
    * @throws InvocationTargetException if an error occurs during invocation
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
    * @param obj      the object to invoke the instance method of
    * @param sName    the method name
    * @param aoParam  the method arguments
    *
    * @return the return value (if any) from the method
    *
    * @throws NoSuchMethodException     if method with given name cannot be found
    * @throws IllegalAccessException    if the current method does not have access
    * @throws InvocationTargetException if an error occurs during invocation
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
    * @param clz      the class to invoke the method on
    * @param obj      the object to invoke the method on
    * @param sName    the method name
    * @param aoParam  the method arguments
    *
    * @return the return value (if any) from the method invocation
    *
    * @throws NoSuchMethodException     if method with given name cannot be found
    * @throws IllegalAccessException    if the current method does not have access
    * @throws InvocationTargetException if an error occurs during invocation
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
                method = ClassHelper.findMethod(aclzIface[i], sName,
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
     * Obtains the component type of a collection or array (taking generic declarations into account).
     *
     * @param type  the collection or array {@link Type}
     *
     * @return the component type of the collection or <code>null</code> if the specified class is not a collection
     */
    public static Class<?> getComponentType(Type type)
        {
        if (type == null)
            {
            return null;
            }
        else if (type instanceof Class<?>)
            {
            Class<?> clz = (Class<?>)type;

            if (clz.isArray())
                {
                return clz.getComponentType();
                }
            else if (Collection.class.isAssignableFrom(clz))
                {
                //when a collection class is passed, it's type has been erased, so we can only return Object.class
                return Object.class;
                }
            else
                {
                return null;
                }
            }
        else if (type instanceof GenericArrayType)
            {
            return getClass(((GenericArrayType)type).getGenericComponentType());
            }
        else if (type instanceof ParameterizedType)
            {
            ParameterizedType typeParameterized = (ParameterizedType)type;
            Type typeRaw = typeParameterized.getRawType();

            if (typeRaw instanceof Class<?> && Collection.class.isAssignableFrom(((Class<?>)typeRaw)))
                {
                return getClass(typeParameterized.getActualTypeArguments()[0]);
                }
            else
                {
                return null;
                }
            }
        else
            {
            return null;
            }
        }

    /**
     * Obtains the most concrete runtime {@link Class} that may be used for
     * the specified {@link Type} (taking generic declarations into account).
     *
     * @param type  the Type
     *
     * @return the runtime Class for the Type.
     */
    public static Class<?> getClass(Type type)
        {
        if (type == null)
            {
            return Object.class;
            }
        else if (type instanceof Class<?>)
            {
            return (Class<?>) type;
            }
        else if (type instanceof ParameterizedType)
            {
            return getClass(((ParameterizedType) type).getRawType());
            }
        else if (type instanceof WildcardType || type instanceof TypeVariable<?>)
            {
            return Object.class;
            }
        else if (type instanceof GenericArrayType)
            {
            return Object[].class;
            }
        else
            {
            return Object.class;
            }
        }

    /**
     * Returns a map of types by type name as defined in
     * {@code clzDefiningType}. Each type is reified by traversing the class
     * hierarchy commencing from the first found reference of
     * {@code clzDefiningType} in {@code clz}. The traversing completes when
     * either a concrete substitution is found or the hierarchy can no longer
     * be traversed, in which case the bounds will be returned.
     * <pre><code>
     *     public class MyCallable
     *             implements Callable&lt;Boolean&gt;
     *         {
     *         ...
     *         }
     *
     *     Map&lt;String, Type[]&gt; mapTypes = ClassHelper.getReifiedTypes(MyCallable.class, Callable.class);
     *     assert mapTypes.get("V")[0] == Boolean.class;
     *
     * </code></pre>
     * The returned map is ordered thus can be traversed assuming each entry
     * being returned is in the same order as the definition of the generic
     * types in {@code clzDefiningType}.
     *
     * @param clz              the concrete class that can be assigned to
     *                         clzDefiningType
     * @param clzDefiningType  the class (interface or abstract class) that
     *                         holds the types to be reified
     *
     * @return  a map of types by type name
     */
    public static Map<String, Type[]> getReifiedTypes(Class<?> clz, Class<?> clzDefiningType)
        {
        if (clz == null)
            {
            throw new IllegalArgumentException("Class must be provided.");
            }
        if (clzDefiningType == null)
            {
            throw new IllegalArgumentException("Class that defines the types to reify must be provided.");
            }
        if (!clzDefiningType.isAssignableFrom(clz))
            {
            throw new IllegalArgumentException(clz.getName() + " is not assignable to: " + clzDefiningType);
            }

        Class<?>            clzInspect = clz;
        Map<String, Type[]> mapTypes   = new LinkedHashMap<String, Type[]>();
        LinkedList<Entry<Class<?>, Type>> listHier = new LinkedList<Entry<Class<?>, Type>>();

        // Note: the algorithm below bubbles up from the provided clz until
        //       a class is found that implements or extends clzDefiningType.
        //       During the process of finding the interface we track the raw
        //       class and type referring to the next class in the hierarchy.
        //       We then walk down the hierarchy resolving type parameters
        //       until either a concrete type or the lowest bound declaration
        //       is reached for every type defined in clzDefiningType
        TypeVariable<? extends Class<?>>[] aIfaceTypes = clzDefiningType.getTypeParameters();
        Type typeRoot = null;

        // walk up the class hierarchy searching for references of the provided
        // class (clzDefiningType)
        for (Type typeCurr = clzInspect.getGenericSuperclass(); clzInspect != null && typeRoot == null;
             clzInspect = clzInspect.getSuperclass(), typeCurr = clzInspect.getGenericSuperclass())
            {
            Entry<Class<?>, Type> entryHier = new SimpleEntry<Class<?>, Type>(clzInspect, typeCurr);
            listHier.add(entryHier);

            // check class extends clause
            if (clzInspect.getSuperclass() == clzDefiningType)
                {
                typeRoot = typeCurr;
                }
            else
                {
                // check class implements clause
                for (Type type : clzInspect.getGenericInterfaces())
                    {
                    Type typeRaw = type instanceof Class             ? type
                                 : type instanceof ParameterizedType ? ((ParameterizedType) type).getRawType()
                                 : null;

                    if (typeRaw == clzDefiningType)
                        {
                        // an exact interface implementation
                        entryHier.setValue(typeRoot = type);
                        break;
                        }
                    else if (typeRaw instanceof Class && clzDefiningType.isAssignableFrom((Class<?>) typeRaw))
                        {
                        // found an interface that extends the clzDefiningType
                        Class<?>   clzCurIface = (Class<?>) typeRaw;
                        Class<?>[] aclzIfaces;

                        entryHier.setValue(type);

                        // walk up the interface hierarchy until clzDefiningType is found
                        while (clzCurIface != clzDefiningType && (aclzIfaces = clzCurIface.getInterfaces()).length > 0)
                            {
                            for (int i = 0, c = aclzIfaces.length; i < c; ++i)
                                {
                                if (clzDefiningType.isAssignableFrom(aclzIfaces[i]))
                                    {
                                    listHier.add(new SimpleEntry<Class<?>, Type>(clzCurIface, clzCurIface.getGenericInterfaces()[i]));
                                    clzCurIface = aclzIfaces[i];
                                    break;
                                    }
                                }
                            // we should never end up in an indefinite loop as
                            // the typeRaw is assignable to the interface thus
                            // the interface must be referenced in the hierarchy
                            }
                        typeRoot = listHier.getLast().getValue();
                        }
                    }
                }
            }

        // clzInspect & typeRoot refers to the last entry in the hierarchy
        clzInspect = getClass(listHier.removeLast().getKey());

        // as we are at the root that references clzDefiningType there are 2
        // possible states; clzDefiningType is referenced without generics or
        // the types are further specialized or fully defined
        if (typeRoot instanceof Class)
            {
            // found an instance of clzDefiningType erasing generics
            // thus use the default bounds as defined by the interface
            for (TypeVariable<? extends Class<?>> typeVarIface : aIfaceTypes)
                {
                mapTypes.put(typeVarIface.getName(), typeVarIface.getBounds());
                }
            }
        else if (typeRoot instanceof ParameterizedType)
            {
            // the types are either further specialized or fully defined
            Type[] aTypes = ((ParameterizedType) typeRoot).getActualTypeArguments();

            // populate a map of type position in the class' list of generic
            // types and a tuple of original type name and the type's bounds
            Map<Integer, Entry<String, Type[]>> mapGenericTypes = new HashMap();
            for (int i = 0, c = aTypes.length; i < c; ++i)
                {
                Type   typeArg       = aTypes[i];
                String sOrigTypeName = aIfaceTypes[i].getName();

                if (typeArg instanceof Class || typeArg instanceof ParameterizedType)
                    {
                    // a reference to a class; either raw or parameterized
                    // we return the actual parameterized type to retain fidelity
                    // for the caller opposed to extracting the associated raw type
                    mapTypes.put(sOrigTypeName, new Type[] {typeArg});
                    }
                else if (typeArg instanceof TypeVariable)
                    {
                    // a generic type reference found therefore track its
                    // position in the class' list of generic types and its
                    // name and bounds
                    TypeVariable typeVar   = (TypeVariable) typeArg;
                    String       sTypeName = typeVar.getName();
                    int          iTypePos  = 0;

                    for (TypeVariable<? extends Class<?>> typeVarClz : clzInspect.getTypeParameters())
                        {
                        if (sTypeName.equals(typeVarClz.getName()))
                            {
                            if (clzInspect == clz)
                                {
                                mapTypes.put(sOrigTypeName, typeVar.getBounds());
                                }
                            else
                                {
                                mapGenericTypes.put(iTypePos, new SimpleEntry<String, Type[]>(
                                        sOrigTypeName, typeVar.getBounds()));
                                }
                            break;
                            }
                        iTypePos++;
                        }
                    }
                }

            // walk down the class hierarchy until all substitutable types
            // are reified or we reach the lowest type (clz)
            for (Iterator<Entry<Class<?>, Type>> iterClzHier = listHier.descendingIterator();
                 !mapGenericTypes.isEmpty() && iterClzHier.hasNext(); )
                {
                Entry<Class<?>, Type> entryHier = iterClzHier.next();
                Class<?>              clzCurr   = entryHier.getKey();
                Type                  typeSuper = entryHier.getValue();

                if (typeSuper instanceof Class)
                    {
                    // erasing generics thus fall back to the bounds
                    for (Map.Entry<String, Type[]> entry : mapGenericTypes.values())
                        {
                        mapTypes.put(entry.getKey(), entry.getValue());
                        }
                    mapGenericTypes.clear();
                    }
                else if (typeSuper instanceof ParameterizedType)
                    {
                    // further definitions for each type, possibly fully defined
                    // or substitutable type
                    Map<Integer, Entry<String, Type[]>> mapRemaining = new HashMap();
                    for (Entry<Integer, Entry<String, Type[]>> entryGeneric : mapGenericTypes.entrySet())
                        {
                        Entry<String, Type[]> entryGenValue = entryGeneric.getValue();
                        Type                  typeGeneric   = ((ParameterizedType) typeSuper).getActualTypeArguments()[entryGeneric.getKey()];

                        if (typeGeneric instanceof Class)
                            {
                            // fully defined type reference
                            mapTypes.put(entryGenValue.getKey(), new Type[] {typeGeneric});
                            }
                        else if (typeGeneric instanceof TypeVariable)
                            {
                            String sGenTypeName = ((TypeVariable) typeGeneric).getName();
                            int    iTypePos     = 0;
                            for (TypeVariable<? extends Class<?>> typeVarClz : clzCurr.getTypeParameters())
                                {
                                if (sGenTypeName.equals(typeVarClz.getName()))
                                    {
                                    if (iterClzHier.hasNext())
                                        {
                                        // more classes with the opportunity
                                        // to further define the type
                                        // set the bounds to that defined by
                                        // nearest class
                                        entryGenValue.setValue(typeVarClz.getBounds());
                                        mapRemaining.put(iTypePos, entryGenValue);
                                        }
                                    else
                                        {
                                        mapTypes.put(entryGeneric.getValue().getKey(), typeVarClz.getBounds());
                                        }
                                    break;
                                    }
                                iTypePos++;
                                }
                            }
                        }
                    // mapGenericTypes will only hold substitutable types
                    // for evaluation by the next class in the hierarchy
                    mapGenericTypes = mapRemaining;
                    }
                }
            }

        return mapTypes;
        }

    /**
     * Determines the fully qualified class name of the Coherence-style abbreviated class name.
     *
     * @param sAbbreviatedClassName  the abbreviated class name (or a regular class name)
     *
     * @return the fully qualified class name
     */
    public static String getFullyQualifiedClassNameOf(String sAbbreviatedClassName)
        {
        if (sAbbreviatedClassName == null)
            {
            return null;
            }
        else
            {
            String sType = sAbbreviatedClassName.trim();

            // attempt to convert Coherence short-hand class names into the appropriate fully qualified class names
            if (sType.equalsIgnoreCase("string"))
                {
                return String.class.getName();
                }
            else if (sType.equalsIgnoreCase("boolean"))
                {
                return Boolean.class.getName();
                }
            else if (sType.equalsIgnoreCase("int"))
                {
                return Integer.class.getName();
                }
            else if (sType.equalsIgnoreCase("long"))
                {
                return Long.class.getName();
                }
            else if (sType.equalsIgnoreCase("double"))
                {
                return Double.class.getName();
                }
            else if (sType.equalsIgnoreCase("decimal"))
                {
                return BigDecimal.class.getName();
                }
            else if (sType.equalsIgnoreCase("file"))
                {
                return File.class.getName();
                }
            else if (sType.equalsIgnoreCase("date"))
                {
                return Date.class.getName();
                }
            else if (sType.equalsIgnoreCase("time"))
                {
                return Time.class.getName();
                }
            else if (sType.equalsIgnoreCase("datetime"))
                {
                return Timestamp.class.getName();
                }
            else if (sType.equalsIgnoreCase("xml"))
                {
                return XmlElement.class.getName();
                }
            else if (sType.equalsIgnoreCase("classloader"))
                {
                return ClassLoader.class.getName();
                }
            else
                {
                return sType.trim();
                }
            }
        }

    /**
    * Returns {@code true} if {@code oTarget} is a valid type for reflection operations.
    *
    * @param oTarget  the reflection target
    *
    * @return {@code true} if {@code oTarget} is a valid type for reflection operations
    */
    public static boolean isReflectionAllowed(Object oTarget)
        {
        return ReflectionAllowedFilter.INSTANCE.evaluate(oTarget.getClass());
        }

    // ----- constants ------------------------------------------------------

    /**
    * Useful constant for methods with no parameters.
    */
    public final static Class[] VOID_PARAMS = new Class[0];

    /**
    * Useful constant for methods with no arguments.
    */
    public final static Object[] VOID = new Object[0];

    /**
     * Separator for filter patterns.
     */
    public final static String REFLECT_FILTER_SEPARATOR = ReflectionAllowedFilter.REFLECT_FILTER_SEPARATOR;

    /**
     *  Configure reflection allowed class white list and/or black list by setting the value of this
     *  system property to a set of patterns.
     *  <p>
     *  Patterns are separated by ";" (semicolon). Whitespace is significant and
     *  is considered part of the pattern.
     *  <ul>
     *  <li>If the pattern starts with "!", the class is rejected if the remaining pattern is matched;
     *      otherwise the class is allowed if the pattern matches.</li>
     *  <li>If the pattern ends with ".**" it matches any class in the package and all subpackages.</li>
     *  <li>If the pattern ends with ".*" it matches any class in the package.</li>
     *  <li>If the pattern ends with "*", it matches any class with the pattern as a prefix.</li>
     *  <li>If the pattern is equal to the class name, it matches.</li>
     *  <li>Otherwise, the pattern is not matched.</li>
     *  </ul>
     *  <p>
     *  The resulting filter tries to match the class, if any.
     *  The first pattern that matches, working from left to right, determines
     *  if a reflection is allowed on a class by the filter or rejected.
     *  If none of the patterns match the class, reflection will be allowed.
     */
    public final static String REFLECT_FILTER_PROPERTY = ReflectionAllowedFilter.REFLECT_FILTER_PROPERTY;

    /**
     * Value to set system property {@link #REFLECT_FILTER_PROPERTY} to disable the reflection allowed filter.
     */
    public final static String REFLECT_ALLOW_ALL = ReflectionAllowedFilter.REFLECT_ALLOW_ALL;

    /**
     * Default reflection filter list which disables reflection against the following types:
     * <ul>
     *     <li>java.lang.Class</li>
     *     <li>java.lang.System</li>
     *     <li>java.lang.Runtime</li>
     * </ul>
     */
    public final static String DEFAULT_REFLECT_ALLOWED_BLACKLIST = ReflectionAllowedFilter.DEFAULT_REFLECT_ALLOWED_BLACKLIST;

    /**
     * Composed of {@link #DEFAULT_REFLECT_ALLOWED_BLACKLIST} followed by allowing reflection against all other classes.
     */
    public final static String DEFAULT_FILTER_LIST = ReflectionAllowedFilter.DEFAULT_FILTER_LIST;
    }
