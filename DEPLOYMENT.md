# VectorWave Deployment Guide

This guide explains how to deploy VectorWave artifacts to Maven repositories.

## Prerequisites

- Maven 3.6+
- Java 21+ (core), Java 24 (extensions)
- Repository credentials (if deploying to remote repository)

## Quick Start - Local Deployment

By default, VectorWave is configured to deploy to a local repository at `~/.m2/local-deploy`:

```bash
# Deploy with default settings (local repository)
mvn clean deploy -DskipTests

# Or specify a custom local repository
mvn clean deploy -DskipTests \
  -Dsnapshots.repository.url=file:///path/to/custom/repo \
  -Dreleases.repository.url=file:///path/to/custom/repo
```

## Configuration Options

### 1. Local Repository Deployment

Deploy to a local file system repository:

```bash
# Deploy to custom local directory
mvn clean deploy -DskipTests \
  -Dsnapshots.repository.url=file:///path/to/local/repo \
  -Dreleases.repository.url=file:///path/to/local/repo
```

### 2. Nexus Repository Deployment

Configure your `~/.m2/settings.xml`:

```xml
<servers>
    <server>
        <id>snapshots</id>
        <username>your-username</username>
        <password>your-password</password>
    </server>
    <server>
        <id>releases</id>
        <username>your-username</username>
        <password>your-password</password>
    </server>
</servers>
```

Deploy with:

```bash
mvn clean deploy \
  -Dsnapshots.repository.url=https://nexus.example.com/repository/maven-snapshots/ \
  -Dreleases.repository.url=https://nexus.example.com/repository/maven-releases/
```

### 3. Artifactory Deployment

Configure your `~/.m2/settings.xml` with Artifactory credentials, then:

```bash
mvn clean deploy \
  -Dsnapshots.repository.url=https://artifactory.example.com/artifactory/libs-snapshot-local \
  -Dreleases.repository.url=https://artifactory.example.com/artifactory/libs-release-local
```

### 4. GitHub Packages Deployment

First, create a GitHub Personal Access Token with `write:packages` permission.

Configure `~/.m2/settings.xml`:

```xml
<servers>
    <server>
        <id>github</id>
        <username>YOUR_GITHUB_USERNAME</username>
        <password>YOUR_GITHUB_TOKEN</password>
    </server>
</servers>
```

Update the POM's distributionManagement to use `github` as the server ID:

```xml
<distributionManagement>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/MorphIQ-Labs/VectorWave</url>
    </repository>
</distributionManagement>
```

Deploy with:

```bash
mvn clean deploy \
  -Dsnapshots.repository.url=https://maven.pkg.github.com/MorphIQ-Labs/VectorWave \
  -Dreleases.repository.url=https://maven.pkg.github.com/MorphIQ-Labs/VectorWave
```

## Using Settings Profiles

Instead of specifying URLs on the command line, you can use the provided `settings-template.xml`:

1. Copy the template to your Maven settings:
   ```bash
   cp settings-template.xml ~/.m2/settings.xml
   ```

2. Edit the file with your repository URLs and credentials

3. Deploy using a profile:
   ```bash
   # Local deployment
   mvn clean deploy -Plocal-deploy
   
   # Nexus deployment
   mvn clean deploy -Pnexus-deploy
   
   # GitHub Packages
   mvn clean deploy -Pgithub-deploy
   ```

## Environment Variables

For security, use environment variables for credentials:

```bash
export REPO_USERNAME=your-username
export REPO_PASSWORD=your-password

mvn clean deploy \
  -Dsnapshots.repository.url=https://your-repo.com/snapshots \
  -Dreleases.repository.url=https://your-repo.com/releases
```

## Deploying Specific Modules

To deploy only specific modules:

```bash
# Deploy only core module
mvn clean deploy -pl vectorwave-core -am

# Deploy core and extensions
mvn clean deploy -pl vectorwave-core,vectorwave-extensions -am

# Skip examples module
mvn clean deploy -pl '!vectorwave-examples'
```

## Release Deployment

To deploy a release version (non-SNAPSHOT):

1. Update version to release:
   ```bash
   mvn versions:set -DnewVersion=1.0.0
   mvn versions:commit
   ```

2. Deploy release:
   ```bash
   mvn clean deploy -DskipTests \
     -Dreleases.repository.url=https://your-repo.com/releases
   ```

3. Update to next SNAPSHOT:
   ```bash
   mvn versions:set -DnewVersion=1.0.0-SNAPSHOT
   mvn versions:commit
   ```

## GPG Signing (Optional)

For release deployments to Maven Central, GPG signing is required:

1. Generate GPG key:
   ```bash
   gpg --gen-key
   ```

2. Configure Maven settings with GPG passphrase:
   ```xml
   <profiles>
     <profile>
       <id>gpg</id>
       <properties>
         <gpg.passphrase>your-passphrase</gpg.passphrase>
       </properties>
     </profile>
   </profiles>
   ```

3. Deploy with signing:
   ```bash
   mvn clean deploy -Pgpg -Dgpg.passphrase=your-passphrase
   ```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Deploy to GitHub Packages
on:
  push:
    branches: [ main ]
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '23'
          distribution: 'temurin'
      - name: Deploy to GitHub Packages
        run: |
          mvn clean deploy -DskipTests \
            -Dsnapshots.repository.url=https://maven.pkg.github.com/${{ github.repository }} \
            -Dreleases.repository.url=https://maven.pkg.github.com/${{ github.repository }}
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

### Jenkins Example

```groovy
pipeline {
    agent any
    tools {
        maven 'Maven-3.9'
        jdk 'JDK-23'
    }
    stages {
        stage('Deploy') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'nexus-credentials',
                    usernameVariable: 'REPO_USERNAME',
                    passwordVariable: 'REPO_PASSWORD'
                )]) {
                    sh '''
                        mvn clean deploy -DskipTests \
                          -Dsnapshots.repository.url=https://nexus.example.com/snapshots \
                          -Dreleases.repository.url=https://nexus.example.com/releases
                    '''
                }
            }
        }
    }
}
```

## Troubleshooting

### Error: "repository element was not specified"
- Ensure `distributionManagement` is configured in pom.xml
- Or provide `-DaltDeploymentRepository=id::url` parameter

### Error: "401 Unauthorized"
- Check credentials in `~/.m2/settings.xml`
- Ensure server ID matches repository ID
- Verify credentials have write permissions

### Error: "409 Conflict" (Nexus/Artifactory)
- Release versions cannot be redeployed
- Either increment version or delete existing artifact

### Error: "Failed to deploy artifacts"
- Check network connectivity
- Verify repository URL is correct
- Ensure repository accepts the artifact type

## Best Practices

1. **Use SNAPSHOT versions** for development
2. **Automate deployments** through CI/CD
3. **Secure credentials** using environment variables or encrypted settings
4. **Version consistently** across all modules
5. **Test locally** before deploying to remote repositories
6. **Document repository URLs** for team members

## Support

For deployment issues:
1. Check Maven debug output: `mvn deploy -X`
2. Verify repository configuration
3. Consult repository manager documentation
4. Open an issue on GitHub if VectorWave-specific