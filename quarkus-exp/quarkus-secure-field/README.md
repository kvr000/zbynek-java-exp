# quarkus-secure-field

Experiment with field level security on REST entities in Quarkus:

- reading: Quarkus REST `@SecureField` on `Ticket` fields (filtered during Jackson serialization)
- writing: custom `@SecureWriteField` enforced by `SecureFieldWriter` for POST / PUT / PATCH

Session is a signed JWT stored in an HttpOnly cookie, the same token is accepted as `Authorization: Bearer`.

## Run

```
mvn quarkus:dev
# or
mvn package && java -jar target/quarkus-secure-field-0.0.1-runner.jar
```

From IDE, run `com.github.kvr000.exp.java.quarkus.securefield.SecureFieldServer` (starts in dev mode).

Open http://localhost:8080/ , log in as `super` / `super`, switch roles on `/login/roles`.

## AWS Lambda (API Gateway HTTP API)

`mvn package` builds both the standalone jar and the Lambda package `target/lambda/function.zip` (nested build of the
`lambda` profile, skip it with `-Dlambda.skip`).  `mvn package -Plambda` builds the Lambda package only.

```
mvn package
npx serverless deploy                              # serverless.yml
# or
sam deploy -g -t src/lambda/aws/template.yaml
```

The `lambda` profile adds `quarkus-amazon-lambda-http`, which provides the handler
`io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest` (a custom handler class must not exist
alongside it).
The same resources, pages and JWT cookie / Bearer security run inside Lambda.  `mvn quarkus:dev -Plambda` runs locally
with the Lambda mock event server.  The API must be served on the `$default` stage (or a custom domain) as pages use
absolute paths.

## Ticket fields

| Field       | Read                | Write                          |
|-------------|---------------------|--------------------------------|
| id          | all                 | nobody (server assigned)       |
| system      | super, admin        | super, admin                   |
| title       | all                 | super, admin, user             |
| views       | super, admin        | super (GET /{id} increments)   |
| description | super, admin, user  | super, admin, user             |

API read requires any of super/admin/user/guest, write requires super/admin/user. Sending a field the caller cannot
write gives 403 unless its value is unchanged (so GET output can be sent back). PUT resets writable fields missing in
the body, PATCH is JSON merge patch.

JSON input is parsed leniently (`JacksonConfig`): trailing commas and unquoted keys are accepted, e.g.
`{title: "x", description: "y",}`.

```
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/ticket
curl -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -X PATCH -d '{"views":7}' http://localhost:8080/api/ticket/1
```

The JWT key pair in `src/main/resources/jwt/` is a demo key, never use it for anything real.

# AWS Lambda Java SnapStart experiment

## Setup

### System

```
```

### Initialization

```
npm install
npx serverless deploy
```


### Manual Tests

```
export HOST=https://whatever

curl -i -X GET $HOST/login/
```
