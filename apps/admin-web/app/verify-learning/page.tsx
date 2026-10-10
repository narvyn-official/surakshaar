import {PublicAward} from '@/components/training/InstitutionAwards';
export const metadata={title:'Verify institutional learning · SurakshaAR',robots:{index:false,follow:false}};
export default async function Page({searchParams}:{searchParams:Promise<{id?:string}>}){return <PublicAward id={(await searchParams).id??''}/>}
