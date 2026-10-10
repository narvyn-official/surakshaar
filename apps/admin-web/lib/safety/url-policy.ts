import {isIP} from 'node:net';
/** Exact hosts and exact approved paths; no credentials, query strings, fragments or IP literals. */
export function sourceUrl(raw:string,hosts:string[]):string {
 let u:URL;try{u=new URL(raw)}catch{throw new Error('Invalid public source URL.');}
 if(raw!==raw.trim()||u.protocol!=='https:'||u.username||u.password||u.port||u.search||u.hash||!hosts.includes(u.hostname)||!/^[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?\.[a-z]{2,}$/i.test(u.hostname)||/(?:^|\.)(?:localhost|local|internal|test|invalid|example|onion)$/.test(u.hostname)||/%(?:00|0a|0d|2f|5c|2e)/i.test(raw)||raw.includes('\\'))throw new Error('Invalid public source URL. Use an allowlisted HTTPS host and a public page without parameters.');
 return u.href;
}
export function isPublicAddress(ip:string):boolean {
 if(!isIP(ip))return false;
 if(ip.includes(':')){
  // Conservative global-unicast allowlist. Exclude transition, documentation and special-use prefixes.
  const value=ip.toLowerCase();
  return /^[23][0-9a-f]{3}:/.test(value)&&!/^200[12]:/.test(value)&&!/^3fff:/.test(value)&&!value.includes('.')&&!value.includes('%');
 }
 const parts=ip.split('.').map(Number);if(parts.length!==4||parts.some(n=>!Number.isInteger(n)||n<0||n>255))return false;
 const [a,b,c]=parts;
 return !(a===0||a===10||a===127||a>=224||a===100&&b>=64&&b<=127||a===169&&b===254||a===172&&b>=16&&b<=31||a===192&&(b===168||b===0||b===88&&c===99)||a===198&&(b===18||b===19||b===51&&c===100)||a===203&&b===0&&c===113);
}
