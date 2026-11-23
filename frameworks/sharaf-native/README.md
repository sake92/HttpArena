# Sharaf on Scala Native

The plain HTTP and compressed JSON handlers match `sharaf`; database, TLS,
and static-file profiles are omitted. Potateh uses its default platform-thread
executor because Scala Native does not provide JVM virtual threads. The build pins
Scala Native `0.5.13-20261119-b703b41-SNAPSHOT` from the Sonatype Maven
snapshots repository for its newer GC fixes.

Build with Scala CLI and Docker, then run `./scripts/benchmark-lite.sh
sharaf-native baseline,pipelined,limited-conn,json-comp`. The build requires
Scala Native's LLVM and C toolchain and zlib development libraries.
