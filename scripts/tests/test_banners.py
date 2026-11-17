"""Tests for quiet-arrange.ps1 -Banners and quiet-banners.cmd (docs/specs/tv-banners.md, AC-4, AC-5),
run against the fake ADB.

Run with: python -m unittest discover -s scripts/tests   (needs PowerShell 7 as `pwsh`)
"""
import json
import unittest

from test_arrange import APPS, ARRANGE, INSTALLED, PKG, SCRIPTS, Arranging, arranged_tv
from test_scripts import ADDRESS, tv

# Stand-ins for PNGs: the script passes them on as they are.
PNGS = {PKG["YouTube"]: "iVBORyoutube", PKG["Netflix"]: "iVBORnetflix", PKG["Prime Video"]: "iVBORprime",
        PKG["Tubi"]: None, PKG["Google Play Store"]: "iVBORplay"}


def banners_tv(**changes):
    return arranged_tv(arrangement={"version": 1, "order": [], "hidden": [PKG["Netflix"]]}, banners=PNGS, **changes)


def banner_calls(a):
    return [c for c in a.tv["calls"] if "GET_BANNER" in c]


class test_banners(unittest.TestCase):
    # tv-banners#AC-4
    def test_it_saves_every_apps_banner_in_one_file(self):
        a = Arranging(self, banners_tv())
        code, out = a.run("-Banners", answers=())
        self.assertEqual(code, 0, out)
        saved = json.loads((a.pc.folder / "tv-banners.json").read_text(encoding="utf-8-sig"))
        self.assertEqual("Quiet Launcher TV", saved["from"])
        self.assertEqual(
            [{"name": "YouTube", "package": PKG["YouTube"], "picture": "data:image/png;base64,iVBORyoutube"},
             {"name": "Prime Video", "package": PKG["Prime Video"], "picture": "data:image/png;base64,iVBORprime"},
             {"name": "Google Play Store", "package": PKG["Google Play Store"], "picture": "data:image/png;base64,iVBORplay"},
             {"name": "Netflix", "package": PKG["Netflix"], "picture": "data:image/png;base64,iVBORnetflix"}],
            saved["apps"],
        )
        self.assertEqual(len(APPS), len(banner_calls(a)), "one question per app, hidden ones too")

    # tv-banners#AC-4
    def test_it_says_what_it_saved_and_what_to_do_next(self):
        a = Arranging(self, banners_tv())
        code, out = a.run("-Banners", answers=())
        self.assertEqual(code, 0, out)
        self.assertIn("Quiet Launcher: TV banners for the Chromebook", out)
        self.assertIn(f"[1/2] TV: connected to {ADDRESS}", out)
        self.assertIn(f"[2/2] Banners: saved 4 of 5 apps' banners in {a.pc.folder / 'tv-banners.json'}", out)
        self.assertIn("      No banner: Tubi", out)
        self.assertIn("      Copy this file to the Chromebook (with Google Drive or a USB stick). Then, on the", out)
        self.assertIn("      launcher, choose Edit services, then Import TV pictures, and pick the file.", out)

    # tv-banners#AC-4
    def test_it_changes_nothing_on_the_tv(self):
        a = Arranging(self, banners_tv())
        before = a.tv
        a.run("-Banners", answers=())
        after = a.tv
        self.assertEqual(before["arrangement"], after["arrangement"])
        self.assertEqual([], a.sets())
        self.assertIsNone(a.seen_lines(), "nothing is opened in Notepad")
        self.assertFalse(any("install" in c or "settings put" in c for c in after["calls"]))

    # tv-banners#AC-5
    def test_an_older_tv_app_saves_nothing_and_says_to_update_it(self):
        a = Arranging(self, arranged_tv())  # no "banners": the app from before this slice
        code, out = a.run("-Banners", answers=())
        self.assertNotEqual(code, 0)
        self.assertIn("[2/2] Banners: the quiet launcher on this TV is too old to send its banners.", out)
        self.assertIn("      Update it with quiet-setup, then run this again.", out)
        self.assertFalse((a.pc.folder / "tv-banners.json").exists())

    # tv-banners#AC-4: on the TV, the app hung drawing Disney+'s banner and the script took the
    # missing answer for an older app, saving nothing.
    def test_an_app_with_no_answer_is_named_and_the_others_are_saved(self):
        a = Arranging(self, banners_tv(no_answer=[PKG["Prime Video"]]))
        code, out = a.run("-Banners", answers=())
        self.assertEqual(code, 0, out)
        saved = json.loads((a.pc.folder / "tv-banners.json").read_text(encoding="utf-8-sig"))
        self.assertEqual(["YouTube", "Google Play Store", "Netflix"], [app["name"] for app in saved["apps"]])
        self.assertIn(f"[2/2] Banners: saved 3 of 5 apps' banners in {a.pc.folder / 'tv-banners.json'}", out)
        self.assertIn("      No answer from the TV: Prime Video", out)
        self.assertIn("      No banner: Tubi", out)
        self.assertNotIn("too old", out)
        self.assertEqual(len(APPS), len(banner_calls(a)), "it goes on to the other apps")

    # tv-banners#AC-5
    def test_without_the_app_it_says_to_run_setup(self):
        a = Arranging(self, tv(installed=None, apps=APPS, arrangement=None, banners=PNGS))
        code, out = a.run("-Banners", answers=())
        self.assertNotEqual(code, 0)
        self.assertIn("[1/2] TV: the quiet home screen is not set up on this TV. Run quiet-setup first.", out)
        self.assertFalse((a.pc.folder / "tv-banners.json").exists())

    # tv-banners#AC-4: the launcher runs the script with -Banners, as quiet-arrange.cmd runs it.
    def test_the_launcher_runs_banners(self):
        text = (SCRIPTS / "quiet-banners.cmd").read_text()
        self.assertIn('quiet-arrange.ps1" -Banners', text)
        self.assertIn("-ExecutionPolicy Bypass", text)
        self.assertIn("pause", text.lower())
        build = (SCRIPTS.parent / "build.ps1").read_text()
        self.assertIn("'quiet-banners.cmd'", build)


if __name__ == "__main__":
    unittest.main()
