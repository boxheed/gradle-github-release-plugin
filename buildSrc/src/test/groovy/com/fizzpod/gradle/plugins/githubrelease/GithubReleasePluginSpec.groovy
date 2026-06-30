/* (C) 2026 */
/* SPDX-License-Identifier: Apache-2.0 */
package com.fizzpod.gradle.plugins.githubrelease

import java.io.File
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class GithubReleasePluginSpec extends Specification {

    MockWebServer server
    Project project
    GithubReleasePluginExtension extension
    GithubReleaseTask task
    File tempFile

    def setup() {
        server = new MockWebServer()
        server.start()

        project = ProjectBuilder.builder().build()
        project.plugins.apply('com.fizzpod.github-release')
        extension = project.extensions.findByName(GithubReleasePlugin.NAME)
        task = project.tasks.findByName(GithubReleasePlugin.NAME)

        tempFile = File.createTempFile("test-asset", ".txt")
        tempFile.write("Asset content")
    }

    def cleanup() {
        server.close()
        tempFile.delete()
    }

    def getBaseUrl() {
        def url = server.url("").toString()
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1)
        }
        return url
    }

    def "plugin applies extension and task"() {
        expect:
        extension != null
        task != null
        task instanceof GithubReleaseTask
    }

    def "runTask dryRun with generateReleaseNotes"() {
        given:
        server.enqueue(new MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body('{"default_branch": "main"}')
            .build())
        server.enqueue(new MockResponse.Builder()
            .code(404)
            .build())
        server.enqueue(new MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body('[{"tag_name": "v0.9"}]')
            .build())
        server.enqueue(new MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body('{"body": " - Auto generated notes", "name": "v1.0"}')
            .build())

        extension.token = "test-token"
        extension.apiEndpoint = getBaseUrl()
        extension.repo = "owner/repo"
        extension.tagName = "v1.0"
        extension.releaseName = "Release v1.0"
        extension.body = "Initial body"
        extension.generateReleaseNotes = true
        extension.dryRun = true
        extension.releaseAssets = { [tempFile] }

        when:
        task.runTask()

        then:
        noExceptionThrown()
        
        RecordedRequest r1 = server.takeRequest()
        r1.getUrl().encodedPath() == "/repos/owner/repo"
        
        RecordedRequest r2 = server.takeRequest()
        r2.getUrl().encodedPath() == "/repos/owner/repo/releases/tags/v1.0"

        RecordedRequest r3 = server.takeRequest()
        r3.getUrl().encodedPath() == "/repos/owner/repo/releases"

        RecordedRequest r4 = server.takeRequest()
        r4.getUrl().encodedPath() == "/repos/owner/repo/releases/generate-notes"
        r4.getBody().utf8().contains('"previous_tag_name": "v0.9"')
    }

    def "runTask creates release and uploads assets when release does not exist"() {
        given:
        server.enqueue(new MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body('{"default_branch": "main"}')
            .build())
        server.enqueue(new MockResponse.Builder()
            .code(404)
            .build())
        server.enqueue(new MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body('[]')
            .build())
        server.enqueue(new MockResponse.Builder()
            .code(201)
            .setHeader("Content-Type", "application/json")
            .body('{"id": 999, "tag_name": "v1.0", "upload_url": "' + getBaseUrl() + '/upload{?name,label}"}')
            .build())
        server.enqueue(new MockResponse.Builder()
            .code(201)
            .build())

        extension.token = "test-token"
        extension.apiEndpoint = getBaseUrl()
        extension.repo = "owner/repo"
        extension.tagName = "v1.0"
        extension.releaseName = "Release v1.0"
        extension.body = "Initial body"
        extension.generateReleaseNotes = false
        extension.dryRun = false
        extension.releaseAssets = { [tempFile] }

        when:
        task.runTask()

        then:
        noExceptionThrown()

        server.takeRequest()
        server.takeRequest()
        server.takeRequest()
        
        RecordedRequest createReq = server.takeRequest()
        createReq.getMethod() == "POST"
        createReq.getUrl().encodedPath() == "/repos/owner/repo/releases"
        
        RecordedRequest uploadReq = server.takeRequest()
        uploadReq.getMethod() == "POST"
        uploadReq.getUrl().encodedPath() == "/upload"
        uploadReq.getUrl().queryParameter("name") == tempFile.name
    }

    def "runTask overwrites release when release exists and overwrite is true"() {
        given:
        server.enqueue(new MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body('{"default_branch": "main"}')
            .build())
        server.enqueue(new MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body('{"id": 1234, "tag_name": "v1.0"}')
            .build())
        server.enqueue(new MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body('[]')
            .build())
        server.enqueue(new MockResponse.Builder()
            .code(204)
            .build())
        server.enqueue(new MockResponse.Builder()
            .code(201)
            .setHeader("Content-Type", "application/json")
            .body('{"id": 1235, "tag_name": "v1.0", "upload_url": "' + getBaseUrl() + '/upload{?name,label}"}')
            .build())
        server.enqueue(new MockResponse.Builder()
            .code(201)
            .build())

        extension.token = "test-token"
        extension.apiEndpoint = getBaseUrl()
        extension.repo = "owner/repo"
        extension.tagName = "v1.0"
        extension.releaseName = "Release v1.0"
        extension.generateReleaseNotes = false
        extension.overwrite = true
        extension.dryRun = false
        extension.releaseAssets = { [tempFile] }

        when:
        task.runTask()

        then:
        noExceptionThrown()

        server.takeRequest()
        server.takeRequest()
        server.takeRequest()
        
        RecordedRequest deleteReq = server.takeRequest()
        deleteReq.getMethod() == "DELETE"
        deleteReq.getUrl().encodedPath() == "/repos/owner/repo/releases/1234"
        
        RecordedRequest createReq = server.takeRequest()
        createReq.getMethod() == "POST"
        
        RecordedRequest uploadReq = server.takeRequest()
        uploadReq.getMethod() == "POST"
    }

    def "runTask uploads directly to existing release when allowUploadToExisting is true"() {
        given:
        server.enqueue(new MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body('{"default_branch": "main"}')
            .build())
        server.enqueue(new MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body('{"id": 1234, "tag_name": "v1.0", "upload_url": "' + getBaseUrl() + '/upload{?name,label}"}')
            .build())
        server.enqueue(new MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body('[]')
            .build())
        server.enqueue(new MockResponse.Builder()
            .code(201)
            .build())

        extension.token = "test-token"
        extension.apiEndpoint = getBaseUrl()
        extension.repo = "owner/repo"
        extension.tagName = "v1.0"
        extension.releaseName = "Release v1.0"
        extension.generateReleaseNotes = false
        extension.allowUploadToExisting = true
        extension.dryRun = false
        extension.releaseAssets = { [tempFile] }

        when:
        task.runTask()

        then:
        noExceptionThrown()

        server.takeRequest()
        server.takeRequest()
        server.takeRequest()
        
        RecordedRequest uploadReq = server.takeRequest()
        uploadReq.getMethod() == "POST"
        uploadReq.getUrl().encodedPath() == "/upload"
    }
}
