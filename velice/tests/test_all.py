import contextlib
import io
import os
import re
import subprocess
import sys
import unittest

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, ROOT)

from velice.lexer import Lexer, TT  # noqa: E402
from velice.parser import Parser  # noqa: E402
from velice.interpreter import Interpreter, VeliceError  # noqa: E402
from velice import ast_nodes as A  # noqa: E402


def run(code):
    tokens = Lexer(code).tokenize()
    ast = Parser(tokens, code).parse()
    interp = Interpreter()
    buf = io.StringIO()
    with contextlib.redirect_stdout(buf):
        interp.run(ast)
    return buf.getvalue().strip()


def show(value):
    if isinstance(value, list):
        return '[' + ', '.join(show(v) for v in value) + ']'
    return str(value).lower() if isinstance(value, bool) else str(value)


class TestLexer(unittest.TestCase):
    def test_tokens(self):
        tokens = Lexer('let x = 42 + 3.14').tokenize()
        kinds = [t.type for t in tokens]
        self.assertIn(TT.LET, kinds)
        self.assertIn(TT.IDENT, kinds)
        self.assertIn(TT.INT, kinds)

    def test_keywords(self):
        tokens = Lexer('fn class struct enum match').tokenize()
        kinds = [t.type.value for t in tokens]
        for k in ['fn', 'class', 'struct', 'enum', 'match']:
            self.assertIn(k, kinds)

    def test_strings_and_numbers(self):
        tokens = Lexer('"hi" r"raw" 0x1F 1_000 2.5e3').tokenize()
        values = [t.value for t in tokens]
        for expected in ['hi', 'raw', 0x1F, 1000, 2500.0]:
            self.assertIn(expected, values)


class TestParser(unittest.TestCase):
    def test_program_shape(self):
        ast = Parser(Lexer('print("hi")').tokenize(), 'print("hi")').parse()
        self.assertEqual(len(ast.stmts), 1)

    def test_let_binding(self):
        ast = Parser(Lexer('let x = 5').tokenize(), 'let x = 5').parse()
        self.assertEqual(ast.stmts[0].name, 'x')

    def test_fn_decl(self):
        ast = Parser(Lexer('fn add(a, b) { return a + b }').tokenize(),
                     'fn add(a, b) { return a + b }').parse()
        decl = ast.stmts[0]
        self.assertEqual(decl.name, 'add')
        self.assertEqual([p.name for p in decl.params], ['a', 'b'])

    def test_var_decl(self):
        ast = Parser(Lexer('var a = "Hello"').tokenize(), 'var a = "Hello"').parse()
        decl = ast.stmts[0]
        self.assertEqual(decl.name, 'a')
        self.assertTrue(decl.mutable)

    def test_var_thunk(self):
        ast = Parser(Lexer('var a = (print("hi"))').tokenize(),
                     'var a = (print("hi"))').parse()
        self.assertIsInstance(ast.stmts[0].value, A.ThunkExpr)

    def test_var_params(self):
        ast = Parser(Lexer('fn calc(var a, var b) { return a + b }').tokenize(),
                     'fn calc(var a, var b) { return a + b }').parse()
        decl = ast.stmts[0]
        self.assertEqual([p.name for p in decl.params], ['a', 'b'])
        self.assertTrue(all(p.mutable for p in decl.params))


