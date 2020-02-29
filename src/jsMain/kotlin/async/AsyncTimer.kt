/*
 * Copyright (c) 2020. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm.async

import kotlin.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

actual class AsyncTimer<S, E, C, A, R> actual constructor(
    val parentFsm: AsyncStateMapInstance<S, E, C, A, R>,
    val context: C,
    val arg: A?,
    val definition: AsyncTimerDefinition<S, E, C, A, R>
) {
    var active: Boolean

    init {
        active = true
        window.setTimeout({
            GlobalScope.launch {
                trigger()
            }
        }, definition.timeout.toInt())
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
