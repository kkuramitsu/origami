[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](#license)
[![Build Status](https://travis-ci.org/kkuramitsu/origami.svg?branch=master)](https://travis-ci.org/kkuramitsu/origami)
<!--
[![Coverage Status](https://coveralls.io/repos/github/sekiguchi-nagisa/ydsh/badge.svg?branch=master)](https://coveralls.io/github/sekiguchi-nagisa/ydsh?branch=master)
-->

# ORIGAMI - an Extensible Language Engineering Framework

Currently, under heavy development.

Description
-----------

Origami is a language engineering framework that includes ..

* Nez parser generator based on typed parsing expression grammars
* Configurable type checker
* JVM bytecode compiler

License
-------

Origami is distributed under the [Apache License 2](http://www.apache.org/licenses/LICENSE-2.0), meaning that you are completely free to redistribute, modify, or sell it with almost no restrictions.

<!--
Documentation
-------------

* [wiki](https://github.com/kkuramitsu/origami/wiki)

Forums
------

* [origami-users](https://groups.google.com/group/origami-users)
* [origami-dev](https://groups.google.com/group/origami-dev)

-->

Maven Usage
-----------

Use the following definition to use Origami in your maven project:

```
<dependency>
  <groupId>blue.origami</groupId>
  <artifactId>origami</artifactId>
  <version>0.0.1</version>
</dependency>
```

Building
--------

### Requirements

* Maven 3+
* Java 8+

Check out and build:

```
git clone git://github.com/kkuramitsu/origami.git
cd origami
mvn install
```

### Getting started

```
$ java -ea -jar origami-X.Y.Z-run.jar run
java -ea -jar target/origami-0.0.1-run.jar
ORIGAMI-0.0.1 (Celery) on Java JVM-1.8.0_91
Copyright 2017, Kimio Kuramitsu and ORIGAMI project
Enter an input string to parse and run.
Tips: Start with an empty line for multiple lines.

>>> 
let f n = n + 1

  [#Source [#FuncDecl $name=[#NameExpr 'f'] $param=[# [#LetDecl $name=[#NameExpr 'n']]] $body=[#AddExpr $left=[#NameExpr 'n'] $right=[#IntExpr '1']]]]
>>> f 1
  [#Source [#ApplyExpr $recv=[#NameExpr 'f'] $param=[# [#IntExpr '1']]]]

[Generated] $C0.class size=276
public abstract class $C0 implements origami.ffi.OrigamiObject {
  public static final int f(int);
    descriptor: (I)I
    Code:
       0: iload_0
       1: iconst_1
       2: iadd
       3: ireturn

  public $C0();
    descriptor: ()V
    Code:
       0: aload_0
       1: invokespecial #13                 // Method java/lang/Object."<init>":()V
       4: return
}

 => 2 :int
>>> 


```


