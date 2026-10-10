import {env} from '@/lib/runtime-env';
import {recoveryReady} from '@/lib/recovery-mail';
/** Public feature availability only; no account lookup or operator configuration disclosure. */
export function GET(){return Response.json({accounts:true,passwordRecovery:recoveryReady(env as unknown as Record<string,string>)},{headers:{'Cache-Control':'no-store'}})}
