# To run postgresql in a Docker container.
docker run --rm -p 5432:5432 -e POSTGRES_PASSWORD=agda --name postgres-dev postgres-11