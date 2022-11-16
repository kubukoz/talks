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

Jakub Kozłowski | The Art Of Scala 2nd edition | 16.11.2022

---

# Disclaimer

- I don't work directly on JVM bytecode during the workweek
- This was purely for fun and learning
- No JVMs were hurt while making this talk

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
    background-image: url("/compilation.png");
    background-repeat: no-repeat;
    background-position: center;
    background-size: contain;
    background-color: white;
  }
</style>

# What's a classfile

- binary file
- **output** of a compiler
- a single source can produce 0..n classes
- **input** format for the JVM
- represents **one** class/interface/~~module~~

::right::

---

# Classfile structure

```scala {all|2-3|4|5|6-7|8|9-10|11}
case class ClassFile(
  minorVersion: Int,
  majorVersion: Int,
  constants: ConstantPool,
  accessFlags: Set[ClassAccessFlag],
  thisClass: String,
  superClass: String,
  interfaces: List[InterfaceName],
  fields: List[FieldInfo],
  methods: List[MethodInfo],
  attributes: Map[AttributeName, AttributeInfo],
)
```

<p>everything in this talk is based on <a href="https://docs.oracle.com/javase/specs/jvms/se17/html/index.html">the Java SE 17 spec</a>.</p>

---
layout: center
---

# Binary files 101

---

# Binary files 101

- binary (non-text) file: sequence of bits - e.g. `0b1100101010110101100`
- bit: single digit in a base-2 numeric system - (`0` or `1`)
- byte: a group of 8 bits (_usually_) - e.g. `0b10011111`, or `0x9f`

---
layout: center
---

# Classfile encoding

---

# Classfile binary format

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

```c {1,2,18}
ClassFile {
    u4             magic;
    u2             minor_version;
    u2             major_version;
    u2             constant_pool_count;
    cp_info        constant_pool[constant_pool_count-1];
    u2             access_flags;
    u2             this_class;
    u2             super_class;
    u2             interfaces_count;
    u2             interfaces[interfaces_count];
    u2             fields_count;
    field_info     fields[fields_count];
    u2             methods_count;
    method_info    methods[methods_count];
    u2             attributes_count;
    attribute_info attributes[attributes_count];
}
```

---
layout: two-cols
class: classfile-encoding-sample
---

# JVM version

Minor, major version

- minor goes first (for some reason)
- major - minimum JVM version required to run this
- minor - since JDK 12, either:
  - `0x0000` (0, normal classfile) or
  - `0xffff` (65 535) - experimental features required

::right::

```c {3,4}
ClassFile {
    u4             magic;
    u2             minor_version;
    u2             major_version;
    u2             constant_pool_count;
    cp_info        constant_pool[constant_pool_count-1];
    u2             access_flags;
    u2             this_class;
    u2             super_class;
    u2             interfaces_count;
    u2             interfaces[interfaces_count];
    u2             fields_count;
    field_info     fields[fields_count];
    u2             methods_count;
    method_info    methods[methods_count];
    u2             attributes_count;
    attribute_info attributes[attributes_count];
}
```

---

# Major versions

- 52 (Java 8)
- 55 (Java 11),
- 61 (Java 17)...
- ...enough space (`u2`) to let us have Java 65491 :)

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
<img src="/compat-table.png" style="object-fit: contain; width: 50%;">
</div>

---
layout: two-cols
class: classfile-encoding-sample
---

# Constant pool

- an ordered list of reusable constants
- contains all literals, class/method/field/type names etc.
- prefixed with 2 bytes for the pool size
- each constant is prefixed with a discriminator byte (`tag`)
- indices and sizes are **off by one (start at 1)**

::right::

```c {5,6}
ClassFile {
    u4             magic;
    u2             minor_version;
    u2             major_version;
    u2             constant_pool_count;
    cp_info        constant_pool[constant_pool_count-1];
    u2             access_flags;
    u2             this_class;
    u2             super_class;
    u2             interfaces_count;
    u2             interfaces[interfaces_count];
    u2             fields_count;
    field_info     fields[fields_count];
    u2             methods_count;
    method_info    methods[methods_count];
    u2             attributes_count;
    attribute_info attributes[attributes_count];
}
```

---
layout: two-cols
class: classfile-encoding-sample
---

# Integer_info constant

- content: 4-byte (32-bit) **signed** integer
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

# Utf8_info constant

- content:
  - `u2 length`: the amount of data bytes
  - `u1` x `length`: "modified UTF-8"-encoded data

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

- <a href="https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-4.4.7">pretty complex</a>, but space efficient: ASCII characters only use 1 byte each
- can be encoded/decoded with JDK's `DataOutputStream`/`DataInputStream`
- also used in Java Serialiation

Examples:

