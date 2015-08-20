# Parallel-Code-Optimization
Code Optimization
</br><i><b>Introduction</b></i></br>
The code written converts the O'neils construct(special grammar) to three-op-form.The O'neil's construct has label,go-to syntax which needs to be eliminated upon conversion.
Dependency blocks among the code has been formulated.The code involves identifying the successor and predessor blocks based on the go-to statements in the blocks.All the generate 
and kill statements for each block are pre-calculated.The out,in statements have also been generated.Also,each block is checked for multiple definitions and redundancy has been 
deleted.A new set of statements have been insert namely phi block statements to replace multiple definitions.The code block statements have also been checked for <i>Constant 
Propagation<i>,<i> Constant Folding</i>,<i> Dead Code Elimination</i>,<i> Loop Normalization </i> and implemented.
