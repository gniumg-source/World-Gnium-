#!/usr/bin/env bash
set -euo pipefail

# Script to generate README.md from GitHub profile data (simple aggregator).
# Relies on GITHUB_TOKEN and GH_USERNAME environment variables.
# Writes README.md in repository root.

if [ -z "${GH_USERNAME:-}" ]; then
  echo "GH_USERNAME is not set"
  exit 1
fi

API_TOKEN="${GITHUB_TOKEN:-}"
USER="${GH_USERNAME}"

AUTH_HEADER=""
if [ -n "$API_TOKEN" ]; then
  AUTH_HEADER="-H \"Authorization: token ${API_TOKEN}\""
fi

# Helper to call GitHub API (REST)
gh_api() {
  local path="$1"
  if [ -n "$API_TOKEN" ]; then
    curl -s -H "Authorization: token ${API_TOKEN}" -H "Accept: application/vnd.github.v3+json" "https://api.github.com${path}"
  else
    curl -s -H "Accept: application/vnd.github.v3+json" "https://api.github.com${path}"
  fi
}

# Fetch profile
profile_json="$(gh_api "/users/${USER}")"

name="$(echo "$profile_json" | jq -r '.name // empty')"
bio="$(echo "$profile_json" | jq -r '.bio // empty')"
location="$(echo "$profile_json" | jq -r '.location // empty')"
html_url="$(echo "$profile_json" | jq -r '.html_url')"
blog="$(echo "$profile_json" | jq -r '.blog // empty')"
email="$(echo "$profile_json" | jq -r '.email // empty')"
company="$(echo "$profile_json" | jq -r '.company // empty')"

# Fetch repos (public, up to 100)
repos_json="$(gh_api "/users/${USER}/repos?per_page=100&sort=pushed")"

# Build top repos: by stargazers_count desc, limit 4
top_repos="$(echo "$repos_json" | jq -r '.[] | {name: .name, html_url: .html_url, stargazers: .stargazers_count, description: .description, language: .language} | @base64' | \
  while read -r repo; do echo "$repo"; done | \
  xargs -n1 -I{} bash -c 'echo {}' | \
  while read -r line; do echo "$line"; done | \
  (echo; ) )"

# Simpler: extract top 4 by stars:
top4="$(echo "$repos_json" | jq -r 'sort_by(-.stargazers_count)[0:4] | .[] | {name: .name, html_url: .html_url, stargazers: .stargazers_count, description: (.description // ""), language: (.language // "")} | @json')"

# Aggregate top languages (count languages field)
top_langs="$(echo "$repos_json" | jq -r '.[] .language' | grep -v null | sort | uniq -c | sort -rn | awk '{print $2}' | head -n 6 | paste -sd ',' - | sed 's/,/, /g')"

# Generate README content
readme_file="README.md"
cat > "${readme_file}" <<EOF
# Hola 👋 Soy ${name:-${USER}}

${bio:-"Developer. Perfil sincronizado con GitHub."}

[![GitHub followers](https://img.shields.io/github/followers/${USER}?label=Follow&style=social)](${html_url})
[![Top Langs](https://github-readme-stats.vercel.app/api/top-langs/?username=${USER}&layout=compact)](https://github.com/${USER})

---

## Acerca de mí
- Nombre: ${name:-N/A}
- Ubicación: ${location:-N/A}
- Empresa: ${company:-N/A}
- Blog/Web: ${blog:-N/A}
- Email: ${email:-N/A}

## Tecnologías principales
${top_langs:-"No disponible"}

## Proyectos destacados
$(if [ -n "$top4" ]; then
  echo "$top4" | jq -r '. | "- [\(.name)](\(.html_url)) — \(.description) (⭐ \(.stargazers))\n"'
else
  echo "- No hay repos públicos para mostrar."
fi)

---

_Este README se actualiza automáticamente desde los datos públicos de GitHub (script simple)._  
EOF

echo "README.md generado."
