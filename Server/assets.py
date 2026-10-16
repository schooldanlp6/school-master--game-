#!/usr/bin/env python3
# Author: danlp6
"""Asset housekeeping for the static assets server (Server/Assets).

Derives the expected asset tree from a single definitions file
(assets.defs.json), which points at the real sources of truth (Java enums,
CardMeta.json, Game-Plugin jars). Every path is <type>/<group>/<name>.<ext>,
all lower-case; plugin assets are <type>/plugins/<plugin>/<name>.<ext>.

Every name must match [a-z0-9][a-z0-9_.-]* after lower-casing, so paths are
URL safe without encoding (see API.md).

Usage: assets.py <command> [options]   (see --help; normally run via make)
All default paths are relative to this script's folder (Server/).
"""
import argparse
import hashlib
import json
import re
import shutil
import sys
import zipfile
from pathlib import Path

ENUM_RE = re.compile(r"\benum\s+\w+\s*\{(.*?)(?:;|\})", re.S)
CONST_RE = re.compile(r"^\s*([A-Z][A-Z0-9_]*)\s*(?:\(.*?\))?\s*$", re.S)
INDEX_NAME = "index.json"
PLUGIN_NAME_RE = re.compile(r"^[A-Za-z0-9][A-Za-z0-9_.-]*$")  # same rule as PluginDefsInitiator
SEGMENT_RE = re.compile(r"^[a-z0-9][a-z0-9_.-]*$")  # one lower-cased path segment, see API.md
HERE = Path(__file__).resolve().parent


# --- derivation -------------------------------------------------------------

def enum_values(java_file: Path) -> list[str]:
    """Return the constant names of the first enum declared in a Java file."""
    src = re.sub(r"//.*?$|/\*.*?\*/", "", java_file.read_text(encoding="utf-8"), flags=re.S | re.M)
    m = ENUM_RE.search(src)
    if not m:
        sys.exit(f"error: no enum found in {java_file}")
    names = []
    for part in m.group(1).split(","):
        c = CONST_RE.match(part)
        if c:
            names.append(c.group(1))
    return names


def card_ids(meta_file: Path) -> list[str]:
    return list(json.loads(meta_file.read_text(encoding="utf-8")).keys())


def plugin_assets(plugin_dir: Path, group: str, types: dict, wanted: list[str]) -> dict[str, tuple[Path, str]]:
    """Map asset path -> (jar, entry) for every assets/<type>/<name>.<ext> shipped in a plugin jar.

    Accepts the same jars as PluginDefsInitiator: plugin.json with string main/version/name, a
    valid name, and on duplicate names (case-insensitive) the first jar in sorted order wins.
    """
    out = {}
    seen = set()
    for jar in sorted(plugin_dir.glob("*.jar")) if plugin_dir.is_dir() else []:
        try:
            with zipfile.ZipFile(jar) as z:
                meta = json.loads(z.read("plugin.json"))
                if not isinstance(meta, dict):
                    raise ValueError("plugin.json is not a JSON object")
                for key in ("main", "version", "name"):
                    if not isinstance(meta.get(key), str):
                        raise ValueError(f'plugin.json is missing "{key}" (must be a string)')
                name = meta["name"]
                if not PLUGIN_NAME_RE.match(name):
                    raise ValueError(f"invalid plugin name '{name}'")
                if name.lower() in seen:
                    raise ValueError(f"a plugin named '{name}' is already loaded")
                seen.add(name.lower())
                for entry in z.namelist():
                    parts = entry.split("/")
                    if len(parts) != 3 or parts[0] != "assets" or parts[1] not in wanted or not parts[2]:
                        continue
                    t, file = parts[1], parts[2]
                    stem, _, ext = file.rpartition(".")
                    if ext.lower() != types[t] or not stem:
                        print(f"warning: {jar.name}: skipping {entry} (expected .{types[t]})", file=sys.stderr)
                        continue
                    if not SEGMENT_RE.match(stem.lower()):
                        print(f"warning: {jar.name}: skipping {entry} (name must match {SEGMENT_RE.pattern})",
                              file=sys.stderr)
                        continue
                    path = f"{t}/{group}/{name.lower()}/{stem.lower()}.{types[t]}"
                    if path in out:
                        print(f"warning: {jar.name}: {entry} and {out[path][1]} both map to {path}, keeping the first",
                              file=sys.stderr)
                        continue
                    out[path] = (jar, entry)
        except (KeyError, ValueError, zipfile.BadZipFile) as e:  # KeyError: no plugin.json in the jar
            print(f"warning: skipping plugin {jar.name}: {e}", file=sys.stderr)
    return out


