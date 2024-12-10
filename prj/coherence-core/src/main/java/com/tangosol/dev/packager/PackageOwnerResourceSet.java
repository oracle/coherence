/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.packager;


import  java.util.Arrays;
import  java.util.Iterator;
import  java.util.List;
import  java.util.LinkedList;


/**
* This is a funny sort of PackagerSet that is used only to specify
* membership by packages, for the purpose of excluding classes from
* a packager based on what Java package they're in.
*/
public class PackageOwnerResourceSet
        extends ResourceSet
    {
    /**
    * Construct a PackageOwnerResourceSet.
    */
    public PackageOwnerResourceSet()
        {
        }

    /**
    * Construct a PackageOwnerResourceSet based on the specified package name.
    */
    public PackageOwnerResourceSet(String packageName)
        {
        addOwnedPackageName(packageName);
        }

    /**
    * Return whether the specified PackagerEntry is believed to be in the
    * PackageOwnerPackagerContainer based on whether it is a member of
    * any of the packages owned by the container.
    */
    public boolean containsKey(PackagerPath entryPath)
        {
        String pathName = entryPath.getPathName();

        for (Iterator iter = ownedPackageList.iterator(); iter.hasNext();)
            {
            String packageName = (String) iter.next();
            if (pathName.startsWith(packageName))
                {
                return true;
                }
            }

        return false;
        }

    /**
    * Return the names of the packages specified to be contained by this
    * PackageOwnerResourceSet.
    */
    public String[] getOwnedPackageNames()
        {
        return (String[]) ownedPackageList.toArray(new String[ownedPackageList.size()]);
        }

    /**
    * Assign the names of the packages specified to be contained by this
    * PackageOwnerResourceSet.
    */
    public void setOwnedPackageNames(String[] packageNames)
        {
        azzert(packageNames != null);
        ownedPackageList = new LinkedList(Arrays.asList(packageNames));
        }

    /**
    * Add a package name to the set of packages specified to be contained
    * by this PackageOwnerResourceSet.
    */
    public void addOwnedPackageName(String packageName)
        {
        azzert(packageName != null);

        String packagePath = packageName.replace('.', '/');
        if (!packagePath.endsWith("/"))
            {
            packagePath = packagePath + "/";
            }
        ownedPackageList.add(packagePath);
        }

    private List ownedPackageList = new LinkedList();
    }
