#!/usr/bin/env python3
"""Detect fork-injection lines lost to an upstream sync commit.

After each `update to X.Y.Z` squash merge, run:

    python3 Tools/scripts/detect_injection_loss.py <sync_sha>

For every file the sync touches that our fork commits also touched before the
sync, the script intersects lines added by our commits with lines removed by
the sync. Every hit is a candidate for manual triage: either the sync really
dropped our injection (bug), or our code was adapted to an upstream API change
(benign), or upstream absorbed our mechanism (benign). Static line matching
cannot tell these apart — the output is a review list, not a verdict.

    python3 Tools/scripts/detect_injection_loss.py <sync_sha> --reverse

Reverse mode intersects the other side (upstream additions × fork-dead
entities): identifiers whose creation the fork commented out or deleted
(in the fork range or already dead at the frozen base) and that sync-added
lines reference — upstream code resurrecting a fork-disabled entity, e.g.
unconditional use of a button the fork commented out (NPE class,
compile-green, file not in conflict). Same triage-list philosophy.

Validated on syncs 12.10.3 (c21f3c2a8e) and 12.10.6 (f9d33af325): 3 hits, all
benign after triage. Reverse mode validated on 13.2.1 (6d9dacfd36): 101
candidates / 40 files — flags the LoginActivity proxyButtonView NPE
(1b4f934bd8) alongside benign upstream re-creations (triage list).
"""

import argparse
import re
import subprocess
import sys
from collections import Counter, defaultdict

FROZEN_BASE = "nagramx/dev"
SYNC_SUBJECT_PREFIX = "update to"

IDENT_RE = re.compile(r"[A-Za-z_][A-Za-z0-9_]{2,}")
KEYWORDS = {
    "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
    "class", "const", "continue", "default", "do", "double", "else", "enum",
    "extends", "false", "final", "finally", "float", "for", "goto", "if",
    "implements", "import", "instanceof", "int", "interface", "long", "native",
    "new", "null", "package", "private", "protected", "public", "return",
    "short", "static", "strictfp", "super", "switch", "synchronized", "this",
    "throw", "throws", "transient", "true", "try", "void", "volatile", "while",
    "val", "var", "when", "object", "data", "sealed", "override", "open",
    "fun", "internal", "lazy", "init", "companion", "suspend", "typealias",
}
STOPWORDS = {
    "context", "view", "holder", "adapter", "parent", "position", "theme",
    "string", "color", "size", "count", "index", "value", "name", "type",
    "text", "time", "user", "chat", "dialog", "message", "file", "path",
    "item", "state", "list", "array", "viewGroup", "layout", "params",
    "resources", "intent", "bundle", "account", "instance", "shared",
    "preferences", "application", "activity", "fragment", "nullable",
    "overridePendingTransition", "getApplicationContext", "bitmap", "canvas",
    "paint", "rect", "rectF", "drawable", "animation", "animator", "listener",
    "callback", "handler", "runnable", "thread", "result", "values", "entry",
    "keys", "inner", "outer", "width", "height", "left", "right", "top",
    "bottom", "center", "alpha", "progress", "tag", "error", "cause", "args",
    "savedInstanceState", "inflater", "container", "saved", "first", "last",
    "next", "prev", "current", "total", "limit", "offset", "key", "extra",
}


def git(*args: str) -> str:
    result = subprocess.run(
        ["git", *args],
        capture_output=True,
        check=False,
        encoding="utf-8",
        errors="replace",
    )
    if result.returncode != 0:
        sys.exit(f"git {' '.join(args)} failed:\n{result.stderr.strip()}")
    return result.stdout


def git_soft(*args: str):
    """git output or None on failure (e.g. `show rev:path` for a missing file)."""
    result = subprocess.run(
        ["git", *args],
        capture_output=True,
        check=False,
        encoding="utf-8",
        errors="replace",
    )
    return result.stdout if result.returncode == 0 else None


def strip_line(raw: str) -> str:
    return raw[1:].strip() if raw else ""


def is_noise(line: str) -> bool:
    if len(line) < 8:
        return True
    if line.startswith("@") and line[1:].replace(" ", "").isalpha():
        return True
    return all(ch in "{}()[];,.=<>+-*!:?|&" or ch.isspace() for ch in line)