class TestInterpreter(unittest.TestCase):
    def test_arithmetic(self):
        self.assertEqual(run('print(2 + 3 * 4)'), '14')

    def test_precedence(self):
        self.assertEqual(run('print((2 + 3) * 4)'), '20')

    def test_variables(self):
        self.assertEqual(run('let x = 5\nprint(x + 1)'), '6')

    def test_var_keyword(self):
        self.assertEqual(run('var a = "Hello"\nprint(a)'), 'Hello')

    def test_var_thunk_runs_on_call(self):
        self.assertEqual(run('var greet = (print("Hello from thunk"))\ngreet()\ngreet()'),
                         'Hello from thunk\nHello from thunk')

    def test_var_thunk_returns_value(self):
        self.assertEqual(run('var f = (5 * 4)\nprint(f())'), '20')

    def test_var_fn_params(self):
        self.assertEqual(run('fn calc(var a, var b) { return a + b }\nprint(calc(2, 3))'), '5')

    def test_immutability(self):
        tokens = Lexer('let x = 5\nx = 6').tokenize()
        ast = Parser(tokens, 'let x = 5\nx = 6').parse()
        with self.assertRaises(VeliceError):
            Interpreter().run(ast, raise_errors=True)

    def test_mutability(self):
        self.assertEqual(run('let mut x = 5\nx = 6\nprint(x)'), '6')

    def test_strings_interpolation(self):
        self.assertEqual(run('let n = "World"\nprint("Hello ${n}!")'),
                         'Hello World!')

    def test_functions(self):
        self.assertEqual(run('fn add(a, b) { return a + b }\nprint(add(2, 3))'),
                         '5')

    def test_default_args(self):
        code = 'fn f(a, b = 10) { return a + b }\nprint(f(1))'
        self.assertEqual(run(code), '11')

    def test_closures(self):
        code = 'fn outer() { let mut c = 0; return fn() { c = c + 1; return c } }\n' \
               'let f = outer()\nprint(f())\nprint(f())'
        self.assertEqual(run(code), '1\n2')

    def test_recursion(self):
        code = 'fn fib(n) { if n < 2 { return n } return fib(n-1) + fib(n-2) }\n' \
               'print(fib(10))'
        self.assertEqual(run(code), '55')

    def test_while(self):
        code = 'let mut i = 0\nlet mut s = 0\nwhile i < 5 { s = s + i; i = i + 1 }\nprint(s)'
        self.assertEqual(run(code), '10')

    def test_for(self):
        code = 'let mut s = 0\nfor x in 0..3 { s = s + x }\nprint(s)'
        self.assertEqual(run(code), '6')

    def test_range_operator(self):
        self.assertEqual(run('print(0..3)'), '[0, 1, 2, 3]')

    def test_arrays(self):
        self.assertEqual(run('let a = [1, 2, 3]\nprint(a[1])'), '2')
        self.assertEqual(run('let a = [1, 2]\nappend(a, 3)\nprint(len(a))'), '3')

    def test_maps(self):
        self.assertEqual(run('let m = {"k": 42}\nprint(m["k"])'), '42')

    def test_match(self):
        code = 'fn f(n) { return match n { 0 => "zero", _ => "other" } }\nprint(f(0))'
        self.assertEqual(run(code), 'zero')

    def test_classes(self):
        code = 'class C { fn init(v) { self.v = v } fn get() { return self.v } }\n' \
               'let c = C(42)\nprint(c.get())'
        self.assertEqual(run(code), '42')

    def test_inheritance(self):
        code = ('class A { fn init(x) { self.x = x } fn val() { return self.x } }\n'
                'class B extends A { fn val() { return self.x * 2 } }\n'
                'let b = B(21)\nprint(b.val())')
        self.assertEqual(run(code), '42')

    def test_try_catch(self):
        code = 'try { throw "boom" } catch e { print("caught:" + e) }'
        self.assertEqual(run(code), 'caught:boom')

    def test_pipes(self):
        self.assertEqual(run('fn sq(x) { return x * x }\nprint(3 |> sq)'), '9')

    def test_null_coalescing(self):
        self.assertEqual(run('print(nil ?? "default")'), 'default')

    def test_boolean_logic(self):
        self.assertEqual(run('print(true and false)'), 'false')
        self.assertEqual(run('print(true or false)'), 'true')
        self.assertEqual(run('print(not true)'), 'false')

    def test_string_methods(self):
        self.assertEqual(run('print(len("hello"))'), '5')

    def test_typeof(self):
        self.assertEqual(run('print(typeof(42))'), 'int')

    def test_print_multiple(self):
        self.assertEqual(run('print(1, 2, 3)'), '1 2 3')


class TestExamples(unittest.TestCase):
    def test_all_examples_run(self):
        examples = os.path.join(ROOT, 'examples')
        for name in sorted(os.listdir(examples)):
            if not name.endswith('.velice'):
                continue
            path = os.path.join(examples, name)
            result = subprocess.run(
                [sys.executable, '-m', 'velice', 'run', path],
                capture_output=True, text=True, cwd=ROOT,
            )
            self.assertEqual(
                result.returncode, 0,
                f'{name} failed:\n{result.stderr or result.stdout}',
            )


if __name__ == '__main__':
    unittest.main()
