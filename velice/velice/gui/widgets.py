"""Velice GUI widget classes over the Tk backend."""
import os

from velice.gui.runtime import app as _app, tk, GuiError

TAG = "_velice"


class Widget:
    widget_type = "widget"
    _prop_map = {}
    _events = {"onClick": "<Button-1>", "onDoubleClick": "<Double-Button-1>",
               "onMouseMove": "<Motion>", "onMouseEnter": "<Enter>",
               "onMouseLeave": "<Leave>", "onMouseWheel": "<MouseWheel>",
               "onKeyDown": "<KeyPress>", "onKeyUp": "<KeyRelease>",
               "onFocus": "<FocusIn>", "onBlur": "<FocusOut>"}

    def __init__(self, parent=None, name=None, app=None, **props):
        self.app = app or _app
        self.parent = parent
        self.name = name
        self.children = []
        self.props = {}
        self.events = {}
        self.tk = None
        self._visible = True
        for k, v in props.items():
            self.set(k, v)

    # ── properties ─────────────────────────────────────────────────────
    def set(self, key, value):
        key = key.replace("-", "_")
        self.props[key] = value
        if key == "text":
            self._set_text(value)
        elif key == "value":
            self._set_value(value)
        elif key in ("enabled", "disabled"):
            self._set_enabled(value if key == "enabled" else not value)
        elif key == "visible":
            self._set_visible(value)
        elif key == "tooltip":
            self._set_tooltip(value)
        elif key == "font_size":
            self._set_font_size(value)
        elif key in ("width", "height"):
            self._set_size(key, value)
        elif key == "background":
            self._set_background(value)
        elif key == "items":
            self._set_items(value)
        elif key == "selected":
            self._set_selected(value)
        elif key == "placeholder":
            self._set_placeholder(value)
        elif key == "readonly":
            self._set_readonly(value)
        elif key == "max_length":
            self._set_max_length(value)
        elif key in ("min", "max", "step"):
            self._set_scale(key, value)
        elif key == "justify":
            self._set_justify(value)
        elif key == "bold":
            self._set_bold(value)
        elif key == "color":
            self._set_color(value)
        elif key == "multiple":
            self._set_multiple(value)
        return self

    def get(self, key):
        key = key.replace("-", "_")
        if key == "text":
            return self._get_text()
        if key == "value":
            return self._get_value()
        if key == "selected":
            return self._get_selected()
        return self.props.get(key)

    def _set_text(self, v):
        if self.tk is None: return
        try:
            if isinstance(self.tk, tk.Label):
                self.tk.configure(text=str(v))
            elif isinstance(self.tk, tk.Button):
                self.tk.configure(text=str(v))
            elif hasattr(self.tk, "insert") and not self._tk_has_text():
                self.tk.delete(0, "end")
                self.tk.insert(0, str(v))
            elif isinstance(self.tk, tk.Text):
                self.tk.delete("1.0", "end")
                self.tk.insert("1.0", str(v))
        except Exception:
            pass

    def _tk_has_text(self):
        return isinstance(self.tk, (tk.Text, tk.Label, tk.Button))

    def _set_value(self, v):
        if self.tk is None: return
        if hasattr(self.tk, "set"):
            try:
                self.tk.set(v)
            except Exception:
                pass

    def _set_enabled(self, v):
        if self.tk is None: return
        try:
            self.tk.configure(state="normal" if v else "disabled")
        except Exception:
            pass

    def _set_visible(self, v):
        self._visible = bool(v)
        if self.tk is None: return
        try:
            if v: self.tk.pack(**self._pack_args())
            else: self.tk.pack_forget()
        except Exception:
            pass

    def _set_tooltip(self, v):
        if self.tk is None: return
        tip = tk.Toplevel(self.tk, bg="black")
        tip.overrideredirect(True)
        lab = tk.Label(tip, text=str(v), bg="#333", fg="#fff", padx=6, pady=3)
        lab.pack()
        self.tk.bind("<Enter>", lambda e: tip.geometry(f"+{e.x_root+12}+{e.y_root+12}") or tip.deiconify())
        self.tk.bind("<Leave>", lambda e: tip.withdraw())
        tip.withdraw()

    def _set_font_size(self, v):
        if self.tk is None: return
        try:
            self.tk.configure(font=(self._font_family(), int(v)))
        except Exception:
            pass

    def _set_size(self, key, v):
        if self.tk is None: return
        try:
            if key == "width": self.tk.configure(width=int(v))
            else: self.tk.configure(height=int(v))
        except Exception:
            pass

    def _set_background(self, v):
        if self.tk is None: return
        try: self.tk.configure(bg=v)
        except Exception: pass

    def _set_items(self, v):
        pass
    def _set_selected(self, v):
        pass
    def _set_placeholder(self, v):
        pass
    def _set_readonly(self, v):
        pass
    def _set_max_length(self, v):
        pass
    def _set_scale(self, key, v):
        pass
    def _set_justify(self, v):
        pass
    def _set_bold(self, v):
        pass
    def _set_color(self, v):
        pass
    def _set_multiple(self, v):
        pass

    def _get_text(self): return self.props.get("text", "")
    def _get_value(self): return self.props.get("value", None)
    def _get_selected(self): return self.props.get("selected", None)

    # ── events ─────────────────────────────────────────────────────────
    def bind(self, event, callback):
        ev = event if event.startswith("on") else "on" + event
        self.events[ev] = callback
        if self.tk is None:
            return
        seq = self._events.get(ev)
        if seq:
            try:
                self.tk.bind(seq, lambda e, ev=ev: self._fire(ev, e))
            except Exception:
                pass

    def _fire(self, event, e=None):
        cb = self.events.get(event)
        if cb:
            info = None
            if e is not None:
                try:
                    info = {"x": e.x, "y": e.y, "key": getattr(e, "keysym", None),
                            "state": getattr(e, "state", 0)}
                except Exception:
                    info = {}
            cb(info)

    def _fire_now(self, event):
        self._fire(event)

    # ── hierarchy ──────────────────────────────────────────────────────
    def add(self, child):
        self.children.append(child)
        if child.tk is not None and self.tk is not None:
            child.place_in(self.tk)
        return child

    def realize(self):
        """Create the underlying Tk widget (no-op when headless)."""
        if self.app.headless:
            self.app.record("create", type=self.widget_type, name=self.name,
                            props=dict(self.props))
            return self
        if self.tk is not None:
            return self
        self._create()
        if self.tk is not None:
            for ev, cb in list(self.events.items()):
                self.bind(ev, cb)
            self.apply_theme()
        return self

    def realize_tree(self):
        self.realize()
        for c in self.children:
            c.realize_tree()
        return self

    def place_in(self, parent_tk):
        self.realize()
        if self.tk is not None:
            if self._visible:
                try: self.tk.pack(**self._pack_args())
                except Exception: pass

    def _pack_args(self):
        return {"fill": "both", "expand": True, "padx": 4, "pady": 4}

    def descendants(self):
        out = []
        for c in self.children:
            out.append(c)
            out.extend(c.descendants())
        return out

    def apply_theme(self):
        t = self.app.theme
        if self.tk is None:
            return
        try:
            if isinstance(self.tk, (tk.Label, tk.Button)):
                self.tk.configure(bg=t["button"], fg=t["button_text"])
            elif isinstance(self.tk, (tk.Entry, tk.Text, tk.Listbox)):
                self.tk.configure(bg=t["input"], fg=t["text"])
        except Exception:
            pass

    def _font_family(self):
        return self.props.get("font_family", "Segoe UI" if os.name == "nt" else "Helvetica")

    def __repr__(self):
        return f"<{self.widget_type}{(' ' + self.name) if self.name else ''}>"


