create_clock -period 1.500 -waveform {0.000 0.750} [get_ports fast_clk]
create_clock -period 3.000 -waveform {0.000 1.500} [get_ports clk]

# slow 2 fast
set_multicycle_path -setup -end -from clk -to fast_clk 2
set_multicycle_path -hold -end -from clk -to fast_clk 1

# fast 2 slow
set_multicycle_path -setup -start -from fast_clk -to clk 2
set_multicycle_path -hold -start -from fast_clk -to clk 1






