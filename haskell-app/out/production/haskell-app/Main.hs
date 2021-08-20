{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE DeriveGeneric #-}
module Main where

import GHC.Generics
import Data.Aeson
import Network.Wai
import Network.Wai.Handler.Warp
import Network.HTTP.Types (ok200, notFound404, methodNotAllowed405)
import Data.Monoid
import qualified Data.ByteString as B
import qualified Data.ByteString.Char8 as C
import qualified Data.ByteString.Builder as BB
import qualified Data.ByteString.Lazy as L
import Lib
import Crawler

main :: IO ()
main = do
    let port = 3000
    putStrLn $ "Listening on port " ++ show port
    run port app
--curl -vvv -X POST "http://localhost:3000/crawler" -d '{"urls": {"ya": "https://ya.ru"}, "childrenLevel": 2, "childrenPattern": "https://yandex\\.ru.*"}'
app :: Application
app req respond = 
    case pathInfo req of
        ["crawler"] -> 
          case requestMethod req of 
            "POST" -> do
                        body <- (getRequestBodyChunk req)
                        case parse body of
                          Right(request@Crawler.CrawlingRequest {}) -> 
                            do
                              response <- crawl request
                              respond $ ok response
                          Left(error) -> respond $ errorRequest error
            _ -> respond badRequest
        _ -> respond notFound

parse :: (FromJSON a) => B.ByteString -> Either String a
parse json = eitherDecodeStrict json 

ok :: (ToJSON a) => a -> Response
ok object = responseLBS ok200 [ ("Content-Type", "application/json; UTF-8") ] (encode object)

notFound :: Response
notFound = responseBuilder notFound404 [] "Not found"

badRequest :: Response
badRequest = responseBuilder methodNotAllowed405 [] "Bad request method"

errorRequest :: String -> Response
errorRequest error = responseBuilder methodNotAllowed405 [] (BB.string8 error)