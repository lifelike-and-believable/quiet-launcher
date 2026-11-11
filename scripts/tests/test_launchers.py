"""Tests for the setup and undo launchers (docs/specs/setup-launchers.md), run against the fake ADB."""
import os
import shutil
import subprocess
import unittest
from unittest import mock

from test_scripts import FAKE_ADB, SCRIPTS, SERVICE, Pc, tv

ROOT = SCRIPTS.parent


@unittest.skipUnless(os.name == "nt", "the launchers are for Windows")
class test_launchers(unittest.TestCase):
    def folder(self, pc):
        """A Quiet Launcher folder whose path has a space, as out\\QuietLauncher in C:\\AI Projects has."""
        folder = pc.dir / "Quiet Launcher"
        folder.mkdir()
        for name in ("quiet-setup.ps1", "quiet-setup.cmd", "quiet-undo.ps1", "quiet-undo.cmd"):
            source = SCRIPTS / name
            if source.exists():
                shutil.copy(source, folder)
        shutil.copy(pc.apk, folder / "quiet-launcher.apk")
        return folder

    def launch(self, pc, launcher, *args, answers=("y", "y")):
        env = {"QUIET_ADB": str(FAKE_ADB), "QUIET_FAKE_STATE": str(pc.state_file), "QUIET_FOLDER": str(pc.folder),
               "QUIET_ANSWERS": "|".join(answers), "QUIET_FAST": "1"}
        with mock.patch.dict(os.environ, env):
            # Started from another folder, as Explorer may; the newline answers the pause.
            p = subprocess.run(["cmd", "/c", str(launcher), *args], input="\n", capture_output=True, text=True,
                               timeout=180, cwd=str(pc.dir))
        return p.returncode, p.stdout + p.stderr

    # setup-launchers#AC-1, AC-3
    def test_double_clicking_setup_sets_up_the_tv_with_the_install_file_beside_it(self):
        pc = Pc(self, tv())
        folder = self.folder(pc)
        code, out = self.launch(pc, folder / "quiet-setup.cmd")
        self.assertEqual(code, 0, out)
        self.assertIn("[4/6] Install: installed quiet-launcher 1.0.0", out)
        self.assertIn(SERVICE, pc.tv["settings"]["enabled_accessibility_services"])
        text = (folder / "quiet-setup.cmd").read_text()
        self.assertIn("-ExecutionPolicy Bypass", text)
        self.assertIn("pause", text.lower())

    # setup-launchers#AC-2, AC-3
    def test_double_clicking_undo_removes_the_app(self):
        pc = Pc(self, tv())
        folder = self.folder(pc)
        self.assertEqual(0, self.launch(pc, folder / "quiet-setup.cmd")[0])
        code, out = self.launch(pc, folder / "quiet-undo.cmd")
        self.assertEqual(code, 0, out)
        self.assertIn("[3/4] Remove: uninstalled quiet-launcher", out)
        self.assertIsNone(pc.tv["installed"])
        text = (folder / "quiet-undo.cmd").read_text()
        self.assertIn("-ExecutionPolicy Bypass", text)
        self.assertIn("pause", text.lower())

    # setup-launchers#AC-3: a failing script's exit code comes back through the launcher.
    def test_the_script_exit_code_comes_back(self):
        pc = Pc(self, tv())
        folder = self.folder(pc)
        (folder / "quiet-launcher.apk").unlink()
        code, out = self.launch(pc, folder / "quiet-setup.cmd")
        self.assertNotEqual(code, 0, out)
        self.assertIn("The install file was not found", out)

    # setup-launchers#AC-3: options are passed on.
    def test_options_are_passed_on(self):
        # -Tv reaches the script: it tries that address only, and cannot connect to it.
        pc = Pc(self, tv(reachable=False, devices=[]))
        folder = self.folder(pc)
        for launcher, step in (("quiet-setup.cmd", "[2/6]"), ("quiet-undo.cmd", "[1/4]")):
            code, out = self.launch(pc, folder / launcher, "-Tv", "10.0.0.99:5555")
            self.assertNotEqual(code, 0, out)
            self.assertIn(f"{step} TV: COULD NOT CONNECT", out)
            self.assertTrue(any("10.0.0.99:5555" in c for c in pc.tv["calls"]), out)

    # setup-launchers#AC-4
    def test_build_copies_the_launchers(self):
        text = (ROOT / "build.ps1").read_text()
        for name in ("quiet-setup.cmd", "quiet-undo.cmd"):
            self.assertIn(f"'{name}'", text)


if __name__ == "__main__":
    unittest.main()
