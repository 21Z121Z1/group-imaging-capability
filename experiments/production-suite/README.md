# Production Suite Validation Workspace

Temporary CI workspace for the nine-project Spring Boot + Vue production-suite hardening run.

This branch is disposable. The payload is reconstructed and checksum-verified by `reconstruct.sh`; GitHub Actions then performs Maven verification, Vue production builds/audits, security policy checks, and CodeQL analysis.

Current iteration: v2 backend/security fixes after the first CI run exposed shared compilation regressions.
