# Control-plane API scaffold

## Run locally

```bash
python -m venv .venv
. .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --host 127.0.0.1 --port 8000
```

Health: `GET /health`

Planned protocol catalog: `GET /api/v1/protocols`

This is a development scaffold only. It has no authentication, persistence, subscription enforcement, provisioning, or VPN data plane. Do not expose it publicly.
