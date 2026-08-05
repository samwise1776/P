"""GUI runtime: owns the Tk backend, windows, widget registry, and main loop.

The backend is Tk (part of the CPython standard library), which works on
Windows, macOS, and Linux. For headless environments (CI, servers) the
runtime falls back to a recording backend so programs still run.
"""
import os

HEADLESS = os.environ.get("VELICE_GUI", "").lower() in ("none", "headless", "0")

try:
    import tkinter as tk
    from tkinter import ttk, messagebox, filedialog, colorchooser, font as tkfont
    _tk_import_error = None
except Exception as e:  # pragma: no cover
    tk = None
    _tk_import_error = e


class GuiError(Exception):
    pass


def backend_available():
    return tk is not None and (HEADLESS or _has_display())


def _has_display():
    if os.name == "nt":
        return True
    try:
        import subprocess
        r = subprocess.run(["xdpyinfo"], capture_output=True)
        if r.returncode == 0:
            return True
    except Exception:
        pass
    return "DISPLAY" in os.environ


class App:
    def __init__(self, headless=None):
        self.headless = HEADLESS if headless is None else headless
        self.root = None
        self.windows = {}      # name -> Window
        self.named = {}        # name -> Widget
        self.theme_name = "Dark"
        self.theme = {}
        self._event_handlers = {}
        self.log = []

    def ensure_root(self):
        if self.root is None and not self.headless:
            if tk is None:
                raise GuiError(f"tkinter unavailable: {_tk_import_error}")
            self.root = tk.Tk()
            self.root.withdraw()
        return self.root

    # ── theme ──────────────────────────────────────────────────────────
    def set_theme(self, name):
        from velice.gui import theme as theme_mod
        if name not in theme_mod.THEMES:
            raise GuiError(f"unknown theme '{name}' (available: {', '.join(theme_mod.THEMES)})")
        self.theme_name = name
        self.theme = dict(theme_mod.THEMES[name])
        if self.root is not None:
            self.root.configure(bg=self.theme["background"])
            for w in self._all_widgets():
                w.apply_theme()
        return name

    def _all_widgets(self):
        out = []
        for win in self.windows.values():
            out.append(win)
            out.extend(win.descendants())
        return out

    # ── main loop ──────────────────────────────────────────────────────
    def run(self):
        if self.headless:
            return
        self.ensure_root()
        if not self.windows:
            self.root.mainloop()
        else:
            for win in self.windows.values():
                win.show()
            self.root.mainloop()

    def update(self):
        if self.root is not None:
            self.root.update_idletasks()
            self.root.update()

    # ── record / headless ──────────────────────────────────────────────
    def record(self, event, **kw):
        self.log.append((event, kw))
        return kw.get("return", None)


app = App()
