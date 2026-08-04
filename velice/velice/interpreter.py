"""Velice Interpreter – tree-walking evaluator with environments and builtins."""
from __future__ import annotations
import math, time, random, sys, os, json, hashlib, datetime
from typing import Any, Optional
from velice import ast_nodes as A
from velice.lexer import Lexer
from velice.parser import Parser

class VeliceError(Exception):
    def __init__(self, msg, traceback=None):
        super().__init__(msg)
        self.vl_traceback = traceback or []

class ReturnSignal(Exception):
    def __init__(self, val): self.val = val

class BreakSignal(Exception):
    def __init__(self, val=None): self.val = val

class ContinueSignal(Exception):
    pass

# ── Environment ──────────────────────────────────────────────────────────
class Env:
    def __init__(self, parent=None):
        self.vars: dict[str, Any] = {}; self.parent = parent

    def get(self, name):
        if name in self.vars:
            val = self.vars[name]
            return val[0] if isinstance(val, tuple) else val
        if self.parent: return self.parent.get(name)
        raise VeliceError(f"Undefined variable '{name}'")

    def set(self, name, val, mutable=True):
        if name in self.vars and not mutable:
            raise VeliceError(f"Cannot reassign immutable variable '{name}'")
        self.vars[name] = val

    def define(self, name, val, mutable=True):
        self.vars[name] = (val, mutable)

    def update(self, name, val):
        if name in self.vars:
            v, m = self.vars[name]
            if not m: raise VeliceError(f"Cannot reassign immutable variable '{name}'")
            self.vars[name] = (val, True); return
        if self.parent: self.parent.update(name, val); return
        raise VeliceError(f"Undefined variable '{name}'")

    def resolve(self, name):
        if name in self.vars: return self
        if self.parent: return self.parent.resolve(name)
        return None

# ── Callable ─────────────────────────────────────────────────────────────
class VLFunction:
    def __init__(self, name, params, body, closure, is_method=False, is_native=False, native_fn=None):
        self.name = name; self.params = params; self.body = body
        self.closure = closure; self.is_method = is_method; self.is_native = is_native; self.native_fn = native_fn

    def call(self, interp, args, kwargs=None):
        if self.is_native: return self.native_fn(interp, args, kwargs or {})
        env = Env(self.closure)
        if hasattr(self, '_bound_to'):
            env.define("self", self._bound_to)
        for i, p in enumerate(self.params):
            pname = p.name if hasattr(p, 'name') else p
            if i < len(args): env.define(pname, args[i])
            elif pname in (kwargs or {}): env.define(pname, kwargs[pname])
            elif hasattr(p, 'value') and p.value is not None:
                env.define(pname, interp.eval(p.value, env))
            else: raise VeliceError(f"Missing argument '{pname}'")
        try: interp.exec(self.body, env)
        except ReturnSignal as r: return r.val
        return None

    def __repr__(self): return f"<fn {self.name}>"

class VLClass:
    def __init__(self, name, methods=None, superclass=None):
        self.name = name; self.methods = methods or {}; self.superclass = superclass

    def _find_method(self, name):
        if name in self.methods: return self.methods[name]
        if self.superclass: return self.superclass._find_method(name)
        return None

    def call(self, interp, args, kwargs=None):
        obj = VLInstance(self)
        init = self._find_method("init")
        if init:
            bound = VLFunction(init.name, init.params, init.body, init.closure, is_method=True, is_native=init.is_native, native_fn=init.native_fn)
            bound._bound_to = obj
            bound.call(interp, args, kwargs or {})
        return obj
    def __repr__(self): return f"<class {self.name}>"

class VLInstance:
    def __init__(self, klass):
        self.klass = klass; self.fields = {}

    def get(self, name):
        if name in self.fields: return self.fields[name]
        method = self.klass._find_method(name)
        if method: return method
        raise VeliceError(f"Undefined property '{name}' on {self.klass.name}")

    def set(self, name, val): self.fields[name] = val

    def __repr__(self): return f"<{self.klass.name} instance>"

