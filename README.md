jvpk
=======

This is a simple Java library to read Valve's VPK archive files.
Write support also should be possible, but I won't implement it until I fully understand the file format of VPK v2.

It supports both VPK v1 and v2. The old headerless VPK format as well as the "Vampire: The Masquerade - Bloodlines" VPK format, on the other hand, isn't supported.

It is writen for Java 6 and uses NIO buffers and memory mapping for good I/O performance.

Dependencies
------------

* [apache-commons-io-2.0.1](http://commons.apache.org/io/)