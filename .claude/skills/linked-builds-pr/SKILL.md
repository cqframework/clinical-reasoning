---
name: linked-builds-pr
description: Use when the user asks to create PRs, commit and push, or submit changes across linked builds (sibling repositories configured in local.properties). Handles cross-repo PR creation with Depends-On directives, transitive linked builds, and topological ordering.
---

# Creating PRs with Linked Builds

## Overview

This project supports **linked builds** — composite Gradle builds that substitute Maven Central
artifacts with local source checkouts of sibling repositories. When creating PRs that span
multiple repositories, each PR must declare its cross-repo dependencies using `Depends-On:`
directives so CI can build them together.

## Key Files

- **`local.properties`** (gitignored): Developer's active linked builds
- **`local.properties.example`**: Template showing available linked builds
- **`build-logic/src/main/kotlin/cqf/LinkedBuild.kt`**: Registry of all known linked builds
  (`LinkedBuildRegistry`) — contains repo slugs, property keys, build roots, and substitution mappings
- **`build-logic/src/main/kotlin/cqf.linked-builds.settings.gradle.kts`**: Settings plugin
  that reads `local.properties` and calls `includeBuild()` with dependency substitutions
- **`build-logic/src/main/kotlin/cqf.ci-conventions.gradle.kts`**: CI plugin with the
  `resolveLinkedBuilds` task that parses `Depends-On:` from PR descriptions

## Workflow

When a developer asks you to create PRs for their linked build changes, follow this procedure.
The goal is to create PRs from the **most upstream** repo down to **this** repo, with each
downstream PR declaring `Depends-On:` for its upstream dependencies.

### Step 1: Discover Active Linked Builds

Read `local.properties` in the project root. Each property key maps to a `LinkedBuild` entry
in the registry. For example:

```properties
cql.engine.path=../clinical_quality_language/Src/java
```

Look up each active property key in `LinkedBuildRegistry` (in `LinkedBuild.kt`) to get the
repo slug, build root, etc.

### Step 2: Check for Transitive Linked Builds

For each linked build path discovered in Step 1, check if **that** repository also has a
`local.properties` with its own linked builds. Repeat recursively to build a full dependency
graph. This is important — if repo A depends on repo B which depends on repo C, PRs must be
created starting from C (most upstream) and working down.

For example, if `clinical_quality_language/Src/java` has its own `local.properties` pointing
to another sibling repo, that repo needs a PR first.

Each linked repo may have its own `LinkedBuildRegistry` (or equivalent) in its `build-logic/`
directory — read it to understand its structure.

### Step 3: Create PRs from Most Upstream to Most Downstream

For each repository in topological order (most upstream first):

1. **Navigate to the repo directory** (the path from `local.properties`, minus the `buildRoot`
   suffix — e.g., `../clinical_quality_language` not `../clinical_quality_language/Src/java`)

2. **Check git status**: Identify the current branch, staged/unstaged changes, and unpushed commits.

3. **Ensure a feature branch exists**: If the repo is on `main`/`master`, the developer likely
   forgot to create a branch. Ask them what branch name to use. Never create PRs from main.

4. **Commit any uncommitted changes**: Ask the user for a commit message or suggest one based
   on the changes. Follow the repo's commit conventions (check recent git log).

5. **Push the branch**: Push with `-u` to set the upstream tracking branch.

6. **Create the PR**: Use `gh pr create`. The PR description must include:
   - A summary of changes
   - A test plan
   - `Depends-On:` lines for any of **its own** upstream linked builds (if transitive)
   - The `Generated with Claude Code` footer

7. **Record the branch name and PR URL** — downstream repos will reference these.

### Step 4: Create the PR in This Repo (Most Downstream)

After all upstream PRs are created:

1. **Check git status** in this repo.

2. **Commit changes** including any modifications to `build-logic/`, `settings.gradle.kts`,
   `build.gradle.kts`, CI workflows, and application code.

3. **Push the branch**.

4. **Create the PR** with `gh pr create`. The PR body **must** include `Depends-On:` lines
   for every upstream linked build that has a PR:

   ```
   ## Summary
   - Description of changes

   ## Linked Builds
   Depends-On: cqframework/clinical_quality_language#feature-branch-name

   ## Test plan
   - [ ] CI builds with linked dependencies
   - [ ] Tests pass

   🤖 Generated with [Claude Code](https://claude.com/claude-code)
   ```

