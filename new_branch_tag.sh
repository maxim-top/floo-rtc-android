ver=$1
git checkout master
git stash
git checkout -b $ver
git push --set-upstream origin $ver
git tag -a $ver -m '$ver'
git checkout -b 'v'$ver
git push --set-upstream origin 'v'$ver
git tag -a 'v'$ver -m 'v'$ver
git push --tags
git checkout master
git stash pop
