import {getAppUser} from '@/lib/auth';
import Login from '@/components/training/Login';
import {IdentityEnrolment} from '@/components/studio/IdentityEnrolment';
export const dynamic='force-dynamic';
export default async function Page({searchParams}:{searchParams:Promise<{staff?:string}>}){return await getAppUser()?<IdentityEnrolment staff={(await searchParams).staff==='1'}/>:<Login/>}