def derive(defs_file: Path, repo: Path) -> list[str]:
    defs = json.loads(defs_file.read_text(encoding="utf-8"))
    types = defs["types"]
    paths = []
    for group, spec in defs["groups"].items():
        src = spec["source"]
        for t in spec["types"]:
            if t not in types:
                sys.exit(f"error: group '{group}' uses unknown type '{t}'")
        if "pluginJars" in src:
            # per plugin, only the files it actually ships (no type x name cross product)
            paths += plugin_assets(repo / src["pluginJars"], group, types, spec["types"])
            continue
        if "enum" in src:
            names = enum_values(repo / src["enum"])
        elif "cardMeta" in src:
            names = card_ids(repo / src["cardMeta"])
        elif "names" in src:
            names = src["names"]
        else:
            sys.exit(f"error: group '{group}' has no known source")
        for n in names:
            if not SEGMENT_RE.match(n.lower()):
                sys.exit(f"error: group '{group}': name '{n}' must match {SEGMENT_RE.pattern} after lower-casing")
        for t in spec["types"]:
            paths += [f"{t}/{group}/{n.lower()}.{types[t]}" for n in names]
    return sorted(set(paths))


def sources(defs_file: Path, repo: Path) -> list[Path]:
    """Every file/folder the manifest is derived from (for make prerequisites)."""
    defs = json.loads(defs_file.read_text(encoding="utf-8"))
    out = [defs_file]
    for spec in defs["groups"].values():
        src = spec["source"]
        for key in ("enum", "cardMeta"):
            if key in src:
                out.append(repo / src[key])
        if "pluginJars" in src:
            d = repo / src["pluginJars"]
            if d.is_dir():  # the folder too: its mtime changes when a jar is removed
                out += [d, *sorted(d.glob("*.jar"))]
    return out


# --- helpers ----------------------------------------------------------------

def read_manifest(manifest: Path) -> list[str]:
    if not manifest.exists():
        sys.exit(f"error: {manifest} missing, run 'make asset-paths' first")
    return [l.strip() for l in manifest.read_text().splitlines() if l.strip()]


def actual(root: Path) -> list[str]:
    if not root.exists():
        return []
    # dotfiles (.gitkeep, ...) are never assets, see SEGMENT_RE
    return sorted(str(p.relative_to(root)) for p in root.rglob("*")
                  if p.is_file() and p.name != INDEX_NAME and not p.name.startswith("."))


def dirs_deepest_first(root: Path) -> list[Path]:
    """All directories, deepest first, so parents are empty once children are gone."""
    return sorted((d for d in root.rglob("*") if d.is_dir()), key=lambda d: len(d.parts), reverse=True)


# --- commands ---------------------------------------------------------------

def cmd_paths(a):
    paths = derive(a.defs, a.repo)
    a.manifest.write_text("\n".join(paths) + "\n")
    print(f"{len(paths)} asset paths -> {a.manifest}")


def cmd_sources(a):
    print(" ".join(str(p) for p in sources(a.defs, a.repo)))


def cmd_list(a):
    for p in actual(a.root):
        print(p)


def cmd_tree(a):
    made = 0
    for p in read_manifest(a.manifest):
        f = a.root / p
        f.parent.mkdir(parents=True, exist_ok=True)
        if a.placeholders and not f.exists():
            f.touch()
            made += 1
    print(f"tree ready under {a.root}" + (f", {made} placeholders created" if a.placeholders else ""))


def cmd_check(a):
    want, have = set(read_manifest(a.manifest)), set(actual(a.root))
    missing = sorted(want - have)
    empty = sorted(p for p in want & have if (a.root / p).stat().st_size == 0)
    orphans = sorted(have - want)
    for label, items in (("missing", missing), ("placeholder", empty), ("orphan", orphans)):
        for p in items:
            print(f"{label:12} {p}")
    print(f"\n{len(want)} expected, {len(want & have) - len(empty)} ok, "
          f"{len(missing)} missing, {len(empty)} placeholders, {len(orphans)} orphans")
    if a.strict and (missing or empty):
        sys.exit(1)


def cmd_clean(a):
    want = set(read_manifest(a.manifest))
    doomed = [a.root / p for p in actual(a.root) if p not in want]
    if a.prune_empty:
        doomed += [a.root / p for p in actual(a.root) if p in want and (a.root / p).stat().st_size == 0]
    verb = "rm" if a.force else "would rm"
    for f in doomed:
        print(f"{verb} {f}")
        if a.force:
            f.unlink()
    if a.root.exists():
        for d in dirs_deepest_first(a.root):
            if not any(d.iterdir()):
                print(f"{verb} {d}/")
                if a.force:
                    d.rmdir()
    if not a.force and doomed:
        print("\ndry run, pass FORCE=1 to delete")


