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
import java.util.function.Function;

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
        try (FileSystem zipfs = FileSystems.newFileSystem(uri, env))
        {
            Path base = zipfs.getPath("/META-INF/resources/encoding");
            PathUtils.ensureDirExists(base);
            touchFile(base.resolve("exists.txt"), "Base exists");
            generateFilesInDir(base, "semi;colon");
            generateFilesInDir(base, "question?mark");
            generateFilesInDir(base, "hash#mark");
            generateFilesInDir(base, "%2e%2e");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static void generateFilesInDir(Path base, String rawDirName) throws IOException
    {
        generateFiles(PathUtils.ensureDirExists(base.resolve(rawDirName)), (in) -> String.format("%s: %s", rawDirName, in));
        String encodedDirName = URLEncoder.encode(rawDirName, StandardCharsets.UTF_8);
        if (!rawDirName.equals(encodedDirName))
        {
            generateFiles(PathUtils.ensureDirExists(base.resolve(encodedDirName)), (in) -> String.format("%s: %s", encodedDirName, in));
        }
    }

    private static void generateFiles(Path outputDir, Function<String, String> contentsFunction)
    {
        // Add simple file to directory
        touchFile(outputDir.resolve("exists.txt"), "dir exists: " + outputDir.getFileName().toString());

        // URI Reserved = gen-delims  = ":" / "/" / "?" / "#" / "[" / "]" / "@"
        touchFiles(outputDir, "this_is_100%_valid.txt", "reserved-percent-100", contentsFunction);
        touchFiles(outputDir, "vote-results-50%-of-precincts.txt", "reserved-percent-50", contentsFunction);
        touchFiles(outputDir, "%_played.txt", "reserved-percent-played", contentsFunction);
        touchFiles(outputDir, "there_are_two_choices:foo_or_bar.txt", "reserved-colon-choices", contentsFunction);
        touchFiles(outputDir, "m:main_or_master.txt", "reserved-colon-m", contentsFunction);
        touchFiles(outputDir, ":colon.txt", "reserved-colon-start", contentsFunction);
        touchFiles(outputDir, "are_we_not_robots?.txt", "reserved-question-mark-end", contentsFunction);
        touchFiles(outputDir, "?question.txt", "reserved-question-mark-start", contentsFunction);
        touchFiles(outputDir, "deck_#4_of_15.txt", "reserved-hash-deck4", contentsFunction);
        touchFiles(outputDir, "#hashcode.txt", "reserved-hash-start", contentsFunction);
        touchFiles(outputDir, "[brackets].txt", "reserved-brackets-start", contentsFunction);
        touchFiles(outputDir, "byte[0].txt", "reserved-brackets-byte-array-empty", contentsFunction);

        // URI Reserved = sub-delims  = "!" / "$" / "&" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "="
        touchFiles(outputDir, "wat!.txt", "reserved-exclamation-wat", contentsFunction);
        touchFiles(outputDir, "!important.txt", "reserved-exclamation-important", contentsFunction);
        touchFiles(outputDir, "us$.txt", "reserved-dollar-us", contentsFunction);
        touchFiles(outputDir, "$for_null.txt", "reserved-dollar-start", contentsFunction);
        touchFiles(outputDir, "this&that.txt", "reserved-ampersand-this-that", contentsFunction);
        touchFiles(outputDir, "&more.txt", "reserved-ampersand-more", contentsFunction);
        touchFiles(outputDir, "quoth_the_raven_'nevermore'.txt", "reserved-apostrophe-raven", contentsFunction);
        touchFiles(outputDir, "'apostrophe.txt", "reserved-apostrophe-start", contentsFunction);
        touchFiles(outputDir, "method(params).txt", "reserved-parens-params", contentsFunction);
        touchFiles(outputDir, "(parens).txt", "reserved-parens-start", contentsFunction);
        touchFiles(outputDir, "asterisk-*.txt", "reserved-asterisk-end", contentsFunction);
        touchFiles(outputDir, "*-global.txt", "reserved-asterisk-start", contentsFunction);
        touchFiles(outputDir, "that+those.txt", "reserved-plus-that-those", contentsFunction);
        touchFiles(outputDir, "+plus.txt", "reserved-plus-start", contentsFunction);
        touchFiles(outputDir, "some,more,commands.txt", "reserved-comma-some-more", contentsFunction);
        touchFiles(outputDir, ",delim.txt", "reserved-comma-start", contentsFunction);
        touchFiles(outputDir, "wait;what.txt", "reserved-semi-wait-what", contentsFunction);
        touchFiles(outputDir, ";how.txt", "reserved-semi-start", contentsFunction);
        touchFiles(outputDir, "foo=bar.txt", "reserved-equals-foo-bar", contentsFunction);
        touchFiles(outputDir, "=zed.txt", "reserved-equals-foo-start", contentsFunction);

        // complex filenames - combinations of reserved characters
        touchFiles(outputDir, "&lt;xml&gt;.txt", "reserved-ampersand-xml-teeth", contentsFunction);
        touchFiles(outputDir, "copyright&#00a9;2021.txt", "reserved-ampersand-entity-copyright-symbol", contentsFunction);
    }

    private static void touchFiles(Path outputDir, String filename, String contents, Function<String, String> contentsFunction)
    {
        touchFile(outputDir.resolve(filename), contentsFunction.apply(contents + "-raw"));
        String encodedFilename = PathUtils.encodePath(filename);
        if (!encodedFilename.equals(filename))
        {
            touchFile(outputDir.resolve(encodedFilename), contentsFunction.apply(contents + "-encoded"));
        }
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
