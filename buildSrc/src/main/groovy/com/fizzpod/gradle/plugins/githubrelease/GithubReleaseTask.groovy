/* (C) 2024-2026 */
/* SPDX-License-Identifier: Apache-2.0 */
package com.fizzpod.gradle.plugins.githubrelease

import static GithubReleaseTaskResolvers.*

import groovy.json.*
import javax.inject.Inject
import org.apache.tika.Tika
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "Not worth caching")
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
                repoName: context.repoName,
                endpoint: context.endpoint,
                clientConfigurer: context.clientConfigurer,
                customHeaders: context.customHeaders)
                .get()
                
            context.body = context.body + notes.body
            
        }
        if(context.dryRun) {
            println("DryRun:")
            def printedContext = context.clone()
            if (printedContext.token) {
                printedContext.token = "********"
            }
            println(printedContext)
        } else {
            if(context.release && context.overwrite) {
                context.client.deleteRelease(context.release.id)
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
        context.release = context.client.createRelease(
            context.tagName,
            context.releaseName,
            context.targetCommitish,
            context.draft,
            context.prerelease,
            context.body
        )
    }

    def upload(def context) {
        Tika tika = new Tika()
        for (asset in context.assets.files) {
            String mimetype = tika.detect(asset)
            context.client.uploadAsset(context.release.upload_url, asset, mimetype)
        }
    }
}
