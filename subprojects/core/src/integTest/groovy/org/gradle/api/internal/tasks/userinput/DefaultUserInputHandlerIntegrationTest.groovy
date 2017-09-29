/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.api.internal.tasks.userinput

import org.gradle.util.ToBeImplemented
import spock.lang.Ignore
import spock.lang.Unroll

class DefaultUserInputHandlerIntegrationTest extends AbstractUserInputHandlerIntegrationTest {

    private static final String USER_INPUT_REQUEST_TASK_NAME = 'userInputRequest'
    private static final String PROMPT = 'Enter your response:'
    private static final String HELLO_WORLD_USER_INPUT = 'Hello World'
    private static final List<Boolean> VALID_BOOLEAN_CHOICES = [false, true]
    private static final String DUMMY_TASK_NAME = 'doSomething'

    @Unroll
    def "can capture user input for interactive build [daemon enabled: #daemon, rich console: #richConsole]"() {
        given:
        interactiveExecution()
        withDaemon(daemon)
        withRichConsole(richConsole)
        buildFile << userInputRequestedTask()

        when:
        executer.withTasks(USER_INPUT_REQUEST_TASK_NAME)
        def gradleHandle = executer.start()

        then:
        writeToStdInAndClose(gradleHandle, HELLO_WORLD_USER_INPUT.bytes)
        gradleHandle.waitForFinish()
        gradleHandle.standardOutput.contains(PROMPT)

        where:
        [daemon, richConsole] << [VALID_BOOLEAN_CHOICES, VALID_BOOLEAN_CHOICES].combinations()
    }

    @Unroll
    def "use of ctrl-d when capturing user input returns null [daemon enabled: #daemon, rich console: #richConsole]"() {
        given:
        interactiveExecution()
        withDaemon(daemon)
        withRichConsole(richConsole)
        buildFile << userInputRequestedTask(PROMPT, null)

        when:
        executer.withTasks(USER_INPUT_REQUEST_TASK_NAME)
        def gradleHandle = executer.start()

        then:
        writeToStdInAndClose(gradleHandle, EOF)
        gradleHandle.waitForFinish()
        gradleHandle.standardOutput.contains(PROMPT)

        where:
        [daemon, richConsole] << [VALID_BOOLEAN_CHOICES, VALID_BOOLEAN_CHOICES].combinations()
    }

    def "can capture user input when executed in parallel"() {
        given:
        interactiveExecution()
        withParallel()
        buildFile << verifyUserInput(PROMPT, HELLO_WORLD_USER_INPUT)
        buildFile << """
            subprojects {
                task $DUMMY_TASK_NAME
            }
        """
        settingsFile << "include 'a', 'b', 'c'"

        when:
        executer.withTasks(DUMMY_TASK_NAME)
        def gradleHandle = executer.start()

        then:
        writeToStdInAndClose(gradleHandle, HELLO_WORLD_USER_INPUT.bytes)
        gradleHandle.waitForFinish()
        gradleHandle.standardOutput.contains(PROMPT)
    }

    def "can capture user input from plugin"() {
        file('buildSrc/src/main/java/UserInputPlugin.java') << """
            import org.gradle.api.Project;
            import org.gradle.api.Plugin;

            import org.gradle.api.internal.project.ProjectInternal;
            import org.gradle.api.internal.tasks.userinput.UserInputHandler;
            import org.gradle.api.internal.tasks.userinput.DefaultUserInputHandler;
            import org.gradle.api.internal.tasks.userinput.InputRequest;
            import org.gradle.api.internal.tasks.userinput.DefaultInputRequest;

            public class UserInputPlugin implements Plugin<Project> {
                @Override
                public void apply(Project project) {
                    UserInputHandler userInputHandler = ((ProjectInternal) project).getServices().get(UserInputHandler.class);
                    InputRequest inputRequest = new DefaultInputRequest("$PROMPT");
                    String response = userInputHandler.getInput(inputRequest);
                    System.out.println("You entered '" + response + "'");
                }
            }
        """
        buildFile << """
            apply plugin: UserInputPlugin
            
            task $DUMMY_TASK_NAME
        """
        interactiveExecution()

        when:
        def gradleHandle = executer.withTasks(DUMMY_TASK_NAME).start()

        then:
        writeToStdInAndClose(gradleHandle, HELLO_WORLD_USER_INPUT.bytes)
        gradleHandle.waitForFinish()
        gradleHandle.standardOutput.contains(PROMPT)
        gradleHandle.standardOutput.contains("You entered '$HELLO_WORLD_USER_INPUT'")
    }

    @Ignore
    @ToBeImplemented
    def "fails gracefully if console is not interactive"() {
        given:
        buildFile << userInputRequestedTask()

        when:
        def gradleHandle = executer.withTasks(USER_INPUT_REQUEST_TASK_NAME).start()

        then:
        def failure = gradleHandle.waitForFailure()
        failure.assertHasCause('Console does not support capturing input')
    }

    static String userInputRequestedTask(String prompt = PROMPT, String expectedInput = HELLO_WORLD_USER_INPUT) {
        """
            task $USER_INPUT_REQUEST_TASK_NAME {
                doLast {
                    ${verifyUserInput(prompt, expectedInput)}
                }
            }
        """
    }

    static String verifyUserInput(String prompt, String expectedInput) {
        """
            ${createUserInputHandler()}
            ${createInputRequest(prompt)}
            def response = userInputHandler.getInput(inputRequest)
            assert response == ${formatExpectedInput(expectedInput)}
        """
    }

    static String createUserInputHandler() {
        """
            def userInputHandler = project.services.get(${UserInputHandler.class.getName()})
        """
    }

    static String createInputRequest(String prompt) {
        StringBuilder inputRequest = new StringBuilder()
        inputRequest.append("def inputRequest = new ${DefaultInputRequest.class.getName()}")
        inputRequest.append("('$prompt')")
        inputRequest.toString()
    }

    static String formatExpectedInput(String input) {
        if (input == null) {
            return 'null'
        }

        return "'$input'"
    }
}