def is_ours(commit_subject: str) -> bool:
    return not commit_subject.lower().startswith(SYNC_SUBJECT_PREFIX)


def sync_removed_lines(sync_sha: str, path: str) -> set:
    diff = git("diff", "--no-renames", f"{sync_sha}^", sync_sha, "--", path)
    removed = set()
    for line in diff.splitlines():
        if line.startswith("---"):
            continue
        if line.startswith("-"):
            stripped = strip_line(line)
            if stripped and not is_noise(stripped):
                removed.add(stripped)
    return removed


def our_added_lines(sync_sha: str, base_ref: str, path: str) -> set:
    log = git(
        "log", f"{base_ref}..{sync_sha}^", "--format=%H %s", "--", path
    )
    our_commits = []
    for entry in log.splitlines():
        if not entry:
            continue
        _sha, subject = entry.split(" ", 1)
        if is_ours(subject):
            our_commits.append(entry.split(" ", 1)[0])
    added = set()
    for sha in our_commits:
        diff = git("show", sha, "--", path)
        for line in diff.splitlines():
            if line.startswith("+++"):
                continue
            if line.startswith("+"):
                stripped = strip_line(line)
                if stripped and not is_noise(stripped):
                    added.add(stripped)
    return added


def sync_added_lines(sync_sha: str, path: str) -> set:
    diff = git("diff", "--no-renames", f"{sync_sha}^", sync_sha, "--", path)
    added = set()
    for line in diff.splitlines():
        if line.startswith("+++"):
            continue
        if line.startswith("+"):
            stripped = strip_line(line)
            if stripped and not is_noise(stripped):
                added.add(stripped)
    return added


def our_removed_lines(sync_sha: str, base_ref: str, path: str) -> set:
    """Lines our fork commits deleted in `path` (incl. what we commented out)."""
    log = git("log", f"{base_ref}..{sync_sha}^", "--format=%H", "--", path)
    removed = set()
    for sha in log.split():
        diff = git("show", sha, "--", path)
        for line in diff.splitlines():
            if line.startswith("---"):
                continue
            if line.startswith("-"):
                stripped = strip_line(line)
                if stripped and not is_noise(stripped):
                    removed.add(stripped)
    return removed


def identifiers(lines: set) -> dict:
    """lowerCamel identifiers per line-sample; classes/constants/keywords/imports skipped."""
    found: dict = {}
    for line in lines:
        if line.startswith(("import ", "package ")):
            continue
        for ident in IDENT_RE.findall(line):
            if ident[0].isupper() or ident in KEYWORDS or ident in STOPWORDS:
                continue
            found.setdefault(ident, line)
    return found


CREATION_RE = re.compile(r"=\s*new\s|\w+\.addView\(|findViewById\(|inflate\(|fromHtml\(")


def split_code_comments(content: str):
    """(active code, comment text) with string literals kept inside code."""
    code, comment = [], []
    i, n, state = 0, len(content), "code"
    while i < n:
        c = content[i]
        nxt = content[i + 1] if i + 1 < n else ""
        if state == "code":
            if c == "/" and nxt == "/":
                state, i = "line", i + 2
                comment.append("\n")
            elif c == "/" and nxt == "*":
                state, i = "block", i + 2
                comment.append("\n")
            elif c == '"':
                state = "str"
                code.append(c)
                i += 1
            elif c == "'":
                state = "chr"
                code.append(c)
                i += 1
            else:
                code.append(c)
                i += 1
        elif state == "line":
            if c == "\n":
                state = "code"
                code.append("\n")
            comment.append(c)
            i += 1
        elif state == "block":
            if c == "*" and nxt == "/":
                state, i = "code", i + 2
                comment.append("\n")
            else:
                comment.append(c)
                i += 1
        elif state in ("str", "chr"):
            if c == "\\":
                code.append(content[i:i + 2])
                i += 2
                continue
            if (state == "str" and c == '"') or (state == "chr" and c == "'"):
                state = "code"
            code.append(c)
            i += 1
    return "".join(code), "".join(comment)


