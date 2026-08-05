"""Bridge between Velice AST and the widget runtime."""
from velice.gui.runtime import App, Theme, get_theme
from velice.gui import widgets as W


class Builder:
    def __init__(self, interp):
        self.interp = interp
        self.app = None

    def build_app(self):
        if self.app is None:
            self.app = App()
        return self.app

    # ── window ─────────────────────────────────────────────────────────
    def build_window(self, decl):
        app = self.build_app()
        name = decl.name if hasattr(decl, "name") else "Main"
        props = self._props(decl)
        title = props.pop("title", name)
        theme_name = props.pop("theme", None)
        window = W.Window(name=name, title=title, parent=None, app=app, **props)
        if theme_name:
            window.set_theme(theme_name)
        app.add_window(window)
        if decl.children:
            for node in decl.children:
                widget = self.build_widget(node, window)
                window.add(widget)
        window.realize_tree()
        return window

    # ── generic widget builder ─────────────────────────────────────────
    def build_widget(self, node, parent):
        wtype = node.name
        props = self._props(node)
        text = props.get("text")
        cls = self._class_for(wtype)
        if cls is None:
            raise self.interp.error("unknown widget type: %s" % wtype, node)
        widget = cls(parent=parent, name=None, app=self.app, **props)
        for ev, cb in self._events(node):
            widget.bind(ev, cb)
        if wtype == "tabs" or wtype == "tabview":
            self._build_tab_children(node, widget)
        else:
            for child in (node.children or []):
                widget.add(self.build_widget(child, widget))
        return widget

    def _build_tab_children(self, node, tabview):
        for child in (node.children or []):
            if child.name in ("tab", "page"):
                tab = self.build_widget(child, tabview)
                tabview.add(tab)
            else:
                tabview.add(self.build_widget(child, tabview))

    # ── props / events ─────────────────────────────────────────────────
    def _props(self, node):
        props = {}
        for k, v in (node.properties or {}).items():
            props[k] = self.interp.eval_value(v)
        return props

    def _events(self, node):
        out = []
        for k, v in (node.events or {}).items():
            ev = {"onclick": "onClick", "onchange": "onChange",
                  "oninput": "onChange", "onsubmit": "onSubmit",
                  "onmouseenter": "onMouseEnter", "onmouseleave": "onMouseLeave",
                  "onfocus": "onFocus", "onblur": "onBlur"}.get(
                k.lower(), k)
            if callable(v):
                cb = v
            else:
                cb = lambda *a, _v=v: self.interp.call_block(_v)
            out.append((ev, cb))
        return out

    @staticmethod
    def _class_for(wtype):
        return {
            "window": W.Window, "frame": W.Frame, "button": W.Button,
            "label": W.Label, "textfield": W.TextField,
            "passwordfield": W.PasswordField, "textarea": W.TextArea,
            "checkbox": W.Checkbox, "radiobutton": W.RadioButton,
            "toggle": W.ToggleSwitch, "slider": W.Slider, "spinner": W.Spinner,
            "progressbar": W.ProgressBar, "combobox": W.ComboBox,
            "listbox": W.ListBox, "table": W.Table, "image": W.ImageView,
            "hyperlink": W.Hyperlink, "menubar": W.MenuBar, "menu": W.Menu,
            "item": W.MenuItem, "separator": W.MenuSeparator,
            "tabs": W.TabView, "tabview": W.TabView, "tab": W.Tab,
            "page": W.Tab, "statusbar": W.StatusBar,
        }.get(str(wtype).lower())

    @staticmethod
    def _split_list(text):
        text = str(text).strip()
        if text == "":
            return []
        if text.startswith("[") and text.endswith("]"):
            text = text[1:-1]
        return [s.strip().strip("'\"") for s in text.split(",") if s.strip()]


def build_gui(interp, decls, run_nodes):
    builder = Builder(interp)
    app = builder.build_app()
    for decl in decls:
        builder.build_window(decl)
    if not app.windows:
        raise interp.error("no window declared", None)
    for node in run_nodes:
        if node.name == "Main":
            main = app.get_window("Main") or app.windows[0]
            main.run()
        else:
            win = app.get_window(node.name)
            if win is not None:
                win.run()
    return app
