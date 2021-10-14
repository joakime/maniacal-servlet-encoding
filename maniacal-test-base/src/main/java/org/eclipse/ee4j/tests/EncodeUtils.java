package org.eclipse.ee4j.tests;

import java.nio.charset.StandardCharsets;

public class EncodeUtils
{
    // Simple hex array
    private static final char[] HEXES = "0123456789ABCDEF".toCharArray();
    // The first 127 characters and their encodings.
    private static final String[] JAVA_PATH_TO_URI_PATH_ENCODINGS;

    static
    {
        JAVA_PATH_TO_URI_PATH_ENCODINGS = new String[127];

        // ABNF from https://datatracker.ietf.org/doc/html/rfc3986#appendix-A
        String uriAlpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        String uriDigit = "0123456789";
        String uriUnreserved = uriAlpha + uriDigit + "-._~";
        String uriPctEncoded = "%";
        String uriSubDelims = "!$&'()*+,;=";
        String uriPChar = uriUnreserved + uriPctEncoded + uriSubDelims + ":@";
        String uriPathSegment = uriPChar;

        // Altered ANBF to suit Java Path name to safe URI name encoding
        String javaToUriPathSafe = uriUnreserved + uriSubDelims.replace(';', '/') + ":@";

        for (int i = 0; i < JAVA_PATH_TO_URI_PATH_ENCODINGS.length; i++)
        {
            if (javaToUriPathSafe.indexOf(i) >= 0)
            {
                // raw character
                JAVA_PATH_TO_URI_PATH_ENCODINGS[i] = String.valueOf((char)i);
            }
            else
            {
                // encoding required
                JAVA_PATH_TO_URI_PATH_ENCODINGS[i] = String.format("%%%02X", i);
            }
        }
    }

    public static String encodeJavaPathToUriPath(String rawpath)
    {
        byte[] rawpathbuf = rawpath.getBytes(StandardCharsets.UTF_8);
        int len = rawpathbuf.length;
        StringBuilder buf = new StringBuilder(len * 2);
        for (byte c : rawpathbuf)
        {
            if (c <= JAVA_PATH_TO_URI_PATH_ENCODINGS.length)
            {
                buf.append(JAVA_PATH_TO_URI_PATH_ENCODINGS[c]);
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
