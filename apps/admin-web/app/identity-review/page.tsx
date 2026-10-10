import {getAppUser} from '@/lib/auth';
import Login from '@/components/training/Login';
import {IdentityReview} from '@/components/studio/IdentityReview';
export const dynamic='force-dynamic';
export default async function Page(){return await getAppUser()?<IdentityReview/>:<Login/>}
