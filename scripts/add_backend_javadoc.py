#!/usr/bin/env python3
"""Ajoute une JavaDoc de classe aux fichiers Java qui n'en ont pas encore (dto, repository)."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src/main/java/fsegs/pfebackendemnagouuiaa"
PACKAGES = ["dto", "repository"]

def has_class_javadoc(content: str) -> bool:
    m = re.search(
        r"/\*\*.*?\*/\s*(?:@\w+[^\n]*\n\s*)*(?:public\s+)?(?:abstract\s+)?(?:class|interface|enum)\s",
        content,
        re.S,
    )
    return bool(m)

def describe_dto(name: str) -> str:
    base = name.replace(".java", "")
    if base.endswith("Request") or base.endswith("RequestDTO"):
        kind = "requête entrante (corps JSON)"
    elif base.endswith("Response") or base.endswith("ResponseDTO") or base.endswith("Dto"):
        kind = "réponse API (sérialisation JSON vers le client Angular)"
    else:
        kind = "objet de transfert (couche API)"
    return (
        f"/**\n"
        f" * DTO — {kind} pour le domaine déduit du nom « {base} ».\n"
        f" * <p>Transporte les données entre les contrôleurs REST et les services ; "
        f"ne contient pas de logique métier.</p>\n"
        f" * <p>Consommé par les contrôleurs et mappers du package "
        f"{{@code fsegs.pfebackendemnagouuiaa}}.</p>\n"
        f" */"
    )

def describe_repository(name: str) -> str:
    entity = name.replace("Repository.java", "")
    return (
        f"/**\n"
        f" * Accès JPA à l'entité {@link fsegs.pfebackendemnagouuiaa.entities.{entity}}.\n"
        f" * <p><b>Rôle :</b> persistance et requêtes dérivées Spring Data ; injecté par les "
        f"services métier du même domaine.</p>\n"
        f" */"
    )

def method_javadoc(signature: str) -> str | None:
    sig = signature.strip()
    if sig.startswith("@") or "class " in sig or "interface " in sig:
        return None
    m = re.match(r"(\w[\w<>,\s]*)\s+(\w+)\s*\(([^)]*)\)", sig)
    if not m:
        return None
    ret, name, params = m.group(1), m.group(2), m.group(3).strip()
    if name in ("class", "interface", "enum"):
        return None
    lines = [f"    /**", f"     * Requête Spring Data : {@code {name}}."]
    if params:
        lines.append(f"     * @param paramètres dérivés du nom de méthode")
    if ret != "void":
        lines.append(f"     * @return résultat typé {@code {ret.strip()}}")
    lines.append("     */")
    return "\n".join(lines)

def process_file(path: Path) -> bool:
    text = path.read_text(encoding="utf-8")
    if has_class_javadoc(text):
        return False
    pkg = path.parent.name
    name = path.name
    if pkg == "dto":
        doc = describe_dto(name)
    else:
        doc = describe_repository(name)

    # Insert before public class/interface
    new_text, n = re.subn(
        r"(\n)(@Repository\n)?(public (?:interface|class|enum) )",
        lambda m: f"\n{doc}\n{m.group(2) or ''}{m.group(3)}",
        text,
        count=1,
    )
    if n == 0:
        new_text, n = re.subn(
            r"(\n)(public (?:interface|class|enum) )",
            lambda m: f"\n{doc}\n{m.group(2)}",
            text,
            count=1,
        )
    if n == 0:
        return False

    # Repository: add method docs where missing
    if pkg == "repository":
        out_lines = []
        for line in new_text.splitlines():
            stripped = line.strip()
            if (
                stripped
                and not stripped.startswith("//")
                and not stripped.startswith("*")
                and not stripped.startswith("/**")
                and not stripped.startswith("@")
                and "(" in stripped
                and stripped.endswith(";")
                and not stripped.startswith("import")
            ):
                prev = out_lines[-1].strip() if out_lines else ""
                if not prev.endswith("*/"):
                    mj = method_javadoc(stripped)
                    if mj:
                        out_lines.append(mj)
            out_lines.append(line)
        new_text = "\n".join(out_lines) + ("\n" if new_text.endswith("\n") else "")

    path.write_text(new_text, encoding="utf-8")
    return True

def main() -> None:
    updated = 0
    for pkg in PACKAGES:
        for f in sorted((ROOT / pkg).glob("*.java")):
            if process_file(f):
                updated += 1
                print(f"OK {f.relative_to(ROOT)}")
    print(f"Updated {updated} files")

if __name__ == "__main__":
    main()
