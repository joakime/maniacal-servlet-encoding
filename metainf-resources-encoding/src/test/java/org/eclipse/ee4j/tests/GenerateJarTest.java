package org.eclipse.ee4j.tests;

import java.io.BufferedWriter;
import java.io.IOException;
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
        Path outputJar = Paths.get("target/metainf-resources-encoding.jar");
        Files.deleteIfExists(outputJar);
        PathUtils.ensureDirExists(outputJar.getParent());
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");

        URI uri = URI.create("jar:" + outputJar.toUri().toASCIIString());
        try (FileSystem zipfs = FileSystems.newFileSystem(uri, env);)
        {
            Path base = zipfs.getPath("/META-INF/resources/encoding");
            PathUtils.ensureDirExists(base);
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
        // Add simple file to directory
        touchFile(outputDir.resolve("exists.txt"), "dir exists: " + outputDir.getFileName().toString());

        // URI Reserved = gen-delims  = ":" / "/" / "?" / "#" / "[" / "]" / "@"
        touchFiles(outputDir, "this_is_100%_valid.txt", "reserved-percent-100");
        touchFiles(outputDir, "vote-results-50%-of-precincts.txt", "reserved-percent-50");
        touchFiles(outputDir, "%_played.txt", "reserved-percent-played");
        touchFiles(outputDir, "there_are_two_choices:foo_or_bar.txt", "reserved-colon-choices");
        touchFiles(outputDir, "m:main_or_master.txt", "reserved-colon-m");
        touchFiles(outputDir, ":colon.txt", "reserved-colon-start");
        touchFiles(outputDir, "are_we_not_robots?.txt", "reserved-question-mark-end");
        touchFiles(outputDir, "?question.txt", "reserved-question-mark-start");
        touchFiles(outputDir, "deck_#4_of_15.txt", "reserved-hash-deck4");
        touchFiles(outputDir, "#hashcode.txt", "reserved-hash-start");
        touchFiles(outputDir, "[brackets].txt", "reserved-brackets-start");
        touchFiles(outputDir, "byte[0].txt", "reserved-brackets-byte-array-empty");

        // URI Reserved = sub-delims  = "!" / "$" / "&" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "="
        touchFiles(outputDir, "wat!.txt", "reserved-exclamation-wat");
        touchFiles(outputDir, "!important.txt", "reserved-exclamation-important");
        touchFiles(outputDir, "us$.txt", "reserved-dollar-us");
        touchFiles(outputDir, "$for_null.txt", "reserved-dollar-start");
        touchFiles(outputDir, "this&that.txt", "reserved-ampersand-this-that");
        touchFiles(outputDir, "&more.txt", "reserved-ampersand-more");
        touchFiles(outputDir, "quoth_the_raven_'nevermore'.txt", "reserved-apostrophe-raven");
        touchFiles(outputDir, "'apostrophe.txt", "reserved-apostrophe-start");
        touchFiles(outputDir, "method(params).txt", "reserved-parens-params");
        touchFiles(outputDir, "(parens).txt", "reserved-parens-start");
        touchFiles(outputDir, "asterisk-*.txt", "reserved-asterisk-end");
        touchFiles(outputDir, "*-global.txt", "reserved-asterisk-start");
        touchFiles(outputDir, "that+those.txt", "reserved-plus-that-those");
        touchFiles(outputDir, "+plus.txt", "reserved-plus-start");
        touchFiles(outputDir, "some,more,commands.txt", "reserved-comma-some-more");
        touchFiles(outputDir, ",delim.txt", "reserved-comma-start");
        touchFiles(outputDir, "wait;what.txt", "reserved-semi-wait-what");
        touchFiles(outputDir, ";how.txt", "reserved-semi-start");
        touchFiles(outputDir, "foo=bar.txt", "reserved-equals-foo-bar");
        touchFiles(outputDir, "=zed.txt", "reserved-equals-foo-start");

        // complex filenames - combinations of reserved characters
        touchFiles(outputDir, "&lt;xml&gt;.txt", "reserved-ampersand-xml-teeth");
        touchFiles(outputDir, "copyright&#00a9;2021.txt", "reserved-ampersand-entity-copyright-symbol");
    }

    private static void touchFiles(Path outputDir, String filename, String contents)
    {
        touchFile(outputDir.resolve(filename), contents + "-raw");
        String encodedFilename = PathUtils.encodePath(filename);
        touchFile(outputDir.resolve(encodedFilename), contents + "-encoded");
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
