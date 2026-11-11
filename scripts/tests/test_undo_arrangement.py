"""Tests for undo saving the arrangement (docs/specs/undo-arrangement.md), run against the fake ADB."""
import glob
import unittest

from test_arrange import APPS, HEADER, PKG, SERIAL, Arranging, arranged_tv
from test_scripts import Pc, tv


def copies(pc):
    return sorted(glob.glob(str(pc.folder / f"arrangement-{SERIAL}-*.txt")))


class test_undo_arrangement(unittest.TestCase):
    # undo-arrangement#AC-1
    def test_undo_saves_the_arrangement_before_removing_the_app(self):
        pc = Pc(self, arranged_tv(arrangement={"version": 2, "order": [PKG["Prime Video"]], "hidden": [PKG["Tubi"]]}))
        code, out = pc.undo()
        self.assertEqual(code, 0, out)
        saved = copies(pc)
        self.assertEqual(1, len(saved))
        lines = open(saved[0], encoding="utf-8-sig").read().splitlines()
        self.assertEqual(HEADER + ["Prime Video", "YouTube", "Netflix", "Google Play Store", "", "Hidden:", "Tubi"], lines)
        self.assertIn(f"Arrangement of the apps: saved to {saved[0]}", out)
        # Saved before the app was removed: read, then uninstalled.
        calls = pc.tv["calls"]
        read = max(i for i, c in enumerate(calls) if "GET_ARRANGEMENT" in c)
        removed = min(i for i, c in enumerate(calls) if c.endswith("uninstall quiet.launcher"))
        self.assertLess(read, removed)
        self.assertIn("[3/4] Remove: uninstalled quiet-launcher", out)

    # undo-arrangement#AC-2
    def test_never_arranged_saves_nothing(self):
        pc = Pc(self, arranged_tv())
        code, out = pc.undo()
        self.assertEqual(code, 0, out)
        self.assertEqual([], copies(pc))
        self.assertNotIn("Arrangement of the apps", out)

    # undo-arrangement#AC-3
    def test_an_app_that_does_not_answer_is_no_error(self):
        pc = Pc(self, tv(installed=None, apps=APPS, arrangement={"version": 1, "order": [], "hidden": []}))
        code, out = pc.undo()
        self.assertEqual(code, 0, out)
        self.assertIn("[3/4] Remove: already removed", out)
        self.assertNotIn("Arrangement of the apps", out)
        self.assertEqual([], copies(pc))

    # undo-arrangement#AC-4
    def test_quiet_arrange_offers_what_undo_saved_after_a_new_setup(self):
        pc = Pc(self, arranged_tv(arrangement={"version": 4, "order": [PKG["Netflix"]], "hidden": [PKG["Tubi"]]}))
        pc.undo()
        # Set up again: the app is back, never arranged.
        pc.update_tv(installed={"version": "1.0.0", "sha": "abc"}, arrangement=None)
        a = Arranging.__new__(Arranging)
        a.pc = pc
        a.editor = pc.dir / "editor.ps1"
        a.editor.write_text("param($f)\n")
        a.seen = pc.dir / "seen.txt"
        code, out = a.run(answers=("y",))
        self.assertEqual(code, 0, out)
        self.assertIn("[2/3] Arrangement: the apps on this TV have not been arranged.", out)
        self.assertIn("Put back the arrangement saved on", out)
        self.assertEqual([PKG["Netflix"], PKG["YouTube"], PKG["Prime Video"], PKG["Google Play Store"]], pc.tv["last_sent"]["order"])
        self.assertEqual([PKG["Tubi"]], pc.tv["last_sent"]["hidden"])


if __name__ == "__main__":
    unittest.main()
