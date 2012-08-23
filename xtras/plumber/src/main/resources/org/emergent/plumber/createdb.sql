CREATE TABLE GlobalDat
(schemaver INT NOT NULL
, created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
, modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE WeaveUser
(userid INT NOT NULL GENERATED ALWAYS AS IDENTITY
, username VARCHAR(32) NOT NULL UNIQUE
, created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
, modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
, password VARCHAR(256) NOT NULL
, email VARCHAR(256) NOT NULL UNIQUE
, PRIMARY KEY (userid)
);

CREATE TABLE EngineWbo
(userid INT NOT NULL
, engine VARCHAR(32) NOT NULL
, nodeid VARCHAR(64) NOT NULL
, sortindex INT
, modified TIMESTAMP NOT NULL
, payload VARCHAR(32000) NOT NULL
, ttl INT
, PRIMARY KEY (userid, engine, nodeid)
);
