-- Run first:

CREATE DATABASE spatialexp;

\connect spatialexp

-- Run next:

CREATE USER spatialexp WITH ENCRYPTED PASSWORD 'spatialexp';

GRANT ALL PRIVILEGES ON DATABASE spatialexp TO spatialexp;

GRANT ALL ON schema public TO spatialexp;

CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;