| input     | length   | data               |
| --------- | -------- | ------------------ |
| `"hello"` | `0x0005` | `0x68656c6c6f`     |
| `"łódź"`  | `0x0007` | `0xc582c3b364c5ba` |


---
layout: two-cols
class: classfile-encoding-sample
---

# Class_info constant

- content: `u2 name_index`: index to the constant pool
  - **must** target a `UTF8_Info` constant (class name)
- example: index 2 (third item in pool) -> `0x03`

::right::

```c
CONSTANT_Class_info {
    u1 tag;
    u2 name_index;
}
```

---

# Real example

`javap -v Hello`

```rust {all|5|10|9|6|12|11}
public class Hello
  minor version: 0
  major version: 48
  flags: (0x0021) ACC_PUBLIC, ACC_SUPER
  this_class: #2                          // Hello
  super_class: #4                         // java/lang/Object
  interfaces: 0, fields: 0, methods: 1, attributes: 1
Constant pool:
   #1 = Utf8               Hello
   #2 = Class              #1             // Hello
   #3 = Utf8               java/lang/Object
   #4 = Class              #3             // java/lang/Object
...
```

---
layout: two-cols
class: classfile-encoding-sample
---

# (last one I promise) Long_info constant

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

# Long_info / Double_info

> All 8-byte constants take up two entries in the constant_pool table of the class file

<div v-click>
e.g. with this constant pool

| index    | 1    | 2     | 3   | 4        | 5   | 6    |
| -------- | ---- | ----- | --- | -------- | --- | ---- |
| contents | utf8 | class | int | **long** | ❌   | utf8 |

the pool's size is still 6 (encoded as `7`)!
</div>

---
layout: two-cols
class: classfile-encoding-sample enums
---
<style>
.enums.col-left {
  pre {
    /* background: red; */
    width: fit-content;
  }
}
</style>

# Access flags

- `u2` (16-bit) bitmask
- info about class modifiers under non-overlapping bits:

```scala
Public -> 0x0001,
Final -> 0x0010,
Super -> 0x0020,
Interface -> 0x0200,
Abstract -> 0x0400,
Synthetic -> 0x1000,
Annotation -> 0x2000,
Enum -> 0x4000,
```

example: `Public` + `Enum` == `0x4001`

Not a lot of space left!

::right::

```c {7}
ClassFile {
    u4             magic;
    u2             minor_version;
    u2             major_version;
    u2             constant_pool_count;
    cp_info        constant_pool[constant_pool_count-1];
    u2             access_flags;
    u2             this_class;
    u2             super_class;
    u2             interfaces_count;
    u2             interfaces[interfaces_count];
    u2             fields_count;
    field_info     fields[fields_count];
    u2             methods_count;
    method_info    methods[methods_count];
    u2             attributes_count;
    attribute_info attributes[attributes_count];
}
```

---
layout: two-cols
class: classfile-encoding-sample
---

# Skip ahead

- we've talked about class names already
- one piece of trivia: if `super_class` contains `0` (non-existent index), it means you're looking at `java/lang/Object`
- interfaces/fields out of scope

::right::

```c {8-13}
ClassFile {
    u4             magic;
    u2             minor_version;
    u2             major_version;
    u2             constant_pool_count;
    cp_info        constant_pool[constant_pool_count-1];
    u2             access_flags;
    u2             this_class;
    u2             super_class;
    u2             interfaces_count;
    u2             interfaces[interfaces_count];
    u2             fields_count;
    field_info     fields[fields_count];
    u2             methods_count;
    method_info    methods[methods_count];
    u2             attributes_count;
    attribute_info attributes[attributes_count];
}
```

---
layout: two-cols
class: classfile-encoding-sample
---

# Methods

- prefixed with `u2` for the amount of methods

::right::

```c {14,15}
ClassFile {
    u4             magic;
    u2             minor_version;
    u2             major_version;
    u2             constant_pool_count;
    cp_info        constant_pool[constant_pool_count-1];
    u2             access_flags;
    u2             this_class;
    u2             super_class;
    u2             interfaces_count;
    u2             interfaces[interfaces_count];
    u2             fields_count;
    field_info     fields[fields_count];
    u2             methods_count;
    method_info    methods[methods_count];
    u2             attributes_count;
    attribute_info attributes[attributes_count];
}
```

---
layout: two-cols
class: classfile-encoding-sample
---

# Methods inside

- `u2`: access flags (similar to those of classes)
- `u2`: name index in constant pool (`Utf8_info`)
- `u2`: **descriptor** index in constant pool (`Utf8_info`)
- attributes, ignoring (classes have identical layout)

::right::

```c {1-4,7}
method_info {
    u2             access_flags;
    u2             name_index;
    u2             descriptor_index;
    u2             attributes_count;
    attribute_info attributes[attributes_count];
}
```

