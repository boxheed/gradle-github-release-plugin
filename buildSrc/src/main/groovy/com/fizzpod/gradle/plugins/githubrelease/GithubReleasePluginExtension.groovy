package com.fizzpod.gradle.plugins.githubrelease

public class GithubReleasePluginExtension {
	def token
    def owner
    def repo
    def tagName
    def targetCommitish
    def releaseName
    def generateReleaseNotes = false
    def body = ""
    def draft = true
    def prerelease = false
    def releaseAssets = []
    def allowUploadToExisting = false
    def overwrite = false
    def dryRun = false
    def apiEndpoint

}
