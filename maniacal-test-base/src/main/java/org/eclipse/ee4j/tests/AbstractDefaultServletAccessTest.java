package org.eclipse.ee4j.tests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractDefaultServletAccessTest extends AbstractWebappTest
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDefaultServletAccessTest.class);

    public static Stream<Arguments> metaInfResourceCases()
    {
        List<Arguments> cases = new ArrayList<>();

        // Basic access of root
        cases.add(Arguments.of("exists.txt", "This content is the root of the webapp / war"));

        // Access of resources with "\" which is encoded as "%5C"
        cases.add(Arguments.of("slosh/exists.txt", "this is the slosh jar"));
        cases.add(Arguments.of("slosh/root%5Cthere.txt", "this is root there"));
        cases.add(Arguments.of("slosh/a%5Ca/foo.txt", "this is content in a\\a/foo.txt"));
        cases.add(Arguments.of("slosh/b%5C/bar.txt", "this is more content in b\\/bar.txt"));
        cases.add(Arguments.of("slosh/%5Cc/zed.txt", "this is even more content in \\c/zed.txt"));

        // Access of resources in slosh.jar!/META-INF/resources which has the string
        // sequence "%5C" in their filename, which is encoded as "%25" (for "%") followed by "5C"
        // resulting in "%255C"
        cases.add(Arguments.of("slosh/root%255Cthere.txt", "this is root%5Cthere"));
        cases.add(Arguments.of("slosh/a%255Ca/foo.txt", "this is content in a%5Ca/foo.txt"));
        cases.add(Arguments.of("slosh/b%255C/bar.txt", "this is more content in b%5C/bar.txt"));
        cases.add(Arguments.of("slosh/%255Cc/zed.txt", "this is even more content in %5Cc/zed.txt"));

        // Access of resources in uri-reserved.jar!/META-INF/resources which utilize
        // uri-reserved characters.
        List<String> dirNames = new ArrayList<>();
        dirNames.add("uri-reserved/");
        // dirNames.add("uri-reserved/semi;colon/"); // TODO: Not supported
        dirNames.add("uri-reserved/semi%3bcolon/");
        // dirNames.add("uri-reserved/question?mark/"); // TODO: Not supported
        dirNames.add("uri-reserved/question%3fmark/");
        // dirNames.add("uri-reserved/hash#mark/"); // TODO: Not supported
        dirNames.add("uri-reserved/hash%23mark/");

        for (String dirPrefix : dirNames)
        {
            // Basic access
            String[] dirPrefixParts = dirPrefix.split("/");
            cases.add(Arguments.of(dirPrefix + "exists.txt", "dir exists: " + URLDecoder.decode(dirPrefixParts[dirPrefixParts.length - 1], UTF_8)));

            // Access of specific content
            cases.add(Arguments.of(dirPrefix + "this_is_100%_valid.txt", "reserved-percent-100-raw"));
            cases.add(Arguments.of(dirPrefix + "this_is_100%25_valid.txt", "reserved-percent-100-raw"));
            cases.add(Arguments.of(dirPrefix + "this_is_100%2525_valid.txt", "reserved-percent-100-encoded"));
            cases.add(Arguments.of(dirPrefix + "vote-results-50%-of-precincts.txt", "reserved-percent-50-raw"));
            cases.add(Arguments.of(dirPrefix + "vote-results-50%25-of-precincts.txt", "reserved-percent-50-raw"));
            cases.add(Arguments.of(dirPrefix + "vote-results-50%2525-of-precincts.txt", "reserved-percent-50-encoded"));
            cases.add(Arguments.of(dirPrefix + "%_played.txt", "reserved-percent-played-raw"));
            cases.add(Arguments.of(dirPrefix + "%25_played.txt", "reserved-percent-played-raw"));
            cases.add(Arguments.of(dirPrefix + "%2525_played.txt", "reserved-percent-played-encoded"));
            cases.add(Arguments.of(dirPrefix + "there_are_two_choices:foo_or_bar.txt", "reserved-colon-choices-raw"));
            cases.add(Arguments.of(dirPrefix + "there_are_two_choices%3Afoo_or_bar.txt", "reserved-colon-choices-raw"));
            cases.add(Arguments.of(dirPrefix + "there_are_two_choices%253Afoo_or_bar.txt", "reserved-colon-choices-encoded"));
            cases.add(Arguments.of(dirPrefix + "m:main_or_master.txt", "reserved-colon-m-raw"));
            cases.add(Arguments.of(dirPrefix + "m%3Amain_or_master.txt", "reserved-colon-m-raw"));
            cases.add(Arguments.of(dirPrefix + "m%253Amain_or_master.txt", "reserved-colon-m-encoded"));
            cases.add(Arguments.of(dirPrefix + ":colon.txt", "reserved-colon-start-raw"));
            cases.add(Arguments.of(dirPrefix + "%3Acolon.txt", "reserved-colon-start-raw"));
            cases.add(Arguments.of(dirPrefix + "%253Acolon.txt", "reserved-colon-start-encoded"));
            cases.add(Arguments.of(dirPrefix + "are_we_not_robots?.txt", "reserved-question-mark-end-raw"));
            cases.add(Arguments.of(dirPrefix + "are_we_not_robots%3F.txt", "reserved-question-mark-end-raw"));
            cases.add(Arguments.of(dirPrefix + "are_we_not_robots%253F.txt", "reserved-question-mark-end-encoded"));
            cases.add(Arguments.of(dirPrefix + "?question.txt", "reserved-question-mark-start-raw"));
            cases.add(Arguments.of(dirPrefix + "%3Fquestion.txt", "reserved-question-mark-start-raw"));
            cases.add(Arguments.of(dirPrefix + "%253Fquestion.txt", "reserved-question-mark-start-encoded"));
            cases.add(Arguments.of(dirPrefix + "deck_#4_of_15.txt", "reserved-hash-deck4-raw"));
            cases.add(Arguments.of(dirPrefix + "deck_%234_of_15.txt", "reserved-hash-deck4-raw"));
            cases.add(Arguments.of(dirPrefix + "deck_%25234_of_15.txt", "reserved-hash-deck4-encoded"));
            cases.add(Arguments.of(dirPrefix + "#hashcode.txt", "reserved-hash-start-raw"));
            cases.add(Arguments.of(dirPrefix + "%23hashcode.txt", "reserved-hash-start-raw"));
            cases.add(Arguments.of(dirPrefix + "%2523hashcode.txt", "reserved-hash-start-encoded"));
            cases.add(Arguments.of(dirPrefix + "[brackets].txt", "reserved-brackets-start-raw"));
            cases.add(Arguments.of(dirPrefix + "%5Bbrackets%5D.txt", "reserved-brackets-start-raw"));
            cases.add(Arguments.of(dirPrefix + "%255Bbrackets%255D.txt", "reserved-brackets-start-encoded"));
            cases.add(Arguments.of(dirPrefix + "byte[0].txt", "reserved-brackets-byte-array-empty-raw"));
            cases.add(Arguments.of(dirPrefix + "byte%5B0%5D.txt", "reserved-brackets-byte-array-empty-raw"));
            cases.add(Arguments.of(dirPrefix + "byte%255B0%255D.txt", "reserved-brackets-byte-array-empty-encoded"));

            // URI Reserved = sub-delims  = "!" / "$" / "&" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "="
            cases.add(Arguments.of(dirPrefix + "wat!.txt", "reserved-exclamation-wat-raw"));
            cases.add(Arguments.of(dirPrefix + "wat%21.txt", "reserved-exclamation-wat-raw"));
            cases.add(Arguments.of(dirPrefix + "wat%2521.txt", "reserved-exclamation-wat-encoded"));
            cases.add(Arguments.of(dirPrefix + "!important.txt", "reserved-exclamation-important-raw"));
            cases.add(Arguments.of(dirPrefix + "%21important.txt", "reserved-exclamation-important-raw"));
            cases.add(Arguments.of(dirPrefix + "%2521important.txt", "reserved-exclamation-important-encoded"));
            cases.add(Arguments.of(dirPrefix + "us$.txt", "reserved-dollar-us-raw"));
            cases.add(Arguments.of(dirPrefix + "us%24.txt", "reserved-dollar-us-raw"));
            cases.add(Arguments.of(dirPrefix + "us%2524.txt", "reserved-dollar-us-encoded"));
            cases.add(Arguments.of(dirPrefix + "$for_null.txt", "reserved-dollar-start-raw"));
            cases.add(Arguments.of(dirPrefix + "%24for_null.txt", "reserved-dollar-start-raw"));
            cases.add(Arguments.of(dirPrefix + "%2524for_null.txt", "reserved-dollar-start-encoded"));
            cases.add(Arguments.of(dirPrefix + "this&that.txt", "reserved-ampersand-this-that-raw"));
            cases.add(Arguments.of(dirPrefix + "this%26that.txt", "reserved-ampersand-this-that-raw"));
            cases.add(Arguments.of(dirPrefix + "this%2526that.txt", "reserved-ampersand-this-that-encoded"));
            cases.add(Arguments.of(dirPrefix + "&more.txt", "reserved-ampersand-more-raw"));
            cases.add(Arguments.of(dirPrefix + "%26more.txt", "reserved-ampersand-more-raw"));
            cases.add(Arguments.of(dirPrefix + "%2526more.txt", "reserved-ampersand-more-encoded"));
            cases.add(Arguments.of(dirPrefix + "quoth_the_raven_'nevermore'.txt", "reserved-apostrophe-raven-raw"));
            cases.add(Arguments.of(dirPrefix + "quoth_the_raven_%27nevermore%27.txt", "reserved-apostrophe-raven-raw"));
            cases.add(Arguments.of(dirPrefix + "quoth_the_raven_%2527nevermore%2527.txt", "reserved-apostrophe-raven-encoded"));
            cases.add(Arguments.of(dirPrefix + "'apostrophe.txt", "reserved-apostrophe-start-raw"));
            cases.add(Arguments.of(dirPrefix + "%27apostrophe.txt", "reserved-apostrophe-start-raw"));
            cases.add(Arguments.of(dirPrefix + "%2527apostrophe.txt", "reserved-apostrophe-start-encoded"));
            cases.add(Arguments.of(dirPrefix + "method(params).txt", "reserved-parens-params-raw"));
            cases.add(Arguments.of(dirPrefix + "method%28params%29.txt", "reserved-parens-params-raw"));
            cases.add(Arguments.of(dirPrefix + "method%2528params%2529.txt", "reserved-parens-params-encoded"));
            cases.add(Arguments.of(dirPrefix + "(parens).txt", "reserved-parens-start-raw"));
            cases.add(Arguments.of(dirPrefix + "%28parens%29.txt", "reserved-parens-start-raw"));
            cases.add(Arguments.of(dirPrefix + "%2528parens%2529.txt", "reserved-parens-start-encoded"));
            cases.add(Arguments.of(dirPrefix + "asterisk-*.txt", "reserved-asterisk-end-raw"));
            cases.add(Arguments.of(dirPrefix + "asterisk-%2A.txt", "reserved-asterisk-end-raw"));
            cases.add(Arguments.of(dirPrefix + "asterisk-%252A.txt", "reserved-asterisk-end-encoded"));
            cases.add(Arguments.of(dirPrefix + "*-global.txt", "reserved-asterisk-start-raw"));
            cases.add(Arguments.of(dirPrefix + "%2A-global.txt", "reserved-asterisk-start-raw"));
            cases.add(Arguments.of(dirPrefix + "%252A-global.txt", "reserved-asterisk-start-encoded"));
            cases.add(Arguments.of(dirPrefix + "that+those.txt", "reserved-plus-that-those-raw"));
            cases.add(Arguments.of(dirPrefix + "that%2Bthose.txt", "reserved-plus-that-those-raw"));
            cases.add(Arguments.of(dirPrefix + "that%252Bthose.txt", "reserved-plus-that-those-encoded"));
            cases.add(Arguments.of(dirPrefix + "+plus.txt", "reserved-plus-start-raw"));
            cases.add(Arguments.of(dirPrefix + "%2Bplus.txt", "reserved-plus-start-raw"));
            cases.add(Arguments.of(dirPrefix + "%252Bplus.txt", "reserved-plus-start-encoded"));
            cases.add(Arguments.of(dirPrefix + "some,more,commands.txt", "reserved-comma-some-more-raw"));
            cases.add(Arguments.of(dirPrefix + "some%2Cmore%2Ccommands.txt", "reserved-comma-some-more-raw"));
            cases.add(Arguments.of(dirPrefix + "some%252Cmore%252Ccommands.txt", "reserved-comma-some-more-encoded"));
            cases.add(Arguments.of(dirPrefix + ",delim.txt", "reserved-comma-start-raw"));
            cases.add(Arguments.of(dirPrefix + "%2Cdelim.txt", "reserved-comma-start-raw"));
            cases.add(Arguments.of(dirPrefix + "%252Cdelim.txt", "reserved-comma-start-encoded"));
            cases.add(Arguments.of(dirPrefix + "wait;what.txt", "reserved-semi-wait-what-raw"));
            cases.add(Arguments.of(dirPrefix + "wait%3Bwhat.txt", "reserved-semi-wait-what-raw"));
            cases.add(Arguments.of(dirPrefix + "wait%253Bwhat.txt", "reserved-semi-wait-what-encoded"));
            cases.add(Arguments.of(dirPrefix + ";how.txt", "reserved-semi-start-raw"));
            cases.add(Arguments.of(dirPrefix + "%3Bhow.txt", "reserved-semi-start-raw"));
            cases.add(Arguments.of(dirPrefix + "%253Bhow.txt", "reserved-semi-start-encoded"));
            cases.add(Arguments.of(dirPrefix + "foo=bar.txt", "reserved-equals-foo-bar-raw"));
            cases.add(Arguments.of(dirPrefix + "foo%3Dbar.txt", "reserved-equals-foo-bar-raw"));
            cases.add(Arguments.of(dirPrefix + "foo%253Dbar.txt", "reserved-equals-foo-bar-encoded"));
            cases.add(Arguments.of(dirPrefix + "=zed.txt", "reserved-equals-foo-start-raw"));
            cases.add(Arguments.of(dirPrefix + "%3Dzed.txt", "reserved-equals-foo-start-raw"));
            cases.add(Arguments.of(dirPrefix + "%253Dzed.txt", "reserved-equals-foo-start-encoded"));

            // complex filenames - combinations of reserved characters
            cases.add(Arguments.of(dirPrefix + "&lt;xml&gt;.txt", "reserved-ampersand-xml-teeth-raw"));
            cases.add(Arguments.of(dirPrefix + "%26lt%3Bxml%26gt%3B.txt", "reserved-ampersand-xml-teeth-raw"));
            cases.add(Arguments.of(dirPrefix + "%2526lt%253Bxml%2526gt%253B.txt", "reserved-ampersand-xml-teeth-encoded"));
            cases.add(Arguments.of(dirPrefix + "copyright&#00a9;2021.txt", "reserved-ampersand-entity-copyright-symbol-raw"));
            cases.add(Arguments.of(dirPrefix + "copyright%26%2300a9%3B2021.txt", "reserved-ampersand-entity-copyright-symbol-raw"));
            cases.add(Arguments.of(dirPrefix + "copyright%2526%252300a9%253B2021.txt", "reserved-ampersand-entity-copyright-symbol-encoded"));
        }

        return cases.stream();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("metaInfResourceCases")
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
    @MethodSource("metaInfResourceCases")
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
}
