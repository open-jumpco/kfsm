/*
 * Copyright (c) 2020. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.example.kfsm


import io.jumpco.open.kfsm.PacketReaderFSM
import io.jumpco.open.kfsm.PayingTurnstileFSM
import io.jumpco.open.kfsm.SecureTurnstileFSM
import io.jumpco.open.kfsm.TurnstileFSM
import io.jumpco.open.kfsm.viz.plantUml
import io.jumpco.open.kfsm.viz.visualize
import org.junit.Before
import org.junit.Test
import java.io.File

class VisualizeTurnstileTest {
    @Before
    fun setup() {
        val generated = File("generated")
        if (generated.exists() && !generated.isDirectory) {
            error("Expected generated to be a directory")
        } else if (!generated.exists()) {
            generated.mkdirs()
        }
    }

    @Test
    fun produceVisualizationTurnstileFSM() {
        println("== TurnStile")
        val visualization =
            visualize(TurnstileFSM.definition)
        visualization.forEach { v ->
            println("$v")
        }
        File("generated", "turnstile.plantuml").writeText(plantUml(visualization))
    }

    @Test
    fun produceVisualizationPayingTurnstile() {
        println("== PayingTurnstile")
        val visualization =
            visualize(PayingTurnstileFSM.definition)
        visualization.forEach { v ->
            println("$v")
        }
        File("generated", "paying-turnstile.plantuml").writeText(plantUml(visualization))
    }

    @Test
    fun produceVisualizationSecureTurnstile() {
        println("== SecureTurnstile")
        val visualization =
            visualize(SecureTurnstileFSM.definition)
        visualization.forEach { v ->
            println("$v")
        }
        File("generated", "secure-turnstile.plantuml").writeText(plantUml(visualization))
    }

    @Test
    fun produceVisualizationPacketReader() {
        println("== PacketReader")
        val visualization =
            visualize(PacketReaderFSM.definition)
        visualization.forEach { v ->
            println("$v")
        }
        File("generated", "packet-reader.plantuml").writeText(plantUml(visualization))
    }
}
