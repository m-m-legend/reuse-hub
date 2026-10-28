"""Demonstra a API real usando só a biblioteca padrão do Python 3.10+."""
import argparse
import json
import secrets
import time
import urllib.error
import urllib.request
import uuid

parser = argparse.ArgumentParser(description="Cria duas contas fictícias, oferta e reservas reais na API.")
parser.add_argument("--base-url", default="http://localhost:8080")
args = parser.parse_args()
base = args.base_url.rstrip("/")


def call(method, path, data=None, token=None, key=None, expected=200):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    if key:
        headers["Idempotency-Key"] = key
    request = urllib.request.Request(base + path, data=json.dumps(data).encode() if data is not None else None,
                                     headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            status, payload = response.status, response.read()
    except urllib.error.HTTPError as error:
        status, payload = error.code, error.read()
    result = json.loads(payload) if payload else {}
    if status != expected:
        raise SystemExit(f"Falha: {method} {path}: esperado {expected}, recebido {status}: {result}")
    return result


def login(name):
    email = f"demo-{uuid.uuid4().hex[:16]}@example.test"
    password = secrets.token_urlsafe(24)
    call("POST", "/api/v1/auth/register", {"name": name, "email": email, "password": password}, expected=201)
    return call("POST", "/api/v1/auth/login", {"email": email, "password": password})["accessToken"]


for attempt in range(60):
    try:
        if call("GET", "/actuator/health")["status"] == "UP":
            break
    except (urllib.error.URLError, TimeoutError, SystemExit):
        if attempt == 59:
            raise SystemExit("API não ficou disponível em 120 segundos; verifique os logs.")
        time.sleep(2)

owner, requester = login("Laboratório doador"), login("Projeto educacional")
offer = call("POST", "/api/v1/offers", {
    "title": "Kit ESP32 + sensores", "description": "Kit funcional para aulas de robótica. Retirada a combinar.",
    "category": "ELECTRONICS", "condition": "GOOD", "city": "Rio de Janeiro", "quantity": 2,
}, token=owner, expected=201)
payload = {"offerId": offer["id"], "quantity": 1}
key = str(uuid.uuid4())
reservation = call("POST", "/api/v1/reservations", payload, requester, key, 201)
replay = call("POST", "/api/v1/reservations", payload, requester, key)
assert replay["id"] == reservation["id"]
call("POST", f"/api/v1/reservations/{reservation['id']}/collect", token=requester, expected=403)
call("POST", f"/api/v1/reservations/{reservation['id']}/collect", token=owner)
second = call("POST", "/api/v1/reservations", payload, requester, str(uuid.uuid4()), 201)
call("POST", f"/api/v1/reservations/{second['id']}/cancel", token=requester)
call("POST", f"/api/v1/reservations/{second['id']}/cancel", token=requester)
stock = call("GET", f"/api/v1/offers/{offer['id']}")["availableQuantity"]
assert stock == 1
history = call("GET", f"/api/v1/reservations/{reservation['id']}/events", token=owner)
assert len(history) == 2
print("Demonstração concluída: cadastro, login, oferta, reserva, replay, permissão, retirada e cancelamento.")
print("Oferta:", offer["id"])
print("Reserva retirada:", reservation["id"])
print("Estoque final: 1 disponível, 1 retirado. Contas e oferta fictícias permanecem no banco.")
