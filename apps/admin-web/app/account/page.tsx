import Link from 'next/link';
import {getAppUser} from '@/lib/auth';
import Login from '@/components/training/Login';
import {AccountActions} from '@/components/training/AccountActions';
export const dynamic='force-dynamic';
export default async function Page(){return await getAppUser()?<main className="learner-page"><Link href="/dashboard">Back to your dashboard</Link><h1>Account security</h1><p>Institution staff must enable an authenticator before opening institutional records.</p><AccountActions/></main>:<Login/>}
