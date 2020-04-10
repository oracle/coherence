/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.maven.pof;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.EnumSet;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * @author jk  2018.03.27
 */
public class Copy
    {
    /**
     * Returns {@code true} if okay to overwrite a  file ("cp -i")
     */
    static boolean okayToOverwrite(Path file)
        {
        String answer = System.console().readLine("overwrite %s (yes/no)? ", file);
        return (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes"));
        }

    /**
     * Copy source file to target location. If {@code prompt} is true then
     * prompt user to overwrite target if it exists. The {@code preserve}
     * parameter determines if file attributes should be copied/preserved.
     */
    static void copyFile(Path source, Path target, boolean prompt, boolean preserve)
        {
        CopyOption[] options = (preserve) ?
                new CopyOption[]{COPY_ATTRIBUTES, REPLACE_EXISTING} :
                new CopyOption[]{REPLACE_EXISTING};
        if (!prompt || Files.notExists(target) || okayToOverwrite(target))
            {
            try
                {
                Files.copy(source, target, options);
                }
            catch (IOException x)
                {
                System.err.format("Unable to copy: %s: %s%n", source, x);
                }
            }
        }

    /**
     * A {@code FileVisitor} that copies a file-tree ("cp -r")
     */
    static class TreeCopier
            implements FileVisitor<Path>
        {
        private final Path source;
        private final Path target;
        private final boolean prompt;
        private final boolean preserve;

        TreeCopier(Path source, Path target, boolean prompt, boolean preserve)
            {
            this.source = source;
            this.target = target;
            this.prompt = prompt;
            this.preserve = preserve;
            }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
            {
            // before visiting entries in a directory we copy the directory
            // (okay if directory already exists).
            CopyOption[] options = (preserve) ?
                    new CopyOption[]{COPY_ATTRIBUTES} : new CopyOption[0];

            Path newdir = target.resolve(source.relativize(dir));
            try
                {
                Files.copy(dir, newdir, options);
                }
            catch (FileAlreadyExistsException x)
                {
                // ignore
                }
            catch (IOException x)
                {
                System.err.format("Unable to create: %s: %s%n", newdir, x);
                return SKIP_SUBTREE;
                }
            return CONTINUE;
            }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            {
            copyFile(file, target.resolve(source.relativize(file)),
                     prompt, preserve);
            return CONTINUE;
            }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc)
            {
            // fix up modification time of directory when done
            if (exc == null && preserve)
                {
                Path newdir = target.resolve(source.relativize(dir));
                try
                    {
                    FileTime time = Files.getLastModifiedTime(dir);
                    Files.setLastModifiedTime(newdir, time);
                    }
                catch (IOException x)
                    {
                    System.err.format("Unable to copy all attributes to: %s: %s%n", newdir, x);
                    }
                }
            return CONTINUE;
            }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc)
            {
            if (exc instanceof FileSystemLoopException)
                {
                System.err.println("cycle detected: " + file);
                }
            else
                {
                System.err.format("Unable to copy: %s: %s%n", file, exc);
                }
            return CONTINUE;
            }
        }

    public static void copyDir(Path source, Path target) throws IOException
        {
        copy(source, target, true);
        }

    public static void copy(Path source, Path target, boolean recursive) throws IOException
        {
        copy(new Path[]{source}, target, recursive, false, false);
        }

    public static void copy(Path[] source, Path target, boolean recursive, boolean prompt, boolean preserve) throws IOException
        {
        boolean isDir = Files.isDirectory(target);

        // copy each source file/directory to target
        for (Path path : source)
            {
            Path dest = (isDir) ? target.resolve(path.getFileName()) : target;

            if (recursive)
                {
                // follow links when copying files
                EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
                TreeCopier tc = new TreeCopier(path, dest, prompt, preserve);

                Files.walkFileTree(path, opts, Integer.MAX_VALUE, tc);
                }
            else
                {
                // not recursive so source must not be a directory
                if (Files.isDirectory(path))
                    {
                    System.err.format("%s: is a directory%n", path);
                    continue;
                    }

                copyFile(path, dest, prompt, preserve);
                }
            }
        }

    public static void usage()
        {
        System.err.println("java Copy [-ip] source... target");
        System.err.println("java Copy -r [-ip] source-dir... target");
        System.exit(-1);
        }

    public static void main(String[] args) throws IOException
        {
        boolean recursive = false;
        boolean prompt    = false;
        boolean preserve  = false;

        // process options
        int argi = 0;
        while (argi < args.length)
            {
            String arg = args[argi];
            if (!arg.startsWith("-"))
                {
                break;
                }
            if (arg.length() < 2)
                {
                usage();
                }
            for (int i = 1; i < arg.length(); i++)
                {
                char c = arg.charAt(i);
                switch (c)
                    {
                    case 'r':
                        recursive = true;
                        break;
                    case 'i':
                        prompt = true;
                        break;
                    case 'p':
                        preserve = true;
                        break;
                    default:
                        usage();
                    }
                }
            argi++;
            }

        // remaining arguments are the source files(s) and the target location
        int remaining = args.length - argi;

        if (remaining < 2)
            {
            usage();
            }

        Path[] source = new Path[remaining - 1];
        int    i      = 0;

        while (remaining > 1)
            {
            source[i++] = Paths.get(args[argi++]);
            remaining--;
            }

        Path target = Paths.get(args[argi]);
        copy(source, target, recursive, prompt, preserve);
        }
    }