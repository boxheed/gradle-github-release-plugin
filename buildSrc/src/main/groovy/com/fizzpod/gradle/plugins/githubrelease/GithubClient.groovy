/* (C) 2026 */
/* SPDX-License-Identifier: Apache-2.0 */
package com.fizzpod.gradle.plugins.githubrelease

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import java.net.URLEncoder
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

public class GithubClient {
    private final OkHttpClient client
    private final String token
    private final String apiEndpoint
    private final String repoName
    private final Map<String, String> customHeaders

    public GithubClient(String token, String apiEndpoint, String repoName, Closure clientConfigurer, Map<String, String> customHeaders) {
        def builder = new OkHttpClient.Builder()
        if (clientConfigurer != null) {
            clientConfigurer.call(builder)
        }
        this.client = builder.build()
        this.token = token
        this.apiEndpoint = apiEndpoint
        this.repoName = repoName
        this.customHeaders = customHeaders ?: [:]
    }

    private Request.Builder newBuilder(String path) {
        def url = apiEndpoint + path
        def builder = new Request.Builder()
            .url(url)
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("X-GitHub-Api-Version", "2022-11-28")
        
        if (token) {
            builder.addHeader("Authorization", "Bearer " + token)
        }

        customHeaders.each { key, value ->
            builder.addHeader(key, value)
        }
        return builder
    }

    public String getLogin() {
        Request request = newBuilder("/user").get().build()
        try (def response = client.newCall(request).execute()) {
            def body = response.body().string()
            if (response.code() != 200) {
                throw new IOException("Failed to get user info: ${response.code()} ${body}")
            }
            return new JsonSlurper().parseText(body).login
        }
    }

    public String getDefaultBranch() {
        Request request = newBuilder("/repos/" + repoName).get().build()
        try (def response = client.newCall(request).execute()) {
            def body = response.body().string()
            if (response.code() != 200) {
                throw new IOException("Failed to get repo info: ${response.code()} ${body}")
            }
            return new JsonSlurper().parseText(body).default_branch
        }
    }

    public Map getReleaseByTagName(String tagName) {
        Request request = newBuilder("/repos/" + repoName + "/releases/tags/" + tagName).get().build()
        try (def response = client.newCall(request).execute()) {
            if (response.code() == 404) return null
            def body = response.body().string()
            if (response.code() != 200) {
                throw new IOException("Failed to get release: ${response.code()} ${body}")
            }
            return new JsonSlurper().parseText(body) as Map
        }
    }

    public List listReleases() {
        Request request = newBuilder("/repos/" + repoName + "/releases").get().build()
        try (def response = client.newCall(request).execute()) {
            def body = response.body().string()
            if (response.code() != 200) {
                throw new IOException("Failed to list releases: ${response.code()} ${body}")
            }
            return new JsonSlurper().parseText(body) as List
        }
    }

    public void deleteRelease(long releaseId) {
        Request request = newBuilder("/repos/" + repoName + "/releases/" + releaseId).delete().build()
        try (def response = client.newCall(request).execute()) {
            if (response.code() != 204) {
                throw new IOException("Failed to delete release: ${response.code()} ${response.body().string()}")
            }
        }
    }

    public Map createRelease(String tagName, String releaseName, String commitish, boolean isDraft, boolean isPrerelease, String bodyText) {
        JsonBuilder builder = new JsonBuilder()
        builder {
            tag_name tagName
            target_commitish commitish
            name releaseName
            body bodyText
            draft isDraft
            prerelease isPrerelease
        }
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), builder.toString())
        Request request = newBuilder("/repos/" + repoName + "/releases").post(body).build()
        try (def response = client.newCall(request).execute()) {
            def responseBody = response.body().string()
            if (response.code() != 201) {
                throw new IOException("Failed to create release: ${response.code()} ${responseBody}")
            }
            return new JsonSlurper().parseText(responseBody) as Map
        }
    }

    public void uploadAsset(String uploadUrlTemplate, File file, String contentType) {
        def uploadUrl = uploadUrlTemplate
        int bracketIndex = uploadUrl.indexOf('{')
        if (bracketIndex != -1) {
            uploadUrl = uploadUrl.substring(0, bracketIndex)
        }
        uploadUrl += "?name=" + URLEncoder.encode(file.name, "UTF-8")

        RequestBody body = RequestBody.create(MediaType.parse(contentType), file)
        
        def builder = new Request.Builder()
            .url(uploadUrl)
            .post(body)
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("X-GitHub-Api-Version", "2022-11-28")
            .addHeader("Content-Type", contentType)
        
        if (token) {
            builder.addHeader("Authorization", "Bearer " + token)
        }

        customHeaders.each { key, value ->
            builder.addHeader(key, value)
        }
        Request request = builder.build()

        try (def response = client.newCall(request).execute()) {
            if (response.code() != 201) {
                throw new IOException("Failed to upload asset: ${response.code()} ${response.body().string()}")
            }
        }
    }
}
