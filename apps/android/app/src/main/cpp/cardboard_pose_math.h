#pragma once
#include <cmath>
#include <array>

namespace suraksha {
inline std::array<float,2> EyeTextureU(bool shared_image,int eye) {
  if(shared_image)return {0.f,1.f};
  return eye==0 ? std::array<float,2>{0.f,.5f} : std::array<float,2>{.5f,1.f};
}
// Column-major translation × quaternion rotation, matching Google's HelloCardboard
// GetPose / Quatf::ToMatrix convention. The SDK output is already a world-to-head view.
// Adapted from googlevr/cardboard v1.35.0 (Apache-2.0).
inline bool HeadView(const float* p, const float* q, float* m) {
  for (int i = 0; i < 3; ++i) if (!std::isfinite(p[i])) return false;
  float norm = 0;
  for (int i = 0; i < 4; ++i) { if (!std::isfinite(q[i])) return false; norm += q[i]*q[i]; }
  if (!std::isfinite(norm) || norm < .000001f) return false;
  const float k = 1.f/std::sqrt(norm);
  const float x=q[0]*k, y=q[1]*k, z=q[2]*k, w=q[3]*k;
  m[0]=1-2*y*y-2*z*z; m[1]=2*x*y+2*z*w; m[2]=2*x*z-2*y*w; m[3]=0;
  m[4]=2*x*y-2*z*w; m[5]=1-2*x*x-2*z*z; m[6]=2*y*z+2*x*w; m[7]=0;
  m[8]=2*x*z+2*y*w; m[9]=2*y*z-2*x*w; m[10]=1-2*x*x-2*y*y; m[11]=0;
  m[12]=p[0]; m[13]=p[1]; m[14]=p[2]; m[15]=1;
  return true;
}
}
