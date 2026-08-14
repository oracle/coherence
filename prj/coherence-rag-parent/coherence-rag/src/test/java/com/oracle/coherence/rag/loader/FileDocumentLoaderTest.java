/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.loader;

import com.oracle.coherence.rag.api.RagSecurity;
import com.oracle.coherence.rag.api.RagSecurityTestSupport;
import com.oracle.coherence.rag.parser.ParserSupplier;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.Metadata;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;

import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FileDocumentLoader} class.
 *
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FileDocumentLoader")
class FileDocumentLoaderTest
    {
    @Mock
    private DocumentParser mockDocumentParser;

    @Mock
    private ParserSupplier mockParserSupplier;

    @TempDir
    private Path tempDir;

    private Path pathAllowed;
    private Path pathOutside;
    private FileDocumentLoader loader;

    @BeforeEach
    void setUp()
            throws IOException
        {
        pathAllowed = Files.createDirectory(tempDir.resolve("allowed"));
        pathOutside = Files.createDirectory(tempDir.resolve("outside"));
        loader      = new FileDocumentLoader(mockParserSupplier);

        System.setProperty(RagSecurity.PROP_IMPORT_ALLOWED_SCHEMES, "file");
        System.setProperty(RagSecurity.PROP_IMPORT_FILE_ALLOWED_ROOTS, pathAllowed.toString());
        RagSecurityTestSupport.setFileOpenHook(null);
        RagSecurityTestSupport.setDirectoryStreamFactory(null);
        }

    @AfterEach
    void cleanup()
        {
        System.clearProperty(RagSecurity.PROP_IMPORT_ALLOWED_SCHEMES);
        System.clearProperty(RagSecurity.PROP_IMPORT_FILE_ALLOWED_ROOTS);
        RagSecurityTestSupport.setFileOpenHook(null);
        RagSecurityTestSupport.setDirectoryStreamFactory(null);
        }

    @Test
    @DisplayName("should parse content from securely opened file")
    void shouldParseContentFromSecurelyOpenedFile()
            throws Exception
        {
        assumeSecureDirectoryStreamsSupported();
        Path pathFile = Files.writeString(pathAllowed.resolve("document.txt"), "allowed content");
        AtomicReference<String> content = new AtomicReference<>();
        Document document = Document.from("parsed");
        when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
        when(mockDocumentParser.parse(any(InputStream.class))).thenAnswer(invocation ->
            {
            InputStream in = invocation.getArgument(0);
            content.set(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            return document;
            });

        Document result = loader.load(pathFile.toUri());

        assertThat(result, is(sameInstance(document)));
        assertThat(content.get(), is("allowed content"));
        }

    @Test
    @DisplayName("should parse an empty regular file")
    void shouldParseEmptyRegularFile()
            throws IOException
        {
        assumeSecureDirectoryStreamsSupported();
        Path pathFile = Files.createFile(pathAllowed.resolve("empty.txt"));
        Document document = Document.from("parsed");
        when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
        when(mockDocumentParser.parse(any(InputStream.class))).thenReturn(document);

        Document result = loader.load(pathFile.toUri());

        assertThat(result, is(sameInstance(document)));
        }

    @Test
    @DisplayName("should reject a stable special file")
    void shouldRejectStableSpecialFile()
            throws IOException
        {
        Path pathSpecial = Path.of("/dev/null");
        Assumptions.assumeTrue(Files.exists(pathSpecial), "/dev/null is not available");
        assumeSecureDirectoryStreamsSupported();
        System.setProperty(RagSecurity.PROP_IMPORT_FILE_ALLOWED_ROOTS, pathSpecial.getParent().toString());

        RagSecurity.PolicyViolation error = assertThrows(RagSecurity.PolicyViolation.class,
                () -> loader.load(pathSpecial.toUri()));

        assertThat(error.reason(), is(RagSecurity.REASON_PATH_NOT_ALLOWED));
        }

    @Test
    @DisplayName("should reject a FIFO replacement without hanging")
    void shouldRejectFifoReplacementWithoutHanging()
            throws Exception
        {
        assumeSecureDirectoryStreamsSupported();
        Path pathFile = Files.writeString(pathAllowed.resolve("document.txt"), "allowed");
        String sJava = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String sClassPath = System.getProperty("surefire.test.class.path",
                System.getProperty("java.class.path"));
        Process process = new ProcessBuilder(sJava, "-cp", sClassPath,
                FifoReplacementProbe.class.getName(), pathAllowed.toString(), pathFile.toString())
                .redirectErrorStream(true)
                .start();

        boolean fExited = process.waitFor(10, TimeUnit.SECONDS);
        if (!fExited)
            {
            process.destroyForcibly();
            process.waitFor();
            }
        String sOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(fExited, "FIFO open blocked until the probe was killed");
        Assumptions.assumeTrue(process.exitValue() != 77, "mkfifo is not available");
        assertThat(sOutput, process.exitValue(), is(0));
        }

    @Test
    @DisplayName("should reject file outside allowed root")
    void shouldRejectFileOutsideAllowedRoot()
            throws IOException
        {
        Path pathFile = Files.writeString(pathOutside.resolve("secret.txt"), "secret");

        RagSecurity.PolicyViolation error = assertThrows(RagSecurity.PolicyViolation.class,
                () -> loader.load(pathFile.toUri()));

        assertThat(error.reason(), is(RagSecurity.REASON_PATH_NOT_ALLOWED));
        }

    @Test
    @DisplayName("should reject final symlink swapped after validation")
    void shouldRejectFinalSymlinkSwappedAfterValidation()
            throws IOException
        {
        assumeSecureDirectoryStreamsSupported();
        assumeSymlinksSupported();
        Path pathFile   = Files.writeString(pathAllowed.resolve("document.txt"), "allowed");
        Path pathSecret = Files.writeString(pathOutside.resolve("secret.txt"), "secret");
        RagSecurityTestSupport.setFileOpenHook(path ->
            {
            Files.delete(path);
            Files.createSymbolicLink(path, pathSecret);
            });

        RagSecurity.PolicyViolation error = assertThrows(RagSecurity.PolicyViolation.class,
                () -> loader.load(pathFile.toUri()));

        assertThat(error.reason(), is(RagSecurity.REASON_PATH_NOT_ALLOWED));
        }

    @Test
    @DisplayName("should reject parent symlink swapped after validation")
    void shouldRejectParentSymlinkSwappedAfterValidation()
            throws IOException
        {
        assumeSecureDirectoryStreamsSupported();
        assumeSymlinksSupported();
        Path pathParent = Files.createDirectory(pathAllowed.resolve("current"));
        Path pathFile   = Files.writeString(pathParent.resolve("document.txt"), "allowed");
        Files.writeString(pathOutside.resolve("document.txt"), "secret");
        RagSecurityTestSupport.setFileOpenHook(path ->
            {
            Files.move(pathParent, pathAllowed.resolve("original"));
            Files.createSymbolicLink(pathParent, pathOutside);
            });

        RagSecurity.PolicyViolation error = assertThrows(RagSecurity.PolicyViolation.class,
                () -> loader.load(pathFile.toUri()));

        assertThat(error.reason(), is(RagSecurity.REASON_PATH_NOT_ALLOWED));
        }

    @Test
    @DisplayName("should use canonical target for existing symlink under allowed root")
    void shouldUseCanonicalTargetForExistingSymlinkUnderAllowedRoot()
            throws Exception
        {
        assumeSecureDirectoryStreamsSupported();
        assumeSymlinksSupported();
        Path pathDirectory = Files.createDirectory(pathAllowed.resolve("documents"));
        Path pathFile      = Files.writeString(pathDirectory.resolve("document.txt"), "allowed content");
        Path pathLink      = pathAllowed.resolve("current");
        Files.createSymbolicLink(pathLink, pathDirectory.getFileName());
        AtomicReference<String> content = new AtomicReference<>();
        Document document = Document.from("parsed");
        when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
        when(mockDocumentParser.parse(any(InputStream.class))).thenAnswer(invocation ->
            {
            InputStream in = invocation.getArgument(0);
            content.set(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            return document;
            });

        Document result = loader.load(pathLink.resolve(pathFile.getFileName()).toUri());

        assertThat(result, is(sameInstance(document)));
        assertThat(content.get(), is("allowed content"));
        }

    @Test
    @DisplayName("should fail closed without secure directory stream")
    void shouldFailClosedWithoutSecureDirectoryStream()
            throws IOException
        {
        Path pathFile = Files.writeString(pathAllowed.resolve("document.txt"), "allowed");
        RagSecurityTestSupport.setDirectoryStreamFactory(path ->
            {
            DirectoryStream<Path> delegate = Files.newDirectoryStream(path);
            return new DirectoryStream<>()
                {
                @Override
                public Iterator<Path> iterator()
                    {
                    return delegate.iterator();
                    }

                @Override
                public void close()
                        throws IOException
                    {
                    delegate.close();
                    }
                };
            });

        RagSecurity.PolicyViolation error = assertThrows(RagSecurity.PolicyViolation.class,
                () -> loader.load(pathFile.toUri()));

        assertThat(error.reason(), is(RagSecurity.REASON_PATH_NOT_ALLOWED));
        }

    @Test
    @DisplayName("should fail closed when the default filesystem lacks secure directory streams")
    void shouldFailClosedWhenDefaultFileSystemLacksSecureDirectoryStreams()
            throws IOException
        {
        Assumptions.assumeFalse(supportsSecureDirectoryStreams(),
                "the default filesystem supports secure directory streams");
        Path pathFile = Files.writeString(pathAllowed.resolve("document.txt"), "allowed");

        RagSecurity.PolicyViolation error = assertThrows(RagSecurity.PolicyViolation.class,
                () -> loader.load(pathFile.toUri()));

        assertThat(error.reason(), is(RagSecurity.REASON_PATH_NOT_ALLOWED));
        }

    @Test
    @DisplayName("should retain parser exception wrapping")
    void shouldRetainParserExceptionWrapping()
            throws IOException
        {
        assumeSecureDirectoryStreamsSupported();
        Path pathFile = Files.writeString(pathAllowed.resolve("document.txt"), "allowed");
        RuntimeException expected = new RuntimeException("Failed to parse document");
        when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
        when(mockDocumentParser.parse(any(InputStream.class))).thenThrow(expected);

        RuntimeException error = assertThrows(RuntimeException.class, () -> loader.load(pathFile.toUri()));

        assertThat(error.getMessage(), is("Failed to load document"));
        assertThat(error.getCause(), is(sameInstance(expected)));
        }

    @Test
    @DisplayName("should wrap null document from parser")
    void shouldWrapNullDocumentFromParser()
            throws IOException
        {
        assumeSecureDirectoryStreamsSupported();
        Path pathFile = Files.writeString(pathAllowed.resolve("document.txt"), "allowed");
        when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
        when(mockDocumentParser.parse(any(InputStream.class))).thenReturn(null);

        RuntimeException error = assertThrows(RuntimeException.class, () -> loader.load(pathFile.toUri()));

        assertThat(error.getMessage(), is("Failed to load document"));
        assertThat(error.getCause(), is(instanceOf(NullPointerException.class)));
        }

    @Test
    @DisplayName("should preserve parser and file metadata")
    void shouldPreserveParserAndFileMetadata()
            throws IOException
        {
        assumeSecureDirectoryStreamsSupported();
        Path pathFile = Files.writeString(pathAllowed.resolve("document.txt"), "allowed");
        Metadata metadata = Metadata.from("author", "John Doe");
        Document document = Document.from("Document content", metadata);
        when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
        when(mockDocumentParser.parse(any(InputStream.class))).thenReturn(document);

        Document result = loader.load(pathFile.toUri());

        assertThat(result, is(sameInstance(document)));
        assertThat(result.metadata().getString("author"), is("John Doe"));
        assertThat(result.metadata().getString("file_name"), is(pathFile.getFileName().toString()));
        assertThat(result.metadata().getString("absolute_directory_path"), is(pathFile.getParent().toString()));
        }

    private void assumeSecureDirectoryStreamsSupported()
            throws IOException
        {
        Assumptions.assumeTrue(supportsSecureDirectoryStreams(),
                "the default filesystem does not support secure directory streams");
        }

    private boolean supportsSecureDirectoryStreams()
            throws IOException
        {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pathAllowed))
            {
            return stream instanceof SecureDirectoryStream<?>;
            }
        }

    private void assumeSymlinksSupported()
            throws IOException
        {
        Path pathTarget = Files.writeString(tempDir.resolve("symlink-target"), "target");
        Path pathLink   = tempDir.resolve("symlink-probe");
        try
            {
            Files.createSymbolicLink(pathLink, pathTarget);
            }
        catch (UnsupportedOperationException | SecurityException | IOException e)
            {
            Assumptions.assumeTrue(false, "symbolic links are not supported: " + e.getClass().getSimpleName());
            }
        finally
            {
            Files.deleteIfExists(pathLink);
            Files.deleteIfExists(pathTarget);
            }
        }

    /**
     * Isolated process used to guarantee that a FIFO-open regression cannot
     * hang the owning test JVM.
     */
    public static class FifoReplacementProbe
        {
        public static void main(String[] args)
                throws Exception
            {
            Path pathAllowed = Path.of(args[0]);
            Path pathFile    = Path.of(args[1]);
            System.setProperty(RagSecurity.PROP_IMPORT_ALLOWED_SCHEMES, "file");
            System.setProperty(RagSecurity.PROP_IMPORT_FILE_ALLOWED_ROOTS, pathAllowed.toString());
            RagSecurityTestSupport.setFileOpenHook(path ->
                {
                Files.delete(path);
                Process process;
                try
                    {
                    process = new ProcessBuilder("mkfifo", path.toString()).start();
                    }
                catch (IOException e)
                    {
                    System.exit(77);
                    return;
                    }
                int nExit;
                try
                    {
                    nExit = process.waitFor();
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while creating FIFO", e);
                    }
                if (nExit != 0)
                    {
                    System.exit(77);
                    }
                });

            try (InputStream ignored = RagSecurity.openValidatedFile(pathFile.toUri()))
                {
                throw new AssertionError("FIFO was opened");
                }
            catch (RagSecurity.PolicyViolation e)
                {
                if (!RagSecurity.REASON_PATH_NOT_ALLOWED.equals(e.reason()))
                    {
                    throw e;
                    }
                }
            }
        }
    }
