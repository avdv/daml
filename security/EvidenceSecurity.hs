-- Copyright (c) 2022 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
-- SPDX-License-Identifier: Apache-2.0

module Main (main) where

import Control.Monad (when,void)
import Data.List ((\\),sortOn)
import Data.List.Extra (groupOn,foldl')
import Data.Map (Map)
import Data.Text (Text)
import Data.Void (Void)
import System.Exit (exitWith,ExitCode(ExitFailure))
import System.FilePath (splitPath)
import System.IO.Extra (hPutStrLn,stderr)
import Text.Megaparsec (Parsec,runParser,errorBundlePretty,eof,takeWhileP,single,label,satisfy,noneOf,chunk,(<|>),some)
import qualified Text.Megaparsec.Char (space)
import qualified Data.Char as Char (isDigit,digitToInt)
import qualified Data.Map as Map (fromList,toList)
import qualified Data.Text as T (pack,unpack)
import qualified Data.Text.IO as T (getContents)

{-
Generate _security evidence_ by documenting _security_ test cases.

Security tests may be found anywhere in the Daml repository, and written in any language
(scala, haskell, shell, etc). They are marked by the *magic comment*: "TEST_EVIDENCE"
followed by a ":".

Following the marker, the remaining text on the line is split on the next ":" to give:
        Category : Free text description of the test case.

There are a fixed set of categories, listed in the enum below. There expect at least one
testcase for every category.

The generated evidence is a markdown file, listing each testcase, grouped by Category. For
each testcase we note the free-text with a link to the line in the original file.

This program is expected to be run with stdin generated by a git grep command, and stdout
redirected to the name of the generated file:

```
git grep --line-number TEST_EVIDENCE\: | bazel run security:evidence-security > security-evidence.md
```
-}

main :: IO ()
main = do
  text <- T.getContents
  lines <- parseLines text
  let missingCats = [minBound..maxBound] \\ [ cat | Line{cat} <- lines ]
  when (not $ null missingCats) $ do
    messageAndExitFail ("No tests for categories: " ++ show missingCats)
  putStrLn (ppCollated (collateLines lines))

type Parser = Parsec Void Text

parseLines :: Text -> IO [Line]
parseLines text = do
  case runParser theParser "<stdin>" text of
    Right xs -> pure xs
    Left e -> messageAndExitFail $ errorBundlePretty e

messageAndExitFail :: String -> IO a
messageAndExitFail message = do
  hPutStrLn stderr "** EvidenceSecurity: generation failed:"
  hPutStrLn stderr message
  exitWith $ ExitFailure 1

theParser :: Parser [Line]
theParser = some line <* eof
  where
    line = do
      filename <- some notColonOrNewline
      colon
      lineno <- number
      colon
      marker
      colon
      optWhiteSpace
      cat <- parseCategory
      colon
      optWhiteSpace
      freeText <- takeWhileP (Just "freetext") (/= '\n')
      void $ single '\n'
      pure Line {cat, desc = Description{filename,lineno,freeText}}

    number = foldl' (\acc d -> 10*acc+d) 0 <$> some digit
    digit = label "digit" $ Char.digitToInt <$> satisfy Char.isDigit

    marker =
      (void $ chunk "TEST_EVIDENCE")
      <|> do void notColonOrNewline; marker

    optWhiteSpace = Text.Megaparsec.Char.space

    parseCategory = do
      foldl1 (<|>)
        [ do void $ chunk $ T.pack $ ppCategory cat; pure cat
        | cat <- [minBound..maxBound]
        ]

    colon = void $ single ':'

    notColonOrNewline = noneOf [':','\n']


data Category = Authorization | Privacy | Semantics | Performance | InputValidation
  deriving (Eq,Ord,Bounded,Enum,Show)

data Description = Description
  { filename:: FilePath
  , lineno:: Int
  , freeText:: Text
  }

data Line = Line { cat :: Category, desc :: Description }

newtype Collated = Collated (Map Category [Description])

collateLines :: [Line] -> Collated
collateLines lines =
  Collated $ Map.fromList
  [ (cat, [ desc | Line{desc} <- group ])
  | group@(Line{cat}:_) <- groupOn cat (sortOn cat lines)
  ]

ppCollated :: Collated -> String
ppCollated (Collated m) =
  unlines (["# Security tests, by category",""] ++
           [ unlines (("## " ++ ppCategory cat ++ ":") : map ppDescription (sortOn freeText descs))
           | (cat,descs) <- sortOn fst (Map.toList m)
           ])

ppDescription :: Description -> String
ppDescription Description{filename,lineno,freeText} =
  "- " ++ T.unpack freeText ++  ": [" ++ basename filename ++ "](" ++ filename ++ "#L" ++ show lineno ++ ")"
  where
    basename :: FilePath -> FilePath
    basename p = case reverse (splitPath p) of [] -> ""; x:_ -> x

ppCategory :: Category -> String
ppCategory = \case
  Authorization -> "Authorization"
  Privacy -> "Privacy"
  Semantics -> "Semantics"
  Performance -> "Performance"
  InputValidation -> "Input Validation"
