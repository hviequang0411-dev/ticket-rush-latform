#!/usr/bin/env python3
"""Make a JHipster React entity in the gateway read-only: no create/edit/delete routes, buttons or screens.

Counterpart of make-readonly.py for the UI, for entities whose REST API only allows reads.

    python scripts/make-readonly-react.py gateway/src/main/webapp/app/entities/order/order order

The second argument is the entity's route and file prefix (`/order`, `order-update.tsx`, ...). Idempotent.
"""
import re
import sys
from pathlib import Path


def sub(pattern: str, text: str) -> str:
    return re.sub(pattern, "", text, flags=re.S)


def main(folder: str, name: str) -> None:
    root = Path(folder)
    route = re.escape(name)

    for screen in (f"{name}-update.tsx", f"{name}-delete-dialog.tsx"):
        (root / screen).unlink(missing_ok=True)

    index = root / "index.tsx"
    text = index.read_text(encoding="utf-8")
    text = sub(r"import \w+(Update|DeleteDialog) from '\./[\w-]+';\n", text)
    text = sub(r'\n\s*<Route path="(new|edit|delete)" element=\{<\w+ />\} />', text)
    index.write_text(text, encoding="utf-8", newline="\n")

    listing = root / f"{name}.tsx"
    text = listing.read_text(encoding="utf-8")
    text = sub(rf'\n\s*<Link to="/{route}/new".*?</Link>', text)
    text = sub(rf"\n\s*<Button\n\s*as=\{{Link as any\}}\n\s*to=\{{`/{route}/\$\{{[^}}]+\}}/edit.*?</Button>", text)
    text = sub(rf"\n\s*<Button\n\s*onClick=\{{\(\) =>\s*\(globalThis\.location\.href = `/{route}/\$\{{[^}}]+\}}/delete.*?</Button>", text)
    listing.write_text(text, encoding="utf-8", newline="\n")

    detail = root / f"{name}-detail.tsx"
    text = detail.read_text(encoding="utf-8")
    text = sub(rf"\n\s*&nbsp;\n\s*<Button as=\{{Link as any\}} to=\{{`/{route}/\$\{{\w+\.id\}}/edit`\}}.*?</Button>", text)
    detail.write_text(text, encoding="utf-8", newline="\n")

    leftovers = [
        str(path)
        for path in (index, listing, detail)
        if re.search(rf"/{route}/(new|\$\{{[^}}]+\}}/(edit|delete))|\b\w*Update\b|DeleteDialog", path.read_text(encoding="utf-8"))
    ]
    if leftovers:
        sys.exit(f"Write actions still referenced in: {leftovers}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
