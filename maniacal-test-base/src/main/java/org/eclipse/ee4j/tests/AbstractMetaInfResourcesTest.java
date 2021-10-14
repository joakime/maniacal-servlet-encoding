package org.eclipse.ee4j.tests;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class AbstractMetaInfResourcesTest extends AbstractWebappTest
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMetaInfResourcesTest.class);

    public static List<Arguments> metaInfResourceArguments() throws IOException
    {
        List<Arguments> cases = new ArrayList<>();

        String resourceName = "META-INF/resources/encoding/exists.txt";
        URL url = AbstractMetaInfResourcesTest.class.getClassLoader().getResource(resourceName);
        assertNotNull(url, "Unable to find " + resourceName);

        String deepClassRef = url.toExternalForm();
        int deepRefIdx = deepClassRef.indexOf("!/");
        Map<String, String> env = new HashMap<>();
        URI uri = URI.create(deepClassRef.substring(0, deepRefIdx));
        try (FileSystem zipfs = FileSystems.newFileSystem(uri, env))
        {
            Path base = zipfs.getPath("/META-INF/resources");
            Files.walk(base, 10)
                .filter(Files::isRegularFile)
                .forEach(path ->
                {
                    try
                    {
                        String relativeRawPath = base.relativize(path).toString();
                        String contents = Files.readString(path, UTF_8);
                        cases.add(Arguments.of(relativeRawPath, contents));
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                });
        }

        return cases;
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("metaInfResourceArguments")
    public void testAccessMetaInfResourceUsingJdkHttpClient(String rawPath, String expectedContents) throws IOException, InterruptedException
    {
        try
        {
            HttpClient httpClient = HttpClient.newHttpClient();
            URI uri = getWebappURI().resolve(new URI(rawPath));
            HttpRequest httpRequest = HttpRequest.newBuilder(uri).GET().build();
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, httpResponse.statusCode(), "Response status code");
            assertThat(httpResponse.body(), containsString(expectedContents));
        }
        catch (URISyntaxException e)
        {
            Assumptions.assumeFalse(true, "URI rawPath is invalid: [" + rawPath + "]: " + e.getMessage());
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("metaInfResourceArguments")
    public void testAccessMetaInfResourceUsingRawSocket(String rawPath, String expectedContents) throws IOException, InterruptedException
    {
        URI baseURI = getWebappURI();

        try (Socket socket = new Socket(baseURI.getHost(), baseURI.getPort());
             OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream())
        {
            StringBuilder req = new StringBuilder();
            req.append("GET ").append(baseURI.getRawPath()).append(rawPath).append(" HTTP/1.1\r\n");
            req.append("Host: ").append(baseURI.getRawAuthority()).append("\r\n");
            req.append("Connection: close\r\n");
            req.append("\r\n");

            byte[] bufRequest = req.toString().getBytes(UTF_8);

            if (LOGGER.isDebugEnabled())
                LOGGER.debug("--Request--\n" + req);
            out.write(bufRequest);
            out.flush();

            ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
            IOUtils.copy(in, outBuf);
            String response = outBuf.toString(UTF_8);
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("--Response--\n" + response);

            List<String> responseLines = response.lines()
                .map(String::trim)
                .collect(Collectors.toList());

            // Find status code
            String responseStatusLine = responseLines.get(0);
            assertThat("Status Code Response", responseStatusLine, containsString("HTTP/1.1 200"));

            // Find body content (always last line)
            String bodyContent = responseLines.get(responseLines.size() - 1);
            assertThat("Body Content", bodyContent, containsString(expectedContents));
        }
    }

    @Test
    public void testGenerateResultsCSV() throws IOException
    {
        Path outputPath = Paths.get("target/results-encoding.csv");
        if (!Files.exists(outputPath.getParent()))
        {
            Files.createDirectories(outputPath.getParent());
        }

        try (BufferedWriter results = Files.newBufferedWriter(outputPath))
        {
            List<Arguments> accessCases = AbstractMetaInfResourcesTest.metaInfResourceArguments();

            results.write(String.format("\"%s\", \"%s\", \"%s\", \"%s\", \"%s\"%n",
                "META-INF/resources path (as it exists in JAR)",
                "Raw Path Requested",
                "Raw URI Result",
                "Encoded Path Requested",
                "Encoded URI Result"));

            for (Arguments args : accessCases)
            {
                String rawPath = (String)args.get()[0];
                String expectedContents = (String)args.get()[1];
                String rawRequestResults = getRequestResults(rawPath, expectedContents);
                String encodedPath = EncodeUtils.encodeJavaPathToUriPath(rawPath);
                String encodedRequestResults = getRequestResults(encodedPath, expectedContents);
                results.write(String.format("\"%s\", \"%s\", \"%s\", \"%s\", \"%s\"%n",
                    rawPath, rawPath, rawRequestResults, encodedPath, encodedRequestResults));
            }
        }
    }

    protected String getRequestResults(String pathToRequest, String expectedContents)
    {
        URI baseURI = getWebappURI();

        try (Socket socket = new Socket(baseURI.getHost(), baseURI.getPort());
             OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream())
        {
            StringBuilder req = new StringBuilder();
            req.append("GET ").append(baseURI.getRawPath()).append(pathToRequest).append(" HTTP/1.1\r\n");
            req.append("Host: ").append(baseURI.getRawAuthority()).append("\r\n");
            req.append("Connection: close\r\n");
            req.append("\r\n");

            byte[] bufRequest = req.toString().getBytes(UTF_8);

            if (LOGGER.isDebugEnabled())
                LOGGER.debug("--Request--\n" + req);
            out.write(bufRequest);
            out.flush();

            ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
            IOUtils.copy(in, outBuf);
            String response = outBuf.toString(UTF_8);
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("--Response--\n" + response);

            List<String> responseLines = response.lines()
                .map(String::trim)
                .collect(Collectors.toList());

            // Find status code
            String responseStatusLine = responseLines.get(0);
            if (!responseStatusLine.startsWith("HTTP/1.1 200"))
            {
                if (responseStatusLine.startsWith("HTTP/1.1 "))
                    return responseStatusLine.substring("HTTP/1.1 ".length());
                else
                    return responseStatusLine;
            }

            // Find body content (always last line)
            String bodyContent = responseLines.get(responseLines.size() - 1);
            if (!bodyContent.contains(expectedContents))
            {
                return "Wrong Resource: Expected <" + expectedContents + "> got <" + bodyContent + ">";
            }
            return "OK";
        }
        catch (Throwable t)
        {
            return "Exception: " + t.getClass().getName() + ": " + t.getMessage();
        }
    }
}
