"""Tests for quiet-arrange.ps1 (docs/specs/arrange-script.md), run against the fake ADB.

Run with: python -m unittest discover -s scripts/tests   (needs PowerShell 7 as `pwsh`)
"""
import glob
import os
import shutil
import subprocess
import unittest
from unittest import mock

from test_scripts import ADDRESS, PWSH, SCRIPTS, Pc, tv

ARRANGE = SCRIPTS / "quiet-arrange.ps1"
LAUNCHER = SCRIPTS / "quiet-arrange.cmd"
SERIAL = "17241HFDD2WLBM"
INSTALLED = {"version": "1.0.0", "sha": "abc"}

APPS = [
    {"name": "YouTube", "package": "com.google.android.youtube.tv"},
    {"name": "Netflix", "package": "com.netflix.ninja"},
    {"name": "Prime Video", "package": "com.amazon.amazonvideo.livingroom"},
    {"name": "Tubi", "package": "com.tubitv"},
    {"name": "Google Play Store", "package": "com.android.vending"},
]
PKG = {a["name"]: a["package"] for a in APPS}

# Stands in for Notepad: keeps a copy of what it was given, puts the test's edit in its place, and
# can change the arrangement on the pretend TV while the file is open.
EDITOR = r'''param($f)
Copy-Item -LiteralPath $f -Destination $env:QUIET_TEST_SEEN
if ($env:QUIET_TEST_EDIT) { Copy-Item -LiteralPath $env:QUIET_TEST_EDIT -Destination $f }
if ($env:QUIET_TEST_BUMP) {
    $s = Get-Content -Raw $env:QUIET_FAKE_STATE | ConvertFrom-Json
    $s.arrangement.version = [long]$s.arrangement.version + 1
    $s | ConvertTo-Json -Depth 6 | Set-Content -Encoding UTF8 $env:QUIET_FAKE_STATE
}
'''

HEADER = [
    "# Quiet Launcher: the order of the apps on the home screen, one per line.",
    "# Move lines to change the order. Apps below \"Hidden:\" are not on the home screen.",
    "# Save the file, then press Enter in the script's window. Lines starting with # are ignored.",
]


def arranged_tv(arrangement=None, apps=APPS, **changes):
    return tv(installed=INSTALLED, apps=apps, arrangement=arrangement, **changes)


class Arranging:
    """A PC with the Quiet Launcher folder and the fake ADB, running quiet-arrange.ps1."""

    def __init__(self, test, state):
        self.pc = Pc(test, state)
        self.editor = self.pc.dir / "editor.ps1"
        self.editor.write_text(EDITOR)
        self.seen = self.pc.dir / "seen.txt"

    def run(self, *args, answers=(" ",), edit=None, bump=False, shell=PWSH):
        env = {"QUIET_EDITOR": str(self.editor), "QUIET_TEST_SEEN": str(self.seen),
               "QUIET_FAKE_STATE": str(self.pc.state_file)}
        if edit is not None:
            path = self.pc.dir / "edit.txt"
            path.write_text("\n".join(edit) + "\n", encoding="utf-8")
            env["QUIET_TEST_EDIT"] = str(path)
        if bump:
            env["QUIET_TEST_BUMP"] = "1"
        with mock.patch.dict(os.environ, env):
            return self.pc.run(ARRANGE, *args, answers=answers, shell=shell)

    @property
    def tv(self):
        return self.pc.tv

    def sets(self):
        return [c for c in self.tv["calls"] if "SET_ARRANGEMENT" in c]

    def seen_lines(self):
        return self.seen.read_text(encoding="utf-8-sig").splitlines() if self.seen.exists() else None

    def copies(self):
        return sorted(glob.glob(str(self.pc.folder / f"arrangement-{SERIAL}-*.txt")))


