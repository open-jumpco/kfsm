/*
 * Copyright (c) 2020-2021. Open JumpCO
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package io.jumpco.open.kfsm.async

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

actual class AsyncTimer<S, E, C, A, R> actual constructor(
    val parentFsm: AsyncStateMapInstance<S, E, C, A, R>,
    val context: C,
    val arg: A?,
    val definition: AsyncTimerDefinition<S, E, C, A, R>
) {
    var active: Boolean
    val timer: Job

    init {
        active = true
        timer = GlobalScope.launch {
            delay(definition.timeout)
            trigger()
        }
    }

    actual fun cancel() {
        active = false
    }

    actual suspend fun trigger() {
        if (active) {
            val defaultTransition = definition.rule.transition
            definition.rule.findGuard(context, arg)?.apply {
                parentFsm.execute(this, arg)
            } ?: run {
                if (defaultTransition != null) {
                    parentFsm.execute(defaultTransition, arg)
                }
            }
        }
    }
}