# ── concrete widgets ───────────────────────────────────────────────────
class Window(Widget):
    widget_type = "window"

    def __init__(self, title="Velice", width=800, height=600, parent=None, name=None, **props):
        super().__init__(parent=parent, name=name, **props)
        app = self.app
        app.ensure_root()
        self.title = title
        self.tk = None
        self._frame = None
        if not app.headless and app.root is not None:
            self.tk = tk.Toplevel(app.root)
            self.tk.protocol("WM_DELETE_WINDOW", lambda: self._fire_now("onClose"))
            self.tk.title(str(title))
            self.tk.geometry(f"{width}x{height}")
        app.windows[name or title] = self
        self.apply_props(props)

    def apply_props(self, props):
        mapping = {
            "title": lambda v: self._set_title(v),
            "width": lambda v: self._set_win_size("width", v),
            "height": lambda v: self._set_win_size("height", v),
            "resizable": lambda v: self._set_resizable(v),
            "maximized": lambda v: self._set_maximized(v),
            "fullscreen": lambda v: self._set_fullscreen(v),
            "transparent": lambda v: self._set_transparent(v),
            "borderless": lambda v: self._set_borderless(v),
            "always_on_top": lambda v: self._set_ontop(v),
        }
        for k, v in props.items():
            fn = mapping.get(k)
            if fn: fn(v)
            else: self.set(k, v)

    def _set_title(self, v):
        self.title = str(v)
        if self.tk is not None:
            self.tk.title(str(v))

    def _set_win_size(self, key, v):
        if self.tk is None: return
        try:
            self.tk.geometry(f"{v}x{self.tk.winfo_height()}" if key == "width"
                             else f"{self.tk.winfo_width()}x{v}")
        except Exception:
            pass

    def _set_resizable(self, v):
        if self.tk is not None:
            try: self.tk.resizable(bool(v), bool(v))
            except Exception: pass

    def _set_maximized(self, v):
        if self.tk is not None and v:
            try: self.tk.state("zoomed")
            except Exception: pass

    def _set_fullscreen(self, v):
        if self.tk is not None:
            try: self.tk.attributes("-fullscreen", bool(v))
            except Exception: pass

    def _set_transparent(self, v):
        if self.tk is not None:
            try: self.tk.attributes("-alpha", 0.5 if v else 1.0)
            except Exception: pass

    def _set_borderless(self, v):
        if self.tk is not None:
            try: self.tk.overrideredirect(bool(v))
            except Exception: pass

    def _set_ontop(self, v):
        if self.tk is not None:
            try: self.tk.attributes("-topmost", bool(v))
            except Exception: pass

    def show(self):
        if self.tk is not None:
            try: self.tk.deiconify()
            except Exception: pass
        self._fire_now("onLoad")

    def close(self):
        if self.tk is not None:
            try: self.tk.destroy()
            except Exception: pass

    def place_in(self, parent_tk):
        pass

    def _pack_args(self):
        return {}


