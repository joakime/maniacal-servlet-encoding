package org.eclipse.ee4j.tests;

import java.nio.charset.StandardCharsets;

public class PathUtils
{
    // Simple hex array
    private static final char[] HEXES = "0123456789ABCDEF".toCharArray();
    // The first 127 characters and their encodings.
    private static final String[] URI_ENCODED_CHARS;

    static
    {
        URI_ENCODED_CHARS = new String[127];

        // URI Reserved Chars
        String uriReservedGenDelims = ":?#[]@"; // intentionally missing "/"
        String uriReservedSubDelims = "!$&'()*+,;=";
        // Extra Reserved Chars (specified by Jetty)
        String jettyReserved = "%\"<> \\^`{}|";

        String reserved = uriReservedGenDelims + uriReservedSubDelims + jettyReserved;

        for (int i = 0; i < URI_ENCODED_CHARS.length; i++)
        {
            if ((i < 0x20) || // control characters
                (reserved.indexOf(i) != (-1)))
            {
                // encoding needed
                URI_ENCODED_CHARS[i] = String.format("%%%02X", i);
            }
            else
            {
                // raw character
                URI_ENCODED_CHARS[i] = String.valueOf((char)i);
            }
        }
    }

    public static String encodePath(String rawpath)
    {
        byte[] rawpathbuf = rawpath.getBytes(StandardCharsets.UTF_8);
        int len = rawpathbuf.length;
        StringBuilder buf = new StringBuilder(len * 2);
        for (byte c : rawpathbuf)
        {
            if (c <= URI_ENCODED_CHARS.length)
            {
                buf.append(URI_ENCODED_CHARS[c]);
            }
            else
            {
                buf.append('%');
                buf.append(HEXES[(c & 0xF0) >> 4]);
                buf.append(HEXES[(c & 0x0F)]);
            }
        }
        return buf.toString();
    }
}
