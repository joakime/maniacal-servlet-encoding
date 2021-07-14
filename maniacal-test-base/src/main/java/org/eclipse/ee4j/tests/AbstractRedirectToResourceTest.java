package org.eclipse.ee4j.tests;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractRedirectToResourceTest extends AbstractWebappTest
{
    @TestFactory
    public Stream<DynamicTest> dynamicTestsFromIntStream() throws IOException, InterruptedException
    {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder(getWebappURI().resolve("redir-resource/"))
            .GET()
            .build();
        HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, httpResponse.statusCode(), "Response status code");
        int indexCount = Integer.parseInt(httpResponse.body());

        return IntStream.iterate(0, n -> n + 1).limit(indexCount)
            .mapToObj(n -> DynamicTest.dynamicTest("RedirectToResource[" + n + "]",
                () -> testRedirectToResource(n)));
    }

    public void testRedirectToResource(int idx) throws IOException, InterruptedException
    {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder(getWebappURI().resolve("redir-resource/" + idx))
            .GET()
            .build();
        HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(302, httpResponse.statusCode(), "Redirect Response status code");
        String expectedRawPath = httpResponse.headers().firstValue("X-Idx-RawPath").orElseThrow(() -> new IllegalStateException("No X-Idx-RawPath response header found"));
        String expectedContents = httpResponse.headers().firstValue("X-Idx-Contents").orElseThrow(() -> new IllegalStateException("No X-Idx-Contents response header found"));
        String location = httpResponse.headers().firstValue("Location").orElseThrow(() -> new IllegalStateException("No Location response header found"));

        URI resourceUri = getWebappURI().resolve(location);
        String identifier = String.format("GET Request [%d] - RawPath [%s] - Location [%s] - URI [%s]", idx, expectedRawPath, location, resourceUri.toASCIIString());
        httpRequest = HttpRequest.newBuilder(resourceUri)
            .GET()
            .build();
        httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, httpResponse.statusCode(), "Resource Response status code - " + identifier);
        assertThat("Body Contents of : " + identifier, httpResponse.body(), containsString(expectedContents));
    }
}
