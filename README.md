= Project description =

This projects aims at providing a simple way of analzying
[Caliper Microbenchmarking](https://code.google.com/p/caliper/) results.

Caliper includes a web interface hosted on Google AppEngine, however it
currently is quite slow and unreliable: many result uploads fail. Plus,
not all microbenchmarking results are interesting to be shared and put online.
Some may just be temporary and incomplete results and of interest to a
single person for a few hours.

This project will read the local result cache, and analyze the data it
find there.

== Building and running ==

To build, use

    mvn compile package install appassembler:assemble

To run, use

    sh target/appassembler/bin/caliper-analyze

The current (early) version does not sort. The simplest is to sort yourself:

    sh target/appassembler/bin/caliper-analyze | sort -d" " -n -k1,1 -k6n

will sort by columns 1 to 1 (alphabetically), then 6-end (numerical)
