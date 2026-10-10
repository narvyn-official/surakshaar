import {z} from 'zod';
export const centreId = z.string().min(1).max(128).regex(/^[A-Za-z0-9_-]+$/);
export type LearnerLink = {owner:string;worker_id:string;name:string;sector:string;centre:string};
/** Never infer a tenant from row order when one profile belongs to several institutions. */
export function selectLearnerLink(links:LearnerLink[], workerId:string, owner?:string):LearnerLink {
 const matches=links.filter(link=>link.worker_id===workerId&&(owner===undefined||link.owner===owner));
 if(!matches.length)throw new Error('Forbidden: this learner is not linked to your account in the selected approved centre.');
 if(matches.length!==1)throw new Error('Record conflict: choose a training centre for this learner before continuing.');
 return matches[0];
}
