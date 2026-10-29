package io.watchtower.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/** {@code mvn watchtower:upload}: create the release, then upload each ProGuard mapping file. */
@Mojo(name = "upload", defaultPhase = LifecyclePhase.PACKAGE)
public class UploadMojo extends AbstractMojo {
  /** API base URL. */
  @Parameter(property = "watchtower.url", defaultValue = "${env.WT_API}")
  String url;

  /** Project id. */
  @Parameter(property = "watchtower.project", defaultValue = "${env.WT_PROJECT}")
  String project;

  /** Project-admin API key; use the environment variable, not the pom. Sent as a Bearer header, never logged. */
  @Parameter(property = "watchtower.authToken", defaultValue = "${env.WT_API_KEY}")
  String authToken;

  /** Release name; must match the app's service.version. */
  @Parameter(property = "watchtower.release", defaultValue = "${project.version}")
  String release;

  /** ProGuard mapping files; missing files are skipped. */
  @Parameter(property = "watchtower.mappingFiles")
  List<File> mappingFiles;

  @Override
  public void execute() throws MojoExecutionException {
    if (mappingFiles == null) mappingFiles = List.of();
    List<File> present = mappingFiles.stream().filter(File::isFile).toList();
    if (present.isEmpty()) {
      getLog().info("Watchtower: no mapping files found, nothing to upload");
      return;
    }
    try {
      WatchtowerClient c = new WatchtowerClient(url, project, authToken);
      c.ensureRelease(release);
      for (File f : present) {
        byte[] data = Files.readAllBytes(f.toPath());
        String name = f.getParentFile() == null ? f.getName() : f.getParentFile().getName() + "/" + f.getName();
        c.upload("proguard", name, release, WatchtowerClient.debugId(data), data);
        getLog().info("Watchtower: uploaded " + name);
      }
    } catch (IllegalArgumentException | IOException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MojoExecutionException("interrupted", e);
    }
  }
}
