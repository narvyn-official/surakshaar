#!/usr/bin/env python3
"""Build a locally signed distribution APK. Private signing material never enters artifacts/."""
import hashlib,json,os,pathlib,re,secrets,shutil,subprocess
root=pathlib.Path(__file__).resolve().parents[1]
private=root/'.runtime'/'android-release-signing'
private.mkdir(parents=True,exist_ok=True,mode=0o700)
config=private/'signing.json'
env=os.environ.copy()
env.setdefault('JAVA_HOME','/Applications/Android Studio.app/Contents/jbr/Contents/Home')
env.setdefault('ANDROID_HOME',str(pathlib.Path.home()/'Library/Android/sdk'))
if not config.exists():
    key=private/'surakshaar-release.keystore'
    if key.exists(): raise SystemExit('Signing key exists without configuration. Recover its existing password; do not overwrite it.')
    password=secrets.token_urlsafe(48)
    values={'SURAKSHA_ANDROID_KEYSTORE':str(key),'SURAKSHA_ANDROID_STORE_PASSWORD':password,'SURAKSHA_ANDROID_KEY_PASSWORD':password,'SURAKSHA_ANDROID_KEY_ALIAS':'surakshaar-release'}
    env.update(values)
    subprocess.run([env['JAVA_HOME']+'/bin/keytool','-genkeypair','-keystore',str(key),'-storetype','PKCS12','-alias','surakshaar-release','-keyalg','RSA','-keysize','3072','-validity','10000','-dname','CN=SurakshaAR Release, O=SurakshaAR, C=IN','-storepass:env','SURAKSHA_ANDROID_STORE_PASSWORD','-keypass:env','SURAKSHA_ANDROID_KEY_PASSWORD'],env=env,check=True)
    key.chmod(0o600)
    fd=os.open(config,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
    with os.fdopen(fd,'w') as f:json.dump(values,f)
else: env.update(json.loads(config.read_text()))
subprocess.run([str(root/'apps/android/gradlew'),'-p',str(root/'apps/android'),'testDebugUnitTest','assembleRelease'],env=env,check=True)
version=re.search(r'versionName = "([^"]+)"',(root/'apps/android/app/build.gradle.kts').read_text()).group(1)
out=root/'artifacts'/('surakshaar-'+version)
out.mkdir(parents=True,exist_ok=True)
apk=out/('surakshaar-'+version+'-release.apk')
shutil.copyfile(root/'apps/android/app/build/outputs/apk/release/app-release.apk',apk)
buildtools=pathlib.Path(env['ANDROID_HOME'])/'build-tools'
versions=sorted((p for p in buildtools.iterdir() if p.is_dir() and (p/'apksigner').exists()),key=lambda p:tuple(int(x) for x in p.name.split('.') if x.isdigit()))
subprocess.run([str(versions[-1]/'zipalign'),'-c','-P','16','-v','4',str(apk)],env=env,check=True,stdout=subprocess.DEVNULL)
report=subprocess.check_output([str(versions[-1]/'apksigner'),'verify','--verbose','--print-certs',str(apk)],env=env,text=True)
(out/'android-signature.txt').write_text(report)
print('Signed APK:',apk)
print('SHA-256:',hashlib.sha256(apk.read_bytes()).hexdigest())
print('Private key kept outside deliverables. Back up .runtime/android-release-signing securely before distributing updates.')
