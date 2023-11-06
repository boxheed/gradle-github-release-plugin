package com.fizzpod.gradle.plugins.githubrelease

import static GithubReleaseTaskResolvers.*

import org.gradle.api.Project
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import groovy.json.*
import javax.inject.Inject

import org.kohsuke.github.GitHub
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GHReleaseBuilder

import org.apache.tika.Tika

public class GithubReleaseTask extends DefaultTask {

    public static final NAME = GithubReleasePlugin.NAME

    private Project project

    @Inject
    GithubReleaseTask(Project project) {
        this.project = project
    }

    static register(Project project) {
        project.getLogger().debug("Registering task {}", NAME)
        def taskContainer = project.getTasks()

        taskContainer.create([name: NAME,
            type: GithubReleaseTask,
            dependsOn: [],
            group: GithubReleasePlugin.GROUP,
            description: 'Creates a release on Github and uploads artefacts'])
    }

        
    @TaskAction
    def runTask() {
        def extension = project[GithubReleasePlugin.NAME]
        def context = [:]
        context.project = project
        context.extension = extension
        github(context, extension)
        repository(context, extension)
        releaseName(context, extension)
        tagName(context, extension)
        targetCommitish(context, extension)
        draft(context, extension)
        prerelease(context, extension)
        body(context, extension)
        allowUploadToExisting(context, extension)
        overwrite(context, extension)
        dryRun(context, extension)
        assets(context, extension)
        release(context, extension)
        previousRelease(context, extension)
        if(context.extension.generateReleaseNotes) {
            def notes = new GithubReleaseNotes(
                toTag: context.tagName,
                fromTag: context.previousTagName,
                token: context.token,
                targetCommitish: context.targetCommitish,
                repoName: context.repoName)
                .get()
                
            context.body = context.body + notes.body
            
        }
        if(context.dryRun) {
            println("DryRun:")
            println(context)
        } else {
            if(context.release && context.overwite) {
                context.release.delete()
                context.release = null
            } else if (context.release && context.allowUploadToExisting) {
                upload(context)
            }
            if(!context.release) {
                create(context)
                upload(context)
            }
        }
        
    }

    def create(def context) {
        
        context.release = context.repo.createRelease(context.tagName)
            .name(context.releaseName)
            .commitish(context.targetCommitish)
            .draft(context.draft)
            .prerelease(context.prerelease)
            .body(context.body)
            .create()
    }

    def upload(def context) {
        Tika tika = new Tika();
        for (asset in context.assets.files) {
            String mimetype = tika.detect(asset)
            context.release.uploadAsset(asset, mimetype)
        }
    }
}