{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE DeriveGeneric #-}
{-# LANGUAGE DuplicateRecordFields #-}
{-# LANGUAGE QuasiQuotes #-}
module Crawler where

import GHC.Generics
import Network.HTTP.Client
import Network.HTTP.Client.TLS
import Network.HTTP.Types.Header()
import Network.HTTP.Types.Status
import Network.URI (URI (..), parseURI)
import Control.Concurrent.Async (mapConcurrently)
import Text.HTML.TagSoup
import Control.Monad.Catch (try, SomeException, displayException, toException)
import Data.Aeson
import Data.Map as M (Map, empty, fromList, toList)
import Data.Text as T (Text, pack, unpack, isPrefixOf, concat)
import Data.Text.Encoding (decodeUtf8)
import Text.RE.TDFA.Text
import qualified Data.ByteString.Lazy as L
import qualified Data.CaseInsensitive as CI
import Network.HTTP.Simple (setRequestHeader, parseRequest)

data CrawlingRequest = CrawlingRequest {
      urls :: Map Text Text,
      childrenPattern :: Text,
      childrenLevel :: Int
    } deriving (Generic, Show)

instance ToJSON CrawlingRequest

instance FromJSON CrawlingRequest

data SingleResponse = SingleResponse {
    successful :: Bool,
    code :: Maybe Int,
    message :: Text,
    headers :: Map Text Text,
    body :: Maybe Text,
    links :: [Text],
    children :: Maybe CrawlingResponse
  } deriving (Generic, Show)
  
instance ToJSON SingleResponse

instance FromJSON SingleResponse

newtype CrawlingResponse
  = CrawlingResponse {responses :: Map Text SingleResponse}
  deriving (Generic, Show)

instance ToJSON CrawlingResponse

instance FromJSON CrawlingResponse

prepareRequest :: Text -> IO Request
prepareRequest url = do
  parsedRequest <- parseRequest $ T.unpack url
  return parsedRequest
  
executeRequest :: Request -> Manager -> IO (Response L.ByteString)
executeRequest parsedRequest manager = do
  response <- httpLbs parsedRequest manager
  return response

extractLinks :: Text -> [Tag Text] -> [Text]
extractLinks parentUrl parsedBody = 
  map processHref $ filter (~== TagOpen (T.pack "a") [("href", "")]) parsedBody
  where 
    parentUrlSchema = maybe "http:" (T.pack . uriScheme) (parseURI $ T.unpack parentUrl)
    processHref tag = let url = fromAttrib "href" tag in 
      if T.isPrefixOf "//" url 
      then T.concat [parentUrlSchema, url]
      else url

parseResponse :: Text -> Either SomeException (Response L.ByteString) -> SingleResponse
parseResponse url (Left e) = SingleResponse {
    successful = False,
    code = Nothing,
    message = T.pack $ displayException e,
    headers = M.empty,
    body = Nothing,
    links = [],
    children = Nothing
  }
parseResponse url (Right response) = 
  let textBody = decodeUtf8 (L.toStrict (responseBody response))
      parsedBody = parseTags textBody
  in
      SingleResponse {
        successful = True,
        code = Just $ statusCode $ responseStatus response,
        message = decodeUtf8 $ statusMessage $ responseStatus response,
        headers = M.fromList (map (\ (name, value) -> (decodeUtf8 (CI.original name), decodeUtf8 value)) (responseHeaders response)),
        body = Just textBody,
        links = extractLinks url parsedBody,
        children = Nothing
      }

toRequest :: Text -> Int -> SingleResponse -> CrawlingRequest
toRequest pattern level response = 
  CrawlingRequest { 
    childrenPattern = pattern, 
    childrenLevel = level, 
    urls = M.fromList (map (\ url -> (url, url)) (filter matchPattern (links response)))}
  where
    matchPattern link = 
      case compileRegex(T.unpack(pattern)) of
        Just(re) -> matched $ link ?=~ re
        Nothing -> False

crawlChildren :: Text -> Int -> SingleResponse -> IO SingleResponse
crawlChildren _ 0 response = return response
crawlChildren pattern level response = do
  childrenResponse <- crawl $ toRequest pattern (level - 1) response
  return response { children = Just childrenResponse }

crawl :: CrawlingRequest -> IO CrawlingResponse
crawl req = do
  manager <- newManager tlsManagerSettings
  responses <- mapConcurrently (process manager) (toList (urls req))
  responsesWithChildren <- mapConcurrently processChildren responses
  return CrawlingResponse { responses = fromList responsesWithChildren }
  where
    processSafely manager url = try $ do
              request <- prepareRequest url
              response <- executeRequest request manager
              return response
    process manager (id, url) = fmap (\response -> (id, parseResponse url response)) (processSafely manager url) 
    processChildren (id, response) = do 
      responseWithChildren <- (crawlChildren (childrenPattern req) (childrenLevel req) response)
      return (id, responseWithChildren)
