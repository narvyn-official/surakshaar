package com.narvyn.suraksha

/** Illustrative display materials only. These colours never classify a person's skin or condition. */
internal data class AnatomySkinTone(val id:Int,val label:String,val colour:FloatArray)
internal data class AnatomySurfaceMaterial(val roughness:Float,val specular:Float,val softness:Float,val variation:Float)

internal object AnatomyMaterials {
    const val DEFAULT_SKIN_TONE=3
    val skinTones=listOf(
        AnatomySkinTone(0,"Light",floatArrayOf(.88f,.68f,.55f)),
        AnatomySkinTone(1,"Light-medium",floatArrayOf(.76f,.52f,.36f)),
        AnatomySkinTone(2,"Medium",floatArrayOf(.66f,.42f,.27f)),
        AnatomySkinTone(3,"Medium brown",floatArrayOf(.55f,.33f,.21f)),
        AnatomySkinTone(4,"Deep brown",floatArrayOf(.40f,.23f,.15f)),
        AnatomySkinTone(5,"Dark",floatArrayOf(.27f,.15f,.105f))
    )
    private val skin=AnatomySurfaceMaterial(.72f,.16f,.22f,.025f)
    private val bone=AnatomySurfaceMaterial(.74f,.12f,.04f,.018f)
    private val muscle=AnatomySurfaceMaterial(.56f,.22f,.16f,.018f)
    private val organ=AnatomySurfaceMaterial(.46f,.28f,.18f,.015f)
    private val lung=AnatomySurfaceMaterial(.69f,.16f,.22f,.025f)
    private val brain=AnatomySurfaceMaterial(.61f,.20f,.14f,.015f)
    private val polymer=AnatomySurfaceMaterial(.36f,.34f,0f,.006f)
    private val rubber=AnatomySurfaceMaterial(.86f,.08f,0f,.012f)
    private val display=AnatomySurfaceMaterial(.20f,.46f,0f,0f)
    fun forPart(part:AnatomyPart):AnatomySurfaceMaterial=when {
        part.layer=="skin"->skin
        part.layer=="skeleton"->bone
        part.layer=="muscles"->muscle
        part.id=="FMA7309"||part.id=="FMA7310"->lung
        part.id=="FMA50801"||part.id=="FMA7647"->brain
        part.id==FirstAidEquipment.DISPLAY->display
        part.layer=="equipment"&&("handle" in part.id||part.id==FirstAidEquipment.POWER)->rubber
        part.layer=="equipment"->polymer
        else->organ
    }
}