---

# Descriptors

Some examples:

| type                             | descriptor           |
| -------------------------------- | -------------------- |
| `Int`                            | `I`                  |
| `Double`                         | `D`                  |
| `String`                         | `Ljava/lang/String;` |
| `Unit`                           | `V`                  |
| `def f(a: Int, d: Double): Unit` | `(ID)V`              |
| `(Int, Double) => Unit`          | `Lscala/Function2;`  |

---
layout: two-cols
class: classfile-encoding-sample
---

# Attributes

- A "second class" construct
- key-value mapping (think `Map[String, Attribute]`)
- prefixed with `u2` for the amount of attributes
- each attribute has variable length

::right::

```c {16,17}
ClassFile {
    u4             magic;
    u2             minor_version;
    u2             major_version;
    u2             constant_pool_count;
    cp_info        constant_pool[constant_pool_count-1];
    u2             access_flags;
    u2             this_class;
    u2             super_class;
    u2             interfaces_count;
    u2             interfaces[interfaces_count];
    u2             fields_count;
    field_info     fields[fields_count];
    u2             methods_count;
    method_info    methods[methods_count];
    u2             attributes_count;
    attribute_info attributes[attributes_count];
}
```

---
layout: two-cols
class: classfile-encoding-sample
---

# Attributes inside

- `u2`: **name** constant pool index (`Utf8_Info` constant)
- `u4`: data length
- `u1` x `length`: data

::right::

```c
attribute_info {
    u2 attribute_name_index;
    u4 attribute_length;
    u1 info[attribute_length];
}
```

---
layout: two-cols
class: classfile-encoding-sample
---

# Example attributes - Code

- method attribute containing the actual "bytecode" of the method
- it has its own attributes!

::right::

```c {6,7,14,15}
Code_attribute {
    u2 attribute_name_index;
    u4 attribute_length;
    u2 max_stack;
    u2 max_locals;
    u4 code_length;
    u1 code[code_length];
    u2 exception_table_length;
    {   u2 start_pc;
        u2 end_pc;
        u2 handler_pc;
        u2 catch_type;
    } exception_table[exception_table_length];
    u2 attributes_count;
    attribute_info attributes[attributes_count];
}
```

---

# Attribute trivia

- The spec defines 30 standard attributes
  - 7 **must** be supported by JVMs (e.g. `Code`)
  - 10 **must** be supported by JDK libraries (e.g. `LineNumberTable`)
  - 13 are non-critical metadata (e.g. `Deprecated`)

<br/>

- Unrecognized attributes **must** be ignored

<br/>

- JVMs **must** ignore attributes that don't exist in a given classfile format
  - e.g. the `Record` attribute will be ignored in old JVMs even if the file targets an old major version

---

# Instruction trivia

- in JRE 17, there are ~202 opcodes
- most are constant-size but some are not
  - e.g.`tableswitch` (`0xaa`) has optional 0-3 byte padding after the opcode to ensure alignment

---

# What's that? A file input? 🤔

<div style="overflow: scroll; height: 100%; margin-top: 2em; font-family: monospace;">
<input
    type="file"
    id="file_input"
    accept=".class"
    onchange="fileUploadedSimple()"
  />
<div id="output"></div>
</div>

---

# Built with [scodec](http://scodec.org/)

```scala
val classFile: Codec[ClassFile] =
  (
    ("magic number " | constant(hex"CAFEBABE")) ::
      ("minor version" | u2) ::
      ("major version" | u2) ::
      constantPool ::
      classAccessFlags ::
      ("this class" | constantPoolIndex[Constant.ClassInfo]) ::
      ("super class" | constantPoolIndex[Constant.ClassInfo]) ::
      listOfN(
        "interface count" | u2,
        "interface index" | constantPoolIndex[Constant.ClassInfo],
      ) ::
      listOfN(
        "field count" | u2,
        fieldInfo,
      ) ::
      listOfN(
        "method count" | u2,
        methodInfo,
      ) ::
      attributes
  ).dropUnits.as[ClassFile]
```

---

# Resources

- [Java SE 17 spec (classfile format)](https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html)
- [Inside the JVM book](https://www.artima.com/insidejvm/blurb.html)
- [Mateusz's JVM book :)](https://leanpub.com/jvm-scala-book)

# Tools

- `javap`, `javap -v` (Metals can show these too!)
- `xxd` / `hexdump` / a dozen other binary editors/viewers
- `java.io` `DataInput`/`DataOutput`

---

# Thank you

- Watch my YouTube! <a href="https://yt.kubukoz.com">yt.kubukoz.com</a>
- Contact + slides + YT: <a href="https://linktr.ee/kubukoz">linktr.ee/kubukoz</a>

