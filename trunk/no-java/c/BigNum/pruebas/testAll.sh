#!/bin/bash
javac TestGenerator.java && java TestGenerator > testBig.in &&  echo "Python:" && time python good_solutions.py < testBig.in > testBig.spython && javac GoodSolutions.java && echo -e "\nJava:" && time java GoodSolutions < testBig.in > testBig.sjava && gcc -O2 testbn.c 2> comp.out && echo -e "\nBnlib:" && time ./a.out < testBig.in > testBig.out && diff testBig.spython testBig.sjava && diff testBig.sjava testBig.out