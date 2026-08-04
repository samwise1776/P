# Velice

A general-purpose, dynamic, multi-paradigm programming language with a clean
C-family syntax — implemented as a dependency-free, tree-walking interpreter in
pure Python.

```velice
fn fib(n) {
    return match n {
        n if n < 2 => n,
        _ => fib(n - 1) + fib(n - 2),
    }
}
print("fib(10) =", fib(10))   # → fib(10) = 55
```

## Features

- Immutable-by-default bindings (`let`, `let mut`, `const`)
- Functions as first-class values: closures, recursion, default args
- Pattern matching with guards (`match ... { _ => ... }`)
- Classes with single inheritance, traits, and structs
- Error handling: `try` / `catch` / `finally`, `throw`, `??`
- Functional helpers: pipes `|>`, `map`, `filter`, `reduce`
- Full standard library: collections, JSON, hashing, time, math, random
- VS Code extension with syntax highlighting and file icons

## Quick start

```bash
# run a script
python3 -m velice run examples/hello_world.velice

# REPL
python3 -m velice

# evaluate an expression
python3 -m velice eval "1 + 2 |> * 3"

# install the CLI
pip install -e .
velice run examples/hello_world.velice
```

## Project layout

```
velice/
├── velice/            # the interpreter (lexer, parser, AST, evaluator, REPL)
├── examples/          # runnable .velice programs
├── docs/SPEC.md       # full language specification
├── editor/vscode/     # VS Code extension (syntax, icons, snippets)
├── tests/             # unit tests
└── setup.py           # pip packaging
```

## Hello, World

```velice
print("Hello, World!")
```

## VS Code

Install the extension from `editor/vscode/` (see its README) to get syntax
highlighting, a custom `.velice` file icon, snippets, and run commands.

## License

MIT