class test_arrange_errors(unittest.TestCase):
    # arrange-script#AC-1
    def test_without_adb_it_stops_and_sends_nothing(self):
        a = Arranging(self, arranged_tv())
        code, out = a.pc.run(ARRANGE, adb=a.pc.dir / "no-such-adb.exe")
        self.assertNotEqual(code, 0)
        self.assertIn("[1/3] ADB: NOT FOUND", out)
        self.assertIn("winget install Google.PlatformTools", out)
        self.assertEqual(a.tv["calls"], [])

    # arrange-script#AC-1
    def test_without_the_app_it_says_to_run_setup(self):
        a = Arranging(self, tv(installed=None, apps=APPS, arrangement=None))
        code, out = a.run()
        self.assertNotEqual(code, 0)
        self.assertIn("[1/3] TV: the quiet home screen is not set up on this TV. Run quiet-setup first.", out)
        self.assertEqual(a.sets(), [])
        self.assertIsNone(a.seen_lines())

    # arrange-script#AC-1
    def test_the_tv_step_is_setups(self):
        a = Arranging(self, arranged_tv(devices=[ADDRESS, "emulator-5554"], serials={"emulator-5554": "EMU1"}))
        code, out = a.run()
        self.assertNotEqual(code, 0)
        self.assertIn("[1/3] TV: MORE THAN ONE DEVICE", out)
        a = Arranging(self, arranged_tv())
        code, out = a.run()
        self.assertIn(f"[1/3] TV: connected to {ADDRESS}", out)


class test_arrange_file(unittest.TestCase):
    # arrange-script#AC-2
    def test_it_writes_the_arrangement_and_asks_for_enter(self):
        a = Arranging(self, arranged_tv(arrangement={"version": 3, "order": [PKG["Prime Video"]], "hidden": [PKG["Tubi"]]}))
        code, out = a.run()
        self.assertEqual(code, 0, out)
        self.assertEqual(
            HEADER + ["Prime Video", "YouTube", "Netflix", "Google Play Store", "", "Hidden:", "Tubi"],
            a.seen_lines(),
        )
        self.assertTrue((a.pc.folder / f"arrangement-{SERIAL}.txt").exists())
        self.assertIn(f"arrangement-{SERIAL}.txt", out)
        self.assertIn("Save the file, then press Enter here to send it (or type n to send nothing): ", out)


class test_arrange_send(unittest.TestCase):
    # arrange-script#AC-3
    def test_an_edited_file_is_sent_with_the_version_read_and_a_copy_kept(self):
        a = Arranging(self, arranged_tv(arrangement={"version": 2, "order": [], "hidden": []}))
        code, out = a.run(edit=HEADER + ["Prime Video", "YouTube", "Tubi", "Google Play Store", "", "Hidden:", "Netflix"])
        self.assertEqual(code, 0, out)
        sent = a.tv["last_sent"]
        self.assertEqual([PKG[n] for n in ("Prime Video", "YouTube", "Tubi", "Google Play Store")], sent["order"])
        self.assertEqual([PKG["Netflix"]], sent["hidden"])
        self.assertEqual(2, sent["version"])
        self.assertFalse(sent["replace"])
        self.assertIn("[3/3] Sent to the TV: 4 apps shown, 1 hidden. Press Home to see it.", out)
        self.assertEqual(1, len(a.copies()))
        self.assertIn("Copy kept:", out)

    # arrange-script#AC-6
    def test_an_unchanged_file_sends_nothing(self):
        a = Arranging(self, arranged_tv())
        code, out = a.run()
        self.assertEqual(code, 0, out)
        self.assertIn("[3/3] Nothing changed.", out)
        self.assertEqual(a.sets(), [])
        self.assertEqual(a.copies(), [])

    # arrange-script#AC-6
    def test_n_sends_nothing(self):
        a = Arranging(self, arranged_tv())
        code, out = a.run(answers=("n",), edit=HEADER + ["Netflix", "YouTube", "", "Hidden:"])
        self.assertIn("[3/3] Nothing changed.", out)
        self.assertEqual(a.sets(), [])

    # arrange-script#AC-7
    def test_a_change_on_the_tv_meanwhile_is_asked_about_and_n_keeps_it(self):
        a = Arranging(self, arranged_tv(arrangement={"version": 1, "order": [], "hidden": []}))
        code, out = a.run(answers=(" ", "n"), edit=HEADER + ["Netflix", "", "Hidden:"], bump=True)
        self.assertIn("The apps were arranged on the TV since this list was opened.", out)
        self.assertIn("Send your list anyway, replacing what was done on the TV? (y/n): ", out)
        self.assertIn("Nothing changed. Run this again to start from the TV's arrangement.", out)
        self.assertEqual(2, a.tv["arrangement"]["version"])

    # arrange-script#AC-7
    def test_a_change_on_the_tv_meanwhile_is_replaced_with_y(self):
        a = Arranging(self, arranged_tv(arrangement={"version": 1, "order": [], "hidden": []}))
        code, out = a.run(answers=(" ", "y"), edit=HEADER + ["Netflix", "", "Hidden:"], bump=True)
        self.assertEqual(code, 0, out)
        self.assertTrue(a.tv["last_sent"]["replace"])
        self.assertEqual([PKG["Netflix"]], a.tv["arrangement"]["order"])
        self.assertIn("[3/3] Sent to the TV:", out)