def cmd_gather(a):
    if not a.src or not a.src.is_dir():
        sys.exit("error: SRC=<dir> must point at a directory with source assets")
    copied = 0
    for p in read_manifest(a.manifest):
        # exact relative path first, then <group>/[<plugin>/]<file> anywhere, then bare <file>
        sub = p.split("/", 1)[1]
        name = p.rsplit("/", 1)[1]
        cands = [a.src / p, *sorted(a.src.rglob(sub)), *sorted(a.src.rglob(name))]
        hit = next((c for c in cands if c.is_file() and c.stat().st_size > 0), None)
        if not hit:
            continue
        dst = a.root / p
        if dst.exists() and dst.stat().st_size and dst.read_bytes() == hit.read_bytes():
            continue
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(hit, dst)
        print(f"{hit} -> {dst}")
        copied += 1
    print(f"{copied} assets gathered from {a.src}")


def cmd_plugins(a):
    """Copy the assets shipped inside plugin jars into the root (the jar is their source of truth)."""
    defs = json.loads(a.defs.read_text(encoding="utf-8"))
    copied = found = 0
    for group, spec in defs["groups"].items():
        if "pluginJars" not in spec["source"]:
            continue
        for p, (jar, entry) in sorted(plugin_assets(a.repo / spec["source"]["pluginJars"], group,
                                                    defs["types"], spec["types"]).items()):
            found += 1
            with zipfile.ZipFile(jar) as z:
                data = z.read(entry)
            dst = a.root / p
            if dst.exists() and dst.read_bytes() == data:
                continue
            dst.parent.mkdir(parents=True, exist_ok=True)
            dst.write_bytes(data)
            print(f"{jar.name}!{entry} -> {dst}")
            copied += 1
    print(f"{copied} of {found} plugin assets updated")


def cmd_index(a):
    entries = {}
    for p in read_manifest(a.manifest):
        f = a.root / p
        if f.is_file() and f.stat().st_size:
            entries[p] = {"size": f.stat().st_size, "sha256": hashlib.sha256(f.read_bytes()).hexdigest()}
    (a.root / INDEX_NAME).write_text(json.dumps({"assets": entries}, indent=2, sort_keys=True) + "\n")
    print(f"{len(entries)} assets indexed -> {a.root / INDEX_NAME}")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--repo", type=Path, default=HERE, help="base for paths in the defs (default: Server/)")
    ap.add_argument("--defs", type=Path, help="default: <repo>/assets.defs.json")
    ap.add_argument("--root", type=Path, help="asset root to serve, default: <repo>/Assets")
    ap.add_argument("--manifest", type=Path, help="default: <repo>/assets.manifest")
    sub = ap.add_subparsers(dest="cmd", required=True)
    sub.add_parser("paths", help="derive expected paths into the manifest").set_defaults(fn=cmd_paths)
    sub.add_parser("sources", help="print the files the manifest is derived from").set_defaults(fn=cmd_sources)
    sub.add_parser("list", help="list files present under the root").set_defaults(fn=cmd_list)
    p = sub.add_parser("tree", help="create folders from the manifest")
    p.add_argument("--placeholders", action="store_true")
    p.set_defaults(fn=cmd_tree)
    p = sub.add_parser("check", help="diff manifest vs. root")
    p.add_argument("--strict", action="store_true", help="exit 1 on missing/placeholder assets")
    p.set_defaults(fn=cmd_check)
    p = sub.add_parser("clean", help="remove orphans and empty dirs (dry run by default)")
    p.add_argument("--force", action="store_true")
    p.add_argument("--prune-empty", action="store_true", help="also remove empty placeholder files")
    p.set_defaults(fn=cmd_clean)
    p = sub.add_parser("gather", help="copy source assets into the root")
    p.add_argument("--src", type=Path)
    p.set_defaults(fn=cmd_gather)
    sub.add_parser("plugins", help="extract assets shipped in plugin jars into the root").set_defaults(fn=cmd_plugins)
    sub.add_parser("index", help="write index.json (path, size, sha256) into the root").set_defaults(fn=cmd_index)
    a = ap.parse_args()
    a.defs = a.defs or a.repo / "assets.defs.json"
    a.root = a.root or a.repo / "Assets"
    a.manifest = a.manifest or a.repo / "assets.manifest"
    a.fn(a)


if __name__ == "__main__":
    main()
