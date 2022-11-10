# convention 4 kubernetes: c4k-website
[![Clojars Project](https://img.shields.io/clojars/v/org.domaindrivenarchitecture/c4k-website.svg)](https://clojars.org/org.domaindrivenarchitecture/c4k-website) [![pipeline status](https://gitlab.com/domaindrivenarchitecture/c4k-website/badges/master/pipeline.svg)](https://gitlab.com/domaindrivenarchitecture/c4k-website/-/commits/main) 

[<img src="https://domaindrivenarchitecture.org/img/delta-chat.svg" width=20 alt="DeltaChat"> chat over e-mail](mailto:buero@meissa-gmbh.de?subject=community-chat) | [<img src="https://meissa-gmbh.de/img/community/Mastodon_Logotype.svg" width=20 alt="team@social.meissa-gmbh.de"> team@social.meissa-gmbh.de](https://social.meissa-gmbh.de/@team) | [Website & Blog](https://domaindrivenarchitecture.org)

## Purpose

Delivering cryogen generated static sites with the push of a few buttons.

c4k-website generates configuration files for nginx webservers, and
a cryogen static site generator build container. It automatically downloads a branch.zip from a specified
gitea API url. You need an authorization token to access the specified gitea user account.
The build container is based on clojure:lein. 

Following the example in valid-config.edn and valid-auth.edn you can add as many websites as you like (provided you have the DNS Routes set up).
One set of configmaps, deployment, services etc will be created for each element in the :websites and :auth list.  

c4k-website consists of the following parts:
* unique nginx webserver + service + ingress + certificate
* unique build container cron job and build secret
* unique volume claim for both

## Try out

Click on the image to try out live in your browser:

[![Try it out](doc/tryItOut.png "Try out yourself")](https://domaindrivenarchitecture.org/pages/dda-provision/c4k-website/)

Your input will stay in your browser. No server interaction is required.

## Setup

You need:

* DNS routes matching the fqdns in the lists
* cryogen as a static site generator
* a cryogen project ready to build
* and a gitea account which holds the buildable project
* a kubernetes cluster provisioned by [provs]

Before deploying, you need an authorization token, that can be generated in your gitea account.
Then you need a URL that points to: `https://your.gitea.host/api/v1/repos/<owner>/<repo>/archive/main.zip`.
Add this to your auth.edn config file and you should be ready to go.

Let c4k-website generate your .yaml file and `kubectl apply yourApp.yaml`. Done.

## License

Copyright © 2022 meissa GmbH
Licensed under the [Apache License, Version 2.0](LICENSE) (the "License")
Pls. find licenses of our subcomponents [here](doc/SUBCOMPONENT_LICENSE)

[provs]: https://gitlab.com/domaindrivenarchitecture/provs/-/commits/master
