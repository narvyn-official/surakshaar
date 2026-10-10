package com.narvyn.suraksha
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SpatialAssetContractTest {
 private fun json(path:String)=JSONObject(File("../../../$path").readText())
 @Test fun everyLearningStepHasAnExplicitExistingScene(){
  val plans=json("content/spatial/catalog.json").getJSONObject("steps");val equipment=WorldEquipment()
  val anatomy=json("content/anatomy/manifest.json").getJSONArray("parts").objects().map{it.getString("id")}.toSet();val keys=mutableSetOf<String>()
  for(s in json("content/learning/scenarios.json").getJSONArray("scenarios").objects())for(step in s.getJSONArray("steps").objects()){
   val key=s.getString("id")+"/"+step.getString("id");keys.add(key);val plan=plans.getJSONObject(key)
   assertEquals(s.getString("audience"),plan.getString("audience"))
   if(plan.getString("model")=="anatomy"){
    if(plan.optString("part").isNotEmpty())assertTrue(key,plan.getString("part") in anatomy)
    if(plan.has("teachingKey"))assertNotNull(key,FirstAidSceneGuide.byKey(plan.getString("teachingKey")))
   }else assertNotNull(key,equipment.exportMesh(plan.getString("model")))
   if(s.getString("audience")=="awareness")assertFalse(key,plan.has("teachingKey"))
  };assertEquals(keys,plans.keys().asSequence().toSet())
 }
 @Test fun detailedGeometryIsFiniteBoundedAndExportedFromNativeBuffers(){
  val equipment=WorldEquipment();val ids=listOf("fire","gas","machinery","ppe","emergency","fire-pin-removed","fire-aimed","fire-discharging","fire-damaged","machinery-unlocked","machinery-guard-missing","ppe-damaged")+FireScenePlan.propKinds
  val out=System.getenv("SURAKSHA_EXPORT_PATH")?.let{File(it).apply{mkdirs()}};val entries=JSONArray()
  fun export(id:String,values:FloatArray,group:String="",name:String=id){
   assertEquals(id,0,values.size%36);assertTrue("$id triangle budget: ${values.size/36}",values.size/36 in 1..85000)
   for(i in values.indices step 12){for(j in 0..11)assertTrue("$id finite",values[i+j].isFinite());val n=(3..5).sumOf{values[i+it].toDouble()*values[i+it]};assertEquals("$id normal",1.0,n,.003)}
   out?.let{dir->val raw=ByteBuffer.allocate(values.size*4).order(ByteOrder.LITTLE_ENDIAN);values.forEach{raw.putFloat(it)};File(dir,"$id.bin").writeBytes(raw.array());entries.put(JSONObject().put("id",id).put("name",name).put("group",group).put("file","$id.bin").put("vertices",values.size/12))}
  }
  for(id in ids){val mesh=equipment.exportMesh(id)!!;export(id,FloatArray(mesh.capacity()){mesh.get(it)})}
  val manifest=json("content/anatomy/manifest.json")
  val landmarks=manifest.getJSONArray("parts").objects().filter{it.getString("id") in setOf("FMA7485","FMA7163")}.map{p->
   val raw=java.util.zip.GZIPInputStream(File("../../../content/anatomy/"+p.getString("id")+".meshpack").inputStream()).use{it.readBytes()}
   val buffer=ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).apply{position(8)}.asFloatBuffer();val v=FloatArray(buffer.remaining());buffer.get(v)
   AnatomyPart(p.getString("id"),p.getString("name"),p.getString("layer"),"",FloatArray(6){p.getJSONArray("bounds").getDouble(it).toFloat()},floatArrayOf(1f,1f,1f),v)
  }
  val calibration=FirstAidCprPlacement.forParts(landmarks)!!
  for(part in FirstAidEquipment.parts+FirstAidTeachingProps.parts+AnatomyClothing.parts){
   val values=FloatArray(part.vertices.size*2)
   for(i in part.vertices.indices step 6){val start=i*2;for(j in 0..5)values[start+j]=part.vertices[i+j]+if(j<3&&FirstAidCprPlacement.usesLandmark(part.id))calibration.translation[j]else 0f;for(j in 0..2)values[start+6+j]=part.colour[j];values[start+9]=.85f}
   export(part.id.replace(':','_'),values,part.name,part.name)
  }
  out?.let{dir->File(dir,"meshes.json").writeText(entries.toString());val scenes=JSONObject();val plans=json("content/spatial/catalog.json").getJSONObject("steps")
   for(key in plans.keys())plans.getJSONObject(key).optString("teachingKey").takeIf{it.isNotEmpty()}?.let{id->val s=FirstAidSceneGuide.byKey(id)!!;scenes.put(id,JSONObject().put("groups",JSONArray(s.props.toList())).put("motion",JSONArray(s.motion.toList())))}
   File(dir,"teaching-scenes.json").writeText(scenes.toString())
   val procedures=JSONArray()
   for(module in ProcedureCatalog.modules.values){val steps=JSONArray();for(step in module.steps){val actions=JSONArray();for(action in step.actions)actions.put(JSONObject().put("id",action.id).put("label",action.label).put("hindi",action.hindi).put("correct",action.correct).put("point",JSONArray(action.point.toList())))
    steps.put(JSONObject().put("id",step.id).put("title",step.title).put("hindi",step.hindi).put("explanation",step.explanation).put("hindiExplanation",step.hindiExplanation).put("actions",actions))}
    procedures.put(JSONObject().put("id",module.id).put("title",module.title).put("hindi",module.hindi).put("steps",steps))}
   File(dir,"procedures.json").writeText(JSONObject().put("version",ProcedureCatalog.VERSION).put("status","draft").put("modules",procedures).toString())
  }
 }
 @Test fun defectsAndOperatingStatesHaveDifferentGeometry(){val e=WorldEquipment();for(pair in listOf("fire" to "fire-pin-removed","fire" to "fire-damaged","machinery" to "machinery-guard-missing"))assertNotEquals(e.exportMesh(pair.first)!!.capacity(),e.exportMesh(pair.second)!!.capacity())}
}
