#!/usr/bin/env python3
"""Parity batch/check/apply/clean/migrate tool for fork UI strings.

Sources: strings_turbo.xml (all fork strings, the only writable space) +
frozen legacy sets strings_na/nax/neko. Baselines for ownership checks are
computed from local git refs (nagramx/dev is frozen forever; official/master
filters Telegram-added keys) — no manifest files.

Modes:
  --batches --locale <L> [--out DIR]   write missing-key batch files (key | source | EN | RU)
  --apply --locale <L> --input DIR     merge translated batch outputs into values-<L>/
  --check [--all | --locale <L>]       validate parity/placeholders/escaping/XML/hygiene/frozen-spaces
  --clean                              hygiene pass: dead keys, cross-dup transfer, multi-line split, BiDi strip
  --migrate-turbo                      one-time move of fork keys from strings.xml/na/nax/neko into strings_turbo.xml
"""
import argparse
import os
import re
import subprocess
import sys
try:
    from defusedxml.ElementTree import parse as safe_parse
except ImportError:
    from xml.etree.ElementTree import parse as safe_parse

REPO = os.path.join(os.path.dirname(__file__), "..", "..")
RES = os.path.join(REPO, "TMessagesProj", "src", "main", "res")
TURBO_SOURCE = "strings_turbo.xml"
LEGACY_SOURCES = ["strings_na.xml", "strings_nax.xml", "strings_neko.xml"]
SET_SOURCES = [TURBO_SOURCE] + LEGACY_SOURCES
STRINGS_XML = "strings.xml"
ALL_SOURCES = SET_SOURCES + [STRINGS_XML]
LOCALES = ["ar", "es", "es-rES", "pt-rBR", "fa-rIR", "tr-rTR", "uk-rUA",
           "pl-rPL", "it-rIT", "in-rID", "vi-rVN", "ja-rJP", "ko",
           "zh-rCN", "zh-rTW", "ru-rRU"]
EN_DIR = "values"
NAGRAMX_REF = "nagramx/dev"
OFFICIAL_REF = "official/master"
CATS = ["zero", "one", "two", "few", "many", "other"]

# Required plural forms per locale for fork plural families (CL practice of this repo).
PLURAL_FORMS = {
    "ar": ["zero", "one", "two", "few", "many", "other"],
    "ru-rRU": ["one", "few", "many", "other"],
    "uk-rUA": ["one", "few", "many", "other"],
    "pl-rPL": ["one", "few", "many", "other"],
}

# Dead since the 18-theme icon rebrand: launcher icons resolve through the
# LauncherIconController enum, none of these keys has an R.string reference.
DEAD_KEYS = {
    "AppIconGoogle", "AppIconColorful", "AppIconDarkGreen", "AppIconNiello",
    "AppIconBlue", "AppIconDarkBlue", "AppIconBlurBlue",
}

BIDI_MARKS = ["‎", "‏"]


def parse_strings(path):
    result = {}
    if not os.path.exists(path):
        return result
    root = safe_parse(path).getroot()
    for node in root.findall("string"):
        result[node.get("name")] = (
            "".join(node.itertext()),
            node.get("translatable") == "false",
        )
    return result


def load_locale(locale, source):
    return parse_strings(os.path.join(RES, f"values-{locale}", source))


def load_ref_string_keys(git_ref, res_rel):
    rel = f"TMessagesProj/src/main/res/{res_rel}"
    try:
        content = subprocess.run(
            ["git", "-C", REPO, "show", f"{git_ref}:{rel}"],
            capture_output=True, text=True, check=True).stdout
    except subprocess.CalledProcessError as error:
        sys.exit(f"Can't read {git_ref}:{rel} (add remote / git fetch {git_ref.split('/')[0]}): {error.stderr.strip()}")
    return set(re.findall(r'<string name="([^"]+)"', content))


def build_set_source_map():
    """key -> owning set file, for translatable base set keys."""
    mapping = {}
    for source in SET_SOURCES:
        for key, (_, untranslatable) in parse_strings(os.path.join(RES, EN_DIR, source)).items():
            if not untranslatable and key not in DEAD_KEYS:
                mapping[key] = source
    return mapping


