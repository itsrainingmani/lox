# Getting Started

Welcome to the Lox programming language. I am following the *Crafting Interpreters* [book](http://craftinginterpreters.com/)

## Folder Structure

- `lox` : the root folder that contains the source code for the lox language
- `lox\tool`: the folder that contains other tooling to help with building the language. For ex., `GenerateAst.java` allows us to programmatically build a java file that implements Expressions.
- `lox\java`: the folder that contains the source code for the Java interpreter, `jlox`
- `lox\c`: the folder that contains the source code for the C Bytecode VM, `clox`

Meanwhile, the compiled output files will be generated in the `bin` folder by default

## Building Clox

There are two ways to build the `clox`, the Bytecode VM implementation of Lox -

- Using the conventional make commands that the author built
- Using the `build.zig` file that I wrote. This should be a little faster at compiling c files on average due to the power of zig's c compiler

To use `build.zig`, run the following command in your shell -

```shell
~ zig build
```

To build and open the clox REPL directly, run -

```shell
~ zig build repl
```

## Progress Report

* May 26<sup>th</sup>, 2022 at 10:43 AM ET, the entire jlox interpreter is complete and is passing all tests!
* March 25<sup>th</sup>, 2023 at 5:17 AM ET - Completed Chapter 20: Hash Tables