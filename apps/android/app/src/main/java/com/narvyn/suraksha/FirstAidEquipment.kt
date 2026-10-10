package com.narvyn.suraksha

import kotlin.math.*

/** Original generic AED trainer illustration, not a manufacturer's device or pad-position guide. */
internal object FirstAidEquipment {
    const val POWER = "trainer-aed-power"
    const val DISPLAY = "trainer-aed-display"
    val parts: List<AnatomyPart> by lazy {
        listOf(
            box("trainer-aed-case", "Virtual AED casing", .59f, 1.06f, -.06f, .28f, .36f, .10f, floatArrayOf(.20f, .28f, .36f)),
            box("trainer-aed-face", "Virtual AED face", .59f, 1.06f, -.003f, .25f, .33f, .016f, floatArrayOf(.90f, .93f, .95f)),
            box(DISPLAY, "Authored AED prompt", .59f, 1.14f, .009f, .17f, .08f, .014f, floatArrayOf(.10f, .32f, .33f)),
            disk(POWER, "Virtual AED power control", .54f, 1.02f, .022f, .032f, floatArrayOf(.06f, .55f, .34f)),
            box("trainer-aed-pad-packet", "Illustrative pad packet", .66f, .82f, -.025f, .17f, .13f, .02f, floatArrayOf(.75f, .83f, .88f)),
            box("trainer-aed-handle-left", "Virtual AED handle", .52f, 1.27f, -.05f, .024f, .07f, .04f, floatArrayOf(.20f, .28f, .36f)),
            box("trainer-aed-handle-right", "Virtual AED handle", .66f, 1.27f, -.05f, .024f, .07f, .04f, floatArrayOf(.20f, .28f, .36f)),
            box("trainer-aed-handle-top", "Virtual AED handle", .59f, 1.30f, -.05f, .16f, .024f, .04f, floatArrayOf(.20f, .28f, .36f))
        ) + buildList {
            val seam=floatArrayOf(.35f,.43f,.47f);val foil=floatArrayOf(.86f,.89f,.88f)
            // Packet heat-seal ridges and peel tab; no manufacturer pad-placement diagram.
            for(i in 0..12)add(box("trainer-aed-packet-seal-$i","Pad packet sealed edge",.585f+i*.012f,.878f,-.012f,.004f,.008f,.003f,seam))
            add(box("trainer-aed-peel-tab","Pad packet peel tab",.735f,.862f,-.010f,.024f,.026f,.004f,foil))
            for(i in 0..3)add(box("trainer-aed-slip-line-$i","Instruction slip",.66f,.85f-i*.016f,-.013f,.105f,.002f,.002f,seam))
            for(x in listOf(.48f,.70f))for(y in listOf(.92f,1.20f))add(disk("trainer-aed-screw-$x-$y","Casing fastener",x,y,.015f,.005f,seam))
            for(i in 0..6)add(box("trainer-aed-speaker-$i","Speaker grille",.65f,.99f+i*.008f,.018f,.064f,.002f,.002f,seam))
            add(box("trainer-aed-connector","Trainer connector",.69f,1.245f,-.008f,.035f,.020f,.024f,seam))
        }
    }
    private fun part(id: String, name: String, colour: FloatArray, vertices: List<Float>): AnatomyPart {
        val data = vertices.toFloatArray()
        // Rest the trainer case on the same source-floor plane as the supine body.
        for(i in data.indices step 6)data[i+2]-=.035f
        val bounds = FloatArray(6)
        for (axis in 0..2) {
            bounds[axis] = (data.indices step 6).minOf { data[it + axis] }
            bounds[axis + 3] = (data.indices step 6).maxOf { data[it + axis] }
        }
        return AnatomyPart(id, name, "equipment", "Generic educational trainer. Follow the instructions of the actual device used in approved training.", bounds, colour, data)
    }
    private fun box(id: String, name: String, x: Float, y: Float, z: Float, w: Float, h: Float, d: Float, colour: FloatArray): AnatomyPart {
        val vertices = mutableListOf<Float>()
        val corners = listOf(floatArrayOf(x-w/2,y-h/2,z-d/2),floatArrayOf(x+w/2,y-h/2,z-d/2),floatArrayOf(x+w/2,y+h/2,z-d/2),floatArrayOf(x-w/2,y+h/2,z-d/2),floatArrayOf(x-w/2,y-h/2,z+d/2),floatArrayOf(x+w/2,y-h/2,z+d/2),floatArrayOf(x+w/2,y+h/2,z+d/2),floatArrayOf(x-w/2,y+h/2,z+d/2))
        val faces = listOf(intArrayOf(4,5,6,7),intArrayOf(1,0,3,2),intArrayOf(0,4,7,3),intArrayOf(5,1,2,6),intArrayOf(3,7,6,2),intArrayOf(0,1,5,4))
        val normals = listOf(floatArrayOf(0f,0f,1f),floatArrayOf(0f,0f,-1f),floatArrayOf(-1f,0f,0f),floatArrayOf(1f,0f,0f),floatArrayOf(0f,1f,0f),floatArrayOf(0f,-1f,0f))
        faces.forEachIndexed { i, f -> for (j in intArrayOf(0,1,2,0,2,3)) { vertices.addAll(corners[f[j]].toList()); vertices.addAll(normals[i].toList()) } }
        return part(id,name,colour,vertices)
    }
    private fun disk(id: String, name: String, x: Float, y: Float, z: Float, radius: Float, colour: FloatArray): AnatomyPart {
        val vertices = mutableListOf<Float>()
        for (i in 0 until 32) {
            val a=i*2*PI/32;val b=(i+1)*2*PI/32
            for (p in listOf(floatArrayOf(x,y,z),floatArrayOf(x+cos(a).toFloat()*radius,y+sin(a).toFloat()*radius,z),floatArrayOf(x+cos(b).toFloat()*radius,y+sin(b).toFloat()*radius,z))) {
                vertices.addAll(p.toList()); vertices.addAll(listOf(0f,0f,1f))
            }
        }
        return part(id,name,colour,vertices)
    }
}
