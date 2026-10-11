import {staffWorkspaces,selectedWorkspace,defaultStaffWorkspace} from "./workspace-store";
import { env } from "@/lib/runtime-env";
import { headers } from "next/headers";
import { permitted, type Role, type Permission } from "@/lib/access";
import { getAppUser, requireStaffMfa } from "@/lib/auth";
import {staffScope} from './staff-scope';
export function db() {
  if (!env.DB) throw new Error("Training storage is unavailable.");
  return env.DB as D1Database;
}
export type Access = { owner: string; role: Role; restricted:boolean; user: NonNullable<Awaited<ReturnType<typeof getAppUser>>> };
export async function access(permission: Permission = "read", scoped = false): Promise<Access> {
  const user = await getAppUser();
  if (!user) throw new Error("Sign in to access training records.");
  const selected=selectedWorkspace((await headers()).get("cookie")??"");
  const choices=await staffWorkspaces(user.userId);
  const current=selected?choices.find(w=>w.owner===selected):defaultStaffWorkspace(choices,user.userId);
  if(!current)throw new Error("Forbidden workspace: choose an approved institute or ask your administrator to restore access.");
  const who=current.owner,role=current.role;
  await requireStaffMfa(user.userId);
  if (!permitted(role, permission)) throw new Error("Forbidden: your role does not allow this action.");
  const {restricted}=await staffScope(who,user.userId);
  if(restricted&&!scoped)throw new Error('Forbidden: this tool requires institution-wide access. Use the scoped tools on your dashboard.');
  return {owner: who, role, user,restricted};
}
export async function owner(permission: Permission = "read") { return (await access(permission)).owner; }
export function audit(a: Omit<Access,"restricted">, action: string, target: string, detail: unknown = {}, onlyAfterChange = false) {
  return db().prepare("INSERT INTO audit_log(id,owner,actor,actor_email,action,target,detail,at) SELECT ?,?,?,?,?,?,?,?" + (onlyAfterChange ? " WHERE changes() > 0" : ""))
    .bind(crypto.randomUUID(), a.owner, a.user.userId, a.user.email, action, target, JSON.stringify(detail), Date.now());
}
export function failure(error: unknown) {
  const raw = error instanceof Error ? error.message : "Request failed";
  const message=raw.includes('staff_scope_current')?'Forbidden: your staff scope or learner placement changed. Refresh before continuing.':raw;
  const allowed =
    /^(Invalid|Unsupported|No |Not |Sign in|Record conflict|Only |Forbidden|Already |Credential |Training |File |Unknown)/.test(
      message,
    );
  return Response.json(
    {
      error: allowed
        ? message
        : "Could not complete the request. Please try again.",
    },
    { headers: {"Cache-Control":"no-store"}, status: message.startsWith("Sign in") ? 401 : message.startsWith("Forbidden") ? 403 : 400 },
  );
}
import {readJson} from "./http";
/** Next self-hosting may use an internal request URL behind a TLS proxy. */
export function json(request:Request,limit?:number){return readJson(request,limit,typeof env.BETTER_AUTH_URL==="string"?env.BETTER_AUTH_URL:undefined);}
export function canonical(value: unknown): string {
  if (Array.isArray(value)) return `[${value.map(canonical).join(",")}]`;
  if (value !== null && typeof value === "object")
    return `{${Object.entries(value)
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([k, v]) => `${JSON.stringify(k)}:${canonical(v)}`)
      .join(",")}}`;
  return JSON.stringify(value);
}
export async function digest(value: unknown) {
  const b = await crypto.subtle.digest(
    "SHA-256",
    new TextEncoder().encode(canonical(value)),
  );
  return Array.from(new Uint8Array(b), (n) =>
    n.toString(16).padStart(2, "0"),
  ).join("");
}
export function signingKey() {
  const k = (env as unknown as Record<string, string>).ISSUER_PRIVATE_JWK;
  if (!k) throw new Error("Credential issuer is not configured.");
  return JSON.parse(k);
}
