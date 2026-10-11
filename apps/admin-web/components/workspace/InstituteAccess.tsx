'use client';
import Link from 'next/link';
import {useRef,useState} from 'react';
import {requestJson} from '@/lib/client-api';
import {GovernancePanel} from '@/components/training/GovernancePanel';
const saved=async()=>{};
export function InstituteAccess({email,platform}:{email:string;platform:boolean}){
 const [busy,setBusy]=useState(false),[error,setError]=useState('');const pending=useRef(false);
 return <main className="studio-main institution-workspace"><nav className="institution-links" aria-label="Account navigation"><Link href="/dashboard">Your dashboard</Link><Link href="/account">Account security</Link></nav><header className="studio-heading"><div><span className="studio-eyebrow">INSTITUTE ACCESS</span><h1>Join your team or register an institute.</h1><p>Your current account: {email}. Joining staff and applying for an institute are separate from learner enrolment.</p></div></header>
 {platform?<p className="studio-notice">Manage institute applications from <Link href="/platform?section=institutes">platform approvals</Link>.</p>:<>
 <section className="practice-lesson"><h2>Accept a staff invitation</h2><p>Use the private code your administrator gave you. The invitation must match this account’s email. After joining, your assigned role appears on your dashboard. Staff must set up an authenticator before opening records.</p><form className="studio-policy-form" onSubmit={async e=>{e.preventDefault();if(pending.current)return;pending.current=true;setBusy(true);setError('');const invitation=String(new FormData(e.currentTarget).get('invitation')??'').trim();try{await requestJson('/api/admin/session','POST',{invitation});window.location.assign(new URL('/dashboard',window.location.origin));}catch(e){setError((e as Error).message);setBusy(false);pending.current=false;}}}><label className="studio-field">Staff invitation code<input name="invitation" required maxLength={500} autoComplete="off" spellCheck={false} disabled={busy}/></label>{error&&<p className="studio-notice" role="alert">{error}</p>}<button className="studio-button" disabled={busy}>{busy?'Joining your institute…':'Accept staff invitation'}</button></form></section>
 <section aria-label="Institute application"><GovernancePanel onChange={saved} section="institutes"/><p className="studio-notice">Pending applications do not grant administrative access. If your institute is approved, <Link href="/dashboard">open your dashboard</Link> to continue. For suspended access, contact the platform operator and retain the review note.</p></section>
 </>}
 </main>;
}
