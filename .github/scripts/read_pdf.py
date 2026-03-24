#!/usr/bin/env python3
"""Read a PDF file and return its content as plain text.

Usage:
        python .github/scripts/read_pdf.py /path/to/file.pdf
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path


def extract_pdf_text(pdf_path: Path) -> str:
    """Extract text from all pages in a PDF file."""
    if not pdf_path.exists():
        raise FileNotFoundError(f"PDF file not found: {pdf_path}")

    if not pdf_path.is_file():
        raise IsADirectoryError(f"Path is not a file: {pdf_path}")

    if pdf_path.suffix.lower() != ".pdf":
        raise ValueError(f"Expected a .pdf file, got: {pdf_path}")

    try:
        from pypdf import PdfReader  # type: ignore[import-not-found]
    except ImportError as exc:
        raise RuntimeError(
            "Missing dependency 'pypdf'. Install it with: pip install pypdf"
        ) from exc

    reader = PdfReader(str(pdf_path))
    pages_text: list[str] = []

    for page in reader.pages:
        # page.extract_text() may return None for scanned/image-only pages
        pages_text.append(page.extract_text() or "")

    return "\n\n".join(pages_text)


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Extract text from a PDF file and print it to stdout."
    )
    parser.add_argument("pdf_path", type=Path, help="Path to the PDF file")
    args = parser.parse_args()

    try:
        text = extract_pdf_text(args.pdf_path)
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        return 1

    print(text)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
