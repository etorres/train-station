# Train Station
[![GitHub Workflow Status](https://img.shields.io/github/workflow/status/etorres/train-station/CI)](https://github.com/etorres/train-station/actions?query=workflow%3A%22CI%22)
[![Codecov](https://img.shields.io/codecov/c/github/etorres/train-station?logo=codecov)](https://codecov.io/gh/etorres/train-station)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

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
