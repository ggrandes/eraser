# Eraser

Eraser is a secure file erasure. Open Source Java project under Apache License v2.0

### Current Stable Version is [1.0.0](https://maven-release.s3.amazonaws.com/release/org/javastack/eraser/1.0.0/eraser-1.0.0.jar)

---

## DOC

#### Configuration: System Properties

 - Type or erasure: ```eraser.type```
   - Types: Zero (Z), One (O), Random (R)
   - Example: -Deraser.type=OR
   - Default value: OZR
 - Block size: ```eraser.blocksize```
   - Example: -Deraser.blocksize=65536
   - Default value: 4096

---

## Running

    java -Deraser.type=R -jar eraser-x.y.z.jar <file>

---
Inspired in [DBAN](http://www.dban.org/), this code is Java-minimalistic version.
