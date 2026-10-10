'use client';
import {useCallback,useEffect,useRef,useState} from 'react';
import {Button} from '@/components/ui/button';
import {Input} from '@/components/ui/input';
import {requestJson} from '@/lib/client-api';
import {displayError} from '@/lib/safety/messages';
import type {EquipmentPanel,EquipmentResult} from '@/lib/safety/search-model';
export function EquipmentReferences(){
 const [data,setData]=useState<EquipmentPanel|null>(null),[error,setError]=useState(''),[message,setMessage]=useState(''),[busy,setBusy]=useState(false);
 const pending=useRef(false);
 const load=useCallback(async()=>setData(await requestJson<EquipmentPanel>('/api/safety-updates/equipment')),[]);
 useEffect(()=>{let active=true;void requestJson<EquipmentPanel>('/api/safety-updates/equipment').then(d=>{if(active)setData(d)}).catch(e=>{if(active)setError(e.message)});return()=>{active=false}},[]);
 async function save(url:string,body:unknown,success:string){if(pending.current)return;pending.current=true;setBusy(true);setError('');setMessage('');try{const result=await requestJson<{cached?:boolean}>(url,'POST',body);await load();setMessage(result.cached?'Recent results reused. No new provider request was made.':success)}catch(e){setError(e instanceof Error?e.message:'Could not complete this action.');await load().catch(()=>{})}finally{pending.current=false;setBusy(false)}}
 return <div className="safety-panel" aria-label="Equipment references"><div className="safety-context"><h2>Find equipment references</h2><p>Look up public manuals and specifications on selected manufacturer websites. Check the exact model, market and revision before using a reference.</p></div>
 {error&&<p className="notice error" role="alert">{error} <Button variant="outline" disabled={busy} onClick={()=>void load().then(()=>setError('')).catch(e=>setError(e.message))}>Reload</Button></p>}
 {message&&<p className="notice success" role="status">{message}</p>}
 {!data&&!error&&<p role="status">Loading equipment search…</p>}
 {data&&<><form className="safety-source safety-form" onSubmit={e=>{e.preventDefault();const f=new FormData(e.currentTarget);void save('/api/safety-updates/equipment',{action:'search',host:f.get('host'),model:f.get('model'),publicOnly:f.get('publicOnly')==='on'},'Search complete. These results are unreviewed references.')}}>
 <label>Manufacturer website<select name="host" required disabled={!data.hosts.length}>{data.hosts.map(h=><option key={h}>{h}</option>)}</select></label>
 <label>Public make and model<Input name="model" required minLength={2} maxLength={100} placeholder="For example: ZOLL AED Plus" aria-describedby="equipment-privacy"/></label>
 <p className="fine" id="equipment-privacy">This make/model and selected host are sent to TinyFish Search. Do not enter names, serial numbers, worker records or private documents. Search history is private to this workspace and removed after 30 days; backups expire separately.</p>
 <label className="safety-consent"><input name="publicOnly" type="checkbox" required/>I am searching only for public product information.</label>
 <Button disabled={busy||!data.configured||!data.hosts.length}>{busy?'Working…':'Search manufacturer references'}</Button>
 {!data.configured&&<p className="notice">Equipment search is unavailable until the server administrator configures TinyFish.</p>}
 <p className="fine">Search and collection share the daily request cap. Searches have no automatic retries. Matching searches reuse results for one hour. Results never create accurate 3D geometry or approve a training procedure.</p>
 </form>
 <div aria-busy={busy}>{!data.searches.length&&<p className="safety-empty">No searches yet. Start with a public equipment make and model.</p>}{data.searches.map(search=><article key={search.id} className="safety-source"><h3>{search.model}</h3><p className="fine">{search.host} · {new Date(search.created_at).toLocaleString()} · {search.status}</p>
 {search.status==='pending'&&<p role="status">Search is processing. Reload after it finishes. Interrupted searches time out after three minutes.</p>}
 {search.error&&<p className="notice error">{displayError(search.error)}</p>}
 {search.status==='completed'&&!search.results.length&&<p>No results met the exact-host and public-URL rules. Try the manufacturer’s product name or inspect its website directly.</p>}
 {search.results.map(result=><div className="safety-audit" key={result.url}><a className="safety-source-link" href={result.url} target="_blank" rel="noopener noreferrer">{result.title} (new tab)</a><p className="fine safety-wrap">{result.url}</p><p>{result.snippet}</p><p className="fine">Unreviewed search result · model compatibility and document rights still need checking.</p>{data.operator?<Proposal key={result.url} result={result} busy={busy} save={body=>save('/api/safety-updates/sources',body,'Source proposed. Approve it separately in Sources before collection.')}/>:<p className="fine">A platform administrator can propose this URL in the source registry. Search does not authorize collection.</p>}</div>)}
 </article>)}</div></>}
 </div>;
}
function Proposal({result,busy,save}:{result:EquipmentResult;busy:boolean;save:(body:unknown)=>Promise<void>}){
 return <details><summary>Propose this source for review</summary><form className="safety-form" onSubmit={e=>{e.preventDefault();const f=new FormData(e.currentTarget);void save({action:'propose',url:result.url,title:f.get('title'),publisher:f.get('publisher'),topic:f.get('topic'),jurisdiction:f.get('jurisdiction'),rationale:f.get('rationale')})}}>
 <label>Title<Input name="title" required minLength={3} maxLength={200} defaultValue={result.title}/></label><label>Publisher<Input name="publisher" required minLength={3} maxLength={150} defaultValue={result.host}/></label>
 <label>Training area<select name="topic"><option value="fire">Fire Safety</option><option value="first-aid">First Aid</option></select></label><label>Applicable market / jurisdiction<Input name="jurisdiction" required minLength={2} maxLength={120}/></label><label>Relevance and limitations<textarea name="rationale" required minLength={10} maxLength={1000}/></label><p className="fine">Confirm publisher, model and permission to retain excerpts. A recent sign-in is required.</p><Button disabled={busy}>Save proposed source</Button></form></details>;
}
