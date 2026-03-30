# CODEX Collaboration Guide

## Goal
- Build the project and improve it in a production-friendly way.
- Learn practical development, review, and operational habits during the process.

## Working Style
- Codex should not only suggest ideas, but also inspect code, implement changes, run tests, and explain why the change is practical.
- Each task should be handled with both delivery and learning in mind.
- If required information is missing and affects architecture, security, deployment, or external integrations, clarify it first.

## Standard Task Flow
1. Clarify the goal and expected behavior.
2. Inspect the affected files and explain the impact scope.
3. Share a short implementation plan.
4. Make the code change.
5. Run validation such as tests or targeted verification.
6. Summarize:
   - what changed
   - why it changed
   - how to review it in Git or IntelliJ
   - what risks or next steps remain

## Default Response Format For Project Tasks
- Goal: what we are trying to achieve
- Affected files: the main files involved
- Plan: the implementation or review steps
- Result: what changed
- Verification: what was tested or what could not be tested
- Practical note: the engineering lesson or real-world reason behind the change

## Review Mode
- When asked for review, prioritize:
  - bugs
  - behavioral regressions
  - operational risks
  - missing tests
- Keep summaries short until findings are covered.

## Learning Mode
- Explain changes as if pairing with a junior engineer on a real team.
- Prefer concrete tradeoffs over abstract theory.
- Point out practical patterns:
  - why a test is valuable
  - why a design is easier to maintain
  - what would matter in CI/CD or production

## Git And Change Review
- Prefer feature-branch workflow instead of direct `main` edits.
- Review every task with `git status` and `git diff`.
- Generated files should stay out of version control unless intentionally committed.
- After each meaningful change, check:
  - code diff
  - test result
  - docs/config impact

## When Codex Should Pause And Ask First
- Security-sensitive changes
- Public/internal endpoint exposure decisions
- Infra or deployment behavior changes
- Schema migrations with non-obvious production impact
- LLM provider strategy changes

## This Project's Current Priority Order
1. Test coverage for core behavior
2. Internal endpoint safety and operating policy
3. LLM integration strategy cleanup
4. Message and encoding cleanup
5. Deployment/documentation alignment
