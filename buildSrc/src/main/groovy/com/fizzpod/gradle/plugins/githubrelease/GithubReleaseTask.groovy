package com.fizzpod.gradle.plugins.githubrelease

import org.gradle.api.Project
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

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
    }
}