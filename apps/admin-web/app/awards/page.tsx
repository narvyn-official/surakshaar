import {getAppUser} from '@/lib/auth';
import Login from '@/components/training/Login';
import {InstitutionAwards} from '@/components/training/InstitutionAwards';
export const dynamic='force-dynamic';
export default async function Page({searchParams}:{searchParams:Promise<{learner?:string}>}){return await getAppUser()?<InstitutionAwards learner={(await searchParams).learner==='1'}/>:<Login/>}
