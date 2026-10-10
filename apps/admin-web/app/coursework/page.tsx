import {getAppUser} from '@/lib/auth';
import Login from '@/components/training/Login';
import {CourseworkReview} from '@/components/training/CourseworkReview';
export const dynamic='force-dynamic';
export default async function Page(){return await getAppUser()?<CourseworkReview/>:<Login/>}
