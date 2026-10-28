"""Carrega .env e executa Spring Boot com o Maven Wrapper, em Windows/Linux/macOS."""
import os
from pathlib import Path
import subprocess

root = Path(__file__).resolve().parents[1]
env = os.environ.copy()
path = root / ".env"
if not path.exists():
    raise SystemExit("Execute primeiro: python scripts/setup_env.py")
for line in path.read_text(encoding="utf-8").splitlines():
    if line.strip() and not line.lstrip().startswith("#"):
        key, value = line.split("=", 1)
        env.setdefault(key, value)
command = ["cmd", "/c", "mvnw.cmd"] if os.name == "nt" else ["./mvnw"]
raise SystemExit(subprocess.call(command + ["spring-boot:run"], cwd=root, env=env))
