#!/usr/bin/env bash
# PreToolUse hook: refuse commands that would delete the garage database.
# Exit 2 = block the tool call and show stderr to Claude. No jq needed.

input="$(cat)"

block() {
  echo "BLOCKED by block-db-delete hook: $1" >&2
  echo "The database must never be deleted. Ask the user to run this themselves if it is truly intended." >&2
  exit 2
}

has() { printf '%s' "$input" | grep -qiE "$1"; }

if has '"tool_name" *: *"Bash"'; then
  # SQL that drops or wipes data
  has '(drop|truncate)[[:space:]]+(database|schema|table)' && block "DROP/TRUNCATE statement"
  has '\b(dropdb|drop_database)\b' && block "dropdb"

  # Flyway clean wipes every object in the schema
  has 'flyway(Clean|[[:space:]]+clean)' && block "Flyway clean"

  # Docker: removing the container or its volume destroys the data
  has 'docker[[:space:]]+(container[[:space:]]+)?(rm|kill)[^"]*garage-db' && block "removing the garage-db container"
  has 'docker[[:space:]]+volume[[:space:]]+(rm|prune)' && block "docker volume removal"
  has 'docker[[:space:]]+system[[:space:]]+prune[^"]*--volumes' && block "docker system prune --volumes"
  has 'docker[[:space:]-]+compose[^"]*down[^"]*(-v|--volumes)' && block "docker compose down -v"

  # Postgres data directory
  has 'rm[[:space:]]+(-[a-z]+[[:space:]]+)*[^"]*(pgdata|postgresql/data|/var/lib/postgresql)' && block "deleting Postgres data files"
fi

if has '"tool_name" *: *"(Edit|Write)"'; then
  # Config changes that would let the app wipe or rebuild the schema on startup
  has 'ddl-auto[[:space:]]*=[[:space:]]*(create|create-drop)' && block "Hibernate ddl-auto=create/create-drop"
  has 'flyway\.clean-disabled[[:space:]]*=[[:space:]]*false' && block "enabling Flyway clean"
fi

exit 0
