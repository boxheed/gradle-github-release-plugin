package com.fizzpod.gradle.plugins.githubrelease

import org.gradle.api.Project

import org.kohsuke.github.GitHub
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GHReleaseBuilder

import org.apache.tika.Tika

public class GithubReleaseTaskResolvers {

    public static def github(def context, def extension) {
        def token = resolve(extension.token)
        context.github = GitHub.connectUsingOAuth(token)
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
        println("aa " + repo)
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