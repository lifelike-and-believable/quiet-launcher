"""Tests for quiet-setup.ps1 and quiet-undo.ps1 (docs/specs/setup-scripts.md), run against a fake ADB.

Run with: python -m unittest discover -s scripts/tests   (needs PowerShell 7 as `pwsh`)
"""
import glob
import json
import os
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
SCRIPTS = HERE.parent
SETUP = SCRIPTS / "quiet-setup.ps1"
UNDO = SCRIPTS / "quiet-undo.ps1"
FAKE_ADB = HERE / "fake-adb.ps1"
PWSH = os.environ.get("QUIET_PWSH", "pwsh")
SERVICE = "quiet.launcher/.HomeWatcher"
ADDRESS = "10.0.0.202:39423"


def tv(**changes):
    """A pretend TV, paired and connected, set up the way the reference device was."""
    state = dict(
        reachable=True, paired=True, devices=[ADDRESS], address=ADDRESS,
        pairing_address="10.0.0.202:41234", pairing_code="482913",
        serialno="17241HFDD2WLBM", model="Chromecast", release="14",
        settings={"enabled_accessibility_services": None, "accessibility_enabled": "0"},
        installed=None, apk_version="1.0.0", install_failure=None, settings_locked=False,
        launcherx_enabled=True, service_runs=True, covers=True,
        top="com.google.android.apps.tv.launcherx/.home.HomeActivity", screen_on_sdcard=False, calls=[],
    )
    state.update(changes)
    return state


class Pc:
    """A PC with a Quiet Launcher folder, an install file, and the fake ADB wired to a pretend TV."""

    def __init__(self, test, state):
        self.dir = Path(tempfile.mkdtemp(prefix="quiet-"))
        test.addCleanup(shutil.rmtree, self.dir, True)
        self.folder = self.dir / "QuietLauncher"
        self.apk = self.dir / "quiet-launcher.apk"
        self.apk.write_bytes(b"APK version one")
        self.state_file = self.dir / "tv.json"
        self.state_file.write_text(json.dumps(state))

    @property
    def tv(self):
        return json.loads(self.state_file.read_text(encoding="utf-8-sig"))

    def update_tv(self, **changes):
        s = self.tv
        s.update(changes)
        self.state_file.write_text(json.dumps(s))

    def run(self, script, *args, answers=(), adb=None, shell=PWSH, adb_on_path=False):
        env = dict(os.environ)
        env.update(QUIET_ADB=str(adb or FAKE_ADB), QUIET_FAKE_STATE=str(self.state_file),
                   QUIET_FOLDER=str(self.folder), QUIET_ANSWERS="|".join(answers), QUIET_FAST="1")
        if adb_on_path:
            # As a set-up person has it: no QUIET_ADB, and an adb program on PATH.
            del env["QUIET_ADB"]
            env["PATH"] = str(self.adb_program()) + os.pathsep + env.get("PATH", "")
        p = subprocess.run([shell, "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", str(script), *args],
                           capture_output=True, text=True, env=env, timeout=120)
        return p.returncode, p.stdout + p.stderr

    def setup(self, *extra, answers=("y", "y"), **kw):
        return self.run(SETUP, "-Apk", str(self.apk), *extra, answers=answers, **kw)

    def undo(self, *extra, answers=("y", "y"), **kw):
        return self.run(UNDO, *extra, answers=answers, **kw)

    def adb_program(self):
        """A folder with an adb program (adb.cmd on Windows, an adb shell script elsewhere) that runs the fake."""
        bin_dir = self.dir / "bin"
        bin_dir.mkdir(exist_ok=True)
        if os.name == "nt":
            (bin_dir / "adb.cmd").write_text(
                f'@"{shutil.which(PWSH) or PWSH}" -NoProfile -ExecutionPolicy Bypass -File "{FAKE_ADB}" %*\r\n')
        else:
            program = bin_dir / "adb"
            program.write_text(f'#!/bin/sh\nexec "{shutil.which(PWSH) or PWSH}" -NoProfile -File "{FAKE_ADB}" "$@"\n')
            program.chmod(0o755)
        return bin_dir

    def backup(self):
        files = list(self.folder.glob("backup-*.json"))
        files = [f for f in files if "-undone-" not in f.name]
        return json.loads(files[0].read_text(encoding="utf-8-sig")) if files else None

    def log(self, kind="setup"):
        logs = sorted(self.folder.glob(f"{kind}-*.log"))
        return logs[-1].read_text(encoding="utf-8-sig") if logs else ""


