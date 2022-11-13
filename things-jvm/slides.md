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
layout: two-cols
---

<style>
  .col-right {
    background-image: url("compilation.png");
    background-repeat: no-repeat;
    background-position: center;
    background-size: contain;
    background-color: white;
    /* display: flex; */
  }
</style>

# What's a classfile

- binary file produced by a compiler (usually)
- input format for the JVM
- corresponds to **one** class/interface/module

::right::

---

# Classfile overview (simplified)

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
