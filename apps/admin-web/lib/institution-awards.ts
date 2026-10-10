import {generateKeyPairSync,createPrivateKey,createPublicKey,sign,verify,type JsonWebKey} from 'node:crypto';
import {db} from './server';
import {env} from './runtime-env';
import {encryptEvidence,decryptEvidence,identityReady} from './identity-evidence';
export function awardStorageReady(){return identityReady(String(env.IDENTITY_EVIDENCE_KEY??''));}
export async function awardKey(owner:string){
 let row=await db().prepare('SELECT kid,public_jwk,private_ciphertext FROM institution_signing_keys WHERE owner=? AND retired_at IS NULL').bind(owner).first<{kid:string;public_jwk:string;private_ciphertext:string}>();
 if(!row){
  const pair=generateKeyPairSync('ed25519'),kid=crypto.randomUUID(),publicJwk=JSON.stringify(pair.publicKey.export({format:'jwk'})),cipher=encryptEvidence(Buffer.from(JSON.stringify(pair.privateKey.export({format:'jwk'}))),owner+':key:'+kid,String(env.IDENTITY_EVIDENCE_KEY??''));
  await db().prepare('INSERT INTO institution_signing_keys VALUES(?,?,?,?,?,NULL) ON CONFLICT DO NOTHING').bind(owner,kid,publicJwk,cipher,Date.now()).run();row=await db().prepare('SELECT kid,public_jwk,private_ciphertext FROM institution_signing_keys WHERE owner=? AND retired_at IS NULL').bind(owner).first();
 }
 if(!row)throw Error('Training signing key could not be created.');
 const jwk=JSON.parse(decryptEvidence(row.private_ciphertext,owner+':key:'+row.kid,String(env.IDENTITY_EVIDENCE_KEY??'')).toString()) as JsonWebKey;
 return {kid:row.kid,sign:(payload:string)=>Buffer.from(sign(null,Buffer.from(payload),createPrivateKey({key:jwk,format:'jwk'}))).toString('base64url')};
}
export function verifyAward(payload:string,signature:string,publicJwk:string){return verify(null,Buffer.from(payload),createPublicKey({key:JSON.parse(publicJwk),format:'jwk'}),Buffer.from(signature,'base64url'));}
export function awardStatus(row:{revoked_at:number|null;replaced_by:string|null;expires_at:number},now=Date.now()){return row.revoked_at!==null?'Revoked':row.replaced_by?'Replaced':row.expires_at<=now?'Expired':'Active';}
