# watchtower-maven-plugin

Maven plugin for JVM builds: uploads ProGuard mappings to Watchtower so stack traces are readable, and writes the
release name where the [Java SDK](https://github.com/watchtowerobservability/watchtower-java) picks it up.

## Install

GitHub Packages Maven repository `https://maven.pkg.github.com/watchtowerobservability/watchtower-maven-plugin`
(add it as a `pluginRepository`, token with `read:packages`). Attested jars are also on each release.

## Quick start

```xml
<plugin>
  <groupId>io.github.watchtowerobservability</groupId>
  <artifactId>watchtower-maven-plugin</artifactId>
  <version>0.1.0</version>
  <configuration>
    <url>https://watchtower.example.com</url>
    <project>prj_123</project>
    <mappingFiles><file>${project.build.directory}/proguard/mapping.txt</file></mappingFiles>
  </configuration>
  <executions><execution><goals><goal>write-release</goal><goal>upload</goal></goals></execution></executions>
</plugin>
```

The key comes from `WT_API_KEY` (a project-admin key); keep it out of the pom.

## Integrations

| Goal | Phase | What it does |
|---|---|---|
| `watchtower:write-release` | `process-resources` | writes `META-INF/watchtower.properties` with the release |
| `watchtower:upload` | `package` | creates the release (idempotent), uploads each mapping file as kind `proguard` |

Native symbols and Android builds are covered by `watchtower-gradle-plugin`.

## Configuration

| Parameter | Property | Env | Default |
|---|---|---|---|
| `url` | `watchtower.url` | `WT_API` | required |
| `project` | `watchtower.project` | `WT_PROJECT` | required |
| `authToken` | `watchtower.authToken` | `WT_API_KEY` | required; sent as `Authorization: Bearer`, never logged |
| `release` | `watchtower.release` | | project version |
| `mappingFiles` | `watchtower.mappingFiles` | | none; missing files are skipped |

Same API as `wtctl`: files go gzip-compressed to `/v1/projects/{id}/artifacts`; identical bytes upload as a no-op.

## Provenance

`gh attestation verify <file> --repo watchtowerobservability/watchtower-maven-plugin`

## License

Apache-2.0. Source of truth is the Watchtower monorepo; this repository is a published mirror.
