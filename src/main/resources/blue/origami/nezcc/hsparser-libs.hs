module NezParser (parser) where

import Control.Monad
import Control.Monad.ST
import Control.Applicative
import Data.ByteString
import Data.ByteString.Short
import Data.Word

data NezSubAST = Leaf String
              | Subtree [(String, NezAST)] --(label,AST)
              deriving (Show)

type NezAST = (String, NezSubAST)--(Tag,SUBAST)

type AST = Either NezAST NezSubAST

