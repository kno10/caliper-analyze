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

While caliper-analyze has some heuristics to sort data (mostly by the number of
distinct values), this heuristic may fail. You can however easily resort the
data yourself on the command line:

    sh target/appassembler/bin/caliper-analyze | sort -t" " -k1,1 -k6n

will sort by columns 1 to 1 (alphabetically), then 6-end (numerical)

== Tips & Tricks ==

caliper-analyze can actually merge multiple results. Just load multiple files
on the command line, and it will treat them as one run (it is up to you to
ensure that this is fair, and you e.g. didn't use different computers or
changed the source code in the meantime!)

    sh target/appassembler/bin/caliper-analyze ~/.caliper/results/MyBenchmarkClass*.json*

The results *could* come from both micro and macrobenchmarks, but I havn't
checked if this makes sense.
There will likely be a mismatch in overhead for micro- and macro-benchmarks.

== Future plans ==

I'm currently working on *trend estimation* for parameters. This works to some
extend, but you probably need to perform a larger benchmark experiment, e.g.
with many different values of the size parameter. As of now, trend prediction
will only run when you provide at least 8 different values.

Here is an example result. Note that the trend estimation for the textbook
quicksort (using the first element as pivot) was not reasonably estimated. Also
for the insertion sort with binary search, the result looks a bit odd (but may
actually make sense, as the search cost is `O(n log n)`, only the insertion
cost is `O(n * n)`, and it calls `System.arraycopy` for them.

Note that this example output is *manually* formatted and organized.

    BidirectionalBubbleSort                             QUADRATIC: 1.6535924977590981
    BubbleSortTextbook                                  QUADRATIC: 1.4429338581257727
    BubbleSort                                          QUADRATIC: 0.9034948309289309
    InsertionSort                                       QUADRATIC: 0.53465632649279
    BinaryInsertionSort     NLOG2N:  9.465045383450686  QUADRATIC: 0.25766206198325714
    HeapSortTextbook        NLOG2N: 37.66446143948735
    QuickSortTextbook                        LINEAR: 299.64777536392694
    JavaSort                NLOG2N: 11.230093881229035
    QuickSortBo3            NLOG2N:  9.757780301009847
    QuickSortBo5            NLOG2N: 13.391194525882707
    DualPivotQuickSortBo5   NLOG2N: 13.155984199466108