def creation_ids(text: str) -> dict:
    """Identifiers whose creation lives in `text` (commented-out / removed code)."""
    found: dict = {}
    for line in text.splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith(("import ", "package ")):
            continue
        if not CREATION_RE.search(line) or is_noise(stripped):
            continue
        for ident in IDENT_RE.findall(line):
            if ident[0].isupper() or ident in KEYWORDS or ident in STOPWORDS:
                continue
            found.setdefault(ident, stripped)
    return found


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("sync_sha", help="squash-merge commit of an upstream sync")
    parser.add_argument(
        "--base",
        default=FROZEN_BASE,
        help=f"frozen fork base ref (default: {FROZEN_BASE})",
    )
    parser.add_argument(
        "--reverse",
        action="store_true",
        help="intersect upstream additions × fork deletions instead",
    )
    args = parser.parse_args()

    subject = git("log", "-1", "--format=%s", args.sync_sha).strip()
    if is_ours(subject):
        print(f"warning: {args.sync_sha[:10]} may not be a sync commit: «{subject}»")

    sync_files = git(
        "diff", "--no-renames", f"{args.sync_sha}^", args.sync_sha, "--name-only"
    ).splitlines()
    risk_files = [f for f in sync_files if f.endswith((".java", ".kt"))]

    touched_before = defaultdict(bool)
    log_all = git(
        "log", f"{args.base}..{args.sync_sha}^", "--format=%H %s", "--name-only"
    )
    current_is_ours = False
    for line in log_all.splitlines():
        if not line:
            continue
        if len(line) > 40 and line[40] == " " and all(
            c in "0123456789abcdef" for c in line[:40]
        ):
            current_is_ours = is_ours(line[41:])
        elif current_is_ours:
            touched_before[line] = True

    hits = 0
    checked = 0
    hit_files = 0

    if args.reverse:
        # Dead entities often predate the frozen base (fork lineage commented
        # them out long ago), so files need NOT be touched by our commits —
        # every sync-touched java/kt file is scanned. The fork-range log is
        # consulted only for files that have commented-out creations.
        per_file = []
        for path in risk_files:
            checked += 1
            raw = git_soft("show", f"{args.sync_sha}^:{path}")
            if not raw:
                continue
            _code, comment = split_code_comments(raw)
            dead = creation_ids(comment)
            if not dead:
                continue
            for ident, line in creation_ids(
                "\n".join(our_removed_lines(args.sync_sha, args.base, path))
            ).items():
                dead.setdefault(ident, line)
            per_file.append((path, dead, identifiers(sync_added_lines(args.sync_sha, path))))
        # generic creation names (addButton, sendButton, …) recur across files;
        # an entity-specific resurrection lives in one or two
        dead_freq = Counter()
        for _path, dead, _added in per_file:
            dead_freq.update(dead.keys())
        for path, dead, added in per_file:
            found = [
                (ident, dead[ident], added[ident])
                for ident in sorted(dead.keys() & added.keys())
                if dead_freq[ident] <= 3
            ]
            if found:
                hit_files += 1
                hits += len(found)
                print(f"{path}: {len(found)} cross-side candidate(s)")
                for ident, dead_line, sync_line in found:
                    print(f"  - {ident}: dead creation «{dead_line[:90]}»")
                    print(f"      sync added «{sync_line[:90]}»")
    else:
        for path in risk_files:
            if not touched_before.get(path):
                continue
            checked += 1
            lost = our_added_lines(args.sync_sha, args.base, path) & sync_removed_lines(
                args.sync_sha, path
            )
            if lost:
                hits += len(lost)
                hit_files += 1
                print(f"{path}: {len(lost)} candidate line(s) removed by sync")
                for line in sorted(lost):
                    print(f"  - {line[:120]}")

    direction = (
        "sync-added reference(s) to fork-dead entities"
        if args.reverse
        else "candidate line(s) removed by sync"
    )
    print(f"\nchecked {checked} risk file(s), {hits} {direction} in {hit_files} file(s)")
    print(
        "triage each hit: guard/adapt (real) / dead code cleanup / churn (ok)"
        if args.reverse
        else "triage each hit: real loss (restore) / API adaptation (ok) / upstream absorbed (ok)"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
