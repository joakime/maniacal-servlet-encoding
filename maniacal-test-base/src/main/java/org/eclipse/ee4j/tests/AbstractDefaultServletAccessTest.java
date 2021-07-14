package org.eclipse.ee4j.tests;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractDefaultServletAccessTest
{
    public abstract URI getWebappURI();

    @Test
    public void testAccessRootExists() throws IOException, InterruptedException
    {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder(getWebappURI().resolve("exists.txt"))
            .GET()
            .build();
        HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, httpResponse.statusCode(), "Response status code");
        assertThat(httpResponse.body(), containsString("This content is the root of the webapp / war"));
    }

    public static Stream<Arguments> sloshAccessCases()
    {
        List<Arguments> cases = new ArrayList<>();

        // Access of resources with "/" which is encoded as "%2f"
        cases.add(Arguments.of("slosh/exists.txt", "this is the slosh jar"));
        cases.add(Arguments.of("slosh/root%2fthere.txt", "this is root there"));
        cases.add(Arguments.of("slosh/a%2fa/foo.txt", "this is content in a\\a/foo.txt"));
        cases.add(Arguments.of("slosh/b%2f/bar.txt", "this is more content in b\\/bar.txt"));
        cases.add(Arguments.of("slosh/%2fc/zed.txt", "this is even more content in \\c/zed.txt"));

        // Access of resources in slosh.jar!/META-INF/resources which has the string
        // sequence "%2f" in their filename, which is encoded as "%25" (for "%") followed by "2f"
        // resulting in "%252f"
        cases.add(Arguments.of("slosh/root%252Fthere.txt", "this is root%2Fthere"));
        cases.add(Arguments.of("slosh/a%252fa/foo.txt", "this is content in a%2fa/foo.txt"));
        cases.add(Arguments.of("slosh/b%252f/bar.txt", "this is more content in b%2f/bar.txt"));
        cases.add(Arguments.of("slosh/%252fc/zed.txt", "this is even more content in %2fc/zed.txt"));

        return cases.stream();
    }

    @ParameterizedTest
    @MethodSource("sloshAccessCases")
    public void testAccessSloshFile(String rawPath, String expectedContents) throws IOException, InterruptedException
    {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder(getWebappURI().resolve(rawPath))
            .GET()
            .build();
        HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, httpResponse.statusCode(), "Response status code");
        assertThat(httpResponse.body(), containsString(expectedContents));
    }
}
