# Train Station

This is an adaptation of the blog entry [Event-driven railway network based on Pulsar](https://scala.monster/train-station/) (by [Pavels Sisojevs](https://github.com/psisoyev)) to the following stack:
* [Cats Effect](https://typelevel.org/cats-effect/).
* [Apache Kafka](https://kafka.apache.org/).
* [PostgreSQL](https://www.postgresql.org/).
* [Weaver Test](https://disneystreaming.github.io/weaver-test/).

## Configuration

Required properties:
```properties
STATION=Barcelona
CONNECTED_STATIONS=Madrid,Valencia
JDBC_PASSWORD=changeme
```

Optional properties:
```properties
CI=true
```

## Examples

```commandline
curl --request POST 
     --url http://localhost:8080/departure 
     --header 'content-type: application/json' 
     --data '{
              "trainId": "456", 
              "to": {"station": "Valencia"}, 
              "expected": {"moment": "2021-02-27T21:21:15Z"}, 
              "actual": {"moment": "2021-02-27T22:05:42Z"}
             }'
```

```commandline
curl --request POST 
     --url http://localhost:8080/arrival 
     --header 'Content-Type: application/json' 
     --data '{
              "trainId": "2b424db4-b111-4f27-8c7c-70f866c3cc50",
              "actual": {"moment":"2021-03-08T22:05:42Z"}
             }'
```

## Useful Links
* [http4s-body-missing-test](https://github.com/bastewart/http4s-body-missing-test).