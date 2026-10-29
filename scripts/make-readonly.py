#!/usr/bin/env python3
"""Strip write endpoints (POST/PUT/PATCH/DELETE) from a JHipster entity resource and the tests that call them.

Equivalent to generating the entity with the JDL `@readOnly` annotation, applied in place so hand-written code
in the same service is not overwritten by a regeneration. Idempotent.

    python scripts/make-readonly.py <Resource.java> <ResourceIT.java>
"""
import re
import sys

MEMBER_START = re.compile(r"^    (/\*\*|@)")
MEMBER_HEADER = re.compile(r"^    (public|private|protected|void|[A-Z][\w<>, ]*\s+\w+\()")
WRITE_MAPPING = re.compile(r"@(Post|Put|Patch|Delete)Mapping")
TEST_NAME = re.compile(r"void (\w+)\(\)")
IMPORT = re.compile(r"^import (?:static )?([\w.]+(?:\.\*)?);\n", re.M)


def remove_members(src: str, should_remove) -> str:
    """Drops each class member (javadoc + annotations + body) whose header matches."""
    lines = src.split("\n")
    out = []
    i = 0
    while i < len(lines):
        if not MEMBER_START.match(lines[i]):
            out.append(lines[i])
            i += 1
            continue
        j = i
        while j < len(lines) and not MEMBER_HEADER.match(lines[j]):
            j += 1
        depth, k, started = 0, j, False
        while k < len(lines):
            depth += lines[k].count("{") - lines[k].count("}")
            started = started or "{" in lines[k]
            if started and depth == 0:
                break
            k += 1
        if should_remove("\n".join(lines[i : j + 1])):
            i = k + 1
            while i < len(lines) and lines[i].strip() == "":
                i += 1
            continue
        out.extend(lines[i : k + 1])
        i = k + 1
    text = "\n".join(out).rstrip()
    return text[:-1].rstrip() + "\n}\n" if text.endswith("}") else text + "\n"


def drop_unused(src: str) -> str:
    """Removes imports, and the fields only write endpoints used, that nothing references any more."""
    for name in ("ENTITY_NAME", "applicationName"):
        if len(re.findall(r"\b" + name + r"\b", src)) == 1:
            src = re.sub(r"\n(    @Value\([^)]*\)\n)?    private (static final )?String " + name + r"[^;]*;\n", "\n", src)
    body = IMPORT.sub("", src)

    def keep(match: re.Match) -> str:
        name = match.group(1).rsplit(".", 1)[-1]
        return match.group(0) if name == "*" or re.search(r"\b" + re.escape(name) + r"\b", body) else ""

    src = IMPORT.sub(keep, src)
    return re.sub(r"\n{3,}", "\n\n", src)


def is_write_test(header: str) -> bool:
    match = TEST_NAME.search(header)
    return bool(match) and "@Test" in header and not match.group(1).startswith(("get", "init", "cleanup"))


def rewrite(path: str, transform) -> None:
    with open(path, encoding="utf-8") as f:
        src = f.read()
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(drop_unused(transform(src)))


def main(resource: str, test: str) -> None:
    rewrite(resource, lambda src: remove_members(src, lambda header: WRITE_MAPPING.search(header) is not None))
    rewrite(test, lambda src: remove_members(src, is_write_test))


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
