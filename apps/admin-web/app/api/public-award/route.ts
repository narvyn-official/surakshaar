import {db,failure} from '@/lib/server';
import {awardStatus,verifyAward} from '@/lib/institution-awards';
export async function GET(request:Request){try{
 const id=new URL(request.url).searchParams.get('id');if(!id||!/^[0-9a-f-]{36}$/i.test(id))throw Error('Invalid learning record reference.');
 const row=await db().prepare('SELECT a.payload,a.signature,a.expires_at,a.revoked_at,a.replaced_by,k.public_jwk FROM institution_awards a JOIN institution_signing_keys k ON k.owner=a.owner AND k.kid=a.kid WHERE a.id=?').bind(id).first<{payload:string;signature:string;expires_at:number;revoked_at:number|null;replaced_by:string|null;public_jwk:string}>();
 if(!row)throw Error('No institution record found.');if(!verifyAward(row.payload,row.signature,row.public_jwk))throw Error('Invalid record signature.');const p=JSON.parse(row.payload);
 return Response.json({id,status:awardStatus(row),issuer:p.issuer,learner:p.learner,course:p.course,issuedAt:p.issuedAt,expiresAt:p.expiresAt,mode:p.mode,observation:p.observation,identityMethod:p.identityMethod,scope:p.scope,replacedBy:row.replaced_by,signatureVerified:true},{headers:{'Cache-Control':'no-store','X-Robots-Tag':'noindex'}});
}catch(e){return failure(e)}}