# ── Interpreter ──────────────────────────────────────────────────────────
class Interpreter:
    def __init__(self):
        self.globals = Env()
        self._setup_builtins()

    def _setup_builtins(self):
        builtins = {
            "print": lambda interp, a, kw: print(*[interp._to_str(x) for x in a], **({"end": kw.get("end", "\n")})),
            "println": lambda interp, a, kw: print(*[interp._to_str(x) for x in a]),
            "len": lambda interp, a, kw: len(a[0]) if a else 0,
            "str": lambda interp, a, kw: interp._to_str(a[0]) if a else "",
            "int": lambda interp, a, kw: int(a[0]) if a else 0,
            "float": lambda interp, a, kw: float(a[0]) if a else 0.0,
            "bool": lambda interp, a, kw: bool(a[0]) if a else False,
            "abs": lambda interp, a, kw: abs(a[0]) if a else 0,
            "min": lambda interp, a, kw: min(a[0]) if a and isinstance(a[0], list) else (min(a) if a else 0),
            "max": lambda interp, a, kw: max(a[0]) if a and isinstance(a[0], list) else (max(a) if a else 0),
            "sum": lambda interp, a, kw: sum(a[0]) if a else 0,
            "range": lambda interp, a, kw: list(range(*[int(x) for x in a])),
            "enumerate": lambda interp, a, kw: list(enumerate(a[0])) if a else [],
            "zip": lambda interp, a, kw: list(zip(*[x for x in a])),
            "map": lambda interp, a, kw: list(map(lambda x: a[1].call(interp, [x]) if isinstance(a[1], VLFunction) else x, a[0])) if len(a) >= 2 else [],
            "filter": lambda interp, a, kw: [x for x in a[0] if a[1].call(interp, [x])] if len(a) >= 2 else [],
            "sorted": lambda interp, a, kw: sorted(a[0], key=lambda x: a[1].call(interp, [x]) if len(a) > 1 and isinstance(a[1], VLFunction) else x) if a else [],
            "reversed": lambda interp, a, kw: list(reversed(a[0])) if a else [],
            "type": lambda interp, a, kw: type(a[0]).__name__ if a else "none",
            "typeof": lambda interp, a, kw: interp._vl_type(a[0]) if a else "none",
            "input": lambda interp, a, kw: input(interp._to_str(a[0]) if a else ""),
            "assert": lambda interp, a, kw: None if (a and a[0]) else (_ for _ in ()).throw(VeliceError("Assertion failed" + (f": {interp._to_str(a[1])}" if len(a) > 1 else ""))),
            "panic": lambda interp, a, kw: (_ for _ in ()).throw(VeliceError(interp._to_str(a[0]) if a else "panic")),
            "chr": lambda interp, a, kw: chr(int(a[0])) if a else "",
            "ord": lambda interp, a, kw: ord(str(a[0])) if a else 0,
            "hex": lambda interp, a, kw: hex(int(a[0])) if a else "0x0",
            "oct": lambda interp, a, kw: oct(int(a[0])) if a else "0o0",
            "bin": lambda interp, a, kw: bin(int(a[0])) if a else "0b0",
            "append": lambda interp, a, kw: (a[0].append(a[1]) if len(a) >= 2 else None) or a[0],
            "push": lambda interp, a, kw: (a[0].append(a[1]) if len(a) >= 2 else None),
            "pop": lambda interp, a, kw: a[0].pop() if a else None,
            "insert": lambda interp, a, kw: a[0].insert(int(a[1]), a[2]) if len(a) >= 3 else None,
            "remove": lambda interp, a, kw: a[0].remove(a[1]) if len(a) >= 2 else None,
            "contains": lambda interp, a, kw: a[1] in a[0] if len(a) >= 2 else False,
            "keys": lambda interp, a, kw: list(a[0].keys()) if a and isinstance(a[0], dict) else [],
            "values": lambda interp, a, kw: list(a[0].values()) if a and isinstance(a[0], dict) else [],
            "items": lambda interp, a, kw: list(a[0].items()) if a and isinstance(a[0], dict) else [],
            "join": lambda interp, a, kw: interp._to_str(a[1]).join([interp._to_str(x) for x in a[0]]) if len(a) >= 2 else "",
            "split": lambda interp, a, kw: interp._to_str(a[0]).split(interp._to_str(a[1]) if len(a) > 1 else " ") if a else [],
            "replace": lambda interp, a, kw: interp._to_str(a[0]).replace(interp._to_str(a[1]), interp._to_str(a[2])) if len(a) >= 3 else "",
            "trim": lambda interp, a, kw: interp._to_str(a[0]).strip() if a else "",
            "lower": lambda interp, a, kw: interp._to_str(a[0]).lower() if a else "",
            "upper": lambda interp, a, kw: interp._to_str(a[0]).upper() if a else "",
            "parse_int": lambda interp, a, kw: int(interp._to_str(a[0])) if a else 0,
            "parse_float": lambda interp, a, kw: float(interp._to_str(a[0])) if a else 0.0,
            "to_json": lambda interp, a, kw: json.dumps(a[0]) if a else "null",
            "from_json": lambda interp, a, kw: json.loads(interp._to_str(a[0])) if a else None,
            "time": lambda interp, a, kw: time.time(),
            "sleep": lambda interp, a, kw: time.sleep(float(a[0]) if a else 0),
            "random": lambda interp, a, kw: random.random(),
            "rand_int": lambda interp, a, kw: random.randint(int(a[0]), int(a[1])) if len(a) >= 2 else random.randint(0, 100),
            "sha256": lambda interp, a, kw: hashlib.sha256(interp._to_str(a[0]).encode()).hexdigest() if a else "",
            "now": lambda interp, a, kw: datetime.datetime.now().isoformat(),
            "exit": lambda interp, a, kw: sys.exit(int(a[0]) if a else 0),
            "clone": lambda interp, a, kw: list(a[0]) if a and isinstance(a[0], list) else dict(a[0]) if a and isinstance(a[0], dict) else a[0],
            "nil": None,
        }
        for name, val in builtins.items():
            if callable(val) and not isinstance(val, VLFunction):
                self.globals.define(name, VLFunction(name, [], None, self.globals, is_native=True, native_fn=val))
            else:
                self.globals.define(name, val)

    def _eval_interp_str(self, s, env):
        parts = []
        i = 0
        while i < len(s):
            if s[i:i+2] == "${":
                depth = 1; j = i + 2; expr = ""
                while j < len(s) and depth > 0:
                    if s[j:j+2] == "${": depth += 1
                    elif s[j] == "}": depth -= 1
                    if depth > 0: expr += s[j]
                    j += 1
                try:
                    toks = Lexer(expr).tokenize()
                    ast = Parser(toks, expr).parse()
                    val = self.eval(ast, env)
                    parts.append(self._to_str(val))
                except Exception as e:
                    parts.append(str(e))
                i = j
            else:
                parts.append(s[i]); i += 1
        return "".join(parts)

    def _to_str(self, val):
        if val is None: return "nil"
        if isinstance(val, bool): return "true" if val else "false"
        if isinstance(val, list): return "[" + ", ".join(self._to_str(x) for x in val) + "]"
        if isinstance(val, dict): return "{" + ", ".join(f"{self._to_str(k)}: {self._to_str(v)}" for k, v in val.items()) + "}"
        return str(val)

    def _vl_type(self, val):
        if val is None: return "nil"
        if isinstance(val, bool): return "bool"
        if isinstance(val, int): return "int"
        if isinstance(val, float): return "float"
        if isinstance(val, str): return "string"
        if isinstance(val, list): return "array"
        if isinstance(val, dict): return "map"
        if isinstance(val, VLFunction): return "function"
        if isinstance(val, VLClass): return "class"
        if isinstance(val, VLInstance): return val.klass.name
        return "unknown"

    def run(self, ast_node: A.Node, env: Optional[Env] = None, raise_errors: bool = False):
        if env is None: env = self.globals
        try:
            return self.eval(ast_node, env)
        except VeliceError as e:
            if raise_errors: raise
            print(f"\033[91mRuntime Error: {e}\033[0m", file=sys.stderr)
            return None
        except ReturnSignal as r:
            return r.val

    def eval(self, node, env):
        if isinstance(node, A.Literal):
            if isinstance(node.value, str) and "${" in node.value and node.kind == "string":
                return self._eval_interp_str(node.value, env)
            return node.value
        if isinstance(node, A.Identifier): return env.get(node.name)
        if isinstance(node, A.BinaryOp): return self._eval_binop(node, env)
        if isinstance(node, A.UnaryOp): return self._eval_unaryop(node, env)
        if isinstance(node, A.Assignment): return self._eval_assignment(node, env)
        if isinstance(node, A.Call): return self._eval_call(node, env)
        if isinstance(node, A.DotAccess): return self._eval_dot(node, env)
        if isinstance(node, A.IndexAccess): return self._eval_index(node, env)
        if isinstance(node, A.SliceAccess): return self._eval_slice(node, env)
        if isinstance(node, A.TernaryExpr): return self.eval(node.then, env) if self.eval(node.cond, env) else self.eval(node.else_, env)
        if isinstance(node, A.NullCoalesce):
            l = self.eval(node.left, env); return l if l is not None else self.eval(node.right, env)
        if isinstance(node, A.ArrayLit): return [self.eval(e, env) for e in node.elems]
        if isinstance(node, A.MapLit): return {self.eval(k, env): self.eval(v, env) for k, v in zip(node.keys, node.vals)}
        if isinstance(node, A.TupleLit): return tuple(self.eval(e, env) for e in node.elems)
        if isinstance(node, A.InterpString): return self._eval_interp(node, env)
        if isinstance(node, A.PipeExpr): return self._eval_pipe(node, env)
        if isinstance(node, A.LambdaExpr): return self._eval_lambda(node, env)
        if isinstance(node, A.LetStmt): return self._eval_let(node, env)
        if isinstance(node, A.ConstStmt): return self._eval_const(node, env)
        if isinstance(node, A.ExprStmt): return self.eval(node.expr, env)
        if isinstance(node, A.ReturnStmt): raise ReturnSignal(self.eval(node.value, env) if node.value else None)
        if isinstance(node, A.BreakStmt): raise BreakSignal(self.eval(node.value, env) if node.value else None)
        if isinstance(node, A.ContinueStmt): raise ContinueSignal()
        if isinstance(node, A.Block): return self._eval_block(node, env)
        if isinstance(node, A.IfStmt): return self._eval_if(node, env)
        if isinstance(node, A.WhileStmt): return self._eval_while(node, env)
        if isinstance(node, A.ForInStmt): return self._eval_for_in(node, env)
        if isinstance(node, A.LoopStmt): return self._eval_loop(node, env)
        if isinstance(node, A.MatchStmt): return self._eval_match(node, env)
        if isinstance(node, A.DeferStmt):
            env._defers = getattr(env, '_defers', []); env._defers.append(node.body); return None
        if isinstance(node, A.ThrowStmt): raise VeliceError(self._to_str(self.eval(node.expr, env)))
        if isinstance(node, A.TryStmt): return self._eval_try(node, env)
        if isinstance(node, A.AssertStmt): return self._eval_assert(node, env)
        if isinstance(node, (A.FnDecl, A.LambdaExpr)): return self._eval_fn_decl(node, env)
        if isinstance(node, A.ClassDecl): return self._eval_class(node, env)
        if isinstance(node, A.StructDecl): return self._eval_struct(node, env)
        if isinstance(node, A.EnumDecl): return self._eval_enum(node, env)
        if isinstance(node, A.ImplDecl): return self._eval_impl(node, env)
        if isinstance(node, A.Program):
            result = None
            for s in node.stmts: result = self.eval(s, env)
            return result
        return None

    def _eval_binop(self, n, env):
        if n.op == "and":
            l = self.eval(n.left, env); return self.eval(n.right, env) if l else l
        if n.op == "or":
            l = self.eval(n.left, env); return l if l else self.eval(n.right, env)
        l = self.eval(n.left, env); r = self.eval(n.right, env)
        ops = {"+":lambda a,b:a+b,"-":lambda a,b:a-b,"*":lambda a,b:a*b,"%":lambda a,b:a%b,"**":lambda a,b:a**b,
            "==":lambda a,b:a==b,"!=":lambda a,b:a!=b,"<":lambda a,b:a<b,">":lambda a,b:a>b,"<=":lambda a,b:a<=b,">=":lambda a,b:a>=b,
            "&":lambda a,b:a&b,"|":lambda a,b:a|b,"^":lambda a,b:a^b}
        if n.op == "/": return l / r if isinstance(r, float) or isinstance(l, float) else l // r if r != 0 else (_ for _ in ()).throw(VeliceError("Division by zero"))
        if n.op in ops: return ops[n.op](l, r)
        if n.op == "..": return list(range(int(l), int(r) + 1))
        raise VeliceError(f"Unknown operator '{n.op}'")

    def _eval_unaryop(self, n, env):
        val = self.eval(n.operand, env)
        if n.op == "-": return -val
        if n.op == "+": return +val
        if n.op == "!": return not val
        if n.op == "not": return not val
        return val

    def _eval_assignment(self, n, env):
        val = self.eval(n.value, env)
        if isinstance(n.target, A.Identifier):
            if n.op: val = self._binop_val(env.get(n.target.name), val, n.op)
            env.update(n.target.name, val); return val
        if isinstance(n.target, A.DotAccess):
            obj = self.eval(n.target.obj, env)
            if isinstance(obj, VLInstance): obj.set(n.target.prop, val)
            return val
        if isinstance(n.target, A.IndexAccess):
            obj = self.eval(n.target.obj, env); idx = self.eval(n.target.index, env)
            if isinstance(obj, list): obj[int(idx)] = val
            elif isinstance(obj, dict): obj[idx] = val
            return val
        return val

    def _binop_val(self, old, val, op):
        if op == "+=": return old + val
        if op == "-=": return old - val
        if op == "*=": return old * val
        if op == "/=": return old / val
        if op == "%=": return old % val
        return val

    def _eval_call(self, n, env):
        if isinstance(n.func, A.Identifier) and n.func.name in ("Some", "Some"):
            return self.eval(n.args[0], env) if n.args else None
        callee = self.eval(n.func, env)
        args = [self.eval(a, env) for a in n.args]
        kwargs = {k: self.eval(v, env) for k, v in n.kwargs.items()}
        if isinstance(callee, VLFunction): return callee.call(self, args, kwargs)
        if isinstance(callee, VLClass): return callee.call(self, args, kwargs)
        if callable(callee): return callee(*args, **kwargs)
        if isinstance(callee, (list, str)):
            if args: return callee[int(args[0])]
            return callee
        raise VeliceError(f"Cannot call {type(callee).__name__}")

    def _eval_dot(self, n, env):
        obj = self.eval(n.obj, env)
        if isinstance(obj, VLInstance):
            val = obj.get(n.prop)
            if isinstance(val, VLFunction):
                bound = VLFunction(val.name, val.params, val.body, val.closure, is_method=val.is_method, is_native=val.is_native, native_fn=val.native_fn)
                bound._bound_to = obj
                return bound
            return val
        if isinstance(obj, dict): return obj.get(n.prop)
        if isinstance(obj, str):
            if n.prop == "len": return len(obj)
            return getattr(obj, n.prop, None)
        if isinstance(obj, (list, tuple)):
            if n.prop == "len": return len(obj)
            if n.prop == "push" or n.prop == "append":
                return VLFunction(n.prop, [A.LetStmt(0,0,"item")], None, env, is_native=True,
                    native_fn=lambda i,a,k: (obj.append(a[0]) if a else None) or obj)
        if isinstance(obj, VLClass) and hasattr(obj, n.prop): return getattr(obj, n.prop)
        if hasattr(obj, n.prop): return getattr(obj, n.prop)
        raise VeliceError(f"Undefined property '{n.prop}'")

    def _eval_index(self, n, env):
        obj = self.eval(n.obj, env); idx = self.eval(n.index, env)
        if isinstance(obj, (list, tuple)): return obj[int(idx)]
        if isinstance(obj, dict): return obj.get(idx)
        if isinstance(obj, str): return obj[int(idx)]
        raise VeliceError("Cannot index this value")

    def _eval_slice(self, n, env):
        obj = self.eval(n.obj, env)
        s = self.eval(n.start, env) if n.start else None
        e = self.eval(n.end, env) if n.end else None
        return obj[int(s):int(e)] if isinstance(obj, (list, str)) else obj

    def _eval_interp(self, n, env):
        parts = []
        for p in n.parts:
            if isinstance(p, str): parts.append(p)
            else: parts.append(self._to_str(self.eval(p, env)))
        return "".join(parts)

    def _eval_pipe(self, n, env):
        val = self.eval(n.left, env)
        if isinstance(n.right, A.Call):
            args = [val] + [self.eval(a, env) for a in n.right.args[1:]]
            callee = self.eval(n.right.func, env)
            if isinstance(callee, VLFunction): return callee.call(self, args)
            if callable(callee): return callee(*args)
        if isinstance(n.right, A.Identifier):
            callee = self.eval(n.right, env)
            if isinstance(callee, VLFunction): return callee.call(self, [val])
        if isinstance(n.right, A.LambdaExpr):
            fn = self._eval_lambda(n.right, env)
            return fn.call(self, [val])
        return val

    def _eval_lambda(self, n, env):
        return VLFunction("<lambda>", n.params, n.body, env)

    def _eval_let(self, n, env):
        val = self.eval(n.value, env) if n.value else None
        env.define(n.name, val, mutable=n.mutable); return val

    def _eval_const(self, n, env):
        val = self.eval(n.value, env) if n.value else None
        env.define(n.name, val, mutable=False); return val

    def _eval_block(self, n, env):
        block_env = Env(env); result = None
        for s in n.stmts:
            result = self.eval(s, block_env)
        defers = getattr(block_env, '_defers', [])
        for d in reversed(defers): self.exec(d, env)
        return result

    def _eval_if(self, n, env):
        if self.eval(n.cond, env): return self.exec(n.then, env)
        for cond, body in n.elifs:
            if self.eval(cond, env): return self.exec(body, env)
        if n.else_: return self.exec(n.else_, env)
        return None

    def _eval_while(self, n, env):
        result = None
        while self.eval(n.cond, env):
            try: result = self.exec(n.body, env)
            except BreakSignal: break
            except ContinueSignal: continue
        return result

    def _eval_for_in(self, n, env):
        iterable = self.eval(n.iterable, env)
        result = None
        for item in iterable:
            env.define(n.var, item, mutable=n.mutable)
            try: result = self.exec(n.body, env)
            except BreakSignal: break
            except ContinueSignal: continue
        return result

    def _eval_loop(self, n, env):
        while True:
            try: self.exec(n.body, env)
            except BreakSignal: break
            except ContinueSignal: continue
        return None

    def _eval_match(self, n, env):
        val = self.eval(n.expr, env)
        for arm in n.arms:
            bindings = {}
            if self._match_pattern(arm.pattern, val, env, bindings):
                if arm.guard:
                    guard_env = Env(env)
                    for k, v in bindings.items(): guard_env.define(k, v)
                    if not self.eval(arm.guard, guard_env): continue
                match_env = Env(env)
                for k, v in bindings.items(): match_env.define(k, v)
                return self.eval(arm.body, match_env)
        raise VeliceError("No matching pattern in match expression")

    def _match_pattern(self, pat, val, env, bindings):
        if isinstance(pat, A.WildcardPattern): return True
        if isinstance(pat, A.LitPattern): return pat.value == val
        if isinstance(pat, A.IdentPattern): bindings[pat.name] = val; return True
        if isinstance(pat, A.OrPattern): return any(self._match_pattern(a, val, env, bindings) for a in pat.alts)
        if isinstance(pat, A.ArrayPattern) and isinstance(val, list):
            if len(pat.elems) != len(val): return False
            return all(self._match_pattern(p, v, env, bindings) for p, v in zip(pat.elems, val))
        if isinstance(pat, A.TuplePattern) and isinstance(val, tuple):
            if len(pat.elems) != len(val): return False
            return all(self._match_pattern(p, v, env, bindings) for p, v in zip(pat.elems, val))
        return False

    def _eval_try(self, n, env):
        try:
            return self.exec(n.body, env)
        except VeliceError as e:
            for catch in n.catches:
                catch_env = Env(env)
                if catch.name: catch_env.define(catch.name, str(e))
                return self.exec(catch.body, catch_env)
        finally:
            if n.finally_: self.exec(n.finally_, env)

    def _eval_assert(self, n, env):
        val = self.eval(n.expr, env)
        if not val:
            msg = self._to_str(self.eval(n.msg, env)) if n.msg else "Assertion failed"
            raise VeliceError(msg)

    def _eval_fn_decl(self, n, env):
        fn = VLFunction(n.name, n.params, n.body, env)
        env.define(n.name, fn); return fn

    def _eval_class(self, n, env):
        methods = {}
        for m in n.members:
            if isinstance(m, A.FnDecl):
                fn = VLFunction(m.name, m.params, m.body, env, is_method=True)
                methods[m.name] = fn
        sup = None
        if n.superclass:
            name = None
            if isinstance(n.superclass, A.Identifier): name = n.superclass.name
            elif isinstance(n.superclass, A.TypeName): name = n.superclass.name
            if name: sup = env.get(name)
            if not isinstance(sup, VLClass): sup = None
        klass = VLClass(n.name, methods, sup)
        env.define(n.name, klass); return klass

    def _eval_struct(self, n, env):
        methods = {}
        for m in n.methods:
            if isinstance(m, A.FnDecl):
                fn = VLFunction(m.name, m.params, m.body, env, is_method=True)
                methods[m.name] = fn
        def struct_init(interp, args, kwargs):
            obj = VLInstance(VLClass(n.name, methods))
            for i, f in enumerate(n.fields):
                fname = f.name if hasattr(f, 'name') else f
                if i < len(args): obj.fields[fname] = args[i]
            for k, v in (kwargs or {}).items(): obj.fields[k] = v
            return obj
        klass = VLClass(n.name, methods)
        init_fn = VLFunction(n.name, n.fields, None, env, is_native=True, native_fn=struct_init)
        env.define(n.name, init_fn); return init_fn

    def _eval_enum(self, n, env):
        variants = {}
        for v in n.variants:
            variants[v.name] = len(variants)
        env.define(n.name, {"variants": variants, "type": "enum"}); return variants

    def _eval_impl(self, n, env):
        target = env.get(n.target.name) if isinstance(n.target, A.Identifier) else None
        if isinstance(target, VLClass):
            for m in n.methods:
                fn = VLFunction(m.name, m.params, m.body, env, is_method=True)
                target.methods[m.name] = fn
        return None

    def exec(self, node, env):
        if isinstance(node, A.Block): return self._eval_block(node, env)
        return self.eval(node, env)
