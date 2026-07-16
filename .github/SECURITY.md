# Security Policy

This file explains how to **report a security vulnerability** in LedgerAI. It is not the project's security
architecture — the controls, threat model, and internal security policy are owned by
[`docs/01-architecture/SECURITY.md`](../docs/01-architecture/SECURITY.md) and are not restated here.

## Reporting a vulnerability

Please report suspected vulnerabilities **privately**, and do **not** open a public issue for them.

- **Preferred:** use GitHub's private vulnerability reporting — the *Report a vulnerability* button under this
  repository's **Security** tab.
- **Alternatively:** contact the maintainer privately at `<security contact — to be configured>`.

Please do not disclose the issue publicly until it has been confirmed and a resolution has been coordinated.

## What to include

A useful report usually contains:

- what the vulnerability is and the impact you believe it has,
- the steps or conditions needed to reproduce or trigger it,
- the affected area of the repository, if known,
- any preconditions (configuration, access, or data) required.

You do not need to provide a fix. Please avoid including real user data or secrets in your report.

## What to expect

We will acknowledge the report, investigate, and coordinate a fix and a disclosure timeline with you. Please allow
reasonable time to address the issue before any public disclosure.

## Supported versions

LedgerAI has not yet had a release ([`docs/05-releases/`](../docs/05-releases/)); a supported-versions policy will be
published here once releases exist. Until then, the default branch is the only supported code.
