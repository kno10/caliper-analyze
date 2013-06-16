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