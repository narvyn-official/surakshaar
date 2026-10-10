import {z} from 'zod';
import {getAppUser,freshUser,isOperator,requireStaffMfa} from '@/lib/auth';
import {db,audit,failure,json} from '@/lib/server';
import {env} from '@/lib/runtime-env';
import {awardStorageReady} from '@/lib/institution-awards';
async function operator(fresh=false){const user=await(fresh?freshUser():getAppUser());if(!user)throw Error('Sign in to continue.');if(!isOperator(user.userId))throw Error('Forbidden platform operator.');await requireStaffMfa(user.userId);return user;}
export async function GET(){try{
 await operator();const rows=await db().prepare('SELECT c.owner,c.name,a.status,p.name AS plan_name,p.learner_limit,p.valid_until,p.restricted,p.reason,(SELECT COUNT(*) FROM workers w LEFT JOIN institution_people i ON i.owner=w.owner AND i.worker_id=w.id WHERE w.owner=c.owner AND i.archived_at IS NULL) AS active_learners FROM training_centres c JOIN centre_approvals a ON a.owner=c.owner LEFT JOIN institution_plans p ON p.owner=c.owner ORDER BY c.name LIMIT 200').all();
 return Response.json({institutions:rows.results,services:{encryptedEvidence:awardStorageReady(),institutionSigning:awardStorageReady(),tinyfish:!!env.TINYFISH_API_KEY,google:!!env.GOOGLE_CLIENT_ID&&!!env.GOOGLE_CLIENT_SECRET,emailOtp:env.EMAIL_OTP_ENABLED==='true'&&!!env.RECOVERY_MAIL_URL,faceVerification:false},scope:'Configuration status, not provider health or validation'},{headers:{'Cache-Control':'no-store'}});
}catch(e){return failure(e)}}
export async function POST(request:Request){try{
 const user=await operator(true),c=z.object({owner:z.string().min(1).max(300),name:z.string().trim().min(2).max(100),learnerLimit:z.number().int().min(1).max(10000),validUntil:z.number().int().positive().nullable(),restricted:z.boolean(),reason:z.string().trim().min(10).max(1000)}).strict().parse(await json(request));
 if(!await db().prepare('SELECT 1 FROM training_centres WHERE owner=?').bind(c.owner).first())throw Error('No institution found.');
 await db().batch([db().prepare('INSERT INTO institution_plans VALUES(?,?,?,?,?,?,?,?) ON CONFLICT(owner) DO UPDATE SET name=excluded.name,learner_limit=excluded.learner_limit,valid_until=excluded.valid_until,restricted=excluded.restricted,reason=excluded.reason,updated_at=excluded.updated_at,updated_by=excluded.updated_by').bind(c.owner,c.name,c.learnerLimit,c.validUntil,c.restricted?1:0,c.reason,Date.now(),user.userId),audit({owner:c.owner,role:'admin',user},'institution.plan',c.owner,{learnerLimit:c.learnerLimit,restricted:c.restricted,reason:c.reason})]);return Response.json({saved:true},{headers:{'Cache-Control':'no-store'}});
}catch(e){return failure(e)}}