class Frame(Widget):
    widget_type = "frame"

    def _create(self):
        if self.tk is None:
            self.tk = tk.Frame(self._host(), bg=self.app.theme.get("surface", "#2a2a3e"))

    def _host(self):
        if self.parent is not None and self.parent.tk is not None:
            return self.parent.tk
        if self.tk is None:
            return None
        return self.tk.master


class Button(Widget):
    widget_type = "button"

    def _create(self):
        if self.tk is None:
            self.tk = tk.Button(self._host(), text=self.props.get("text", ""),
                                command=lambda: self._fire_now("onClick"))

    def _host(self):
        p = self.parent
        while p is not None and p.tk is None and p.widget_type != "window":
            p = p.parent
        if p is not None and p.tk is not None:
            return p.tk
        if p is not None and p.widget_type == "window" and p._frame is not None:
            return p._frame
        return p.tk if (p and p.tk) else (self.app.root or None)


class Label(Widget):
    widget_type = "label"

    def _create(self):
        if self.tk is None:
            self.tk = tk.Label(self._host(), text=self.props.get("text", ""),
                               bg=self.app.theme.get("surface", "#2a2a3e"),
                               fg=self.app.theme.get("text", "#e6e6f0"),
                               font=(self._font_family(), self.app.theme.get("font_size", 12)))


class TextField(Widget):
    widget_type = "textfield"

    def _create(self):
        if self.tk is None:
            self.tk = tk.Entry(self._host(), bg=self.app.theme.get("input", "#23233a"),
                               fg=self.app.theme.get("text", "#e6e6f0"))
            if self.props.get("text"):
                self._set_text(self.props["text"])


class PasswordField(TextField):
    widget_type = "passwordfield"

    def _create(self):
        super()._create()
        if self.tk is not None:
            self.tk.configure(show="*")


class TextArea(Widget):
    widget_type = "textarea"

    def _create(self):
        if self.tk is None:
            self.tk = tk.Text(self._host(), bg=self.app.theme.get("input", "#23233a"),
                              fg=self.app.theme.get("text", "#e6e6f0"),
                              wrap="word", height=self.props.get("height", 8))


