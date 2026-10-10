import {isAbsolute} from 'node:path';
const major=Number(process.versions.node.split('.')[0]);if(major<24||major>=27)throw Error('Use supported Node 24 LTS (>=24.13.1).');
const origin=new URL(process.env.BETTER_AUTH_URL??'');if(origin.protocol!=='https:'||origin.pathname!=='/'||origin.username||origin.password||origin.hash||origin.search)throw Error('Set a dedicated canonical HTTPS origin.');
if(!process.env.BETTER_AUTH_SECRET||process.env.BETTER_AUTH_SECRET.length<32||process.env.BETTER_AUTH_SECRET.includes('REPLACE_'))throw Error('Set a random account secret.');
if(!isAbsolute(process.env.SURAKSHA_DB_PATH??''))throw Error('Set an absolute private database path.');
if(process.env.REQUIRE_STAFF_MFA==='false')throw Error('Staff MFA must remain enabled for public deployment.');
console.log('Core runtime configuration accepted. Operator, issuer, recovery and content gates remain separate.');
