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

package org.gradle.language.swift;

import org.gradle.api.Incubating;
import org.gradle.api.tasks.Nested;
import org.gradle.nativeplatform.OperatingSystemFamily;
import org.gradle.nativeplatform.platform.NativePlatform;

/**
 * A target platform for building Swift binaries.
 *
 * @since 4.5
 */
@Incubating
public interface SwiftPlatform extends NativePlatform {
    /**
     * The operating system family being targeted.
     *
     * @since 4.8
     */
    @Nested
    OperatingSystemFamily getOperatingSystemFamily();
}
