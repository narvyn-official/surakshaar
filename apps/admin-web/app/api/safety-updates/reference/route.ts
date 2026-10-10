import {access,db,failure} from '@/lib/server';
import {requireSafetyRole} from '@/lib/safety/model';
export async function GET(request:Request){try{
 const a=await access();requireSafetyRole(a.role,'read');
 const id=new URL(request.url).searchParams.get('id');if(!id||id.length>100)throw new Error('Invalid reference.');
 const row=await db().prepare(`SELECT v.id,v.source_id,v.title,v.publisher,v.source_url,v.published_date,v.retrieved_at,v.content_hash,v.excerpt,v.previous_excerpt,v.topic,v.jurisdiction,v.change_kind,v.origin,v.source_revision,s.revision AS current_revision,s.status AS source_status,COALESCE(r.status,'new') AS status,COALESCE(r.notes,'') AS notes,COALESCE(r.revision,0) AS review_revision,r.reviewed_at FROM safety_versions v JOIN safety_sources s ON s.id=v.source_id LEFT JOIN safety_reviews r ON r.version_id=v.id AND r.owner=? WHERE v.id=?`).bind(a.owner,id).first();
 if(!row)return Response.json({error:'Unknown reference.'},{status:404,headers:{'Cache-Control':'no-store'}});
 return Response.json(row,{headers:{'Cache-Control':'no-store'}});
}catch(e){return failure(e)}}
