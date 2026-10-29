package io.watchtower.maven;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/** {@code watchtower:write-release}: puts META-INF/watchtower.properties into the build output; the Java SDK reads it. */
@Mojo(name = "write-release", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class WriteReleaseMojo extends AbstractMojo {
  @Parameter(property = "watchtower.release", defaultValue = "${project.version}")
  String release;

  @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true)
  File outputDirectory;

  @Override
  public void execute() throws MojoExecutionException {
    try {
      Path f = outputDirectory.toPath().resolve("META-INF/watchtower.properties");
      Files.createDirectories(f.getParent());
      Files.writeString(f, "release=" + release.replace("\\", "\\\\").replace("\n", " ") + "\n", StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }
}
