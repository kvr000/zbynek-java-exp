# AWS Lambda Java SnapStart experiment

Java AWS Lambda, with SnapStart enabled to improve cold start.


## Benchmark

| Type              | Time  |
|-------------------|-------|
| Java-17 Cold      | 1.002 |
| Java-17 Warm      | 0.103 |
| Java-17 SnapStart | 1.255 |
| Python 3.10 Cold  | 4.355 |
| Python 3.10 Warm  | 0.164 |


## Setup


### System

```
```


### Initialization

```
npx sls plugin install -n serverless-vpc-plugin
serverless plugin install -n serverless-dependson-plugin
```


### Manual Tests

```
export HOST=https://whatever

curl -i -X GET $HOST/time
```