class Checkbox(Widget):
    widget_type = "checkbox"

    def _create(self):
        if self.tk is None:
            var = tk.BooleanVar(value=bool(self.props.get("value", False)))
            self.tk = tk.Checkbutton(self._host(), text=self.props.get("text", ""),
                                     variable=var, bg=self.app.theme.get("surface"),
                                     fg=self.app.theme.get("text"), activebackground=self.app.theme.get("surface"),
                                     command=lambda: self._fire_now("onChange"))
            self._var = var

    def _get_value(self):
        return bool(getattr(self, "_var", None) and self._var.get()) or self.props.get("value", False)


class RadioButton(Widget):
    widget_type = "radio"

    def _create(self):
        if self.tk is None:
            var = tk.StringVar(value=self.props.get("value", ""))
            self.tk = tk.Radiobutton(self._host(), text=self.props.get("text", ""),
                                     variable=var, value=str(self.props.get("radio_value", "")),
                                     bg=self.app.theme.get("surface"), fg=self.app.theme.get("text"),
                                     command=lambda: self._fire_now("onChange"))
            self._var = var


class ToggleSwitch(Checkbox):
    widget_type = "toggle"


class Slider(Widget):
    widget_type = "slider"

    def _create(self):
        if self.tk is None:
            self.tk = tk.Scale(self._host(), from_=self.props.get("min", 0),
                               to=self.props.get("max", 100),
                               orient="horizontal",
                               command=lambda v: (self._fire_now("onChange")),
                               bg=self.app.theme.get("surface"), fg=self.app.theme.get("text"),
                               highlightthickness=0)
            self.tk.set(self.props.get("value", 50))

    def _get_value(self):
        try: return self.tk.get()
        except Exception: return self.props.get("value", 50)


class Spinner(Widget):
    widget_type = "spinner"

    def _create(self):
        if self.tk is None:
            self.tk = tk.Spinbox(self._host(), from_=self.props.get("min", 0),
                                 to=self.props.get("max", 100))


class ProgressBar(Widget):
    widget_type = "progress"

    def _create(self):
        if self.tk is None:
            try:
                from tkinter import ttk
                self.tk = ttk.Progressbar(self._host(), maximum=100)
                self.tk.set(self.props.get("value", 0))
            except Exception:
                self.tk = tk.Label(self._host(), text=f"{self.props.get('value', 0)}%")

    def _set_value(self, v):
        if self.tk is not None:
            try: self.tk.set(v)
            except Exception: pass


class ComboBox(Widget):
    widget_type = "combobox"

    def _create(self):
        if self.tk is None:
            from tkinter import ttk
            items = [str(x) for x in self.props.get("items", [])]
            self.tk = ttk.Combobox(self._host(), values=items, state="readonly")
            if items and self.props.get("selected") is not None:
                try: self.tk.set(str(self.props["selected"]))
                except Exception: pass
            self.tk.bind("<<ComboboxSelected>>", lambda e: self._fire_now("onSelect"))

    def _set_items(self, v):
        if self.tk is not None:
            try: self.tk.configure(values=[str(x) for x in v])
            except Exception: pass

    def _get_value(self):
        try: return self.tk.get()
        except Exception: return None


class ListBox(Widget):
    widget_type = "listbox"

    def _create(self):
        if self.tk is None:
            self.tk = tk.Listbox(self._host(), bg=self.app.theme.get("input"),
                                 fg=self.app.theme.get("text"),
                                 selectmode="multiple" if self.props.get("multiple") else "browse",
                                 height=self.props.get("height", 6))
            for item in self.props.get("items", []):
                self.tk.insert("end", str(item))
            self.tk.bind("<<ListboxSelect>>", lambda e: self._fire_now("onSelect"))

    def _set_items(self, v):
        if self.tk is not None:
            try:
                self.tk.delete(0, "end")
                for item in v:
                    self.tk.insert("end", str(item))
            except Exception: pass

    def _get_selected(self):
        try:
            sel = self.tk.curselection()
            return [self.tk.get(i) for i in sel]
        except Exception:
            return None


