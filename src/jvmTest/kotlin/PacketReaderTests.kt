/*
 * Copyright (c) 2019. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import java.io.ByteArrayOutputStream
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// tag::context[]
class Block {
    val byteArrayOutputStream = ByteArrayOutputStream(32)
    fun addByte(byte: Int) {
        byteArrayOutputStream.write(byte)
    }
}

interface ProtocolHandler {
    fun sendNACK()
    fun sendACK()
}

interface PacketHandler : ProtocolHandler {
    val checksumValid: Boolean
    fun print()
    fun addField()
    fun endField()
    fun addByte(byte: Int)
    fun addChecksum(byte: Int)
    fun checksum()
}

class ProtocolSender : ProtocolHandler {
    override fun sendNACK() {
        println("NACK")
    }

    override fun sendACK() {
        println("ACK")
    }
}

class Packet(private val protocolHandler: ProtocolHandler) : PacketHandler,
    ProtocolHandler by protocolHandler {
    val fields = mutableListOf<ByteArray>()
    private var currentField: Block? = null
    private var _checksumValid: Boolean = false

    override val checksumValid: Boolean
        get() = _checksumValid
    private val checkSum = Block()

    override fun print() {
        println("Checksum:$checksumValid:Fields:${fields.size}")
        fields.forEachIndexed { index, bytes ->
            print("FLD:$index:")
            bytes.forEach { byte ->
                val hex = byte.toString(16).padStart(2, '0')
                print(" $hex")
            }
            println()
        }
        println()
    }

    override fun addField() {
        currentField = Block()
    }

    override fun endField() {
        val field = currentField
        require(field != null) { "expected currentField to have a value" }
        fields.add(field.byteArrayOutputStream.toByteArray())
        currentField = null
    }

    override fun addByte(byte: Int) {
        val field = currentField
        require(field != null) { "expected currentField to have a value" }
        field.addByte(byte)
    }

    override fun addChecksum(byte: Int) {
        checkSum.addByte(byte)
    }

    override fun checksum() {
        require(checkSum.byteArrayOutputStream.size() > 0)
        val checksumBytes = checkSum.byteArrayOutputStream.toByteArray()
        _checksumValid = if (checksumBytes.size == fields.size) {
            checksumBytes.mapIndexed { index, cs ->
                cs == fields[index][0]
            }.reduce { a, b -> a && b }
        } else {
            false
        }
    }
}

// end::context[]
// tag::states-events[]
class CharacterConstants {
    companion object {
        const val SOH = 0x01
        const val STX = 0x02
        const val ETX = 0x03
        const val EOT = 0x04
        const val ACK = 0x06
        const val NAK = 0x15
        const val ESC = 0x1b
    }
}

/**
 * CTRL :
 * BYTE everything else
 */
enum class ReaderEvents {
    BYTE, // everything else
    CTRL, // SOH, EOT, STX, ETX, ACK, NAK
    ESC // ESC = 0x1B
}

enum class ReaderStates {
    START,
    RCVPCKT,
    RCVDATA,
    RCVESC,
    RCVCHK,
    RCVCHKESC,
    CHKSUM,
    END
}

// end::states-events[]
// tag::packaged[]
class PacketReaderFSM(private val packetHandler: PacketHandler) {
    companion object {

        private val definition = stateMachine(
            ReaderStates.values().toSet(),
            ReaderEvents.values().toSet(),
            PacketHandler::class,
            Int::class
        ) {
            defaultInitialState = ReaderStates.START
            default {
                onEvent(ReaderEvents.BYTE to ReaderStates.END) {
                    sendNACK()
                }
                onEvent(ReaderEvents.CTRL to ReaderStates.END) {
                    sendNACK()
                }
                onEvent(ReaderEvents.ESC to ReaderStates.END) {
                    sendNACK()
                }
            }
            whenState(ReaderStates.START) {
                onEvent(
                    ReaderEvents.CTRL to ReaderStates.RCVPCKT,
                    guard = { byte -> byte == CharacterConstants.SOH }) {}
            }
            whenState(ReaderStates.RCVPCKT) {
                onEvent(ReaderEvents.CTRL to ReaderStates.RCVDATA, guard = { byte -> byte == CharacterConstants.STX }) {
                    addField()
                }
                onEvent(ReaderEvents.BYTE to ReaderStates.RCVCHK) { byte ->
                    require(byte != null)
                    addChecksum(byte)
                }
            }
            whenState(ReaderStates.RCVDATA) {
                onEvent(ReaderEvents.BYTE) { byte ->
                    require(byte != null)
                    addByte(byte)
                }
                onEvent(ReaderEvents.CTRL to ReaderStates.RCVPCKT, guard = { byte -> byte == CharacterConstants.ETX }) {
                    endField()
                }
                onEvent(ReaderEvents.ESC to ReaderStates.RCVESC) {}
            }
            whenState(ReaderStates.RCVESC) {
                onEvent(ReaderEvents.ESC to ReaderStates.RCVDATA) {
                    addByte(CharacterConstants.ESC)
                }
                onEvent(ReaderEvents.CTRL to ReaderStates.RCVDATA) { byte ->
                    require(byte != null)
                    addByte(byte)
                }
            }
            whenState(ReaderStates.RCVCHK) {
                onEvent(ReaderEvents.BYTE) { byte ->
                    require(byte != null)
                    addChecksum(byte)
                }
                onEvent(ReaderEvents.ESC to ReaderStates.RCVCHKESC) {}
                onEvent(ReaderEvents.CTRL to ReaderStates.CHKSUM, guard = { byte -> byte == CharacterConstants.EOT }) {
                    checksum()
                }
            }
            whenState(ReaderStates.CHKSUM) {
                automatic(ReaderStates.END, guard = { !checksumValid }) {
                    sendNACK()
                }
                automatic(ReaderStates.END, guard = { checksumValid }) {
                    sendACK()
                }
            }
            whenState(ReaderStates.RCVCHKESC) {
                onEvent(ReaderEvents.ESC to ReaderStates.RCVCHK) {
                    addChecksum(CharacterConstants.ESC)
                }
                onEvent(ReaderEvents.CTRL to ReaderStates.RCVCHK) { byte ->
                    require(byte != null)
                    addChecksum(byte)
                }
            }
        }.build()
    }

