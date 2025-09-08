/* (C) 2024-2025 */
/* SPDX-License-Identifier: Apache-2.0 */
package com.fizzpod.gradle.plugins.githubrelease

public class GithubReleasePluginExtension {
	def token = ""
    def authorization = ""
    def owner = ""
    def repo = ""
    def tagName = ""
    def targetCommitish = ""
    def releaseName = ""
    def generateReleaseNotes = false
    def body = ""
    def draft = true
    def prerelease = false
    def releaseAssets = []
    def allowUploadToExisting = false
    def overwrite = false
    def dryRun = false
    def apiEndpoint = "https://api.github.com"
    def login = ""
    def github = null
}
