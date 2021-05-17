# sbt-lessc

Compilation of .less files to .css using your local lessc command.

This plugin is made from [less-sbt](https://github.com/softprops/less-sbt) the only difference is that it uses NodeJS lessc command instead of Rino JavaScript virtual machine. I've made this plugin just because of NodeJS lessc is working much quicker for me. But this kind of solution require you have lessc installed and configured.

## Install

```scala
val verLessc = "0.1.3"

lazy val lesscPlugin = uri(s"https://github.com/hitsoft/sbt-lessc.git#$verLessc")

lazy val root = project.in(file("."))
	.dependsOn(lesscPlugin)
```

_NOTE_ this plugin tested with lessc v.3.8.1 on nodejs v.7.5.0

_NOTE_ this plugin is targeting the release of sbt, 0.13.7

You will need to add the following to your `project/build.properties` file if you have multiple versions of sbt installed

    sbt.version=0.13.7

Be sure to use the [latest launcher](http://www.scala-sbt.org/0.13.0/docs/Getting-Started/Setup.html#installing-sbt).


## Usage

### Out of the box

Add the following to your `build.sbt` file

```scala
seq(closureSettings:_*)
```

Default configuration suppose you have following files structure

`src/main/less` - the place where your less files should be placed

All files named *.entry.less will be compiled to .css

If you'd like to specify some custom filter of files to compile, you could use following snippet

```scala
// Specify here your main .less file name that will be compiled to css/*.css file in your webapp
(LessKeys.entryFilter in (Compile, LessKeys.less)) := "app.less"
```

## Customization

Here is the list of options (keys) that can be adjusted in your `build.sbt` script

```scala
// Turn on output css file minimization
(LessKeys.mini in (Compile, LessKeys.less)) := true
```
