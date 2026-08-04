"""Velice CLI – compiler and runtime entry point."""
import sys, os
from velice.lexer import Lexer
from velice.parser import Parser
from velice.interpreter import Interpreter

def run_file(path):
    if not os.path.exists(path):
        print(f"Error: file not found: {path}", file=sys.stderr); sys.exit(1)
    with open(path) as f: source = f.read()
    try:
        tokens = Lexer(source, path).tokenize()
        ast = Parser(tokens, source).parse()
        interp = Interpreter()
        result = interp.run(ast)
    except Exception as e:
        print(f"\033[91mError: {e}\033[0m", file=sys.stderr); sys.exit(1)

def run_string(code):
    tokens = Lexer(code).tokenize()
    ast = Parser(tokens, code).parse()
    interp = Interpreter()
    return interp.run(ast)

def main():
    args = sys.argv[1:]
    if not args:
        from velice.repl import run_repl; run_repl()
    elif args[0] == "run" and len(args) > 1:
        run_file(args[1])
    elif args[0] == "eval" and len(args) > 1:
        run_string(" ".join(args[1:]))
    elif args[0] == "repl":
        from velice.repl import run_repl; run_repl()
    elif args[0] == "version" or args[0] == "--version":
        from velice import __version__; print(f"Velice v{__version__}")
    elif args[0] == "help" or args[0] == "--help":
        print("Usage: velice [command] [file]")
        print("  (no args)    Start the REPL")
        print("  run <file>   Run a .velice file")
        print("  eval <code>  Evaluate a string")
        print("  repl         Start the REPL")
        print("  version      Show version")
        print("  help         Show this help")
    elif args[0].endswith(".velice"):
        run_file(args[0])
    else:
        print(f"Unknown command: {args[0]}", file=sys.stderr); sys.exit(1)
