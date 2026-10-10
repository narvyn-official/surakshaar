import {getAppUser} from '@/lib/auth';
import {learnerLinks} from '@/lib/learner';
import {db,failure} from '@/lib/server';
export async function GET(request:Request){try{
 const user=await getAppUser();if(!user)throw Error('Sign in to continue.');const links=await learnerLinks(user.userId),owner=new URL(request.url).searchParams.get('owner');
 if(!owner)return Response.json({institutions:links.map(l=>({owner:l.owner,name:l.centre,workerId:l.worker_id}))},{headers:{'Cache-Control':'no-store'}});
 if(!links.some(l=>l.owner===owner))throw Error('Forbidden: current institutional membership is required.');
 const rows=await db().prepare("SELECT id,title,body,sources,language,revision,reviewed_at,review_note FROM local_procedures WHERE owner=? AND status='approved' ORDER BY title LIMIT 100").bind(owner).all();return Response.json({procedures:rows.results,scope:'Institution-reviewed local instructions; follow the stated equipment, setting and training limits.'},{headers:{'Cache-Control':'private, no-store'}});
}catch(e){return failure(e)}}
