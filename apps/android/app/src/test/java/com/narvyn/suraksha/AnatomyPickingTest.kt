package com.narvyn.suraksha

import org.junit.Assert.*
import org.junit.Test

/** Geometric edge cases matter: screen-space approximations must not select anatomy behind the ray. */
class AnatomyPickingTest {
    private fun triangle(z: Float, reverse: Boolean = false): FloatArray {
        val points = listOf(floatArrayOf(-1f, -1f, z), floatArrayOf(1f, -1f, z), floatArrayOf(0f, 1f, z))
        return (if (reverse) points.reversed() else points).flatMap { it.toList() + listOf(0f, 0f, 1f) }.toFloatArray()
    }
    private val origin = floatArrayOf(0f, 0f, 4f)
    private val forward = floatArrayOf(0f, 0f, -1f)
    private fun bounds(z: Float) = floatArrayOf(-1f, -1f, z, 1f, 1f, z)

    @Test fun selectsNearestSurfaceRegardlessOfTriangleOrder() {
        val meshes = triangle(0f) + triangle(2f)
        val box = floatArrayOf(-1f, -1f, 0f, 1f, 1f, 2f)
        assertEquals(2f, AnatomyPicking.distance(meshes, box, origin, forward)!!, 0.0001f)
        assertEquals(2f, AnatomyPicking.distance(triangle(2f) + triangle(0f), box, origin, forward)!!, 0.0001f)
    }

    @Test fun rejectsTrianglesBehindOriginAndRaysMissingActualTriangle() {
        assertNull(AnatomyPicking.distance(triangle(0f), bounds(0f), origin, floatArrayOf(0f, 0f, 1f)))
        // Inside the bounding box, but outside the triangular surface.
        assertNull(AnatomyPicking.distance(triangle(0f), bounds(0f), floatArrayOf(.9f, .9f, 4f), forward))
    }

    @Test fun parallelRaysOutsideBoundsAndCoplanarRaysDoNotSelect() {
        assertNull(AnatomyPicking.distance(triangle(0f), bounds(0f), floatArrayOf(2f, 0f, 4f), forward))
        assertNull(AnatomyPicking.distance(triangle(0f), bounds(0f), floatArrayOf(0f, 0f, 0f), floatArrayOf(1f, 0f, 0f)))
    }

    @Test fun permitsBoundaryHitsAndBothWindings() {
        assertEquals(4f, AnatomyPicking.distance(triangle(0f), bounds(0f), floatArrayOf(0f, 1f, 4f), forward)!!, 0.0001f)
        assertEquals(4f, AnatomyPicking.distance(triangle(0f, true), bounds(0f), origin, forward)!!, 0.0001f)
    }

    @Test fun rejectsNonFiniteAndZeroRays() {
        for (bad in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertNull(AnatomyPicking.distance(triangle(0f), bounds(0f), floatArrayOf(bad, 0f, 4f), forward))
            assertNull(AnatomyPicking.distance(triangle(0f), bounds(0f), origin, floatArrayOf(0f, bad, -1f)))
        }
        assertNull(AnatomyPicking.distance(triangle(0f), bounds(0f), origin, floatArrayOf(0f, 0f, 0f)))
    }

    @Test fun nonUnitDirectionPreservesRayParameter() {
        // Inverse model transforms may alter ray length. The hit point must still be correct.
        val direction = floatArrayOf(0f, 0f, -.5f)
        val distance = AnatomyPicking.distance(triangle(0f), bounds(0f), origin, direction)!!
        assertEquals(0f, origin[2] + distance * direction[2], .0001f)
    }
}
