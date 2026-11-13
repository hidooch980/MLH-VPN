#!/usr/bin/env python3
"""Check that every advertised ABI carries the actual native tunnel and core."""
import argparse
import json
from pathlib import Path
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument('directory', type=Path)
args = parser.parse_args()
expected = {'armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64'}
seen = set()
for apk in sorted(args.directory.glob('*.apk')):
    with zipfile.ZipFile(apk) as z:
        names = set(z.namelist())
        abis = {n.split('/')[1] for n in names if n.startswith('lib/') and n.endswith('.so')}
        assert abis, f'{apk}: no native libraries'
        for abi in abis:
            for library in ('libgojni.so', 'libhev-socks5-tunnel.so', 'libhevsockstun.so'):
                assert f'lib/{abi}/{library}' in names, f'{apk}: missing {abi}/{library}'
        build = json.loads(z.read('assets/mlh_build.json'))
        assert build['application_id'] == 'io.github.hidooch980.mlhvpn'
        assert 'MLH VPN / v2rayNG' in z.read('assets/open_source_licenses.html').decode()
        assert 'GNU GENERAL PUBLIC LICENSE' in z.read('assets/mlh_upstream_license.txt').decode()
        if 'universal' in apk.name:
            assert abis == expected
        seen |= abis
        print(f'{apk.name}: native libraries, build metadata and notices OK ({", ".join(sorted(abis))})')
assert seen == expected, f'Missing APK ABIs: {expected - seen}'
