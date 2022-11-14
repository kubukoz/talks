---
# try also 'default' to start simple
theme: default
# random image from a curated Unsplash collection by Anthony
# like them? see https://unsplash.com/collections/94734566/slidev
background: https://images.unsplash.com/photo-1611702700098-dec597b27d9d?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxjb2xsZWN0aW9uLXBhZ2V8MjB8OTQ3MzQ1NjZ8fGVufDB8fHx8&auto=format&fit=crop&w=900&q=60
# apply any windi css classes to the current slide
class: 'text-center'
# https://sli.dev/custom/highlighters.html
highlighter: prism
# show line numbers in code blocks
lineNumbers: false
# persist drawings in exports and build
drawings:
  persist: false
# use UnoCSS
css: unocss
---

# Things I didn't want to know about JVM bytecode but learned anyway

Jakub Kozłowski

---

<style>
.columnz {
  display: flex;
  height: 80%;
  div {
    flex: 1;
    text-align: center;
    /* background: red; */
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;

    svg {
      width: 6em;
      height: 6em;
      margin-bottom: 0.5em;
    }
  }
}
</style>

# Agenda

<div class="columnz">
<div class="lhs">

<ant-design-layout-outlined />

Classfile overview

</div>

<div class="chs">

<codicon-file-binary />
Binary files 101

</div>
<div class="rhs">

<ant-design-file-search-outlined />
Classfile encoding

</div>
</div>

---
layout: center
---

# Classfile overview

---
layout: two-cols
class: whats-a-classfile
---

<style>
  .whats-a-classfile.col-right {
    background-image: url("compilation.png");
    background-repeat: no-repeat;
    background-position: center;
    background-size: contain;
    background-color: white;
  }
</style>

# What's a classfile

- binary file
- **output** of a compiler
- **input** format for the JVM
- respresents **one** class/interface/~~module~~
- a single source can produce 0..n classes

::right::

---

# Classfile structure

```scala {all|2-3|4|5|6-7|8|9-10|11}
case class ClassFile(
  minorVersion: Int,
  majorVersion: Int,
  constants: ConstantPool,
  accessFlags: Set[ClassAccessFlag],
  thisClass: ClassName,
  superClass: ClassName,
  interfaces: List[InterfaceName],
  fields: List[FieldInfo],
  methods: List[MethodInfo],
  attributes: List[AttributeInfo],
)
```

<p>everything in this talk is based on <a href="https://docs.oracle.com/javase/specs/jvms/se17/html/index.html">the Java SE 17 spec</a>.</p>

---
layout: center
---

# Binary files 101

---

# Binary files 101

- binary file: sequence of bits - e.g. `0b1100101010110101100`
- bit: single digit in a base-2 numeric system - (`0` or `1`)
- byte: a group of 8 bits (_usually_) - e.g. `0b10011111`, or `0x9f`
- we'll mostly be dealing with unsigned, big-endian integers: `0x000e` means `14` in decimal

---
layout: center
---

# Classfile encoding

---

# Classfile binary format

- big-endian 8-bit unsigned bytes
- `u1`: 1 unsigned byte
- `u2`: 2 unsigned bytes
- `u4`: ...

---
layout: two-cols
class: classfile-encoding-sample
---

<style>
.classfile-encoding-sample.col-right {
  /* background: red; */
  display: flex;
  flex-direction: column;
  justify-content: center;
}
</style>

# Magic number

`0b11001010111111101011101010111110`

<p v-click>⁉️</p>

<span v-click>`0xCAFEBABE` 💀</span>

::right::

```c {1,2,6}
ClassFile {
    u4             magic;
    u2             minor_version;
    u2             major_version;
    ...
}
```

---
layout: two-cols
class: classfile-encoding-sample
---

# JVM version

Minor, major version

- major - minimum JVM version required to run this
- minor - since JDK 12, either `0x0000` or `0xffff`&nbsp;(65&nbsp;535)
- minor goes first (for some reason)

::right::

```c {3,4}
ClassFile {
    u4             magic;
    u2             minor_version;
    u2             major_version;
    ...
}
```

---

# Major versions

- 52 (`0x0034`, Java 8)
- 55 (`0x0037`, Java 11),
- 61 (`0x003D`, Java 17)...
- ...enough space to let us have Java 65491 :)

