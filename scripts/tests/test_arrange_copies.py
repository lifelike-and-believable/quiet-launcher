"""Tests for the dated copies quiet-arrange.ps1 keeps (docs/specs/arrange-script.md, AC-3).

Putting back a saved copy sent a file that already was a dated copy, and copied it again under the
current time: when that time was the copy's own (undo, then setup and put-back, in the same second,
as test_undo_arrangement can be on a fast machine), Copy-Item refused to copy the file onto itself
and the script stopped. A file that already is a dated copy is now kept as it is.
"""
import unittest

from test_arrange import HEADER, PKG, SERIAL, Arranging, arranged_tv


class test_arrange_copies(unittest.TestCase):
    def saved(self, a, stamp, lines):
        a.pc.folder.mkdir(parents=True, exist_ok=True)
        path = a.pc.folder / f"arrangement-{SERIAL}-{stamp}.txt"
        path.write_text("\n".join(lines) + "\n", encoding="utf-8")
        return path

    # arrange-script#AC-3: a put-back copy is the copy kept; it is not copied again.
    def test_putting_back_a_saved_copy_keeps_it_as_the_copy(self):
        a = Arranging(self, arranged_tv())
        path = self.saved(a, "2026-11-01-120000", HEADER + ["Prime Video", "", "Hidden:", "Tubi"])
        code, out = a.run(answers=("y",))
        self.assertEqual(code, 0, out)
        self.assertEqual([PKG["Prime Video"]], a.tv["last_sent"]["order"][:1])
        self.assertEqual([str(path)], a.copies())
        self.assertIn(f"Copy kept: {path}", out)

    # arrange-script#AC-3, AC-9: the same for a dated copy given with -File.
    def test_a_dated_copy_given_with_file_is_not_copied_again(self):
        a = Arranging(self, arranged_tv(arrangement={"version": 1, "order": [], "hidden": []}))
        path = self.saved(a, "2026-11-01-120000", HEADER + ["Netflix", "", "Hidden:"])
        code, out = a.run("-File", str(path))
        self.assertEqual(code, 0, out)
        self.assertEqual([str(path)], a.copies())
        self.assertIn(f"Copy kept: {path}", out)

    # arrange-script#AC-3: any other file sent still gets a dated copy.
    def test_another_file_given_with_file_gets_a_dated_copy(self):
        a = Arranging(self, arranged_tv(arrangement={"version": 1, "order": [], "hidden": []}))
        other = a.pc.dir / "mine.txt"
        other.write_text("\n".join(HEADER + ["Tubi", "", "Hidden:"]) + "\n", encoding="utf-8")
        code, out = a.run("-File", str(other))
        self.assertEqual(code, 0, out)
        self.assertEqual(1, len(a.copies()))
        self.assertIn(f"Copy kept: {a.copies()[0]}", out)


if __name__ == "__main__":
    unittest.main()
