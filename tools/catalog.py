#!/usr/bin/env python3
"""Regenerates CATALOG.md from the showcase code.

Run from anywhere: python3 tools/catalog.py
The catalog is derived, never hand-edited: categories come from showcase/Demos.kt, sets and
animations from each package's DemoGroup, descriptions from the caption passed to the demo's
stage, and "copy" lists from the package's files plus the files its imports resolve to.
"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "shared" / "src"
PKG_PATH = "kotlin/com/azersun/uitemplates"
COMMON_MAIN = SRC / "commonMain" / PKG_PATH
DEMOS = COMMON_MAIN / "demos"
OUT = ROOT / "CATALOG.md"

STRING = r'"((?:[^"\\]|\\.)*)"'
DEMO_RE = re.compile(r'Demo\(\s*' + STRING + r'\s*,\s*' + STRING + r'\s*,\s*' + STRING + r'\s*,\s*\{\s*((\w+)\([^}]*\))\s*\}')
GROUP_RE = re.compile(r'(?:val|private val)\s+(\w+)(?:\s*:\s*DemoGroup)?\s*=\s*DemoGroup\(\s*' + STRING)
CATEGORY_RE = re.compile(
    r'DemoCategory\(\s*' + STRING + r'\s*,\s*' + STRING + r'\s*,\s*' + STRING + r'\s*,\s*listOf\(([^)]*)\)'
)
CAPTION_RE = re.compile(
    r'(?:number\s*=\s*)?\d+\s*,\s*(?:title\s*=\s*)?' + STRING + r'\s*,\s*(?:caption\s*=\s*)?' + STRING
)
IMPORT_RE = re.compile(r'^import com\.azersun\.uitemplates\.(demos\.(\w+)|common)\.(\w+)', re.M)


def unescape(s):
    return s.replace('\\"', '"').replace("\\\\", "\\")


def norm(s):
    return re.sub(r"[^a-z0-9]", "", s.lower())


def rel(path):
    """Paths under commonMain are shown relative to it; platform source sets keep their full path."""
    if path.is_relative_to(COMMON_MAIN):
        return path.relative_to(COMMON_MAIN).as_posix()
    return path.relative_to(ROOT).as_posix()


def package_files(pkg):
    """All source files of a demo package across source sets (commonMain plus any platform actuals)."""
    return sorted(p for p in SRC.glob(f"*/{PKG_PATH}/demos/{pkg}/*.kt"))


def find_definition(directory, symbol):
    pattern = re.compile(
        r'^(?:(?:internal|private|public|actual|expect|data|enum|sealed|inline|value|const)\s+)*'
        r'(?:fun|val|var|class|object|interface)\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?' + re.escape(symbol) + r'\b',
        re.M,
    )
    for path in sorted(directory.glob("*.kt")):
        if pattern.search(path.read_text()):
            return path
    return None


def collect_groups():
    """Group val name -> (title, package, [demo dicts])."""
    groups = {}
    for path in sorted(COMMON_MAIN.rglob("*.kt")):
        text = path.read_text()
        starts = [(m.start(), m.group(1), unescape(m.group(2))) for m in GROUP_RE.finditer(text)]
        for index, (start, name, title) in enumerate(starts):
            end = starts[index + 1][0] if index + 1 < len(starts) else len(text)
            demos = [
                {
                    "id": unescape(m.group(1)),
                    "title": unescape(m.group(2)),
                    "subtitle": unescape(m.group(3)),
                    "call": m.group(4),
                    "composable": m.group(5),
                }
                for m in DEMO_RE.finditer(text, start, end)
            ]
            groups[name] = {"title": title, "file": path, "demos": demos}
    return groups


def demo_file(composable):
    pattern = re.compile(r'^(?:(?:private|internal)\s+)?fun\s+' + re.escape(composable) + r'\s*\(', re.M)
    for path in sorted(COMMON_MAIN.rglob("*.kt")):
        if pattern.search(path.read_text()):
            return path
    return None


def caption_for(path, title, subtitle):
    if path is None:
        return subtitle
    found = [(unescape(t), unescape(c)) for t, c in CAPTION_RE.findall(path.read_text())]
    found = [(t, c) for t, c in found if len(c.split()) >= 4]
    for t, c in found:
        if norm(t) == norm(title):
            return c
    if len(found) == 1:
        return found[0][1]
    return subtitle


def package_of(path):
    parts = path.relative_to(COMMON_MAIN).parts
    return parts[1] if parts[0] == "demos" and len(parts) > 2 else None


def dependencies(pkg):
    """Files outside the package that its imports resolve to, followed transitively."""
    needed, queue, seen = [], list(package_files(pkg)), set()
    while queue:
        path = queue.pop()
        if path in seen:
            continue
        seen.add(path)
        for full, other_pkg, symbol in IMPORT_RE.findall(path.read_text()):
            directory = DEMOS / other_pkg if other_pkg else COMMON_MAIN / "common"
            if other_pkg == pkg:
                continue
            target = find_definition(directory, symbol)
            if target and target not in needed:
                needed.append(target)
                queue.append(target)
    return sorted(needed)


TOP_LEVEL_RE = re.compile(
    r'^(?!private)(?:(?:internal|public|actual|expect|data|enum|sealed|inline|value|const|abstract|open)\s+)*'
    r'(?:fun\s+(?:<[^>]*>\s*)?(?:.+?\.)?(\w+)\s*\(|(?:val|var)\s+(?:.+?\.)?(\w+)\s*[:=]|(?:class|object|interface)\s+(\w+))',
    re.M,
)
_symbols = {}


def symbols_of(path):
    """Top-level declarations another file could use (private ones are file-local)."""
    if path not in _symbols:
        _symbols[path] = {next(g for g in m.groups() if g) for m in TOP_LEVEL_RE.finditer(path.read_text())}
    return _symbols[path]


def used_in(text, names):
    return {n for n in names if re.search(r"\b" + re.escape(n) + r"\b", text)}


def is_frame(symbol):
    """`*Stage` composables only draw the showcase card, caption, autoplay and Replay pill."""
    return symbol.endswith("Stage")


def needs_for(demo_path, registration):
    """file -> symbols that file supplies to this animation, followed through every file it pulls in."""
    pkg = package_of(demo_path)
    supplied, queue, seen = {}, [demo_path], set()
    while queue:
        path = queue.pop()
        if path in seen:
            continue
        seen.add(path)
        text = path.read_text()
        sources = []
        if pkg:
            for other in package_files(pkg):
                if other != path and other != registration and other.is_relative_to(COMMON_MAIN):
                    sources.append((other, used_in(text, symbols_of(other) - symbols_of(path))))
        for _, other_pkg, symbol in IMPORT_RE.findall(text):
            if other_pkg == pkg:
                continue
            directory = DEMOS / other_pkg if other_pkg else COMMON_MAIN / "common"
            target = find_definition(directory, symbol)
            if target:
                sources.append((target, {symbol}))
        for target, names in sources:
            if not names:
                continue
            supplied.setdefault(target, set()).update(names)
            if not all(is_frame(n) for n in names):
                queue.append(target)
    return supplied


def copy_line(demo_path, registration):
    supplied = needs_for(demo_path, registration)
    pkg = package_of(demo_path)
    keep, strip = [demo_path.name], []
    for target, names in sorted(supplied.items()):
        frames = sorted(n for n in names if is_frame(n))
        real = sorted(n for n in names if not is_frame(n))
        strip += [f"`{n}`" for n in frames]
        if not real:
            continue
        if package_of(target) == pkg and target.is_relative_to(COMMON_MAIN):
            keep.append(target.name)
        else:
            keep.append(f"from `{rel(target)}` only {', '.join(f'`{n}`' for n in real)}")
    if pkg and any("expect " in f.read_text() for f in [demo_path, *supplied] if package_of(f) == pkg):
        keep += [f"`{rel(f)}`" for f in package_files(pkg) if not f.is_relative_to(COMMON_MAIN)]
    own = [f"`{k}`" for k in keep if not k.startswith(("from", "`"))]
    other = [k for k in keep if k.startswith(("from", "`"))]
    line = "Copy " + "; ".join([", ".join(own), *other]) + "."
    if strip:
        line += " Strip " + ", ".join(sorted(set(strip))) + " (showcase frame)."
    return line


KEYWORDS = [
    ("spring / bounce / overshoot", r"spring|bounc|overshoot"),
    ("squash / squish / jelly / wobble", r"squash|squish|jelly|wobbl|jiggl"),
    ("stretch / goo / blob", r"stretch|goo|blob|liquid drop"),
    ("flip / 3D / hinge", r"flip|3d|hinge|turnstile|barrel|twist"),
    ("spin / rotate", r"spin|rotat|rolls"),
    ("ripple / ring / pulse / echo", r"ripple|ring|pulse|echo|halo"),
    ("glow / neon / shine / sparkle", r"glow|neon|shine|sparkl|twinkl|shimmer"),
    ("liquid / wave / fill / ink", r"liquid|wav|fills? |ink"),
    ("confetti / burst / particles / sparks", r"confetti|burst|particle|spark|dust|pixel|crumbl"),
    ("shake / error", r"shake|error|wrong|fail|declin"),
    ("morph / shape change", r"morph|becomes|turns into|bends into"),
    ("stagger / one by one", r"stagger|one by one|one after another|in turn"),
    ("slide / swipe / drag", r"slide|swipe|drag"),
    ("drop / fall / gravity", r"drop|fall|lands"),
    ("fold / unfold / unroll / blinds", r"fold|unroll|blind|curtain|door"),
    ("draw / trace / stroke", r"draws|trace|stroke|scribbl"),
    ("count / roll / odometer / numbers", r"count|odometer|digit|number|flap"),
    ("blur / focus / depth", r"blur|focus|depth|tilt"),
    ("typing / text letters", r"letter|types|typing|word"),
    ("press / hold / long-press", r"press|hold"),
    ("reveal / wipe / clip", r"reveal|wipe|sweep|portal|eclipse|circle grows"),
]


def keyword_index(entries):
    lines = ["## Motion index", "",
             "Animations grouped by the kind of motion in their description. An id can appear under several headings.", ""]
    for label, pattern in KEYWORDS:
        rx = re.compile(r"\b(?:" + pattern + ")")
        ids = [e["id"] for e in entries if rx.search(e["text"].lower())]
        if ids:
            lines.append(f"- **{label}**: " + ", ".join(f"`{i}`" for i in ids))
    return lines + [""]


def main():
    demos_kt = (COMMON_MAIN / "showcase" / "Demos.kt").read_text()
    groups = collect_groups()
    categories = [
        {
            "id": unescape(m.group(1)),
            "title": unescape(m.group(2)),
            "blurb": unescape(m.group(3)),
            "groups": [g.strip() for g in m.group(4).split(",") if g.strip()],
        }
        for m in CATEGORY_RE.finditer(demos_kt)
    ]

    total = sum(len(groups[g]["demos"]) for c in categories for g in c["groups"])
    lines = [
        "# Animation catalog",
        "",
        "<!-- Generated by tools/catalog.py. Do not edit by hand: change the code, then rerun it. -->",
        "",
        f"{total} Compose Multiplatform animations in {len(categories)} categories. "
        "Each entry says what the animation looks like, which function draws it, and which files to copy. "
        "See [AGENTS.md](AGENTS.md) for how to port one into another project.",
        "",
        "Paths are relative to `shared/src/commonMain/kotlin/com/azersun/uitemplates/` unless they start with `shared/`.",
        "",
        "Find animations by component (categories below) or by motion (the motion index), then read the entry: "
        "its Copy line is the minimal set of files for that one animation, and Strip names the showcase-only "
        "wrapper to remove after copying.",
        "",
        "## Categories",
        "",
    ]
    for c in categories:
        count = sum(len(groups[g]["demos"]) for g in c["groups"])
        lines.append(f"- [{c['title']}](#{re.sub(r'[^a-z0-9 -]', '', c['title'].lower()).replace(' ', '-')}) "
                     f"({count}) — {c['blurb']}")
    lines.append("")

    entries, body = [], []
    for c in categories:
        body += [f"## {c['title']}", "", c["blurb"] + ".", ""]
        for gname in c["groups"]:
            group = groups[gname]
            # Sets registered straight in showcase/Demos.kt live in the package of their demo file.
            first = demo_file(group["demos"][0]["composable"]) if group["demos"] else None
            pkg = package_of(first) if first else package_of(group["file"])
            files = package_files(pkg) if pkg else []
            registration = group["file"] if package_of(group["file"]) == pkg else None
            # A registration file that also holds code is copied; only its group val is dropped.
            shared_registration = registration is not None and re.search(r"^\s*(?:private\s+|internal\s+)?fun\s", registration.read_text(), re.M)
            if shared_registration:
                registration = None
            copy = [f for f in files if f != registration]
            deps = dependencies(pkg) if pkg else []
            body += [f"### {group['title']}", ""]
            if pkg:
                platform = [f for f in copy if not f.is_relative_to(COMMON_MAIN)]
                line = f"Copy folder `demos/{pkg}/`"
                if registration:
                    line += f" except `{registration.name}` (showcase registration)"
                elif shared_registration:
                    line += f" (drop the `{gname}` val in `{group['file'].name}`; it only registers the showcase)"
                if platform:
                    line += "; platform actuals: " + ", ".join(f"`{rel(f)}`" for f in platform)
                body.append(line + ". For a single animation use its own Copy line below.")
            if deps:
                body.append("Whole set also needs: " + ", ".join(f"`{rel(f)}`" for f in deps) + ".")
            body.append("")
            for d in group["demos"]:
                path = demo_file(d["composable"])
                where = f"`{d['call']}` in `{rel(path)}`" if path else f"`{d['call']}`"
                text = caption_for(path, d["title"], d["subtitle"]).rstrip()
                if text[-1] not in ".!?":
                    text += "."
                body.append(f"- **{d['title']}** (`{d['id']}`): {text} → {where}")
                if path:
                    body.append(f"  {copy_line(path, registration or group['file'])}")
                entries.append({"id": d["id"], "text": f"{d['title']} {text} {d['subtitle']}"})
            body.append("")

    lines += keyword_index(entries) + body
    OUT.write_text("\n".join(lines).rstrip() + "\n")
    print(f"Wrote {rel(OUT)}: {total} animations, {len(categories)} categories")


if __name__ == "__main__":
    main()