class test_setup_errors(unittest.TestCase):
    # setup-scripts#AC-1
    def test_without_adb_it_stops_and_changes_nothing(self):
        pc = Pc(self, tv())
        code, out = pc.setup(adb=pc.dir / "no-such-adb.exe")
        self.assertNotEqual(code, 0)
        self.assertIn("[1/6] ADB: NOT FOUND", out)
        self.assertIn("winget install Google.PlatformTools", out)
        self.assertEqual(pc.tv["calls"], [])

    # setup-scripts#AC-1: ADB found on PATH, as a set-up person has it (both scripts have a
    # function named Adb, which Get-Command would find first).
    def test_it_finds_adb_on_the_path(self):
        pc = Pc(self, tv())
        code, out = pc.setup(adb_on_path=True)
        self.assertNotIn("ADB: NOT FOUND", out)
        self.assertIn("[1/6] ADB: found", out)
        self.assertEqual(code, 0, out)
        code, out = pc.undo(adb_on_path=True)
        self.assertNotIn("ADB: NOT FOUND", out)
        self.assertEqual(code, 0, out)

    # setup-scripts#AC-1
    def test_without_apk_it_says_how_to_give_it(self):
        pc = Pc(self, tv())
        code, out = pc.run(SETUP)
        self.assertNotEqual(code, 0)
        self.assertIn("Give the app's install file with -Apk, for example: -Apk .\\quiet-launcher.apk", out)


class test_setup_tv(unittest.TestCase):
    # setup-scripts#AC-2
    def test_it_pairs_when_the_tv_is_not_paired(self):
        pc = Pc(self, tv(paired=False, devices=[]))
        code, out = pc.setup(answers=("10.0.0.202:41234", "482913", "y", "y"))
        self.assertEqual(code, 0, out)
        self.assertIn("[2/6] TV: not paired with this PC", out)
        self.assertIn("Pair device with pairing code", out)
        self.assertIn(f"Paired. Connected to {ADDRESS} (Chromecast, Android 14)", out)

    # setup-scripts#AC-2
    def test_a_wrong_code_is_asked_again(self):
        pc = Pc(self, tv(paired=False, devices=[]))
        code, out = pc.setup(answers=("10.0.0.202:41234", "000000", "10.0.0.202:41234", "482913", "y", "y"))
        self.assertEqual(code, 0, out)
        self.assertEqual(out.count("TV: PAIRING FAILED (wrong or expired code, or wrong address)"), 1)
        self.assertIn("Use the address shown under the code", out)

    # setup-scripts#AC-2
    def test_three_wrong_codes_stop_it(self):
        pc = Pc(self, tv(paired=False, devices=[]))
        code, out = pc.setup(answers=("a:1", "1", "a:1", "2", "a:1", "3"))
        self.assertNotEqual(code, 0)
        self.assertEqual(out.count("PAIRING FAILED"), 3)
        self.assertFalse(any(c.startswith("install") or " install " in c for c in pc.tv["calls"]))

    # setup-scripts#AC-2
    def test_an_unreachable_tv_could_not_connect(self):
        pc = Pc(self, tv(reachable=False, devices=[]))
        code, out = pc.setup("-Tv", ADDRESS)
        self.assertNotEqual(code, 0)
        self.assertIn("[2/6] TV: COULD NOT CONNECT", out)
        self.assertIn("Wireless debugging is on", out)

    # setup-scripts#AC-2
    def test_more_than_one_device_lists_them(self):
        pc = Pc(self, tv(devices=[ADDRESS, "emulator-5554"], serials={"emulator-5554": "EMULATOR36X1X18X0"}))
        code, out = pc.setup()
        self.assertNotEqual(code, 0)
        self.assertIn("[2/6] TV: MORE THAN ONE DEVICE", out)
        self.assertIn("emulator-5554", out)
        self.assertIn(f"-Tv {ADDRESS}", out)

    # setup-scripts#AC-2: ADB lists one TV twice, by address and by mDNS name (seen on the
    # reference device); that is one TV, used by its address.
    def test_the_same_tv_listed_twice_is_one_tv(self):
        mdns = "adb-17241HFDD2WLBM-LWjXi8._adb-tls-connect._tcp"
        for devices in ([ADDRESS, mdns], [mdns, ADDRESS]):
            pc = Pc(self, tv(devices=devices))
            code, out = pc.setup()
            self.assertNotIn("MORE THAN ONE DEVICE", out)
            self.assertIn(f"[2/6] TV: connected to {ADDRESS}", out)
            self.assertEqual(code, 0, out)
            code, out = pc.undo()
            self.assertNotIn("MORE THAN ONE DEVICE", out)
            self.assertIn(f"TV: connected to {ADDRESS}", out)
            self.assertEqual(code, 0, out)