def resolve_plural_cat(key):
    for cat in CATS:
        if key.endswith("_" + cat):
            return key[: -(len(cat) + 1)], cat
    return key, None


def resolve_required_forms(locale):
    return PLURAL_FORMS.get(locale, ["one", "other"])


def resolve_canonical_source(key, set_map):
    bare, _ = resolve_plural_cat(key)
    if bare in set_map:
        return set_map[bare]
    for cat in CATS:
        if f"{bare}_{cat}" in set_map:
            return set_map[f"{bare}_{cat}"]
    return None


def check_family_alive(key, all_keys):
    bare, cat = resolve_plural_cat(key)
    if cat is None:
        return key in all_keys
    return any(f"{bare}_{form}" in all_keys for form in CATS)


PLACEHOLDER_RE = re.compile(r"%(?:\d+\$)?(?P<ph>[sd])|(?P<nl>\\n|\\t)")


def placeholders(value):
    value = value or ""
    return sorted((m.group("ph") or m.group("nl")) for m in PLACEHOLDER_RE.finditer(value))


def xml_escape(value):
    return (value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"))


def iter_string_lines(path):
    with open(path, encoding="utf-8") as handle:
        return handle.read().splitlines(keepends=True)


def write_lines(path, lines):
    with open(path, "w", encoding="utf-8") as handle:
        handle.write("".join(lines))


def build_string_line_regex(key):
    return re.compile(r'^[ \t]*<string name="%s"(?:[^>])*>.*?</string>[ \t]*\n?$' % re.escape(key))


def find_string_line(lines, key):
    pattern = build_string_line_regex(key)
    for index, line in enumerate(lines):
        if pattern.match(line):
            return index
    return -1


def insert_before_close(lines, text):
    for index in range(len(lines) - 1, -1, -1):
        if "</resources>" in lines[index]:
            lines.insert(index, "    " + text + "\n")
            return True
    return False


def locale_dir(locale):
    return os.path.join(RES, f"values-{locale}")


def build_migration_key_lists():
    """Ordered (source, key) fork-key list computed from base files only.

    strings.xml fork keys use triple subtraction: a plain diff vs the
    frozen nagramx/dev would also catch Telegram keys DrKLO added after
    the NagramX freeze (they arrived here via squash updates).
    """
    entries = []
    local_xml = set(parse_strings(os.path.join(RES, EN_DIR, STRINGS_XML)))
    fork_xml = local_xml \
        - load_ref_string_keys(NAGRAMX_REF, f"{EN_DIR}/{STRINGS_XML}") \
        - load_ref_string_keys(OFFICIAL_REF, f"{EN_DIR}/{STRINGS_XML}")
    for key in parse_strings(os.path.join(RES, EN_DIR, STRINGS_XML)):
        if key in fork_xml and key not in DEAD_KEYS:
            entries.append((STRINGS_XML, key))
    for source in LEGACY_SOURCES:
        local_keys = set(parse_strings(os.path.join(RES, EN_DIR, source)))
        ours = local_keys - load_ref_string_keys(NAGRAMX_REF, f"{EN_DIR}/{source}")
        for key in parse_strings(os.path.join(RES, EN_DIR, source)):
            if key in ours and key not in DEAD_KEYS:
                entries.append((source, key))
    return entries


def collect_family_keys(keys, bare):
    return [key for key in keys if resolve_plural_cat(key)[0] == bare]


def load_turbo_lines(directory):
    path = os.path.join(directory, TURBO_SOURCE)
    if not os.path.exists(path):
        write_lines(path, ['<?xml version="1.0" encoding="utf-8"?>\n', "<resources>\n", "</resources>\n"])
    return path, iter_string_lines(path)


def move_string_family(directory, source, key, should_probe_strings_xml=False):
    """Move key + its plural family from any source file of this directory
    into strings_turbo.xml. Line-based, byte-identical transfer. Returns
    the number of moved string blocks."""
    probes = [source] + [s for s in SET_SOURCES if s != source]
    if should_probe_strings_xml:
        probes.append(STRINGS_XML)
    source_lines = None
    family = None
    for probe in probes:
        path = os.path.join(directory, probe)
        if not os.path.exists(path):
            continue
        lines = iter_string_lines(path)
        present = [k for k in re.findall(r'<string name="([^"]+)"', "".join(lines))]
        bare, _ = resolve_plural_cat(key)
        if key in present:
            if probe == TURBO_SOURCE:
                return 0
            family = collect_family_keys(present, bare)
            source_lines = lines
            source_path = path
            break
    if source_lines is None:
        return 0
    turbo_path, turbo_lines = load_turbo_lines(directory)
    moved = 0
    for family_key in family:
        index = find_string_line(source_lines, family_key)
        if index < 0:
            continue
        row = re.sub(r'^[ \t]+', '', source_lines[index]).rstrip("\n")
        if find_string_line(turbo_lines, family_key) < 0:
            if not insert_before_close(turbo_lines, row):
                continue
        del source_lines[index]
        moved += 1
    write_lines(source_path, source_lines)
    write_lines(turbo_path, turbo_lines)
    return moved


def cmd_migrate_turbo():
    entries = build_migration_key_lists()
    if not entries:
        print("migrate: nothing to migrate (already done)")
        return
    targets = [EN_DIR] + [f"values-{locale}" for locale in LOCALES]
    per_source = {}
    moved_total = 0
    for source, key in entries:
        for target in targets:
            directory = os.path.join(RES, target)
            moved_total += move_string_family(
                directory, source, key, should_probe_strings_xml=target != EN_DIR)
        per_source[source] = per_source.get(source, 0) + 1
    for source, count in sorted(per_source.items()):
        print(f"migrate: {source} -> {TURBO_SOURCE}: {count} keys")
    print(f"migrate: {moved_total} string blocks moved across {len(targets)} directories")


def remove_hygiene_keys(lines, keys):
    removed = 0
    for key in sorted(keys):
        index = find_string_line(lines, key)
        if index >= 0:
            del lines[index]
            removed += 1
    return removed


def transfer_cross_dup_strings(lines, directory, source, set_map):
    transferred = 0
    for key in list(dict.fromkeys(m.group(1) for line in lines for m in re.finditer(r'<string name="([^"]+)"', line))):
        canonical = resolve_canonical_source(key, set_map)
        if not canonical or canonical == STRINGS_XML or canonical == source:
            continue
        index = find_string_line(lines, key)
        if index < 0:
            continue
        row = re.sub(r'^[ \t]+', '', lines[index]).rstrip("\n")
        target_path = os.path.join(directory, canonical)
        if not os.path.exists(target_path):
            continue
        target_lines = iter_string_lines(target_path)
        if find_string_line(target_lines, key) < 0:
            if not insert_before_close(target_lines, row):
                continue
            write_lines(target_path, target_lines)
        del lines[index]
        transferred += 1
    return transferred


def split_multiline_strings(lines):
    split_count = 0
    for position, line in enumerate(lines):
        if line.count("<string") > 1:
            fixed = re.sub(r'(</string>)\s*(<string name=)', r'\1\n    \2', line)
            if fixed != line:
                lines[position:position + 1] = [part if part.endswith("\n") else part + "\n" for part in fixed.splitlines(keepends=True)]
                split_count += 1
    return split_count


def strip_bidi_marks(lines, source):
    stripped = 0
    for position, line in enumerate(lines):
        if source == STRINGS_XML or "<string" not in line or not any(mark in line for mark in BIDI_MARKS):
            continue
        lines[position] = "".join(ch for ch in line if ch not in BIDI_MARKS)
        stripped += 1
    return stripped


def cmd_clean():
    set_map = build_set_source_map()
    untranslatable_base = set()
    for source in SET_SOURCES:
        for key, (_, untranslatable) in parse_strings(os.path.join(RES, EN_DIR, source)).items():
            if untranslatable:
                untranslatable_base.add(key)
    removed_dead = transferred = split_count = stripped_marks = 0
    clean_targets = [("base", EN_DIR)] + [("locale", f"values-{locale}") for locale in LOCALES]
    for kind, locale in clean_targets:
        directory = os.path.join(RES, locale)
        for source in ALL_SOURCES:
            path = os.path.join(directory, source)
            if not os.path.exists(path):
                continue
            lines = iter_string_lines(path)
            file_dead = remove_hygiene_keys(
                lines, DEAD_KEYS | (untranslatable_base if kind == "locale" else set()))
            file_split = split_multiline_strings(lines)
            file_transferred = transfer_cross_dup_strings(lines, directory, source, set_map) \
                if source in ALL_SOURCES and kind == "locale" else 0
            file_stripped = strip_bidi_marks(lines, source)
            if file_dead or file_transferred or file_split or file_stripped:
                write_lines(path, lines)
            removed_dead += file_dead
            transferred += file_transferred
            split_count += file_split
            stripped_marks += file_stripped
    print(f"clean: dead={removed_dead} transferred={transferred} split={split_count} bidi={stripped_marks}")
    failures = list_cross_dup_failures()
    if failures:
        for locale, source, key in failures:
            print(f"CLEAN FAIL cross-dup remains: {locale}/{source} {key}")
        sys.exit(1)


def list_cross_dup_failures():
    failures = []
    for locale in LOCALES:
        seen = {}
        for source in ALL_SOURCES:
            for key in parse_strings(os.path.join(locale_dir(locale), source)):
                seen.setdefault(key, []).append(source)
        for key, homes in seen.items():
            if len(homes) > 1:
                failures.append((locale, "+".join(homes), key))
    return failures


def cmd_batches(locale, out_dir):
    source_map = build_set_source_map()
    en = {}
    for source in ALL_SOURCES:
        for key, (value, untranslatable) in parse_strings(os.path.join(RES, EN_DIR, source)).items():
            if not untranslatable:
                en[key] = value
    ru = {}
    for source in ALL_SOURCES:
        ru.update({k: v[0] for k, v in load_locale("ru-rRU", source).items()})
    existing = set()
    for source in ALL_SOURCES:
        existing.update(load_locale(locale, source))
    bare_map = {}
    for key in source_map:
        bare, _ = resolve_plural_cat(key)
        bare_map.setdefault(bare, source_map[key])
    missing = []
    for bare in sorted(bare_map):
        is_plural = any(f"{bare}_{cat}" in source_map for cat in CATS)
        if is_plural:
            for form in resolve_required_forms(locale):
                if f"{bare}_{form}" not in existing:
                    missing.append(f"{bare}_{form}")
        elif bare not in existing:
            missing.append(bare)
    if not missing:
        print(f"{locale}: no missing keys")
        return
    os.makedirs(out_dir, exist_ok=True)
    rows = []
    for full_key in missing:
        bare, _ = resolve_plural_cat(full_key)
        source = bare_map.get(bare, "-")
        en_ref = (en.get(full_key) or en.get(f"{bare}_other") or en.get(f"{bare}_one") or "-").replace("\n", "\\n")
        ru_ref = ru.get(full_key, "-").replace("\n", "\\n")
        rows.append(f"{full_key} | {source} | {en_ref} | {ru_ref}")
    size = 150
    for index in range(0, len(rows), size):
        path = os.path.join(out_dir, f"batch-{index // size + 1}.md")
        with open(path, "w", encoding="utf-8") as handle:
            handle.write("\n".join(rows[index:index + size]) + "\n")
    print(f"{locale}: {len(rows)} missing -> {(len(rows) + size - 1) // size} batches in {out_dir}")


def cmd_apply(locale, input_dir):
    translations = {}
    for name in sorted(os.listdir(input_dir)):
        if not name.startswith("out-") or not name.endswith((".md", ".txt")):
            continue
        with open(os.path.join(input_dir, name), encoding="utf-8") as handle:
            for line in handle:
                parts = [p.strip() for p in line.split("|")]
                if len(parts) != 2 or not parts[0] or parts[0] in ("key",):
                    continue
                translations[parts[0]] = parts[1]
    existing = set()
    for source in ALL_SOURCES:
        existing.update(load_locale(locale, source))
    set_only_map = build_set_source_map()
    per_file = {}
    for full_key, value in translations.items():
        if full_key in existing:
            continue
        target = resolve_canonical_source(full_key, set_only_map)
        if not target:
            print(f"SKIP unknown key: {full_key}")
            continue
        per_file.setdefault(target, {})[full_key] = value
    applied = 0
    for source, rows in per_file.items():
        directory = locale_dir(locale)
        os.makedirs(directory, exist_ok=True)
        path = os.path.join(directory, source)
        if not os.path.exists(path):
            write_lines(path, ['<?xml version="1.0" encoding="utf-8"?>\n', "<resources>\n", "</resources>\n"])
        lines = iter_string_lines(path)
        for key in sorted(rows):
            value = xml_escape(rows[key]).replace("'", "\\'").replace('"', '\\"')
            if not insert_before_close(lines, f'<string name="{key}">{value}</string>'):
                continue
            applied += 1
        write_lines(path, lines)
    print(f"{locale}: applied {applied} translations")


def collect_legacy_set_violations():
    violations = []
    for source in LEGACY_SOURCES:
        local_keys = set(parse_strings(os.path.join(RES, EN_DIR, source)))
        ref_keys = load_ref_string_keys(NAGRAMX_REF, f"{EN_DIR}/{source}")
        for key in sorted(local_keys - ref_keys):
            violations.append((source, key))
    return violations


def collect_strings_xml_foreign_keys():
    local_keys = set(parse_strings(os.path.join(RES, EN_DIR, STRINGS_XML)))
    ref_keys = load_ref_string_keys(NAGRAMX_REF, f"{EN_DIR}/{STRINGS_XML}") \
        | load_ref_string_keys(OFFICIAL_REF, f"{EN_DIR}/{STRINGS_XML}")
    return sorted(local_keys - ref_keys - DEAD_KEYS)


def cmd_check(all_locales, locale):
    set_map = build_set_source_map()
    failures = 0
    locales = LOCALES if all_locales else [locale]
    for loc in locales:
        problems = []
        merged = {}
        try:
            for source in SET_SOURCES:
                merged.update(load_locale(loc, source))
            strings_xml = load_locale(loc, STRINGS_XML)
        except Exception as error:
            failures += 1
            print(f"FAIL {loc}: xml-parse {error}")
            continue
        extra = sorted(k for k in merged if k not in set_map and not (resolve_plural_cat(k)[1] and check_family_alive(k, set_map)) and k not in DEAD_KEYS)
        dead_found = sorted(k for k in list(merged) + list(strings_xml) if k in DEAD_KEYS)
        missing = sorted(k for k in set_map if k not in merged and not check_family_alive(k, merged))
        if missing:
            problems.append(f"set-missing={len(missing)} {missing[:4]}")
        if extra:
            problems.append(f"set-extra={len(extra)} {extra[:4]}")
        if dead_found:
            problems.append(f"dead={dead_found[:4]}")
        turbo_bares = sorted({resolve_plural_cat(k)[0] for k in set_map if set_map[k] == TURBO_SOURCE})
        for bare in turbo_bares:
            is_plural = any(set_map.get(f"{bare}_{cat}") == TURBO_SOURCE for cat in CATS)
            if is_plural:
                for form in resolve_required_forms(loc):
                    if f"{bare}_{form}" not in merged:
                        problems.append(f"turbo-plural-missing {bare}_{form}")
        for key, (value, _) in merged.items():
            if len(re.findall(r'%(?!\d+\$)s', value)) > 1:
                problems.append(f"bare-positional {key}")
        for key in set(merged) & set(set_map):
            if placeholders(merged[key][0]) != placeholders(parse_strings(os.path.join(RES, EN_DIR, set_map[key]))[key][0]):
                problems.append(f"placeholder {key}")
        for key, (value, _) in merged.items():
            bare, cat = resolve_plural_cat(key)
            if not cat or key in set_map:
                continue
            base_source = set_map.get(bare) or set_map.get(f"{bare}_one") or set_map.get(f"{bare}_other")
            if not base_source:
                continue
            base_entries = parse_strings(os.path.join(RES, EN_DIR, base_source))
            base_ref = base_entries.get(bare) or base_entries.get(f"{bare}_one") or base_entries.get(f"{bare}_other")
            if base_ref and placeholders(value) != placeholders(base_ref[0]):
                problems.append(f"placeholder {key}")
        for source in SET_SOURCES:
            path = os.path.join(locale_dir(loc), source)
            if not os.path.exists(path):
                continue
            try:
                for index, line in enumerate(iter_string_lines(path)):
                    if line.count("<string") > 1:
                        problems.append(f"multi-line {source}:{index + 1}")
                    if "<string" in line and (any(mark in line for mark in BIDI_MARKS) or re.search(r'\\u20(?:0[EF]|2[0-9])', line)):
                        problems.append(f"bidi-mark {source}:{index + 1}")
                    if "CDATA" in line:
                        continue
                    value_match = re.search(r">([^<]*)</string>", line)
                    if value_match and re.search(r"(?<!\\)'", value_match.group(1)):
                        problems.append(f"unescaped-quote {source}:{index + 1}")
            except Exception as error:
                problems.append(f"xml-parse {source}: {error}")
        if problems:
            failures += 1
            print(f"FAIL {loc}: " + "; ".join(problems[:8]))
    cross_dups = [f for f in list_cross_dup_failures() if all_locales or f[0] == locale]
    if cross_dups:
        failures += len(cross_dups)
        for locale, source, key in cross_dups[:8]:
            print(f"FAIL cross-dup: {locale} {source} {key}")
    legacy_violations = collect_legacy_set_violations()
    if legacy_violations:
        failures += len(legacy_violations)
        for source, key in legacy_violations[:8]:
            print(f"FAIL legacy-frozen: {source} gained {key} (new fork keys belong in {TURBO_SOURCE})")
    foreign = collect_strings_xml_foreign_keys()
    if foreign:
        failures += len(foreign)
        print(f"FAIL strings-xml-foreign: {len(foreign)} keys not in upstream refs {foreign[:6]}")
    official_keys = load_ref_string_keys(OFFICIAL_REF, f"{EN_DIR}/{STRINGS_XML}")
    turbo_keys = set(parse_strings(os.path.join(RES, EN_DIR, TURBO_SOURCE)))
    clashes = sorted(turbo_keys & official_keys)
    if clashes:
        failures += len(clashes)
        print(f"FAIL turbo-official-clash: {clashes[:6]} (future DrKLO key-name collisions)")
    for locale in locales:
        misplaced = sorted(
            key for key in parse_strings(os.path.join(locale_dir(locale), STRINGS_XML))
            if resolve_canonical_source(key, set_map) == TURBO_SOURCE)
        if misplaced:
            failures += len(misplaced)
            print(f"FAIL {locale}: strings-xml-misplaced {misplaced[:4]} (turbo keys belong in {TURBO_SOURCE})")
    print("CHECK:", "FAIL" if failures else "OK", f"({failures} failures)")
    sys.exit(1 if failures else 0)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--batches", action="store_true")
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--clean", action="store_true")
    parser.add_argument("--migrate-turbo", action="store_true")
    parser.add_argument("--all", action="store_true")
    parser.add_argument("--locale")
    parser.add_argument("--input")
    parser.add_argument("--out", default="/tmp/tr")
    args = parser.parse_args()
    if args.migrate_turbo:
        cmd_migrate_turbo()
    elif args.clean:
        cmd_clean()
    elif args.batches:
        if not args.locale:
            sys.exit("--locale required")
        cmd_batches(args.locale, os.path.join(args.out, args.locale))
    elif args.apply:
        if not args.locale or not args.input:
            sys.exit("--locale and --input required")
        cmd_apply(args.locale, args.input)
    elif args.check:
        if not args.all and not args.locale:
            sys.exit("--all or --locale required")
        cmd_check(args.all, args.locale)
    else:
        sys.exit("mode required: --batches | --apply | --check | --clean | --migrate-turbo")