If you get it wrong:

```scala
Error: LinkageError occurred while loading main class Foo
	java.lang.UnsupportedClassVersionError: Foo has been compiled by a more recent version
  of the Java Runtime (class file version 61.0), this version of the Java Runtime
  only recognizes class file versions up to 55.0
```

---

# Major version compatibility table

<div style="width: 100%; height: 100%; display: block; overflow: scroll">
<img src="compat-table.png" style="object-fit: contain; width: 50%;">
</div>

---
layout: two-cols
class: classfile-encoding-sample
---

# Constant pool

- an ordered list of reusable constants
- contains all literals, class/method/field/type names etc.
- prefixed with the amount of constants included
- each constant is prefixed with a discriminator byte (`u1 tag`)
- indices and sizes are **off by one (+1)**: a single-item pool has size `2`, and the only index is `1`

::right::

```c {3,4}
ClassFile {
    ...
    u2             constant_pool_count;
    cp_info        constant_pool[constant_pool_count-1];
    ...
}
```

---
layout: two-cols
class: classfile-encoding-sample
---

# Integer_info constant

- tag: `3` (`0x03`)
- content: `u4` (32-bit) signed integer
- example: `48` -> `0x00000030`

::right::

```c
CONSTANT_Integer_info {
    u1 tag;
    u4 bytes;
}
```

---
layout: two-cols
class: classfile-encoding-sample
---

# Class_info constant

- tag: `7` (`0x07`)
- content: `u1`: index to the constant pool
- **must** target an UTF-8 string constant (class name)
- example: index 2 (third item in pool) -> `0x03`

::right::

```c
CONSTANT_Class_info {
    u1 tag;
    u2 name_index;
}
```

---
layout: two-cols
class: classfile-encoding-sample
---

# Utf8_info constant

- tag: `1` (`0x01`)
- content:
  - `u2 length`: the amount of remaining bytes
  - `u1` x `length`: "modified UTF-8"-encoded

::right::

```c
CONSTANT_Utf8_info {
    u1 tag;
    u2 length;
    u1 bytes[length];
}
```

---

# Modified UTF-8 encoding

- <a href="https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-4.4.7">pretty complex</a>, but space efficieffffffnt: ASCII characters only use 1 byte each
- can be encoded/decoded with JDK's `DataOutputStream` / `DataInputStream`
- also used in Java Serialiation

Examples:

| input   | length   | data               |
| ------- | -------- | ------------------ |
| `hello` | `0x0005` | `0x68656c6c6f`     |
| `łódź`  | `0x0007` | `0xc582c3b364c5ba` |

---

# Real example

`javap -v Hello`

```rust
public class Foo
  minor version: 0
  major version: 48
  flags: (0x0021) ACC_PUBLIC, ACC_SUPER
  this_class: #2                          // Foo
  super_class: #4                         // java/lang/Object
  interfaces: 0, fields: 0, methods: 1, attributes: 1
Constant pool:
   #1 = Utf8               Hello
   #2 = Class              #1             // Hello
   #3 = Utf8               java/lang/Object
   #4 = Class              #3             // java/lang/Object
```

---
layout: two-cols
class: classfile-encoding-sample
---

# Long_info constant

- tag: `5` (`0x05`)
- content:
  - `u4`: high bytes
  - `u4`: low bytes

**but wait!**

::right::

```c
CONSTANT_Long_info {
    u1 tag;
    u4 high_bytes;
    u4 low_bytes;
}
```

---

## Long_info / Double_info

> All 8-byte constants take up two entries in the constant_pool table of the class file

<div v-click>
e.g. with this constant pool

| index    | 1    | 2     | 3   | 4        | 5   | 6    |
| -------- | ---- | ----- | --- | -------- | --- | ---- |
| contents | utf8 | class | int | **long** | ❌   | utf8 |

the pool's size is still 6 (encoded as `0x0007`)!
</div>

---

## What's that? A file input?

<div style="overflow: scroll; height: 100%; margin-top: 2em; font-family: monospace;">
<input
    type="file"
    id="file_input"
    accept=".class"
    onchange="fileUploadedSimple()"
  />
<div id="output"></div>
</div>
