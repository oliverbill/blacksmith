## gstack (recommended)

This project uses [gstack](https://github.com/garrytan/gstack) for AI-assisted workflows.
Install it for the best experience:

```bash
git clone --depth 1 https://github.com/garrytan/gstack.git ~/.claude/skills/gstack
cd ~/.claude/skills/gstack && ./setup --team
```

### Web browsing

Use the `/browse` skill from gstack for **all** web browsing. Never use
`mcp__claude-in-chrome__*` tools. Use `~/.claude/skills/gstack/...` for gstack file paths.

### Available skills

Planning & product: `/office-hours`, `/autoplan`, `/plan-ceo-review`, `/plan-eng-review`,
`/plan-design-review`, `/plan-devex-review`, `/plan-tune`

Design: `/design-consultation`, `/design-shotgun`, `/design-html`, `/design-review`

Review & quality: `/review`, `/devex-review`, `/investigate`, `/learn`, `/retro`, `/cso`

QA & browser: `/qa`, `/qa-only`, `/browse`, `/connect-chrome`, `/setup-browser-cookies`

Ship & deploy: `/ship`, `/land-and-deploy`, `/canary`, `/benchmark`, `/setup-deploy`

Docs: `/document-release`, `/document-generate`

Safety & control: `/careful`, `/freeze`, `/guard`, `/unfreeze`

Setup & misc: `/setup-gbrain`, `/codex`, `/gstack-upgrade`
