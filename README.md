[![Travis CI](https://travis-ci.org/peshrus/revolut-interview-backend-2019.svg?branch=master)](https://travis-ci.org/peshrus/revolut-interview-backend-2019)
# [Revolut Backend Test](https://drive.google.com/file/d/19IM0HmgQO_Tggke8wh7N764l67JFwMnq/view)
Design and implement a RESTful API (including data model and the backing implementation)
for money transfers between accounts.

## Explicit requirements:
1. You can use Java, Scala or Kotlin.
2. Keep it simple and to the point (e.g. no need to implement any authentication).
3. Assume the API is invoked by multiple systems and services on behalf of end users.
4. You can use frameworks/libraries if you like (except Spring), but don't forget about
requirement #2 – keep it simple and avoid heavy frameworks.
5. The datastore should run in-memory for the sake of this test.
6. The final result should be executable as a standalone program (should not require
a pre-installed container/server).
7. Demonstrate with tests that the API works as expected.

## Implicit requirements:
1. The code produced by you is expected to be of high quality.
2. There are no detailed requirements, use common sense.

**Please put your work on github or bitbucket.**

## Solution details:
1. Java is used for the implementation.
2. The implementation is quite simple. Nothing redundant.
3. API is assumed to be invoked by multiple systems and services on behalf of end users.
4. Used libraries:
    - Logback - fast and simple logging
    - Guice - lightweight dependency injection
    - Javalin - a simple web framework
    - Jedis - a small redis client
    - embedded-redis - Redis embedded server
    - Junit - a well-known and simple framework for unit tests
    - Mockito - a simple mocking framework for unit tests
    - zerocode - a lightweight API testing framework
5. Redis is used to meet requirement #5
In-memory H2 Database could be used instead of Redis but in such case we would need an additional 
tool to sync it with data in some database on the disk. While Redis has the persistence mechanism
(https://redis.io/topics/persistence).
On the other hand in this particular case it's possible to store data in simple ConcurrentHashMap.
6. Gradle Application Plugin is used to meet requirement #6.
embedded-redis is also used as the data store to meet that requirement but in a production 
environment I would wrap the application into a Docker container and I would use another Docker 
container for Redis. Kubernetes would be used for orchestration. 
7. See unit tests.

## How to run:
1. Run `./gradlew clean build` or `gradlew.bat clean build`
2. Go to `build/distributions` and unpack `interview-backend-<VERSION>-SNAPSHOT.tar` or 
`interview-backend-<VERSION>-SNAPSHOT.zip`
3. Go to `interview-backend-<VERSION>-SNAPSHOT/bin` and run there `./interview-backend` or 
`interview-backend.bat`. 
You can run it with optional parameters: `./interview-backend <REDIS_HOST> <REDIS_PORT> <REST_HOST>`
4. Open `http://localhost:7000/` in your browser, `Revolut Backend Test` should be displayed there.
The application is ready for usage.

## Load testing:
1. Run `./gradlew clean build -PenableLoadTest` or `gradlew.bat clean build -PenableLoadTest`
2. Find reports in `target/zerocode-junit-granular-report.csv` and 
`target/zerocode-junit-interactive-fuzzy-search.html`
3. **NOTE: it's just an example pre-configured to run 200 parallel requests** 

## Review result with my comments:
- The solution is not synchronised: balances might change between get and set operations which will 
cause the inconsistent state

That's completely right. That's why I considered another approach but didn't complete it, 
unfortunately: https://github.com/peshrus/revolut-interview-backend-2019/commit/0acf1d6f991fa8d9d411caa653668db9c16eaa19#diff-53a12fc275be5173149facb06b630eb3R83
I'll try to implement it so. In the case of the relational database, I would simply do the changes 
directly in the storage and I would add a constraint on the field to reject negative values.

- Load test does not test concurrent transfers

It's not true. It doesn't check the validity of the result, that's the issue. I'll add a proper unit 
test checking concurrent transfers.
As of load tests, they have to be implemented in a different way anyway: the application is on one 
server and a JMeter instance running tests is on a different machine.

- Poor REST: GET for transfers - GET requests should be idempotent

POST requests are used actually: https://github.com/peshrus/revolut-interview-backend-2019/commit/ee88535b6a2a110f45d00889cf6a9d1750d2a089#diff-4591e7c4054eea81edc493b567da1243R90  