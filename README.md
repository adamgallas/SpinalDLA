# About SpinalDLA
SpinalDLA is an RTL design library for deep learning accelerators based on SpinalHDL, aiming to provide agile development tools for hardware developers specializing in customized hardware.

SpinalDLA integrates the following features:

- flexible and versatile DSP-based arithmetic units on Xilinx FPGAs
- very useful memory controller for deep learning applications
- versatile systolic array instantiation
- agile simulation for deep neural networks
- several deep learning accelerator design examples
- and more...

# How to Use it
This library is based on SpinalHDL with SBT build.

1. Add sbt-github-packages plugin to enable sbt to consume the package. Add this line to your `./project/plugins.sbt` file:
```
addSbtPlugin("com.codecommit" % "sbt-github-packages" % "0.5.2")
```
2. Add the following lines to your `./build.sbt` file:
```
githubTokenSource := TokenSource.GitConfig("github.token")
resolvers += Resolver.githubPackages("adamgallas", "SpinalDLA")
libraryDependencies += "casia" %% "SpinalDLA" % "0.1"
```

3. Rebuid your project with `sbt compile`.