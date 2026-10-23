#include <jni.h>
#include <GLES2/gl2.h>
#include <cardboard.h>
#include <array>
#include <cmath>
#include <cstdint>
#include <memory>
#include <mutex>
#include <unordered_map>
#include "cardboard_pose_math.h"

namespace {
struct Viewer {
  CardboardHeadTracker* tracker=nullptr;
  CardboardLensDistortion* lens=nullptr;
  CardboardDistortionRenderer* compositor=nullptr;
  int width=0, height=0;
  bool running=false;
  ~Viewer() {
    if (tracker) { CardboardHeadTracker_pause(tracker); CardboardHeadTracker_destroy(tracker); }
    if (compositor) CardboardDistortionRenderer_destroy(compositor);
    if (lens) CardboardLensDistortion_destroy(lens);
  }
};
std::mutex mutex;
std::unordered_map<jlong, std::unique_ptr<Viewer>> viewers;
jlong next_id=1;
bool initialised=false;
void Fail(JNIEnv* env, const char* message) {
  if (!env->ExceptionCheck()) env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), message);
}
Viewer* Get(JNIEnv* env, jlong id) {
  const auto it=viewers.find(id);
  if (it==viewers.end()) { Fail(env,"The headset renderer is closed."); return nullptr; }
  return it->second.get();
}
bool Output(JNIEnv* env, jfloatArray out, const float* matrix) {
  if (!out || env->GetArrayLength(out)!=16) { Fail(env,"Invalid headset matrix destination."); return false; }
  for (int i=0;i<16;++i) if (!std::isfinite(matrix[i])) { Fail(env,"The viewer calibration produced an invalid matrix."); return false; }
  env->SetFloatArrayRegion(out,0,16,matrix); return true;
}
CardboardEye Eye(jint eye) { return eye==0?kLeft:kRight; }
CardboardViewportOrientation Orientation(jint rotation) {
  switch(rotation) { case 1:return kLandscapeLeft; case 3:return kLandscapeRight; case 2:return kPortraitUpsideDown; default:return kPortrait; }
}
}

