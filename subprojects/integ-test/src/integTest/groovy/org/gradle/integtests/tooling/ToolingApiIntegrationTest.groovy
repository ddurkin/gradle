/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.integtests.tooling

import org.gradle.tooling.GradleConnector
import org.gradle.tooling.model.Project
import org.gradle.util.GradleVersion

class ToolingApiIntegrationTest extends ToolingApiSpecification {
    def canUseToolingApiWithoutSpecifyingADistributionToUse() {
        def projectDir = dist.testDir
        projectDir.file('build.gradle').text = "assert gradle.gradleVersion == '${new GradleVersion().version}'"

        when:
        GradleConnector connector = GradleConnector.newConnector()
        Project model = withConnection(connector) { connection -> connection.getModel(Project.class) }

        then:
        model != null
    }

    def canSpecifyAGradleInstallationToUse() {
        def projectDir = dist.testDir
        projectDir.file('build.gradle').text = "assert gradle.gradleVersion == '${new GradleVersion().version}'"

        when:
        GradleConnector connector = GradleConnector.newConnector()
        connector.useInstallation(dist.gradleHomeDir)
        Project model = withConnection(connector) { connection -> connection.getModel(Project.class) }

        then:
        model != null
    }

    def canSpecifyAGradleDistributionToUse() {
        def projectDir = dist.testDir
        projectDir.file('build.gradle').text = "assert gradle.gradleVersion == '${new GradleVersion().version}'"

        when:
        GradleConnector connector = GradleConnector.newConnector()
        connector.useDistribution(dist.binDistribution.toURI())
        Project model = withConnection(connector) { connection -> connection.getModel(Project.class) }

        then:
        model != null
    }

    def canSpecifyAGradleVersionToUse() {
        def projectDir = dist.testDir
        projectDir.file('build.gradle').text = "assert gradle.gradleVersion == '${new GradleVersion().version}'"

        when:
        GradleConnector connector = GradleConnector.newConnector()
        connector.useGradleVersion(new GradleVersion().version)
        Project model = withConnection(connector) { connection -> connection.getModel(Project.class) }

        then:
        model != null
    }
}