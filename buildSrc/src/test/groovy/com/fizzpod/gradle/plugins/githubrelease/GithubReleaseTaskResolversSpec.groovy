/* (C) 2026 */
/* SPDX-License-Identifier: Apache-2.0 */
package com.fizzpod.gradle.plugins.githubrelease

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class GithubReleaseTaskResolversSpec extends Specification {

    MockWebServer server

    def setup() {
        server = new MockWebServer()
        server.start()
    }

    def cleanup() {
        server.close()
    }

    def getBaseUrl() {
        def url = server.url("").toString()
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1)
        }
        return url
    }

    def "github resolver parses token and apiEndpoint"() {
        given:
        def extension = new GithubReleasePluginExtension()
        extension.token = "my-token"
        extension.apiEndpoint = "http://my-endpoint"
        extension.login = "my-login"
        extension.headers = [H1: "V1"]
        def context = [:]

        when:
        def result = GithubReleaseTaskResolvers.github(context, extension)

        then:
        result.token == "my-token"
        result.endpoint == "http://my-endpoint"
        result.login == "my-login"
        result.customHeaders == [H1: "V1"]
    }

    def "github resolver falls back to authorization"() {
        given:
        def extension = new GithubReleasePluginExtension()
        extension.token = ""
        extension.authorization = "auth-token"
        def context = [:]

        when:
        def result = GithubReleaseTaskResolvers.github(context, extension)

        then:
        result.token == "auth-token"
    }

    def "github resolver throws exception if no token is provided"() {
        given:
        def extension = new GithubReleasePluginExtension()
        extension.token = ""
        extension.authorization = ""
        def context = [:]

        when:
        GithubReleaseTaskResolvers.github(context, extension)

        then:
        thrown(IllegalArgumentException)
    }

    def "repository resolver matches ssh pattern"() {
        given:
        def extension = new GithubReleasePluginExtension()
        extension.repo = "git@github.com:fizzpod/gradle-github-release-plugin.git"
        def context = [token: "test", endpoint: getBaseUrl()]

        when:
        def result = GithubReleaseTaskResolvers.repository(context, extension)

        then:
        result.repoName == "fizzpod/gradle-github-release-plugin"
        result.client != null
    }

    def "repository resolver matches https pattern"() {
        given:
        def extension = new GithubReleasePluginExtension()
        extension.repo = "https://github.com/fizzpod/gradle-github-release-plugin.git"
        def context = [token: "test", endpoint: getBaseUrl()]

        when:
        def result = GithubReleaseTaskResolvers.repository(context, extension)

        then:
        result.repoName == "fizzpod/gradle-github-release-plugin"
    }

    def "repository resolver matches owner/repo pattern"() {
        given:
        def extension = new GithubReleasePluginExtension()
        extension.repo = "fizzpod/gradle-github-release-plugin"
        def context = [token: "test", endpoint: getBaseUrl()]

        when:
        def result = GithubReleaseTaskResolvers.repository(context, extension)

        then:
        result.repoName == "fizzpod/gradle-github-release-plugin"
    }

    def "repository resolver resolves repo with explicit owner"() {
        given:
        def extension = new GithubReleasePluginExtension()
        extension.repo = "gradle-github-release-plugin"
        extension.owner = "explicit-owner"
        def context = [token: "test", endpoint: getBaseUrl()]

        when:
        def result = GithubReleaseTaskResolvers.repository(context, extension)

        then:
        result.repoName == "explicit-owner/gradle-github-release-plugin"
    }

    def "repository resolver resolves repo with empty owner by calling user login"() {
        given:
        server.enqueue(new MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body('{"login": "resolved-owner"}')
            .build())
        def extension = new GithubReleasePluginExtension()
        extension.repo = "gradle-github-release-plugin"
        extension.owner = ""
        def context = [token: "test", endpoint: getBaseUrl()]

        when:
        def result = GithubReleaseTaskResolvers.repository(context, extension)

        then:
        result.repoName == "resolved-owner/gradle-github-release-plugin"
    }

    def "resolve releaseName and tagName"() {
        given:
        def extension = new GithubReleasePluginExtension()
        extension.releaseName = "Release v1.0"
        extension.tagName = "v1.0"
        def context = [:]

        when:
        GithubReleaseTaskResolvers.releaseName(context, extension)
        GithubReleaseTaskResolvers.tagName(context, extension)

        then:
        context.releaseName == "Release v1.0"
        context.tagName == "v1.0"
    }

    def "targetCommitish is set when non-empty"() {
        given:
        def extension = new GithubReleasePluginExtension()
        extension.targetCommitish = "development"
        def context = [:]

        when:
        GithubReleaseTaskResolvers.targetCommitish(context, extension)

        then:
        context.targetCommitish == "development"
    }

    def "targetCommitish queries default branch when empty"() {
        given:
        server.enqueue(new MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body('{"default_branch": "main"}')
            .build())
        def extension = new GithubReleasePluginExtension()
        extension.targetCommitish = ""
        def context = [
            client: new GithubClient("test", getBaseUrl(), "owner/repo", null, null)
        ]

        when:
        GithubReleaseTaskResolvers.targetCommitish(context, extension)

        then:
        context.targetCommitish == "main"
    }

    def "draft, prerelease, and body resolvers"() {
        given:
        def extension = new GithubReleasePluginExtension()
        extension.draft = true
        extension.prerelease = false
        extension.body = "Changelog details"
        def context = [:]

        when:
        GithubReleaseTaskResolvers.draft(context, extension)
        GithubReleaseTaskResolvers.prerelease(context, extension)
        GithubReleaseTaskResolvers.body(context, extension)

        then:
        context.draft == true
        context.prerelease == false
        context.body == "Changelog details"
    }

    def "assets resolver sets from releaseAssets closure"() {
        given:
        Project project = ProjectBuilder.builder().build()
        def extension = new GithubReleasePluginExtension()
        def dummyFile = new File(project.projectDir, "dummy.txt")
        extension.releaseAssets = { [dummyFile] }
        def context = [project: project]

        when:
        GithubReleaseTaskResolvers.assets(context, extension)

        then:
        context.assets != null
        context.assets.files.contains(dummyFile)
    }

    def "release resolver retrieves release by tag name"() {
        given:
        server.enqueue(new MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body('{"id": 12345, "tag_name": "v1.0"}')
            .build())
        def context = [
            tagName: "v1.0",
            client: new GithubClient("test", getBaseUrl(), "owner/repo", null, null)
        ]

        when:
        GithubReleaseTaskResolvers.release(context, [:])

        then:
        context.release != null
        context.release.id == 12345
    }

    def "previousRelease resolver retrieves previous release tag"() {
        given:
        server.enqueue(new MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body('[{"tag_name": "v1.0"}, {"tag_name": "v0.9"}]')
            .build())
        def context = [
            tagName: "v1.0",
            client: new GithubClient("test", getBaseUrl(), "owner/repo", null, null)
        ]

        when:
        GithubReleaseTaskResolvers.previousRelease(context, [:])

        then:
        context.previousTagName == "v0.9"
    }

    def "previousRelease resolver sets empty when no other release exists"() {
        given:
        server.enqueue(new MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body('[{"tag_name": "v1.0"}]')
            .build())
        def context = [
            tagName: "v1.0",
            client: new GithubClient("test", getBaseUrl(), "owner/repo", null, null)
        ]

        when:
        GithubReleaseTaskResolvers.previousRelease(context, [:])

        then:
        context.previousTagName == ""
    }

    def "allowUploadToExisting, overwrite, and dryRun resolvers"() {
        given:
        def extension = new GithubReleasePluginExtension()
        extension.allowUploadToExisting = true
        extension.overwrite = false
        extension.dryRun = true
        def context = [:]

        when:
        GithubReleaseTaskResolvers.allowUploadToExisting(context, extension)
        GithubReleaseTaskResolvers.overwrite(context, extension)
        GithubReleaseTaskResolvers.dryRun(context, extension)

        then:
        context.allowUploadToExisting == true
        context.overwrite == false
        context.dryRun == true
    }

    def "resolve helper wraps closure and provider correctly"() {
        given:
        def providerMock = [get: { "provider-value" }] as org.gradle.api.provider.Provider
        def closureVal = { "closure-value" }
        def plainVal = "plain-value"

        expect:
        GithubReleaseTaskResolvers.resolve(providerMock) == "provider-value"
        GithubReleaseTaskResolvers.resolve(closureVal) == "closure-value"
        GithubReleaseTaskResolvers.resolve(plainVal) == "plain-value"
    }
}
