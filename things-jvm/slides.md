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

---
layout: center
---

# Binary files 101

---

# Binary files 101

- binary file: sequence of bits - e.g. `0b1100101010110101100`
- bit: single digit in a base-2 numeric system - (`0` or `1`)
- byte: a group of 8 bits (_usually_) - e.g. `0b10011111`, or `0x9f`
- we'll be dealing with unsigned, big-endian integers only: `0b1110` / `0x0e` means <!-- Poprawic -->`14` in decimal

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

# Classfile encoding

Magic number

`0b11001010111111101011101010111110`

<p v-click>⁉️</p>

<span v-click>`0xCAFEBABE`</span>

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

# Classfile encoding

Minor, major version

- major - minimum JVM version required to run this
- minor - since JDK 12, either `0x0000` or `0xffff`&nbsp;(65&nbsp;535)
- minor goes first (for some reason)

::right::

```c {1,3,4,6}
ClassFile {
    u4             magic;
    u2             minor_version;
    u2             major_version;
    ...
}
```

---

# Classfile encoding

Major versions:

- 52 (`0x0034`, Java 8)
- 55 (`0x0037`, Java 11),
- 61 (`0x003D`, Java 17)...

If you get it wrong:

```scala
Error: LinkageError occurred while loading main class Foo
	java.lang.UnsupportedClassVersionError: Foo has been compiled by a more recent version
  of the Java Runtime (class file version 61.0), this version of the Java Runtime
  only recognizes class file versions up to 55.0
```

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
