# Release process for stable release

``` bash
git checkout main # for old projects replace main with master
```

Open package.json, find ":version" keyword and remove "-SNAPSHOT" from version number.

``` bash
lein release
git push --follow-tags
```

Open package.json again, increase version increment by one and add "-SNAPSHOT".

``` bash
git commit -am "version bump"
git push
```

Done.