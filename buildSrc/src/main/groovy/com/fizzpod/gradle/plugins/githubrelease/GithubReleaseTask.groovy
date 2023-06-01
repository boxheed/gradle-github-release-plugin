package com.fizzpod.gradle.plugins.githubrelease

import static GithubReleaseTaskResolvers.*

import org.gradle.api.Project
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
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
        def context = [:]
        def extension = project[GithubReleasePlugin.NAME]
        github(context, extension)
        repository(context, extension)
        releaseName(context, extension)
        tagName(context, extension)
        targetCommitish(context, extension)
        draft(context, extension)
        prerelease(context, extension)
        body(context, extension)
        context.assets = project.files()
        context.assets.setFrom(extension.releaseAssets.call())

        def builder = new GHReleaseBuilder(context.repo, context.tagName)
        def release = builder.name(context.releaseName)
            .commitish(context.targetCommitish)
            .draft(context.draft)
            .prerelease(context.prerelease)
            .body(context.body)
            .create()
        
        Tika tika = new Tika();
        for (asset in context.assets.files) {
            String mimetype = tika.detect(asset)
            release.uploadAsset(asset, mimetype)
        }
    }
}