class test_arrange_lines(unittest.TestCase):
    # arrange-script#AC-4
    def test_capitals_spaces_notes_and_blank_lines(self):
        a = Arranging(self, arranged_tv())
        code, out = a.run(edit=["# my notes", "  netflix  ", "", "YOUTUBE", "prime video", "tubi", "google play store", "Hidden:", ""])
        self.assertEqual(code, 0, out)
        self.assertEqual([PKG[n] for n in ("Netflix", "YouTube", "Prime Video", "Tubi", "Google Play Store")], a.tv["last_sent"]["order"])

    # arrange-script#AC-4
    def test_a_line_that_is_not_an_app_is_skipped_and_named(self):
        a = Arranging(self, arranged_tv())
        code, out = a.run(edit=HEADER + ["Netflix", "Hulu", "YouTube", "", "Hidden:"])
        self.assertEqual(code, 0, out)
        self.assertIn('Skipped 1 line that is not an app on this TV: "Hulu"', out)
        self.assertEqual([PKG["Netflix"], PKG["YouTube"]], a.tv["last_sent"]["order"])
        # Apps missing from the file stay shown, at the end.
        self.assertIn("Sent to the TV: 5 apps shown, 0 hidden. Press Home to see it.", out)

    # arrange-script#AC-4
    def test_two_apps_with_the_same_name_are_told_apart_by_package(self):
        apps = APPS + [{"name": "Netflix", "package": "com.netflix.kids"}]
        a = Arranging(self, arranged_tv(apps=apps))
        code, out = a.run(edit=HEADER + ["Netflix (com.netflix.kids)", "Netflix (com.netflix.ninja)", "", "Hidden:"])
        self.assertIn("Netflix (com.netflix.ninja)", a.seen_lines())
        self.assertIn("Netflix (com.netflix.kids)", a.seen_lines())
        self.assertEqual(["com.netflix.kids", "com.netflix.ninja"], a.tv["last_sent"]["order"])

    # arrange-script#AC-5
    def test_a_file_without_the_hidden_line_is_not_sent(self):
        a = Arranging(self, arranged_tv())
        code, out = a.run(edit=HEADER + ["Netflix", "YouTube"])
        self.assertNotEqual(code, 0)
        self.assertIn('The file has no "Hidden:" line. Put it back (on a line of its own) and try again.', out)
        self.assertEqual(a.sets(), [])

    # arrange-script#AC-5
    def test_a_file_that_hides_every_app_is_not_sent(self):
        a = Arranging(self, arranged_tv())
        code, out = a.run(edit=HEADER + ["", "Hidden:"] + [x["name"] for x in APPS])
        self.assertNotEqual(code, 0)
        self.assertIn('The file hides every app; keep at least one above "Hidden:".', out)
        self.assertEqual(a.sets(), [])


