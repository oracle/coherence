/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy;

import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.MetaClass;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.Profile;
import com.oracle.bedrock.runtime.java.ClassPath;
import com.oracle.bedrock.runtime.java.options.JavaModules;
import com.oracle.coherence.common.base.Exceptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ClasspathProfile
        implements Profile, Option
    {
    public ClasspathProfile()
        {
        }

    @Override
    public void onLaunching(Platform platform, MetaClass metaClass, OptionsByType optionsByType)
        {
        String       sPath     = System.getProperty("test.server.classpath");
        File         fileWork  = new File("");
        File         fileBuild = new File(fileWork, "target");
        List<String> list      = new ArrayList<>();

        if (sPath == null)
            {
            if (fileBuild.exists() && fileBuild.isDirectory())
                {
                File fileCP = new File(fileBuild, "server");
                if (fileCP.exists() && fileCP.isDirectory())
                    {
                    sPath = fileCP.getAbsolutePath();
                    }
                }
            }

        addFiles(System.getProperty("user.dir"), list);
        addFiles(sPath, list);

        if (!list.isEmpty())
            {
            if (JavaModules.useModules())
                {
                optionsByType.remove(JavaModules.class);
                optionsByType.add(JavaModules.automatic().withClassPath(ClassPath.of(list)));
                }
            optionsByType.add(ClassPath.of(list));
            }
        }

    @Override
    public void onLaunched(Platform platform, Application application, OptionsByType optionsByType)
        {
        }

    @Override
    public void onClosing(Platform platform, Application application, OptionsByType optionsByType)
        {
        }

    private void addFiles(String sPath, List<String> list)
        {
        if (sPath != null && !sPath.isEmpty())
            {
            String[] asPart = sPath.split(",");
            for (String sPart : asPart)
                {
                File file = new File(sPart);
                if (file.exists() && file.isDirectory())
                    {
                    try
                        {
                        try (DirectoryStream<Path> stream = Files.newDirectoryStream(file.toPath()))
                            {
                            for (Path path : stream)
                                {
                                String s = path.toFile().getAbsolutePath();
                                if (s.endsWith(".jar"))
                                    {
                                    list.add(s);
                                    }
                                }
                            }
                        }
                    catch (IOException e)
                        {
                        throw Exceptions.ensureRuntimeException(e);
                        }
                    }
                }
            }
        }

    // ----- data members ---------------------------------------------------

    public static final ClasspathProfile INSTANCE = new ClasspathProfile();
    }
