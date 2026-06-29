/* (C) 2024-2026 */
/* SPDX-License-Identifier: Apache-2.0 */
package com.fizzpod.gradle.plugins.githubrelease

import org.gradle.api.Project
import org.gradle.api.provider.Provider

public class GithubReleaseTaskResolvers {

    public static def github(def context, def extension) {
        def token = resolve(extension.token)? resolve(extension.token):resolve(extension.authorization)
        def endpoint = resolve(extension.apiEndpoint)
        def login = resolve(extension.login)
        def clientConfigurer = extension.client
        def customHeaders = extension.headers
        context.token = token
        context.endpoint = endpoint
        context.login = login
        context.clientConfigurer = clientConfigurer
        context.customHeaders = customHeaders

        if (!token) {
            throw new IllegalArgumentException("GitHub token must be provided")
        }

        return context
    }

    public static def repository(def context, def extension) {
        def patterns = [
			~/^git@.*:(\S+\/\S+)\.git/,        //ssh pattern
			~/^https:\/\/.*\/(\S+\/\S+)\.git/, //http url patter
			~/^(\S+\/\S+)/                     //just the reponame pattern
		]
        def repo = resolve(extension.repo)
        def repoName = null
        patterns.any { p ->
            def matcher = p.matcher(repo) 
            if(matcher.matches()) {
                repoName = matcher.group(1)
                return true
            }
        }
        if(repoName == null) {
            def owner = resolve(extension.owner)
            if("" == owner.trim()) {
                def tempClient = new GithubClient(context.token, context.endpoint, "", context.clientConfigurer, context.customHeaders)
                owner = tempClient.getLogin()
            }
            repoName = "${owner}/${repo}"
        }
        //create the repository
        context.repoName = repoName
        context.client = new GithubClient(context.token, context.endpoint, repoName, context.clientConfigurer, context.customHeaders)
        return context
    }

    public static def releaseName(def context, def extension) {
        context.releaseName = resolve(extension.releaseName)
        return context
    }

    public static def tagName(def context, def extension) {
        context.tagName = resolve(extension.tagName)
        return context
    }

    public static def targetCommitish(def context, def extension) {
        context.targetCommitish = resolve(extension.targetCommitish)
        if("" == context.targetCommitish) {
            context.targetCommitish = context.client.getDefaultBranch()
        }
        return context
    }

    public static def draft(def context, def extension) {
        context.draft = resolve(extension.draft)
        return context
    }

    public static def assets(def context, def extension) {
        context.assets = context.project.files()
        context.assets.setFrom(extension.releaseAssets.call())
        return context
    }

    public static def release(def context, def extension) {
        context.release = context.client.getReleaseByTagName(context.tagName)
        return context
    }
    
    public static def previousRelease(def context, def extension) {
        context.previousTagName = ""
        def previousRelease = context.client.listReleases().find {
            release -> release.tag_name != context.tagName 
        }
        if(previousRelease != null) {
            context.previousTagName = previousRelease.tag_name
        }
        return context
    }

    public static def allowUploadToExisting(def context, def extension) {
        context.allowUploadToExisting = extension.allowUploadToExisting
        return context
    }

    public static def overwrite(def context, def extension) {
        context.overwrite = extension.overwrite
        return context
    }

    public static def dryRun(def context, def extension) {
        context.dryRun = extension.dryRun
        return context
    }

    public static def prerelease(def context, def extension) {
        context.prerelease = resolve(extension.prerelease)
        return context
    }

    public static def body(def context, def extension) {
        context.body = resolve(extension.body)
        return context
    }

    public static def resolve(def value) {
        def v = value
        if (value instanceof Provider) {
            v = { value.get() }
        } else if (!(value instanceof Closure)) {
            v = { value }
        }
        return v.call()
    }

}
