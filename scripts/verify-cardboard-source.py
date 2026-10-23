#!/usr/bin/env python3
"""Check the vendored, pinned SDK and generated proto without fetching anything."""
from pathlib import Path
import hashlib
import json

task_root = Path(__file__).resolve().parent.parent
module = task_root / "apps/android/cardboard-sdk"
expected = json.loads((module / "source-sha256.json").read_text())
for name, digest in expected.items():
    path = module / name
    if not path.is_file() or hashlib.sha256(path.read_bytes()).hexdigest() != digest:
        raise SystemExit(f"Cardboard source integrity mismatch: {name}")
print(f"Verified {len(expected)} pinned Cardboard/generated source files")
