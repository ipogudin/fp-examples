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
import Text.HTML.TagSoup
import Control.Monad.Catch (try, SomeException, displayException)
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

data RequestContext = RequestContext {
    url :: Text,
    parsedRequest :: Either SomeException Request,
    response :: Either SomeException (Response L.ByteString)
  } deriving (Generic, Show)

prepareRequestContext :: (Text, Text) -> IO (Text, RequestContext)
prepareRequestContext (id, url) = do
  parsedRequest_ <- try (parseRequest $ T.unpack url)
  return (id, RequestContext {
    url = url,
    parsedRequest = parsedRequest_,
    response = Left $ error "Undefined"
  })
  
executeRequest :: (Text, RequestContext) -> Manager -> IO (Text, RequestContext)
executeRequest (id, r@RequestContext{parsedRequest = Left e}) manager = return (id, r)
executeRequest (id, r@RequestContext{parsedRequest = Right request}) manager = do
  response <- try (httpLbs request manager)
  return (id, r{response = response})

extractLinks :: Text -> [Tag Text] -> [Text]
extractLinks parentUrl parsedBody = 
  map processHref $ filter (~== TagOpen (T.pack "a") [("href", "")]) parsedBody
  where 
    parentUrlSchema = maybe "http:" (T.pack . uriScheme) (parseURI $ T.unpack parentUrl)
    processHref tag = let url = fromAttrib "href" tag in 
      if T.isPrefixOf "//" url 
      then T.concat [parentUrlSchema, url]
      else url

parseResponse :: RequestContext -> SingleResponse
parseResponse RequestContext {response = Left e} = SingleResponse {
  successful = False,
  code = Nothing,
  message = T.pack $ displayException e,
  headers = M.empty,
  body = Nothing,
  links = [],
  children = Nothing
}
parseResponse RequestContext {url = url, response = Right response} = 
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
toRequest pattern level resp = 
  CrawlingRequest { 
    childrenPattern = pattern, 
    childrenLevel = level, 
    urls = M.fromList (map (\ url -> (url, url)) (filter matchPattern (links resp)))}
  where
    matchPattern link = 
      case compileRegex(T.unpack(pattern)) of
        Just(re) -> matched $ link ?=~ re
        Nothing -> False

handleChildren :: Text -> Int -> SingleResponse -> IO SingleResponse
handleChildren _ 0 response = return response
handleChildren pattern level response = do
  childrenResponse <- crawl $ toRequest pattern (level - 1) response
  return response { children = Just childrenResponse }

crawl :: CrawlingRequest -> IO CrawlingResponse
crawl req = do
  manager <- newManager tlsManagerSettings
  requests <- mapM prepareRequestContext (toList (urls req))
  responses <- mapM (`executeRequest` manager) requests
  let parsedResponses = map (\(id, response) -> (id, parseResponse response)) responses
  handledResponse <- mapM processChildren parsedResponses
  return CrawlingResponse { responses = fromList handledResponse }
  where 
    processChildren (id, response) = do 
      r <- (handleChildren (childrenPattern req) (childrenLevel req) response)
      return (id, r)
