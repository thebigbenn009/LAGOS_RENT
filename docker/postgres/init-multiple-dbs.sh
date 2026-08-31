#!/bin/bash
set -e

function create_user_and_database() {
	local database=$1
	local user=$2
	local password=$3
	echo "Creating database '$database' and user '$user'"
	psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
	    CREATE USER $user WITH PASSWORD '$password';
	    CREATE DATABASE $database;
	    GRANT ALL PRIVILEGES ON DATABASE $database TO $user;
	    ALTER DATABASE $database OWNER TO $user;
EOSQL
}

if [ -n "$POSTGRES_MULTIPLE_DATABASES" ]; then
	echo "Multiple database creation requested: $POSTGRES_MULTIPLE_DATABASES"
	for db in $(echo $POSTGRES_MULTIPLE_DATABASES | tr ',' ' '); do
		create_user_and_database $db "${db}_user" "${db}_pass"
	done
	echo "Multiple databases created successfully!"
fi