import type { NextConfig } from "next";
import path from 'node:path';
const nextConfig: NextConfig = {
 output:"standalone",
 allowedDevOrigins:["127.0.0.1","localhost"],
 outputFileTracingRoot:path.resolve(process.cwd(),'../..'),
 poweredByHeader:false,
 serverExternalPackages:['node:sqlite'],
 async headers(){return [{source:"/:path*",headers:[
   {key:"X-Content-Type-Options",value:"nosniff"},
   {key:"X-Frame-Options",value:"DENY"},
   {key:"Referrer-Policy",value:"no-referrer"},
   {key:"Permissions-Policy",value:"camera=(self), microphone=(), geolocation=()"},
 ]},... ["/","/login","/forgot-password","/reset-password","/api/:path*"].map(source=>({source,headers:[{key:"Cache-Control",value:"private, no-store"}]}))];},
};
export default nextConfig;
