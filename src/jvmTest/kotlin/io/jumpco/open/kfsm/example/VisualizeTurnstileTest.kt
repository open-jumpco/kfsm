/*
 * Copyright (c) 2021. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm.example

import io.jumpco.open.kfsm.viz.Parser
import io.jumpco.open.kfsm.viz.Visualization
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
        val visualisation = Parser.parseStateMachine("TurnstileFSM", File("src/commonTest/kotlin/io/jumpco/open/kfsm/example/TurnstileTypes.kt"))
        println(visualisation)
        File("generated", "turnstile.plantuml").writeText(Visualization.plantUml(visualisation))
    }

    @Test
    fun produceVisualizationPayingTurnstile() {
        println("== PayingTurnstile")
        val visualization = Parser.parseStateMachine("PayingTurnstileFSM", File("src/commonTest/kotlin/io/jumpco/open/kfsm/example/PayingTurnstileTypes.kt"))
        println(visualization)
        File("generated", "paying-turnstile.plantuml").writeText(Visualization.plantUml(visualization))
    }

    @Test
    fun produceVisualizationSecureTurnstile() {
        println("== SecureTurnstile")
        val visualization = Parser.parseStateMachine("SecureTurnstileFSM", File("src/commonTest/kotlin/io/jumpco/open/kfsm/example/SecureTurnstile.kt"))
        println(visualization)
        File("generated", "secure-turnstile.plantuml").writeText(Visualization.plantUml(visualization))
    }

    @Test
    fun produceVisualizationPacketReader() {
        println("== PacketReader")
        val visualization = Parser.parseStateMachine("PacketReaderFSM", File("src/jvmTest/kotlin/io/jumpco/open/kfsm/example/PacketReaderTests.kt"))
        println(visualization)
        File("generated", "packet-reader.plantuml").writeText(Visualization.plantUml(visualization))
    }
}
