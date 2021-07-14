package org.eclipse.ee4j.tests;

import java.io.File;
import java.net.URI;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JettyDefaultServletAccessTest extends AbstractDefaultServletAccessTest
{
    private Server server;

    @BeforeEach
    public void init() throws Exception
    {
        server = new Server();

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(0);
        server.addConnector(connector);

        MavenArtifactResolver mavenArtifactResolver = new MavenArtifactResolver();
        File warFile = mavenArtifactResolver.resolveTestWarFile();
        System.out.println("war file = " + warFile);
        assertNotNull(warFile, "war file reference");
        assertTrue(warFile.exists(), "war file should exist");

        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setWarResource(new PathResource(warFile.toPath()));

        server.setHandler(webAppContext);
        server.start();
    }

    @AfterEach
    public void destroy() throws Exception
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