class test_arrange_put_back(unittest.TestCase):
    def saved(self, a, stamp, lines):
        a.pc.folder.mkdir(parents=True, exist_ok=True)
        (a.pc.folder / f"arrangement-{SERIAL}-{stamp}.txt").write_text("\n".join(lines) + "\n", encoding="utf-8")

    # arrange-script#AC-8
    def test_a_never_arranged_tv_is_offered_the_newest_copy(self):
        a = Arranging(self, arranged_tv())
        self.saved(a, "2026-10-01-090000", HEADER + ["YouTube", "", "Hidden:"])
        self.saved(a, "2026-11-01-120000", HEADER + ["Prime Video", "Netflix", "", "Hidden:", "Tubi"])
        code, out = a.run(answers=("y",))
        self.assertEqual(code, 0, out)
        self.assertIn("[2/3] Arrangement: the apps on this TV have not been arranged.", out)
        self.assertIn("Put back the arrangement saved on 2026-11-01 12:00 (2 apps shown, 1 hidden)? (y/n): ", out)
        self.assertEqual([PKG["Prime Video"], PKG["Netflix"]], a.tv["last_sent"]["order"])
        self.assertEqual([PKG["Tubi"]], a.tv["last_sent"]["hidden"])
        self.assertIsNone(a.seen_lines())

    # arrange-script#AC-8
    def test_n_carries_on_as_usual(self):
        a = Arranging(self, arranged_tv())
        self.saved(a, "2026-11-01-120000", HEADER + ["Prime Video", "", "Hidden:"])
        code, out = a.run(answers=("n", " "))
        self.assertIn("[3/3] Nothing changed.", out)
        self.assertIsNotNone(a.seen_lines())

    # arrange-script#AC-8
    def test_no_offer_once_arranged(self):
        a = Arranging(self, arranged_tv(arrangement={"version": 1, "order": [], "hidden": []}))
        self.saved(a, "2026-11-01-120000", HEADER + ["Prime Video", "", "Hidden:"])
        code, out = a.run()
        self.assertNotIn("Put back the arrangement", out)

    # arrange-script#AC-9
    def test_file_sends_a_given_file_without_notepad(self):
        a = Arranging(self, arranged_tv())
        path = a.pc.dir / "mine.txt"
        path.write_text("\n".join(HEADER + ["Tubi", "YouTube", "", "Hidden:", "Netflix"]) + "\n", encoding="utf-8")
        code, out = a.run("-File", str(path))
        self.assertEqual(code, 0, out)
        self.assertEqual([PKG["Tubi"], PKG["YouTube"]], a.tv["last_sent"]["order"])
        self.assertIsNone(a.seen_lines())


class test_arrange_common(unittest.TestCase):
    # arrange-script#AC-10
    def test_everything_printed_is_in_the_log(self):
        a = Arranging(self, arranged_tv())
        code, out = a.run(edit=HEADER + ["Netflix", "", "Hidden:"])
        self.assertEqual(code, 0, out)
        logs = sorted(a.pc.folder.glob("arrange-*.log"))
        log = logs[-1].read_text(encoding="utf-8-sig")
        for line in ("[1/3] TV: connected to", "[2/3] Arrangement: opened in Notepad", "[3/3] Sent to the TV:", "Done."):
            self.assertIn(line, log)

    # arrange-script#AC-10
    def test_help_has_a_synopsis_options_and_examples(self):
        p = subprocess.run([PWSH, "-NoProfile", "-Command", f"Get-Help '{ARRANGE}' -Full | Out-String -Width 200"],
                           capture_output=True, text=True, timeout=60)
        for word in ("SYNOPSIS", "-File", "-Tv", "-Pair", "EXAMPLE"):
            self.assertIn(word, p.stdout)

    # arrange-script#AC-10
    @unittest.skipUnless(shutil.which("powershell"), "Windows PowerShell 5.1 is only on Windows")
    def test_it_runs_in_windows_powershell_5(self):
        a = Arranging(self, arranged_tv())
        code, out = a.run(edit=HEADER + ["Netflix", "", "Hidden:", "Tubi"], shell="powershell")
        self.assertEqual(code, 0, out)
        self.assertEqual([PKG["Tubi"]], a.tv["last_sent"]["hidden"])

    # arrange-script#AC-11: a file dropped on the launcher is sent; the window waits for a key.
    @unittest.skipUnless(os.name == "nt", "the launcher is for Windows")
    def test_the_launcher_sends_a_dropped_file(self):
        text = LAUNCHER.read_text()
        self.assertIn("-ExecutionPolicy Bypass", text)
        self.assertIn("pause", text.lower())
        a = Arranging(self, arranged_tv())
        path = a.pc.dir / "dropped.txt"
        path.write_text("\n".join(HEADER + ["Tubi", "", "Hidden:"]) + "\n", encoding="utf-8")
        with mock.patch.dict(os.environ, {"QUIET_FAKE_STATE": str(a.pc.state_file), "QUIET_FOLDER": str(a.pc.folder),
                                          "QUIET_FAST": "1", "QUIET_ADB": str(SCRIPTS / "tests" / "fake-adb.ps1")}):
            p = subprocess.run(["cmd", "/c", str(LAUNCHER), str(path)], input="\n", capture_output=True, text=True, timeout=120)
        self.assertEqual([PKG["Tubi"]], a.tv["last_sent"]["order"], p.stdout + p.stderr)


if __name__ == "__main__":
    unittest.main()
