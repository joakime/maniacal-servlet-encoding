package org.eclipse.ee4j.tests;

import java.io.File;
import java.net.URI;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JettyRedirectToResourceTest extends AbstractRedirectToResourceTest
{
    private static Server server;

    @BeforeAll
    public static void init() throws Exception
    {
        server = new Server();

        HttpConfiguration httpConfiguration = new HttpConfiguration();
        // httpConfiguration.setUriCompliance(UriCompliance.RFC3986);

        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfiguration));
        connector.setPort(0);
        server.addConnector(connector);

        MavenArtifactResolver mavenArtifactResolver = new MavenArtifactResolver();
        File warFile = mavenArtifactResolver.resolveTestWarFile();
        System.out.println("war file = " + warFile);
        assertNotNull(warFile, "war file reference");
        assertTrue(warFile.exists(), "war file should exist");

        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setContextPath("/maniacal");
        webAppContext.setWarResource(new PathResource(warFile.toPath()));

        server.setHandler(webAppContext);
        server.start();
    }

    @AfterAll
    public static void destroy() throws Exception
    {
        server.stop();
        server.join();
    }

    @Override
    public URI getWebappURI()
    {
        return server.getURI().resolve("/maniacal/");
    }
}
