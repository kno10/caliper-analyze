**Important notice: this project is abandoned. I'm using [JMH](http://openjdk.java.net/projects/code-tools/jmh/) instead of caliper now**

# Project description #

This projects aims at providing a simple way of analzying
[Caliper Microbenchmarking](https://code.google.com/p/caliper/) results.

Caliper includes a web interface hosted on Google App Engine, however it
currently is quite slow and unreliable: many result uploads fail. Plus,
not all microbenchmarking results are interesting to be shared and put online.
Some may just be temporary and incomplete results and of interest to a
single person for a few hours.

This project will read the local result cache, and analyze the data it
find there. By default, it analyzes the latest result. We tried to use a robust parser, so it should also be able to analyze _incomplete_ results!

## Example output ##
(Indentation manually improved)
```
Loading latest results file: ...KNNHeapBenchmark.2013-06-20T02:40:37Z.json
1000  ARRAYI runtime[ns]: mean:  36955,25 +-   185,25  (0,50%) min:  36764,51 max:  37359,44 weight: 22647
1000  HEAP24 runtime[ns]: mean: 155544,12 +-  3856,43  (2,48%) min: 149391,42 max: 161661,37 weight: 5762
1000  HEAP2  runtime[ns]: mean: 170641,35 +- 24986,98 (14,64%) min: 147397,26 max: 207636,29 weight: 5709
1000  HEAP3  runtime[ns]: mean: 192135,08 +-  3033,57  (1,58%) min: 188965,79 max: 198495,85 weight: 4168
1000  HEAP3L runtime[ns]: mean: 195420,54 +-  2219,51  (1,14%) min: 193646,76 max: 201247,87 weight: 4435
1000  HEAP4L runtime[ns]: mean: 206763,24 +-  1644,87  (0,80%) min: 205241,37 max: 210535,85 weight: 4642
1000  HEAP5L runtime[ns]: mean: 213275,80 +-   432,26  (0,20%) min: 212461,92 max: 213899,25 weight: 4048
1000  HEAP4  runtime[ns]: mean: 214414,13 +-  1255,37  (0,59%) min: 213069,28 max: 217054,08 weight: 3821
1000  HEAP5  runtime[ns]: mean: 275159,03 +- 25613,78  (9,31%) min: 262853,58 max: 341142,37 weight: 3259
1000  JAVA   runtime[ns]: mean: 301519,22 +-  1720,92  (0,57%) min: 300213,27 max: 305867,59 weight: 2945
10000 ARRAYI runtime[ns]: mean:  396141,51 +- 14590,91 (3,68%) min:  387423,82 max:  430492,54 weight: 2434
10000 HEAP2  runtime[ns]: mean: 1480709,29 +- 21551,04 (1,46%) min: 1456021,11 max: 1527109,21 weight: 527
10000 HEAP24 runtime[ns]: mean: 1575388,92 +- 23412,45 (1,49%) min: 1537285,68 max: 1604644,04 weight: 558
10000 HEAP4L runtime[ns]: mean: 1822968,86 +- 66315,35 (3,64%) min: 1782957,93 max: 1985361,31 weight: 498
10000 HEAP3  runtime[ns]: mean: 1901088,05 +- 29840,63 (1,57%) min: 1867122,21 max: 1949673,34 weight: 386
10000 HEAP3L runtime[ns]: mean: 1987847,72 +- 35116,84 (1,77%) min: 1950965,17 max: 2047777,88 weight: 456
10000 HEAP5L runtime[ns]: mean: 2224519,34 +- 20506,15 (0,92%) min: 2203073,39 max: 2258665,67 weight: 362
10000 HEAP4  runtime[ns]: mean: 2239727,96 +- 47886,54 (2,14%) min: 2181654,78 max: 2302378,37 weight: 408
10000 JAVA   runtime[ns]: mean: 3114948,76 +-  9139,43 (0,29%) min: 3098143,13 max: 3127734,94 weight: 300
10000 HEAP5  runtime[ns]: mean: 4602213,44 +- 1316228,25 (28,60%) min: 2863835,65 max: 5954895,91 weight: 263
```

## Building and running ##

To build, use
```
mvn compile package install appassembler:assemble
```
To run, use
```
sh target/appassembler/bin/caliper-analyze
```

If you do not give file names, it will read the last modified file automatically.

While caliper-analyze has some heuristics to sort data (mostly by the number of
distinct values), this heuristic may fail. You can however easily resort the
data yourself on the command line:

```
sh target/appassembler/bin/caliper-analyze | sort -t" " -k1,1 -k4n
```
will sort by columns 1 to 1 (alphabetically), then 4-end (numerical)

## Tips & Tricks ##

caliper-analyze can actually merge multiple results. Just load multiple files
on the command line, and it will treat them as one run (it is up to you to
ensure that this is fair, and you e.g. didn't use different computers or
changed the source code in the meantime!)

```
    sh target/appassembler/bin/caliper-analyze ~/.caliper/results/MyBenchmarkClass*.json*
```

The results **could** come from both micro and macrobenchmarks, but I havn't
checked if this makes sense.
There will likely be a mismatch in overhead for micro- and macro-benchmarks.

## Future plans ##

I'm currently working on **trend estimation** for parameters. This works to some
extend, but you probably need to perform a larger benchmark experiment, e.g.
with many different values of the size parameter. As of now, trend prediction
will only run when you provide at least 8 different values.

Here is an example result. Note that the trend estimation for the textbook
quicksort (using the first element as pivot) was not reasonably estimated. Also
for the insertion sort with binary search, the result looks a bit odd (but may
actually make sense, as the search cost is `O(n log n)`, only the insertion
cost is `O(n * n)`, and it calls `System.arraycopy` for them.

Note that this example output is _manually_ formatted and organized.

```
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
```


---


## License ##

I deliberately chose the AGPL-3 license. This is a variant of the GPL-3 licence, commonly considered to be GPL-3 compatible, and in my opinion the better GPL, for a simple reason:
Roughly said, it is GPL-3 with an additional copyleft for interaction with the application
of the web: if you embed this code into a web application, you must also share the source
code. (Note that it still only applies to people that you grant access to the application.)
This is a deliberate restriction: if you embed this functionality, you will also have to
adhere to copyleft regulations; not just if you give someone the compiled program.