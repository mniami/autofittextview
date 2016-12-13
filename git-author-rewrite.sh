git filter-branch -f --env-filter '

OLD_EMAIL="damian.szczepanski@mobica.com"
CORRECT_NAME="mniami"
CORRECT_EMAIL="d.szczepek@gmail.com"

export GIT_COMMITTER_NAME="$CORRECT_NAME"
export GIT_COMMITTER_EMAIL="$CORRECT_EMAIL"
export GIT_AUTHOR_NAME="$CORRECT_NAME"
export GIT_AUTHOR_EMAIL="$CORRECT_EMAIL"
' --tag-name-filter cat -- --branches --tags