class Table(Widget):
    widget_type = "table"

    def _create(self):
        if self.tk is None:
            from tkinter import ttk
            columns = [str(c) for c in self.props.get("columns", [])]
            self.tk = ttk.Treeview(self._host(), columns=columns, show="headings")
            for c in columns:
                self.tk.heading(c, text=c)
                self.tk.column(c, width=120)
            for row in self.props.get("rows", []):
                self.tk.insert("", "end", values=[str(x) for x in row])
            scroll = tk.Scrollbar(self._host(), orient="vertical", command=self.tk.yview)
            self.tk.configure(yscrollcommand=scroll.set)
            scroll.pack(side="right", fill="y")
            self._scrollbar = scroll

    def _set_rows(self, rows):
        if self.tk is not None:
            try:
                self.tk.delete(*self.tk.get_children())
                for row in rows:
                    self.tk.insert("", "end", values=[str(x) for x in row])
            except Exception: pass


class ImageView(Widget):
    widget_type = "image"

    def _create(self):
        if self.tk is None:
            self.tk = tk.Label(self._host())
        self._load_image(self.props.get("path") or self.props.get("source"))

    def _load_image(self, path):
        if self.tk is None or not path or not os.path.exists(str(path)):
            return
        try:
            img = tk.PhotoImage(file=str(path))
            self.tk.configure(image=img)
            self.tk.image = img
        except Exception:
            pass


class Hyperlink(Widget):
    widget_type = "hyperlink"

    def _create(self):
        if self.tk is None:
            self.tk = tk.Label(self._host(), text=self.props.get("text", ""),
                               fg=self.app.theme.get("primary"), cursor="hand2")
            self.tk.bind("<Button-1>", lambda e: self._fire_now("onClick"))


class MenuBar(Widget):
    widget_type = "menubar"

    def __init__(self, parent=None, name=None, app=None, **props):
        super().__init__(parent=parent, name=name, app=app, **props)
        if self.parent is not None and self.parent.tk is not None and not self.app.headless:
            try:
                self.tk = tk.Menu(self.parent.tk)
                self.parent.tk.config(menu=self.tk)
            except Exception:
                self.tk = None

    def add(self, child):
        if self.tk is not None and child.tk is not None:
            self.tk.add_cascade(label=child.props.get("text", ""), menu=child.tk)
        super().add(child)

    def place_in(self, parent_tk):
        pass


class Menu(Widget):
    widget_type = "menu"

    def _create(self):
        if self.tk is None:
            self.tk = tk.Menu(self._menubar_tk())

    def _menubar_tk(self):
        p = self.parent
        while p is not None and p.tk is None:
            p = p.parent
        return p.tk if p else None

    def add(self, child):
        if self.tk is not None:
            if child.widget_type == "item":
                self.tk.add_command(label=child.props.get("text", ""),
                                    command=lambda: child._fire_now("onClick"))
            elif child.widget_type == "separator":
                self.tk.add_separator()
            elif child.widget_type == "menu":
                self.tk.add_cascade(label=child.props.get("text", ""), menu=child.tk)
        self.children.append(child)
        return child

    def place_in(self, parent_tk):
        pass


class TabView(Widget):
    widget_type = "tabview"

    def _create(self):
        if self.tk is None:
            from tkinter import ttk
            self.tk = ttk.Notebook(self._host())
            self._tabs = {}

    def add(self, child):
        super().add(child)
        if self.tk is not None and child.widget_type == "tab":
            try:
                frame = tk.Frame(self.tk, bg=self.app.theme.get("surface"))
                self.tk.add(frame, text=str(child.props.get("text", child.name or "Tab")))
                child._frame = frame
            except Exception:
                pass

    def _host(self):
        return self.parent.tk if (self.parent and self.parent.tk) else (self.app.root or None)


class Tab(Widget):
    widget_type = "tab"

    def __init__(self, parent=None, name=None, app=None, **props):
        super().__init__(parent=parent, name=name, app=app, **props)
        self._frame = None
        if self.tk is not None:
            self.tk = None

    def _host(self):
        if self._frame is not None:
            return self._frame
        return self.parent._host() if (self.parent and hasattr(self.parent, "_host")) else None

    def place_in(self, parent_tk):
        pass


class StatusBar(Widget):
    widget_type = "statusbar"

    def _create(self):
        if self.tk is None:
            self.tk = tk.Label(self._host(), text=self.props.get("text", ""),
                               anchor="w", bg=self.app.theme.get("surface"),
                               fg=self.app.theme.get("text_muted"))
