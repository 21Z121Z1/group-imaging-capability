# Production security baseline

- Production profile: MySQL, Flyway migrations, Hibernate `ddl-auto=validate`; startup fails when required DB/CORS environment variables are absent.
- Demo credentials/data are restricted to dev/test/ci profiles and are never seeded in prod.
- Authentication: opaque 256-bit SecureRandom bearer token; only SHA-256 token digests are persisted. Passwords use BCrypt cost 12. Registration requires 12+ characters with upper/lowercase letters and digits.
- Abuse resistance: login/register fixed-window limiter with bounded in-memory key storage. A production reverse proxy/WAF must provide distributed rate limiting.
- HTTP: stateless security context, strict exact-origin CORS, CSP/frame/referrer/HSTS headers, generic 401/403/5xx responses, request header/body size limits. TLS termination is expected at the ingress/reverse proxy.
- Data integrity: optimistic locking on entities, DB foreign keys/unique constraints, transaction boundaries for stock/book-copy changes, pessimistic lab-resource lock before overlap checks.
- Operations: graceful shutdown, HikariCP production pool settings, health probes and Prometheus metrics.
- Frontend bearer token is kept in memory only (no localStorage/sessionStorage persistence); templates do not render raw HTML.

This baseline materially reduces common attack surface but is not a claim of absolute security. Real production approval still requires environment-specific penetration testing, dependency/SBOM scanning, secret scanning, TLS/WAF configuration, backup/restore testing, monitoring and incident response.
