/*
 * ============================================================================
 * (C) Copyright Schalk W. Cronje 2015
 *
 * This software is licensed under the Apache License 2.0
 * See http://www.apache.org/licenses/LICENSE-2.0 for license details
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 *
 * ============================================================================
 */
package org.ysb33r.gradle.gradletest

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.tasks.TaskExecutionException
import org.ysb33r.gradle.gradletest.internal.IntegrationTestHelper
import spock.lang.Specification


/**
 * @author Schalk W. Cronjé
 */
class GradleTestIntegrationSpec extends Specification {
    static final File repoTestFile = new File(System.getProperty('GRADLETESTREPO'))
    Project project = IntegrationTestHelper.buildProject('gtis')
    File simpleTestSrcDir = new File(IntegrationTestHelper.PROJECTROOT,'build/resources/integrationTest/gradleTest')
    File simpleTestDestDir = new File(project.projectDir,'src/'+Names.DEFAULT_TASK)
    File expectedOutputDir = new File(project.buildDir,Names.DEFAULT_TASK + '/' + project.gradle.gradleVersion )
    File repoDir = new File(project.projectDir,'srcRepo').absoluteFile

    void setup() {

        assert  simpleTestSrcDir.exists()
        FileUtils.copyDirectory simpleTestSrcDir, simpleTestDestDir
        IntegrationTestHelper.createTestRepo(repoDir)

        project.allprojects {
            apply plugin: 'org.ysb33r.gradletest'

            project.repositories {
                flatDir {
                    dirs repoDir
                }
            }

            // Restrict the test to no downloading except from a local source
            gradleLocations {
                includeGradleHome = false
                searchGradleUserHome = false
                searchGvm = false
                download = false
                useGradleSite = false
                downloadToGradleUserHome = false
                search IntegrationTestHelper.CURRENT_GRADLEHOME.parentFile
            }

            dependencies {
                gradleTest ':commons-cli:1.2'
                gradleTest ':doxygen:0.2'
            }

            // Only use the current gradle version for testing
            gradleTest {
                versions gradle.gradleVersion
            }
        }
    }

    def "Two simple gradleTests; one will pass and one will fail"() {

        when: 'Evaluation has been completed'
        project.evaluate()

        then:
        project.tasks.gradleTest.versions.size()
        project.tasks.gradleTest.sourceDir == simpleTestDestDir
        project.tasks.gradleTest.outputDirs.contains( expectedOutputDir )

        when: 'The tasks is executed'
        project.tasks.gradleTest.execute()

        then: "We expect this ugly exception, but need to fix it to be more like the 'test' task "
        thrown(TaskExecutionException)

        when:
        def results = project.tasks.gradleTest.testResults

        then:
        project.tasks.gradleTest.didWork
        results.size() == 2

        and:
        new File(expectedOutputDir,'simpleTest').exists()
        results[1].passed
        results[1].gradleVersion == project.gradle.gradleVersion
        results[1].testName == 'simpleTest'

        and:
        new File(expectedOutputDir,'failureTest').exists()
        !results[0].passed
        results[0].gradleVersion == project.gradle.gradleVersion
        results[0].testName == 'failureTest'
    }

}