/*
    Copyright 2019-2021 Open JumpCO

    Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
    documentation files (the "Software"), to deal in the Software without restriction, including without limitation
    the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
    and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all copies or substantial
    portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
    THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
    CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
    DEALINGS IN THE SOFTWARE.
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
