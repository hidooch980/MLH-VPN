import pytest

from app.auth import AccessTokenCodec, AuthenticationError, hash_password, verify_password


def test_password_hash_is_salted_and_verifies():
    first = hash_password("correct horse battery staple")
    second = hash_password("correct horse battery staple")
    assert first != second
    assert verify_password("correct horse battery staple", first)
    assert not verify_password("wrong password", first)


@pytest.mark.parametrize("password", ["short", "", None])
def test_password_policy_rejects_weak_or_invalid_input(password):
    with pytest.raises(AuthenticationError):
        hash_password(password)


def test_malformed_password_hash_fails_closed():
    assert not verify_password("some password", "not-a-hash")


def test_access_token_round_trip_and_claims():
    codec = AccessTokenCodec(b"x" * 32, lifetime_seconds=300)
    claims = codec.verify(codec.issue("user-123", now=1000), now=1100)
    assert (claims.subject, claims.issued_at, claims.expires_at) == ("user-123", 1000, 1300)
    assert claims.token_id


def test_access_token_rejects_expiry_and_future_issuance():
    codec = AccessTokenCodec(b"x" * 32, lifetime_seconds=300)
    with pytest.raises(AuthenticationError):
        codec.verify(codec.issue("user-123", now=1000), now=1300)
    with pytest.raises(AuthenticationError):
        codec.verify(codec.issue("user-123", now=2000), now=1000)


def test_access_token_rejects_tampering_and_wrong_secret():
    token = AccessTokenCodec(b"x" * 32).issue("user-123", now=1000)
    with pytest.raises(AuthenticationError):
        AccessTokenCodec(b"y" * 32).verify(token, now=1001)
    parts = token.split(".")
    parts[1] = parts[1][:-1] + ("A" if parts[1][-1] != "A" else "B")
    with pytest.raises(AuthenticationError):
        AccessTokenCodec(b"x" * 32).verify(".".join(parts), now=1001)


def test_token_rejects_invalid_signing_key_and_lifetime():
    with pytest.raises(AuthenticationError):
        AccessTokenCodec(b"too short")
    with pytest.raises(AuthenticationError):
        AccessTokenCodec(b"x" * 32, lifetime_seconds=10)


@pytest.mark.parametrize("token", ["é.e.e", "a.سلام.a", "a.a.é", "a.a.a", ".."])
def test_malformed_tokens_raise_authentication_error(token):
    with pytest.raises(AuthenticationError):
        AccessTokenCodec(b"x" * 32).verify(token)
