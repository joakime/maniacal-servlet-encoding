package org.eclipse.ee4j.tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Issue a request for a numbered resource, return a redirect for that resource
 * being served by the DefaultServlet.
 */
public class RedirectToDefaultResourceServlet extends HttpServlet
{
    private static class Arguments
    {
        private final String rawPath;
        private final String contents;

        public Arguments(String rawPath, String contents)
        {
            this.rawPath = rawPath;
            this.contents = contents;
        }

        public static Arguments of(String rawPath, String contents)
        {
            return new Arguments(rawPath, contents);
        }
    }

    private List<Arguments> redirectArguments = new ArrayList<>();

    @Override
    public void init() throws ServletException
    {
        super.init();

        // Basic access of root
        redirectArguments.add(Arguments.of("exists.txt", "This content is the root of the webapp / war"));

        // Access of resources with "/" which is encoded as "%2f"
        redirectArguments.add(Arguments.of("slosh/exists.txt", "this is the slosh jar"));
        redirectArguments.add(Arguments.of("slosh/root%2fthere.txt", "this is root there"));
        redirectArguments.add(Arguments.of("slosh/a%2fa/foo.txt", "this is content in a\\a/foo.txt"));
        redirectArguments.add(Arguments.of("slosh/b%2f/bar.txt", "this is more content in b\\/bar.txt"));
        redirectArguments.add(Arguments.of("slosh/%2fc/zed.txt", "this is even more content in \\c/zed.txt"));

        // Access of resources in slosh.jar!/META-INF/resources which has the string
        // sequence "%2f" in their filename, which is encoded as "%25" (for "%") followed by "2f"
        // resulting in "%252f"
        redirectArguments.add(Arguments.of("slosh/root%252Fthere.txt", "this is root%2Fthere"));
        redirectArguments.add(Arguments.of("slosh/a%252fa/foo.txt", "this is content in a%2fa/foo.txt"));
        redirectArguments.add(Arguments.of("slosh/b%252f/bar.txt", "this is more content in b%2f/bar.txt"));
        redirectArguments.add(Arguments.of("slosh/%252fc/zed.txt", "this is even more content in %2fc/zed.txt"));

        // Access of resources in uri-reserved.jar!/META-INF/resources which utilize
        // uri-reserved characters.
        List<String> dirNames = new ArrayList<>();
        dirNames.add("uri-reserved/");
        dirNames.add("uri-reserved/semi;colon/"); // TODO: This results in 100% 404 failure on Jetty
        dirNames.add("uri-reserved/semi%3bcolon/");
        dirNames.add("uri-reserved/question?mark/");  // TODO: This results in 100% 404 failure on Jetty
        dirNames.add("uri-reserved/question%3fmark/");
        // URI_UNSUPPORTED : java.lang.IllegalArgumentException: Illegal character in fragment at index 55: http://127.0.1.1:36131/maniacal/uri-reserved/hash#mark/#hashcode.txt
        // dirNames.add("uri-reserved/hash#mark/");
        dirNames.add("uri-reserved/hash%23mark/");

        for (String dirPrefix : dirNames)
        {
            // Basic access
            redirectArguments.add(Arguments.of(dirPrefix + "exists.txt", "dir exists: uri-reserved"));

            // Access of specific content
            // URI_UNSUPPORTED : java.lang.IllegalArgumentException: Malformed escape pair at index 56: http://127.0.1.1:46805/maniacal/uri-reserved/this_is_100%_valid.txt
            // redirectArguments.add(Arguments.of(dirPrefix + "this_is_100%_valid.txt", "reserved-percent-100-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "this_is_100%25_valid.txt", "reserved-percent-100-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "this_is_100%2525_valid.txt", "reserved-percent-100-encoded"));
            // URI_UNSUPPORTED : java.lang.IllegalArgumentException: Malformed escape pair at index 60: http://127.0.1.1:46805/maniacal/uri-reserved/vote-results-50%-of-precincts.txt
            // redirectArguments.add(Arguments.of(dirPrefix + "vote-results-50%-of-precincts.txt", "reserved-percent-50-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "vote-results-50%25-of-precincts.txt", "reserved-percent-50-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "vote-results-50%2525-of-precincts.txt", "reserved-percent-50-encoded"));
            // URI_UNSUPPORTED : java.lang.IllegalArgumentException: Malformed escape pair at index 45: http://127.0.1.1:46805/maniacal/uri-reserved/%_played.txt
            // redirectArguments.add(Arguments.of(dirPrefix + "%_played.txt", "reserved-percent-played-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%25_played.txt", "reserved-percent-played-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%2525_played.txt", "reserved-percent-played-encoded"));
            redirectArguments.add(Arguments.of(dirPrefix + "there_are_two_choices:foo_or_bar.txt", "reserved-colon-choices-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "there_are_two_choices%3Afoo_or_bar.txt", "reserved-colon-choices-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "there_are_two_choices%253Afoo_or_bar.txt", "reserved-colon-choices-encoded"));
            redirectArguments.add(Arguments.of(dirPrefix + "m:main_or_master.txt", "reserved-colon-m-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "m%3Amain_or_master.txt", "reserved-colon-m-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "m%253Amain_or_master.txt", "reserved-colon-m-encoded"));
            redirectArguments.add(Arguments.of(dirPrefix + ":colon.txt", "reserved-colon-start-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%3Acolon.txt", "reserved-colon-start-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%253Acolon.txt", "reserved-colon-start-encoded"));
            redirectArguments.add(Arguments.of(dirPrefix + "are_we_not_robots?.txt", "reserved-question-mark-end-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "are_we_not_robots%3F.txt", "reserved-question-mark-end-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "are_we_not_robots%253F.txt", "reserved-question-mark-end-encoded"));
            redirectArguments.add(Arguments.of(dirPrefix + "?question.txt", "reserved-question-mark-start-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%3Fquestion.txt", "reserved-question-mark-start-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%253Fquestion.txt", "reserved-question-mark-start-encoded"));
            // URI_UNSUPPORTED : java.lang.IllegalArgumentException: Illegal character in fragment at index 60: http://127.0.1.1:46059/maniacal/uri-reserved/hash#mark/deck_#4_of_15.txt
            // redirectArguments.add(Arguments.of(dirPrefix + "deck_#4_of_15.txt", "reserved-hash-deck4-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "deck_%234_of_15.txt", "reserved-hash-deck4-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "deck_%25234_of_15.txt", "reserved-hash-deck4-encoded"));
            redirectArguments.add(Arguments.of(dirPrefix + "#hashcode.txt", "reserved-hash-stgart-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%23hashcode.txt", "reserved-hash-stgart-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%2523hashcode.txt", "reserved-hash-stgart-encoded"));
            // URI_UNSUPPORTED : java.lang.IllegalArgumentException: Illegal character in path at index 45: http://127.0.1.1:46805/maniacal/uri-reserved/[brackets].txt
            // redirectArguments.add(Arguments.of(dirPrefix + "[brackets].txt", "reserved-brackets-start-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%5Bbrackets%5D.txt", "reserved-brackets-start-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%255Bbrackets%255D.txt", "reserved-brackets-start-encoded"));
            // URI_UNSUPPORTED : java.lang.IllegalArgumentException: Illegal character in path at index 49: http://127.0.1.1:46805/maniacal/uri-reserved/byte[0].txt
            // redirectArguments.add(Arguments.of(dirPrefix + "byte[0].txt", "reserved-brackets-byte-array-empty-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "byte%5B0%5D.txt", "reserved-brackets-byte-array-empty-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "byte%255B0%255D.txt", "reserved-brackets-byte-array-empty-encoded"));

            // URI Reserved = sub-delims  = "!" / "$" / "&" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "="
            redirectArguments.add(Arguments.of(dirPrefix + "wat!.txt", "reserved-exclamation-wat-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "wat%21.txt", "reserved-exclamation-wat-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "wat%2521.txt", "reserved-exclamation-wat-encoded"));
            redirectArguments.add(Arguments.of(dirPrefix + "!important.txt", "reserved-exclamation-important-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%21important.txt", "reserved-exclamation-important-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%2521important.txt", "reserved-exclamation-important-encoded"));
            redirectArguments.add(Arguments.of(dirPrefix + "us$.txt", "reserved-dollar-us-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "us%24.txt", "reserved-dollar-us-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "us%2524.txt", "reserved-dollar-us-encoded"));
            redirectArguments.add(Arguments.of(dirPrefix + "$for_null.txt", "reserved-dollar-start-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%24for_null.txt", "reserved-dollar-start-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%2524for_null.txt", "reserved-dollar-start-encoded"));
            redirectArguments.add(Arguments.of(dirPrefix + "this&that.txt", "reserved-ampersand-this-that-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "this%26that.txt", "reserved-ampersand-this-that-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "this%2526that.txt", "reserved-ampersand-this-that-encoded"));
            redirectArguments.add(Arguments.of(dirPrefix + "&more.txt", "reserved-ampersand-more-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%26more.txt", "reserved-ampersand-more-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%2526more.txt", "reserved-ampersand-more-encoded"));
            redirectArguments.add(Arguments.of(dirPrefix + "quoth_the_raven_'nevermore'.txt", "reserved-apostrophe-raven-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "quoth_the_raven_%27nevermore%27.txt", "reserved-apostrophe-raven-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "quoth_the_raven_%2527nevermore%2527.txt", "reserved-apostrophe-raven-encoded"));
            redirectArguments.add(Arguments.of(dirPrefix + "'apostrophe.txt", "reserved-apostrophe-start-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%27apostrophe.txt", "reserved-apostrophe-start-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%2527apostrophe.txt", "reserved-apostrophe-start-encoded"));
            redirectArguments.add(Arguments.of(dirPrefix + "method(params).txt", "reserved-parens-params-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "method%28params%29.txt", "reserved-parens-params-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "method%2528params%2529.txt", "reserved-parens-params-encoded"));
            redirectArguments.add(Arguments.of(dirPrefix + "(parens).txt", "reserved-parens-start-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%28parens%29.txt", "reserved-parens-start-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%2528parens%2529.txt", "reserved-parens-start-encoded"));
            redirectArguments.add(Arguments.of(dirPrefix + "asterisk-*.txt", "reserved-asterisk-end-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "asterisk-%2A.txt", "reserved-asterisk-end-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "asterisk-%252A.txt", "reserved-asterisk-end-encoded"));
            redirectArguments.add(Arguments.of(dirPrefix + "*-global.txt", "reserved-asterisk-start-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%2A-global.txt", "reserved-asterisk-start-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%252A-global.txt", "reserved-asterisk-start-encoded"));
            redirectArguments.add(Arguments.of(dirPrefix + "that+those.txt", "reserved-plus-that-those-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "that%2Bthose.txt", "reserved-plus-that-those-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "that%252Bthose.txt", "reserved-plus-that-those-encoded"));
            redirectArguments.add(Arguments.of(dirPrefix + "+plus.txt", "reserved-plus-start-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%2Bplus.txt", "reserved-plus-start-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%252Bplus.txt", "reserved-plus-start-encoded"));
            redirectArguments.add(Arguments.of(dirPrefix + "some,more,commands.txt", "reserved-comma-some-more-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "some%2Cmore%2Ccommands.txt", "reserved-comma-some-more-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "some%252Cmore%252Ccommands.txt", "reserved-comma-some-more-encoded"));
            redirectArguments.add(Arguments.of(dirPrefix + ",delim.txt", "reserved-comma-start-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%2Cdelim.txt", "reserved-comma-start-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%252Cdelim.txt", "reserved-comma-start-encoded"));
            redirectArguments.add(Arguments.of(dirPrefix + "wait;what.txt", "reserved-semi-wait-what-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "wait%3Bwhat.txt", "reserved-semi-wait-what-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "wait%253Bwhat.txt", "reserved-semi-wait-what-encoded"));
            redirectArguments.add(Arguments.of(dirPrefix + ";how.txt", "reserved-semi-start-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%3Bhow.txt", "reserved-semi-start-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%253Bhow.txt", "reserved-semi-start-encoded"));
            redirectArguments.add(Arguments.of(dirPrefix + "foo=bar.txt", "reserved-equals-foo-bar-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "foo%3Dbar.txt", "reserved-equals-foo-bar-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "foo%253Dbar.txt", "reserved-equals-foo-bar-encoded"));
            redirectArguments.add(Arguments.of(dirPrefix + "=zed.txt", "reserved-equals-foo-start-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%3Dzed.txt", "reserved-equals-foo-start-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%253Dzed.txt", "reserved-equals-foo-start-encoded"));

            // complex filenames - combinations of reserved characters
            redirectArguments.add(Arguments.of(dirPrefix + "&lt;xml&gt;.txt", "reserved-ampersand-xml-teeth-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%26lt%3Bxml%26gt%3B.txt", "reserved-ampersand-xml-teeth-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "%2526lt%253Bxml%2526gt%253B.txt", "reserved-ampersand-xml-teeth-encoded"));
            // URI_UNSUPPORTED : java.lang.IllegalArgumentException: Illegal character in fragment at index 65: http://127.0.1.1:46697/maniacal/uri-reserved/hash#mark/copyright&#00a9;2021.txt
            // redirectArguments.add(Arguments.of(dirPrefix + "copyright&#00a9;2021.txt", "reserved-ampersand-entity-copyright-symbol-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "copyright%26%2300a9%3B2021.txt", "reserved-ampersand-entity-copyright-symbol-raw"));
            redirectArguments.add(Arguments.of(dirPrefix + "copyright%2526%252300a9%253B2021.txt", "reserved-ampersand-entity-copyright-symbol-encoded"));
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
    {
        String idxStr = req.getPathInfo();
        if (idxStr == null)
            idxStr = "";
        else if (idxStr.startsWith("/"))
            idxStr = idxStr.substring(1);

        if (idxStr.trim().length() == 0)
        {
            // no index provided, return the count of resources.
            resp.setContentType("text/plain");
            resp.getWriter().printf("%d", redirectArguments.size());
        }
        else
        {
            int idx = Integer.parseInt(idxStr);
            Arguments args = redirectArguments.get(idx);
            resp.setHeader("X-Idx", String.valueOf(idx));
            resp.setHeader("X-Idx-RawPath", args.rawPath);
            resp.setHeader("X-Idx-Contents", args.contents);
            resp.sendRedirect(req.getContextPath() + "/" + args.rawPath);
        }
    }
}