#define JNI_NAME(name) Java_com_narvyn_suraksha_CardboardBridge_##name
extern "C" {
JNIEXPORT void JNICALL JNI_NAME(nativeInitialise)(JNIEnv* env,jobject,jobject context) {
  std::lock_guard<std::mutex> lock(mutex);
  if (initialised) return;
  JavaVM* vm=nullptr; env->GetJavaVM(&vm);
  Cardboard_initializeAndroid(vm,context);
  initialised=!env->ExceptionCheck();
}
JNIEXPORT void JNICALL JNI_NAME(nativeScanViewer)(JNIEnv* env,jobject) {
  std::lock_guard<std::mutex> lock(mutex);
  if (!initialised) { Fail(env,"Prepare headset setup first."); return; }
  CardboardQrCode_scanQrCodeAndSaveDeviceParams();
}
JNIEXPORT jlong JNICALL JNI_NAME(nativeCreate)(JNIEnv* env,jobject,jint w,jint h,jbyteArray params) {
  std::lock_guard<std::mutex> lock(mutex);
  if (!initialised || w<2 || h<2 || w>16384 || h>16384 || !params) { Fail(env,"Invalid headset configuration."); return 0; }
  const int size=env->GetArrayLength(params);
  if (size<1 || size>8192 || viewers.size()>=2) { Fail(env,"Invalid headset profile or renderer ownership."); return 0; }
  std::array<uint8_t,8192> bytes{};
  env->GetByteArrayRegion(params,0,size,reinterpret_cast<jbyte*>(bytes.data()));
  if(env->ExceptionCheck())return 0;
  auto viewer=std::make_unique<Viewer>(); viewer->width=w;viewer->height=h;
  viewer->lens=CardboardLensDistortion_create(bytes.data(),size,w,h);
  if (!viewer->lens || env->ExceptionCheck()) { Fail(env,"The viewer profile could not be applied."); return 0; }
  const CardboardOpenGlEsDistortionRendererConfig config{kGlTexture2D};
  viewer->compositor=CardboardOpenGlEs2DistortionRenderer_create(&config);
  if (!viewer->compositor) { Fail(env,"Lens correction could not start."); return 0; }
  for (int eye=0;eye<2;++eye) {
    CardboardMesh mesh{}; CardboardLensDistortion_getDistortionMesh(viewer->lens,Eye(eye),&mesh);
    if (mesh.n_indices<=0 || mesh.n_indices>50000 || mesh.n_vertices<=0 || mesh.n_vertices>10000 || !mesh.vertices || !mesh.uvs || !mesh.indices) { Fail(env,"Invalid viewer lens mesh."); return 0; }
    for (int i=0;i<mesh.n_vertices*2;++i) if (!std::isfinite(mesh.vertices[i]) || !std::isfinite(mesh.uvs[i])) { Fail(env,"The viewer profile does not produce a usable lens mesh."); return 0; }
    for (int i=0;i<mesh.n_indices;++i) if (mesh.indices[i]<0 || mesh.indices[i]>=mesh.n_vertices) { Fail(env,"Invalid viewer lens indices."); return 0; }
    CardboardDistortionRenderer_setMesh(viewer->compositor,&mesh,Eye(eye));
  }
  viewer->tracker=CardboardHeadTracker_create();
  if (!viewer->tracker) { Fail(env,"Head tracking could not start."); return 0; }
  const jlong id=next_id++;viewers.emplace(id,std::move(viewer));return id;
}
JNIEXPORT void JNICALL JNI_NAME(nativeResume)(JNIEnv* env,jobject,jlong id) {
  std::lock_guard<std::mutex> lock(mutex);auto* v=Get(env,id);if(v&&!v->running){CardboardHeadTracker_resume(v->tracker);v->running=true;}
}
JNIEXPORT void JNICALL JNI_NAME(nativePause)(JNIEnv* env,jobject,jlong id) {
  std::lock_guard<std::mutex> lock(mutex);auto* v=Get(env,id);if(v&&v->running){CardboardHeadTracker_pause(v->tracker);v->running=false;}
}
JNIEXPORT void JNICALL JNI_NAME(nativeRecenter)(JNIEnv* env,jobject,jlong id) {
  std::lock_guard<std::mutex> lock(mutex);auto* v=Get(env,id);if(v)CardboardHeadTracker_recenter(v->tracker);
}
JNIEXPORT void JNICALL JNI_NAME(nativeDestroy)(JNIEnv*,jobject,jlong id) {
  std::lock_guard<std::mutex> lock(mutex);viewers.erase(id);
}
JNIEXPORT void JNICALL JNI_NAME(nativeHeadView)(JNIEnv* env,jobject,jlong id,jlong time,jint rotation,jfloatArray out) {
  std::lock_guard<std::mutex> lock(mutex);auto* v=Get(env,id);if(!v)return;
  if (time<=0 || rotation<0 || rotation>3) { Fail(env,"Invalid head-tracking timestamp or orientation."); return; }
  std::array<float,3> p{};std::array<float,4> q{};std::array<float,16> matrix{};
  CardboardHeadTracker_getPose(v->tracker,time,Orientation(rotation),p.data(),q.data());
  if(!suraksha::HeadView(p.data(),q.data(),matrix.data())){Fail(env,"Head tracking returned an invalid pose.");return;}
  Output(env,out,matrix.data());
}
JNIEXPORT void JNICALL JNI_NAME(nativeProjection)(JNIEnv* env,jobject,jlong id,jint eye,jfloat near,jfloat far,jfloatArray out) {
  std::lock_guard<std::mutex> lock(mutex);auto* v=Get(env,id);if(!v)return;
  if(eye<0||eye>1||!std::isfinite(near)||!std::isfinite(far)||near<=0||far<=near){Fail(env,"Invalid eye projection.");return;}
  std::array<float,16> m{};CardboardLensDistortion_getProjectionMatrix(v->lens,Eye(eye),near,far,m.data());Output(env,out,m.data());
}
JNIEXPORT void JNICALL JNI_NAME(nativeEyeFromHead)(JNIEnv* env,jobject,jlong id,jint eye,jfloatArray out) {
  std::lock_guard<std::mutex> lock(mutex);auto* v=Get(env,id);if(!v)return;
  if(eye<0||eye>1){Fail(env,"Invalid eye.");return;}
  std::array<float,16> m{};CardboardLensDistortion_getEyeFromHeadMatrix(v->lens,Eye(eye),m.data());Output(env,out,m.data());
}
JNIEXPORT void JNICALL JNI_NAME(nativeComposite)(JNIEnv* env,jobject,jlong id,jint w,jint h,jint texture,jboolean shared_image) {
  std::lock_guard<std::mutex> lock(mutex);auto* v=Get(env,id);if(!v)return;
  if (w!=v->width||h!=v->height||texture<=0) { Fail(env,"The headset frame no longer matches its viewer calibration."); return; }
  const auto l=suraksha::EyeTextureU(shared_image,0),r=suraksha::EyeTextureU(shared_image,1);
  const CardboardEyeTextureDescription left{static_cast<uint64_t>(texture),l[0],l[1],1.f,0.f};
  const CardboardEyeTextureDescription right{static_cast<uint64_t>(texture),r[0],r[1],1.f,0.f};
  glBindFramebuffer(GL_FRAMEBUFFER,0);glViewport(0,0,w,h);
  glDisable(GL_SCISSOR_TEST);glDisable(GL_DEPTH_TEST);glDisable(GL_CULL_FACE);glDisable(GL_BLEND);
  glClearColor(0,0,0,1);glClear(GL_COLOR_BUFFER_BIT);
  CardboardDistortionRenderer_renderEyeToDisplay(v->compositor,0,0,0,w,h,&left,&right);
}
}
