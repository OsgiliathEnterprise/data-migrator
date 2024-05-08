# Contribution welcomed!

Thank you so much for your interest in this project, I hope it's also useful for you and that you'll enjoy the
experience.
Still nobody is perfect and we enjoy constructive feedbac and even more individual contribution to make it AWESOME!

## Issue management

0. Create an issue with your idea and tell if you're eager to implement or want the maintainers to implement.
1. Wait for the 'go to implement' or "I'll do it I have time" or 3 days.

## Setup the environment

2. Fork this repository.
3. Create your branch and contribute.
4. Execute integration tests locally.

## Run locally

5. Install the main repo locally: `./mvnw clean install`.
6. `cd sample-mono && ./mvnw clean verify -Pentities-from-changelog`.

## Resync and pull request

7. Configure upstream: `git remote add upstream git@github.com:OsgiliathEnterprise/data-migrator.git` (or join the
   jetbrain space).
8. Pull the upstream: `git pull upstream main`.
9. Push your branch to your fork.
10. Create a pull request to upstream main if CI pass.
11. Wait for review by core team, do not hesitate to ping if you wait too long (We're doing our best and promise to
    review quickly, but never know).