class test_setup_backup(unittest.TestCase):
    # setup-scripts#AC-3
    def test_first_setup_saves_the_settings_and_a_picture(self):
        pc = Pc(self, tv(settings={"enabled_accessibility_services": None, "accessibility_enabled": "0"}))
        code, out = pc.setup()
        self.assertEqual(code, 0, out)
        self.assertIn("[3/6] Backup: saved to", out)
        b = pc.backup()
        self.assertEqual(b["enabled_accessibility_services"], "null")
        self.assertEqual(b["accessibility_enabled"], "0")
        self.assertTrue((pc.folder / "before-17241HFDD2WLBM.png").exists())
        self.assertIn("Picture of Google TV's home screen:", out)

    # setup-scripts#AC-3
    def test_a_later_run_keeps_the_first_backup(self):
        pc = Pc(self, tv())
        pc.setup()
        first = pc.backup()
        code, out = pc.setup()
        self.assertEqual(code, 0, out)
        self.assertIn(f"[3/6] Backup: kept the one from {first['date']}", out)
        self.assertEqual(pc.backup(), first)

    # setup-scripts#AC-3
    def test_with_the_service_already_on_it_is_left_out_and_no_picture(self):
        pc = Pc(self, tv(settings={"enabled_accessibility_services": f"com.other/.Svc:{SERVICE}",
                                   "accessibility_enabled": "1"}))
        code, out = pc.setup()
        self.assertEqual(code, 0, out)
        self.assertEqual(pc.backup()["enabled_accessibility_services"], "com.other/.Svc")
        self.assertIn("Picture skipped: the quiet home screen is already on.", out)
        self.assertFalse((pc.folder / "before-17241HFDD2WLBM.png").exists())


class test_setup_install(unittest.TestCase):
    # setup-scripts#AC-4
    def test_it_installs_the_app(self):
        pc = Pc(self, tv())
        code, out = pc.setup()
        self.assertEqual(code, 0, out)
        self.assertIn("[4/6] Install: installed quiet-launcher 1.0.0", out)
        self.assertIsNotNone(pc.tv["installed"])

    # setup-scripts#AC-4
    def test_the_same_install_file_is_skipped(self):
        pc = Pc(self, tv())
        pc.setup()
        installs = lambda: sum(1 for c in pc.tv["calls"] if " install -r " in f" {c} ")
        before = installs()
        code, out = pc.setup()
        self.assertEqual(code, 0, out)
        self.assertIn("[4/6] Install: already installed (1.0.0); skipped", out)
        self.assertEqual(installs(), before)

    # setup-scripts#AC-4
    def test_a_failed_install_stops_with_nothing_changed(self):
        pc = Pc(self, tv(install_failure="Failure [INSTALL_FAILED_INSUFFICIENT_STORAGE]"))
        code, out = pc.setup()
        self.assertNotEqual(code, 0)
        self.assertIn("[4/6] Install: FAILED: Failure [INSTALL_FAILED_INSUFFICIENT_STORAGE]", out)
        self.assertIn("Nothing on the TV was changed. A backup was saved on this PC. Run this again.", out)
        self.assertIsNone(pc.tv["settings"].get("enabled_accessibility_services"))


