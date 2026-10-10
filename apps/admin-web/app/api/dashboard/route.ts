import {dashboardData} from '@/lib/dashboard-data';
import {failure} from '@/lib/server';
export async function GET(request:Request){try{return Response.json(await dashboardData(new URL(request.url).searchParams.get('context')==='1'),{headers:{'Cache-Control':'private, no-store'}})}catch(e){return failure(e)}}
