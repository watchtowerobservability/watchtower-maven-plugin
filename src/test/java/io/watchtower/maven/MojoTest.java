package io.watchtower.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MojoTest {
  @Test
  void uploadsReleaseThenMapping(@TempDir File dir) throws Exception {
    List<String> seen = new ArrayList<>();
    HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    s.createContext("/", ex -> {
      byte[] raw = ex.getRequestBody().readAllBytes();
      boolean gz = "gzip".equals(ex.getRequestHeaders().getFirst("Content-Encoding"));
      byte[] body = gz ? new GZIPInputStream(new ByteArrayInputStream(raw)).readAllBytes() : raw;
      seen.add(ex.getRequestURI() + " " + ex.getRequestHeaders().getFirst("Authorization") + " " + new String(body));
      ex.sendResponseHeaders(201, -1);
      ex.close();
    });
    s.start();
    try {
      File map = new File(dir, "proguard/mapping.txt");
      map.getParentFile().mkdirs();
      Files.writeString(map.toPath(), "a.B -> a:\n");
      UploadMojo m = new UploadMojo();
      m.url = "http://127.0.0.1:" + s.getAddress().getPort() + "/";
      m.project = "prj_1";
      m.authToken = "test-key-not-real";
      m.release = "2.0";
      m.mappingFiles = List.of(map, new File(dir, "missing.txt"));
      m.execute();
    } finally {
      s.stop(0);
    }
    assertEquals(2, seen.size());
    assertTrue(seen.get(0).startsWith("/v1/projects/prj_1/releases Bearer test-key-not-real {\"version\":\"2.0\"}"));
    assertTrue(seen.get(1).contains("kind=proguard&name=proguard%2Fmapping.txt&release=2.0&debug_id="));
    assertTrue(seen.get(1).endsWith("a.B -> a:\n"));
  }

  @Test
  void missingCredentialsFail(@TempDir File dir) throws Exception {
    File map = new File(dir, "mapping.txt");
    Files.writeString(map.toPath(), "x");
    UploadMojo m = new UploadMojo();
    m.release = "1";
    m.mappingFiles = List.of(map);
    assertThrows(MojoExecutionException.class, m::execute);
  }

  @Test
  void nothingToUploadIsFine() throws Exception {
    new UploadMojo().execute();
  }

  @Test
  void writesRelease(@TempDir File dir) throws Exception {
    WriteReleaseMojo m = new WriteReleaseMojo();
    m.release = "3.1";
    m.outputDirectory = dir;
    m.execute();
    assertEquals("release=3.1\n", Files.readString(new File(dir, "META-INF/watchtower.properties").toPath()));
  }
}
