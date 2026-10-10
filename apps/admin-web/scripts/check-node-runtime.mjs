const [major,minor,patch]=process.versions.node.split('.').map(Number);
if(major<24||major===24&&(minor<13||minor===13&&patch<1))throw new Error('Suraksha standalone requires Node 24.13.1 or newer; deploy with supported Node 24 LTS.');
if(process.argv.includes('--deployment')){
 const origin=new URL(process.env.BETTER_AUTH_URL??'');
 if(origin.protocol!=='https:'||origin.pathname!=='/'||origin.username||origin.password||origin.search||origin.hash)throw new Error('Deployment requires a dedicated HTTPS account origin');
 if((process.env.BETTER_AUTH_SECRET?.length??0)<32)throw new Error('Account secret is not configured');
 if(process.env.VINEXT_TRUSTED_HOSTS!==origin.host)throw new Error('Trust only the dedicated account host behind the local HTTPS proxy');
 if(!process.env.SURAKSHA_DB_PATH?.startsWith('/var/lib/surakshaar/'))throw new Error('Expected private Suraksha storage');
}
console.log('Suraksha Node runtime configuration validated.');
