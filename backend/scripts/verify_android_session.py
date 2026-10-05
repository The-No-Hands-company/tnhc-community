#!/usr/bin/env python3
"""Exercise invitation import and session restoration on a dedicated local emulator.

Requires a backend-configured debug APK and its instrumentation APK installed.
Creates synthetic accounts only on the local Supabase CLI stack, then removes them.
Clears this app's data on the selected emulator before testing.
"""
import argparse
import json
import re
import subprocess
import urllib.error
import urllib.parse
import urllib.request
import uuid
from datetime import datetime, timedelta, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--adb', default=str(ROOT / '.local-tools/android-sdk/platform-tools/adb'))
parser.add_argument('--supabase', default='supabase')
parser.add_argument('--serial', default='emulator-5554')
args = parser.parse_args()
if not args.serial.startswith('emulator-'):
    parser.error('Use a dedicated emulator; this test clears the app data.')

status = subprocess.run([args.supabase, 'status', '--workdir', str(ROOT / 'backend'), '-o', 'json'],
                        check=True, capture_output=True, text=True)
keys = json.loads(status.stdout)
base = keys['API_URL'].rstrip('/')
if urllib.parse.urlparse(base).hostname not in ('127.0.0.1', 'localhost'):
    raise SystemExit('Refusing to create test accounts on a non-local backend.')
service = keys['SERVICE_ROLE_KEY']
headers = {'apikey': service, 'Authorization': 'Bearer ' + service, 'Content-Type': 'application/json'}


def api(path, payload=None, method=None):
    request = urllib.request.Request(base + path,
        data=None if payload is None else json.dumps(payload).encode(), headers=headers,
        method=method or ('GET' if payload is None else 'POST'))
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            body = response.read()
            return json.loads(body) if body else None
    except urllib.error.HTTPError as error:
        raise RuntimeError(f'Local backend request failed: {request.method} {path.split("?")[0]} HTTP {error.code}') from None


def adb(*command):
    return subprocess.run([args.adb, '-s', args.serial, *command], check=True,
                          capture_output=True, text=True, timeout=120).stdout


def instrument(phase, member, link=None):
    command = ['shell', 'am', 'instrument', '-w', '-e', 'class',
               'com.tnhc.community.LocalBackendSmokeTest#invitedSessionAndFollowSurviveProcessRestart',
               '-e', 'localBackendPhase', phase, '-e', 'localBackendMemberId', member]
    if link:
        # adb shell receives a command string; quote the URI's ampersands and tokens.
        import shlex
        command += ['-e', 'localBackendInviteUri', shlex.quote(link)]
    output = adb(*command, 'com.tnhc.community.debug.test/androidx.test.runner.AndroidJUnitRunner')
    if 'OK (1 test)' not in output:
        # Do not emit instrumentation extras or Auth tokens in failure logs.
        raise RuntimeError(f'Android {phase} phase failed; inspect local instrumentation/logcat privately.')
    print(f'PASS: {phase} phase', flush=True)


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, hdrs, newurl):
        return None


suffix = uuid.uuid4().hex
admin_id = member_id = None
stage = 'creating the synthetic administrator'
failed = None
email = f'android-member-{suffix}@example.test'
try:
    admin = api('/auth/v1/admin/users', {'email': f'android-admin-{suffix}@example.test', 'email_confirm': True})
    admin_id = admin['id']
    stage = 'creating the local invitation'
    api('/rest/v1/invitations', {'normalized_email': email, 'invited_by': admin_id,
        'expires_at': (datetime.now(timezone.utc) + timedelta(hours=1)).isoformat()})
    stage = 'generating the Auth invitation link'
    generated = api('/auth/v1/admin/generate_link', {'type': 'invite', 'email': email,
                                                   'redirect_to': 'tnhccommunity://invite'})
    member_id = generated.get('user', {}).get('id') or generated.get('id')
    if not member_id:
        raise RuntimeError('Auth did not return the ID for its synthetic invited user.')
    # Verify the one-use link without following its custom-scheme redirect.
    stage = 'verifying Auth invitation redirect'
    action = generated['action_link']
    parts = urllib.parse.urlsplit(action)
    local_action = base + parts.path + '?' + parts.query
    try:
        urllib.request.build_opener(NoRedirect).open(local_action, timeout=20)
        raise RuntimeError('Expected Auth to redirect to the Android invite handler.')
    except urllib.error.HTTPError as response:
        link = response.headers.get('Location', '')
        if response.code != 303 or not link.startswith('tnhccommunity://invite#'):
            shape = urllib.parse.urlsplit(link)
            safe_location = f'{shape.scheme}://{shape.netloc}{shape.path}' if shape.scheme else '(missing)'
            raise RuntimeError(f'Auth redirect mismatch: HTTP {response.code}, destination {safe_location}') from None
    stage = 'accepting the invitation on the emulator'
    adb('reverse', 'tcp:54321', 'tcp:54321')
    adb('install', '-r', str(ROOT / 'app/build/outputs/apk/debug/app-debug.apk'))
    adb('install', '-r', str(ROOT / 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'))
    adb('shell', 'pm', 'clear', 'com.tnhc.community.debug')
    instrument('accept', member_id, link)
    # A second am instrument invocation starts a new app process, loading encrypted disk state.
    stage = 'restoring the encrypted session in a fresh app process'
    adb('shell', 'am', 'force-stop', 'com.tnhc.community.debug')
    instrument('restore', member_id)
    stage = 'checking invitation activation'
    if not re.fullmatch(r'[0-9a-fA-F-]{36}', member_id):
        raise RuntimeError('Refusing to inspect an unexpected Auth ID.')
    activation = subprocess.run(
        ['docker', 'exec', 'supabase_db_tnhc-community-local', 'psql', '-U', 'postgres',
         '-d', 'postgres', '-Atqc',
         f"select account_state from public.profiles where id = '{member_id}'::uuid"],
        capture_output=True, text=True, check=True, timeout=20,
    ).stdout.strip()
    if activation != 'active':
        raise RuntimeError('Accepted invitation did not activate its profile.')
    print('PASS: invitation activation, profile/follow retries, process restart, and sign-out', flush=True)
except Exception as error:
    failed = RuntimeError(f'{stage} failed: {error}')
finally:
    # The local REST grant intentionally forbids deleting invitations; clean fixtures in local PostgreSQL.
    if not re.fullmatch(r'android-member-[0-9a-f]{32}@example\.test', email):
        raise RuntimeError('Refusing fixture cleanup for an unexpected address.')
    identifiers = [value for value in (member_id, admin_id) if value]
    if any(not re.fullmatch(r'[0-9a-fA-F-]{36}', value) for value in identifiers):
        raise RuntimeError('Refusing fixture cleanup for an unexpected Auth ID.')
    delete_users = ','.join("'" + value + "'::uuid" for value in identifiers) or "'00000000-0000-0000-0000-000000000000'::uuid"
    sql = (f"delete from public.invitations where normalized_email = '{email}';\n"
           f"delete from auth.users where id in ({delete_users});\n")
    subprocess.run(['docker', 'exec', '-i', 'supabase_db_tnhc-community-local',
                    'psql', '-v', 'ON_ERROR_STOP=1', '-U', 'postgres', '-d', 'postgres'],
                   input=sql, text=True, capture_output=True, check=True, timeout=20)
if failed:
    raise failed
