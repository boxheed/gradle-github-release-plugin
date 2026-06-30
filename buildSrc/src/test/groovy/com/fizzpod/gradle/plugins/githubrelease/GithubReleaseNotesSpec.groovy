/* (C) 2026 */
/* SPDX-License-Identifier: Apache-2.0 */
package com.fizzpod.gradle.plugins.githubrelease

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import spock.lang.Specification

class GithubReleaseNotesSpec extends Specification {

    MockWebServer server

    def setup() {
        server = new MockWebServer()
        server.start()
    }

    def cleanup() {
        server.close()
    }

    def "gets release notes successfully"() {
        given:
        server.enqueue(new MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body('{"body": "Changelog details", "name": "v1.0"}')
            .build())
            
        def baseUrl = server.url("").toString()
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1)
        }

        def configurer = { builder ->
            builder.addInterceptor({ chain ->
                def original = chain.request()
                def request = original.newBuilder()
                    .header("X-Custom-From-Builder", "yes")
                    .build()
                chain.proceed(request)
            })
        }

        def notes = new GithubReleaseNotes(
            toTag: "v1.0",
            fromTag: "v0.9",
            token: "test-token",
            targetCommitish: "main",
            repoName: "owner/repo",
            endpoint: baseUrl,
            clientConfigurer: configurer,
            customHeaders: ["X-Test-Header": "test-value"]
        )

        when:
        def result = notes.get()

        then:
        result.body == "Changelog details"
        RecordedRequest request = server.takeRequest()
        request.getUrl().encodedPath() == "/repos/owner/repo/releases/generate-notes"
        request.getMethod() == "POST"
        request.getHeaders().get("Authorization") == "Bearer test-token"
        request.getHeaders().get("X-Test-Header") == "test-value"
        request.getHeaders().get("X-Custom-From-Builder") == "yes"
        request.getBody().utf8().contains('"tag_name": "v1.0"')
        request.getBody().utf8().contains('"previous_tag_name": "v0.9"')
    }

    def "throws IOException on non-200 response"() {
        given:
        server.enqueue(new MockResponse.Builder()
            .code(400)
            .body("Bad Request")
            .build())
            
        def baseUrl = server.url("").toString()
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1)
        }

        def notes = new GithubReleaseNotes(
            toTag: "v1.0",
            fromTag: "v0.9",
            token: "test-token",
            targetCommitish: "main",
            repoName: "owner/repo",
            endpoint: baseUrl
        )

        when:
        notes.get()

        then:
        thrown(IOException)
    }
}
