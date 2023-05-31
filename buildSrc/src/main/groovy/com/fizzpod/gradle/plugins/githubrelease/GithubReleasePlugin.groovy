package com.fizzpod.gradle.plugins.githubrelease

import org.gradle.api.Plugin
import org.gradle.api.Project

public class GithubReleasePlugin implements Plugin<Project> {

	public static final String NAME = "githubRelease"
	public static final String GROUP = "release"

	void apply(Project project) {
		project.extensions.create(NAME, GithubReleasePluginExtension)
		GithubReleaseTask.register(project)
	}
}