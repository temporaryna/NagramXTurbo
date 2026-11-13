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

    python3 Tools/scripts/detect_injection_loss.py <sync_sha> --xml

Same forward intersection, but for .xml files under the project tree
(AndroidManifest permissions/receivers/tools, res/values resources; layout/
anim churn excluded). The fork side is scanned from the first "update to"
commit, not from the frozen base — manifest additions can predate the freeze
(inherited NagramX lineage, e.g. passkeys). A fork permission or manifest
entry dropped by the sync never reaches java triage — this is its detector
(13.2.1: CREDENTIAL_MANAGER_SET_ORIGIN).

    python3 Tools/scripts/detect_injection_loss.py <sync_sha> --residual-api \
        [--api-marker useSimpleActionBar] [--old-api 'actionBar\\.']

Residual-API sweep (the actionBar-NPE class): files whose sync diff added the
new-API marker (default useSimpleActionBar) are grepped for residual calls of
the old API (default 'actionBar\\.' — the field is null in the new context).
Every hit = suspect with file:line; signal:noise ~3:1 is expected — triage on
top of the list (13.2.1: 80 screens converted, 10 suspects, 3 real crashes).

    python3 Tools/scripts/detect_injection_loss.py <sync_sha> --fork-alive

fork-symbol-alive check (heuristic, needs triage): fork-created symbols
(declaration added by a fork commit) that are alive pre-sync but have 0
references post-sync — either gone from the whole file, or lost from a
specific shared method whose upstream version dropped the fork call-site
(13.2.1: video-conversion-progress — the symbol survived the merge, the
rewritten draw path stopped using it). Cross-file frequency filter drops
generic names; brace-based method extraction is approximate, so method
attribution and the noise floor are triage inputs, not facts.

