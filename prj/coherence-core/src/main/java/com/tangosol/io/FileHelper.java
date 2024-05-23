/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.channels.FileLock;

import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * A Collection of helper methods for files.
 *
 * @author jh  2012.07.13
 */
public class FileHelper
    {
    // ----- filename utility methods ---------------------------------------

    /**
     * Determine if the given character is unsafe to use in a filename.
     *
     * @param ch  the character in question
     *
     * @return true if the given character should be avoided in a filename
     */
    private static boolean isUnsafeChar(char ch)
        {
        // see:
        // http://www.mtu.edu/umc/services/web/resources/cms/characters-to-avoid.html
        // http://stackoverflow.com/questions/620605/how-to-make-a-valid-windows-filename-from-an-arbitrary-string/
        // http://www.unicodemap.org/range/1/Basic_Latin/

        return (ch >= 0x0000 && ch <= 0x0020) || // non-printable and whitespace
               (ch >= 0x0021 && ch <= 0x0027) || // !, ", #, $, %, &, '
                ch == '*'                     ||
                ch == '+'                     ||
                ch == '.'                     ||
                ch == '/'                     ||
                ch == ':'                     ||
               (ch >= 0x003c && ch <= 0x0040) || // <, =, >, ?, @
                ch == '\\'                    ||
                ch == '`'                     ||
               (ch >= 0x007b && ch <= 0x007d);   // {, |, }
        }

    /**
     * Given a string, return a derivative version that is safe to use for a
     * filename.
     *
     * @param sName  the string
     *
     * @return a derivative of the given string that is safe to use as a
     *         filename (may be the same as the string supplied)
     */
    public static String toFilename(String sName)
        {
        final char REPLACEMENT = '-';

        // if the given name is empty, return a single character string
        int cch = sName == null ? 0 : sName.length();
        if (cch == 0)
            {
            return String.valueOf(REPLACEMENT);
            }

        // scan the given string for unsafe characters, replacing them with
        // a safe alternative
        boolean fNew = false;
        char[]  ach  = new char[cch];
        for (int i = 0; i < cch; ++i)
            {
            char ch = sName.charAt(i);
            if (isUnsafeChar(ch))
                {
                ch   = REPLACEMENT;
                fNew = true;
                }
            ach[i] = ch;
            }

        return fNew ? new String(ach) : sName;
        }

    /**
     * Return the path of a file.
     * <p>
     * The implementation attempts to get the canonical path and iff this call
     * abruptly fails is the absolute path returned.
     *
     * @param file  the {@link File} to be interrogated to return the path
     *
     * @return the path of the provided file
     */
    public static String getPath(File file)
        {
        try
            {
            return file.getCanonicalPath();
            }
        catch (IOException ignore) {}

        return file.getAbsolutePath();
        }

    // ----- directory utility methods --------------------------------------

    /**
     * Validate that the given File represents a directory, creating it if
     * it doesn't already exist.
     *
     * @param file  the File to check
     *
     * @return the validated File
     *
     * @throws IOException on error creating the directory
     */
    public static File ensureDir(File file)
            throws IOException
        {
        if (file == null)
            {
            throw new IllegalArgumentException("null file");
            }

        return file.isDirectory() ? file
                : Files.createDirectories(file.toPath()).toFile();
        }

    /**
     * Validate that the given File exists and represents a directory.
     *
     * @param file  the File to check
     *
     * @throws IOException if the given File doesn't exist or doesn't
     *         represent a directory
     */
    public static void validateDir(File file)
            throws IOException
        {
        if (file == null)
            {
            throw new IllegalArgumentException("null file");
            }

        Path path = file.toPath();
        if (!Files.isDirectory(path))
            {
            throw new IOException("the specified path \"" + path
                    + "\" is not a directory");
            }
        }

    /**
     * Create a deep copy of the specified directory into the specified target
     * directory.
     *
     * @param fileDirFrom  the directory to copy from
     * @param fileDirTo    the directory to copy into
     *
     * @throws IOException if an error occurred copying the directory
     */
    public static void copyDir(File fileDirFrom, File fileDirTo)
            throws IOException
        {
        validateDir(fileDirFrom);
        if (fileDirTo == null)
            {
            throw new IllegalArgumentException("null file");
            }

        // ensure that the parent directory of the destination path exists
        File fileParent = fileDirTo.getParentFile();
        if (fileParent != null)
            {
            ensureDir(fileParent);
            }

        Path pathFrom = fileDirFrom.toPath();
        Path pathTo   = fileDirTo.toPath();

        Files.walkFileTree(pathFrom, new CopyDirVisitor(pathFrom, pathTo));
        }

    /**
     * Delete a directory recursively.
     *
     * @param fileDir  directory to delete
     *
     * @throws IOException if an error occurred deleting the directory
     */
    public static void deleteDir(File fileDir)
            throws IOException
        {
        Path pathDir = fileDir.toPath();
        if (Files.exists(pathDir))
            {
            validateDir(fileDir);
            Files.walkFileTree(pathDir, new DeleteDirVisitor());
            }
        }

    /**
     * Recursively delete a directory suppressing any raised exceptions.
     *
     * @param fileDir  directory to delete
     */
    public static void deleteDirSilent(File fileDir)
        {
        try
            {
            deleteDir(fileDir);
            }
        catch (Exception ioe) {}
        }

    /**
     * Move the specified directory to the specified destination path.
     *
     * @param fileDirFrom  the directory to move
     * @param fileDirTo    the destination path
     *
     * @throws IOException if an error occurred moving the directory
     */
    public static void moveDir(File fileDirFrom, File fileDirTo)
            throws IOException
        {
        validateDir(fileDirFrom);
        if (fileDirTo == null)
            {
            throw new IllegalArgumentException("null file");
            }

        // ensure that the parent directory of the destination path exists
        File fileParent = fileDirTo.getParentFile();
        if (fileParent != null)
            {
            ensureDir(fileParent);
            }

        Path pathFrom = fileDirFrom.toPath();
        Path pathTo   = fileDirTo.toPath();

        // first attempt an atomic move
        try
            {
            Files.move(pathFrom,
                    pathTo,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            }
        catch (AtomicMoveNotSupportedException e)
            {
            // if the atomic move failed, copy the directory to the
            // destination and then delete it
            Files.walkFileTree(pathFrom, new CopyDirVisitor(pathFrom, pathTo));
            Files.walkFileTree(pathFrom, new DeleteDirVisitor());
            }
        }

    /**
     * Create a unique temporary directory.
     *
     * @return a unique temporary directory
     */
    public static File createTempDir()
            throws IOException
        {
        File file = Files.createTempDirectory(null).toFile();
        file.deleteOnExit();
        return file;
        }

    /**
     * Return the approximate disk usage in bytes for a given directory. Note
     * the size may differ from the actual size on the file system due to
     * compression, support for sparse files, or other reasons.
     *
     * @param fileDir  the directory to size
     *
     * @return the number of total bytes used by the files in the directory
     */
    public static long sizeDir(File fileDir)
        {
        Path pathDir = fileDir == null ? null : fileDir.toPath();
        if (pathDir == null || !Files.isDirectory(pathDir))
            {
            // COH-10194
            // Ignore this as there is the possibility that one of the
            // directories as this operation is running. We don't want to
            // throw the exception, just return 0L to indicate we are not
            // counting size in this instance.
            return 0L;
            }

        SizeDirVisitor visitor = new SizeDirVisitor();
        try
            {
            Files.walkFileTree(pathDir, visitor);
            }
        catch (IOException e)
            {
            // again, ignore as this is just an approximation
            }

        return visitor.getSize();
        }

    // ----- file utility methods -------------------------------------------

    /**
     * Copy the specified file according to the source and destination File
     * objects preserving the time-stamp of the source.
     *
     * @param fileFrom  the {@link File} to copy from
     * @param fileTo    the {@link File} to copy to
     *
     * @throws IOException if an error occurred copying the file
     */
    public static void copyFile(File fileFrom, File fileTo)
            throws IOException
        {
        if (fileFrom == null || fileTo == null)
            {
            throw new IllegalArgumentException("null file");
            }

        // ensure that the parent directory of the destination path exists
        File fileParent = fileTo.getParentFile();
        if (fileParent != null)
            {
            ensureDir(fileParent);
            }

        Files.copy(fileFrom.toPath(),
                   fileTo.toPath(),
                   StandardCopyOption.REPLACE_EXISTING,
                   StandardCopyOption.COPY_ATTRIBUTES,
                   LinkOption.NOFOLLOW_LINKS);
        }

    /**
     * Move the specified file to the specified destination path.
     *
     * @param fileFrom  the file to move
     * @param fileTo    the destination path
     *
     * @throws IOException if an error occurred moving the file
     */
    public static void moveFile(File fileFrom, File fileTo)
            throws IOException
        {
        if (fileFrom == null || fileTo == null)
            {
            throw new IllegalArgumentException("null file");
            }

        // ensure that the parent directory of the destination path exists
        File fileParent = fileTo.getParentFile();
        if (fileParent != null)
            {
            ensureDir(fileParent);
            }

        Files.move(fileFrom.toPath(), fileTo.toPath());
        }

    /**
     * Obtain an exclusive lock on the specified file. The lock should be
     * released with the {@link #unlockFile(FileLock) unlockFile} method.
     *
     * @param file  the file to lock
     *
     * @return a <tt>FileLock</tt> if an exclusive lock was obtained,
     *         <tt>null</tt> otherwise
     */
    public static FileLock lockFile(File file)
        {
        FileLock         lock = null;
        FileOutputStream out  = null;
        try
            {
            // try to obtain a lock on the lock file
            out  = new FileOutputStream(file);
            lock = out.getChannel().tryLock();
            }
        catch (Throwable t)
            {
            // fall through
            }
        finally
            {
            if (lock == null && out != null)
                {
                try
                    {
                    // will also close all Channels
                    out.close();
                    }
                catch (IOException e)
                    {
                    // ignore
                    }
                }
            }

        return lock;
        }

    /**
     * Release an {@link #lockFile(File) exclusive lock} on a file.
     *
     * @param lock  the {@link #lockFile(File) exclusive lock} to release
     */
    public static void unlockFile(FileLock lock)
        {
        // release the file lock
        try
            {
            lock.release();
            }
        catch (IOException e)
            {
            // ignore
            }

        // close the lock file
        try
            {
            lock.channel().close();
            }
        catch (IOException e)
            {
            // ignore
            }
        }

    /**
     * Checks if the character is a separator.
     *
     * @param ch the character to check
     *
     * @return true if it is a separator character
     */
    private static boolean isSeparator(final char ch)
        {
        return ch == UNIX_SEPARATOR || ch == WINDOWS_SEPARATOR;
        }

    /**
     * Converts all separators to the Unix separator of forward slash.
     *
     * @param path the path to be changed, null ignored
     *
     * @return the updated path
     */
    public static String separatorsToUnix(final String path)
        {
        if (path == null || path.indexOf(WINDOWS_SEPARATOR) == NOT_FOUND)
            {
            return path;
            }
        return path.replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR);
        }

    /**
     * Returns the length of the filename prefix, such as <code>C:/</code> or
     * <code>~/</code>.
     * <p>
     * This method will handle a file in either Unix or Windows format.
     * <p>
     * The prefix length includes the first slash in the full filename if
     * applicable. Thus, it is possible that the length returned is greater than
     * the length of the input string.
     * <pre>
     * Windows:
     * a\b\c.txt           --&gt; ""          --&gt; relative
     * \a\b\c.txt          --&gt; "\"         --&gt; current drive absolute
     * C:a\b\c.txt         --&gt; "C:"        --&gt; drive relative
     * C:\a\b\c.txt        --&gt; "C:\"       --&gt; absolute
     * \\server\a\b\c.txt  --&gt; "\\server\" --&gt; UNC
     * \\\a\b\c.txt        --&gt;  error, length = -1
     *
     * Unix:
     * a/b/c.txt           --&gt; ""          --&gt; relative
     * /a/b/c.txt          --&gt; "/"         --&gt; absolute
     * ~/a/b/c.txt         --&gt; "~/"        --&gt; current user
     * ~                   --&gt; "~/"        --&gt; current user (slash added)
     * ~user/a/b/c.txt     --&gt; "~user/"    --&gt; named user
     * ~user               --&gt; "~user/"    --&gt; named user (slash added)
     * //server/a/b/c.txt  --&gt; "//server/"
     * ///a/b/c.txt        --&gt; error, length = -1
     * </pre>
     * <p>
     * The output will be the same irrespective of the machine that the code is
     * running on. ie. both Unix and Windows prefixes are matched regardless.
     * <p>
     * Note that a leading // (or \\) is used to indicate a UNC name on Windows.
     * These must be followed by a server name, so double-slashes are not
     * collapsed to a single slash at the start of the filename.
     *
     * @param filename the filename to find the prefix in, null returns -1
     *
     * @return the length of the prefix, -1 if invalid or null
     */
    public static int getPrefixLength(final String filename)
        {
        if (filename == null)
            {
            return NOT_FOUND;
            }
        final int len = filename.length();
        if (len == 0)
            {
            return 0;
            }
        char ch0 = filename.charAt(0);
        if (ch0 == ':')
            {
            return NOT_FOUND;
            }
        if (len == 1)
            {
            if (ch0 == '~')
                {
                return 2;  // return a length greater than the input
                }
            return isSeparator(ch0) ? 1 : 0;
            }
        else
            {
            if (ch0 == '~')
                {
                int posUnix = filename.indexOf(UNIX_SEPARATOR, 1);
                int posWin = filename.indexOf(WINDOWS_SEPARATOR, 1);
                if (posUnix == NOT_FOUND && posWin == NOT_FOUND)
                    {
                    return len + 1;  // return a length greater than the input
                    }
                posUnix = posUnix == NOT_FOUND ? posWin : posUnix;
                posWin = posWin == NOT_FOUND ? posUnix : posWin;
                return Math.min(posUnix, posWin) + 1;
                }
            final char ch1 = filename.charAt(1);
            if (ch1 == ':')
                {
                ch0 = Character.toUpperCase(ch0);
                if (ch0 >= 'A' && ch0 <= 'Z')
                    {
                    if (len == 2 || isSeparator(filename.charAt(2)) == false)
                        {
                        return 2;
                        }
                    return 3;
                    }
                return NOT_FOUND;

                }
            else if (isSeparator(ch0) && isSeparator(ch1))
                {
                int posUnix = filename.indexOf(UNIX_SEPARATOR, 2);
                int posWin = filename.indexOf(WINDOWS_SEPARATOR, 2);
                if (posUnix == NOT_FOUND && posWin == NOT_FOUND || posUnix == 2 || posWin == 2)
                    {
                    return NOT_FOUND;
                    }
                posUnix = posUnix == NOT_FOUND ? posWin : posUnix;
                posWin = posWin == NOT_FOUND ? posUnix : posWin;
                return Math.min(posUnix, posWin) + 1;
                }
            else
                {
                return isSeparator(ch0) ? 1 : 0;
                }
            }
        }

    /**
     * Returns the index of the last directory separator character.
     * <p>
     * This method will handle a file in either Unix or Windows format. The
     * position of the last forward or backslash is returned.
     * <p>
     * The output will be the same irrespective of the machine that the code is
     * running on.
     *
     * @param filename the filename to find the last path separator in, null
     *                 returns -1
     *
     * @return the index of the last separator character, or -1 if there is no
     * such character
     */
    public static int indexOfLastSeparator(final String filename)
        {
        if (filename == null)
            {
            return NOT_FOUND;
            }
        final int lastUnixPos    = filename.lastIndexOf(UNIX_SEPARATOR);
        final int lastWindowsPos = filename.lastIndexOf(WINDOWS_SEPARATOR);
        return Math.max(lastUnixPos, lastWindowsPos);
        }

    /**
     * Returns the index of the last extension separator character, which is a
     * dot. <p> This method also checks that there is no directory separator
     * after the last dot. To do this it uses {@link #indexOfLastSeparator(String)}
     * which will handle a file in either Unix or Windows format. </p> <p> The
     * output will be the same irrespective of the machine that the code is
     * running on. </p>
     *
     * @param filename the filename to find the last extension separator in,
     *                 null returns -1
     *
     * @return the index of the last extension separator character, or -1 if
     * there is no such character
     */
    public static int indexOfExtension(final String filename)
        {
        if (filename == null)
            {
            return NOT_FOUND;
            }
        final int extensionPos = filename.lastIndexOf(EXTENSION_SEPARATOR);
        final int lastSeparator = indexOfLastSeparator(filename);
        return lastSeparator > extensionPos ? NOT_FOUND : extensionPos;
        }

    /**
     * Gets the path from a full filename, which excludes the prefix, and also
     * excluding the final directory separator.
     * <p>
     * This method will handle a file in either Unix or Windows format. The
     * method is entirely text based, and returns the text before the last
     * forward or backslash.
     * <pre>
     * C:\a\b\c.txt --&gt; a\b
     * ~/a/b/c.txt  --&gt; a/b
     * a.txt        --&gt; ""
     * a/b/c        --&gt; a/b
     * a/b/c/       --&gt; a/b/c
     * </pre>
     * <p>
     * The output will be the same irrespective of the machine that the code is
     * running on.
     *
     * @param filename the filename to query, null returns null
     *
     * @return the path of the file, an empty string if none exists, null if
     * invalid. Null bytes inside string will be removed
     */
    public static String getPathNoEndSeparator(final String filename)
        {
        return doGetPath(filename, 0);
        }

    /**
     * Does the work of getting the path.
     *
     * @param filename     the filename
     * @param separatorAdd 0 to omit the end separator, 1 to return it
     *
     * @return the path. Null bytes inside string will be removed
     */
    static String doGetPath(final String filename, final int separatorAdd)
        {
        if (filename == null)
            {
            return null;
            }
        final int prefix = getPrefixLength(filename);
        if (prefix < 0)
            {
            return null;
            }
        final int index = indexOfLastSeparator(filename);
        final int endIndex = index + separatorAdd;
        if (prefix >= filename.length() || index < 0 || prefix >= endIndex)
            {
            return "";
            }
        return filterNullBytes(filename.substring(prefix, endIndex));
        }

    /**
     * Gets the name minus the path from a full filename.
     * <p>
     * This method will handle a file in either Unix or Windows format. The text
     * after the last forward or backslash is returned.
     * <pre>
     * a/b/c.txt --&gt; c.txt
     * a.txt     --&gt; a.txt
     * a/b/c     --&gt; c
     * a/b/c/    --&gt; ""
     * </pre>
     * <p>
     * The output will be the same irrespective of the machine that the code is
     * running on.
     *
     * @param filename the filename to query, null returns null
     *
     * @return the name of the file without the path, or an empty string if none
     * exists. Null bytes inside string will be removed
     */
    public static String getName(final String filename)
        {
        if (filename == null)
            {
            return null;
            }
        String cleanFileName = filterNullBytes(filename);
        final int index = indexOfLastSeparator(cleanFileName);
        return cleanFileName.substring(index + 1);
        }

    /**
     * Filters the supplied path for null byte characters. Can be used for
     * normalizations to avoid poison byte attacks.
     * <p>
     * This mimicks behaviour of 1.7u40+. Once minimum java requirement is above
     * this version, this code can be removed.
     *
     * @param path the path
     *
     * @return the supplied string without any embedded null characters
     */
    static String filterNullBytes(String path)
        {
        return path.contains("\u0000") ? path.replace("\u0000", "") : path;
        }

    /**
     * Gets the base name, minus the full path and extension, from a full
     * filename.
     * <p>
     * This method will handle a file in either Unix or Windows format. The text
     * after the last forward or backslash and before the last dot is returned.
     * <pre>
     * a/b/c.txt --&gt; c
     * a.txt     --&gt; a
     * a/b/c     --&gt; c
     * a/b/c/    --&gt; ""
     * </pre>
     * <p>
     * The output will be the same irrespective of the machine that the code is
     * running on.
     *
     * @param filename the filename to query, null returns null
     *
     * @return the name of the file without the path, or an empty string if none
     * exists. Null bytes inside string will be removed
     */
    public static String getBaseName(final String filename)
        {
        return removeExtension(getName(filename));
        }

    /**
     * Gets the extension of a filename.
     * <p>
     * This method returns the textual part of the filename after the last dot.
     * There must be no directory separator after the dot.
     * <pre>
     * foo.txt      --&gt; "txt"
     * a/b/c.jpg    --&gt; "jpg"
     * a/b.txt/c    --&gt; ""
     * a/b/c        --&gt; ""
     * </pre>
     * <p>
     * The output will be the same irrespective of the machine that the code is
     * running on.
     *
     * @param filename the filename to retrieve the extension of.
     *
     * @return the extension of the file or an empty string if none exists or
     * {@code null} if the filename is {@code null}.
     */
    public static String getExtension(final String filename)
        {
        if (filename == null)
            {
            return null;
            }
        final int index = indexOfExtension(filename);
        if (index == NOT_FOUND)
            {
            return "";
            }
        else
            {
            return filename.substring(index + 1);
            }
        }

    /**
     * Removes the extension from a filename.
     * <p>
     * This method returns the textual part of the filename before the last dot.
     * There must be no directory separator after the dot.
     * <pre>
     * foo.txt    --&gt; foo
     * a\b\c.jpg  --&gt; a\b\c
     * a\b\c      --&gt; a\b\c
     * a.b\c      --&gt; a.b\c
     * </pre>
     * <p>
     * The output will be the same irrespective of the machine that the code is
     * running on.
     *
     * @param filename the filename to query, null returns null
     *
     * @return the filename minus the extension
     */
    public static String removeExtension(final String filename)
        {
        if (filename == null)
            {
            return null;
            }
        String cleanFileName = filterNullBytes(filename);

        final int index = indexOfExtension(cleanFileName);
        if (index == NOT_FOUND)
            {
            return cleanFileName;
            }
        else
            {
            return cleanFileName.substring(0, index);
            }
        }

    /**
     * Return true if the specified directory is empty.
     *
     * @param  fileDir the directory to check
     *
     * @return true if the directory is empty
     *
     * @since 24.09
     */
    public static boolean isEmpty(File fileDir)
        {
        File[] afile = fileDir.listFiles();

        return fileDir.isDirectory() && (afile == null || afile.length == 0);
        }

    // ----- static helper classes ------------------------------------------

    /**
     * Extension of <tt>SimpleFileVisitor</tt> used to copy a directory and
     * it's contents.
     */
    private static class CopyDirVisitor
            extends SimpleFileVisitor<Path>
        {

        // ----- constructors -----------------------------------------------

        /**
         * Create a new CopyDirVisitor that will copy the given source
         * directory to the specified destination.
         *
         * @param pathFrom  the source directory
         * @param pathTo    the destination directory
         */
        public CopyDirVisitor(Path pathFrom, Path pathTo)
            {
            this(pathFrom, pathTo, StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS);
            }

        /**
         * Create a new CopyDirVisitor that will copy the given source
         * directory to the specified destination with the given options.
         *
         * @param pathFrom  the source directory
         * @param pathTo    the destination directory
         * @param opts      the copy options
         */
        public CopyDirVisitor(Path pathFrom, Path pathTo, CopyOption... opts)
            {
            f_pathFrom = pathFrom;
            f_pathTo   = pathTo;
            f_opts     = opts;
            }

        // ----- SimpleFileVisitor overrides --------------------------------

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                throws IOException
            {
            Path pathTo = f_pathTo.resolve(f_pathFrom.relativize(dir));
            if (!Files.exists(pathTo))
                {
                Files.createDirectory(pathTo);
                }
            return FileVisitResult.CONTINUE;
            }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException
            {
            Files.copy(file, f_pathTo.resolve(f_pathFrom.relativize(file)), f_opts);
            return FileVisitResult.CONTINUE;
            }

        // ----- data members -----------------------------------------------

        private final Path         f_pathFrom;
        private final Path         f_pathTo;
        private final CopyOption[] f_opts;
        }

    /**
     * Extension of <tt>SimpleFileVisitor</tt> used to delete a directory and
     * it's contents.
     */
    private static class DeleteDirVisitor
            extends SimpleFileVisitor<Path>
        {

        // ----- SimpleFileVisitor overrides --------------------------------

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException
            {
            super.visitFile(file, attrs);
            Files.delete(file);
            return FileVisitResult.CONTINUE;
            }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException e)
                throws IOException
            {
            super.postVisitDirectory(dir, e);
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
            }
        }

    /**
     * Extension of <tt>SimpleFileVisitor</tt> used to size a directory and
     * it's contents.
     */
    private static class SizeDirVisitor
            extends SimpleFileVisitor<Path>
        {

        // ----- SimpleFileVisitor overrides --------------------------------

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException
            {
            m_cb += Files.size(file);
            return FileVisitResult.CONTINUE;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return the calculated size of the directory and it's contents.
         *
         * @return the size (in bytes) of the directory and it's contents
         */
        public long getSize()
            {
            return m_cb;
            }

        // ----- data members -----------------------------------------------

        private long m_cb;
        }

    // ---- constants -------------------------------------------------------

    /**
     * The extension separator character.
     */
    private static final char EXTENSION_SEPARATOR = '.';

    /**
     * A value returned when a character ot string is not found.
     */
    private static final int NOT_FOUND = -1;

    /**
     * The Unix separator character.
     */
    private static final char UNIX_SEPARATOR = '/';

    /**
     * The Windows separator character.
     */
    private static final char WINDOWS_SEPARATOR = '\\';
    }
