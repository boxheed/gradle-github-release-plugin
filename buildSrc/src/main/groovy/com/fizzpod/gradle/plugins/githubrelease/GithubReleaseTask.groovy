package com.fizzpod.gradle.plugins.githubrelease

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
        def extension = project[GithubReleasePlugin.NAME]
        
        GitHub github = GitHub.connectUsingOAuth(extension.token)
        def myself = github.getMyself()
        def repo = github.getRepository(extension.owner + "/" + extension.repo)
        def builder = new GHReleaseBuilder(repo, extension.tagName)
        def release = builder.name(extension.releaseName)
            .commitish(extension.targetCommitish)
            .draft(extension.draft)
            .prerelease(extension.prerelease)
            .body(extension.body)
            .create()
        
        def assets = project.files()
        assets.setFrom(extension.releaseAssets.call())
        Tika tika = new Tika();
        for (asset in assets.files) {
            String mimetype = tika.detect(asset)
            release.uploadAsset(asset, mimetype)
        }
    }
}