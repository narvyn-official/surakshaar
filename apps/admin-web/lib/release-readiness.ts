import {createPrivateKey,createPublicKey,sign,verify,type JsonWebKey} from 'node:crypto';
import {configuredContextCoach} from './context-coach-provider';
import {recoveryReady} from './recovery-mail';
function publicHttps(value:string|undefined,originOnly=false){try{const u=new URL(value??'');return u.protocol==='https:'&&!u.username&&!u.password&&!u.search&&!u.hash&&(!originOnly||u.pathname==='/')&&!['localhost','127.0.0.1'].includes(u.hostname)&&!u.hostname.endsWith('.local')&&!u.hostname.endsWith('.internal')&&!u.hostname.includes(':')&&!/^\d+(?:\.\d+){3}$/.test(u.hostname)&&u.hostname.includes('.')}catch{return false}}
/** Local configuration inspection only. Never emits secret values or contacts a provider. */
export function inspectReleaseConfiguration(values:Record<string,string|undefined>,trust:{jwk:JsonWebKey;scope:string}) {
 let issuer='missing';
 if(values.ISSUER_PRIVATE_JWK){try{
  const key=createPrivateKey({key:JSON.parse(values.ISSUER_PRIVATE_JWK),format:'jwk'}),publicKey=createPublicKey({key:trust.jwk,format:'jwk'});
  const challenge=Buffer.from('SurakshaAR local key-pair readiness check; not a credential');
  issuer=verify('sha256',challenge,publicKey,sign('sha256',challenge,key))?'matches-bundled-trust':'mismatched-trust';
 }catch{issuer='invalid'}}
 const checks={canonicalHttpsOrigin:publicHttps(values.BETTER_AUTH_URL,true),accountSecretPresent:!!values.BETTER_AUTH_SECRET&&values.BETTER_AUTH_SECRET.length>=32,recoveryConfigured:recoveryReady(values)&&publicHttps(values.RECOVERY_MAIL_URL),platformOperatorConfigured:!!values.PLATFORM_OPERATOR_IDS?.split(',').some(v=>v.trim()),issuer};
 const blocked=Object.entries(checks).filter(([key,value])=>key==='issuer'?value!=='matches-bundled-trust':value!==true).map(([key])=>key);
 return {schemaVersion:1,scope:'local-configuration-inspection',checks,configurationBlockers:blocked,issuerScope:trust.scope,coachConfiguration:configuredContextCoach(values).status,networkRequests:0,credentialsExposed:false,productionReady:false,
  requiredExternalEvidence:['HTTPS deployment and restore drill','Account recovery delivery and domain authentication','Approved institutional/operator governance','Qualified review of exact safety content and translations','Signed Android release and supported-phone sustained AR validation','Dependency security gate','Privacy/retention and support ownership'],
  note:'Passing local configuration checks does not confirm deployment, delivery, provider privacy, content approval or release readiness.'};
}
