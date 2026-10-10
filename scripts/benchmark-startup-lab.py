#!/usr/bin/env python3
"""Emulator-only first-frame startup diagnostic, not proof of usable-home/physical performance."""
import argparse,datetime,hashlib,json,math,pathlib,re,subprocess
p=argparse.ArgumentParser();p.add_argument('--serial',required=True);p.add_argument('--gpu',choices=['host','swiftshader'],required=True);p.add_argument('--samples',type=int,default=30);p.add_argument('--output',type=pathlib.Path,required=True);a=p.parse_args()
assert a.serial.startswith('emulator-') and 5<=a.samples<=100
root=pathlib.Path(__file__).resolve().parents[1];adb=pathlib.Path.home()/'Library/Android/sdk/platform-tools/adb'
def run(*args):return subprocess.run([str(adb),'-s',a.serial,*args],check=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=40).stdout
assert run('shell','getprop','ro.hardware').strip() in ['ranchu','goldfish']
installed=run('shell','dumpsys','package','com.narvyn.suraksha')
assert re.search(r'versionCode=24\b',installed) and 'versionName=0.11.0-dev' in installed,'Install the current debug APK before sampling.'
values=[]
for i in range(a.samples):
 run('shell','am','force-stop','com.narvyn.suraksha')
 result=run('shell','am','start','-W','-n','com.narvyn.suraksha/.MainActivity')
 m=re.search(r'^TotalTime: (\d+)$',result,re.M);assert m,result;values.append(int(m[1]))
 print(f'Sample {i+1}: {values[-1]} ms',flush=True)
sorted_values=sorted(values)
def percentile(q):return sorted_values[math.ceil(len(values)*q)-1]
apk=root/'artifacts/surakshaar-0.11.0-dev-debug.apk'
report={'capturedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'status':'Partial','scope':'Lab cold process to first activity frame (am start -W TotalTime), not usability or physical-device target proof','deviceKind':'emulator','model':run('shell','getprop','ro.product.model').strip(),'api':run('shell','getprop','ro.build.version.sdk').strip(),'build':'0.11.0-dev debug/code 24','apkSha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'gpuRequested':a.gpu,'sampleMs':values,'p50Ms':percentile(.5),'p95Ms':percentile(.95),'p99Ms':percentile(.99),'unverified':['Usable local Home on physical devices','100ms interaction feedback','30fps sustained AR','20-minute physical training','20 real AR entry/exit memory trend','Thermal and battery behavior']}
a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_text(json.dumps(report,indent=2)+'\n')
