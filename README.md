# About SpinalDLA
SpinalDLA is an RTL design library for deep learning accelerators based on SpinalHDL, aiming to provide agile development tools for hardware developers specializing in customized hardware. ```(still under development)```

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

The table is listed below:

| Module Name             | Description                                                                                                      |
|:------------------------|:-----------------------------------------------------------------------------------------------------------------|
| ```int_8_mul.v```       | Xilinx INT8 Packing Technique                                                                                    |
| ```unt_4_mul.v```       | Xilinx INT4 Packing Technique                                                                                    |
| ```int12_xadd.v```      | Spike-Based Synaptic Operation                                                                                   |
| ```int8_dotp.v```       | Xilinx INT8 Dot Product Chain                                                                                    |
| ```int8_ws_B_P.v```     | INT8 Weight Stationary Systolic Array Chain with B and P Cascade, B path is used by In-DSP Operand Prefetching.  |
| ```int8_ws_AD_B.v```    | INT8 Weight Stationary Systolic Array Chain with AD and B Cascade, B path is used by In-DSP Operand Prefetching. |
| ```int16_dotp.v```      | INT16 Dot Product Chain                                                                                          |
| ```int16_dotp_ddr.v```  | INT16 Dot Product Chain with In-DSP Time-Multiplexing                                                            |
| ```int16_ws_B_P.v```    | INT16 Weight Stationary Systolic Array Chain with B and P Cascade                                                |
| ```int16_os_B_P.v```    | INT16 Output Stationary Systolic Array Chain with B and P Cascade, P path is used by Partial Sums Offloading.    |
| ```int24_acc_scale.v``` | SIMD=2 Accumulate then Scale Operation                                                                           |                                                                                                             |


# How to Use it
This library is based on SpinalHDL with SBT build. ```(still under development)```

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
