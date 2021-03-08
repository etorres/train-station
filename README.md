This is an adaptation of the blog entry [Event-driven railway network based on Pulsar](https://scala.monster/train-station/) (by [Pavels Sisojevs](https://github.com/psisoyev)) to the following stack:
* [Cats Effect](https://typelevel.org/cats-effect/).
* [Apache Kafka](https://kafka.apache.org/).
* [Weaver Test](https://disneystreaming.github.io/weaver-test/).

```commandline
curl --request POST 
     --url http://localhost:8082/departure 
     --header 'content-type: application/json' 
     --data '{
              "trainId": "456", 
              "to": {"station": "Valencia"}, 
              "expected": {"moment": "2020-10-04T21:21:15Z"}, 
              "actual": {"moment": "2020-09-18T22:05:42Z"}
             }'
```

```commandline
curl --request POST 
     --url http://localhost:8082/arrival 
     --header 'Content-Type: application/json' 
     --data '{
              "trainId": "2b424db4-b111-4f27-8c7c-70f866c3cc50",
              "actual": {"moment":"2021-03-08T22:05:42Z"}
             }'
```

Useful links:
* [http4s-body-missing-test](https://github.com/bastewart/http4s-body-missing-test).