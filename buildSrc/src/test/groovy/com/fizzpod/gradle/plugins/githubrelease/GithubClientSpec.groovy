/* (C) 2026 */
/* SPDX-License-Identifier: Apache-2.0 */
package com.fizzpod.gradle.plugins.githubrelease

import java.io.File
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import spock.lang.Specification

class GithubClientSpec extends Specification {

    MockWebServer server
    GithubClient client

    def setup() {
        server = new MockWebServer()
        server.start()
        def baseUrl = server.url("").toString()
        // Strip trailing slash
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1)
        }
        client = new GithubClient("test-token", baseUrl, "owner/repo", null, null)
    }

    def cleanup() {
        server.close()
    }

    def "getLogin retrieves user login"() {
        given:
        server.enqueue(new MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body('{"login": "test-user"}')
            .build())

        when:
        def login = client.getLogin()

        then:
        login == "test-user"
        RecordedRequest request = server.takeRequest()
        request.getUrl().encodedPath() == "/user"
        request.getHeaders().get("Authorization") == "Bearer test-token"
        request.getHeaders().get("Accept") == "application/vnd.github+json"
    }

    def "getDefaultBranch retrieves default branch"() {
        given:
        server.enqueue(new MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body('{"default_branch": "main"}')
            .build())

        when:
        def branch = client.getDefaultBranch()

        then:
        branch == "main"
        RecordedRequest request = server.takeRequest()
        request.getUrl().encodedPath() == "/repos/owner/repo"
    }

    def "getReleaseByTagName returns map when release exists"() {
        given:
        server.enqueue(new MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body('{"id": 12345, "tag_name": "v1.0"}')
            .build())

        when:
        def release = client.getReleaseByTagName("v1.0")

        then:
        release != null
        release.id == 12345
        release.tag_name == "v1.0"
        RecordedRequest request = server.takeRequest()
        request.getUrl().encodedPath() == "/repos/owner/repo/releases/tags/v1.0"
    }

    def "getReleaseByTagName returns null when release not found"() {
        given:
        server.enqueue(new MockResponse.Builder()
            .code(404)
            .build())

        when:
        def release = client.getReleaseByTagName("v1.0")

        then:
        release == null
    }

    def "deleteRelease performs delete request"() {
        given:
        server.enqueue(new MockResponse.Builder().code(204).build())

        when:
        client.deleteRelease(12345)

        then:
        noExceptionThrown()
        RecordedRequest request = server.takeRequest()
        request.getUrl().encodedPath() == "/repos/owner/repo/releases/12345"
        request.getMethod() == "DELETE"
    }

    def "createRelease creates release and returns map"() {
        given:
        server.enqueue(new MockResponse.Builder()
            .code(201)
            .setHeader("Content-Type", "application/json")
            .body('{"id": 9876, "tag_name": "v2.0", "upload_url": "http://upload"}')
            .build())

        when:
        def release = client.createRelease("v2.0", "Release v2.0", "main", false, false, "Description")

        then:
        release.id == 9876
        release.tag_name == "v2.0"
        RecordedRequest request = server.takeRequest()
        request.getUrl().encodedPath() == "/repos/owner/repo/releases"
        request.getMethod() == "POST"
        request.getBody().utf8().contains('"tag_name":"v2.0"')
    }

    def "uploadAsset uploads binary file"() {
        given:
        server.enqueue(new MockResponse.Builder().code(201).build())
        File tempFile = File.createTempFile("test-asset", ".txt")
        tempFile.write("Hello World")
        def rawUploadUrl = server.url("/assets").toString() + "{?name,label}"

        when:
        client.uploadAsset(rawUploadUrl, tempFile, "text/plain")

        then:
        noExceptionThrown()
        RecordedRequest request = server.takeRequest()
        request.getUrl().encodedPath() == "/assets"
        request.getUrl().queryParameter("name") == tempFile.name
        request.getMethod() == "POST"
        request.getHeaders().get("Content-Type") == "text/plain"
        request.getBody().utf8() == "Hello World"

        cleanup:
        tempFile.delete()
    }

    def "custom configurer and headers are applied"() {
        given:
        server.enqueue(new MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body('{"login": "user"}')
            .build())

        // Custom configurer setting a header via interceptor, and a custom header map
        def configurer = { builder ->
            builder.addInterceptor({ chain ->
                def original = chain.request()
                def request = original.newBuilder()
                    .header("X-Custom-From-Builder", "yes")
                    .build()
                chain.proceed(request)
            })
        }
        def customHeaders = ["X-Custom-From-Map": "indeed"]

        def baseUrl = server.url("").toString()
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1)
        }
        def customClient = new GithubClient("test-token", baseUrl, "owner/repo", configurer, customHeaders)

        when:
        customClient.getLogin()

        then:
        RecordedRequest request = server.takeRequest()
        request.getHeaders().get("X-Custom-From-Builder") == "yes"
        request.getHeaders().get("X-Custom-From-Map") == "indeed"
    }

    def "listReleases returns list of releases"() {
        given:
        server.enqueue(new MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body('[{"id": 12345, "tag_name": "v1.0"}]')
            .build())

        when:
        def releases = client.listReleases()

        then:
        releases != null
        releases.size() == 1
        releases[0].id == 12345
        RecordedRequest request = server.takeRequest()
        request.getUrl().encodedPath() == "/repos/owner/repo/releases"
    }

    def "throws IOException on failed getLogin"() {
        given:
        server.enqueue(new MockResponse.Builder().code(500).body("Error").build())

        when:
        client.getLogin()

        then:
        thrown(IOException)
    }

    def "throws IOException on failed getDefaultBranch"() {
        given:
        server.enqueue(new MockResponse.Builder().code(500).body("Error").build())

        when:
        client.getDefaultBranch()

        then:
        thrown(IOException)
    }

    def "throws IOException on failed getReleaseByTagName"() {
        given:
        server.enqueue(new MockResponse.Builder().code(500).body("Error").build())

        when:
        client.getReleaseByTagName("v1.0")

        then:
        thrown(IOException)
    }

    def "throws IOException on failed listReleases"() {
        given:
        server.enqueue(new MockResponse.Builder().code(500).body("Error").build())

        when:
        client.listReleases()

        then:
        thrown(IOException)
    }

    def "throws IOException on failed deleteRelease"() {
        given:
        server.enqueue(new MockResponse.Builder().code(500).body("Error").build())

        when:
        client.deleteRelease(12345)

        then:
        thrown(IOException)
    }

    def "throws IOException on failed createRelease"() {
        given:
        server.enqueue(new MockResponse.Builder().code(500).body("Error").build())

        when:
        client.createRelease("v2.0", "Release v2.0", "main", false, false, "Description")

        then:
        thrown(IOException)
    }

    def "throws IOException on failed uploadAsset"() {
        given:
        server.enqueue(new MockResponse.Builder().code(500).body("Error").build())
        File tempFile = File.createTempFile("test-asset", ".txt")
        tempFile.write("Hello World")
        def rawUploadUrl = server.url("/assets").toString()

        when:
        client.uploadAsset(rawUploadUrl, tempFile, "text/plain")

        then:
        thrown(IOException)

        cleanup:
        tempFile.delete()
    }

    def "builder does not include Authorization header when token is null"() {
        given:
        server.enqueue(new MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body('{"login": "user"}')
            .build())
        def baseUrl = server.url("").toString()
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1)
        }
        def noTokenClient = new GithubClient(null, baseUrl, "owner/repo", null, null)

        when:
        noTokenClient.getLogin()

        then:
        RecordedRequest request = server.takeRequest()
        request.getHeaders().get("Authorization") == null
    }
}
