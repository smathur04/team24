
DROP PROCEDURE IF EXISTS add_movie;

DELIMITER $$

CREATE PROCEDURE add_movie(
    IN m_title VARCHAR(100),
    IN m_year INT,
    IN m_director VARCHAR(100),
    IN s_name VARCHAR(100),
    IN g_name VARCHAR(32),
    IN s_birthYear INT,
    IN m_price FLOAT
)
    proc: BEGIN
    DECLARE m_id VARCHAR(10);
    DECLARE s_id VARCHAR(10);
    DECLARE g_id INT;
    DECLARE message TEXT DEFAULT '';

SELECT id INTO m_id
FROM movies
WHERE title = m_title AND year = m_year AND director = m_director
    LIMIT 1;

IF m_id IS NOT NULL THEN
        SET message = CONCAT('Movie already exists with ID: ', m_id);
SELECT message;
LEAVE proc;
END IF;

SELECT CONCAT('tt', LPAD(COALESCE(MAX(CAST(SUBSTRING(id, 3) AS UNSIGNED)) + 1, 1), 7, '0')) INTO m_id FROM movies;

INSERT INTO movies(id, title, year, director, price)
VALUES (m_id, m_title, m_year, m_director, m_price);

SET message = CONCAT(message, 'Movie added with ID: ', m_id, '\n');

SELECT id INTO s_id
FROM stars
WHERE name = s_name
    LIMIT 1;

IF s_id IS NULL THEN
SELECT CONCAT('nm', LPAD(COALESCE(MAX(CAST(SUBSTRING(id, 3) AS UNSIGNED)) + 1, 1), 7, '0')) INTO s_id FROM stars;
INSERT INTO stars(id, name, birthYear) VALUES (s_id, s_name, s_birthYear);
SET message = CONCAT(message, 'New star added with ID: ', s_id, '\n');
ELSE
        SET message = CONCAT(message, 'Existing star used with ID: ', s_id, '\n');
END IF;

INSERT INTO stars_in_movies(starId, movieId) VALUES (s_id, m_id);
SET message = CONCAT(message, 'Linked star to movie.\n');

SELECT id INTO g_id
FROM genres
WHERE name = g_name
    LIMIT 1;

IF g_id IS NULL THEN
        INSERT INTO genres(name) VALUES (g_name);
        SET g_id = LAST_INSERT_ID();
        SET message = CONCAT(message, 'New genre added with ID: ', g_id, '\n');
ELSE
        SET message = CONCAT(message, 'Existing genre used with ID: ', g_id, '\n');
END IF;

INSERT INTO genres_in_movies(genreId, movieId) VALUES (g_id, m_id);
SET message = CONCAT(message, 'Linked genre to movie.\n');

SELECT message;
END$$

DELIMITER ;
