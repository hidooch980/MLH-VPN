"""Small, dependency-free authentication primitives for the control plane.

These primitives are intentionally not wired to HTTP routes yet. Account lifecycle,
rate limiting, MFA, session revocation and audit events must be added before exposure.
"""
from __future__ import annotations

import base64
import hashlib
import hmac
import json
import secrets
import time
from dataclasses import dataclass
from typing import Any


class AuthenticationError(ValueError):
    """Raised for invalid credentials or access tokens."""


_SCRYPT_N = 1 << 14
_SCRYPT_R = 8
_SCRYPT_P = 1
_SCRYPT_DKLEN = 32
_SALT_BYTES = 16


def _b64url_encode(value: bytes) -> str:
    return base64.urlsafe_b64encode(value).rstrip(b"=").decode("ascii")


def _b64url_decode(value: str) -> bytes:
    if not isinstance(value, str) or not value or any(c not in "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_" for c in value):
        raise AuthenticationError("Malformed token encoding.")
    try:
        return base64.urlsafe_b64decode(value + "=" * (-len(value) % 4))
    except (ValueError, TypeError) as exc:
        raise AuthenticationError("Malformed token encoding.") from exc


def hash_password(password: str) -> str:
    """Hash a password with scrypt and a fresh random salt."""
    if not isinstance(password, str) or len(password) < 12 or len(password) > 1024:
        raise AuthenticationError("Password must contain 12 to 1024 characters.")
    salt = secrets.token_bytes(_SALT_BYTES)
    digest = hashlib.scrypt(
        password.encode("utf-8"), salt=salt, n=_SCRYPT_N, r=_SCRYPT_R,
        p=_SCRYPT_P, dklen=_SCRYPT_DKLEN, maxmem=64 * 1024 * 1024,
    )
    return f"scrypt\u0024{_SCRYPT_N}\u0024{_SCRYPT_R}\u0024{_SCRYPT_P}\u0024{_b64url_encode(salt)}\u0024{_b64url_encode(digest)}"


def verify_password(password: str, encoded_hash: str) -> bool:
    """Verify a stored scrypt hash; malformed encodings fail closed."""
    try:
        algorithm, n_raw, r_raw, p_raw, salt_raw, digest_raw = encoded_hash.split("$")
        if algorithm != "scrypt":
            return False
        n, r, p = int(n_raw), int(r_raw), int(p_raw)
        if (n, r, p) != (_SCRYPT_N, _SCRYPT_R, _SCRYPT_P):
            return False
        salt, expected = _b64url_decode(salt_raw), _b64url_decode(digest_raw)
        if len(salt) != _SALT_BYTES or len(expected) != _SCRYPT_DKLEN:
            return False
        candidate = hashlib.scrypt(
            password.encode("utf-8"), salt=salt, n=n, r=r, p=p,
            dklen=len(expected), maxmem=64 * 1024 * 1024,
        )
        return hmac.compare_digest(candidate, expected)
    except (AttributeError, TypeError, ValueError, UnicodeError):
        return False


@dataclass(frozen=True)
class AccessClaims:
    subject: str
    issued_at: int
    expires_at: int
    token_id: str


class AccessTokenCodec:
    """HMAC-SHA256 signed short-lived access tokens with strict claim checks."""

    def __init__(self, secret: str | bytes, *, issuer: str = "molla-vpn",
                 audience: str = "molla-vpn-api", lifetime_seconds: int = 900):
        key = secret.encode("utf-8") if isinstance(secret, str) else secret
        if not isinstance(key, bytes) or len(key) < 32:
            raise AuthenticationError("Signing secret must contain at least 32 bytes.")
        if not issuer or not audience or not 60 <= lifetime_seconds <= 3600:
            raise AuthenticationError("Invalid token codec configuration.")
        self._key, self.issuer, self.audience = key, issuer, audience
        self.lifetime_seconds = lifetime_seconds

    def issue(self, subject: str, *, now: int | None = None) -> str:
        if not isinstance(subject, str) or not subject or len(subject) > 128:
            raise AuthenticationError("A valid subject is required.")
        issued = int(time.time()) if now is None else int(now)
        header = {"alg": "HS256", "typ": "JWT"}
        payload = {"iss": self.issuer, "aud": self.audience, "sub": subject,
                   "iat": issued, "exp": issued + self.lifetime_seconds,
                   "jti": secrets.token_urlsafe(16), "typ": "access"}
        signing_input = (
            f"{_b64url_encode(json.dumps(header, separators=(',', ':')).encode())}."
            f"{_b64url_encode(json.dumps(payload, separators=(',', ':')).encode())}"
        )
        signature = hmac.new(self._key, signing_input.encode("ascii"), hashlib.sha256).digest()
        return f"{signing_input}.{_b64url_encode(signature)}"

    def verify(self, token: str, *, now: int | None = None) -> AccessClaims:
        if not isinstance(token, str) or len(token) > 4096:
            raise AuthenticationError("Invalid access token.")
        parts = token.split(".")
        if len(parts) != 3:
            raise AuthenticationError("Invalid access token.")
        signing_input = f"{parts[0]}.{parts[1]}"
        expected = hmac.new(self._key, signing_input.encode("ascii"), hashlib.sha256).digest()
        if not hmac.compare_digest(expected, _b64url_decode(parts[2])):
            raise AuthenticationError("Invalid access token signature.")
        try:
            header = json.loads(_b64url_decode(parts[0]))
            payload: Any = json.loads(_b64url_decode(parts[1]))
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise AuthenticationError("Invalid access token payload.") from exc
        if not isinstance(header, dict) or header != {"alg": "HS256", "typ": "JWT"}:
            raise AuthenticationError("Unsupported access token header.")
        if not isinstance(payload, dict):
            raise AuthenticationError("Invalid access token payload.")
        if (payload.get("iss") != self.issuer or payload.get("aud") != self.audience
                or payload.get("typ") != "access"):
            raise AuthenticationError("Access token claims do not match this service.")
        subject, issued, expires, token_id = (
            payload.get("sub"), payload.get("iat"), payload.get("exp"), payload.get("jti")
        )
        if (not isinstance(subject, str) or not subject or len(subject) > 128
                or type(issued) is not int or type(expires) is not int
                or not isinstance(token_id, str) or not token_id):
            raise AuthenticationError("Invalid access token claims.")
        current = int(time.time()) if now is None else int(now)
        if issued > current + 30 or expires <= current or expires <= issued:
            raise AuthenticationError("Access token is expired or not yet valid.")
        if expires - issued > self.lifetime_seconds:
            raise AuthenticationError("Access token lifetime exceeds policy.")
        return AccessClaims(subject, issued, expires, token_id)
