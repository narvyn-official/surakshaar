import {getAppUser} from './auth';
import {db} from './server';
import {selectLearnerLink,type LearnerLink} from './learner-scope';
export async function learnerLinks(userId:string) {
 const rows=await db().prepare("SELECT l.owner,l.worker_id,w.name,w.sector,c.name AS centre FROM learner_links l JOIN centre_approvals a ON a.owner=l.owner AND a.status='approved' JOIN workers w ON w.owner=l.owner AND w.id=l.worker_id JOIN training_centres c ON c.owner=l.owner WHERE l.user_id=? ORDER BY c.name,l.owner,l.worker_id").bind(userId).all<LearnerLink>();
 return rows.results;
}
export async function learnerAccess(workerId:string,owner?:string){
 const user=await getAppUser();if(!user)throw new Error('Sign in to continue.');
 return {...selectLearnerLink(await learnerLinks(user.userId),workerId,owner),user};
}
/** Content-only coaching has no institution data writer and needs membership, not a tenant choice. */
export async function learnerMembership(workerId:string){
 const user=await getAppUser();if(!user)throw new Error('Sign in to continue.');
 if(!(await learnerLinks(user.userId)).some(link=>link.worker_id===workerId))throw new Error('Forbidden: this learner is not linked to your account in an approved centre.');
 return {user};
}
