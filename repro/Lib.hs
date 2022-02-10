module Lib (double) where

foreign import ccall "add" c_add :: Int -> Int -> Int

double :: Int -> Int
double a = c_add a a

