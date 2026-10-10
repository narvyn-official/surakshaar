import {workspacePage} from '@/lib/workspace-page';
import {PlatformConsole} from '@/components/workspace/PlatformConsole';
export const dynamic='force-dynamic';
export const metadata={title:'Platform operations'};
export default async function Page({searchParams}:{searchParams:Promise<{section?:string}>}){await workspacePage('/platform');const s=(await searchParams).section;return <PlatformConsole section={s==='plans'||s==='sources'?s:'institutes'}/>;}
