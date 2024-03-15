# GitOps for Websites

```mermaid
sequenceDiagram
    Actor a as Website Author
    participant j as Job triggerd by Cron
    participant f as Forgejo Instance
    participant g as Your Git Repo for Website

    a ->> g: commit & push some new content
    j ->> f: check repo hash for new commits
    activate j
    f ->> g: get lates commit hash
    f -->> j: 
    j ->> f: download repo
    j ->> j: generate.sh
    j ->> j: cp /target/html to website
    deactivate j
```

# Runtime view

For the example configuration 

```yaml
issuer: "staging"
websiteconfigs:
- unique-name: "test.io" 
  fqdns: ["test.de", "test.org", "www.test.de", "www.test.org"]
  forgejo-host: "codeberg.org"
  repo-name: "repo"
  branchname: "main"
- unique-name: "example.io"
  fqdns: ["example.org", "www.example.com"]
  forgejo-host: "fineForgejoHost.net"
  repo-name: "repo"
  branchname: "main"
mon-cfg: 
  grafana-cloud-url: "url-for-your-prom-remote-write-endpoint"
  cluster-name: "website"
  cluster-stage: "test"
```

the website runtime looks like:

```mermaid
C4Context
    title c4k-webserver
    Boundary(k8s, "cluster") {
        Boundary(test_io, "namespace test-io"){
                System(website_ingt, "ingress f. test.de")
                Boundary(test_de_srv_t, "webserver") {
                    System(wst, "webserver")
                    SystemDb(file_htmlt, "static html")
                    Rel(wst, file_htmlt, "file ro")
                }
                Boundary(aab, "cron generate website") {
                    System(git_clonet, "git clone/pull & generate.sh & copy to static html")
                    SystemDb(file_gitt, "git repo for test.io")
                    Rel(git_clonet, file_gitt, "file rw")
                    Rel(file_gitt, file_htmlt, "file rw")
                }           
            }
            Rel(website_ingt, wst, "http")
            Boundary(example_io, "namespace example-io"){
                System(website_inge, "ingress f. example.org")
                Boundary(test_de_srv_e, "webserver") {
                    System(wse, "webserver")
                    SystemDb(file_htmle, "static html")
                    Rel(wse, file_htmle, "file ro")
                }
                Boundary(aeb, "cron generate website") {
                    System(git_clonee, "git clone/pull & generate.sh & copy to static html")
                    SystemDb(file_gite, "git repo for example.io")
                    Rel(git_clonee, file_gite, "file rw")
                    Rel(file_gite, file_htmle, "file rw")
                }           
            }
            Rel(website_inge, wse, "http")
        }
```
