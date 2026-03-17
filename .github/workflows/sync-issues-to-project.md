<!--
   Copyright 2021-Present The Serverless Workflow Specification Authors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->

# Sync Issues to Target GitHub Project

Workflow file: `sync-issues-to-project.yml`

Automatically adds issues to a GitHub Project in another organization when they
are opened, sets configurable initial field values, updates the Status when they
are closed, and optionally imports pre-existing repo issues on demand.

---

## How it works

| Event | Action |
|---|---|
| Issue opened | Issue is added to the target project; initial field values are applied (skipped if `PSYNC_ENABLED=false` or `off`) |
| Issue closed | Project item Status is updated to the configured close status (skipped if `PSYNC_ENABLED=false` or `off`) |
| `workflow_dispatch` | If `PSYNC_IMPORT_EXISTING=true`, imports all open repo issues not yet in the project with initial field values applied |

The project item is natively linked to the source issue — no custom fields are
needed. Clicking the item in the project board opens the original issue.

---

## Setup

### 1. Create a Personal Access Token (PAT)

The PAT must belong to a user with access to the target org's project.

Required scopes:
- `project` — read/write access to GitHub Projects v2
- `read:org` — required to resolve the org's project by number
- `repo` (private repos) or `public_repo` (public repos) — required in either
  of these cases:
  - `PSYNC_INITIAL_VALUES` includes `Assignees=...` (REST API call to add
    assignees to the source repo issue)
  - `PSYNC_IMPORT_EXISTING=true` is used (REST API call to list all repo
    issues); for private repos this requires `repo`, for public repos
    `public_repo` is sufficient

> Classic PATs only. Fine-grained PATs do not yet support Projects v2 mutations.

### 2. Add the secret

Go to **Repo → Settings → Secrets and variables → Actions → Secrets**:

| Name | Value |
|---|---|
| `PSYNC_PAT` | The PAT created above |

### 3. Add the variables

Go to **Repo → Settings → Secrets and variables → Actions → Variables**:

| Name | Required | Default | Description | Example |
|---|---|---|---|---|
| `PSYNC_TARGET` | yes | — | Target project in `org:project_number` format | `my-org:1` |
| `PSYNC_INITIAL_VALUES` | no | — | Comma-separated `field=value` pairs applied to new project items | `Status=Backlog, Area=Tooling, Assignees=user1` |
| `PSYNC_CLOSE_STATUS` | no | `Done` | Status option name set on the project item when the issue is closed | `Done` |
| `PSYNC_ENABLED` | no | `true` | Set to `false` or `off` to pause syncing without removing the workflow | `false` |
| `PSYNC_IMPORT_EXISTING` | no | `false` | Set to `true` and trigger manually to bulk-import all open issues not yet in the project | `true` |
| `PSYNC_AUTHORS_FILTER` | no | — | Comma-separated list of GitHub usernames; only issues opened by these users are synced. Empty means all authors are included | `user1, user2` |

The project number is visible in the project URL:
`https://github.com/orgs/<org>/projects/<number>`

**`PSYNC_ENABLED`** — set to `false` or `off` to pause syncing without removing
the workflow. `workflow_dispatch` runs (e.g. for importing) are not affected.

**`PSYNC_AUTHORS_FILTER`** — comma-separated list of GitHub usernames. When set, only
issues created by one of the listed authors are synced to the target project. If
unset or empty, all authors are included.

**`PSYNC_IMPORT_EXISTING`** — set to `true` and trigger the workflow manually
via **Actions → Run workflow** to import all open repo issues not already in the
project. The same `PSYNC_INITIAL_VALUES` rules apply. Issues already in the
project are skipped. The run prints a summary: `Imported: N, Failed: N`.

#### `PSYNC_INITIAL_VALUES` format

Comma-separated `field=value` pairs. Field names must match the project field
names exactly (case-sensitive). Example:

```
Status=Backlog, Area=Tooling, Assignees=user1
```

Supported field types:

| Field type | Behaviour |
|---|---|
| Single-select | Matches by option name |
| Text | Sets the text value directly |
| `Assignees` | Adds assignees to the source issue via the REST API; space-separate multiple users: `Assignees=user1 user2` |

> Number and date fields are not currently supported.

### 4. Ensure the target project has a Status field

The workflow looks for a **single-select field named exactly `Status`**. The
option names used by `PSYNC_INITIAL_VALUES` and `PSYNC_CLOSE_STATUS` must
exist in the project (case-sensitive).

Default options required unless overridden:

| Option | Used when |
|---|---|
| `Done` | Issue closed (default close Status) |

---

## Limitations

- **Sub-issues are not synced** — GitHub does not emit webhook events for
  sub-issues; only top-level issues trigger the `issues` event.
- **Status field name is hardcoded** — the field must be named `Status`.
- **Number and date fields** in `PSYNC_INITIAL_VALUES` are not supported;
  only single-select and text fields.

---

## Troubleshooting

### `gh: To use GitHub CLI in a GitHub Actions workflow, set the GH_TOKEN environment variable`

`PSYNC_PAT` is not set or is empty. Verify it exists under **Repo → Settings → Secrets and variables → Actions → Secrets**.

### `Error: Process completed with exit code 1` on the GraphQL steps

Run the query manually to inspect the response:

```bash
gh api graphql -f query='
  query($org: String!, $number: Int!) {
    organization(login: $org) {
      projectV2(number: $number) {
        id
      }
    }
  }' \
  -f org="<target-org>" \
  -F number=<project-number>
```

Common causes:
- The PAT does not have access to the target org's project
- The project number is wrong
- The org name in `PSYNC_TARGET` has a typo

### Item not found on close (`item_id` is empty)

If the issue is not already in the project when it is closed (e.g., it was opened before the workflow was installed, or the `opened` sync failed), the workflow automatically adds it to the project and then sets the close Status.

If the close path still fails, likely causes are:
- The PAT lacks `project` write access to the target org's project
- The project ID lookup failed (check `PSYNC_TARGET` format and PAT scopes)

### Status not updated on close

Verify the target project has:
- A single-select field named exactly `Status`
- An option matching the value of `PSYNC_CLOSE_STATUS` (default: `Done`)

Field and option names are case-sensitive.

### Field not found warning in `Set initial field values`

The step prints `Warning: field '<name>' not found, skipping.` when a key in
`PSYNC_INITIAL_VALUES` does not match any field in the project. Check for
typos or extra spaces in the variable value.
