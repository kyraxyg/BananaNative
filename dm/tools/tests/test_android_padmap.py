"""Settings › Gamepad buttons on Android (PadMap.java with android/app/src/jvmtest's PadMapJvmTest): the stored custom map in the host's DM_PAD_MAP format
(dm/host/android/input_map.c nxin_map_parse, checked by dm/host/tests/t_android_input.c), assigning a key, and the host's lines for each layout. The JVM part is
skipped without a JDK."""
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
SRC = REPO / 'android/app/src/main/java/dev/nxnative/android/PadMap.java'
TEST = REPO / 'android/app/src/jvmtest/java/dev/nxnative/android/PadMapJvmTest.java'


@unittest.skipUnless(shutil.which('javac') and shutil.which('java'), 'no JDK')
class PadMap(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tmp = Path(tempfile.mkdtemp(prefix='padmap_'))
        subprocess.run(['javac', '-Xlint:all', '-Werror', '-encoding', 'UTF-8', '-d', str(cls.tmp / 'classes'), str(SRC), str(TEST)], check=True, capture_output=True, text=True)

    @classmethod
    def tearDownClass(cls): shutil.rmtree(cls.tmp, ignore_errors=True)

    def java(self, *a):
        r = subprocess.run(['java', '-Dstdout.encoding=UTF-8', '-cp', str(self.tmp / 'classes'), 'dev.nxnative.android.PadMapJvmTest', *map(str, a)],
                           capture_output=True, text=True, encoding='utf-8')
        self.assertEqual(r.returncode, 0, r.stderr)
        return r.stdout.rstrip('\n').split(' ', 1)[1] if ' ' in r.stdout.rstrip('\n') else ''

    def test_parse(self):
        self.assertEqual(self.java('parse', '96=A, 97 = b,99=X,100=Y'), '96=A,97=B,99=X,100=Y')
        # as the host: a bad pair is skipped, the first of a repeated key kept, keycodes 1..999
        self.assertEqual(self.java('parse', '96=Q,=A,abc=B,0=A,1000=A,97=B,97=A,99=,,100=none'), '97=B,100=NONE')
        self.assertEqual(self.java('parse', ''), '')

    def test_assign(self):
        # a key reaches one button and a button comes from one key
        self.assertEqual(self.java('assign', '96=A,97=B', 'A', 97), '97=A')
        self.assertEqual(self.java('assign', '96=A,97=B', 'ZR', 98), '96=A,97=B,98=ZR')
        self.assertEqual(self.java('assign', '96=A,97=B', 'B', 96), '96=B')

    def test_env(self):
        self.assertEqual(self.java('env', 'position', '96=A'), '')
        self.assertEqual(self.java('env', 'unknown', ''), '')                      # an unknown stored value reads as the default
        self.assertEqual(self.java('env', 'label', ''), 'DM_PAD_LABELLED=1')
        self.assertEqual(self.java('env', 'custom', '96=A,97=b,x'), 'DM_PAD_MAP=96=A,97=B')
        self.assertEqual(self.java('env', 'custom', ''), '')                       # nothing mapped: by position

    def test_label(self):
        self.assertTrue(self.java('label', 'label', '').startswith('By label'))
        self.assertEqual(self.java('label', 'custom', '96=A,97=B'), 'Custom: 2 keys mapped')
        self.assertEqual(self.java('label', 'custom', ''), 'Custom: nothing mapped yet (by position)')


if __name__ == '__main__':
    unittest.main()
