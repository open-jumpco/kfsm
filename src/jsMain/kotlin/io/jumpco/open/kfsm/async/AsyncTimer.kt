/*
 * Copyright (c) 2021. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm.async

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

actual class AsyncTimer<S, E, C, A, R> actual constructor(
    val parentFsm: AsyncStateMapInstance<S, E, C, A, R>,
    val context: C,
    val arg: A?,
    val definition: AsyncTimerDefinition<S, E, C, A, R>
) {
    var active: Boolean
    private val timer: Job

    init {
        active = true
        timer = CoroutineScope(Dispatchers.Default).async {
            delay(context.(definition.timeout)())
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
