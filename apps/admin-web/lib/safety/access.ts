import {getAppUser,isOperator,requireStaffMfa} from '../auth';
import {access} from '../server';
import {requireSafetyRole} from './model';
/** Public reference tools also work for explicitly configured platform operators without a centre. */
export async function referenceAccess(){
 const user=await getAppUser();if(!user)throw new Error('Sign in to continue.');
 if(isOperator(user.userId)){await requireStaffMfa(user.userId);return {owner:user.userId,role:'admin',user,operator:true}}
 const a=await access();requireSafetyRole(a.role,'read');return {...a,operator:false};
}