class test_setup_turn_on(unittest.TestCase):
    # setup-scripts#AC-5
    def test_it_adds_the_service_and_keeps_others(self):
        pc = Pc(self, tv(settings={"enabled_accessibility_services": "com.other/.Svc", "accessibility_enabled": "1"}))
        code, out = pc.setup()
        self.assertEqual(code, 0, out)
        self.assertIn("[5/6] Turn on: accessibility service on", out)
        self.assertEqual(pc.tv["settings"]["enabled_accessibility_services"], f"com.other/.Svc:{SERVICE}")
        self.assertEqual(pc.tv["settings"]["accessibility_enabled"], "1")

    # setup-scripts#AC-5
    def test_after_an_update_turned_it_off_it_turns_it_back_on(self):
        pc = Pc(self, tv())
        pc.setup()
        pc.update_tv(settings={"enabled_accessibility_services": None, "accessibility_enabled": "0"})
        code, out = pc.setup()
        self.assertEqual(code, 0, out)
        self.assertIn("[5/6] Turn on: was off; turned it back on", out)
        self.assertEqual(pc.tv["settings"]["enabled_accessibility_services"], SERVICE)

    # setup-scripts#AC-5
    def test_already_on_says_so(self):
        pc = Pc(self, tv())
        pc.setup()
        code, out = pc.setup()
        self.assertEqual(code, 0, out)
        self.assertIn("[5/6] Turn on: already on", out)


class test_setup_checks(unittest.TestCase):
    # setup-scripts#AC-6
    def test_all_three_checks_pass(self):
        pc = Pc(self, tv())
        code, out = pc.setup()
        self.assertEqual(code, 0, out)
        self.assertIn("Google TV home app installed and enabled: yes", out)
        self.assertIn("Quiet Launcher service running: yes", out)
        self.assertRegex(out, r"Google TV's home screen covered when it opens: yes \(\d+(\.\d+)? s\)")

    # setup-scripts#AC-6
    def test_google_tv_disabled_is_reported(self):
        pc = Pc(self, tv(launcherx_enabled=False))
        code, out = pc.setup()
        self.assertNotEqual(code, 0)
        self.assertIn("Google TV home app installed and enabled: NO", out)
        self.assertIn("Re-enable", out)

    # setup-scripts#AC-6
    def test_a_service_that_does_not_start_is_reported(self):
        pc = Pc(self, tv(service_runs=False))
        code, out = pc.setup()
        self.assertNotEqual(code, 0)
        self.assertIn("Quiet Launcher service running: NO", out)
        self.assertIn("Restart the TV", out)

    # setup-scripts#AC-6
    def test_not_covering_is_reported(self):
        pc = Pc(self, tv(covers=False))
        code, out = pc.setup()
        self.assertNotEqual(code, 0)
        self.assertIn("Google TV's home screen covered when it opens: FAILED", out)
        self.assertIn("open an issue", out)


class test_setup_remote_checks(unittest.TestCase):
    # setup-scripts#AC-7
    def test_the_answers_go_in_the_log(self):
        pc = Pc(self, tv())
        code, out = pc.setup(answers=("y", "y"))
        self.assertEqual(code, 0, out)
        log = pc.log()
        self.assertRegex(log, r"Press Home\..*\(y/n\): y")
        self.assertRegex(log, r"Search for \"Severance\"\..*\(y/n\): y")

    # setup-scripts#AC-7
    def test_search_answered_n(self):
        pc = Pc(self, tv())
        code, out = pc.setup(answers=("y", "n"))
        self.assertNotEqual(code, 0)
        self.assertIn("Check the TV's internet connection; if it is fine, run quiet-undo.ps1 and open an issue with the log.", out)

    # setup-scripts#AC-7
    def test_home_answered_n(self):
        pc = Pc(self, tv())
        code, out = pc.setup(answers=("n", "y"))
        self.assertNotEqual(code, 0)
        self.assertIn("Run quiet-undo.ps1 and open an issue with the log.", out)

    # setup-scripts#AC-7
    def test_nothing_to_do_says_nothing_changed(self):
        pc = Pc(self, tv())
        pc.setup()
        code, out = pc.setup(answers=())
        self.assertEqual(code, 0, out)
        self.assertIn("Nothing changed.", out)
        self.assertNotIn("(y/n)", out)


