#!/usr/bin/env python3
"""Explicit emulator-only v0.10/code23 to v0.11/code24 synthetic-record continuity test."""
import argparse,pathlib,re,subprocess
p=argparse.ArgumentParser();p.add_argument('--serial',required=True);p.add_argument('--out',type=pathlib.Path,required=True);a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[1];adb=pathlib.Path.home()/'Library/Android/sdk/platform-tools/adb'
def run(*args):return subprocess.run([str(adb),'-s',a.serial,*args],check=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=90).stdout
assert a.serial.startswith('emulator-') and run('shell','getprop','ro.hardware').strip() in ['ranchu','goldfish']
old=root/'artifacts/surakshaar-0.10.0-debug.apk';new=root/'artifacts/surakshaar-0.11.0-dev-debug.apk';test=root/'apps/android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
a.out.mkdir(parents=True,exist_ok=True)
run('shell','am','force-stop','com.narvyn.suraksha');run('install','-r','-d',str(old));run('install','-r',str(test))
try:
 text=run('shell','am','instrument','-w','-e','class','com.narvyn.suraksha.UpgradeContinuityCheck#seedV010','com.narvyn.suraksha.test/androidx.test.runner.AndroidJUnitRunner');(a.out/'seed.log').write_text(text);assert 'OK (1 test)' in text,text
finally:run('install','-r',str(new))
text=run('shell','am','instrument','-w','-e','class','com.narvyn.suraksha.UpgradeContinuityCheck#verifyV011','com.narvyn.suraksha.test/androidx.test.runner.AndroidJUnitRunner');(a.out/'verify.log').write_text(text);assert 'OK (1 test)' in text,text
print('PASS: profile and original procedure evidence survived an in-place debug upgrade; no production-signing claim.')
