#!/usr/bin/env bash
#
# Configura os secrets e o environment de release do GitHub Actions.
# Rode uma única vez, na raiz do repositório, com `gh` autenticado.
#
# Uso:
#   ./scripts/setup-release-env.sh
#
# Variáveis opcionais:
#   REPO=Krisner94/Harpia-Arch   repositório alvo (padrão: o do diretório atual)
#   GPG_KEY_ID=3885F07BB8ADA7B3  chave usada para assinar
#   ENVIRONMENT=production       nome do environment protegido

set -euo pipefail

REPO="${REPO:-$(gh repo view --json nameWithOwner -q .nameWithOwner)}"
ENVIRONMENT="${ENVIRONMENT:-production}"

if ! gh auth status >/dev/null 2>&1; then
    echo "erro: gh não está autenticado. Rode 'gh auth login'." >&2
    exit 1
fi

echo "Repositório: $REPO"
echo

if [ -z "${GPG_KEY_ID:-}" ]; then
    GPG_KEY_ID=$(gpg --list-secret-keys --with-colons 2>/dev/null | awk -F: '/^sec/ {print $5; exit}')
fi

if [ -z "${GPG_KEY_ID:-}" ]; then
    echo "erro: nenhuma chave GPG secreta encontrada. Informe GPG_KEY_ID." >&2
    exit 1
fi

echo "Chave GPG: $GPG_KEY_ID"
gpg --list-secret-keys "$GPG_KEY_ID" | sed -n '1,4p'
echo

read -r -p "Central Portal username (token): " CENTRAL_USERNAME
read -r -s -p "Central Portal password (token): " CENTRAL_PASSWORD
echo
read -r -s -p "Passphrase da chave GPG (Enter se não houver): " GPG_PASSPHRASE
echo

if [ -z "$CENTRAL_USERNAME" ] || [ -z "$CENTRAL_PASSWORD" ]; then
    echo "erro: usuário e senha do Central Portal são obrigatórios." >&2
    exit 1
fi

echo
echo "Vai gravar 4 secrets em $REPO e configurar o environment '$ENVIRONMENT'"
echo "com aprovação obrigatória de $(gh api user --jq .login)."
read -r -p "Confirmar? [y/N] " CONFIRM
if [[ ! "$CONFIRM" =~ ^[yY]$ ]]; then
    echo "cancelado"
    exit 0
fi

gh secret set CENTRAL_USERNAME --repo "$REPO" --body "$CENTRAL_USERNAME"
gh secret set CENTRAL_PASSWORD --repo "$REPO" --body "$CENTRAL_PASSWORD"
gh secret set GPG_PASSPHRASE   --repo "$REPO" --body "$GPG_PASSPHRASE"
gpg --armor --export-secret-keys "$GPG_KEY_ID" | gh secret set GPG_PRIVATE_KEY --repo "$REPO"
gh secret set GPG_KEY_ID       --repo "$REPO" --body "$GPG_KEY_ID"

USER_ID=$(gh api user --jq .id)
printf '{"reviewers":[{"type":"User","id":%s}]}' "$USER_ID" \
    | gh api --method PUT "repos/$REPO/environments/$ENVIRONMENT" --input - >/dev/null

echo
echo "OK. Secrets gravados e environment '$ENVIRONMENT' com revisor obrigatório."
echo "Verifique em: https://github.com/$REPO/settings/environments"
echo
echo "Próximo passo: Actions > Release > Run workflow > version 1.0.0"