class test_undo(unittest.TestCase):
    # setup-scripts#AC-8
    def test_undo_restores_the_backup_and_removes_the_app(self):
        pc = Pc(self, tv())
        pc.setup()
        code, out = pc.undo()
        self.assertEqual(code, 0, out)
        self.assertIn("[2/4] Accessibility settings: restored from backup-17241HFDD2WLBM.json", out)
        self.assertIsNone(pc.tv["settings"].get("enabled_accessibility_services"))
        self.assertEqual(pc.tv["settings"]["accessibility_enabled"], "0")
        self.assertEqual(len(list(pc.folder.glob("backup-17241HFDD2WLBM-undone-*.json"))), 1)
        self.assertIsNone(pc.backup())
        self.assertIn("[3/4] Remove: uninstalled quiet-launcher", out)
        self.assertIsNone(pc.tv["installed"])
        self.assertIn("Android's home screen is Google TV's: yes", out)
        self.assertIn("Google TV home app installed and enabled: yes", out)
        self.assertRegex(pc.log("undo"), r"Press Home\. Did Google TV's home screen appear\? \(y/n\): y")

    # setup-scripts#AC-8
    def test_undo_with_the_app_already_gone(self):
        pc = Pc(self, tv())
        pc.setup()
        pc.update_tv(installed=None)
        code, out = pc.undo()
        self.assertEqual(code, 0, out)
        self.assertIn("[3/4] Remove: already removed", out)

    # setup-scripts#AC-9
    def test_undo_without_a_backup_turns_off_only_this_service(self):
        pc = Pc(self, tv(settings={"enabled_accessibility_services": f"com.other/.Svc:{SERVICE}",
                                   "accessibility_enabled": "1"}, installed={"version": "1.0.0", "sha": "x"}))
        code, out = pc.undo()
        self.assertEqual(code, 0, out)
        self.assertIn("No backup on this PC: turned off only Quiet Launcher's service and left any others as they were.", out)
        self.assertEqual(pc.tv["settings"]["enabled_accessibility_services"], "com.other/.Svc")


class test_common(unittest.TestCase):
    # setup-scripts#AC-10
    def test_everything_printed_is_in_the_log(self):
        pc = Pc(self, tv())
        code, out = pc.setup()
        self.assertEqual(code, 0, out)
        log = pc.log()
        for line in ("[1/6] ADB: found", "[4/6] Install: installed quiet-launcher 1.0.0", "Done in"):
            self.assertIn(line, log)

    # setup-scripts#AC-10
    def test_help_has_a_synopsis_options_and_examples(self):
        for script in (SETUP, UNDO):
            p = subprocess.run([PWSH, "-NoProfile", "-Command", f"Get-Help '{script}' -Full | Out-String -Width 200"],
                               capture_output=True, text=True, timeout=60)
            self.assertIn("SYNOPSIS", p.stdout)
            self.assertIn("-Tv", p.stdout)
            self.assertIn("-Pair", p.stdout)
            self.assertIn("EXAMPLE", p.stdout)
        p = subprocess.run([PWSH, "-NoProfile", "-Command", f"Get-Help '{SETUP}' -Full | Out-String -Width 200"],
                           capture_output=True, text=True, timeout=60)
        self.assertIn("-Apk", p.stdout)

    # setup-scripts#AC-10
    @unittest.skipUnless(shutil.which("powershell"), "Windows PowerShell 5.1 is only on Windows")
    def test_it_runs_in_windows_powershell_5(self):
        pc = Pc(self, tv())
        code, out = pc.setup(shell="powershell")
        self.assertEqual(code, 0, out)
        code, out = pc.undo(shell="powershell")
        self.assertEqual(code, 0, out)


if __name__ == "__main__":
    unittest.main()
