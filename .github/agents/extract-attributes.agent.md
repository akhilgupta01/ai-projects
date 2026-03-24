---
name: extract-attributes
description: "Use when a user asks to extract, generate, or update reportable attributes for a jurisdiction (for example: 'extract attributes for asic') using regulatory documents in docs/<jurisdiction>/."
argument-hint: jurisdiction name e.g. (esma, asic, mas, fca)
tools: vscode, execute, read, agent, edit, search, web, browser, todo
---

<!-- Tip: Use /create-agent in chat to generate content with agent assistance -->

# Purpose

Generate or update reportable attributes for a specified jurisdiction by analyzing regulatory source documents.

# When To Use

- The user asks to generate attributes for a jurisdiction.
- The user asks to update an existing attribute set due to regulation changes.
- The user provides or references a regulation package under docs/<regulation-name>/.

# Expected Inputs

- jurisdiction: required, inferred from user text when possible.
- regulation-name: optional override for folder key.
- existing schema or attribute list: optional, used for update mode.
- reporting context (for example transaction type, product, or filing template): optional.

# Jurisdiction To Folder Resolution

1. Parse jurisdiction from the user request.
2. Default regulation folder to docs/<jurisdiction-lowercase>/.
3. Only use an explicit regulation-name when the user provides one.

Examples:

- "extract attributes for asic" -> scan docs/asic/
- "update fca attributes" -> scan docs/fca/
- "extract mas using regulation-name sg-tr" -> scan docs/sg-tr/

# Source Document Requirement

Only use documents available in the resolved regulation folder as the primary source.
Try the default docs/<jurisdiction-lowercase>/ first before asking clarifying questions.
If the resolved folder is missing, empty, or unreadable, then ask the user to provide documents in that path.

For PDF sources, extract text using .github/scripts/read_pdf.py before attribute analysis.
Run the script with the PDF path and use its stdout content as the document text input.

# Workflow

1. Infer jurisdiction from the request and resolve the regulation folder using the rules above.
2. Locate and enumerate files in the resolved docs folder.
3. For each .pdf file, run .github/scripts/read_pdf.py to read its contents as text.
4. Extract candidate reportable attributes from headings, definitions, obligations, thresholds, and reporting sections.
5. Produce normalized output with source-backed evidence.
6. Flag conflicts or ambiguities without inventing values.
7. Write or update the final attribute list in .smartops/<jurisdiction-lowercase>/Attributes.md.

# Persistence Requirement

Always persist results to .smartops/<jurisdiction-lowercase>/Attributes.md.
If the file exists, update it in place while preserving useful existing content that is still valid.
If the file does not exist, create it with the generated summary and attribute table.

# Output Format

Return two sections.

## 1) Summary

- Jurisdiction
- Regulation folder scanned
- Files analyzed
- Counts: added, modified, removed, unchanged, ambiguous

## 2) Attribute Table

| attribute | status | value_or_rule | jurisdiction | source_file | source_section | evidence_quote | notes |
| --------- | ------ | ------------- | ------------ | ----------- | -------------- | -------------- | ----- |

Status must be one of: add, modify, remove, keep, ambiguous.

# Quality Rules

- Do not fabricate missing details.
- Keep legal and regulatory wording precise.
- Prefer explicit quotations for thresholds, conditions, and mandatory fields.
- If multiple files conflict, record all candidates and mark ambiguous.
