"""Gera configuração local. Não sobrescreve segredos já existentes."""
import base64
from pathlib import Path
import secrets

path = Path(__file__).resolve().parents[1] / ".env"
try:
    with path.open("x", encoding="utf-8") as output:
        output.write("DB_PASSWORD=" + secrets.token_urlsafe(32) + "\n")
        output.write("JWT_SECRET=" + base64.b64encode(secrets.token_bytes(32)).decode() + "\n")
        output.write("SPRING_PROFILES_ACTIVE=default\n")
    path.chmod(0o600)
    print(".env criado. Não envie este arquivo para o GitHub.")
except FileExistsError:
    print(".env já existe; valores preservados.")
