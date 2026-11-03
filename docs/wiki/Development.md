# Repository structure & contribution workflow

## Repository structure

```
school-master/
├── README.md            overview, setup, TODO
├── API.md               contracts: Assets CDN API + Backend ↔ Client game API
├── Server/
│   ├── Backend/
│   │   ├── CardAPI/     card data model (Maven project)
│   │   ├── GameServer/  game server, plugin loader, events (Maven project)
│   │   └── data/        CardMeta.json, plugins/ (runtime data, working dir of the backend)
│   ├── Assets/          Assets CDN root: serve this folder with any web server
│   ├── Makefile         asset housekeeping (gather, build folder structure, clean/update)
│   ├── assets.py        helper used by the Makefile
│   ├── assets.defs.json source of truth for asset paths
│   └── DEFINITIONS.md   asset definitions and rules
└── Client/              stateless client (asset cache + game connection), shares no code with Server
```

## Branches

| Branch      | Purpose                                                                 |
|-------------|-------------------------------------------------------------------------|
| `master`    | Main branch. Always builds. **Protected: no direct pushes.**             |
| `pages`     | GitHub Pages site deployment. Not merged into `master`, and `master` is not merged into it. |
| `<type>/<name>` | One branch per feature or fix, e.g. `feature/network-layer`, `fix/event-order`. Deleted after merge. |

Branch name prefixes: `feature/`, `fix/`, `docs/`, `refactor/`, `chore/`.

## Workflow: branch → PR → review → merge

### 1. Create a branch from an up-to-date `master`

```sh
git switch master
git pull
git switch -c feature/my-feature
```

### 2. Commit your work

Keep commits small and focused. Write messages that say what changed and why:

```sh
git add <files>
git commit -m "Add WebSocket endpoint for client inputs"
```

Not acceptable as a commit message: `:)`, `fix`, `stuff`.

### 3. Push the branch

```sh
git push -u origin feature/my-feature
```

### 4. Open a pull request into `master`

On GitHub (or `gh pr create --base master`), describe:

- **What** the feature or fix does
- **Why** it's needed (link the TODO item or issue)
- **How it was tested**
- Anything the reviewer should look at closely

One feature per PR. If the PR changes `API.md`, say so explicitly: client and server both depend on it.

### 5. Review

- The maintainer reviews the PR and either **approves** or **requests changes**.
- To address feedback, push more commits to the same branch. The PR updates automatically.
- Don't merge your own PR.

### 6. Merge

- Only the maintainer merges, after approval.
- After the merge, the branch is deleted (on GitHub and locally):

```sh
git switch master
git pull
git branch -d feature/my-feature
```

## Rules

- Never push directly to `master`. Everything goes through a PR.
- Keep your branch up to date: if `master` moved on, run `git pull origin master` on your branch and resolve conflicts before review.
- Don't commit build output, IDE folders (`.settings/`, `.idea/`) or generated files; they're in `.gitignore`.
- Before opening a PR: the code compiles, and the README / `API.md` / `DEFINITIONS.md` are updated if behaviour changed.
