package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UciMoveConverterTest {

    @Test
    fun `uciSquareToPosition converts correctly`() {
        // a1 = row 7, col 0
        assertEquals(Pair(7, 0), UciMoveConverter.uciSquareToPosition("a1"))
        // h8 = row 0, col 7
        assertEquals(Pair(0, 7), UciMoveConverter.uciSquareToPosition("h8"))
        // e2 = row 6, col 4
        assertEquals(Pair(6, 4), UciMoveConverter.uciSquareToPosition("e2"))
        // e4 = row 4, col 4
        assertEquals(Pair(4, 4), UciMoveConverter.uciSquareToPosition("e4"))
        // d7 = row 1, col 3
        assertEquals(Pair(1, 3), UciMoveConverter.uciSquareToPosition("d7"))
    }

    @Test
    fun `positionToUciSquare converts correctly`() {
        assertEquals("a1", UciMoveConverter.positionToUciSquare(Pair(7, 0)))
        assertEquals("h8", UciMoveConverter.positionToUciSquare(Pair(0, 7)))
        assertEquals("e2", UciMoveConverter.positionToUciSquare(Pair(6, 4)))
        assertEquals("e4", UciMoveConverter.positionToUciSquare(Pair(4, 4)))
        assertEquals("d7", UciMoveConverter.positionToUciSquare(Pair(1, 3)))
    }

    @Test
    fun `round trip conversion is identity`() {
        // Test all 64 squares
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                val pos = Pair(row, col)
                val uci = UciMoveConverter.positionToUciSquare(pos)
                val back = UciMoveConverter.uciSquareToPosition(uci)
                assertEquals("Round trip failed for $pos -> $uci", pos, back)
            }
        }
    }

    @Test
    fun `parseUciMove parses standard moves`() {
        val (from, to) = UciMoveConverter.parseUciMove("e2e4")
        assertEquals(Pair(6, 4), from)  // e2
        assertEquals(Pair(4, 4), to)    // e4
    }

    @Test
    fun `parseUciMove parses promotion moves`() {
        val (from, to) = UciMoveConverter.parseUciMove("e7e8q")
        assertEquals(Pair(1, 4), from)  // e7
        assertEquals(Pair(0, 4), to)    // e8
    }

    @Test
    fun `uciMoveToAppMove finds correct piece`() {
        val positions = listOf(Pair(7, 4), Pair(6, 4))  // king at e1, pawn at e2
        val result = UciMoveConverter.uciMoveToAppMove("e2e4", positions)

        assertNotNull(result)
        assertEquals(Pair(4, 4), result!!.first)  // target: e4
        assertEquals(1, result.second)             // piece index: 1 (pawn at e2)
    }

    @Test
    fun `uciMoveToAppMove returns null for missing piece`() {
        val positions = listOf(Pair(7, 4))  // only king at e1
        val result = UciMoveConverter.uciMoveToAppMove("e2e4", positions)

        assertNull("Should return null when no piece at source square", result)
    }

    @Test
    fun `appMoveToUci produces correct UCI string`() {
        val uci = UciMoveConverter.appMoveToUci(Pair(6, 4), Pair(4, 4))
        assertEquals("e2e4", uci)

        val uci2 = UciMoveConverter.appMoveToUci(Pair(0, 1), Pair(2, 0))
        assertEquals("b8a6", uci2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `uciSquareToPosition rejects invalid file`() {
        UciMoveConverter.uciSquareToPosition("i1")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `uciSquareToPosition rejects invalid rank`() {
        UciMoveConverter.uciSquareToPosition("a9")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `uciSquareToPosition rejects wrong length`() {
        UciMoveConverter.uciSquareToPosition("e")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseUciMove rejects too short`() {
        UciMoveConverter.parseUciMove("e2")
    }
}
