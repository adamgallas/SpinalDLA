# About SpinalDLA
SpinalDLA is an RTL design library for deep learning accelerators based on SpinalHDL, aiming to provide agile development tools for hardware developers specializing in customized hardware.

SpinalDLA integrates the following features:

- flexible and versatile DSP-based arithmetic units on Xilinx FPGAs ```(coming soon)```
- very useful memory controller for deep learning applications ```(coming soon)```
- versatile systolic array instantiation templates ```(coming soon)```
- agile simulation for deep neural networks ```(coming soon)```
- several deep learning accelerator design examples ```(coming soon)```
- and more...

# Plug-and-Play RTL Modules

This library provides users with a set of plug-and-play RTL modules, which does not require users to have a deep understanding of SpinalHDL. Users can simply instantiate the modules and connect them together to build their own deep learning accelerators.

Located at ```verilog/``` directory, the RTL modules are written in Verilog, which can be easily integrated into any existing RTL design flow.

The corresponding simulation waveforms are located at ```fst/``` directory, which can be viewed by [GTKWave](http://gtkwave.sourceforge.net/).

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
libraryDependencies += "casia" %% "SpinalDLA" % "0.3"
```

3. Rebuid your project with `sbt compile`.