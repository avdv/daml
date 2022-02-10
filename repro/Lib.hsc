module LibA (double) where

#include "clib.h"

foreign import ccall "clib.h add" c_add :: Int -> Int -> Int

double :: Int -> Int
double a = c_add a a
