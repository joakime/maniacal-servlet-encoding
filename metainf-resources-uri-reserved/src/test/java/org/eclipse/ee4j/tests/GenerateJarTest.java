package org.eclipse.ee4j.tests;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class GenerateJarTest
{
    @Test
    public void testGenerateJar() throws IOException
    {
        Path outputJar = Paths.get("target/uriReservedChars.jar");
        Files.deleteIfExists(outputJar);
        PathUtils.ensureDirExists(outputJar.getParent());
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");

        URI uri = URI.create("jar:" + outputJar.toUri().toASCIIString());
        try (FileSystem zipfs = FileSystems.newFileSystem(uri, env);
             OutputStream pathOut = Files.newOutputStream(Paths.get("target/testCases.txt"));
             PrintStream javaout = new PrintStream(pathOut))
        {
            Path base = zipfs.getPath("/META-INF/resources/uri-reserved");
            PathUtils.ensureDirExists(base);
            generateFiles(base, javaout);
            javaout.flush();
            generateFiles(base, "semi;colon");
            generateFiles(base, "question?mark");
            generateFiles(base, "hash#mark");
            generateFiles(base, "%2e%2e");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static void generateFiles(Path base, String rawDirName) throws IOException
    {
        generateFiles(PathUtils.ensureDirExists(base.resolve(rawDirName)));
        String encodedDirName = URLEncoder.encode(rawDirName, StandardCharsets.UTF_8);
        if (!rawDirName.equals(encodedDirName))
        {
            generateFiles(PathUtils.ensureDirExists(base.resolve(encodedDirName)));
        }
    }

    private static void generateFiles(Path outputDir)
    {
        generateFiles(outputDir, (PrintStream)null);
    }

    private static void generateFiles(Path outputDir, PrintStream testcases)
    {
        // Add simple file to directory
        touchFile(outputDir.resolve("exists.txt"), "dir exists: " + outputDir.getFileName().toString());

        // URI Reserved = gen-delims  = ":" / "/" / "?" / "#" / "[" / "]" / "@"
        touchFiles(testcases, outputDir, "this_is_100%_valid.txt", "reserved-percent-100");
        touchFiles(testcases, outputDir, "vote-results-50%-of-precincts.txt", "reserved-percent-50");
        touchFiles(testcases, outputDir, "%_played.txt", "reserved-percent-played");
        touchFiles(testcases, outputDir, "there_are_two_choices:foo_or_bar.txt", "reserved-colon-choices");
        touchFiles(testcases, outputDir, "m:main_or_master.txt", "reserved-colon-m");
        touchFiles(testcases, outputDir, ":colon.txt", "reserved-colon-start");
        touchFiles(testcases, outputDir, "are_we_not_robots?.txt", "reserved-question-mark-end");
        touchFiles(testcases, outputDir, "?question.txt", "reserved-question-mark-start");
        touchFiles(testcases, outputDir, "deck_#4_of_15.txt", "reserved-hash-deck4");
        touchFiles(testcases, outputDir, "#hashcode.txt", "reserved-hash-start");
        touchFiles(testcases, outputDir, "[brackets].txt", "reserved-brackets-start");
        touchFiles(testcases, outputDir, "byte[0].txt", "reserved-brackets-byte-array-empty");

        // URI Reserved = sub-delims  = "!" / "$" / "&" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "="
        touchFiles(testcases, outputDir, "wat!.txt", "reserved-exclamation-wat");
        touchFiles(testcases, outputDir, "!important.txt", "reserved-exclamation-important");
        touchFiles(testcases, outputDir, "us$.txt", "reserved-dollar-us");
        touchFiles(testcases, outputDir, "$for_null.txt", "reserved-dollar-start");
        touchFiles(testcases, outputDir, "this&that.txt", "reserved-ampersand-this-that");
        touchFiles(testcases, outputDir, "&more.txt", "reserved-ampersand-more");
        touchFiles(testcases, outputDir, "quoth_the_raven_'nevermore'.txt", "reserved-apostrophe-raven");
        touchFiles(testcases, outputDir, "'apostrophe.txt", "reserved-apostrophe-start");
        touchFiles(testcases, outputDir, "method(params).txt", "reserved-parens-params");
        touchFiles(testcases, outputDir, "(parens).txt", "reserved-parens-start");
        touchFiles(testcases, outputDir, "asterisk-*.txt", "reserved-asterisk-end");
        touchFiles(testcases, outputDir, "*-global.txt", "reserved-asterisk-start");
        touchFiles(testcases, outputDir, "that+those.txt", "reserved-plus-that-those");
        touchFiles(testcases, outputDir, "+plus.txt", "reserved-plus-start");
        touchFiles(testcases, outputDir, "some,more,commands.txt", "reserved-comma-some-more");
        touchFiles(testcases, outputDir, ",delim.txt", "reserved-comma-start");
        touchFiles(testcases, outputDir, "wait;what.txt", "reserved-semi-wait-what");
        touchFiles(testcases, outputDir, ";how.txt", "reserved-semi-start");
        touchFiles(testcases, outputDir, "foo=bar.txt", "reserved-equals-foo-bar");
        touchFiles(testcases, outputDir, "=zed.txt", "reserved-equals-foo-start");

        // complex filenames - combinations of reserved characters
        touchFiles(testcases, outputDir, "&lt;xml&gt;.txt", "reserved-ampersand-xml-teeth");
        touchFiles(testcases, outputDir, "copyright&#00a9;2021.txt", "reserved-ampersand-entity-copyright-symbol");
    }

    private static void touchFiles(PrintStream testcases, Path outputDir, String filename, String contents)
    {
        touchFile(outputDir.resolve(filename), contents + "-raw");
        String encodedFilename = PathUtils.encodePath(filename);
        touchFile(outputDir.resolve(encodedFilename), contents + "-encoded");
        if (testcases != null)
        {
            testcases.printf("cases.add(Arguments.of(dirPrefix + \"%s\", \"%s-raw\"));%n", filename, contents);
            testcases.printf("cases.add(Arguments.of(dirPrefix + \"%s\", \"%s-raw\"));%n", encodedFilename, contents);
            testcases.printf("cases.add(Arguments.of(dirPrefix + \"%s\", \"%s-encoded\"));%n", encodedFilename.replaceAll("%", "%25"), contents);
        }
    }

    private static void touchFile(Path file, String contents)
    {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8))
        {
            writer.write(contents);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