Validated on syncs 12.10.3 (c21f3c2a8e) and 12.10.6 (f9d33af325): 3 hits, all
benign after triage. Reverse mode validated on 13.2.1 (6d9dacfd36): 101
candidates / 40 files — flags the LoginActivity proxyButtonView NPE
(1b4f934bd8) alongside benign upstream re-creations (triage list). xml,
residual-api and fork-alive modes validated on 13.2.1 (see
docs/memory/upstream-merge.md for the recorded outputs).
"""

import argparse
import re
import subprocess
import sys
from collections import Counter, defaultdict

FROZEN_BASE = "nagramx/dev"
SYNC_SUBJECT_PREFIX = "update to"

XML_ROOT = "TMessagesProj/src/main/"
XML_SKIP_MARKERS = ("/res/layout", "/res/anim")

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


def fork_epoch() -> str:
    """First "update to" commit — the moment the repo became a sync-following
    fork. Everything non-sync after it is fork lineage (incl. inherited
    NagramX work that predates the frozen base)."""
    for entry in git("log", "--reverse", "--format=%H %s").splitlines():
        if not entry:
            continue
        sha, subject = entry.split(" ", 1)
        if not is_ours(subject):
            return sha
    return ""


METHOD_START_RE = re.compile(
    r"^    (?:@\w+\s+)*(?:[A-Za-z_][\w<>\[\], .]*\s+)?([A-Za-z_]\w*)\s*\("
)
STRING_LITERAL_RE = re.compile(r'"(\\.|[^"\\])*"|\'(\\.|[^\'\\])*\'')


def _brace_delta(line: str) -> int:
    code = split_code_comments(line)[0]
    code = STRING_LITERAL_RE.sub("", code)
    return code.count("{") - code.count("}")


def method_spans(content: str) -> list:
    """(name, start, end) line-index spans of 4-space-indent methods.

    Brace-depth extraction on cleaned lines — string literals with braces or
    generated code can skew a span; good enough for a triage heuristic.
    """
    lines = content.splitlines()
    spans = []
    i, n = 0, len(lines)
    while i < n:
        m = METHOD_START_RE.match(lines[i])
        if m and m.group(1) not in KEYWORDS:
            opened = -1
            for j in range(i, min(i + 6, n)):
                if "{" in split_code_comments(lines[j])[0]:
                    opened = j
                    break
                if lines[j].rstrip().endswith(";"):
                    break
            if opened >= 0:
                depth = 0
                k = opened
                while k < n:
                    depth += _brace_delta(lines[k])
                    if depth <= 0:
                        break
                    k += 1
                spans.append((m.group(1), i, k))
                i = k + 1
                continue
        i += 1
    return spans


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("sync_sha", help="squash-merge commit of an upstream sync")
    parser.add_argument(
        "--base",
        default=FROZEN_BASE,
        help=f"frozen fork base ref (default: {FROZEN_BASE})",
    )
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument(
        "--reverse",
        action="store_true",
        help="intersect upstream additions × fork deletions instead",
    )
    mode.add_argument(
        "--xml",
        action="store_true",
        help="forward intersection on project xml (manifest/resources)",
    )
    mode.add_argument(
        "--residual-api",
        dest="residual_api",
        action="store_true",
        help="files where the sync added the new-API marker, grep residual old-API calls",
    )
    mode.add_argument(
        "--fork-alive",
        dest="fork_alive",
        action="store_true",
        help="fork-added identifiers alive pre-sync with 0 references post-sync (heuristic)",
    )
    parser.add_argument(
        "--api-marker",
        default="useSimpleActionBar",
        help="new-API marker for --residual-api (default: useSimpleActionBar)",
    )
    parser.add_argument(
        "--old-api",
        default=r"actionBar\.",
        help="old-API regex for --residual-api (default: actionBar\\.)",
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

    if args.residual_api:
        for path in risk_files:
            added = sync_added_lines(args.sync_sha, path)
            if not any(re.search(args.api_marker, line) for line in added):
                continue
            content = git_soft("show", f"{args.sync_sha}:{path}")
            if not content:
                continue
            checked += 1
            suspects = [
                (no, line)
                for no, line in enumerate(content.splitlines(), 1)
                if not line.lstrip().startswith("//") and re.search(args.old_api, line)
            ]
            if suspects:
                hit_files += 1
                hits += len(suspects)
                print(
                    f"{path}: {len(suspects)} residual «{args.old_api}» call(s) "
                    f"(marker «{args.api_marker}» added by sync)"
                )
                for no, line in suspects:
                    print(f"  - {no}: {line.strip()[:120]}")
        direction = "residual old-API call(s) after new-API marker"
        triage = "triage each hit: old field null in new context → migrate (crash) / both APIs live (ok)"
    elif args.fork_alive:
        per_file = []
        for path in risk_files:
            if not touched_before.get(path):
                continue
            added = our_added_lines(args.sync_sha, args.base, path)
            if not added:
                continue
            fork_content = git_soft("show", f"{args.sync_sha}^:{path}")
            post_content = git_soft("show", f"{args.sync_sha}:{path}")
            if not fork_content or post_content is None:
                continue
            checked += 1
            fork_lines = fork_content.splitlines()
            post_lines = post_content.splitlines()
            fork_spans = method_spans(fork_content)
            fork_bodies = defaultdict(list)
            for name, s, e in fork_spans:
                fork_bodies[name].append("\n".join(fork_lines[s:e + 1]))
            post_bodies = defaultdict(list)
            for name, s, e in method_spans(post_content):
                post_bodies[name].append("\n".join(post_lines[s:e + 1]))
            per_file.append(
                (path, identifiers(added), added, fork_spans, fork_bodies, post_bodies, fork_lines, fork_content, post_content)
            )
        # generic names (setTextColor, dismiss, …) recur across files; a
        # fork-specific symbol lives in one or two — same filter as reverse
        sym_freq = Counter()
        for _path, syms, *_rest in per_file:
            sym_freq.update(syms.keys())
        for path, syms, added, fork_spans, fork_bodies, post_bodies, fork_lines, fork_content, post_content in per_file:
            # symbols created by the fork: their declaration (method start or
            # field decl) was added by a fork commit — divergence of an
            # upstream-owned field/method is not a fork-injection signal
            fork_created = set()
            for name, s, _e in fork_spans:
                if fork_lines[s].strip() in added:
                    fork_created.add(name)
            for raw in fork_lines:
                stripped = raw.strip()
                if stripped in added and "(" not in stripped and re.match(
                    r"^(?:private|protected|public|static|final|volatile)\b", stripped
                ):
                    fork_created.update(
                        ident
                        for ident in IDENT_RE.findall(stripped)
                        if not ident[0].isupper() and ident not in KEYWORDS
                    )
            found = []
            for sym, line in sorted(syms.items()):
                if sym_freq[sym] > 3:
                    continue
                pat = rf"\b{re.escape(sym)}\b"
                if not re.search(pat, fork_content):
                    continue
                if not re.search(pat, post_content):
                    found.append(("gone from file", sym, line))
                    continue
                if sym not in fork_created:
                    continue
                for name, bodies in fork_bodies.items():
                    if name not in post_bodies:
                        continue
                    if not any(re.search(pat, body) for body in bodies):
                        continue
                    if not any(re.search(pat, body) for body in post_bodies[name]):
                        found.append((f"lost from method {name}()", sym, line))
            if found:
                hit_files += 1
                hits += len(found)
                print(f"{path}: {len(found)} fork-alive candidate(s) [heuristic, needs triage]")
                for where, sym, line in found:
                    print(f"  - {sym} ({where}): fork-added «{line[:80]}»")
        direction = "fork symbol(s) alive but unreferenced post-sync (file or shared method)"
        triage = "triage each hit: real loss (restore) / dead code cleanup (ok) / upstream absorbed (ok)"
    elif args.xml:
        # Manifest/resources additions can predate the frozen base (inherited
        # NagramX lineage, e.g. passkeys), so the fork side is scanned from the
        # first sync commit, not from --base.
        lineage_ref = fork_epoch() or args.base
        xml_files = [
            f
            for f in sync_files
            if f.endswith(".xml")
            and f.startswith(XML_ROOT)
            and not any(marker in f for marker in XML_SKIP_MARKERS)
        ]
        for path in xml_files:
            checked += 1
            lost = our_added_lines(args.sync_sha, lineage_ref, path) & sync_removed_lines(
                args.sync_sha, path
            )
            if lost:
                hits += len(lost)
                hit_files += 1
                print(f"{path}: {len(lost)} candidate line(s) removed by sync")
                for line in sorted(lost):
                    print(f"  - {line[:120]}")
        direction = "candidate xml line(s) removed by sync"
        triage = "triage each hit: real loss (restore) / resource churn (ok) / upstream renamed (ok)"
    elif args.reverse:
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
        direction = "sync-added reference(s) to fork-dead entities"
        triage = "triage each hit: guard/adapt (real) / dead code cleanup / churn (ok)"
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
        direction = "candidate line(s) removed by sync"
        triage = "triage each hit: real loss (restore) / API adaptation (ok) / upstream absorbed (ok)"

    print(f"\nchecked {checked} risk file(s), {hits} {direction} in {hit_files} file(s)")
    print(triage)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
