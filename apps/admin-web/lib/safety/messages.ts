export const errorLabels:Record<string,string>={
 'missing-key':'TinyFish is not configured. Ask the server administrator to set TINYFISH_API_KEY.',
 'budget':'The configured daily collection budget is exhausted. Try after the next UTC day.',
 'rate-limit':'TinyFish is rate limiting requests. A bounded retry is scheduled when possible.',
 'timeout':'The source or provider exceeded the time limit.',
 'provider-auth':'TinyFish rejected the backend credentials. Ask the server administrator to check the key.',
 'provider-quota':'TinyFish allowance or wallet quota is exhausted. No further automatic attempt will be made.',
 'provider-unavailable':'TinyFish is unavailable. Your last successful collection is preserved.',
 'malformed':'The provider returned incomplete or invalid data. Nothing was published.',
 'unsafe-url':'The source or redirect failed the public URL policy. Ask an administrator to review the source.',
 'source-unavailable':'The public source could not be read. Your last successful collection is preserved.',
 'source-changed':'The source approval changed while this job was pending. Check the source registry.',
 'access-revoked':'Workspace or staff permission was revoked. Collection was stopped.',
 'interrupted':'The collection worker was interrupted. Start a new check if needed.',
};
export function displayError(code:string|null){return code?errorLabels[code]??'Collection failed. Retry or ask your administrator.':null;}
