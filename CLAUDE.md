# CLAUDE.md

Thin router for this repository. It stays small and loads every session; everything else loads on
demand. When work starts from an issue, run `/start-issue <N>` first and load only what that work
needs. Do not pre-read the convention skills "just in case".

## Routing (run these even if the user never typed the command)

| The user wants to… | Run |
|---|---|
| decide what to work on next ("what's next", "triage the backlog") | `/triage-issues` |
| start an issue (a pasted issue URL or number, "work on this", "pick up #N") | `/start-issue <N>` |
| create an issue ("file an issue", "raise a ticket", "log this bug", "track this") | `/create-issue` |
| open a PR ("raise the PR", "open the PR") | `/raise-pr` |
| review a PR ("review this PR", a pasted PR URL) | `/pr-review <N>` |
| act on a review ("fix the review comments on PR N", "address the review") | `/fix-review-comments <N>` |
| repair ignored sibling-directory permissions (the "workspace has not been trusted" warning) | `/fix-workspace-trust` |

`/start-issue` maps the issue's labels to the stack conventions in **Label Routing** below and loads
only those. Stack conventions are skills named `<stack>-conventions`; they are never pre-read.

## Guardrails (hooks)

`.claude/hooks/` block secrets on write, block edits to files this repository does not own, block
commits to protected branches, block unsafe shell commands, and lint touched files. They fire
automatically. If one blocks you, fix the cause; do not work around it.

## Which files are this repository's

Most of what sits under `.claude/` is not this repository's own. It belongs to a shared set every
repository in this harness carries: it is changed there once and copied out, and the next install
overwrites whatever a repository has written over it in the meantime.
`.claude/harness/manifest.json` records which files are which, and reading it needs nothing outside
this repository:

| What the manifest records against a file | What that means here |
|---|---|
| `"match": "full"` or `"match": "generated"` | shared whole — never change it here |
| `"match": "common"` | shared above its final `## Project-specific` heading; that section is this repository's to fill |
| `"match": "present"`, a `"stack"` scope, or no entry at all | this repository's own — change it freely |

A change to a shared file belongs in the shared set, where it is made once and reaches every
repository; `.claude/conventions/sibling-repos.md` records where that repository sits. Made here
instead, it is reported as drift once it is pushed and then silently overwritten at the next install.
`.claude/hooks/protect-shared-files.cjs` refuses such a change made with the edit tools; it does not
watch writes made through the shell, so this rule binds whatever the change is written with. The
shared set itself is exempt, since that is where a shared file is meant to be changed.

## Where the rules live

| Topic | Source |
|---|---|
| Stack standards, gate commands, what review flags | the `<stack>-conventions` skill(s) named below |
| Comments in source files | `.claude/conventions/comment-conventions.md` |
| Git, commit and branch hygiene | `.claude/conventions/git-hygiene.md` |
| Issues, PR titles, boards, merge rules | `.claude/conventions/github-pr.md` |
| Review method and the review body shape | `.claude/conventions/review-conventions.md`, `review-template.md` |
| Sibling repositories (the only place they are named) | `.claude/conventions/sibling-repos.md` |
| Reviewer agents | `.claude/agents/` |

Each rule lives in one place; everything else points there. Do not repeat a rule here.

## Project-specific

This is the **only** register of this repo's values. Every convention file and skill that needs one
reads it here; none of them restates it, so a value changes in exactly one place. The two exceptions
are `.claude/conventions/sibling-repos.md`, which holds the sibling table because a sibling repository
is named there and nowhere else, and each `<stack>-conventions` skill, which holds the rules and the
**Scope** of its own stack.

- **Repo**: Anusha-Edstem/be-interview-prep — backend interview prep exercises, one Spring Boot
  feature per branch, each merged into `main` through its own pull request.
- **Stack**: Spring Boot REST service over a relational database, layered
  `controller -> service -> repository -> entity`.
- **Package Manager**: maven
- **Default Branch**: `main`
- **Protected Branches**: `main`
- **Promotion Chain**: NULL — a feature branch merges straight into the default branch.
- **Branch Name**: `feature/q<N>-<short-slug>`
- **Ticket Prefix**: NULL — exercises are numbered, not ticketed.
- **PR Title**: `Q<N> | <short description>` — spaces around the pipe.
- **PR Title Regex**: NULL
- **PR Template**: `.github/PULL_REQUEST_TEMPLATE.md`
- **Project Board**: NULL
- **Assign On Start**: yes
- **Milestones**: no
- **Gate Commands**:
  - `./mvnw spotless:apply`
  - `./mvnw test`
- **Version Bump**: NULL — this repo is not released, so it carries no version to bump.
- **Merge Method**: squash — `gh pr merge <N> --squash`. `deleteBranchOnMerge` is on, so never pass
  `--delete-branch`. Pull `main` before cutting the next branch.
- **Conventions Skills**: `java-conventions`
- **Reviewer Agents**: `senior-java-engineer`
- **Label Routing**: NULL — single area.
- **Repo-specific skills**: NULL
- **Siblings**: see `.claude/conventions/sibling-repos.md`