### Step 5: Back-Annotate Upstream PRs

After the downstream PR is created, go back and update each upstream PR to indicate it blocks
a downstream build. This creates bidirectional awareness so reviewers of the upstream PR
know there's a downstream PR waiting on it.

For each upstream PR that was created (or already existed) in Steps 3-4:

1. **Read the current PR body**:
   ```bash
   gh pr view <number> --json body --jq '.body' --repo <org/repo>
   ```

2. **Append or update the `## Linked Builds` section** in the upstream PR body. If a
   `## Linked Builds` section already exists, add to it. Otherwise, insert one before the
   test plan or footer:

   ```bash
   gh pr edit <number> --repo <org/repo> --body "$(cat <<'EOF'
   <existing body content>

   ## Linked Builds
   Blocks: cqframework/clinical-reasoning#downstream-branch (PR #123)
   EOF
   )"
   ```

3. **Also leave a PR comment** linking to the downstream PR for visibility in the timeline:
   ```bash
   gh pr comment <number> --repo <org/repo> --body "Downstream PR: <downstream-pr-url>
   This PR is referenced via \`Depends-On:\` and will be built as a composite build in CI."
   ```

#### `Blocks:` Format

```
Blocks: <org/repo>#<branch> (PR #<number>)
```

- Mirror of `Depends-On:` but in the opposite direction
- Include the PR number in parentheses for easy navigation
- One `Blocks:` line per downstream repo
- The `Blocks:` directive is informational only — it is not parsed by CI (yet)

#### Updating Existing Entries

If the upstream PR body already has a `## Linked Builds` section:
- Check if the downstream repo is already listed
- If listed with a different branch or PR number, update it
- If not listed, append a new `Blocks:` line
- Never remove existing entries for other repos

#### Symmetry: Both Directions Use `## Linked Builds`

Both upstream and downstream PRs use the same `## Linked Builds` section header, but with
different directives:

- **Downstream PR** (this repo): `Depends-On: org/repo#branch` — parsed by CI
- **Upstream PR** (linked repo): `Blocks: org/repo#branch (PR #N)` — informational

This keeps linked build metadata in one consistent section regardless of direction.

### Important: `Depends-On:` Format

The `Depends-On:` directive is parsed by the `resolveLinkedBuilds` Gradle task with this regex:

```
Depends-On: <org/repo>#<branch>
```

- **Must be on its own line** in the PR body
- **Case-insensitive** (`depends-on:` also works)
- **Repo slug** must match a slug in `LinkedBuildRegistry`
- **Branch name** follows the `#` — no spaces around it
- **Multiple directives**: Use one `Depends-On:` per line for multiple linked repos

Examples:
```
Depends-On: cqframework/clinical_quality_language#feature-new-evaluator
Depends-On: cqframework/some-other-repo#matching-branch
```

## Edge Cases

### No Changes in a Linked Repo
If `local.properties` points to a linked repo but that repo has no uncommitted changes and is
on `main`/`master`, skip it — no PR is needed, and no `Depends-On:` should reference it.
The linked build was likely enabled for local testing only.

### Linked Repo Already Has a PR
If the linked repo's branch already has an open PR, don't create a duplicate. Instead, use
the existing branch name in the `Depends-On:` directive. Check with:
```bash
gh pr list --head <branch-name> --json number,url --repo <org/repo>
```

### Linked Repo Has Unpushed Commits but No New Changes
Push the existing commits and create the PR. The developer likely committed but forgot to push.

### Windows Paths in local.properties
Paths in `local.properties` may use forward or backslashes. Normalize to forward slashes
when resolving.

## Running the CI Task Locally

Developers can test the `Depends-On:` resolution locally:

```bash
# Test against an existing PR
./gradlew resolveLinkedBuilds -PprNumber=123

# Or with env var
PR_NUMBER=123 ./gradlew resolveLinkedBuilds
```

This will parse the PR description, clone linked repos, and write `local.properties` — exactly
what CI does.
