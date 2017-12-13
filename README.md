# bootcamp

## generate data
launch com.datastax.atweiter.GenerateData

## create schema
cqlsh -f src/main/resources/schema.cql

## solr

### create core

`dsetool create_core atwater.product generateResources=true`

`dsetool reload_core atwater.product solrconfig=src/main/resources/solrconfig.xml schema=src/main/resources/core_product.xml reindex=true deleteAll=true`


### load data
`TRUNCATE atwater.product;`

`COPY atwater.product (product_id,name,brandname,short_description,department,long_description_path,image_paths,video_paths,sku,upc,product_tags,rating_count,avg_rating,price,location_with_stock) FROM '/home/florent/product.csv' ;`

`TRUNCATE atwater.product_quantity_by_location;`

`COPY atwater.product_quantity_by_location (product_id,location,quantity) FROM '/home/florent/productLocalisation.csv' ;`

### build suggest

`http://localhost:8983/solr/atwater.product/suggest?suggest=true&suggest.dictionary=nameSuggester&suggest.build=true&suggest.reload=true`

### test suggest

`http://localhost:8983/solr/atwater.product/suggest?suggest=true&suggest.q=Lig`

`http://localhost:8983/solr/atwater.product/suggest?suggest=true&suggest.cfq=ending&suggest.q=dema`

### facet

`select * from atwater.product where solr_query = '{"q":"*:*","fq":"name:light","facet":{"range":"avg_rating","f.avg_rating.range.start":0,"f.avg_rating.range.end":5,"range.gap":1}}';`

`select * from atwater.product where solr_query = '{"q":"*:*","fq":"name:light", "facet":{"field":["product_tags","brandname","department"],"limit":10}}';`

`select * from atwater.product where solr_query = '{"q":"*:*","fq":"name:light","facet":{"field":["product_tags","brandname","department"],"limit":10,"range":"avg_rating","f.avg_rating.range.start":0,"f.avg_rating.range.end":5,"f.avg_rating.range.gap":1}}';`


### search

search with sort and pagination:
`select * from atwater.product where solr_query = '{"q":"*:*","fq":"name:light","sort":"avg_rating desc","start":"10"}' limit 10;`

*with cqlsh think to do "paging off;"*


## analytics

conf kafka

`bin/zookeeper-server-start.sh config/zookeeper.properties`

`bin/kafka-server-start.sh config/server.properties`

`bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic events`

`bin/kafka-console-producer.sh --broker-list localhost:9092 --topic events`
