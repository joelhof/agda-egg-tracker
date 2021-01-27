This is an app for "Hönskooperativet Agda i Sjöbergen" to enable
members to report and track the hens egg laying.
It consists of a Reporting UI and a Statistics UI written in plain JS with some Vaadin WebComponents, chartjs.
Backend is a quarkus service fronting a Postgresql all served from Heroku free tier.

To run PostgreSQL in a docker container:

`start.sh`
or
`docker run --rm -p 5432:5432 -e POSTGRES_PASSWORD=agda --name postgres-dev postgres-11`

To connect to postgresql in running in Docker:

`psql -h localhost -p 5432 -U postgres`

To build docker image and tag it for Heroku:

`./build-with-docker-jvm.sh`

To run locally in docker:

`docker run --rm -it -p 8080:8080 -e PORT=8080 quarkus/egg-tracker-monolith-jvm
`
Push pre-built image to Heroku Container Registry:

`docker push registry.heroku.com/agda-egg-tracker/web
`
To get app running with database and Flyway on Heroku.

Run maven build inside Docker container on Heroku, then create second container
to deploy. This is done in Dockerfile Dockerfile.multistage.jvm.
Deploy is then made with a `heroku.yml` file.
App has custom config to set the quarkus.datasource.* config from the Heroku
DATABASE_URL, see class DatasourceConfig.

To upload a batch file to locally deployed:

`curl --data-binary "@src/main/resources/2020.csv" http://localhost:19080/diary/entries -H "content-type: text/plain"`


SELECT extract('week' from e.datetime) AS week_nr,
 extract('dow' from e.datetime) AS week_day,
  eggs, datetime
   FROM diary.entries AS e PARTITION BY week_nr, week_day;
