# AGENTS.md — working lanes for Ascend

This repo is worked by one human and two AI agents in parallel. To avoid
collisions, each actor stays in its own lane. Read this before pushing anything.

## 1. Lanes

- **Matt (human)** — owns decisions, **all merges**, accounts and secrets, and
  device testing. Nothing merges to `main` except by Matt.
- **Grok** — watches GitHub and is the **sole writer of the Mission Control
  sheet**. Writes plan files on `grok/*` branches. Never pushes to `claude/*`,
  and never merges.
- **Claude Code** — does implementation on `claude/*` branches. Never writes the
  Mission Control sheet, never touches `grok/*` branches, and never merges.

## 2. Claiming a branch

When you create a branch, **open a draft PR for it immediately** and add your
agent label:

- `agent:claude` for Claude Code's branches
- `agent:grok` for Grok's branches

**An open PR carrying the other agent's label means hands off** — do not push to
that branch, rebase it, or build on it.

## 3. PR titles

Start every PR title with the Mission Control item ID in brackets so the tracker
can match it, e.g. `[P6] Pathways awakening evaluator` or `[OPS] Add AGENTS.md`.

## 4. Stop rule

If you find commits on your branch that you did not make — or someone else has
pushed to it — **stop and report to Matt**. Do not rebase over or discard
another actor's work.

## 5. Plan files

Plan files live in `team/` and land through PRs, so every agent reads the same
committed version. Grok authors them on `grok/*` branches; implementation agents
read the merged plan rather than a local copy.
