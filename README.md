[![CircleCI](https://circleci.com/gh/boxheed/gradle-github-release-plugin/tree/main.svg?style=shield)](https://circleci.com/gh/boxheed/gradle-github-release-plugin/tree/main)

# Gradle GitHub Release Plugin

The Gradle GitHub Release Plugin is a Gradle plugin that automates the process of creating GitHub releases for your projects. With this plugin, you can simplify the release management process and ensure that your project releases are well-documented on GitHub.

## Features

- Automatic creation of GitHub releases based on Gradle tasks.
- Customizable release notes and release assets.
- Integration with your GitHub repository.
- Configuration compatible with the [com.github.breadmoirai.github-release](https://plugins.gradle.org/plugin/com.github.breadmoirai.github-release) plugin

## Prerequisites

Before using this plugin, you should have the following prerequisites:

- A Gradle-based project.
- A GitHub repository for your project.

## Installation

To use the Gradle GitHub Release Plugin, add the following to your project's `build.gradle` file:

```groovy
plugins {
    id 'com.github.lifecompany.github-release' version '0.3.0'
}
```

You can replace `0.3.0` with the desired version of the plugin.

## Configuration

To configure the plugin, you can add the following block to your build.gradle file:

```groovy
githubRelease {
    token = 'YOUR_GITHUB_PERSONAL_ACCESS_TOKEN'
    repo = 'YOUR_GITHUB_REPOSITORY'
    tagName = 'v1.0.0'
    releaseName = 'Release 1.0.0'
    generateReleaseNotes = true
    releaseAssets { ->
        jar.outputs.files
    }
}
```

* token (required): Your GitHub personal access token with repo and write:packages scopes.
* authorization (optional): An alias to `token`. If `token` is specified then that overrides the `authorization` value
* login (optional): Username of the for the login to use for authentication that requires a user
* github (optional): Closure to create the `org.kohsuke.github.api.GitHub` object used for the connection, if specified it overrides any `token`/`authorization`/`login` values
* repo (optional): Your GitHub repository in the format username/repository. Automatically detected from the remote URL.
* owner (optional): Github repository owner. Automatically detected from the remote URL.
* releaseName (required): The name of the release
* targetCommitish (optional): Branch name. Defaults to current branch.
* generateReleaseNotes (optional): Automatically generate release notes using the github API
* body (optional): The release note
* draft (optional): Draft github release. Defaults to `true`.
* prerelease (optional): Prerelease flag. Defaults to `false`
* releaseAssets (optional): A list of files to include as release assets.
* allowUploadToExisting (optional): Allows upload to an existing release. Defaults to `false`
* overwrite (optional): Overwrite existing release. Defaults to `false`
* dryRun (optional): Perform a dry run. Defaults to `false`
* apiEndpoint (optional): Override the default API endpoint. Default: `https://api.github.com`

## Authentication Options for GitHub

The plugin requires authentication to interact with the GitHub API. You can authenticate using a personal access token (PAT). Below are the details:

- **Personal Access Token (PAT)**:  
  You must provide a GitHub personal access token with the following scopes:
  - `repo`: Grants full control of private repositories.
  - `write:packages`: Allows writing to GitHub Packages.

  To generate a personal access token:
  1. Go to your GitHub account settings.
  2. Navigate to **Developer settings > Personal access tokens > Tokens (classic)**.
  3. Click **Generate new token**, select the required scopes, and copy the token.

  Once generated, add the token to your `build.gradle` configuration:

  ```groovy
  githubRelease {
      token = 'YOUR_GITHUB_PERSONAL_ACCESS_TOKEN'
  }
  ```
- **Environment Variable**:  
  Alternatively, you can set the token as an environment variable:

  ```bash
  export GITHUB_TOKEN=your_personal_access_token
  ```

  Then, reference it in your `build.gradle` file:

  ```groovy
  githubRelease {
      token = System.getenv('GITHUB_TOKEN')
  }
  ```

## Usage

Once you've configured the plugin, you can create a GitHub release by running the following Gradle task:

```shell
./gradlew githubRelease
```

This task will use the configuration provided in your build.gradle file to create a release on GitHub.

## License

This project is licensed under the Apache 2 License - see the LICENSE file for details.
