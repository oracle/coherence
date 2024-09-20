/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.packager;


import com.tangosol.util.Base;

import  java.io.IOException;
import  java.io.Serializable;

import  java.util.Hashtable;
import  java.util.Iterator;
import  java.util.LinkedList;
import  java.util.List;
import  java.util.Vector;

import  java.lang.reflect.Field;
import  java.lang.reflect.Method;


/**
* This kind of PackagerDependencyElement represents a single Java class.
* These depend on other referenced Java classes, but have only the Java
* class itself as a PackagerEntry.
*/
public class PackagerDependencyClassElement
        extends    Base
        implements Serializable, PackagerDependencyElement
    {
    /**
    * Construct a PackagerDependencyClassElement to model the specified
    * Java class.
    */
    public PackagerDependencyClassElement(String className)
        {
        setClassName(className);
        }

    /**
    * Define two PackagerDependencyClassElements to be equivalent iff
    * their modelled classes are the same.
    */
    public boolean equals(Object obj)
        {
        if (obj instanceof PackagerDependencyClassElement)
            {
            PackagerDependencyClassElement that =
                    (PackagerDependencyClassElement)obj;
            return(this.getClassName().equals(that.getClassName()));
            }
        return(false);
        }

    /**
    * Define the hashCode of a PackagerDependencyClassElement to be that
    * of the modelled class.
    */
    public int hashCode()
        {
        return(getClassName().hashCode());
        }

    /**
    * Return the immediate dependents of this PackagerDependencyElement.
    *
    * This is not intended to be complete, because it's not convenient to
    * determine what classes are used within the methods of a class,
    * but it's ok for testing.
    *
    * This method just collects the superclass, interfaces, types of
    * fields, return types and parameter types of methods.
    */
    public List getDependents(ClassLoader classLoader)
        {
        Hashtable collector = new Hashtable();

        // look at the actual constants section of the class file to get more!
        try
            {
            ConstantPoolEntry[] referencedConstants =
                    ConstantPoolEntry.getClassConstants(m_className, classLoader);
            int numConstants = referencedConstants.length;
            for (int i = 0; i < numConstants; i++)
                {
                ConstantPoolEntry constPoolEntry = referencedConstants[i];
                if (constPoolEntry != null)
                    {
                    Object referencedObject = constPoolEntry.getReferencedObject();
                    if (referencedObject instanceof ClassReference)
                        {
                        ClassReference classRef = (ClassReference)referencedObject;
                        addDependentClass(collector, classRef.getClassName());
                        }
                    }
                }
            }
        catch (IOException ioe)
            {
            err("PDCE: Error getting class constants: " + ioe.getMessage());
            throw new UnexpectedPackagerException(ioe); //TODO: declare the IOException
            }

        // return the dependents in a linked list
        LinkedList list = new LinkedList();
        list.addAll(collector.values());
        return list;
        }

    /**
    * This method is used only by getDependents().
    * Add the specified Class to the collection if it is not the
    * packaged class itself, not primitive, and not already added.
    */
    private void addDependentClass(Hashtable collector, String className)
        {
        if (className.equals(m_className) ||
            className.equals("boolean") ||
            className.equals("byte") ||
            className.equals("char") ||
            className.equals("double") ||
            className.equals("float") ||
            className.equals("int") ||
            className.equals("long") ||
            className.equals("short") ||
            className.equals("void"))
            {
            return;
            }

        if (collector.get(className) == null)
            {
            collector.put(className, new PackagerDependencyClassElement(className));
            }
        }

    /**
    * Return the direct PackagerEntries of this PackagerDependencyElement.
    * This returns a unit-length array where the java package of the
    * packaged class is used to define the directory-style path to the
    * class entry itself.
    */
    public List getPackagerEntries()
        {
        List list = new LinkedList();

        String packageName = null;
        int lastDot = m_className.lastIndexOf('.');
        if (lastDot > 0)
            {
            packageName = m_className.substring(0, lastDot);
            }

        list.add(new PackagerClassEntry(m_className));

        if (packageName != null)
            {
            // gg: the problem I ran into was that j2ee.jar didn't have "javax\ejb" package entry
            // and so "javax.ejb.SessionBean" was not marked as pruned, and the "collectDependents()"
            // algorithm would try to collect dependencies for classes that were known to be excluded!
            // Since I could not see a reason for this entry to be generated, commenting this out
            // was the easiest solution to the problem
            // list.add(new PackagerJavaPackageEntry(packageName)) // this entry will not be pruned!!!
            }
        return list;
        }

    /**
    * Return the Java class modelled by this PackagerDependencyClassElement.
    */
    public String getClassName()
        {
        return m_className;
        }

    /**
    * Set the Java class modelled by this PackagerDependencyClassElement.
    */
    public void setClassName(String className)
        {
        m_className = className;
        }

    /**
    * Return a readable String representation of the
    * PackagerDependencyClassElement, showing the modelled Java class.
    */
    public String toString()
        {
        return("PDCE[" + m_className + "]");
        }


    ///
    ///  unit testing
    ///  args: package-qualified-classname
    ///
    public static void main(String args[])
        {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();

        try
            {
            PackagerDependencyClassElement pdcElem =
                    new PackagerDependencyClassElement(args[0]);

            List dependents = pdcElem.getDependents(classLoader);

            for (Iterator iter = dependents.iterator(); iter.hasNext();)
            out("  " + iter.next());
            }
        catch (Exception e)
            {
            out(e);
            }
        }

    private String m_className;
    }
