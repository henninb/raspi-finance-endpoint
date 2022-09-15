#!/bin/sh

git config --local user.email henninb@msn.com
git config --local user.name 'Brian Henning'
git branch --set-upstream-to=origin/main main
git config --local filter.secrets.clean "sed 's/_PASSWORD=.*/_PASSWORD=********/'"

exit 0
