#!/usr/bin/env python3
"""Apply the small Molla branding overlay to an exact, clean upstream checkout.

No protocol, parser, transport, routing, or VPN-service implementation is replaced.
The generated Android application retains upstream's GPL-3.0 license.
"""
from __future__ import annotations

import argparse
import base64
import hashlib
import html
import json
from pathlib import Path
import subprocess
import urllib.request

HERE = Path(__file__).resolve().parent
LOCK = json.loads((HERE / 'upstream.lock.json').read_text())
REPOSITORY = 'https://github.com/hidooch980/MLH-VPN'


def git(root: Path, *args: str) -> str:
    return subprocess.check_output(['git', '-C', str(root), *args], text=True).strip()


def replace_once(text: str, old: str, new: str) -> str:
    if text.count(old) != 1:
        raise ValueError(f'Upstream source changed: expected one occurrence of {old!r}')
    return text.replace(old, new, 1)


def prepare(root: Path) -> None:
    if git(root, 'rev-parse', 'HEAD') != LOCK['commit']:
        raise ValueError('Wrong upstream commit; use upstream.lock.json')
    if git(root, 'status', '--porcelain', '--untracked-files=no'):
        raise ValueError('Refusing to modify an already modified upstream checkout')
    project = root / 'V2rayNG'
    changes: dict[Path, str] = {}
    gradle = project / 'app/build.gradle.kts'
    content = gradle.read_text()
    for old, new in [
        ('applicationId = "com.v2ray.ang"', f'applicationId = "{LOCK["application_id"]}"'),
        ('versionCode = 755', f'versionCode = {LOCK["application_version_code"]}'),
        ('versionName = "2.3.15"', f'versionName = "{LOCK["application_version"]}"'),
        ('compileSdk = 37', f'compileSdk = 37\n    ndkVersion = "{LOCK["ndk"]}"'),
    ]:
        content = replace_once(content, old, new)
    content = content.replace('"v2rayNG_${variant.versionName}', '"MollaVPN_${variant.versionName}')
    changes[gradle] = content

    for flavor, old_name, new_name in [
        ('main', 'v2rayNG', 'Molla VPN'),
        ('fdroid', 'v2rayNG (F-Droid)', 'Molla VPN'),
    ]:
        path = project / f'app/src/{flavor}/res/values/strings.xml'
        changes[path] = replace_once(path.read_text(), f'>{old_name}</string>', f'>{new_name}</string>')

    path = project / 'app/src/main/java/com/v2ray/ang/AppConfig.kt'
    content = path.read_text()
    for old, new in [
        ('const val APP_URL = "$GITHUB_URL/2dust/v2rayNG"', f'const val APP_URL = "{REPOSITORY}"'),
        ('const val APP_API_URL = "https://api.github.com/repos/2dust/v2rayNG/releases"',
         'const val APP_API_URL = "https://api.github.com/repos/hidooch980/MLH-VPN/releases"'),
        ('const val APP_WIKI_MODE = "$APP_URL/wiki/Mode"',
         'const val APP_WIKI_MODE = "https://github.com/2dust/v2rayNG/wiki/Mode"'),
        ('const val APP_PRIVACY_POLICY = "$GITHUB_RAW_URL/2dust/v2rayNG/master/CR.md"',
         f'const val APP_PRIVACY_POLICY = "{REPOSITORY}/blob/main/apps/android/PRIVACY.md"'),
        ('const val APP_PROMOTION_URL = "aHR0cHM6Ly85LjIzNDQ1Ni54eXovYWJjLmh0bWw="',
         f'const val APP_PROMOTION_URL = "{base64.b64encode(REPOSITORY.encode()).decode()}"'),
    ]:
        content = replace_once(content, old, new)
    changes[path] = content

    # Validate all replacements before writing any source file. Keep Kotlin namespace
    # unchanged: the HEV JNI entry points depend on com.v2ray.ang.service.
    for path, content in changes.items():
        path.write_text(content)
    assets = project / 'app/src/main/assets'
    assets.mkdir(parents=True, exist_ok=True)
    (assets / 'molla_upstream_license.txt').write_text((root / 'LICENSE').read_text())
    (assets / 'molla_build.json').write_text(json.dumps(LOCK, indent=2) + '\n')
    print(f'Prepared Molla VPN {LOCK["application_version"]} from v2rayNG {LOCK["version"]}')


def download_core(root: Path) -> None:
    if git(root / 'AndroidLibXrayLite', 'rev-parse', 'HEAD') != LOCK['core_commit']:
        raise ValueError('Unexpected native core source revision')
    if git(root / 'hev-socks5-tunnel', 'rev-parse', 'HEAD') != LOCK['hev_commit']:
        raise ValueError('Unexpected HEV source revision')
    target = root / 'V2rayNG/app/libs/libv2ray.aar'
    if target.exists() and hashlib.sha256(target.read_bytes()).hexdigest() == LOCK['core_sha256']:
        return
    target.parent.mkdir(parents=True, exist_ok=True)
    temporary = target.with_suffix('.download')
    try:
        with urllib.request.urlopen(LOCK['core_url'], timeout=120) as response, temporary.open('wb') as output:
            while chunk := response.read(1024 * 1024):
                output.write(chunk)
        if hashlib.sha256(temporary.read_bytes()).hexdigest() != LOCK['core_sha256']:
            raise ValueError('Native core checksum mismatch')
        temporary.replace(target)
    finally:
        temporary.unlink(missing_ok=True)


def notices(root: Path) -> None:
    """Append original app license after Gradle generates third-party notices."""
    path = root / 'V2rayNG/app/src/main/assets/open_source_licenses.html'
    current = path.read_text() if path.exists() else '<html><body></body></html>'
    marker = '<!-- Molla VPN notices -->'
    if marker in current:
        return
    notice = (marker + '<section><h2>Molla VPN / v2rayNG</h2>'
              '<p>Molla VPN is a modified build of v2rayNG, copyright its contributors. '
              'Branding/build modifications: Molla VPN contributors, 2026-11-13. '
              'Distributed under GNU GPL version 3, WITHOUT ANY WARRANTY. '
              'You may modify and redistribute it under that license.</p>'
              f'<p>Corresponding source and build scripts: {html.escape(REPOSITORY)}. '
              f'Upstream revision: {LOCK["commit"]}.</p><pre>'
              + html.escape((root / 'LICENSE').read_text()) + '</pre></section>')
    path.write_text(current.replace('</body>', notice + '</body>') if '</body>' in current else current + notice)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('checkout', type=Path)
    parser.add_argument('--download-core', action='store_true')
    parser.add_argument('--notices', action='store_true')
    args = parser.parse_args()
    if args.download_core:
        download_core(args.checkout.resolve())
    elif args.notices:
        notices(args.checkout.resolve())
    else:
        prepare(args.checkout.resolve())
