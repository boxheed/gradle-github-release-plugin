/* (C) 2024-2025 */
/* SPDX-License-Identifier: Apache-2.0 */
package com.fizzpod.gradle.plugins.githubrelease

import org.apache.tika.Tika
import org.gradle.api.Project
import org.kohsuke.github.GHReleaseBuilder
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub

public class GithubReleaseTaskResolvers {

    public static def github(def context, def extension) {
        def token = resolve(extension.token)? resolve(extension.token):resolve(extension.authorization)
        def endpoint = resolve(extension.apiEndpoint)
        def login = resolve(extension.login)
        def github = resolve(extension.github)
        context.token = token
        context.endpoint = endpoint
        context.login = login
        if(github != null) {
            context.github = github
        } else if(token && endpoint && login) {
            context.github = GitHub.connectToEnterpriseWithOAuth(endpoint, login, token)
        } else if(token && endpoint) {
            context.github = GitHub.connectUsingOAuth(endpoint, token)
        } else if(token && login) {
            context.github = GitHub.connect(login, token)
        } else if(token) {
            context.github = GitHub.connectUsingOAuth(token)
        } else {
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
                def myself = context.github.getMyself()
                owner = myself.getLogin()
            }
            repoName = "${owner}/${repo}"
        }
        //create the repository
        context.repoName = repoName
        context.repo = context.github.getRepository(repoName)
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
            context.targetCommitish = context.repo.getDefaultBranch()
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
        context.release = context.repo.getReleaseByTagName(context.tagName)
        return context
    }
    
    public static def previousRelease(def context, def extension) {
        context.previousTagName = ""
        def previousRelease = context.repo.listReleases().find {
            release -> release.getTagName() != context.tagName 
        }
        if(previousRelease != null) {
            context.previousTagName = previousRelease.getTagName()
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
        if(!(value instanceof Closure)) {
            v = {value}
        }
        return v.call()
    }

}