    private val fsm = definition.create(packetHandler)
    fun receiveByte(byte: Int) {
        when (byte) {
            CharacterConstants.ESC -> fsm.sendEvent(ReaderEvents.ESC, CharacterConstants.ESC)
            CharacterConstants.SOH,
            CharacterConstants.EOT,
            CharacterConstants.ETX,
            CharacterConstants.STX,
            CharacterConstants.ACK,
            CharacterConstants.NAK -> fsm.sendEvent(ReaderEvents.CTRL, byte)
            else -> fsm.sendEvent(ReaderEvents.BYTE, byte)
        }
    }
}
// end::packaged[]

// tag::tests[]
class PacketReaderTests {
    @Test
    fun `test reader expect ACK`() {
        val protocolHandler = mockk<ProtocolHandler>()
        every { protocolHandler.sendACK() } just Runs
        val packetReader = Packet(protocolHandler)
        val fsm = PacketReaderFSM(packetReader)
        val stream =
            listOf(
                CharacterConstants.SOH,
                CharacterConstants.STX,
                'A'.toInt(),
                'B'.toInt(),
                'C'.toInt(),
                CharacterConstants.ETX,
                'A'.toInt(),
                CharacterConstants.EOT
            )
        stream.forEach { byte ->
            fsm.receiveByte(byte)
        }
        packetReader.print()
        verify { protocolHandler.sendACK() }
        assertTrue { packetReader.checksumValid }
    }

    @Test
    fun `test reader ESC expect ACK`() {
        val protocolHandler = mockk<ProtocolHandler>()
        every { protocolHandler.sendACK() } just Runs
        val packetReader = Packet(protocolHandler)
        val fsm = PacketReaderFSM(packetReader)
        val stream =
            listOf(
                CharacterConstants.SOH,
                CharacterConstants.STX,
                'A'.toInt(),
                CharacterConstants.ESC,
                CharacterConstants.EOT,
                'C'.toInt(),
                CharacterConstants.ETX,
                'A'.toInt(),
                CharacterConstants.EOT
            )
        stream.forEach { byte ->
            fsm.receiveByte(byte)
        }
        packetReader.print()
        verify { protocolHandler.sendACK() }
        assertTrue { packetReader.checksumValid }
        assertTrue { packetReader.fields.size == 1 }
        assertTrue { packetReader.fields[0].size == 3 }
        assertTrue { packetReader.fields[0][1].toInt() == CharacterConstants.EOT }
    }

    @Test
    fun `test reader ESC expect NACK`() {
        val protocolHandler = mockk<ProtocolHandler>()
        every { protocolHandler.sendNACK() } just Runs
        val packetReader = Packet(protocolHandler)
        val fsm = PacketReaderFSM(packetReader)
        val stream =
            listOf(
                CharacterConstants.SOH,
                CharacterConstants.STX,
                'A'.toInt(),
                CharacterConstants.ESC,
                'C'.toInt(),
                CharacterConstants.ETX,
                'A'.toInt(),
                CharacterConstants.EOT
            )
        stream.forEach { byte ->
            fsm.receiveByte(byte)
        }
        packetReader.print()
        verify { protocolHandler.sendNACK() }
        assertFalse { packetReader.checksumValid }
        assertTrue { packetReader.fields.isEmpty() }
    }

    @Test
    fun `test reader expect NACK`() {
        val protocolHandler = mockk<ProtocolHandler>()
        every { protocolHandler.sendNACK() } just Runs
        val packetReader = Packet(protocolHandler)
        val fsm = PacketReaderFSM(packetReader)
        val stream =
            listOf(
                CharacterConstants.SOH,
                CharacterConstants.STX,
                'A'.toInt(),
                'B'.toInt(),
                'C'.toInt(),
                CharacterConstants.ETX,
                'B'.toInt(),
                CharacterConstants.EOT
            )
        stream.forEach { byte ->
            fsm.receiveByte(byte)
        }
        packetReader.print()
        verify { protocolHandler.sendNACK() }
        assertFalse { packetReader.checksumValid }
    }

    @Test
    fun `test reader multiple fields expect ACK`() {
        val protocolHandler = mockk<ProtocolHandler>()
        every { protocolHandler.sendACK() } just Runs
        val packetReader = Packet(protocolHandler)
        val fsm = PacketReaderFSM(packetReader)
        val stream =
            listOf(
                CharacterConstants.SOH,
                CharacterConstants.STX,
                'A'.toInt(),
                'B'.toInt(),
                'C'.toInt(),
                CharacterConstants.ETX,
                CharacterConstants.STX,
                'D'.toInt(),
                'E'.toInt(),
                'F'.toInt(),
                CharacterConstants.ETX,
                'A'.toInt(),
                'D'.toInt(),
                CharacterConstants.EOT
            )
        stream.forEach { byte ->
            fsm.receiveByte(byte)
        }
        packetReader.print()
        verify { protocolHandler.sendACK() }
        assertTrue { packetReader.checksumValid }
        assertTrue { packetReader.fields.size == 2 }
    }
}
// end::tests[]
