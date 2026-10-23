#include <array>
#include <cassert>
#include <cmath>
#include <iostream>
#include <limits>
#include "cardboard_pose_math.h"
#include "polynomial_radial_distortion.h"

int main() {
  float p[]{.1f,-.2f,.3f};float q[]{0,0,0,1};float m[16]{};
  assert(suraksha::HeadView(p,q,m));
  for(int c=0;c<3;++c)for(int r=0;r<3;++r)assert(std::abs(m[c*4+r]-(c==r?1.f:0.f))<.00001f);
  assert(m[12]==p[0]&&m[13]==p[1]&&m[14]==p[2]&&m[15]==1);
  // A 90-degree yaw uses the same signs/column ordering as the upstream sample.
  float yaw[]{0,std::sqrt(.5f),0,std::sqrt(.5f)};
  assert(suraksha::HeadView(p,yaw,m));assert(std::abs(m[8]-1)<.00001f);assert(std::abs(m[2]+1)<.00001f);
  float scaled[]{0,yaw[1]*3,0,yaw[3]*3};float normalized[16]{};
  assert(suraksha::HeadView(p,scaled,normalized));for(int i=0;i<16;++i)assert(std::abs(normalized[i]-m[i])<.00001f);
  float invalid[]{0,0,0,0};assert(!suraksha::HeadView(p,invalid,m));
  invalid[3]=std::numeric_limits<float>::infinity();assert(!suraksha::HeadView(p,invalid,m));
  p[0]=std::numeric_limits<float>::quiet_NaN();assert(!suraksha::HeadView(p,q,m));
  cardboard::PolynomialRadialDistortion standard({.441f,.156f});
  for(const auto& point:std::array<std::array<float,2>,5>{{{0,0},{.1f,.2f},{-.4f,.3f},{.7f,-.5f},{.6f,.6f}}}){
    const auto roundtrip=standard.DistortInverse(standard.Distort(point));
    assert(std::isfinite(roundtrip[0])&&std::isfinite(roundtrip[1]));
    assert(std::abs(roundtrip[0]-point[0])<.0005f&&std::abs(roundtrip[1]-point[1])<.0005f);
  }
  // Adversarial finite coefficients must terminate. Invalid meshes are rejected
  // by nativeCreate; this does not substitute a different viewer calibration.
  for(float coefficient:std::array<float,7>{-10,-2,-1,-.5f,0,2,10}){
    cardboard::PolynomialRadialDistortion unusual({coefficient,coefficient});
    for(int i=1;i<=30;++i)(void)unusual.DistortInverse({i*.1f,i*.05f});
  }
  std::cout<<"Cardboard pose conventions, finite validation, default lens roundtrips and bounded unusual-profile solves passed\n";
}
