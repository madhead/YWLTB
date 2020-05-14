CREATE TABLE users
(
    id                                    INT PRIMARY KEY,
    accessToken                           VARCHAR(4096) NOT NULL,
    refreshToken                          VARCHAR(4096) NOT NULL,
    videosAdded                           INT
);
