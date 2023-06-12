package com.fizzpod.gradle.plugins.githubrelease

import org.gradle.api.Project

import okhttp3.*
import groovy.json.*

public class GithubReleaseNotes {
    
    def toTag = ""
    def fromTag = ""
    def token = ""
    def targetCommitish = ""
    def repoName = ""
    
    
    
    def get() {
        
        JsonBuilder builder = new JsonBuilder()
        builder {
            tag_name toTag
            target_commitish targetCommitish
            previous_tag_name fromTag
        }
        def payload = JsonOutput.prettyPrint(builder.toString())
    
        OkHttpClient okclient = new OkHttpClient()
            .newBuilder()
            .build();
        MediaType mediaType = MediaType.parse("application/json");

        RequestBody body = RequestBody.create(mediaType, payload)
        def url = "https://api.github.com/repos/" + repoName + "/releases/generate-notes"
        Request request = new Request.Builder()
            .url(url)
            .method("POST", body)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("X-GitHub-Api-Version", "2022-11-28")
            .addHeader("Authorization", "Bearer " + token)
            .build();
        try(def response = okclient.newCall(request).execute()) {
            String content = response.body().string()
            def jsonSlurper = new JsonSlurper()
            def object = jsonSlurper.parseText(content)
            if(response.code != 200)  {
                throw new IOException("Could not get changelog. status: " + response.code + " body: " + content)
            } else {
                return object
            }
        }
    }
}