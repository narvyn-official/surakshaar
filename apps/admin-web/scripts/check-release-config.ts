import {readFileSync} from 'node:fs';
import {parseEnv} from 'node:util';
import {inspectReleaseConfiguration} from '../lib/release-readiness';
import trust from '../lib/trusted-issuer.json';
// Explicit file only: no Keychain, SSH, cloud secrets, environment dumping or network access.
const file=process.argv[2];
if(!file){console.error('Usage: npx tsx scripts/check-release-config.ts /absolute/path/to/project.env');process.exitCode=2;}
else{try{const report=inspectReleaseConfiguration(parseEnv(readFileSync(file,'utf8')),trust);console.log(JSON.stringify(report,null,2));process.exitCode=report.configurationBlockers.length?1:0;}catch{console.error('Could not inspect this configuration file. No values were printed.');process.exitCode=2;}}
