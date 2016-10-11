/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.internal.operations

import org.gradle.integtests.fixtures.AbstractIntegrationSpec

class PluginApplicationBuildOperationTest extends AbstractIntegrationSpec {

    def "plugin application build operation events are emitted"() {
        given:
        settingsFile << soutBuildOperationsListener()
        buildFile << '''
        apply plugin: 'base'
        '''.stripIndent()

        when:
        run 'help', '-q'

        then:
        output.count('STARTED  Apply plugin: org.gradle.api.plugins.BasePlugin') == 1
        output.count('FINISHED Apply plugin: org.gradle.api.plugins.BasePlugin') == 1
    }

    private String soutBuildOperationsListener() {
        return '''
        import org.gradle.internal.progress.BuildOperationInternal
        import org.gradle.internal.progress.InternalBuildListener
        import org.gradle.internal.progress.OperationResult
        import org.gradle.internal.progress.OperationStartEvent

        class SoutBuildOperationsListener implements InternalBuildListener {
            @Override
            void started(BuildOperationInternal buildOperation, OperationStartEvent startEvent) {
                println "STARTED  $buildOperation.displayName @$startEvent.startTime"
            }

            @Override
            void finished(BuildOperationInternal buildOperation, OperationResult finishEvent) {
                println "FINISHED $buildOperation.displayName @$finishEvent.endTime ${finishEvent.failure ? finishEvent.failure.message : ''}"
            }
        }

        gradle.addListener(new SoutBuildOperationsListener())
        '''.stripIndent()
    }
}
